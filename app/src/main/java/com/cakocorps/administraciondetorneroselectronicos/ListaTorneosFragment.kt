package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ListaTorneosFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

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

        db.collection("Torneos")
            .get()
            .addOnSuccessListener { result ->
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