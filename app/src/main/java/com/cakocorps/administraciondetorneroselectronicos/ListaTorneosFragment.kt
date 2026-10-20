package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.analytics.logEvent
class ListaTorneosFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lista_torneos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTorneos = view.findViewById<RecyclerView>(R.id.rvTorneos)
        //como debe ordenarse
        rvTorneos.layoutManager = LinearLayoutManager(requireContext())
        firebaseAnalytics = Firebase.analytics

        db.collection("Torneos")
            .get()
            .addOnSuccessListener { result ->

                Firebase.analytics.logEvent("listado_consultado") {
                    param("tipo_entidad", "Torneo")
                }

                val lista = mutableListOf<Torneo>()
                    for (document in result) {
                        val torneo = document.toObject(Torneo::class.java)
                        torneo.id = document.id
                        lista.add(torneo)

                    }
                        rvTorneos.adapter = TorneoAdapter(lista) { torneoSeleccionado ->

                            //cuando tocas tarjeta guarda torneo orden de remplazar fragment por detalles
                            DatosRepository.torneoSeleccionado = torneoSeleccionado

                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, DetalleTorneoFragment())
                                .addToBackStack(null)
                                .commit()
                        }

            }


    }
}