package com.cakocorps.administraciondetorneroselectronicos

object DatosRepository {

    val listaVideojuego = listOf(

        Videojuego(
            nombre = "Brawl start",
            dispositivos = listOf("Celular"),
            modalidades = listOf("Equipos", "solitario")
        ),
        Videojuego(
            nombre = "League of legend",
            dispositivos = listOf("Celular ","Pc"),
            modalidades = listOf("Equipos", "solitario")
        ),
        Videojuego(
            nombre = "Fifa",
            dispositivos = listOf("Play station 5"),
            modalidades = listOf("Equipos", "solitario")
        )
    )

    var torneoSeleccionado: Torneo? = null

}