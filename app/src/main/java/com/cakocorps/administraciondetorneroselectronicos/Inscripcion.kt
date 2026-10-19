package com.cakocorps.administraciondetorneroselectronicos

data class Inscripcion(
    var idInscripcion: String = "",
    var idTorneo: String = "",
    var idUsuario: String = "",


    var esEquipo: Boolean = false,
    var nombreEquipo: String = "",
    var integrantes: List<String> = emptyList(),
    var contactoLider: String = "",
    var pagoRealizado: Boolean = false

)
