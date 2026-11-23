package com.cakocorps.administraciondetorneroselectronicos

data class Partido(
    var idPartido: String = "",
    var idTorneo: String = "",
    var ronda: Int = 1,
    var idJugador1: String = "",
    var nombreJugador1: String = "Por definir",
    var idJugador2: String = "",
    var nombreJugador2: String = "Por definir",
    var idGanador: String = "",
    var nombreGanador: String = "",
    var numeroPartido: Int = 1,
    var esTercerLugar: Boolean = false,
    var fechaHora: Long = 0L

)