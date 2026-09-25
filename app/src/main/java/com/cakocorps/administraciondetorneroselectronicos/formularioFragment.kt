package com.cakocorps.administraciondetorneroselectronicos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import android.widget.ArrayAdapter
import java.lang.String.format
import java.util.Calendar
import java.util.Locale

class formularioFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_formulario, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //primer card view
        val nombreTorneo = view.findViewById<TextInputEditText>(R.id.txtNombreTorneo)
        val spinnerDispositivos = view.findViewById<Spinner>(R.id.spinnerDisposi)
        val spinnerJuegos= view.findViewById<Spinner>(R.id.spiJuegos)
        val spinnerModalidad = view.findViewById<Spinner>(R.id.spiModalidad)
        val spinnerFormato = view.findViewById<Spinner>(R.id.spiFormato)
        val cupos = view.findViewById<TextView>(R.id.cupos)

        //segundo card view
        val inscripcion = view.findViewById<TextInputEditText>(R.id.edtInscripcion)
        val cierreInscripcion = view.findViewById<TextInputEditText>(R.id.edtCierre)
        val inicioTorneo = view.findViewById<TextInputEditText>(R.id.edtInicio)

        //tercer card dinero
        val entrada = view.findViewById<Spinner>(R.id.spnEntrada)
        val costoEntrada = view.findViewById<TextInputEditText>(R.id.etCostoEntrada)
        val recompensa = view.findViewById<TextInputEditText>(R.id.etPremio)
        val tilCostoEntrada = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCostoEntrada)

        // ultima card y boton
        val formato = view.findViewById<TextView>(R.id.formato)
        val reglas = view.findViewById<TextInputEditText>(R.id.etReglas)
        val boton = view.findViewById<Button>(R.id.btnGuardar)

        //listas
        val listaTipoEntrada = arrayOf("Gratuita", "De Pago")
        val listaFormato = arrayOf("Play offs", "Fase de grupos")
        val listaVideojuegos = DatosRepository.listaVideojuego
        val nombresJuegos = listaVideojuegos.map{it.nombre}

        //spinner
        val adaptadorEntrada = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listaTipoEntrada)
        entrada.adapter = adaptadorEntrada

        val adaptadorFormato = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listaFormato)
        spinnerFormato.adapter = adaptadorFormato

        val adaptadorJuegos = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nombresJuegos)
        spinnerJuegos.adapter = adaptadorJuegos

        var esPrimeraCargaJuegos = true

        spinnerJuegos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val juegoSeleccionado = listaVideojuegos[position]

                spinnerDispositivos.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, juegoSeleccionado.dispositivos)
                spinnerModalidad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, juegoSeleccionado.modalidades)

                val torneo = DatosRepository.torneoSeleccionado
                if (torneo != null && esPrimeraCargaJuegos) {
                    val posDisp = juegoSeleccionado.dispositivos.indexOf(torneo.dispositivo)
                    if (posDisp >= 0) spinnerDispositivos.setSelection(posDisp)

                    val posMod = juegoSeleccionado.modalidades.indexOf(torneo.modalidad)
                    if (posMod >= 0) spinnerModalidad.setSelection(posMod)

                    esPrimeraCargaJuegos = false
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // formato selecciona y indicar cupos
        spinnerFormato.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val formatoSeleccionado = listaFormato[position]

                if (formatoSeleccionado == "Play offs"){
                    cupos.text = "16"
                    formato.text = "El formato de play offs se trata de eliminacion directa con 16 cupos disponibles"
                }else if (formatoSeleccionado == "Fase de grupos"){
                    cupos.text = "32"
                    formato.text = "se Divide en grupos todos contra todos y avancan los que tengan mas puntos a una fase de play offs."
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //entrada gratis -> no debe cobrar por entrar
        entrada.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listaTipoEntrada[position] == "Gratuita"){
                    costoEntrada.isEnabled = false
                    costoEntrada.setText("")
                    tilCostoEntrada.hint = "Entrada Gratis"
                }else{
                    costoEntrada.isEnabled = true
                    tilCostoEntrada.hint = "Costo de entrada por persona/equipo:"
                }
            }override fun onNothingSelected(parent: AdapterView<*>?) {}

        }

        //fechas
        inscripcion.isFocusable = false
        cierreInscripcion.isFocusable = false
        inicioTorneo.isFocusable = false

        inscripcion.setOnClickListener { mostrarSelectorFechaYHora(inscripcion) }
        cierreInscripcion.setOnClickListener { mostrarSelectorFechaYHora(cierreInscripcion) }
        inicioTorneo.setOnClickListener { mostrarSelectorFechaYHora(inicioTorneo) }

        val torneoAEditar = DatosRepository.torneoSeleccionado
        if (torneoAEditar != null) {
            boton.text = "Actualizar Torneo"

            nombreTorneo.setText(torneoAEditar.nombre)
            inscripcion.setText(torneoAEditar.fechaInscripcion)
            cierreInscripcion.setText(torneoAEditar.fechaCierre)
            inicioTorneo.setText(torneoAEditar.fechaInicio)
            cupos.text = torneoAEditar.cupos.toString()
            if (torneoAEditar.costoEntrada > 0) costoEntrada.setText(torneoAEditar.costoEntrada.toString())
            recompensa.setText(torneoAEditar.recompensa)
            reglas.setText(torneoAEditar.reglas)

            val posEntrada = listaTipoEntrada.indexOf(torneoAEditar.tipoEntrada)
            if (posEntrada >= 0) entrada.setSelection(posEntrada)

            val posFormato = listaFormato.indexOf(torneoAEditar.formato)
            if (posFormato >= 0) spinnerFormato.setSelection(posFormato)

            val posJuego = nombresJuegos.indexOf(torneoAEditar.juego)
            if (posJuego >= 0) spinnerJuegos.setSelection(posJuego)
        }


        boton.setOnClickListener {
            val nombre = nombreTorneo.text.toString()
            val txtInscripcion = inscripcion.text.toString()
            val txtCierre = cierreInscripcion.text.toString()
            val txtInicio = inicioTorneo.text.toString()
            val txtCosto = costoEntrada.text.toString()
            val txtPremio = recompensa.text.toString()
            val txtReglas = reglas.text.toString()
            val numeroCupos = cupos.text.toString().toIntOrNull() ?:16

            val juego = spinnerJuegos.selectedItem?.toString() ?: ""
            val dispositivo = spinnerDispositivos.selectedItem?.toString() ?: ""
            val modalidad = spinnerModalidad.selectedItem?.toString() ?: ""
            val formatoElegido = spinnerFormato.selectedItem?.toString() ?: ""
            val tipoEntrada = entrada.selectedItem?.toString() ?: ""

            val inscritosActuales = torneoAEditar?.inscritos ?: mutableListOf()

            val actualizadoTorneo = Torneo(
                nombre = nombre,
                juego = juego,
                dispositivo = dispositivo,
                modalidad = modalidad,
                formato = formatoElegido,
                cupos = numeroCupos,
                fechaInscripcion = txtInscripcion,
                fechaCierre = txtCierre,
                fechaInicio = txtInicio,
                tipoEntrada = tipoEntrada,
                costoEntrada = txtCosto.toDoubleOrNull() ?: 0.0,
                recompensa = txtPremio,
                reglas = txtReglas,
                idCreador = torneoAEditar?.idCreador ?: DatosRepository.usuarioActual?.id ?: "UsuarioDesconocido",
                inscritos = inscritosActuales
                )

            if (torneoAEditar != null) {

                val index = DatosRepository.listaTorneos.lastIndexOf(torneoAEditar)
                if (index != -1) {

                    DatosRepository.listaTorneos[index] = actualizadoTorneo
                    requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ListaTorneosFragment())
                        .commit()
                }


            }else{
                    DatosRepository.listaTorneos.add(actualizadoTorneo)
                    android.widget.Toast.makeText(requireContext(), "¡Torneo creado con éxito!", android.widget.Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }



        }



    }
    private fun mostrarSelectorFechaYHora(campoDeTexto: TextInputEditText) {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, year, monthOfYear, dayOfMonth ->

            val hora = calendario.get(Calendar.HOUR_OF_DAY)
            val minuto = calendario.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val fechaHora = format(
                    Locale.getDefault(),
                    "%02d/%02d/%d %02d:%02d",
                    dayOfMonth,
                    monthOfYear + 1,
                    year,
                    hourOfDay,
                    minute
                )
                campoDeTexto.setText(fechaHora)
            }, hora, minuto, true)

            timePicker.show()
        }, anio, mes, dia)

        datePicker.show()
    }


}