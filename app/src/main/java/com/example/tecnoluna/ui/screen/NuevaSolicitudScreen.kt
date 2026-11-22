@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tecnoluna.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.*
import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario
import com.example.tecnoluna.ui.components.CampoSelector
import com.example.tecnoluna.ui.components.WhatsAppDialog
import com.example.tecnoluna.ui.components.coloresBarraSuperior
import com.example.tecnoluna.ui.utils.anchoMaximo
import com.example.tecnoluna.viewmodel.NuevaSolicitudViewModel
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Formulario para CREAR una solicitud nueva o EDITAR una existente
@Composable
fun NuevaSolicitudScreen(
    usuario: Usuario,
    formVm: NuevaSolicitudViewModel,
    solVm: SolicitudViewModel,
    widthClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val ui by formVm.ui.collectAsState() // lo que el formulario debe mostrar
    val snack = remember { SnackbarHostState() }
    var mostrarWhatsApp by remember { mutableStateOf(false) }
    val editando = ui.codigoEdicion != null

    LaunchedEffect(Unit) { solVm.mensaje.collect { snack.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = coloresBarraSuperior(),
                title = { Text(if (editando) "Editar solicitud" else "Nueva solicitud") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = anchoMaximo(widthClass)).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (editando) "Modifica los datos de la solicitud ${ui.codigoEdicion}."
                    else "Cuéntanos qué necesitas. Los campos con error se marcan en rojo.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Campo: título
                OutlinedTextField(
                    value = ui.titulo, onValueChange = formVm::onTitulo, label = { Text("Título") },
                    singleLine = true, isError = ui.errores.titulo != null,
                    supportingText = { Text(ui.errores.titulo ?: "${ui.titulo.length}/60") }, // muestra el error o el contador
                    modifier = Modifier.fillMaxWidth()
                )
                // Campo: descripción
                OutlinedTextField(
                    value = ui.descripcion, onValueChange = formVm::onDescripcion, label = { Text("Descripción") },
                    minLines = 4, isError = ui.errores.descripcion != null,
                    supportingText = { Text(ui.errores.descripcion ?: "${ui.descripcion.length}/500") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Campo: categoría (lista desplegable)
                CampoSelector(
                    label = "Categoría", valor = ui.categoria?.etiqueta ?: "Elige una categoría",
                    opciones = Categoria.entries, etiqueta = { it.etiqueta },
                    onSeleccion = formVm::onCategoria, error = ui.errores.categoria
                )
                // Campo: servicio o sitio relacionado
                OutlinedTextField(
                    value = ui.servicio, onValueChange = formVm::onServicio,
                    label = { Text("Servicio o sitio web relacionado (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                // Campo: evidencia (solo al crear)
                if (!editando) {
                    CampoSelector(
                        label = "Evidencia (opcional)",
                        valor = ui.archivo?.let { "${it.nombre} (${it.tamanoKb} KB)" } ?: "Sin evidencia",
                        opciones = listOf<ArchivoAdjunto?>(null) + Adjuntos.disponibles,
                        etiqueta = { a -> a?.let { "${it.nombre} (${it.tamanoKb} KB)" } ?: "Sin evidencia" },
                        onSeleccion = formVm::onArchivo, error = ui.errores.archivo
                    )
                    Text("Formatos permitidos: PNG, JPG o PDF · máximo 2 MB.", style = MaterialTheme.typography.bodySmall)
                }
                // Error general (por ejemplo, falta de permiso al editar)
                ui.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(8.dp))
                // Botón principal (muestra "Guardando…" mientras trabaja)
                Button(
                    onClick = { formVm.enviar(usuario) },
                    enabled = !ui.enviando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    if (ui.enviando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else Text(if (editando) "Guardar cambios" else "Enviar solicitud")
                }
            }
        }
    }

    // Ventanas que aparecen cuando la operación terminó bien
    val resultado = ui.resultado
    if (resultado != null && !mostrarWhatsApp) {
        if (editando) {
            // Se editó: se vuelve al detalle
            AlertDialog(
                onDismissRequest = {},
                title = { Text("¡Cambios guardados!") },
                text = { Text("Los datos de la solicitud ${resultado.codigo} fueron actualizados.") },
                confirmButton = { Button(onClick = { formVm.limpiar(); onBack() }) { Text("Volver al detalle") } }
            )
        } else {
            // Se creó: se muestra el código de seguimiento
            AlertDialog(
                onDismissRequest = {},
                title = { Text("¡Solicitud registrada!") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tu código de seguimiento es:")
                        Text(resultado.codigo, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Estado actual: ${resultado.estado.etiqueta}", style = MaterialTheme.typography.bodyMedium)
                        Text("Si quieres, comparte este código por WhatsApp.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = { Button(onClick = { mostrarWhatsApp = true }) { Text("Compartir por WhatsApp") } },
                dismissButton = {
                    TextButton(onClick = { formVm.limpiar(); onVerDetalle(resultado.codigo) }) { Text("Ver solicitud") }
                }
            )
        }
    }
    // Ventana con el mensaje de WhatsApp
    if (resultado != null && mostrarWhatsApp) {
        WhatsAppDialog(
            mensaje = resultado.mensajeWhatsApp(),
            onEnviar = {
                solVm.registrarEnvioWhatsApp(resultado.codigo, usuario)
                formVm.limpiar()
                onVerDetalle(resultado.codigo)
            },
            onCerrar = { mostrarWhatsApp = false }
        )
    }
}
