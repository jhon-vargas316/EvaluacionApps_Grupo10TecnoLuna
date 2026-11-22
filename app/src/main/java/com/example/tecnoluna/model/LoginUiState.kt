package com.example.tecnoluna.model

// Estado de la pantalla de login
data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    val errores: LoginErrores = LoginErrores()
)
