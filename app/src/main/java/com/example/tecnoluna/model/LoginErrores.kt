package com.example.tecnoluna.model

// Errores posibles del login (null = el campo está bien)
data class LoginErrores(
    val correo: String? = null,
    val clave: String? = null,
    val general: String? = null     // credenciales incorrectas
) {
    val hayErrores: Boolean get() = listOfNotNull(correo, clave, general).isNotEmpty()
}
