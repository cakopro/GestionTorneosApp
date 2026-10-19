package com.cakocorps.administraciondetorneroselectronicos

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.compareTo

class DetalleTorneoFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detalle_torneo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNombre = view.findViewById<TextView>(R.id.tvNombreDetalle)
        val tvInfo = view.findViewById<TextView>(R.id.tvInfoDetalle)


        //botones para ocultar y mostrar
        val layoutCreador = view.findViewById<View>(R.id.layoutBotonesCreador)
        val layoutInscripcion = view.findViewById<View>(R.id.layoutBotonesInscripcion)

        val btnEditar = view.findViewById<Button>(R.id.btnEditar)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminar)
        val btnInscribirme = view.findViewById<Button>(R.id.btnInscribirme)
        val btnSalirTorneo = view.findViewById<Button>(R.id.btnSalirTorneo)

        val btnParticipantes = view.findViewById<Button>(R.id.btnParticipantes)


        //ver torneo seleccionado y su usuario
        val torneo = DatosRepository.torneoSeleccionado
        val idUsuarioActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (torneo != null){
            tvNombre.text = torneo.nombre
            tvInfo.text = "Cargando información de cupos..."

            db.collection("Inscripciones")
                .whereEqualTo("idTorneo",torneo.id)
                .get()
                .addOnSuccessListener { querySnapshot ->

                    val cantidadInscritos = querySnapshot.size()
                    var miInscripcion: Inscripcion? = null

                    for (document in querySnapshot)
                        if (document.getString("idUsuario") == idUsuarioActual){

                            miInscripcion = document.toObject(Inscripcion::class.java)
                            miInscripcion.idInscripcion = document.id
                            break
                        }

                    tvInfo.text = """
                Juego: ${torneo.juego} (${torneo.dispositivo})
                Modalidad: ${torneo.modalidad} | Formato: ${torneo.formato}
                Entrada: ${torneo.tipoEntrada} ($${torneo.costoEntrada})
                Premio: ${torneo.recompensa}
                Cupos: $cantidadInscritos / ${torneo.cupos}
                
                Reglas:
                ${torneo.reglas}
                """.trimIndent()

                    //para ver que botones o que interfaz darte
                    val escreador = torneo.idCreador == idUsuarioActual

                    //mostrar botones para borrar y actualizar.
                    if (escreador){

                        layoutCreador.visibility = View.VISIBLE
                        //desaparece todo lo de inscribirse
                        layoutInscripcion.visibility = View.GONE

                        btnEditar.setOnClickListener {
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, formularioFragment())
                                .addToBackStack(null)
                                .commit()
                        }

                        btnEliminar.setOnClickListener {

                            AlertDialog.Builder(context)
                                .setTitle("Eliminar Torneo")
                                .setMessage("¿ Esta seguro de que quiere eliminar este torneo?")
                                .setPositiveButton("Eliminar"){dialog, _ ->

                                    db.collection("Torneos").document(torneo.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "Torneo eliminado correctamente",
                                                Toast.LENGTH_SHORT).show()
                                            requireActivity().supportFragmentManager.popBackStack()
                                        }
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Cancelar"){ dialog, _ ->

                                    dialog.dismiss()
                                }.create()
                                .show()
                        }
                    }else{
                        //desaparece tpdp ñp de creador
                        layoutCreador.visibility = View.GONE
                        layoutInscripcion.visibility = View.VISIBLE

                        if (miInscripcion != null){
                            btnSalirTorneo.visibility = View.VISIBLE

                            btnSalirTorneo.setOnClickListener {

                                AlertDialog.Builder(context)
                                    .setTitle("Salir del Torneo")
                                    .setMessage("¿ Esta seguro de que desea salir del torneo?")
                                    .setPositiveButton("Eliminar"){dialog, _ ->

                                        db.collection("Inscripciones").document(miInscripcion.idInscripcion).delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Has salido del torneo",
                                                    Toast.LENGTH_SHORT).show()
                                                requireActivity().supportFragmentManager.popBackStack()

                                            }
                                        dialog.dismiss()

                                    }
                                    .setNegativeButton("Cancelar"){ dialog, _ ->

                                        dialog.dismiss()
                                    }.create()
                                    .show()
                            }

                        }else{
                            btnInscribirme.text = "Inscribirse al Torneo"
                            btnSalirTorneo.visibility = View.GONE
                        }
                        if (cantidadInscritos >= torneo.cupos) {
                            btnInscribirme.isEnabled = false
                            btnInscribirme.text = "Torneo Lleno"
                        }
                    }


                    btnInscribirme.setOnClickListener {

                        DatosRepository.inscripcionAEditar = miInscripcion

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, InscripcionFormulario())
                            .addToBackStack(null)
                            .commit()
                    }
                }


        }

                }




    }
