package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class Vistapodioo : Fragment(R.layout.fragment_podioo) {


    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var textoInformacion = view.findViewById<TextView>(R.id.tvMensaje)
        var txtsegundoLugar = view.findViewById<TextView>(R.id.tvNombreSegundo)
        var txtprimerLugar = view.findViewById<TextView>(R.id.tvNombrePrimero)
        var txttercerLugar = view.findViewById<TextView>(R.id.tvNombreTercero)

        val torneo = DatosRepository.torneoSeleccionado ?: return


        db.collection("Podios")
            .whereEqualTo("idTorneo",torneo.id)
            .get()
            .addOnSuccessListener { podiosSnapchot ->

                if (!podiosSnapchot.isEmpty){

                    val listaPodios = mutableListOf<Podio>()

                    for (document in podiosSnapchot) {
                        val podio = document.toObject(Podio::class.java)
                        listaPodios.add(podio)
                    }

                    val primerLugar = listaPodios.find { it.puesto == 1 }
                    val segundoLugar = listaPodios.find { it.puesto == 2 }
                    val tercerLugar= listaPodios.find { it.puesto == 3 }

                    txtprimerLugar.text =  primerLugar?.nombreJugador ?: ""
                    txtsegundoLugar.text = segundoLugar?.nombreJugador ?: ""
                    txttercerLugar.text = tercerLugar?.nombreJugador ?: ""
                }else{

                    textoInformacion.text = "Aun no termina el torneo"
                    txtprimerLugar.text = ""
                    txtsegundoLugar.text = ""
                    txttercerLugar.text = ""
                }


            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al traer podios", Toast.LENGTH_SHORT).show()

            }
    }
}