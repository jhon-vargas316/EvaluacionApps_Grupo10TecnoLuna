package com.example.tecnoluna.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.data.local.Usuario
import com.example.tecnoluna.ui.utils.fechaHora
import com.example.tecnoluna.viewmodel.SolicitudViewModel

// Pestaña "Avisos": cambios que otras personas hicieron en las solicitudes del usuario
@Composable
fun NotificacionesScreen(usuario: Usuario, vm: SolicitudViewModel) {
    val todas by vm.solicitudes.collectAsState()
    val visto by vm.vistoHasta.collectAsState()
    // Los avisos se calculan desde las solicitudes (no hay una tabla de avisos)
    val mias = remember(todas, visto, usuario) { vm.calcularAvisos(usuario, todas, visto) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${mias.count { !it.leido }} sin leer", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = vm::marcarLeidas) { Text("Marcar todos como leídos") }
            }
        }
        if (mias.isEmpty()) item { Text("No tienes avisos por ahora.") }
        items(mias) { n ->
            // Los avisos sin leer se ven más oscuros y en negrita
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (n.leido) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(n.mensaje, fontWeight = if (n.leido) FontWeight.Normal else FontWeight.Bold)
                    Text(n.fecha.fechaHora(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
