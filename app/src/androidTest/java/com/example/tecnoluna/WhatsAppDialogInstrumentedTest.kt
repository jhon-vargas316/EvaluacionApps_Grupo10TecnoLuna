package com.example.tecnoluna

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tecnoluna.ui.components.WhatsAppDialog
import com.example.tecnoluna.ui.theme.TecnoLunaTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

// PRUEBA INSTRUMENTADA 2 (Compose UI Test + MockK): el diálogo muestra el mensaje y
// al tocar "Compartir" llama a onEnviar
class WhatsAppDialogInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dialogoWhatsAppMuestraMensajeYAlCompartirLlamaOnEnviar() {
        val onEnviar = mockk<() -> Unit>(relaxed = true)
        val onCerrar = mockk<() -> Unit>(relaxed = true)

        composeRule.setContent {
            TecnoLunaTheme {
                WhatsAppDialog(
                    mensaje = "Código: SOL-1001",
                    onEnviar = onEnviar,
                    onCerrar = onCerrar
                )
            }
        }

        composeRule.onNodeWithText("Código: SOL-1001").assertIsDisplayed()
        composeRule.onNodeWithText("Compartir").performClick()

        verify(exactly = 1) { onEnviar() }
        verify(exactly = 0) { onCerrar() }
    }
}
