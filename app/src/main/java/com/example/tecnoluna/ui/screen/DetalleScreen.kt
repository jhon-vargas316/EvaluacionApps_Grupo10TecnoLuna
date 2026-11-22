@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.*
import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario
import com.example.tecnoluna.ui.components.*
import com.example.tecnoluna.ui.utils.anchoMaximo
import com.example.tecnoluna.ui.utils.fechaHora
import com.example.tecnoluna.viewmodel.SolicitudViewModel
import kotlinx.coroutines.flow.onEach

// Pantalla con todo el detalle de una solicitud (READ), con opciones para editarla (UPDATE) y eliminarla (DELETE)
@Composable
fun DetalleScreen(
    codigo: String,
    usuario: Usuario,
    vm: SolicitudViewModel,
    widthClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onEditar: (Solicitud) -> Unit
) {
    // La solicitud completa se lee de Room y se actualiza sola cuando cambia
    var cargado by remember(codigo) { mutableStateOf(false) }
    val flujo = remember(codigo) { vm.detalle(codigo).onEach { cargado = true } }
    val detalle by flujo.collectAsState(initial = null)

    val snack = remember { SnackbarHostState() }
    var mostrarWhatsApp by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.mensaje.collect { snack.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = coloresBarraSuperior(),
                title = { Text(codigo) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        val s = detalle
        // Mientras carga, o si no existe / no hay permiso, se muestra un aviso
        if (s == null || !Reglas.puedeVer(usuario, s)) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!cargado) CircularProgressIndicator() else Text("No tienes acceso a esta solicitud o no existe.")
            }
            return@Scaffold
        }
        val mod = Modifier.widthIn(max = anchoMaximo(widthClass)).fillMaxWidth()
        // El cliente no ve los comentarios ni las líneas internas
        val verInternos = usuario.perfil != Perfil.CLIENTE
        // Comentarios e historial salen de la columna "comentario" de la propia solicitud
        val lineas = s.lineas().filter { verInternos || !it.interno }
        val comentarios = lineas.filter { !it.esHistorial }
        val historial = lineas.filter { it.esHistorial }
        val evidencias = s.listaEvidencias()

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Cabecera(s, mod) }
            item { Seccion("Datos de la solicitud", mod) { Datos(s) } }
            item { Seccion("Evidencias (${evidencias.size})", mod) { Evidencias(evidencias) } }

            // Editar y eliminar: solo aparecen si las reglas lo permiten
            if (Reglas.puedeEditar(usuario, s) || Reglas.puedeEliminar(usuario, s)) {
                item {
                    Row(mod, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Reglas.puedeEditar(usuario, s)) {
                            OutlinedButton(onClick = { onEditar(s) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Text("Editar solicitud")
                            }
                        }
                        if (Reglas.puedeEliminar(usuario, s)) {
                            OutlinedButton(
                                onClick = { confirmarEliminar = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) { Text("Eliminar") }
                        }
                    }
                }
            }

            // Solo el cliente puede compartir la referencia por WhatsApp
            if (usuario.perfil == Perfil.CLIENTE) {
                item {
                    OutlinedButton(onClick = { mostrarWhatsApp = true }, modifier = mod.heightIn(min = 48.dp)) {
                        Text("Compartir por WhatsApp")
                    }
                }
            }

            item { PanelAcciones(s, usuario, vm, mod) }
            item { Seccion("Comentarios (${comentarios.size})", mod) { Comentarios(comentarios) } }
            item { Seccion("Historial", mod) { Historial(historial) } }
        }

        // Ventana de confirmación antes de eliminar
        if (confirmarEliminar) {
            AlertDialog(
                onDismissRequest = { confirmarEliminar = false },
                title = { Text("¿Eliminar la solicitud ${s.codigo}?") },
                text = { Text("Se borrarán también sus comentarios, evidencias e historial. Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = { confirmarEliminar = false; vm.eliminar(s.codigo, usuario) { onBack() } },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
            )
        }
        if (mostrarWhatsApp) {
            WhatsAppDialog(
                mensaje = s.mensajeWhatsApp(),
                onEnviar = { vm.registrarEnvioWhatsApp(s.codigo, usuario); mostrarWhatsApp = false },
                onCerrar = { mostrarWhatsApp = false }
            )
        }
    }
}

// ---------------------------------------------------------------- partes de la pantalla

// Tarjeta con un título y contenido adentro
@Composable
private fun Seccion(titulo: String, modifier: Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

// Parte de arriba: título, estado, prioridad y qué significa el estado
@Composable
private fun Cabecera(s: Solicitud, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.titulo, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EtiquetaColor(s.estado.etiqueta, s.estado.color())
            EtiquetaColor("Prioridad ${s.prioridad.etiqueta.lowercase()}", s.prioridad.color())
        }
        Text(s.estado.descripcion, style = MaterialTheme.typography.bodyLarge) // explica el estado en palabras simples
        if (s.estaAtrasada()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Solicitud atrasada (debía resolverse antes del ${s.fechaLimite.fechaHora()})", color = MaterialTheme.colorScheme.error)
            }
        }
        Text("Última actualización: ${s.actualizada.fechaHora()}", style = MaterialTheme.typography.bodySmall)
    }
}

