package com.example.tecnoluna.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tecnoluna.model.LoginErrores
import com.example.tecnoluna.model.LoginUiState
import com.example.tecnoluna.model.data.local.AppDatabase
import com.example.tecnoluna.model.data.local.Usuario
import com.example.tecnoluna.repository.SolicitudRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Maneja el inicio y cierre de sesión
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SolicitudRepository(AppDatabase.getDatabase(app))

    // Usuario con la sesión iniciada (null = nadie)
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    // Estado de los campos del login (con sus errores)
    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    // Cuentas para el ingreso rápido (vienen de la tabla usuarios)
    val cuentas: StateFlow<List<Usuario>> =
        repo.usuarios.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Al abrir la app se prepara la base de datos (la primera vez carga los datos iniciales)
    init { viewModelScope.launch { repo.inicializar() } }

    // Cada vez que se escribe, se guarda el texto y se borra el error de ese campo
    fun onCorreo(v: String) = _login.update { it.copy(correo = v, errores = it.errores.copy(correo = null, general = null)) }
    fun onClave(v: String) = _login.update { it.copy(clave = v, errores = it.errores.copy(clave = null, general = null)) }

    // Rellena correo y contraseña con la cuenta elegida
    fun usarCuenta(u: Usuario) = _login.update { it.copy(correo = u.correo, clave = u.clave, errores = LoginErrores()) }

    // Revisa los campos. Devuelve true si están completos.
    private fun validar(): Boolean {
        val s = _login.value
        val errores = LoginErrores(
            correo = if (s.correo.isBlank()) "Escribe tu correo" else null,
            clave = if (s.clave.isBlank()) "Escribe tu contraseña" else null
        )
        _login.update { it.copy(errores = errores) }
        return !errores.hayErrores
    }

    fun ingresar() {
        if (_login.value.cargando || !validar()) return
        val s = _login.value
        viewModelScope.launch {
            _login.update { it.copy(cargando = true, errores = LoginErrores()) }
            delay(500) // pequeña espera para mostrar el círculo de carga
            val u = repo.login(s.correo, s.clave)
            if (u == null) {
                _login.update { it.copy(cargando = false, errores = LoginErrores(general = "Correo o contraseña incorrectos")) }
            } else {
                _login.value = LoginUiState()
                _usuario.value = u // al cambiar, la pantalla navega sola al menú
            }
        }
    }

    fun cerrarSesion() {
        _usuario.value = null
        _login.value = LoginUiState()
    }
}
