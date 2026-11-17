package com.shuyah.shuplay

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Écran unique de ShuPlay : le lecteur (PlayerView, commandes standard), les actions (pictogramme + légende) et la liste de lecture.
 * Le lecteur lui-même vit dans ServiceLecture : cette fenêtre n'est qu'une télécommande (MediaController), ce qui permet à la lecture
 * de continuer quand la fenêtre perd le focus ou que l'écran s'éteint.
 */
@UnstableApi
class MainActivity : ComponentActivity() {
    private var controleur: MediaController? = null
    private lateinit var stockage: Stockage
    private lateinit var vueLecteur: PlayerView
    private lateinit var liste: RecyclerView
    private lateinit var adaptateur: AdaptateurListe
    private var pleinEcran = false

    // Sélecteur de fichiers audio / vidéo du téléphone (plusieurs à la fois)
    private val choisirFichiers = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val pistes = uris.map { uri ->
            // L'accès au fichier est gardé après le redémarrage de l'application (liste enregistrée)
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            Logique.Piste(uri.toString(), Logique.titreDepuisNom(nomDuFichier(uri)))
        }
        ajouter(pistes)
    }
    private val demanderNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        stockage = Stockage(this)
        vueLecteur = findViewById(R.id.vue_lecteur)
        liste = findViewById(R.id.liste)

        // Liste de lecture : toucher = jouer cette piste ; flèches = réordonner ; corbeille = retirer
        adaptateur = AdaptateurListe(
            surChoix = { jouer(it) },
            surMonter = { i -> if (i > 0) controleur?.moveMediaItem(i, i - 1) },
            surDescendre = { i -> controleur?.let { if (i in 0 until it.mediaItemCount - 1) it.moveMediaItem(i, i + 1) } },
            surRetrait = { i -> controleur?.removeMediaItem(i) },
        )
        liste.layoutManager = LinearLayoutManager(this)
        liste.adapter = adaptateur

        // Actions
        findViewById<View>(R.id.act_fichiers).setOnClickListener { choisirFichiers.launch(arrayOf("audio/*", "video/*")) }
        findViewById<View>(R.id.act_lien).setOnClickListener { saisirLien() }
        findViewById<View>(R.id.act_aleatoire).setOnClickListener { basculerAleatoire() }
        findViewById<View>(R.id.act_repeter).setOnClickListener { changerRepetition() }
        findViewById<View>(R.id.act_enregistrer).setOnClickListener { enregistrerListe() }
        findViewById<View>(R.id.act_listes).setOnClickListener { ouvrirListes() }
        findViewById<View>(R.id.act_vider).setOnClickListener { confirmerVider() }

        // Plein écran du lecteur (bouton standard du lecteur) ; « Retour » le quitte
        vueLecteur.setFullscreenButtonClickListener { basculerPleinEcran(it) }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (pleinEcran) basculerPleinEcran(false) else { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }
        })

        // Version affichée (règle 2) : « Version X · déployée le JJ/MM/AAAA HH:MM »
        findViewById<TextView>(R.id.version).text = "Version ${BuildConfig.VERSION_NAME} · déployée le ${dateDeploiement()}"

        // Android 13 et plus : autorisation d'afficher la notification de contrôle (demandée une seule fois)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            demanderNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

        connecter()
    }

    /** Se relie au service de lecture (le démarre si besoin), puis restaure la liste et traite un éventuel « Ouvrir avec ». */
    private fun connecter() {
        val jeton = SessionToken(this, ComponentName(this, ServiceLecture::class.java))
        val futur = MediaController.Builder(this, jeton).buildAsync()
        futur.addListener({
            val c = futur.get()
            controleur = c
            vueLecteur.player = c
            c.addListener(object : Player.Listener {
                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = rafraichir()
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = rafraichir()
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = rafraichirBoutons()
                override fun onRepeatModeChanged(repeatMode: Int) = rafraichirBoutons()
                override fun onIsPlayingChanged(isPlaying: Boolean) { if (!isPlaying) memoriserPositionActuelle() }
                override fun onPositionDiscontinuity(ancienne: Player.PositionInfo, nouvelle: Player.PositionInfo, motif: Int) {
                    val uri = ancienne.mediaItem?.mediaId ?: return
                    // Piste terminée normalement : on l'oublie (la prochaine fois, elle repart du début) ; sinon on mémorise où on en était
                    if (motif == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) stockage.oublierPosition(uri) else if (ancienne.mediaItemIndex != nouvelle.mediaItemIndex) stockage.memoriserPosition(uri, ancienne.positionMs)
                }
                override fun onPlayerError(erreur: PlaybackException) {
                    // Fichier introuvable ou illisible : on le dit et on passe à la suite au lieu de rester bloqué
                    toast("Lecture impossible : ${erreur.errorCodeName}")
                    c.prepare(); if (c.hasNextMediaItem()) { c.seekToNextMediaItem(); c.play() }
                }
            })
            if (c.mediaItemCount == 0) restaurer(c)
            rafraichir(); rafraichirBoutons()
            traiterIntent(intent)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun restaurer(c: MediaController) {
        val (pistes, index, position) = stockage.lireEnCours() ?: return
        c.setMediaItems(pistes.map(::versMediaItem), index.coerceIn(0, pistes.size - 1), position)
        c.prepare()                                   // prête, sans démarrer : l'utilisateur appuie sur lecture
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); traiterIntent(intent) }

    /** « Ouvrir avec ShuPlay » depuis un autre programme : le fichier est ajouté à la liste et lu tout de suite. */
    private fun traiterIntent(i: Intent?) {
        val uri = i?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        i.data = null                                  // ne pas le rejouer à la prochaine rotation
        ajouter(listOf(Logique.Piste(uri.toString(), Logique.titreDepuisNom(nomDuFichier(uri) ?: uri.lastPathSegment))), lire = true)
    }

    override fun onStart() { super.onStart(); Presence.signaler(this) }

    /** Quand la fenêtre n'est plus visible : on garde la liste et la position (la lecture, elle, continue dans le service). */
    override fun onStop() { super.onStop(); sauvegarderEnCours() }

    override fun onDestroy() { vueLecteur.player = null; controleur?.release(); controleur = null; super.onDestroy() }

    // ------------------------------------------------------------------ liste de lecture
    private fun versMediaItem(p: Logique.Piste): MediaItem =
        MediaItem.Builder().setUri(p.uri).setMediaId(p.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(p.titre).build()).build()

    private fun lirePistes(): List<Logique.Piste> {
        val c = controleur ?: return emptyList()
        return (0 until c.mediaItemCount).map { val m = c.getMediaItemAt(it); Logique.Piste(m.mediaId, m.mediaMetadata.title?.toString() ?: "Sans titre") }
    }

    /** Ajoute des pistes ; si la liste était vide (ou si `lire`), la lecture démarre sur la première ajoutée. */
    private fun ajouter(pistes: List<Logique.Piste>, lire: Boolean = false) {
        val c = controleur ?: return
        if (pistes.isEmpty()) return
        val etaitVide = c.mediaItemCount == 0
        val premier = c.mediaItemCount
        c.addMediaItems(pistes.map(::versMediaItem))
        if (etaitVide || lire) { c.prepare(); c.seekToDefaultPosition(premier); c.play() }
        toast(if (pistes.size == 1) "« ${pistes[0].titre} » ajouté à la liste" else "${pistes.size} pistes ajoutées à la liste")
    }

    /** Joue la piste n° `index`, en reprenant là où on s'était arrêté la dernière fois. */
    private fun jouer(index: Int) {
        val c = controleur ?: return
        if (index !in 0 until c.mediaItemCount) return
        val position = Logique.positionDeReprise(stockage.positionMemorisee(c.getMediaItemAt(index).mediaId), -1)
        c.prepare(); c.seekTo(index, position); c.play()
    }

    private fun memoriserPositionActuelle() {
        val c = controleur ?: return
        c.currentMediaItem?.let { stockage.memoriserPosition(it.mediaId, c.currentPosition) }
    }

    private fun sauvegarderEnCours() {
        val c = controleur ?: return
        memoriserPositionActuelle()
        val pistes = lirePistes()
        if (pistes.isNotEmpty()) stockage.enregistrerEnCours(pistes, c.currentMediaItemIndex, c.currentPosition)
    }

    private fun rafraichir() {
        val c = controleur ?: return
        val pistes = lirePistes()
        adaptateur.mettreAJour(pistes, if (pistes.isEmpty()) -1 else c.currentMediaItemIndex)
        findViewById<TextView>(R.id.titre_en_cours).text = c.currentMediaItem?.mediaMetadata?.title ?: "Aucune piste"
        findViewById<TextView>(R.id.etat_liste).text = if (pistes.isEmpty()) "Liste de lecture vide : ajoutez des fichiers ou un lien."
            else "${pistes.size} piste${if (pistes.size > 1) "s" else ""} · liste enregistrée automatiquement sur ce téléphone"
        sauvegarderEnCours()
    }

    /** Aléatoire et Répéter : la couleur de fond montre l'état (et la légende l'écrit : jamais la couleur seule). */
    private fun rafraichirBoutons() {
        val c = controleur ?: return
        val alea = findViewById<TextView>(R.id.act_aleatoire)
        alea.text = if (c.shuffleModeEnabled) "Aléatoire : Oui" else "Aléatoire : Non"
        alea.setBackgroundColor(if (c.shuffleModeEnabled) Color.parseColor("#BAE6FD") else Color.TRANSPARENT)
        val rep = findViewById<TextView>(R.id.act_repeter)
        rep.text = when (c.repeatMode) { Player.REPEAT_MODE_ALL -> "Répéter : Liste"; Player.REPEAT_MODE_ONE -> "Répéter : Piste"; else -> "Répéter : Non" }
        rep.setBackgroundColor(if (c.repeatMode != Player.REPEAT_MODE_OFF) Color.parseColor("#BAE6FD") else Color.TRANSPARENT)
    }

    // ------------------------------------------------------------------ actions
    private fun basculerAleatoire() { controleur?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    private fun changerRepetition() { controleur?.let { it.repeatMode = Logique.modeRepetitionSuivant(it.repeatMode) } }

    private fun saisirLien() {
        val champ = EditText(this).apply { hint = "https://exemple.com/video.mp4"; inputType = InputType.TYPE_TEXT_VARIATION_URI; setSingleLine() }
        android.app.AlertDialog.Builder(this).setTitle("Ajouter Un Lien Direct").setView(champ)
            .setPositiveButton("Ajouter") { _, _ ->
                val lien = Logique.lienValide(champ.text.toString())
                if (lien == null) toast("Lien invalide : il doit commencer par http:// ou https://")
                else ajouter(listOf(Logique.Piste(lien, Logique.titreDepuisNom(Uri.parse(lien).lastPathSegment ?: lien))), lire = true)
            }.setNegativeButton("Annuler", null).show()
    }

    private fun enregistrerListe() {
        val pistes = lirePistes()
        if (pistes.isEmpty()) { toast("La liste est vide : rien à enregistrer"); return }
        val champ = EditText(this).apply { hint = "Nom de la liste"; setSingleLine() }
        android.app.AlertDialog.Builder(this).setTitle("Enregistrer Cette Liste").setView(champ)
            .setPositiveButton("Enregistrer") { _, _ ->
                val nom = champ.text.toString().trim().take(40)
                if (nom.isEmpty()) toast("Donnez un nom à la liste")
                else { stockage.enregistrerListe(nom, pistes); toast("Liste « $nom » enregistrée (${pistes.size} pistes)") }
            }.setNegativeButton("Annuler", null).show()
    }

    private fun ouvrirListes() {
        val noms = stockage.noms()
        if (noms.isEmpty()) { toast("Aucune liste enregistrée pour le moment"); return }
        android.app.AlertDialog.Builder(this).setTitle("Mes Listes").setItems(noms.toTypedArray()) { _, i ->
            val nom = noms[i]
            android.app.AlertDialog.Builder(this).setTitle("« $nom »").setMessage("Que faire de cette liste ?")
                .setPositiveButton("Charger") { _, _ ->
                    val pistes = stockage.lireListe(nom)
                    val c = controleur
                    if (pistes.isNullOrEmpty() || c == null) toast("Liste illisible")
                    else { c.setMediaItems(pistes.map(::versMediaItem)); c.prepare(); c.play(); toast("Liste « $nom » chargée") }
                }
                .setNegativeButton("Supprimer") { _, _ -> stockage.supprimerListe(nom); toast("Liste « $nom » supprimée") }
                .setNeutralButton("Annuler", null).show()
        }.setNegativeButton("Fermer", null).show()
    }

    private fun confirmerVider() {
        val c = controleur ?: return
        if (c.mediaItemCount == 0) return
        android.app.AlertDialog.Builder(this).setTitle("Vider La Liste").setMessage("Retirer les ${c.mediaItemCount} pistes de la liste de lecture ? Les fichiers ne sont pas effacés.")
            .setPositiveButton("Vider") { _, _ -> c.stop(); c.clearMediaItems(); toast("Liste vidée") }
            .setNegativeButton("Annuler", null).show()
    }

    /** Plein écran : on masque l'en-tête et la liste, on passe en paysage et on cache les barres du système. */
    private fun basculerPleinEcran(actif: Boolean) {
        pleinEcran = actif
        findViewById<View>(R.id.entete).visibility = if (actif) View.GONE else View.VISIBLE
        findViewById<View>(R.id.panneau).visibility = if (actif) View.GONE else View.VISIBLE
        vueLecteur.layoutParams = vueLecteur.layoutParams.apply { height = if (actif) ViewGroup.LayoutParams.MATCH_PARENT else (220 * resources.displayMetrics.density).toInt() }
        requestedOrientation = if (actif) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val barres = WindowCompat.getInsetsController(window, window.decorView)
        if (actif) { barres.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE; barres.hide(WindowInsetsCompat.Type.systemBars()) }
        else barres.show(WindowInsetsCompat.Type.systemBars())
    }

    // ------------------------------------------------------------------ outils
    private fun nomDuFichier(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    /** Date de compilation (UTC dans BuildConfig) affichée à l'heure du téléphone au format français court. */
    private fun dateDeploiement(): String = runCatching {
        val source = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(BuildConfig.DATE_BUILD.take(19))!!
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(source)
    }.getOrDefault("—")

    private fun toast(texte: String) = Toast.makeText(this, texte, Toast.LENGTH_SHORT).show()
}