// Una línea "nombre: valor"
@Composable
private fun Fila(label: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
    }
}

// Datos principales de la solicitud
@Composable
private fun Datos(s: Solicitud) {
    Text(s.descripcion, style = MaterialTheme.typography.bodyMedium)
    HorizontalDivider()
    Fila("Cliente", "${s.clienteNombre} (${s.empresa})")
    Fila("Categoría", s.categoria.etiqueta)
    Fila("Servicio o sitio", s.servicio)
    Fila("Responsable", s.responsableNombre ?: "Sin asignar")
    Fila("Creada", s.creada.fechaHora())
    Fila("Fecha límite", s.fechaLimite.fechaHora())
    s.fechaResolucion?.let { Fila("Resuelta", it.fechaHora()) }
    if (s.confirmadaPorCliente()) Fila("Conformidad", "Confirmada por el cliente")
    s.motivoCancelacion()?.let { Fila("Motivo de cancelación", it) }
}

// Archivos adjuntos (en la tabla solo se guarda el nombre del archivo)
@Composable
private fun Evidencias(lista: List<String>) {
    if (lista.isEmpty()) Text("No hay archivos adjuntos.", style = MaterialTheme.typography.bodySmall)
    lista.forEach { nombre ->
        Text(nombre, fontWeight = FontWeight.SemiBold)
    }
}

