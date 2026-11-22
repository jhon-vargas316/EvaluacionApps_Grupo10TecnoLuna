package com.example.tecnoluna.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tecnoluna.model.data.local.AppDatabase
import com.example.tecnoluna.repository.SolicitudRepository
import com.example.tecnoluna.model.*
import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Controla el formulario de solicitud. Sirve para CREAR una solicitud nueva y para EDITAR una existente.
class NuevaSolicitudViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SolicitudRepository(AppDatabase.getDatabase(app))

    private val _ui = MutableStateFlow(SolicitudUiState())
    val ui: StateFlow<SolicitudUiState> = _ui.asStateFlow()

    // Cada vez que el usuario escribe, se guarda el texto y se borra el error de ese campo
    fun onTitulo(v: String) = _ui.update { it.copy(titulo = v.take(60), errores = it.errores.copy(titulo = null)) }
    fun onDescripcion(v: String) = _ui.update { it.copy(descripcion = v.take(500), errores = it.errores.copy(descripcion = null)) }
    fun onCategoria(v: Categoria) = _ui.update { it.copy(categoria = v, errores = it.errores.copy(categoria = null)) }
    fun onServicio(v: String) = _ui.update { it.copy(servicio = v.take(80)) }
    fun onArchivo(a: ArchivoAdjunto?) = _ui.update {
        it.copy(archivo = a, errores = it.errores.copy(archivo = a?.let { x -> Adjuntos.validar(x) }))
    }

    // Prepara el formulario para editar una solicitud existente
    fun cargarParaEditar(s: Solicitud) {
        _ui.value = SolicitudUiState(
            titulo = s.titulo, descripcion = s.descripcion, categoria = s.categoria,
            servicio = if (s.servicio == "No indicado") "" else s.servicio, codigoEdicion = s.codigo
        )
    }

    // Revisa todos los campos. Devuelve true si todo está correcto.
    private fun validar(): Boolean {
        val s = _ui.value
        val e = SolicitudErrores(
            titulo = when {
                s.titulo.isBlank() -> "El título es obligatorio"
                s.titulo.trim().length < 5 -> "Escribe al menos 5 caracteres"
                else -> null
            },
            descripcion = when {
                s.descripcion.isBlank() -> "Cuéntanos qué necesitas"
                s.descripcion.trim().length < 20 -> "Agrega más detalle (mínimo 20 caracteres)"
                else -> null
            },
            categoria = if (s.categoria == null) "Elige una categoría" else null,
            archivo = s.archivo?.let { Adjuntos.validar(it) }
        )
        _ui.update { it.copy(errores = e) }
        return !e.hayErrores
    }

    // Botón "Enviar solicitud" o "Guardar cambios"
    fun enviar(usuario: Usuario) {
        if (_ui.value.enviando || !validar()) return // si hay errores, se detiene aquí
        viewModelScope.launch {
            _ui.update { it.copy(enviando = true, errorGeneral = null) }
            delay(500) // pequeña espera para mostrar "Enviando…"
            val s = _ui.value
            val categoria = s.categoria!!
            val codigo = s.codigoEdicion

            if (codigo == null) {
                // CREAR
                val nueva = repo.crear(usuario, s.titulo, s.descripcion, categoria, s.servicio, s.archivo)
                _ui.update { it.copy(enviando = false, resultado = nueva) }
            } else {
                // EDITAR
                val error = repo.editar(codigo, usuario, s.titulo, s.descripcion, categoria, s.servicio)
                val editada = if (error == null) repo.obtener(codigo) else null
                _ui.update { it.copy(enviando = false, errorGeneral = error, resultado = editada) }
            }
        }
    }

    // Deja el formulario vacío otra vez
    fun limpiar() { _ui.value = SolicitudUiState() }
}
