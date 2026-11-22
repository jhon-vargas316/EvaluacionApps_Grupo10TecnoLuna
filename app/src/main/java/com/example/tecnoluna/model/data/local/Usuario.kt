package com.example.tecnoluna.model.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tecnoluna.model.Perfil

// Tabla 1 de 2: "usuarios". Una sola tabla sirve para los tres perfiles (cliente, administrador y técnico):
// la columna "perfil" indica de cuál se trata. Empresa y teléfono solo se usan en los clientes.
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val id: String,
    val nombre: String,
    val correo: String,
    val clave: String,
    val perfil: Perfil,
    val empresa: String? = null,     // solo clientes
    val telefono: String? = null,    // solo clientes
    val activo: Boolean = true       // estado de la cuenta
)