// Mensajes escritos en la solicitud
@Composable
private fun Comentarios(lista: List<Comentario>) {
    if (lista.isEmpty()) Text("Todavía no hay comentarios.", style = MaterialTheme.typography.bodySmall)
    lista.forEach { c ->
        Column {
            Text("${c.autor} (${c.perfil.etiqueta})${if (c.interno) " · INTERNO" else ""}", style = MaterialTheme.typography.labelLarge)
            Text(c.texto)
            Text(c.fecha.fechaHora(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Línea de tiempo: qué pasó, quién lo hizo y cuándo
@Composable
private fun Historial(lista: List<Comentario>) {
    lista.forEachIndexed { i, e ->
        if (i > 0) HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            e.accion?.let { Text(it, fontWeight = FontWeight.SemiBold) }
            // Si cambió de estado, se muestra "antes -> después"
            if (e.estadoAnterior != null && e.estadoNuevo != null && e.estadoAnterior != e.estadoNuevo) {
                Text("${e.estadoAnterior.etiqueta} → ${e.estadoNuevo.etiqueta}", style = MaterialTheme.typography.bodySmall)
            }
            if (e.texto.isNotBlank()) Text(e.texto, style = MaterialTheme.typography.bodyMedium)
            e.archivo?.let { Text("Archivo: $it", style = MaterialTheme.typography.bodySmall) }
            Text("${e.fecha.fechaHora()} · ${e.autor} (${e.perfil.etiqueta})", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------- acciones según el perfil

@Composable
private fun PanelAcciones(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("¿Qué quieres hacer?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Cada perfil ve opciones distintas
            when (usuario.perfil) {
                Perfil.ADMIN -> AccionesAdmin(s, usuario, vm)
                Perfil.TECNICO ->
                    if (s.responsableId == usuario.id) EstadoYComentarios(s, usuario, vm, permiteAvance = true)
                    else Text("Esta solicitud no está asignada a ti.")
                Perfil.CLIENTE -> AccionesCliente(s, usuario, vm)
            }
        }
    }
}

// Opciones del administrador: clasificar, priorizar y asignar
@Composable
private fun AccionesAdmin(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel) {
    val tecnicos by vm.tecnicos.collectAsState()
    val editable = !s.estado.esFinal // si está cerrada o cancelada ya no se puede cambiar
    CampoSelector(
        label = "Categoría", valor = s.categoria.etiqueta,
        opciones = Categoria.entries, etiqueta = { it.etiqueta },
        onSeleccion = { vm.clasificar(s.codigo, usuario, it) }, enabled = editable
    )
    CampoSelector(
        label = "Prioridad", valor = s.prioridad.etiqueta,
        opciones = Prioridad.entries, etiqueta = { it.etiqueta },
        onSeleccion = { vm.definirPrioridad(s.codigo, usuario, it) }, enabled = editable
    )
    CampoSelector(
        label = "Responsable", valor = s.responsableNombre ?: "Sin asignar",
        opciones = tecnicos, etiqueta = { it.nombre },
        onSeleccion = { vm.asignar(s.codigo, usuario, it.id) }, enabled = editable
    )
    HorizontalDivider()
    EstadoYComentarios(s, usuario, vm, permiteAvance = false)
}

// Texto de cada botón de cambio de estado
private fun etiquetaAccion(e: Estado, perfil: Perfil) = when (e) {
    Estado.EN_REVISION -> "Pasar a revisión"
    Estado.PENDIENTE_INFO -> if (perfil == Perfil.ADMIN) "Pedir información al cliente" else "Necesito más información"
    Estado.EN_PROCESO -> "Comenzar a trabajar"
    Estado.RESUELTA -> "Marcar como resuelta"
    Estado.CERRADA -> "Cerrar solicitud"
    Estado.CANCELADA -> "Cancelar solicitud"
    else -> e.etiqueta
}

// Comentarios, avances y botones para cambiar el estado (administrador y técnico)
@Composable
private fun EstadoYComentarios(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel, permiteAvance: Boolean) {
    var texto by rememberSaveable(s.codigo) { mutableStateOf("") }   // lo que se escribe
    var interno by rememberSaveable { mutableStateOf(false) }         // ¿comentario interno?
    var archivo by remember { mutableStateOf<ArchivoAdjunto?>(null) } // archivo elegido
    val transiciones = Reglas.transiciones(usuario, s)                // estados a los que puede pasar

    OutlinedTextField(
        value = texto, onValueChange = { texto = it },
        label = { Text(if (permiteAvance) "Escribe el avance, comentario o solución" else "Escribe un comentario o motivo") },
        minLines = 2, modifier = Modifier.fillMaxWidth()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = interno, onCheckedChange = { interno = it })
        Text("Comentario interno (el cliente no lo ve)")
    }
    // Solo el técnico puede adjuntar evidencia a un avance
    if (permiteAvance) SelectorEvidencia(archivo) { archivo = it }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { vm.comentar(s.codigo, usuario, texto, interno) { texto = "" } }, // si salió bien, limpia el texto
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) { Text("Comentar") }
        if (permiteAvance) {
            OutlinedButton(
                onClick = { vm.registrarAvance(s.codigo, usuario, texto, archivo) { texto = ""; archivo = null } },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text("Guardar avance") }
        }
    }
    // Botones de cambio de estado (solo los que corresponden)
    AnimatedVisibility(transiciones.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cambiar estado (se usa el texto de arriba como explicación):", style = MaterialTheme.typography.bodySmall)
            transiciones.forEach { e ->
                Button(
                    onClick = { vm.cambiarEstado(s.codigo, usuario, e, texto) { texto = "" } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(etiquetaAccion(e, usuario.perfil)) }
            }
        }
    }
}

// Lista desplegable para elegir un archivo de evidencia
@Composable
private fun SelectorEvidencia(archivo: ArchivoAdjunto?, onSeleccion: (ArchivoAdjunto?) -> Unit) {
    CampoSelector(
        label = "Evidencia (opcional)", valor = archivo?.nombre ?: "Sin evidencia",
        opciones = listOf<ArchivoAdjunto?>(null) + Adjuntos.disponibles,
        etiqueta = { it?.nombre ?: "Sin evidencia" }, onSeleccion = onSeleccion
    )
}

// Campo de texto + botón para pedir que se reabra (lo usan los estados "Resuelta" y "Cerrada")
@Composable
private fun BloqueReabrir(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel) {
    var motivo by rememberSaveable(s.codigo, s.estado) { mutableStateOf("") }
    OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo para reabrir") }, minLines = 2, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { vm.reabrir(s.codigo, usuario, motivo) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Pedir que se reabra")
    }
}

// Opciones del cliente: responder, confirmar, reabrir o comentar
@Composable
private fun AccionesCliente(s: Solicitud, usuario: Usuario, vm: SolicitudViewModel) {
    var texto by rememberSaveable(s.codigo, s.estado) { mutableStateOf("") }
    var archivo by remember(s.estado) { mutableStateOf<ArchivoAdjunto?>(null) }

    when (s.estado) {
        // Le piden más información: el cliente responde
        Estado.PENDIENTE_INFO -> {
            Text("Necesitamos más información para continuar.")
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Tu respuesta") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            SelectorEvidencia(archivo) { archivo = it }
            Button(onClick = { vm.responderInfo(s.codigo, usuario, texto, archivo) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Enviar respuesta")
            }
        }
        // Hay una solución: el cliente la acepta o pide reabrir
        Estado.RESUELTA -> {
            Text("Tenemos una solución. ¿Estás conforme?")
            Button(onClick = { vm.confirmar(s.codigo, usuario) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Estoy conforme, cerrar solicitud")
            }
            BloqueReabrir(s, usuario, vm)
        }
        // Ya está cerrada: solo puede pedir reabrirla
        Estado.CERRADA -> {
            Text("La solicitud está cerrada. Si el problema sigue, puedes pedir que se reabra.")
            BloqueReabrir(s, usuario, vm)
        }
        Estado.CANCELADA -> Text("Esta solicitud fue cancelada y ya no admite cambios.")
        // En cualquier otro estado puede agregar un comentario
        else -> {
            Text("Si quieres, agrega más información a tu solicitud.")
            OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Comentario") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = { vm.comentar(s.codigo, usuario, texto, false) { texto = "" } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Enviar comentario") }
        }
    }
}
