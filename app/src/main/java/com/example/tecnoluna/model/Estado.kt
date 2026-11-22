package com.example.tecnoluna.model

// Etapas por las que pasa una solicitud. "descripcion" se muestra al usuario para que entienda qué significa.
enum class Estado(val etiqueta: String, val descripcion: String) {
    RECIBIDA("Recibida", "Recibimos la solicitud y pronto será revisada."),
    EN_REVISION("En revisión", "Estamos revisando los datos de la solicitud."),
    PENDIENTE_INFO("Pendiente de información", "Necesitamos más información para poder continuar."),
    ASIGNADA("Asignada", "Ya hay un responsable para esta solicitud."),
    EN_PROCESO("En proceso", "Estamos trabajando en esta solicitud."),
    RESUELTA("Resuelta", "Hay una solución. Falta la confirmación del cliente."),
    CERRADA("Cerrada", "La solicitud terminó."),
    CANCELADA("Cancelada", "La solicitud fue cancelada.");

    // Una solicitud cerrada o cancelada ya no se puede modificar
    val esFinal: Boolean get() = this == CERRADA || this == CANCELADA

    // Solo las solicitudes activas pueden quedar "atrasadas"
    val estaActiva: Boolean get() = this != RESUELTA && !esFinal
}
