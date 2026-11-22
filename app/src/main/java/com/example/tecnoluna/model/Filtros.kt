package com.example.tecnoluna.model

// Filtros de la lista de solicitudes
data class Filtros(
    val query: String = "",
    val estado: Estado? = null,
    val prioridad: Prioridad? = null,
    val clienteId: String? = null,
    val responsableId: String? = null,
    val soloAtrasadas: Boolean = false
)
