package com.example.tecnoluna.model

// Resultados posibles al leer un código QR
enum class ResultadoQr {
    ENCONTRADA,       // trae el código de una solicitud y la persona puede verla
    SIN_ACCESO,       // la solicitud existe, pero es de otra persona
    NO_ENCONTRADA,    // tiene formato de solicitud, pero no existe
    NO_ES_SOLICITUD   // trae otra cosa (por ejemplo, una página web)
}
