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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InscripcionFormulario : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inscripcion_formulario, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtNombreEquipo = view.findViewById<TextInputEditText>(R.id.txtNombreEquipo)
        val contacto = view.findViewById<TextInputEditText>(R.id.txtContactoLider)
        val btnGuardar = view.findViewById<Button>(R.id.btnConfirmarInscripcion)
        val contenedorJugadoresExtra = view.findViewById<LinearLayout>(R.id.contenedorJugadoresExtra)

        val torneoSeleccionado = DatosRepository.torneoSeleccionado
        val inscripcionAEditar = DatosRepository.inscripcionAEditar
        val idUsuarioActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val nombreUsuarioActual = DatosRepository.usuarioActual?.nombre ?: "Usuario"

        val listaInputsExtra = mutableListOf<android.widget.EditText>()
        firebaseAnalytics = Firebase.analytics

        if (torneoSeleccionado != null) {

            btnGuardar.text = if (inscripcionAEditar != null) "Actualizar Equipo" else "Confirmar Equipo"

            // Bloquear correo de CONTACTO
            val userEmail = FirebaseAuth.getInstance().currentUser?.email
            contacto.setText(inscripcionAEditar?.contactoLider ?: userEmail)
            contacto.isEnabled = false

            // Pre-llenar datos si está editando
            if (inscripcionAEditar != null) {
                txtNombreEquipo.setText(inscripcionAEditar.nombreEquipo)
            }

            // Generar inputs para el equipo
            val juegoActual = DatosRepository.listaVideojuego.find { it.nombre == torneoSeleccionado.juego }
            val totalJugadores = juegoActual?.jugadoresPorEquipo ?: 1

            for (i in 1..totalJugadores) {
                val nuevoInput = android.widget.EditText(requireContext())

                if (i == 1) {
                    nuevoInput.hint = "1. Tu Nombre (Capitán)"
                    nuevoInput.setText(nombreUsuarioActual)
                    nuevoInput.isEnabled = false
                } else {
                    nuevoInput.hint = "$i. Nickname del compañero ${i - 1}"
                    if (inscripcionAEditar != null) {
                        val indexIntegrante = i - 2 // -2 porque el Capitán no está en la lista 'integrantes'
                        if (indexIntegrante >= 0 && indexIntegrante < inscripcionAEditar.integrantes.size) {
                            nuevoInput.setText(inscripcionAEditar.integrantes[indexIntegrante])
                        }
                    }
                }

                val parametros = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                parametros.setMargins(0, 20, 0, 20)
                nuevoInput.layoutParams = parametros
                nuevoInput.setPadding(30, 40, 30, 40)
                nuevoInput.setBackgroundResource(android.R.drawable.edit_text)

                contenedorJugadoresExtra.addView(nuevoInput)
                if (i > 1) listaInputsExtra.add(nuevoInput) // Solo añadimos los compañeros a la lista a validar
            }

            btnGuardar.setOnClickListener {
                val nombreEquipoStr = txtNombreEquipo.text.toString().trim()
                val contactoLider = contacto.text.toString().trim()

                if (nombreEquipoStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Debe ingresar el nombre del equipo.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val nombresCompaneros = mutableListOf<String>()
                val nombresUnicos = mutableSetOf<String>()
                nombresUnicos.add(nombreUsuarioActual.lowercase()) // Agregamos al capitán

                for (input in listaInputsExtra) {
                    val nombre = input.text.toString().trim()
                    if (nombre.isEmpty()) {
                        Toast.makeText(requireContext(), "Debe rellenar todos los compañeros.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (!nombresUnicos.add(nombre.lowercase())) {
                        Toast.makeText(requireContext(), "Hay nombres duplicados en tu equipo: $nombre", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    nombresCompaneros.add(nombre)
                }

                btnGuardar.isEnabled = false
                btnGuardar.text = "Guardando..."

                val docRef = if (inscripcionAEditar != null) {
                    db.collection("Inscripciones").document(inscripcionAEditar.idInscripcion)
                } else {
                    db.collection("Inscripciones").document()
                }

                // Verificamos nombres ocupados por otros equipos
                db.collection("Inscripciones")
                    .whereEqualTo("idTorneo", torneoSeleccionado.id)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val nombresOcupados = mutableSetOf<String>()

                        for (document in querySnapshot) {
                            if (inscripcionAEditar != null && document.id == inscripcionAEditar.idInscripcion) continue

                            val equipoOcupado = document.getString("nombreEquipo") ?: ""
                            nombresOcupados.add(equipoOcupado.trim().lowercase())
                        }

                        if (nombresOcupados.contains(nombreEquipoStr.lowercase())) {
                            Toast.makeText(requireContext(), "El equipo '$nombreEquipoStr' ya existe.", Toast.LENGTH_LONG).show()
                            btnGuardar.isEnabled = true
                            btnGuardar.text = if (inscripcionAEditar != null) "Actualizar Equipo" else "Confirmar Equipo"
                            return@addOnSuccessListener
                        }

                        val inscripcion = Inscripcion(
                            idInscripcion = docRef.id,
                            idTorneo = torneoSeleccionado.id,
                            idUsuario = idUsuarioActual,
                            esEquipo = true,
                            nombreEquipo = nombreEquipoStr,
                            integrantes = nombresCompaneros,
                            contactoLider = contactoLider,
                            pagoRealizado = inscripcionAEditar?.pagoRealizado ?: false
                        )

                        docRef.set(inscripcion)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "¡Equipo guardado!", Toast.LENGTH_SHORT).show()
                                DatosRepository.inscripcionAEditar = null
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
                        btnGuardar.isEnabled = true
                    }
            }
        }
    }
}