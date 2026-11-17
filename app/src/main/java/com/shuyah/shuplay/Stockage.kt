package com.shuyah.shuplay

import android.content.Context

/**
 * Mémoire de l'application (SharedPreferences, sur le téléphone seulement, jamais envoyée) :
 *  - la liste en cours (enregistrée automatiquement : l'écran l'indique), la piste et la position à reprendre ;
 *  - les listes nommées enregistrées par l'utilisateur ;
 *  - la dernière position de chaque piste (reprise là où l'on s'était arrêté).
 */
class Stockage(contexte: Context) {
    private val prefs = contexte.getSharedPreferences("shuplay", Context.MODE_PRIVATE)

    // ---- liste en cours
    fun enregistrerEnCours(pistes: List<Logique.Piste>, index: Int, positionMs: Long) {
        prefs.edit().putString("en_cours", Logique.versJson("en_cours", pistes)).putInt("index", index).putLong("position", positionMs).apply()
    }
    fun lireEnCours(): Triple<List<Logique.Piste>, Int, Long>? {
        val texte = prefs.getString("en_cours", null) ?: return null
        val (_, pistes) = Logique.depuisJson(texte) ?: return null
        return if (pistes.isEmpty()) null else Triple(pistes, prefs.getInt("index", 0), prefs.getLong("position", 0))
    }

    // ---- listes nommées
    fun noms(): List<String> = prefs.getStringSet("noms", emptySet())!!.sorted()
    fun enregistrerListe(nom: String, pistes: List<Logique.Piste>) {
        prefs.edit().putString("liste_$nom", Logique.versJson(nom, pistes)).putStringSet("noms", noms().toSet() + nom).apply()
    }
    fun lireListe(nom: String): List<Logique.Piste>? = prefs.getString("liste_$nom", null)?.let { Logique.depuisJson(it)?.second }
    fun supprimerListe(nom: String) {
        prefs.edit().remove("liste_$nom").putStringSet("noms", noms().toSet() - nom).apply()
    }

    // ---- reprise par piste
    fun memoriserPosition(uri: String, ms: Long) { prefs.edit().putLong("pos_$uri", ms).apply() }
    fun oublierPosition(uri: String) { prefs.edit().remove("pos_$uri").apply() }
    fun positionMemorisee(uri: String): Long = prefs.getLong("pos_$uri", 0)
}
