package com.shuyah.shuplay

import org.json.JSONArray
import org.json.JSONObject

/**
 * Logique PURE de ShuPlay (aucune dépendance Android, donc testable sur l'ordinateur : voir LogiqueTest).
 * Chaque fonction est petite et commentée ; l'interface (MainActivity) ne fait qu'appeler ces fonctions.
 */
object Logique {
    /** Une piste de la liste de lecture : l'adresse (fichier « content://… » ou lien « https://… ») et le titre affiché. */
    data class Piste(val uri: String, val titre: String)

    /** Titre lisible tiré d'un nom de fichier : sans extension, « _ » remplacés par des espaces. Jamais vide. */
    fun titreDepuisNom(nom: String?): String {
        val sans = (nom ?: "").substringBeforeLast('.', nom ?: "").replace('_', ' ').trim()
        return sans.ifEmpty { "Sans titre" }
    }

    /** Durée « m:ss » ou « h:mm:ss » ; « --:-- » si la durée est inconnue (flux en direct, pas encore chargée). */
    fun formaterDuree(ms: Long): String {
        if (ms < 0) return "--:--"
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /**
     * Position à laquelle reprendre une piste déjà écoutée : 0 si on était au tout début (moins de 5 s) ou presque à la fin
     * (moins de 10 s avant la fin, quand la durée est connue) ; sinon la position mémorisée.
     */
    fun positionDeReprise(sauvegardeeMs: Long, dureeMs: Long): Long {
        if (sauvegardeeMs < 5_000) return 0
        if (dureeMs > 0 && sauvegardeeMs > dureeMs - 10_000) return 0
        return sauvegardeeMs
    }

    /** Un lien direct est accepté s'il commence par http:// ou https:// et contient un nom d'hôte. Rend le lien nettoyé, ou null. */
    fun lienValide(texte: String?): String? {
        val t = (texte ?: "").trim()
        val reste = when {
            t.startsWith("https://", ignoreCase = true) -> t.substring(8)
            t.startsWith("http://", ignoreCase = true) -> t.substring(7)
            else -> return null
        }
        return if (reste.isNotEmpty() && !reste.startsWith("/") && !reste.contains(' ')) t else null
    }

    /** Mode de répétition suivant : aucune (0) → toute la liste (2) → une piste (1) → aucune (0). Valeurs de Player.REPEAT_MODE_*. */
    fun modeRepetitionSuivant(mode: Int): Int = when (mode) { 0 -> 2; 2 -> 1; else -> 0 }

    /** Déplace une piste de `de` vers `vers` (borné) et rend la nouvelle liste ; la liste d'origine n'est pas modifiée. */
    fun deplacer(liste: List<Piste>, de: Int, vers: Int): List<Piste> {
        if (de !in liste.indices) return liste
        val cible = vers.coerceIn(0, liste.size - 1)
        if (cible == de) return liste
        val copie = liste.toMutableList()
        copie.add(cible, copie.removeAt(de))
        return copie
    }

    /** Liste enregistrée → texte JSON (nom + pistes). */
    fun versJson(nom: String, pistes: List<Piste>): String = JSONObject().apply {
        put("nom", nom)
        put("pistes", JSONArray().apply { pistes.forEach { put(JSONObject().put("uri", it.uri).put("titre", it.titre)) } })
    }.toString()

    /** Texte JSON → (nom, pistes). Rend null si le texte est abîmé : une liste illisible ne fait jamais planter l'application. */
    fun depuisJson(texte: String): Pair<String, List<Piste>>? = runCatching {
        val o = JSONObject(texte)
        val tab = o.getJSONArray("pistes")
        val pistes = (0 until tab.length()).map { val p = tab.getJSONObject(it); Piste(p.getString("uri"), p.optString("titre", "Sans titre")) }
        o.getString("nom") to pistes
    }.getOrNull()
}
