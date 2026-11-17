package com.shuyah.shuplay

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Affiche la liste de lecture. La piste en cours est sur fond orange clair avec une police bleu nuit (règle d'affichage des
 * lignes sélectionnées du propriétaire : #f6a35b / #0A1E3D) ; les autres sont sur fond blanc.
 */
class AdaptateurListe(
    private val surChoix: (Int) -> Unit,
    private val surMonter: (Int) -> Unit,
    private val surDescendre: (Int) -> Unit,
    private val surRetrait: (Int) -> Unit,
) : RecyclerView.Adapter<AdaptateurListe.Ligne>() {
    private var pistes: List<Logique.Piste> = emptyList()
    private var enCours = -1

    fun mettreAJour(nouvelles: List<Logique.Piste>, index: Int) {
        pistes = nouvelles; enCours = index
        notifyDataSetChanged()
    }

    class Ligne(v: View) : RecyclerView.ViewHolder(v) {
        val ligne: View = v.findViewById(R.id.ligne)
        val numero: TextView = v.findViewById(R.id.numero)
        val titre: TextView = v.findViewById(R.id.titre)
        val monter: ImageView = v.findViewById(R.id.monter)
        val descendre: ImageView = v.findViewById(R.id.descendre)
        val retirer: ImageView = v.findViewById(R.id.retirer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Ligne(LayoutInflater.from(parent.context).inflate(R.layout.ligne_piste, parent, false))
    override fun getItemCount() = pistes.size

    override fun onBindViewHolder(h: Ligne, position: Int) {
        val actuelle = position == enCours
        h.ligne.setBackgroundColor(if (actuelle) Color.parseColor("#F6A35B") else Color.WHITE)
        h.titre.setTextColor(Color.parseColor("#0A1E3D"))
        h.numero.setTextColor(Color.parseColor(if (actuelle) "#0A1E3D" else "#03316E"))
        h.numero.text = (position + 1).toString()
        h.titre.text = pistes[position].titre
        h.ligne.setOnClickListener { surChoix(h.bindingAdapterPosition) }
        h.monter.setOnClickListener { surMonter(h.bindingAdapterPosition) }
        h.descendre.setOnClickListener { surDescendre(h.bindingAdapterPosition) }
        h.retirer.setOnClickListener { surRetrait(h.bindingAdapterPosition) }
    }
}
