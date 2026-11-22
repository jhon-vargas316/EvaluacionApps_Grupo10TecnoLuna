package com.example.tecnoluna.model

// Errores del formulario de solicitud (null = el campo está bien)
data class SolicitudErrores(
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val archivo: String? = null
) {
    val hayErrores: Boolean get() = listOfNotNull(titulo, descripcion, categoria, archivo).isNotEmpty()
}
