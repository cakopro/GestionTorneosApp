package com.cakocorps.administraciondetorneroselectronicos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TorneoAdapter(
    private val listaTorneos: List<Torneo>,
    private val onClick: (Torneo) -> Unit
) : RecyclerView.Adapter<TorneoAdapter.TorneoViewHolder>() {

    class TorneoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreTorneoItem)
        val tvJuego: TextView = view.findViewById(R.id.tvJuegoTorneoItem)
        val tvFecha: TextView = view.findViewById(R.id.tvFechaInicioItem)
        val tvCupos: TextView = view.findViewById(R.id.tvCuposItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorneoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_torneo, parent, false)
        return TorneoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TorneoViewHolder, position: Int) {
        val torneo = listaTorneos[position]
        holder.tvNombre.text = torneo.nombre
        holder.tvJuego.text = "Juego: ${torneo.juego}"
        holder.tvFecha.text = "Inicio: ${torneo.fechaInicio}"
        holder.tvCupos.text = "Cupos: ${torneo.cupos}"

        holder.itemView.setOnClickListener {
            onClick(torneo)
        }
    }

    override fun getItemCount(): Int = listaTorneos.size
}