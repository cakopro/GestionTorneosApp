package com.cakocorps.administraciondetorneroselectronicos

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.auth.FirebaseAuth

class InscripcionFormulario : Fragment() {


    private val db = FirebaseFirestore.getInstance()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

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

        val contenedorJugadoresExtra =
            view.findViewById<LinearLayout>(R.id.contenedorJugadoresExtra)

        //manipular los if
        val torneoSeleccionado = DatosRepository.torneoSeleccionado

        val inscripcionAEditar = DatosRepository.inscripcionAEditar

        val idUsuarioActual =
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val listaInputsExtra = mutableListOf<android.widget.EditText>()

        firebaseAnalytics = Firebase.analytics


        if (torneoSeleccionado != null) {

            if (inscripcionAEditar != null) {
                btnGuardar.text = "Actualizar Inscripción"
            } else {
                btnGuardar.text = "Confirmar Inscripción"
            }

            //bloquear correo de CONTACTO
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            contacto.setText(inscripcionAEditar?.contactoLider ?: userEmail)
            contacto.isEnabled = false


            if (torneoSeleccionado.modalidad == "Equipos") {

                //nombre equipos si se puede editrar en equipos
                participanteOEquipo.hint = "Nombre del Equipo"
                participanteOEquipo.isEnabled = true
                if (inscripcionAEditar != null) {
                    participanteOEquipo.setText(inscripcionAEditar.nombreEquipo)
                }

                //buscar juego y su total de jugadores
                val juegoActual =
                    DatosRepository.listaVideojuego.find { it.nombre == torneoSeleccionado.juego }
                val totalJugadores = juegoActual?.jugadoresPorEquipo ?: 1

                //recorrer parar crear los editText en base a la cantidad de jugadores
                for (i in 1..totalJugadores) {
                    val nuevoInput = android.widget.EditText(requireContext())


                    if (i == 1) {
                        nuevoInput.hint = "1. Tu Nickname (Capitán)"
                        nuevoInput.setText(DatosRepository.usuarioActual?.nombre)
                        nuevoInput.isEnabled = false

                    } else {
                        nuevoInput.hint = "$i. Nickname del compañero ${i - 1}"
                        nuevoInput.isEnabled = true

                        if (inscripcionAEditar != null) {
                            val indexIntegrante = i - 1 // i - 1 coincide con la lista de integrantes extra
                            if (indexIntegrante < inscripcionAEditar.integrantes.size) {
                                nuevoInput.setText(inscripcionAEditar.integrantes[indexIntegrante])
                            }
                        }
                    }

                    val parametros = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    parametros.setMargins(0, 20, 0, 20)
                    nuevoInput.layoutParams = parametros
                    nuevoInput.setPadding(30, 40, 30, 40)
                    nuevoInput.setBackgroundResource(android.R.drawable.edit_text)

                    contenedorJugadoresExtra.addView(nuevoInput)
                    listaInputsExtra.add(nuevoInput)
                }

            } else {
                participanteOEquipo.hint = "Tu Nickname"
                participanteOEquipo.setText(DatosRepository.usuarioActual?.nombre)
                participanteOEquipo.isEnabled = false

            }


            btnGuardar.setOnClickListener {

                val nombrePrincipal = participanteOEquipo.text.toString().trim()
                val contactoLider = contacto.text.toString().trim()

                if (nombrePrincipal.isEmpty() || contactoLider.isEmpty()) {

                    Toast.makeText(
                        requireContext(), "Debe rellenar todos los campos.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener

                }

                if (!Patterns.EMAIL_ADDRESS.matcher(contactoLider).matches()) {
                    contacto.error = "Ingrese un correo electrónico válido"
                    Toast.makeText(
                        requireContext(),
                        "El correo de contacto no es válido.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }


                val nombresAInscribir = mutableListOf<String>()
                nombresAInscribir.add(nombrePrincipal)
                val esEquipo = torneoSeleccionado.modalidad == "Equipos"

                //si es equipo extraer la lista de los miembros.
                if (esEquipo) {
                    for (inputExtra in listaInputsExtra) {
                        val nombreCompleto = inputExtra.text.toString().trim()
                        if (nombreCompleto.isEmpty()) {
                            Toast.makeText(requireContext(), "Debe rellenar todos los campos de los integrantes.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if (nombresAInscribir.contains(nombreCompleto)) {
                            Toast.makeText(requireContext(), "Hay nombres duplicados en tu equipo: $nombreCompleto", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        nombresAInscribir.add(nombreCompleto)
                    }
                }

                btnGuardar.isEnabled = false
                btnGuardar.text = "Guardando..."




                val docRef = if (inscripcionAEditar != null) {
                    db.collection("Inscripciones").document(inscripcionAEditar.idInscripcion)
                } else {
                    db.collection("Inscripciones").document()
                }

                val ejecutarGuardado = {
                    val inscripcion = Inscripcion(
                        idInscripcion = docRef.id,
                        idTorneo = torneoSeleccionado.id,
                        idUsuario = idUsuarioActual,
                        esEquipo = esEquipo,
                        nombreEquipo = nombrePrincipal,
                        integrantes = if (esEquipo) nombresAInscribir.drop(1) else listOf(),
                        contactoLider = contactoLider,
                        pagoRealizado = inscripcionAEditar?.pagoRealizado ?: false
                    )

                    docRef.set(inscripcion)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "¡Inscripción guardada con éxito!",
                                Toast.LENGTH_SHORT
                            ).show()

                            if (inscripcionAEditar != null) {
                                firebaseAnalytics.logEvent("registro_actualizado") {
                                    param("tipo_entidad", "Inscripcion")
                                }

                            } else {
                                firebaseAnalytics.logEvent("registro_creado") {
                                    param("tipo_entidad", "Inscripcion")
                                }

                            }


                            DatosRepository.inscripcionAEditar = null
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                        .addOnFailureListener {

                            Toast.makeText(
                                requireContext(),
                                "Error de red al guardar",
                                Toast.LENGTH_SHORT
                            ).show()
                            btnGuardar.isEnabled = true
                            btnGuardar.text =
                                if (inscripcionAEditar != null) "Actualizar Inscripción" else "Confirmar Inscripción"
                        }


                }

                db.collection("Inscripciones")
                    .whereEqualTo("idTorneo", torneoSeleccionado.id)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val nombresOcupados = mutableSetOf<String>()
                        val miNombreOficial = DatosRepository.usuarioActual?.nombre?.trim()?.lowercase() ?: ""

                        // Recopilar todos los nombres de los otros equipos
                        for (document in querySnapshot) {
                            val idUsuarioInscrito = document.getString("idUsuario") ?: ""

                            // Ignorar la propia inscripción si se está editando o si ya estoy inscrito
                            if ((inscripcionAEditar != null && document.id == inscripcionAEditar.idInscripcion) || idUsuarioInscrito == idUsuarioActual) {
                                continue
                            }

                            val equipo = document.getString("nombreEquipo") ?: ""
                            if (equipo.isNotBlank()) nombresOcupados.add(equipo.trim().lowercase())

                            val integrantesRegistrados = document.get("integrantes") as? List<String> ?: listOf()
                            for (integrante in integrantesRegistrados) {
                                if (integrante.isNotBlank()) {
                                    val integranteLower = integrante.trim().lowercase()
                                    if (integranteLower != miNombreOficial) {
                                        nombresOcupados.add(integranteLower)
                                    }
                                }
                            }
                        }

                        // Verificar si choco con alguien
                        val nombreRepetidoEnTorneo = nombresAInscribir.find { nuevoNombre ->
                            nombresOcupados.contains(nuevoNombre.trim().lowercase())
                        }

                        if (nombreRepetidoEnTorneo != null) {
                            Toast.makeText(requireContext(), "El nickname '$nombreRepetidoEnTorneo' ya está registrado en este torneo.", Toast.LENGTH_LONG).show()
                            btnGuardar.isEnabled = true
                            btnGuardar.text = if (inscripcionAEditar != null) "Actualizar Inscripción" else "Confirmar Inscripción"
                            return@addOnSuccessListener // Abortar guardado
                        }

                        // Verificar si ya envió formulario antes (solo si es nuevo)
                        if (inscripcionAEditar == null) {
                            val yaInscrito = querySnapshot.documents.any { it.getString("idUsuario") == idUsuarioActual }
                            if (yaInscrito) {
                                Toast.makeText(requireContext(), "Ya estás inscrito en este torneo.", Toast.LENGTH_LONG).show()
                                btnGuardar.isEnabled = true
                                btnGuardar.text = "Confirmar Inscripción"
                                return@addOnSuccessListener // Abortar guardado
                            }
                        }

                        // 4. SI TODO SALIÓ BIEN Y NO HAY DUPLICADOS
                        ejecutarGuardado()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al verificar disponibilidad en el torneo", Toast.LENGTH_SHORT).show()
                        btnGuardar.isEnabled = true
                        btnGuardar.text = if (inscripcionAEditar != null) "Actualizar Inscripción" else "Confirmar Inscripción"
                    }


            }
        }
    }
}