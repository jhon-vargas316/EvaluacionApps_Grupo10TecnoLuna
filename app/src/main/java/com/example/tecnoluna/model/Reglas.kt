package com.example.tecnoluna.model

import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario

// Reglas de la app: quién puede ver, editar, eliminar y cambiar cada solicitud
object Reglas {

    // El cliente titular es quien creó la solicitud
    fun esTitular(actor: Usuario, s: Solicitud) = actor.perfil == Perfil.CLIENTE && s.clienteId == actor.id

    // Cada perfil solo ve las solicitudes que le corresponden
    fun puedeVer(actor: Usuario, s: Solicitud): Boolean = when (actor.perfil) {
        Perfil.ADMIN -> true                          // el administrador ve todas
        Perfil.CLIENTE -> s.clienteId == actor.id     // el cliente solo ve las suyas
        Perfil.TECNICO -> s.responsableId == actor.id // el técnico solo ve las asignadas a él
    }

    // Editar (UPDATE): el cliente mientras esté "Recibida"; el administrador mientras no esté terminada
    fun puedeEditar(actor: Usuario, s: Solicitud): Boolean = when (actor.perfil) {
        Perfil.ADMIN -> !s.estado.esFinal
        Perfil.CLIENTE -> esTitular(actor, s) && s.estado == Estado.RECIBIDA
        Perfil.TECNICO -> false
    }

    // Eliminar (DELETE): el cliente mientras esté "Recibida"; el administrador siempre
    fun puedeEliminar(actor: Usuario, s: Solicitud): Boolean = when (actor.perfil) {
        Perfil.ADMIN -> true
        Perfil.CLIENTE -> esTitular(actor, s) && s.estado == Estado.RECIBIDA
        Perfil.TECNICO -> false
    }

    // Estados a los que puede pasar la solicitud según quién la mire
    fun transiciones(actor: Usuario, s: Solicitud): List<Estado> = when (actor.perfil) {
        Perfil.ADMIN -> buildList {
            when (s.estado) {
                Estado.RECIBIDA -> add(Estado.EN_REVISION)
                Estado.EN_REVISION, Estado.ASIGNADA, Estado.EN_PROCESO -> add(Estado.PENDIENTE_INFO)
                Estado.RESUELTA -> add(Estado.CERRADA)
                else -> Unit
            }
            if (!s.estado.esFinal) add(Estado.CANCELADA) // mientras no esté terminada, se puede cancelar
        }
        Perfil.TECNICO ->
            if (s.responsableId != actor.id) emptyList()
            else when (s.estado) {
                Estado.ASIGNADA -> listOf(Estado.EN_PROCESO)
                Estado.EN_PROCESO -> listOf(Estado.PENDIENTE_INFO, Estado.RESUELTA)
                else -> emptyList()
            }
        Perfil.CLIENTE -> emptyList() // el cliente solo confirma, reabre o responde
    }

    // Estos cambios obligan a escribir una explicación
    fun requiereComentario(nuevo: Estado) =
        nuevo == Estado.PENDIENTE_INFO || nuevo == Estado.RESUELTA || nuevo == Estado.CANCELADA
}
