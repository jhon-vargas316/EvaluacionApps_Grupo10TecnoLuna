package com.example.tecnoluna.model

// Números del panel de resumen del administrador
data class Resumen(
    val total: Int,
    val porEstado: Map<Estado, Int>,
    val porPrioridad: Map<Prioridad, Int>,
    val porResponsable: Map<String, Int>,
    val pendientes: Int,
    val atrasadas: Int
)
