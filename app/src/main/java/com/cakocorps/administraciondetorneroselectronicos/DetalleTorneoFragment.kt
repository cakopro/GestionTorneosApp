package com.cakocorps.administraciondetorneroselectronicos

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleTorneoFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

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

        val layoutCreador = view.findViewById<View>(R.id.layoutBotonesCreador)
        val layoutInscripcion = view.findViewById<View>(R.id.layoutBotonesInscripcion)

        val btnEditar = view.findViewById<Button>(R.id.btnEditar)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminar)
        val btnInscribirme = view.findViewById<Button>(R.id.btnInscribirme)
        val btnSalirTorneo = view.findViewById<Button>(R.id.btnSalirTorneo)
        val ivTrofeoDetalle = view.findViewById<ImageView?>(R.id.ivTrofeoDetalle)

        val torneo = DatosRepository.torneoSeleccionado
        val idUsuarioActual = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        firebaseAnalytics = Firebase.analytics

        if (torneo != null) {
            tvNombre.text = torneo.nombre
            tvInfo.text = "Cargando información..."

            if (ivTrofeoDetalle != null && torneo.trofeoUrl.isNotBlank()) {
                val bitmap = decodificarBase64ABitmap(torneo.trofeoUrl)
                if (bitmap != null) {
                    ivTrofeoDetalle.setImageBitmap(bitmap)
                    ivTrofeoDetalle.visibility = View.VISIBLE
                }
            }

            if (torneo.idCreador == idUsuarioActual) {
                layoutCreador.visibility = View.VISIBLE
                layoutInscripcion.visibility = View.GONE
                configurarCreador(btnEditar, btnEliminar)
            }


            // Consultar cuántos inscritos hay y si el usuario actual está inscrito
            db.collection("Inscripciones")
                .whereEqualTo("idTorneo", torneo.id)
                .get()
                .addOnSuccessListener { querySnapshot ->

                    firebaseAnalytics.logEvent("listado_consultado") {
                        param("tipo_entidad", "Inscripcion")
                    }

                    val cantidadInscritos = querySnapshot.size()
                    var miInscripcion: Inscripcion? = null

                    for (document in querySnapshot) {
                        if (document.getString("idUsuario") == idUsuarioActual) {
                            miInscripcion = document.toObject(Inscripcion::class.java)
                            miInscripcion.idInscripcion = document.id
                            break
                        }
                    }

                    // Cargar texto informativo
                    tvInfo.text = "Juego: ${torneo.juego} (${torneo.dispositivo})\n" +
                            "Modalidad: ${torneo.modalidad} | Formato: ${torneo.formato}\n" +
                            "Cupos: $cantidadInscritos / ${torneo.cupos}\n\n" +
                            "Apertura: ${torneo.fechaInscripcion}\n" +
                            "Cierre: ${torneo.fechaCierre}\n\n" +
                            "Reglas:\n${torneo.reglas}"

                    val esCreador = torneo.idCreador == idUsuarioActual

                    if (esCreador) {
                        layoutCreador.visibility = View.VISIBLE
                        layoutInscripcion.visibility = View.GONE


                    }else {
                        layoutCreador.visibility = View.GONE
                        layoutInscripcion.visibility = View.VISIBLE

                        val tiempoActual = System.currentTimeMillis()
                        val millisApertura = convertirFechaAMillis(torneo.fechaInscripcion)
                        val millisCierre = convertirFechaAMillis(torneo.fechaCierre)

                        if (miInscripcion != null) {
                            btnSalirTorneo.visibility = View.VISIBLE

                            if (tiempoActual > millisCierre) {
                                btnSalirTorneo.isEnabled = false
                                btnSalirTorneo.text = "Inscripciones Cerradas"
                                btnInscribirme.isEnabled = false
                                btnInscribirme.text = "Torneo Cerrado"
                            } else {
                                btnSalirTorneo.isEnabled = true
                                btnSalirTorneo.text = "Salir del Torneo"
                                btnInscribirme.isEnabled = true
                                btnInscribirme.text = "Actualizar mis datos"
                            }

                            btnSalirTorneo.setOnClickListener {
                                AlertDialog.Builder(context)
                                    .setTitle("Salir del Torneo")
                                    .setMessage("¿Está seguro de que desea salir del torneo?")
                                    .setPositiveButton("Salir") { dialog, _ ->
                                        db.collection("Inscripciones").document(miInscripcion.idInscripcion).delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Has salido del torneo", Toast.LENGTH_SHORT).show()
                                                firebaseAnalytics.logEvent("registro_eliminado") {
                                                    param("tipo_entidad", "Inscripcion")
                                                }
                                                // Refrescar contenedor
                                                requireActivity().supportFragmentManager.beginTransaction()
                                                    .replace(R.id.fragment_container, TorneoContenedorFragment())
                                                    .commit()
                                            }
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                    .create()
                                    .show()
                            }
                        } else {
                            btnSalirTorneo.visibility = View.GONE
                            btnInscribirme.visibility = View.VISIBLE

                            if (tiempoActual < millisApertura) {
                                btnInscribirme.isEnabled = false
                                btnInscribirme.text = "Aún no disponible"
                            } else if (tiempoActual > millisCierre) {
                                btnInscribirme.isEnabled = false
                                btnInscribirme.text = "Inscripciones Cerradas"
                            } else if (cantidadInscritos >= torneo.cupos) {
                                btnInscribirme.isEnabled = false
                                btnInscribirme.text = "Torneo Lleno"
                            } else {
                                btnInscribirme.isEnabled = true
                                btnInscribirme.text = "Inscribirse al Torneo"
                            }
                        }
                    }

                    btnInscribirme.setOnClickListener {
                        if (torneo.modalidad == "Equipos") {
                            DatosRepository.inscripcionAEditar = miInscripcion
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, InscripcionFormulario())
                                .addToBackStack(null)
                                .commit()
                        } else {
                            btnInscribirme.isEnabled = false
                            btnInscribirme.text = "Inscribiendo..."

                            val nombreUsuarioReal = DatosRepository.usuarioActual?.nombre ?: "Usuario"
                            val correoUsuario = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""

                            val docRef = db.collection("Inscripciones").document()
                            val nuevaInscripcion = Inscripcion(
                                idInscripcion = docRef.id,
                                idTorneo = torneo.id,
                                idUsuario = idUsuarioActual,
                                esEquipo = false,
                                nombreEquipo = nombreUsuarioReal,
                                integrantes = emptyList(),
                                contactoLider = correoUsuario,
                                pagoRealizado = false
                            )

                            docRef.set(nuevaInscripcion)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "¡Inscrito correctamente!", Toast.LENGTH_SHORT).show()
                                    firebaseAnalytics.logEvent("registro_creado") { param("tipo_entidad", "Inscripcion") }

                                    // Refrescar contenedor para ver estado actualizado
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container, TorneoContenedorFragment())
                                        .commit()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Error al inscribirse", Toast.LENGTH_SHORT).show()
                                    btnInscribirme.isEnabled = true
                                    btnInscribirme.text = "Inscribirse al Torneo"
                                }
                        }
                    }
                }
        }
    }

    private fun configurarCreador(btnEditar: Button, btnEliminar: Button) {
    val torneo = DatosRepository.torneoSeleccionado ?: return
    val yaInicio = System.currentTimeMillis() >= convertirFechaAMillis(torneo.fechaCierre)

    btnEditar.setOnClickListener {
        comprobarSorteo(torneo.id) { sorteado ->
            when {
                sorteado -> Toast.makeText(requireContext(), "No se puede editar: el torneo ya tiene sorteo", Toast.LENGTH_SHORT).show()
                yaInicio -> Toast.makeText(requireContext(), "No se puede editar: el torneo ya inició", Toast.LENGTH_SHORT).show()
                else -> requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, formularioFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }



        btnEliminar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Torneo")
                .setMessage("Se eliminarán también todas las inscripciones y los partidos del torneo. Esta acción no se puede deshacer. ¿Continuar?")
                .setPositiveButton("Eliminar") { _, _ ->
                    eliminarTorneoCompleto(
                        torneo.id,
                        onExito = {
                            Toast.makeText(requireContext(), "Torneo eliminado correctamente", Toast.LENGTH_SHORT).show()
                            firebaseAnalytics.logEvent("registro_eliminado") {
                                param("tipo_entidad", "Torneo")
                            }
                            requireActivity().supportFragmentManager.popBackStack()
                        },
                        onError = {
                            Toast.makeText(requireContext(), "No se pudo eliminar el torneo", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
}

    private fun eliminarTorneoCompleto(idTorneo: String, onExito: () -> Unit, onError: () -> Unit) {
        db.collection("Inscripciones").whereEqualTo("idTorneo", idTorneo).get()
            .addOnSuccessListener { inscripciones ->
                db.collection("Partidos").whereEqualTo("idTorneo", idTorneo).get()
                    .addOnSuccessListener { partidos ->
                        db.collection("Podios").whereEqualTo("idTorneo", idTorneo).get()
                            .addOnSuccessListener { podios ->
                                val batch = db.batch()
                                inscripciones.forEach { batch.delete(it.reference) }
                                partidos.forEach { batch.delete(it.reference) }
                                podios.forEach { batch.delete(it.reference) }
                                batch.delete(db.collection("Torneos").document(idTorneo))
                                batch.commit()
                                    .addOnSuccessListener { onExito() }
                                    .addOnFailureListener { onError() }
                            }
                            .addOnFailureListener { onError() }
                    }
                    .addOnFailureListener { onError() }
            }
            .addOnFailureListener { onError() }
    }

    private fun comprobarSorteo(idTorneo: String, onResultado: (Boolean) -> Unit) {
        db.collection("Partidos")
        .whereEqualTo("idTorneo", idTorneo)
        .limit(1)
        .get()
        .addOnSuccessListener { onResultado(!it.isEmpty) }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "No se pudo verificar el estado del torneo", Toast.LENGTH_SHORT).show()
        }
}

    private fun decodificarBase64ABitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }


    private fun convertirFechaAMillis(fechaStr: String): Long {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fecha = formato.parse(fechaStr)
            fecha?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}