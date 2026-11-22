package com.example.tecnoluna.model

// Archivo que se puede elegir como evidencia (todavía no adjuntado). En la tabla solo se guarda su nombre.
data class ArchivoAdjunto(
    val nombre: String,
    val tipo: String,
    val tamanoKb: Int,
    val descripcion: String
)

// Archivos que se pueden adjuntar y sus límites
object Adjuntos {
    const val MAX_KB = 2048                     // máximo 2 MB
    val FORMATOS = setOf("png", "jpg", "pdf")   // formatos permitidos

    val disponibles = listOf(
        ArchivoAdjunto("captura_error.png", "png", 320, "Captura de pantalla del problema"),
        ArchivoAdjunto("cotizacion.pdf", "pdf", 540, "Documento con la cotización"),
        ArchivoAdjunto("logo_empresa.jpg", "jpg", 150, "Logo de la empresa"),
        ArchivoAdjunto("respaldo.zip", "zip", 5200, "Archivo comprimido")
    )

    // Devuelve un mensaje de error si el archivo no cumple las reglas (o null si está bien)
    fun validar(a: ArchivoAdjunto): String? = when {
        a.tipo !in FORMATOS -> "El formato .${a.tipo} no está permitido (solo PNG, JPG o PDF)"
        a.tamanoKb > MAX_KB -> "El archivo supera el máximo de 2 MB"
        else -> null
    }
}
