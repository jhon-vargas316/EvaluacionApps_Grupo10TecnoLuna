package com.example.tecnoluna.repository

import androidx.room.withTransaction
import com.example.tecnoluna.model.*
import com.example.tecnoluna.model.data.local.AppDatabase
import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Aquí se leen y guardan los datos con Room (tablas "usuarios" y "solicitudes").
// Los ViewModels le piden cosas; las pantallas nunca lo usan directamente.
// Las operaciones devuelven un texto de error, o null si todo salió bien.
class SolicitudRepository(private val db: AppDatabase) {

    private val dao = db.solicitudDao()
    private val usuarioDao = db.usuarioDao()

    // ---------- Lo que las pantallas observan (Flow avisa cuando la tabla cambia) ----------
    val solicitudes = dao.observarTodas()
    val usuarios = usuarioDao.observarTodos()
    fun detalle(codigo: String) = dao.observar(codigo)
    suspend fun obtener(codigo: String) = dao.obtener(codigo)

    // ---------- Inicio de sesión ----------
    suspend fun login(correo: String, clave: String): Usuario? {
        inicializar()
        return usuarioDao.login(correo.trim(), clave)
    }

    // Si la base de datos está vacía (primera vez que se abre la app), carga los datos iniciales
    suspend fun inicializar() = candado.withLock {
        if (usuarioDao.contar() == 0) cargarDatosIniciales()
    }

    // ---------- CREATE: crear solicitud ----------
    suspend fun crear(
        cliente: Usuario, titulo: String, descripcion: String, categoria: Categoria,
        servicio: String, archivo: ArchivoAdjunto?, origen: String = "App TecnoLuna"
    ): Solicitud = db.withTransaction {
        val t = ahora()
        val codigo = "SOL-${dao.ultimoNumero() + 1}"   // código único
        val registro = Comentario(
            t, cliente.nombre, cliente.perfil, "Origen: $origen",
            accion = "Solicitud registrada", estadoNuevo = Estado.RECIBIDA, archivo = archivo?.nombre
        )
        val solicitud = Solicitud(
            codigo = codigo, titulo = titulo.trim(), descripcion = descripcion.trim(), categoria = categoria,
            servicio = servicio.limpiarServicio(), evidencia = archivo?.nombre, comentario = registro.aTexto(),
            clienteId = cliente.id, clienteNombre = cliente.nombre, empresa = cliente.empresa ?: "Sin empresa",
            prioridad = Prioridad.MEDIA, estado = Estado.RECIBIDA,   // siempre parte en "Recibida"
            creada = t, actualizada = t, fechaLimite = t + Prioridad.MEDIA.diasPlazo * DIA
        )
        dao.insertar(solicitud)
        solicitud
    }

    // Registra una solicitud como si hubiera llegado por WhatsApp
    suspend fun registrarDesdeWhatsApp(): Solicitud {
        val cliente = usuarioDao.porPerfil(Perfil.CLIENTE).random()
        val (titulo, descripcion, categoria) = listOf(
            Triple("Cambio de horario en sitio web", "Piden actualizar el horario de atención de la portada.", Categoria.SOLICITUD_CAMBIO),
            Triple("No puedo acceder al panel de hosting", "El acceso al panel muestra un error al ingresar.", Categoria.ACCESO_CONFIG),
            Triple("Consulta por avance de campaña", "Consultan cómo va la campaña de redes sociales.", Categoria.MARKETING_DIGITAL)
        ).random()
        return crear(cliente, titulo, descripcion, categoria, "Sitio web", null, origen = "WhatsApp")
    }

