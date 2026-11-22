package com.example.tecnoluna.model.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tecnoluna.model.Categoria
import com.example.tecnoluna.model.Comentario
import com.example.tecnoluna.model.Estado
import com.example.tecnoluna.model.Perfil
import com.example.tecnoluna.model.Prioridad

// Tabla 2 de 2: "solicitudes". Cada propiedad es una columna.
// Los comentarios, el historial y la evidencia viven dentro de la propia solicitud (no hay tablas aparte).
@Entity(tableName = "solicitudes")
data class Solicitud(
    @PrimaryKey val codigo: String,       // código único, por ejemplo SOL-1001

    // ---- Datos que escribe el cliente ----
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val servicio: String,
    val evidencia: String? = null,        // nombre(s) del archivo adjunto, separados por ", "
    val comentario: String = "",          // comentarios e historial (ver Comentario.kt)

    // ---- Datos de control para que funcionen los tres perfiles ----
    val clienteId: String,                // quién la creó (ve solo las suyas)
    val clienteNombre: String,
    val empresa: String,
    val prioridad: Prioridad,             // la define el administrador
    val estado: Estado,
    val responsableId: String? = null,    // técnico asignado (ve solo las suyas)
    val responsableNombre: String? = null,
    val creada: Long,
    val actualizada: Long,
    val fechaLimite: Long,
    val fechaResolucion: Long? = null
) {
    // Está atrasada si sigue activa y ya pasó la fecha límite
    fun estaAtrasada(ahora: Long = System.currentTimeMillis()): Boolean =
        estado.estaActiva && fechaLimite < ahora

    // Nombres de los archivos adjuntos
    fun listaEvidencias(): List<String> =
        evidencia?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()

    // Todas las líneas guardadas en la columna "comentario", de la más antigua a la más nueva
    fun lineas(): List<Comentario> = Comentario.leer(comentario).sortedBy { it.fecha }

    // El cliente confirmó la solución (la última vez que se cerró fue por él)
    fun confirmadaPorCliente(): Boolean =
        estado == Estado.CERRADA &&
            Comentario.leer(comentario).lastOrNull { it.estadoNuevo == Estado.CERRADA }?.perfil == Perfil.CLIENTE

    // Motivo escrito al cancelar
    fun motivoCancelacion(): String? =
        if (estado != Estado.CANCELADA) null
        else Comentario.leer(comentario).lastOrNull { it.estadoNuevo == Estado.CANCELADA }?.texto?.ifBlank { null }

    // Texto listo para compartir por WhatsApp (solo código, título, categoría y enlace)
    fun mensajeWhatsApp(): String = buildString {
        appendLine("Hola, registré una solicitud de soporte:")
        appendLine("Código: $codigo")
        appendLine("Título: $titulo")
        appendLine("Categoría: ${categoria.etiqueta}")
        append("Seguimiento: https://tecnoluna.cl/seguimiento/$codigo")
    }
}
