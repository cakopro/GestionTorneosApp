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
    var listaTorneos = mutableListOf(
        Torneo(
            nombre = "Copa Mundial ARAM",
            juego = "League of Legends",
            dispositivo = "PC",
            modalidad = "1vs1",
            formato = "Eliminación Directa",
            cupos = 16,
            fechaInscripcion = "25/10/2026 18:00",
            fechaCierre = "30/10/2026 23:59",
            fechaInicio = "01/11/2026 15:00",
            tipoEntrada = "Gratuita",
            costoEntrada = 0.0,
            recompensa = "1000 RP",
            reglas = "Partidas a 1 victoria. Prohibido faltar el respeto.",
            idCreador = "U001",
            inscritos = mutableListOf()
        )
    )
    val listaUsuarios = listOf(
        Usuario(
            id = "U001",
            nombre = "cako",
            contrasena = "123",
            correo = "cako@gmail.com"
        ),
        Usuario(
            id = "U002",
            nombre = "benja",
            contrasena = "123",
            correo = "benja@gmail.com"
        )
    )
    var usuarioActual: Usuario? = null

    var torneoSeleccionado: Torneo? = null

}