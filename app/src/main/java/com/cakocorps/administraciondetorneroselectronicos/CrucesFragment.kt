package com.cakocorps.administraciondetorneroselectronicos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mazecube.tournamentbracket.TournamentBracketView
import com.mazecube.tournamentbracket.data.MatchData
import com.mazecube.tournamentbracket.data.MatchParticipant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class CrucesFragment : Fragment(R.layout.fragment_cruces) {

    private data class Competidor(val idUsuario: String, val nombre: String)

    private val db = FirebaseFirestore.getInstance()
    private val idUsuarioActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var idTorneo = ""
    private var esCreador = false
    private var yaInicio = false
    private var fechaInicio = ""
    private var inicioMillis = 0L
    private val MINUTOS_ENTRE_PARTIDOS = 0
    private var guardarPodioAlRecargar = false

    private var nombreTorneo = ""
    private var partidoTercerLugar: Partido? = null

    private var partidos: List<Partido> = emptyList()
    private val competidores = mutableMapOf<Int, Competidor>()

    //vistas
    private lateinit var tvEstado: TextView
    private lateinit var bracketView: TournamentBracketView
    private lateinit var btnSortear: Button


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val torneo = DatosRepository.torneoSeleccionado ?: return
        idTorneo = torneo.id
        esCreador = torneo.idCreador == idUsuarioActual
        fechaInicio = torneo.fechaCierre
        inicioMillis = convertirFechaAMillis(torneo.fechaCierre)
        yaInicio = System.currentTimeMillis() >= inicioMillis

        tvEstado = view.findViewById(R.id.tvEstadoCruces)
        bracketView = view.findViewById(R.id.tournamentBracketView)
        btnSortear = view.findViewById(R.id.btnSimular)

        nombreTorneo = torneo.nombre

        btnSortear.setOnClickListener { generarSorteo() }
        bracketView.onMatchClickListener = { rondaIdx, partidoIdx ->
            alTocarPartido(rondaIdx + 1, partidoIdx + 1)
        }

        bracketView.onThirdPlaceClickListener = {
            val t = partidoTercerLugar
            if (t != null) mostrarDetalleTercerLugar(t)
            else Toast.makeText(requireContext(), "Se define cuando terminen las semifinales", Toast.LENGTH_SHORT).show()
        }


        verificarEstadoTorneo()

    }

    fun verificarEstadoTorneo() {
        db.collection("Partidos")
            .whereEqualTo("idTorneo", idTorneo)
            .get()
            .addOnSuccessListener { partidoSnapchot ->

                if (partidoSnapchot.isEmpty) {
                    bracketView.visibility = View.GONE
                    tvEstado.visibility = View.VISIBLE

                    if (!yaInicio) {
                        btnSortear.visibility = View.GONE
                        tvEstado.text = "El torneo aún no inicia.\nFecha de inicio: $fechaInicio"
                    } else if (esCreador) {
                        tvEstado.text = "Inscripciones cerradas.\nYa puedes generar los cruces."
                        btnSortear.visibility = View.VISIBLE
                    } else {
                        btnSortear.visibility = View.GONE
                        tvEstado.text = "Esperando a que el creador realice el sorteo."
                    }
                } else {
                    btnSortear.visibility = View.GONE
                    bracketView.visibility = View.VISIBLE
                    reconstruirBracket(partidoSnapchot.toObjects(Partido::class.java))
                }
            }
    }

    fun generarSorteo() {
        btnSortear.isEnabled = false
        db.collection("Inscripciones")
            .whereEqualTo("idTorneo", idTorneo)
            .get()
            .addOnSuccessListener { resultado ->
                val lista = resultado.toObjects(Inscripcion::class.java).toMutableList()

                if (lista.size < 2) {
                    Toast.makeText(
                        requireContext(),
                        "Se necesitan al menos 2 inscritos para sortear",
                        Toast.LENGTH_SHORT
                    ).show()
                    btnSortear.isEnabled = true
                    return@addOnSuccessListener
                }

                lista.shuffle()
                crearPartidas(lista)
            }
            .addOnFailureListener {
                btnSortear.isEnabled = true
                Toast.makeText(requireContext(), "Error al leer inscritos", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun crearPartidas(lista: List<Inscripcion>) {
        competidores.clear()
        val jugadores = lista.mapIndexed { i, insc ->
            competidores[i + 1] = Competidor(insc.idUsuario, insc.nombreEquipo)
            MatchParticipant(i + 1, insc.nombreEquipo, null, null, null)
        }
        // La librería arma los cruces (con byes si hacen falta) y los guardamos tal cual
        val matches = bracketView.initParticipant(jugadores, isShuffle = false)
        guardarRonda(matches, 1)
    }

    private fun guardarRonda(matches: List<MatchData>, ronda: Int) {
        val batch = db.batch()

        matches.forEachIndexed { i, m ->
            val a = competidores[m.playerA.id] ?: return@forEachIndexed
            val b = m.playerB?.let { competidores[it.id] }
            val esBye = (b == null)

            // id predecible: si se guarda dos veces, se sobrescribe en vez de duplicar
            val docId = "${idTorneo}_${ronda}_${i + 1}"

            val partido = Partido(
                idPartido = docId,
                idTorneo = idTorneo,
                ronda = ronda,
                numeroPartido = i + 1,
                fechaHora = calcularHorario(ronda, i + 1),
                idJugador1 = a.idUsuario,
                nombreJugador1 = a.nombre,
                idJugador2 = b?.idUsuario ?: "",
                nombreJugador2 = b?.nombre ?: "Pasa Libre",
                idGanador = if (esBye) a.idUsuario else "",
                nombreGanador = if (esBye) a.nombre else ""
            )
            batch.set(db.collection("Partidos").document(docId), partido)
        }

        batch.commit()
            .addOnSuccessListener {
                if (ronda == 1) Toast.makeText(requireContext(), "¡Sorteo generado y guardado!", Toast.LENGTH_SHORT).show()
                verificarEstadoTorneo()
            }
            .addOnFailureListener {
                btnSortear.isEnabled = true
                Toast.makeText(requireContext(), "Problema al guardar partidas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reconstruirBracket(todos: List<Partido>) {
        // el tercer lugar se maneja aparte del resto de rondas
        partidoTercerLugar = todos.firstOrNull { it.esTercerLugar }
        val lista = todos.filter { !it.esTercerLugar }
        partidos = lista

        val porRonda = lista.groupBy { it.ronda }
            .mapValues { (_, v) -> v.sortedBy { it.numeroPartido } }

        val ronda1 = porRonda[1] ?: return

        competidores.clear()
        val participantes = mutableListOf<MatchParticipant>()
        var id = 1
        for (p in ronda1) {
            competidores[id] = Competidor(p.idJugador1, p.nombreJugador1)
            participantes.add(MatchParticipant(id, p.nombreJugador1, null, null, null))
            id++
            if (p.idJugador2.isNotEmpty()) {
                competidores[id] = Competidor(p.idJugador2, p.nombreJugador2)
                participantes.add(MatchParticipant(id, p.nombreJugador2, null, null, null))
                id++
            }
        }

        var matches = bracketView.initParticipant(participantes, isShuffle = false)
        var ronda = 1
        var finalPartido: Partido? = null

        while (true) {
            val guardados = porRonda[ronda].orEmpty()

            matches.forEachIndexed { i, m ->
                marcarResultado(m, guardados.firstOrNull { it.numeroPartido == i + 1 })
            }

            val completa = matches.indices.all { i ->
                guardados.firstOrNull { it.numeroPartido == i + 1 }?.idGanador?.isNotEmpty() == true
            }
            if (!completa) break

            val jugadoresRonda = matches.flatMap { listOfNotNull(it.playerA, it.playerB) }
            val siguiente = bracketView.updateMatchData(jugadoresRonda)

            if (siguiente.isEmpty()) {
                finalPartido = guardados.firstOrNull()
                break
            }

            ronda++
            matches = siguiente
            if (porRonda[ronda] == null && esCreador) {
                guardarRonda(siguiente, ronda)
            }
        }

        if (participantes.size >= 4) {
            val t = partidoTercerLugar
            if (t == null) {
                bracketView.setThirdPlace(null, null)
            } else {
                val a = MatchParticipant(9001, t.nombreJugador1, null, null, null)
                val b = MatchParticipant(9002, t.nombreJugador2, null, null, null)
                if (t.idGanador.isNotEmpty()) {
                    val ganaA = t.idGanador == t.idJugador1
                    a.isWinner = ganaA
                    a.point = if (ganaA) "1" else "0"
                    b.isWinner = !ganaA
                    b.point = if (ganaA) "0" else "1"
                }
                bracketView.setThirdPlace(a, b)
            }
        }

        bracketView.invalidate()

        // ¿Terminó el torneo? = final jugada y (si aplica) tercer lugar jugado
        val finalizado = finalPartido != null &&
                (participantes.size < 4 || partidoTercerLugar?.idGanador?.isNotEmpty() == true)

        // El podio se guarda una sola vez: justo después de que el creador registra un resultado
        if (guardarPodioAlRecargar) {
            guardarPodioAlRecargar = false
            val fp = finalPartido
            if (finalizado && esCreador && fp != null) guardarPodios(fp, partidoTercerLugar)
        }

        tvEstado.visibility = View.VISIBLE
        tvEstado.text = when {
            finalizado -> "Torneo finalizado"
            finalPartido != null -> "Final jugada. Falta el partido por el tercer lugar."
            else -> "Fase actual: ${nombreFase(matches.size * 2)}"
        }

        crearTercerLugarSiHaceFalta(porRonda)
    }

    private fun marcarResultado(m: MatchData, p: Partido?) {
        val ganador = p?.idGanador.orEmpty()

        fun aplicar(j: MatchParticipant?) {
            if (j == null) return
            if (ganador.isEmpty()) {
                j.isWinner = null
                j.point = null
            } else {
                val gana = competidores[j.id]?.idUsuario == ganador
                j.isWinner = gana
                // sin marcador real: 1-0 para el ganador (luego puedes poner el marcador de verdad)
                j.point = if (m.playerB == null) null else if (gana) "1" else "0"
            }
        }
        aplicar(m.playerA)
        aplicar(m.playerB)
    }

    private fun alTocarPartido(ronda: Int, numeroPartido: Int) {
        val partido = partidos.firstOrNull { it.ronda == ronda && it.numeroPartido == numeroPartido } ?: return
        mostrarDetalle(partido)
    }

    private fun mostrarDetalle(partido: Partido) {
        val esBye = partido.idJugador2.isEmpty()
        val rondaCerrada = partidos.any { it.ronda == partido.ronda + 1 }
        val fase = nombreFase(partidos.count { it.ronda == partido.ronda } * 2)

        val mensaje = buildString {
            append("Fase: $fase\n")
            if (esBye) {
                append("${partido.nombreJugador1} pasa libre a la siguiente ronda.")
            } else {
                append("Horario: ${formatearHorario(partido.fechaHora)}\n")
                append(
                    if (partido.idGanador.isEmpty()) "Ganador: pendiente"
                    else "Ganador: ${partido.nombreGanador}"
                )
            }
        }

        val dialogo = AlertDialog.Builder(requireContext())
            .setTitle(if (esBye) partido.nombreJugador1 else "${partido.nombreJugador1} vs ${partido.nombreJugador2}")
            .setMessage(mensaje)

        if (esCreador && !esBye) {
            if (!rondaCerrada) {
                dialogo
                    .setPositiveButton("Ganó ${partido.nombreJugador1}") { _, _ ->
                        guardarGanador(partido, partido.idJugador1, partido.nombreJugador1)
                    }
                    .setNegativeButton("Ganó ${partido.nombreJugador2}") { _, _ ->
                        guardarGanador(partido, partido.idJugador2, partido.nombreJugador2)
                    }
                    .setNeutralButton("Horario") { _, _ -> programarHorario(partido) }
            } else {
                dialogo
                    .setPositiveButton("Horario") { _, _ -> programarHorario(partido) }
                    .setNegativeButton("Cerrar", null)
            }
        } else {
            dialogo.setPositiveButton("Cerrar", null)
        }

        dialogo.show()
    }

    private fun guardarGanador(partido: Partido, idGanador: String, nombreGanador: String) {
        db.collection("Partidos").document(partido.idPartido)
            .update(mapOf("idGanador" to idGanador, "nombreGanador" to nombreGanador))
            .addOnSuccessListener {
                guardarPodioAlRecargar = true       // al recargar, si el torneo ya terminó, se guarda el podio
                verificarEstadoTorneo()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "No se pudo guardar el ganador", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarPodios(finalPartido: Partido, tercer: Partido?) {
        val batch = db.batch()
        val ahora = System.currentTimeMillis()

        fun agregar(puesto: Int, titulo: String, idJugador: String, nombre: String) {
            val docId = "${idTorneo}_$puesto"           // id fijo: si se corrige un resultado, se sobrescribe
            batch.set(
                db.collection("Podios").document(docId),
                Podio(docId, idTorneo, nombreTorneo, puesto, titulo, idJugador, nombre, ahora)
            )
        }

        val ganaJ1 = finalPartido.idGanador == finalPartido.idJugador1
        agregar(1, "Campeón", finalPartido.idGanador, finalPartido.nombreGanador)
        agregar(
            2, "Subcampeón",
            if (ganaJ1) finalPartido.idJugador2 else finalPartido.idJugador1,
            if (ganaJ1) finalPartido.nombreJugador2 else finalPartido.nombreJugador1
        )
        if (tercer != null) agregar(3, "Tercer lugar", tercer.idGanador, tercer.nombreGanador)

        // El torneo pasa a "finalizado" (útil para tu vista nueva y para los listados)
        batch.update(db.collection("Torneos").document(idTorneo), "estado", "finalizado")

        batch.commit().addOnFailureListener {
            Toast.makeText(requireContext(), "No se pudo guardar el podio", Toast.LENGTH_SHORT).show()
        }
    }


    // Crea el partido por el tercer lugar cuando las dos semifinales ya tienen ganador
    private fun crearTercerLugarSiHaceFalta(porRonda: Map<Int, List<Partido>>) {
        if (!esCreador || partidoTercerLugar != null) return

        // Semifinales = la ronda de 2 partidos reales (sin "Pasa Libre")
        val semis = porRonda.entries.firstOrNull { (_, v) ->
            v.size == 2 && v.all { it.idJugador2.isNotEmpty() }
        } ?: return
        if (!semis.value.all { it.idGanador.isNotEmpty() }) return

        val perdedores = semis.value.map { p ->
            if (p.idGanador == p.idJugador1) p.idJugador2 to p.nombreJugador2
            else p.idJugador1 to p.nombreJugador1
        }

        val docId = "${idTorneo}_tercer_lugar"
        val tercer = Partido(
            idPartido = docId,
            idTorneo = idTorneo,
            ronda = semis.key + 1,                       // se juega el mismo día que la final
            numeroPartido = 0,
            esTercerLugar = true,
            fechaHora = calcularHorario(semis.key + 1, 1),
            idJugador1 = perdedores[0].first,
            nombreJugador1 = perdedores[0].second,
            idJugador2 = perdedores[1].first,
            nombreJugador2 = perdedores[1].second
        )
        db.collection("Partidos").document(docId).set(tercer)
            .addOnSuccessListener { verificarEstadoTorneo() }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "No se pudo crear el partido por el tercer lugar", Toast.LENGTH_SHORT).show()
            }
    }



    private fun mostrarDetalleTercerLugar(p: Partido) {
        val mensaje = "Horario: ${formatearHorario(p.fechaHora)}\n" +
                (if (p.idGanador.isEmpty()) "Ganador: pendiente" else "Ganador: ${p.nombreGanador}")

        val dialogo = AlertDialog.Builder(requireContext())
            .setTitle("Tercer lugar: ${p.nombreJugador1} vs ${p.nombreJugador2}")
            .setMessage(mensaje)

        if (esCreador) {
            dialogo
                .setPositiveButton("Ganó ${p.nombreJugador1}") { _, _ ->
                    guardarGanador(p, p.idJugador1, p.nombreJugador1)
                }
                .setNegativeButton("Ganó ${p.nombreJugador2}") { _, _ ->
                    guardarGanador(p, p.idJugador2, p.nombreJugador2)
                }
                .setNeutralButton("Horario") { _, _ -> programarHorario(p) }
        } else {
            dialogo.setPositiveButton("Cerrar", null)
        }
        dialogo.show()
    }

    private fun nombreFase(espaciosEnRonda: Int) = when (espaciosEnRonda) {
        2 -> "Final"
        4 -> "Semifinales"
        8 -> "Cuartos de final"
        16 -> "Octavos de final"
        else -> "Ronda de $espaciosEnRonda"
    }

    private fun calcularHorario(ronda: Int, numeroPartido: Int): Long {
        if (inicioMillis == 0L) return 0L
        val cal = Calendar.getInstance().apply {
            timeInMillis = inicioMillis
            add(Calendar.DAY_OF_YEAR, ronda - 1)
            add(Calendar.MINUTE, (numeroPartido - 1) * MINUTOS_ENTRE_PARTIDOS)
        }
        return cal.timeInMillis
    }

    private fun formatearHorario(millis: Long): String =
        if (millis == 0L) "Sin programar"
        else SimpleDateFormat("EEE dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

    // Corrección manual del admin: elegir día y hora
    private fun programarHorario(partido: Partido) {
        val cal = Calendar.getInstance()
        if (partido.fechaHora != 0L) cal.timeInMillis = partido.fechaHora

        DatePickerDialog(requireContext(), { _, anio, mes, dia ->
            TimePickerDialog(requireContext(), { _, hora, minuto ->
                cal.set(anio, mes, dia, hora, minuto, 0)
                cal.set(Calendar.MILLISECOND, 0)
                db.collection("Partidos").document(partido.idPartido)
                    .update("fechaHora", cal.timeInMillis)
                    .addOnSuccessListener { verificarEstadoTorneo() }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "No se pudo guardar el horario", Toast.LENGTH_SHORT).show()
                    }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }


    private fun convertirFechaAMillis(fechaStr: String): Long {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fecha = formato.parse(fechaStr)
            fecha?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

}