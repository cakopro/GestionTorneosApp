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

class DetalleTorneoFragment : Fragment() {

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

        val tilNombreEquipo = view.findViewById<View>(R.id.tilNombreEquipo)
        val tilContacto = view.findViewById<View>(R.id.tilContacto)
        val etNombreEquipo = view.findViewById<TextInputEditText>(R.id.etNombreEquipo)
        val etContacto = view.findViewById<TextInputEditText>(R.id.etContacto)

        val layoutCreador = view.findViewById<View>(R.id.layoutBotonesCreador)
        val layoutInscripcion = view.findViewById<View>(R.id.layoutBotonesInscripcion)

        val btnEditar = view.findViewById<Button>(R.id.btnEditar)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminar)
        val btnInscribirme = view.findViewById<Button>(R.id.btnInscribirme)
        val btnSalirTorneo = view.findViewById<Button>(R.id.btnSalirTorneo)

        val torneo = DatosRepository.torneoSeleccionado
        val usuario = DatosRepository.usuarioActual
        val idUsuarioActual = usuario?.id ?: ""

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
            val escreador = torneo.idCreador == idUsuarioActual
            val inscripcionExistente = torneo.inscritos.find { it.idUsuario == idUsuarioActual }

            //mostrar botones para borrar y actualizar.
            if (escreador){

                layoutCreador.visibility = View.VISIBLE
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
                    DatosRepository.listaTorneos.remove(torneo)
                    Toast.makeText(requireContext(), "Torneo eliminado correctamente", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }else{
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

                    if (equipo.isEmpty() || contacto.isEmpty()) {
                        Toast.makeText(requireContext(), "Completa los datos de inscripción", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val nuevaInscri = Inscripcion(idUsuario = idUsuarioActual,
                        nombreEquipo = equipo,
                        nombreParticipante = usuario?.nombre ?: "Anonimo",
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