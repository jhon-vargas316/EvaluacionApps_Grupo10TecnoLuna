package com.example.tecnoluna.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tecnoluna.model.data.local.Usuario

// Pestaña "Mi perfil": datos de la persona y cerrar sesión
@Composable
fun PerfilScreen(usuario: Usuario, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta con los datos de la persona
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleLarge)
                Text("Perfil: ${usuario.perfil.etiqueta}")
                Text("Correo: ${usuario.correo}")
                usuario.empresa?.let { Text("Empresa: $it") }
                usuario.telefono?.let { Text("Teléfono: $it") }
            }
        }

        // Botón para salir de la cuenta
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Cerrar sesión") }
    }
}
