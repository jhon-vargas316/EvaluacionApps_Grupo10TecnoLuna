package com.example.tecnoluna.model

// Prioridad y plazo máximo de atención (en días)
enum class Prioridad(val etiqueta: String, val diasPlazo: Int) {
    BAJA("Baja", 7), MEDIA("Media", 3), ALTA("Alta", 2), URGENTE("Urgente", 1)
}
