package com.cakocorps.administraciondetorneroselectronicos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
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
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.String.format
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos

class formularioFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()


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

        //encargado de al tocar un juego cargar los otros spinner con datos de ese juego
        spinnerJuegos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val juegoSeleccionado = listaVideojuegos[position]

                //rellena los spinner solo con los datos del juego seleccionado
                spinnerDispositivos.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, juegoSeleccionado.dispositivos)
                spinnerModalidad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, juegoSeleccionado.modalidades)

                //este arregla el error si los datos ya no existen al editar
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
                //dependiendo de que formato se selecciona rellena de una forma u otra
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

        //si variable tiene un torneo guardado significa que alguien quiere editar
        val torneoAEditar = DatosRepository.torneoSeleccionado
        //aca se sabe
        if (torneoAEditar != null) {
            //cambiamos logica rellenamos campos con datos del torneo
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
            val nombre = nombreTorneo.text.toString().trim()
            val txtInscripcion = inscripcion.text.toString().trim()
            val txtCierre = cierreInscripcion.text.toString().trim()
            val txtInicio = inicioTorneo.text.toString().trim()
            val txtCosto = costoEntrada.text.toString().trim()
            val txtPremio = recompensa.text.toString().trim()
            val txtReglas = reglas.text.toString().trim()
            val numeroCupos = cupos.text.toString().toIntOrNull() ?:16

            val juego = spinnerJuegos.selectedItem?.toString() ?: ""
            val dispositivo = spinnerDispositivos.selectedItem?.toString() ?: ""
            val modalidad = spinnerModalidad.selectedItem?.toString() ?: ""
            val formatoElegido = spinnerFormato.selectedItem?.toString() ?: ""
            val tipoEntrada = entrada.selectedItem?.toString() ?: ""

            val requiereCosto = tipoEntrada == "De Pago"


            if (nombre.isEmpty() || txtInscripcion.isEmpty() || txtCierre.isEmpty() || txtInicio.isEmpty() ||
                txtPremio.isEmpty() || txtReglas.isEmpty() || (requiereCosto && txtCosto.isEmpty() )){

                Toast.makeText(requireContext(), "Debe rellenar todos los campos",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val costoNum = txtCosto.toDoubleOrNull() ?: 0.0

            //valida costo negativo
            if (costoNum < 0){
                Toast.makeText(requireContext(), "El costo debe ser un numero positivo.",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //id usuario creador
            val miUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "Desconocido"

            //para evitar multiples presionados
            boton.isEnabled = false
            boton.text = "Guardando..."

            val docRef = if (torneoAEditar != null) {
                db.collection("Torneos").document(torneoAEditar.id)
            } else {
                db.collection("Torneos").document()
            }


            val actualizadoTorneo = Torneo(
                id = docRef.id,
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
                costoEntrada = costoNum,
                recompensa = txtPremio,
                reglas = txtReglas,
                idCreador = torneoAEditar?.idCreador ?: miUid,
                estado = "abierto"
                )

             //si torneo se encuentra almacenado significa que se busca editar
            docRef.set(actualizadoTorneo)
                .addOnSuccessListener {
                    val msj = if (torneoAEditar != null) "Torneo actualizado" else "¡Torneo creado con éxito!"
                    Toast.makeText(requireContext(), msj, Toast.LENGTH_SHORT).show()

                    // Limpiar selección previa
                    DatosRepository.torneoSeleccionado = null
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al guardar en la nube", Toast.LENGTH_SHORT).show()
                    boton.isEnabled = true
                    boton.text = if (torneoAEditar != null) "Actualizar Torneo" else "Guardar"
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