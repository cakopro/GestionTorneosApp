package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListaTorneosFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lista_torneos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTorneos = view.findViewById<RecyclerView>(R.id.rvTorneos)
        rvTorneos.layoutManager = LinearLayoutManager(requireContext())
        rvTorneos.adapter = TorneoAdapter(DatosRepository.listaTorneos) { torneoSeleccionado ->

            DatosRepository.torneoSeleccionado = torneoSeleccionado

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DetalleTorneoFragment())
                .addToBackStack(null) // Permite volver atrás
                .commit()
        }
    }
}