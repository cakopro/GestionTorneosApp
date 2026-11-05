package com.cakocorps.administraciondetorneroselectronicos

data class Torneo(
    var id: String = "",
    val nombre: String = "",
    val juego: String = "",
    val dispositivo: String = "",
    val modalidad: String = "",
    val cupos: Int = 0,
    val fechaInscripcion: String = "",
    val formato: String = "",
    val fechaCierre: String = "",
    val fechaInicio: String = "",
    val tipoEntrada: String = "",
    val reglas: String = "",
    val idCreador: String = "",
    val estado: String = "abierto"
)
