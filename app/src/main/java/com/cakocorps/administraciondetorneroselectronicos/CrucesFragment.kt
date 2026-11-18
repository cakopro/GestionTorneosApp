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

        btnSortear.setOnClickListener { generarSorteo() }
        bracketView.onMatchClickListener = { rondaIdx, partidoIdx ->
            alTocarPartido(rondaIdx + 1, partidoIdx + 1)
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
                verificarEstadoTorneo()          // recarga y entra por el camino de siempre
            }
            .addOnFailureListener {
                btnSortear.isEnabled = true
                Toast.makeText(requireContext(), "Problema al guardar partidas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reconstruirBracket(lista: List<Partido>) {
        partidos = lista
        val porRonda = lista.groupBy { it.ronda }
            .mapValues { (_, v) -> v.sortedBy { it.numeroPartido } }

        val ronda1 = porRonda[1] ?: return

        // 1. Lista de la ronda 1, con ids 1, 2, 3... y el diccionario de quién es quién
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
        var campeon: String? = null

        // 2. Ronda por ronda: marcar ganadores y, si la ronda está completa, avanzar
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

            if (siguiente.isEmpty()) {                       // era la final
                campeon = guardados.firstOrNull()?.nombreGanador
                break
            }

            ronda++
            matches = siguiente
            // La ronda nueva aún no existe en Firestore: la crea el creador
            if (porRonda[ronda] == null && esCreador) {
                guardarRonda(siguiente, ronda)
            }
        }

        bracketView.invalidate()

        tvEstado.visibility = View.VISIBLE
        tvEstado.text = if (campeon != null) {
            "Campeón: $campeon"
        } else {
            "Fase actual: ${nombreFase(matches.size * 2)}"
        }
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
                // la ronda ya está cerrada: el ganador no se puede cambiar, pero el horario sí
                dialogo
                    .setPositiveButton("Horario") { _, _ -> programarHorario(partido) }
                    .setNegativeButton("Cerrar", null)
            }
        } else {
            dialogo.setPositiveButton("Cerrar", null)     // usuario normal: solo lectura
        }

        dialogo.show()
    }

    private fun guardarGanador(partido: Partido, idGanador: String, nombreGanador: String) {
        db.collection("Partidos").document(partido.idPartido)
            .update(mapOf("idGanador" to idGanador, "nombreGanador" to nombreGanador))
            .addOnSuccessListener { verificarEstadoTorneo() }   // recarga: marca, y avanza si la ronda se completó
            .addOnFailureListener {
                Toast.makeText(requireContext(), "No se pudo guardar el ganador", Toast.LENGTH_SHORT).show()
            }
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