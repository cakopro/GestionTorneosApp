package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore


class MisTorneosFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //convertir archivo en objeto mani
        val view = inflater.inflate(R.layout.fragment_mis_torneos, container, false)
        val boton = view.findViewById<FloatingActionButton>(R.id.bntFlotante)
        val rvMisTorneos = view.findViewById<RecyclerView>(R.id.rvMisTorneos)

        //buscar mi id
        val idUsuarioActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        //filtro para mostrar solo torneos donde este inscrito o los haya creado

        db.collection("Torneos")
            .get()
            .addOnSuccessListener { result ->
                val lista = mutableListOf<Torneo>()
                for (document in result) {
                    val torneo = document.toObject(Torneo::class.java)

                    if (torneo.idCreador == idUsuarioActual || torneo.inscritos.any { it.idUsuario == idUsuarioActual }) {
                        torneo.id = document.id
                        lista.add(torneo)
                    }
                }
                //vuelve a rellenar el recicle pero solo con los datos filtrados
                rvMisTorneos.layoutManager = LinearLayoutManager(requireContext())
                rvMisTorneos.adapter = TorneoAdapter(lista) { torneoSeleccionado ->

                    //que pasa cuando toca cardview, guarda torneo  para luego cargar los datos
                    DatosRepository.torneoSeleccionado = torneoSeleccionado
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DetalleTorneoFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }

        //para el momento que se usa el boton flotante ir al formulario
        boton.setOnClickListener {
            //se deja null para que al entrar se entienda que se busca crear
            DatosRepository.torneoSeleccionado = null

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, formularioFragment())
                .addToBackStack(null)
                .commit()
        }

        return  view
    }

}