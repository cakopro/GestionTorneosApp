package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MisTorneosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //convertir archivo en objeto mani
        val view = inflater.inflate(R.layout.fragment_mis_torneos, container, false)
        val boton = view.findViewById<FloatingActionButton>(R.id.bntFlotante)
        val rvMisTorneos = view.findViewById<RecyclerView>(R.id.rvMisTorneos)

        val idUsuarioActual = DatosRepository.usuarioActual?.id ?: ""

        val misTorneosFiltrados = DatosRepository.listaTorneos.filter { torneo ->
            val esCreador = torneo.idCreador == idUsuarioActual
            val estaInscrito = torneo.inscritos.any { it.idUsuario == idUsuarioActual }
            esCreador || estaInscrito
        }

        rvMisTorneos.layoutManager = LinearLayoutManager(requireContext())
        rvMisTorneos.adapter = TorneoAdapter(misTorneosFiltrados) { torneoSeleccionado ->

            DatosRepository.torneoSeleccionado = torneoSeleccionado

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DetalleTorneoFragment())
                .addToBackStack(null)
                .commit()
        }



        boton.setOnClickListener {

            DatosRepository.torneoSeleccionado = null

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, formularioFragment())
                .addToBackStack(null)
                .commit()
        }

        return  view
    }

}