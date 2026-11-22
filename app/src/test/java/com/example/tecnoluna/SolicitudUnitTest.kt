package com.example.tecnoluna

import com.example.tecnoluna.model.Categoria
import com.example.tecnoluna.model.Estado
import com.example.tecnoluna.model.Prioridad
import com.example.tecnoluna.model.data.local.Solicitud
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// PRUEBA UNITARIA 2 (JUnit 5 + Kotest): una solicitud activa con plazo vencido está atrasada
class SolicitudUnitTest {

    private fun solicitud(estado: Estado, fechaLimite: Long) = Solicitud(
        codigo = "SOL-1002",
        titulo = "Cambio de correo",
        descripcion = "Configurar cuenta",
        categoria = Categoria.SOPORTE_CORREO,
        servicio = "Correo",
        clienteId = "cli1",
        clienteNombre = "Cliente Uno",
        empresa = "Empresa SA",
        prioridad = Prioridad.MEDIA,
        estado = estado,
        creada = 0L,
        actualizada = 0L,
        fechaLimite = fechaLimite
    )

    @Test
    fun solicitudActivaVencidaEstaAtrasada() {
        val ahora = 10_000L

        solicitud(Estado.EN_PROCESO, fechaLimite = 5_000L).estaAtrasada(ahora) shouldBe true   // activa y vencida
        solicitud(Estado.EN_PROCESO, fechaLimite = 20_000L).estaAtrasada(ahora) shouldBe false // aún hay plazo
        solicitud(Estado.CERRADA, fechaLimite = 5_000L).estaAtrasada(ahora) shouldBe false     // terminada
    }
}
