package com.example.tecnoluna.model

// Una línea de la columna "comentario" de la tabla solicitudes. No es una tabla.
// Hay dos clases de líneas:
//  - Comentario normal (accion == null): un mensaje escrito por alguien.
//  - Registro de historial (accion != null): qué pasó, quién lo hizo y cuándo (cambios de estado, asignaciones, etc.).
// Toda la lista se guarda como un solo texto dentro de la solicitud.
data class Comentario(
    val fecha: Long,
    val autor: String,
    val perfil: Perfil,
    val texto: String,
    val interno: Boolean = false,        // true = el cliente no lo puede ver
    val accion: String? = null,
    val estadoAnterior: Estado? = null,
    val estadoNuevo: Estado? = null,
    val archivo: String? = null          // nombre de la evidencia asociada, si corresponde
) {
    val esHistorial: Boolean get() = accion != null

    // Convierte la línea en texto para guardarla en la columna
    fun aTexto(): String = listOf(
        fecha.toString(), perfil.name, autor.limpio(), if (interno) "1" else "0",
        accion.orEmpty().limpio(), estadoAnterior?.name.orEmpty(), estadoNuevo?.name.orEmpty(),
        archivo.orEmpty().limpio(), texto.limpio()
    ).joinToString(SEP_CAMPO)

    companion object {
        private const val SEP_CAMPO = "\u001F"       // separa los datos de una línea
        private const val SEP_LINEA = "\u001E"       // separa una línea de otra
        private const val CAMPOS = 9

        // Evita que el texto escrito por el usuario rompa el formato
        private fun String.limpio() = replace(SEP_CAMPO, " ").replace(SEP_LINEA, " ")

        // Lee todas las líneas guardadas en la columna
        fun leer(columna: String): List<Comentario> =
            if (columna.isBlank()) emptyList() else columna.split(SEP_LINEA).mapNotNull { desdeTexto(it) }

        // Agrega una línea al final de lo que ya había en la columna
        fun agregar(columna: String, nuevo: Comentario): String =
            if (columna.isBlank()) nuevo.aTexto() else columna + SEP_LINEA + nuevo.aTexto()

        private fun desdeTexto(linea: String): Comentario? {
            val p = linea.split(SEP_CAMPO, limit = CAMPOS)
            if (p.size < CAMPOS) return null
            val fecha = p[0].toLongOrNull() ?: return null
            val perfil = Perfil.entries.firstOrNull { it.name == p[1] } ?: return null
            return Comentario(
                fecha = fecha, autor = p[2], perfil = perfil, texto = p[8], interno = p[3] == "1",
                accion = p[4].ifEmpty { null },
                estadoAnterior = Estado.entries.firstOrNull { it.name == p[5] },
                estadoNuevo = Estado.entries.firstOrNull { it.name == p[6] },
                archivo = p[7].ifEmpty { null }
            )
        }
    }
}
