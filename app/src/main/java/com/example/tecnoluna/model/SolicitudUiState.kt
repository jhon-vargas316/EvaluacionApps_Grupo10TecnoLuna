package com.example.tecnoluna.model

import com.example.tecnoluna.model.data.local.Solicitud

// Estado del formulario de solicitud (sirve para crear y para editar). No es una tabla:
// solo guarda lo que se está escribiendo.
data class SolicitudUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val servicio: String = "",
    val archivo: ArchivoAdjunto? = null,
    val errores: SolicitudErrores = SolicitudErrores(),
    val errorGeneral: String? = null,
    val enviando: Boolean = false,
    val codigoEdicion: String? = null,   // si tiene valor, se está editando esa solicitud
    val resultado: Solicitud? = null     // solicitud creada o editada (cuando termina bien)
)
