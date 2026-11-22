package com.example.tecnoluna.model

// Aviso que se muestra en la pestaña "Avisos". No es una tabla: se calcula a partir
// de los cambios que otras personas hicieron en las solicitudes del usuario.
data class Aviso(
    val clave: String,       // identificador único (código + fecha)
    val mensaje: String,
    val fecha: Long,
    val leido: Boolean
)
