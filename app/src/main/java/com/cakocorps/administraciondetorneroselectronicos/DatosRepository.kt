package com.cakocorps.administraciondetorneroselectronicos

object DatosRepository {

    val listaVideojuego = listOf(

        Videojuego(
            nombre = "Brawl start",
            dispositivos = listOf("Celular"),
            modalidades = listOf("Equipos", "solitario"),
            jugadoresPorEquipo = 3
        ),
        Videojuego(
            nombre = "League of legend",
            dispositivos = listOf("Celular ","Pc"),
            modalidades = listOf("Equipos", "solitario"),
            jugadoresPorEquipo = 5
        ),
        Videojuego(
            nombre = "Fifa",
            dispositivos = listOf("Play station 5"),
            modalidades = listOf("Equipos", "solitario") ,
            jugadoresPorEquipo = 2
        )
    )

    var torneoSeleccionado: Torneo? = null
    var inscripcionAEditar: Inscripcion? = null

    var usuarioActual: Usuario? = null

}