    // ---------- UPDATE: editar los datos de una solicitud ----------
    suspend fun editar(
        codigo: String, actor: Usuario, titulo: String, descripcion: String, categoria: Categoria, servicio: String
    ): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.puedeEditar(actor, s)) return "No puedes editar esta solicitud"
        guardar(
            s, s.copy(titulo = titulo.trim(), descripcion = descripcion.trim(), categoria = categoria, servicio = servicio.limpiarServicio()),
            actor, "Datos de la solicitud editados"
        )
        return null
    }

    // ---------- DELETE: eliminar una solicitud (sus comentarios e historial van dentro, así que se borran con ella) ----------
    suspend fun eliminar(codigo: String, actor: Usuario): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.puedeEliminar(actor, s)) return "No puedes eliminar esta solicitud"
        dao.eliminar(s)
        return null
    }

    // ---------- Acciones del administrador ----------
    suspend fun clasificar(codigo: String, actor: Usuario, categoria: Categoria) = comoAdmin(codigo, actor) { s ->
        if (s.categoria != categoria) {
            guardar(s, s.copy(categoria = categoria), actor, "Categoría cambiada de ${s.categoria.etiqueta} a ${categoria.etiqueta}")
        }
    }

    suspend fun definirPrioridad(codigo: String, actor: Usuario, prioridad: Prioridad) = comoAdmin(codigo, actor) { s ->
        if (s.prioridad != prioridad) {
            // Al cambiar la prioridad también cambia la fecha límite
            guardar(
                s, s.copy(prioridad = prioridad, fechaLimite = s.creada + prioridad.diasPlazo * DIA), actor,
                "Prioridad cambiada de ${s.prioridad.etiqueta} a ${prioridad.etiqueta}"
            )
        }
    }

    suspend fun asignar(codigo: String, actor: Usuario, tecnicoId: String): String? {
        val tecnico = usuarioDao.porPerfil(Perfil.TECNICO).firstOrNull { it.id == tecnicoId } ?: return "Técnico no válido"
        return comoAdmin(codigo, actor) { s ->
            if (s.responsableId != tecnico.id) {
                val accion = if (s.responsableId == null) "Asignada a ${tecnico.nombre}"
                else "Reasignada de ${s.responsableNombre} a ${tecnico.nombre}"
                // Si estaba recién recibida, pasa a "Asignada"
                val estado = if (s.estado == Estado.RECIBIDA || s.estado == Estado.EN_REVISION) Estado.ASIGNADA else s.estado
                guardar(s, s.copy(responsableId = tecnico.id, responsableNombre = tecnico.nombre, estado = estado), actor, accion)
            }
        }
    }

    // ---------- Cambiar el estado (administrador o técnico) ----------
    suspend fun cambiarEstado(codigo: String, actor: Usuario, nuevo: Estado, comentario: String): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (nuevo !in Reglas.transiciones(actor, s)) return "Tu perfil no puede pasar la solicitud a \"${nuevo.etiqueta}\""
        // Algunos cambios necesitan una explicación escrita
        if (Reglas.requiereComentario(nuevo) && comentario.isBlank()) {
            return when (nuevo) {
                Estado.PENDIENTE_INFO -> "Escribe qué información adicional se necesita"
                Estado.RESUELTA -> "Escribe la solución que se realizó"
                else -> "Escribe el motivo de la cancelación"
            }
        }
        guardar(
            s, s.copy(estado = nuevo, fechaResolucion = if (nuevo == Estado.RESUELTA) ahora() else s.fechaResolucion),
            actor, "Cambio de estado a ${nuevo.etiqueta}", texto = comentario.trim()
        )
        return null
    }

    // ---------- Comentarios y avances ----------
    suspend fun comentar(codigo: String, actor: Usuario, texto: String, interno: Boolean): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.puedeVer(actor, s)) return "No tienes acceso a esta solicitud"
        if (texto.isBlank()) return "Escribe un comentario"
        if (actor.perfil == Perfil.CLIENTE && (interno || s.estado.esFinal)) return "No puedes comentar en este estado"
        guardar(
            s, s, actor, if (interno) "Comentario interno registrado" else "Comentario de seguimiento",
            texto = if (interno) "" else texto.trim(),    // los internos no muestran su texto en el historial
            interno = interno, mensaje = texto.trim()
        )
        return null
    }

    suspend fun registrarAvance(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (actor.perfil != Perfil.TECNICO || s.responsableId != actor.id) return "Solo el técnico asignado puede registrar avances"
        if (s.estado != Estado.ASIGNADA && s.estado != Estado.EN_PROCESO) return "No se pueden registrar avances en estado \"${s.estado.etiqueta}\""
        if (texto.isBlank()) return "Describe el avance realizado"
        archivo?.let { Adjuntos.validar(it) }?.let { return it }
        guardar(s, s, actor, "Avance registrado", texto = texto.trim(), mensaje = texto.trim(), evidencia = archivo?.nombre)
        return null
    }

    // ---------- Acciones del cliente ----------
    // El cliente responde cuando le piden más información
    suspend fun responderInfo(codigo: String, actor: Usuario, texto: String, archivo: ArchivoAdjunto?): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.esTitular(actor, s)) return "Solo el cliente titular puede responder"
        if (s.estado != Estado.PENDIENTE_INFO) return "La solicitud no está esperando información"
        if (texto.isBlank()) return "Escribe tu respuesta"
        archivo?.let { Adjuntos.validar(it) }?.let { return it }
        // Si ya tiene responsable vuelve a "En proceso"; si no, a "En revisión"
        val destino = if (s.responsableId != null) Estado.EN_PROCESO else Estado.EN_REVISION
        guardar(
            s, s.copy(estado = destino), actor, "Cliente aportó información adicional",
            texto = texto.trim(), mensaje = texto.trim(), evidencia = archivo?.nombre
        )
        return null
    }

    // El cliente acepta la solución y la solicitud se cierra
    suspend fun confirmarConformidad(codigo: String, actor: Usuario): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.esTitular(actor, s)) return "Solo el cliente titular puede confirmar"
        if (s.estado != Estado.RESUELTA) return "Solo se puede confirmar una solicitud resuelta"
        guardar(s, s.copy(estado = Estado.CERRADA), actor, "Cliente confirmó conformidad", texto = "Solución aceptada")
        return null
    }

    // El cliente pide volver a abrir una solicitud resuelta o cerrada
    suspend fun reabrir(codigo: String, actor: Usuario, motivo: String): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.esTitular(actor, s)) return "Solo el cliente titular puede reabrir"
        if (s.estado != Estado.RESUELTA && s.estado != Estado.CERRADA) return "Solo se reabren solicitudes resueltas o cerradas"
        if (motivo.isBlank()) return "Escribe el motivo de la reapertura"
        guardar(
            s, s.copy(estado = Estado.EN_REVISION, fechaResolucion = null, fechaLimite = ahora() + s.prioridad.diasPlazo * DIA),
            actor, "Solicitud reabierta por el cliente", texto = motivo.trim()
        )
        return null
    }

    // Deja en el historial que se compartió la referencia por WhatsApp
    suspend fun registrarEnvioWhatsApp(codigo: String, actor: Usuario): String? {
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (!Reglas.puedeVer(actor, s)) return "No tienes acceso a esta solicitud"
        guardar(s, s, actor, "Referencia compartida por WhatsApp")
        return null
    }

    // ---------- Funciones internas ----------

    // Todos los cambios pasan por aquí: actualiza la solicitud y agrega al historial (columna "comentario")
    // una línea con fecha, usuario y perfil. Si hay "mensaje", también queda como comentario normal.
    private suspend fun guardar(
        antes: Solicitud, despues: Solicitud, actor: Usuario, accion: String,
        texto: String = "", interno: Boolean = false, mensaje: String? = null, evidencia: String? = null
    ) {
        val t = ahora()
        var bitacora = despues.comentario
        if (mensaje != null) {
            bitacora = Comentario.agregar(bitacora, Comentario(t, actor.nombre, actor.perfil, mensaje, interno))
        }
        bitacora = Comentario.agregar(
            bitacora,
            Comentario(t, actor.nombre, actor.perfil, texto, interno, accion, antes.estado, despues.estado, evidencia)
        )
        dao.actualizar(
            despues.copy(actualizada = t, comentario = bitacora, evidencia = unirEvidencia(despues.evidencia, evidencia))
        )
    }

    // Comprueba que quien actúa sea administrador y que la solicitud siga abierta
    private suspend fun comoAdmin(codigo: String, actor: Usuario, accion: suspend (Solicitud) -> Unit): String? {
        if (actor.perfil != Perfil.ADMIN) return "Solo el administrador puede hacer esto"
        val s = dao.obtener(codigo) ?: return "Solicitud no encontrada"
        if (s.estado.esFinal) return "La solicitud ya terminó y no se puede modificar"
        accion(s)
        return null
    }

    private fun unirEvidencia(actual: String?, nueva: String?): String? = when {
        nueva == null -> actual
        actual.isNullOrBlank() -> nueva
        else -> "$actual, $nueva"
    }

    private fun String.limpiarServicio() = trim().ifBlank { "No indicado" }

    // ---------- Datos iniciales (solo la primera vez) ----------
    private suspend fun cargarDatosIniciales() = db.withTransaction {
        usuarioDao.insertar(USUARIOS_INICIALES)
        val (c1, c2) = USUARIOS_INICIALES.filter { it.perfil == Perfil.CLIENTE }
        val (t1, t2) = USUARIOS_INICIALES.filter { it.perfil == Perfil.TECNICO }

        sembrar(1007, c1, "Crear formulario de contacto", "Se necesita un formulario simple en la página de inicio.", Categoria.DESARROLLO_WEB, Prioridad.ALTA, Estado.EN_REVISION, null, 1, "Sitio web corporativo")
        sembrar(1006, c2, "Consulta por certificado SSL", "¿Cuándo vence el certificado del sitio de la tienda?", Categoria.CONSULTA, Prioridad.BAJA, Estado.CERRADA, t1, 10, "tiendasur.cl")
        sembrar(1005, c1, "Renovación de hosting", "Renovar el plan de hosting anual del sitio web.", Categoria.HOSTING, Prioridad.MEDIA, Estado.RESUELTA, t2, 4, "Hosting anual")
        sembrar(1004, c2, "Cotización de campaña digital", "Solicito una propuesta de campaña en redes sociales.", Categoria.MARKETING_DIGITAL, Prioridad.BAJA, Estado.RECIBIDA, null, 0, "Marketing digital")
        sembrar(1003, c2, "Correo corporativo no recibe mensajes", "Las cuentas de la tienda no reciben correos desde ayer.", Categoria.SOPORTE_CORREO, Prioridad.ALTA, Estado.PENDIENTE_INFO, t1, 3, "Correo corporativo")
        sembrar(1002, c1, "Cambiar banner de portada", "Reemplazar el banner por la promoción de temporada.", Categoria.SOLICITUD_CAMBIO, Prioridad.MEDIA, Estado.ASIGNADA, t2, 2, "pymeandes.cl", Adjuntos.disponibles[2])
        sembrar(1001, c1, "Sitio web caído tras actualización", "El sitio muestra error 500 desde la última actualización.", Categoria.INCIDENTE_TECNICO, Prioridad.URGENTE, Estado.EN_PROCESO, t1, 6, "pymeandes.cl", Adjuntos.disponibles[0])
    }

    // Crea una solicitud de ejemplo ya avanzada, con su historial
    private suspend fun sembrar(
        num: Int, cliente: Usuario, titulo: String, desc: String, cat: Categoria, prio: Prioridad,
        estado: Estado, responsable: Usuario?, diasAtras: Int, servicio: String, archivo: ArchivoAdjunto? = null
    ) {
        val codigo = "SOL-$num"
        val creada = ahora() - diasAtras * DIA
        val admin = USUARIOS_INICIALES.first { it.perfil == Perfil.ADMIN }
        // Una solicitud cerrada pasó antes por "Resuelta" y luego el cliente la confirmó
        val intermedio = if (estado == Estado.CERRADA) Estado.RESUELTA else estado

        // Historial: registrada -> asignada -> estado actual
        val eventos = mutableListOf(
            Comentario(
                creada, cliente.nombre, Perfil.CLIENTE, "Origen: WhatsApp",
                accion = "Solicitud registrada", estadoNuevo = Estado.RECIBIDA, archivo = archivo?.nombre
            )
        )
        if (responsable != null) eventos += Comentario(
            creada + DIA / 4, admin.nombre, Perfil.ADMIN, "",
            accion = "Asignada a ${responsable.nombre}", estadoAnterior = Estado.RECIBIDA, estadoNuevo = Estado.ASIGNADA
        )
        if (intermedio != Estado.RECIBIDA && intermedio != Estado.ASIGNADA) eventos += Comentario(
            creada + DIA / 2, responsable?.nombre ?: admin.nombre, if (responsable != null) Perfil.TECNICO else Perfil.ADMIN, "",
            accion = "Cambio de estado a ${intermedio.etiqueta}",
            estadoAnterior = if (responsable != null) Estado.ASIGNADA else Estado.RECIBIDA, estadoNuevo = intermedio
        )
        if (estado == Estado.CERRADA) eventos += Comentario(
            creada + DIA * 3 / 4, cliente.nombre, Perfil.CLIENTE, "Solución aceptada",
            accion = "Cliente confirmó conformidad", estadoAnterior = Estado.RESUELTA, estadoNuevo = Estado.CERRADA
        )
        val actualizada = eventos.last().fecha

        dao.insertar(
            Solicitud(
                codigo = codigo, titulo = titulo, descripcion = desc, categoria = cat, servicio = servicio,
                evidencia = archivo?.nombre, comentario = eventos.fold("") { acc, e -> Comentario.agregar(acc, e) },
                clienteId = cliente.id, clienteNombre = cliente.nombre, empresa = cliente.empresa ?: "",
                prioridad = prio, estado = estado, responsableId = responsable?.id, responsableNombre = responsable?.nombre,
                creada = creada, actualizada = actualizada, fechaLimite = creada + prio.diasPlazo * DIA,
                fechaResolucion = if (estado == Estado.RESUELTA || estado == Estado.CERRADA) actualizada else null
            )
        )
    }

    private companion object {
        const val DIA = 86_400_000L                 // un día en milisegundos
        val candado = Mutex()                       // evita cargar los datos iniciales dos veces
        fun ahora() = System.currentTimeMillis()

        val USUARIOS_INICIALES = listOf(
            Usuario("u-c1", "Camila Soto", "camila@tecnoluna.cl", "Luna1234", Perfil.CLIENTE, "Pyme Andes SpA", "+56 9 1111 0001"),
            Usuario("u-c2", "Diego Vargas", "diego@tecnoluna.cl", "Luna1234", Perfil.CLIENTE, "Tienda Sur Ltda", "+56 9 1111 0002"),
            Usuario("u-a1", "Alma Coordinadora", "admin@tecnoluna.cl", "Luna1234", Perfil.ADMIN),
            Usuario("u-t1", "Tomás Herrera", "tomas@tecnoluna.cl", "Luna1234", Perfil.TECNICO),
            Usuario("u-t2", "Tania Núñez", "tania@tecnoluna.cl", "Luna1234", Perfil.TECNICO)
        )
    }
}
