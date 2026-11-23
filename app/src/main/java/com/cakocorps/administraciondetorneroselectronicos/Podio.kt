package com.cakocorps.administraciondetorneroselectronicos

data class Podio(
    var idPodio: String = "",
    var idTorneo: String = "",
    var nombreTorneo: String = "",
    var puesto: Int = 0,                 // 1, 2 o 3
    var titulo: String = "",             // "Campeón", "Subcampeón" o "Tercer lugar"
    var idJugador: String = "",          // idUsuario, igual que en Partido
    var nombreJugador: String = "",
    var fecha: Long = 0L
)