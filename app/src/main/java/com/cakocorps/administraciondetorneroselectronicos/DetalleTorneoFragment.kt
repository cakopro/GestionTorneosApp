package com.cakocorps.administraciondetorneroselectronicos

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

        //inscripcion
        val tilNombreEquipo = view.findViewById<View>(R.id.tilNombreEquipo)
        val tilContacto = view.findViewById<View>(R.id.tilContacto)
        val etNombreEquipo = view.findViewById<TextInputEditText>(R.id.etNombreEquipo)
        val etContacto = view.findViewById<TextInputEditText>(R.id.etContacto)

        //botones para ocultar y mostrar
        val layoutCreador = view.findViewById<View>(R.id.layoutBotonesCreador)
        val layoutInscripcion = view.findViewById<View>(R.id.layoutBotonesInscripcion)

        val btnEditar = view.findViewById<Button>(R.id.btnEditar)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminar)
        val btnInscribirme = view.findViewById<Button>(R.id.btnInscribirme)
        val btnSalirTorneo = view.findViewById<Button>(R.id.btnSalirTorneo)

        //ver torneo seleccionado y su usuario
        val torneo = DatosRepository.torneoSeleccionado
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val usuarioActual = auth.currentUser
        val idUsuarioActual = usuarioActual?.uid ?: ""

        if (torneo != null){

            tvNombre.text = torneo.nombre
            tvInfo.text = """
                Juego: ${torneo.juego} (${torneo.dispositivo})
                Modalidad: ${torneo.modalidad} | Formato: ${torneo.formato}
                Entrada: ${torneo.tipoEntrada} ($${torneo.costoEntrada})
                Premio: ${torneo.recompensa}
                Cupos: ${torneo.inscritos.size} / ${torneo.cupos}
                
                Reglas:
                ${torneo.reglas}
            """.trimIndent()
            //para ver que botones o que interfaz darte
            val escreador = torneo.idCreador == idUsuarioActual
            val inscripcionExistente = torneo.inscritos.find { it.idUsuario == idUsuarioActual }

            //mostrar botones para borrar y actualizar.
            if (escreador){

                layoutCreador.visibility = View.VISIBLE
                //desaparece todo lo de inscribirse
                layoutInscripcion.visibility = View.GONE

                tilNombreEquipo.visibility = View.GONE
                tilContacto.visibility = View.GONE
                etNombreEquipo.visibility = View.GONE
                etContacto.visibility = View.GONE

                btnEditar.setOnClickListener {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, formularioFragment())
                        .addToBackStack(null)
                        .commit()
                }

                btnEliminar.setOnClickListener {
                    db.collection("Torneos").document(torneo.id).delete()
                    Toast.makeText(requireContext(), "Torneo eliminado correctamente", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }else{
                //desaparece tpdp ñp de creador
                layoutCreador.visibility = View.GONE
                layoutInscripcion.visibility = View.VISIBLE
                tilNombreEquipo.visibility = View.VISIBLE
                tilContacto.visibility = View.VISIBLE

                if (inscripcionExistente != null){
                    etNombreEquipo.setText(inscripcionExistente.nombreEquipo)
                    etContacto.setText(inscripcionExistente.contactoLider)
                    btnInscribirme.text = "Actualizar mis datos"
                    btnSalirTorneo.visibility = View.VISIBLE

                    btnSalirTorneo.setOnClickListener {
                        torneo.inscritos.remove(inscripcionExistente)
                        Toast.makeText(requireContext(), "Has salido del torneo", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }

                }else{
                    btnInscribirme.text = "Inscribirse al Torneo"
                    btnSalirTorneo.visibility = View.GONE
                }

                btnInscribirme.setOnClickListener {
                    val equipo = etNombreEquipo.text.toString().trim()
                    val contacto = etContacto.text.toString().trim()
                    val nombreParaInscripcion = usuarioActual?.email?.substringBefore("@") ?: "Anónimo"

                    if (equipo.isEmpty() || contacto.isEmpty()) {
                        Toast.makeText(requireContext(), "Completa los datos de inscripción", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val nuevaInscri = Inscripcion(idUsuario = idUsuarioActual,
                        nombreEquipo = equipo,
                        nombreParticipante = nombreParaInscripcion,
                        contactoLider = contacto
                    )

                    if (inscripcionExistente != null) {

                        val index = torneo.inscritos.indexOf(inscripcionExistente)
                        if (index != -1) {

                            torneo.inscritos[index] = nuevaInscri
                        }
                    }else{
                            torneo.inscritos.add(nuevaInscri)
                            Toast.makeText(requireContext(), "¡Inscripción exitosa!", Toast.LENGTH_SHORT).show()
                        }
                        requireActivity().supportFragmentManager.popBackStack()


                }

            }
        }

    }
}