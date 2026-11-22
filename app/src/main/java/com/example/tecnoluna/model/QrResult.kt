package com.example.tecnoluna.model

import com.example.tecnoluna.model.data.local.Solicitud

// Resultado de leer un QR: qué texto traía, qué código se encontró y qué pasó
data class QrResult(
    val content: String,                 // texto completo del QR
    val codigo: String?,                 // código de solicitud encontrado (ej: SOL-1001)
    val resultado: ResultadoQr,
    val solicitud: Solicitud? = null     // solo si se encontró y la persona puede verla
)
