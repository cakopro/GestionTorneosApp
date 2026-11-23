package com.cakocorps.administraciondetorneroselectronicos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
class TorneoAdapter(
    private val listaTorneos: List<Torneo>,
    private val onClick: (Torneo) -> Unit
) : RecyclerView.Adapter<TorneoAdapter.TorneoViewHolder>() {

    class TorneoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreTorneoItem)
        val tvJuego: TextView = view.findViewById(R.id.tvJuegoTorneoItem)
        val tvFecha: TextView = view.findViewById(R.id.tvFechaInicioItem)
        val tvCupos: TextView = view.findViewById(R.id.tvCuposItem)

        val ivTrofeo: ImageView = view.findViewById(R.id.ivTrofeoItem)
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

        if (torneo.trofeoUrl.isNotBlank()) {
            val bitmap = decodificarBase64ABitmap(torneo.trofeoUrl)
            if (bitmap != null) {
                holder.ivTrofeo.setImageBitmap(bitmap)
            } else {
                holder.ivTrofeo.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.ivTrofeo.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            onClick(torneo)
        }
    }

    override fun getItemCount(): Int = listaTorneos.size

    private fun decodificarBase64ABitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}