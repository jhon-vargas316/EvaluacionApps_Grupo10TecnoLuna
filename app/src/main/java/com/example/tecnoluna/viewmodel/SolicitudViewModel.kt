package com.example.tecnoluna.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tecnoluna.model.data.local.AppDatabase
import com.example.tecnoluna.repository.SolicitudRepository
import com.example.tecnoluna.model.*
import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Conecta las pantallas de solicitudes con el repositorio (Room)
class SolicitudViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SolicitudRepository(AppDatabase.getDatabase(app))

    // Convierte un Flow de Room en un StateFlow que las pantallas pueden observar
    private fun <T> Flow<T>.comoEstado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, inicial)

    // ---------- Listas que las pantallas observan ----------
    val solicitudes = repo.solicitudes.comoEstado(emptyList())
    val tecnicos = repo.usuarios.map { l -> l.filter { it.perfil == Perfil.TECNICO } }.comoEstado(emptyList())
    val clientes = repo.usuarios.map { l -> l.filter { it.perfil == Perfil.CLIENTE } }.comoEstado(emptyList())
    fun detalle(codigo: String) = repo.detalle(codigo)

    // ---------- Avisos (se calculan desde las solicitudes, no hay tabla aparte) ----------
    // Hora hasta la cual el usuario ya vio sus avisos
    private val _vistoHasta = MutableStateFlow(0L)
    val vistoHasta: StateFlow<Long> = _vistoHasta.asStateFlow()
    fun marcarLeidas() { _vistoHasta.value = System.currentTimeMillis() }

    // Cambios hechos por OTRAS personas en las solicitudes que este usuario puede ver (los más nuevos primero)
    fun calcularAvisos(usuario: Usuario, todas: List<Solicitud>, vistoHasta: Long): List<Aviso> =
        todas.filter { Reglas.puedeVer(usuario, it) }
            .flatMap { s ->
                s.lineas()
                    .filter { it.esHistorial && it.autor != usuario.nombre && (usuario.perfil != Perfil.CLIENTE || !it.interno) }
                    .map { Aviso("${s.codigo}-${it.fecha}", "${s.codigo}: ${it.accion} (${it.autor})", it.fecha, it.fecha <= vistoHasta) }
            }
            .sortedByDescending { it.fecha }
            .take(30)

    // ---------- Filtros de la lista ----------
    private val _filtros = MutableStateFlow(Filtros())
    val filtros: StateFlow<Filtros> = _filtros.asStateFlow()
    fun actualizarFiltros(cambio: (Filtros) -> Filtros) = _filtros.update(cambio)
    fun limpiarFiltros() { _filtros.value = Filtros() }

    // ---------- Mensajes cortos que se muestran abajo de la pantalla ----------
    private val _mensaje = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensaje: SharedFlow<String> = _mensaje.asSharedFlow()

    // Ejecuta una acción del repositorio. Muestra el error, o el mensaje "ok" y llama a onOk si salió bien.
    private fun ejecutar(ok: String, onOk: () -> Unit, op: suspend () -> String?) {
        viewModelScope.launch {
            val error = op()
            _mensaje.tryEmit(error ?: ok)
            if (error == null) onOk()
        }
    }

    // ---------- Consultas ----------
    // Deja solo las solicitudes que el usuario puede ver y que cumplen los filtros
    fun filtrar(usuario: Usuario, todas: List<Solicitud>, f: Filtros): List<Solicitud> {
        val q = f.query.trim().lowercase()
        return todas.filter { Reglas.puedeVer(usuario, it) }
            .filter { f.estado == null || it.estado == f.estado }
            .filter { f.prioridad == null || it.prioridad == f.prioridad }
            .filter { f.clienteId == null || it.clienteId == f.clienteId }
            .filter { f.responsableId == null || it.responsableId == f.responsableId }
            .filter { !f.soloAtrasadas || it.estaAtrasada() }
            .filter {
                q.isEmpty() || it.codigo.lowercase().contains(q) || it.titulo.lowercase().contains(q) ||
                    it.clienteNombre.lowercase().contains(q) || it.empresa.lowercase().contains(q)
            }
    }

    // Cuenta las solicitudes por estado, prioridad y responsable
    fun calcularResumen(todas: List<Solicitud>) = Resumen(
        total = todas.size,
        porEstado = Estado.entries.associateWith { e -> todas.count { it.estado == e } },
        porPrioridad = Prioridad.entries.associateWith { p -> todas.count { it.prioridad == p } },
        porResponsable = todas.groupingBy { it.responsableNombre ?: "Sin asignar" }.eachCount(),
        pendientes = todas.count {
            it.estado in listOf(Estado.RECIBIDA, Estado.EN_REVISION, Estado.PENDIENTE_INFO, Estado.ASIGNADA)
        },
        atrasadas = todas.count { it.estaAtrasada() }
    )

    // ---------- Acciones (onOk se ejecuta solo si la acción salió bien) ----------
    fun eliminar(codigo: String, actor: Usuario, onOk: () -> Unit = {}) =
        ejecutar("Solicitud eliminada", onOk) { repo.eliminar(codigo, actor) }

    fun clasificar(codigo: String, actor: Usuario, c: Categoria) =
        ejecutar("Categoría actualizada", {}) { repo.clasificar(codigo, actor, c) }

    fun definirPrioridad(codigo: String, actor: Usuario, p: Prioridad) =
        ejecutar("Prioridad actualizada", {}) { repo.definirPrioridad(codigo, actor, p) }

    fun asignar(codigo: String, actor: Usuario, tecnicoId: String) =
        ejecutar("Responsable asignado", {}) { repo.asignar(codigo, actor, tecnicoId) }

    fun cambiarEstado(codigo: String, actor: Usuario, nuevo: Estado, comentario: String, onOk: () -> Unit = {}) =
        ejecutar("Estado actualizado a ${nuevo.etiqueta}", onOk) { repo.cambiarEstado(codigo, actor, nuevo, comentario) }

    fun comentar(codigo: String, actor: Usuario, texto: String, interno: Boolean, onOk: () -> Unit = {}) =
        ejecutar("Comentario agregado", onOk) { repo.comentar(codigo, actor, texto, interno) }

    fun registrarAvance(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?, onOk: () -> Unit = {}) =
        ejecutar("Avance registrado", onOk) { repo.registrarAvance(codigo, actor, texto, archivo) }

    fun responderInfo(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?, onOk: () -> Unit = {}) =
        ejecutar("Información enviada", onOk) { repo.responderInfo(codigo, actor, texto, archivo) }

    fun confirmar(codigo: String, actor: Usuario) =
        ejecutar("¡Gracias! La solicitud quedó cerrada", {}) { repo.confirmarConformidad(codigo, actor) }

    fun reabrir(codigo: String, actor: Usuario, motivo: String) =
        ejecutar("Solicitud reabierta", {}) { repo.reabrir(codigo, actor, motivo) }

    fun registrarEnvioWhatsApp(codigo: String, actor: Usuario) =
        ejecutar("Referencia compartida por WhatsApp", {}) { repo.registrarEnvioWhatsApp(codigo, actor) }

    fun registrarSolicitudDeWhatsApp() {
        viewModelScope.launch {
            val s = repo.registrarDesdeWhatsApp()
            _mensaje.tryEmit("Llegó la solicitud ${s.codigo} por WhatsApp")
        }
    }
}
