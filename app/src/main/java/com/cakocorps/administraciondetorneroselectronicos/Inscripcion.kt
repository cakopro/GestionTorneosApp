package com.cakocorps.administraciondetorneroselectronicos

data class Inscripcion(
    val idUsuario: String,
    val nombreEquipo: String? = null,
    val nombreParticipante: String,
    val nombreCapitan: String? = null,
    val contactoLider: String,
    val pagoRealizado: Boolean = false
)
