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
        val rvTorneosCreados = view.findViewById<RecyclerView>(R.id.rvTorneosCreados)
        val rvTorneosInscritos = view.findViewById<RecyclerView>(R.id.rvTorneosInscritos)

        //buscar mi id
        val idUsuarioActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        //filtro para mostrar solo torneos donde este inscrito o los haya creado

        db.collection("Inscripciones")
            .whereEqualTo("idUsuario",idUsuarioActual)
            .get()
            .addOnSuccessListener { inscripcionesSnapshot ->
                val misTorneosInscritosIds = mutableListOf<String>()
                for (doc in inscripcionesSnapshot) {
                    doc.getString("idTorneo")?.let { misTorneosInscritosIds.add(it) }
                }

                db.collection("Torneos")
                    .get()
                    .addOnSuccessListener { result ->
                        val listaCreados = mutableListOf<Torneo>()
                        val listaInscritos = mutableListOf<Torneo>()
                        for (document in result) {
                            val torneo = document.toObject(Torneo::class.java)

                            torneo.id = document.id

                            if (torneo.idCreador == idUsuarioActual) {
                                listaCreados.add(torneo)
                            } else if (misTorneosInscritosIds.contains(torneo.id)) {
                                listaInscritos.add(torneo)
                            }
                        }
                        //vuelve a rellenar el recicle pero solo con los datos filtrados
                        rvTorneosCreados.layoutManager = LinearLayoutManager(requireContext())
                        rvTorneosCreados.adapter = TorneoAdapter(listaCreados) { torneoSeleccionado ->
                            DatosRepository.torneoSeleccionado = torneoSeleccionado
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, DetalleTorneoFragment())
                                .addToBackStack(null)
                                .commit()
                        }

                            //que pasa cuando toca cardview, guarda torneo  para luego cargar los datos
                        rvTorneosInscritos.layoutManager = LinearLayoutManager(requireContext())
                        rvTorneosInscritos.adapter = TorneoAdapter(listaInscritos) { torneoSeleccionado ->
                            DatosRepository.torneoSeleccionado = torneoSeleccionado
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, DetalleTorneoFragment())
                                .addToBackStack(null)
                                .commit()
                        }
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