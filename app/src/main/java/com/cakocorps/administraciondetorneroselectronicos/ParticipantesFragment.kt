package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class ParticipantesFragment : Fragment(R.layout.fragment_participantes) {

    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvContadorInscritos = view.findViewById<TextView>(R.id.tvContadorInscritos)
        val tvListaCompleta = view.findViewById<TextView>(R.id.tvListaCompleta)

        val torneo = DatosRepository.torneoSeleccionado ?: return

        // 1. Obtener los nombres reales de los usuarios
        db.collection("Usuarios").get().addOnSuccessListener { usuariosSnapshot ->
            val mapaUsuarios = usuariosSnapshot.documents.associate { doc ->
                doc.id to (doc.getString("nombre") ?: "Usuario Desconocido")
            }

            // 2. Buscar las inscripciones de este torneo
            db.collection("Inscripciones")
                .whereEqualTo("idTorneo", torneo.id)
                .get()
                .addOnSuccessListener { inscripcionesSnapshot ->
                    val cantidad = inscripcionesSnapshot.size()
                    tvContadorInscritos.text = "$cantidad inscritos de ${torneo.cupos} cupos"

                    if (inscripcionesSnapshot.isEmpty) {
                        tvListaCompleta.text = "Aún no hay participantes inscritos."
                        return@addOnSuccessListener
                    }

                    val textoGenerado = StringBuilder()

                    inscripcionesSnapshot.documents.forEachIndexed { index, doc ->
                        val inscripcion = doc.toObject(Inscripcion::class.java)

                        val textoJugador = if (inscripcion?.esEquipo == true) {
                            val miembros = inscripcion.integrantes.joinToString(separator = "\n") { "  - $it" }
                            "Equipo: ${inscripcion.nombreEquipo}\nIntegrantes:\n$miembros"
                        } else {
                            val nombreReal = mapaUsuarios[inscripcion?.idUsuario] ?: "Participante Desconocido"
                            "Nickname: ${inscripcion?.nombreEquipo}\nUsuario: $nombreReal"
                        }

                        textoGenerado.append("${index + 1}. $textoJugador\n")
                        textoGenerado.append("--------------------------------------------------\n\n")
                    }

                    tvListaCompleta.text = textoGenerado.toString()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al cargar inscripciones", Toast.LENGTH_SHORT).show()
                }
        }
    }
}