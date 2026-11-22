package com.example.tecnoluna

import com.example.tecnoluna.model.Categoria
import com.example.tecnoluna.model.Estado
import com.example.tecnoluna.model.Perfil
import com.example.tecnoluna.model.Prioridad
import com.example.tecnoluna.model.Reglas
import com.example.tecnoluna.model.data.local.Solicitud
import com.example.tecnoluna.model.data.local.Usuario
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// PRUEBA UNITARIA 1 (JUnit 5 + Kotest): permisos según el perfil
class ReglasUnitTest {

    private fun usuario(id: String, perfil: Perfil) = Usuario(
        id = id, nombre = "Usuario $id", correo = "$id@tecnoluna.cl", clave = "1234", perfil = perfil
    )

    private fun solicitud(estado: Estado, clienteId: String) = Solicitud(
        codigo = "SOL-1001",
        titulo = "Sitio web caído",
        descripcion = "El sitio no carga",
        categoria = Categoria.INCIDENTE_TECNICO,
        servicio = "Hosting",
        clienteId = clienteId,
        clienteNombre = "Cliente Uno",
        empresa = "Empresa SA",
        prioridad = Prioridad.ALTA,
        estado = estado,
        creada = 0L,
        actualizada = 0L,
        fechaLimite = 0L
    )

    @Test
    fun clienteSoloVeYEditaSuSolicitudRecibida() {
        val titular = usuario("cli1", Perfil.CLIENTE)
        val otroCliente = usuario("cli2", Perfil.CLIENTE)
        val tecnico = usuario("tec1", Perfil.TECNICO)
        val solicitud = solicitud(Estado.RECIBIDA, clienteId = "cli1")

        Reglas.puedeVer(titular, solicitud) shouldBe true       // la creó él
        Reglas.puedeVer(otroCliente, solicitud) shouldBe false  // es de otro cliente
        Reglas.puedeEditar(titular, solicitud) shouldBe true    // está "Recibida"
        Reglas.puedeEditar(tecnico, solicitud) shouldBe false   // el técnico nunca edita
    }
}
