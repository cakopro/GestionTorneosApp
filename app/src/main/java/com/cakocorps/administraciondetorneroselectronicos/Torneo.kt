package com.cakocorps.administraciondetorneroselectronicos

data class Torneo(
    val nombre: String,
    val juego: String,
    val dispositivo: String,
    val modalidad: String,
    val cupos: Int,
    val fechaInscripcion: String,
    val formato: String,
    val fechaCierre: String,
    val fechaInicio: String,
    val tipoEntrada: String,
    val costoEntrada: Double,
    val recompensa: String,
    val reglas: String,
    val idCreador: String,
    val inscritos: MutableList<Inscripcion> = mutableListOf()
)
