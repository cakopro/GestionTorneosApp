package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore


class InscripcionFormulario : Fragment() {


    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       val view =inflater.inflate(R.layout.fragment_inscripcion_formulario, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val participanteOEquipo = view.findViewById<TextInputEditText>(R.id.txtParticipanteOEquipo)

        val contacto = view.findViewById<TextInputEditText>(R.id.txtContactoLider)

        val btnGuardar = view.findViewById<Button>(R.id.btnConfirmarInscripcion)

        val contenedorJugadoresExtra = view.findViewById<LinearLayout>(R.id.contenedorJugadoresExtra)

        //manipular los if
        val torneoSeleccionado = DatosRepository.torneoSeleccionado

        val inscripcionAEditar = DatosRepository.inscripcionAEditar

        val idUsuarioActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val listaInputsExtra = mutableListOf<android.widget.EditText>()

        if (torneoSeleccionado != null){

            if (inscripcionAEditar != null) {
                btnGuardar.text = "Actualizar Inscripción"
                participanteOEquipo.setText(inscripcionAEditar.nombreEquipo)
                contacto.setText(inscripcionAEditar.contactoLider)
            } else {
                btnGuardar.text = "Confirmar Inscripción"
            }


            if (torneoSeleccionado.modalidad == "Equipos"){

                participanteOEquipo.hint = "Nombre del Equipo"
                //buscar juego y su total de jugadores, descontar uno ya que el lider se esta inscribiendo
                val juegoActual = DatosRepository.listaVideojuego.find { it.nombre == torneoSeleccionado.juego }
                val totalJugadores = juegoActual?.jugadoresPorEquipo ?:1

                //recorrer parar crear los editText en base a la cantidad de jugadores
                for (i in 1..totalJugadores) {
                    val nuevoInput = android.widget.EditText(requireContext())


                    if (i == 1){
                        nuevoInput.hint = "1. Tu Nickname (Capitán)"

                        if(inscripcionAEditar  == null){

                            nuevoInput.setText(DatosRepository.usuarioActual?.nombre)
                            nuevoInput.isEnabled = false
                        }

                    }else{
                        nuevoInput.hint = "$i. Nickname del compañero ${i - 1}"
                    }

                    val parametros = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    parametros.setMargins(0, 20, 0, 20)
                    nuevoInput.layoutParams = parametros
                    nuevoInput.setPadding(30, 40, 30, 40)
                    nuevoInput.setBackgroundResource(android.R.drawable.edit_text)

                    if (inscripcionAEditar != null){

                        if (inscripcionAEditar.integrantes.size  >= i ){

                            nuevoInput.setText(inscripcionAEditar.integrantes[i - 1])
                        }
                    }
                    contenedorJugadoresExtra.addView(nuevoInput)
                    listaInputsExtra.add(nuevoInput)
                }

            }else {
                participanteOEquipo.hint = "Tu Nickname"
                if (inscripcionAEditar == null) {
                    participanteOEquipo.setText(DatosRepository.usuarioActual?.nombre)
                    participanteOEquipo.isEnabled = false
                }

            }


            btnGuardar.setOnClickListener {

                val nombrePrincipal = participanteOEquipo.text.toString().trim()
                val contactoLider = contacto.text.toString().trim()

                if (nombrePrincipal.isEmpty() || contactoLider.isEmpty()){

                    Toast.makeText(requireContext(), "Debe rellenar todos los campos.",
                        Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                }

                val listaIntegrantesTexto = mutableListOf<String>()
                val esEquipo = torneoSeleccionado.modalidad == "Equipos"
                //si es equipo extraer la lista de los miembros.
                if (esEquipo){
                for (inputExtra in listaInputsExtra){
                    val nombreCompleto = inputExtra.text.toString().trim()

                    if (nombreCompleto.isEmpty()){
                    Toast.makeText(requireContext(), "Debe rellenar todos los campos.",
                        Toast.LENGTH_SHORT).show()
                    return@setOnClickListener

                    }
                    listaIntegrantesTexto.add(nombreCompleto)
                }

                }

                btnGuardar.isEnabled = false
                btnGuardar.text = "Guardando..."


                val docRef = if (inscripcionAEditar != null) {
                    db.collection("Inscripciones").document(inscripcionAEditar.idInscripcion)
                } else {
                    db.collection("Inscripciones").document()
                }

                val inscripcion = Inscripcion(
                    idInscripcion = docRef.id,
                    idTorneo = torneoSeleccionado.id,
                    idUsuario = idUsuarioActual,
                    esEquipo = esEquipo,
                    nombreEquipo = nombrePrincipal ,
                    integrantes = listaIntegrantesTexto,
                    contactoLider = contactoLider,
                    pagoRealizado = inscripcionAEditar?.pagoRealizado ?: false



                )

                docRef.set(inscripcion)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "¡Inscripción guardada con éxito!", Toast.LENGTH_SHORT).show()

                        DatosRepository.inscripcionAEditar = null
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {

                        Toast.makeText(requireContext(), "Error de red al guardar", Toast.LENGTH_SHORT).show()
                        btnGuardar.isEnabled = true
                        btnGuardar.text = if (inscripcionAEditar != null) "Actualizar Inscripción" else "Confirmar Inscripción"
                    }

            }


        }
    }

}