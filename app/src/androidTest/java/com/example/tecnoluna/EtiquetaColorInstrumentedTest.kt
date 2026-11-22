package com.example.tecnoluna

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.tecnoluna.ui.components.EtiquetaColor
import com.example.tecnoluna.ui.theme.TecnoLunaTheme
import org.junit.Rule
import org.junit.Test

// PRUEBA INSTRUMENTADA 1 (Compose UI Test): la etiqueta de color muestra su texto
class EtiquetaColorInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun etiquetaColorMuestraElTexto() {
        composeRule.setContent {
            TecnoLunaTheme {
                EtiquetaColor(texto = "Prioridad alta", color = Color(0xFFFFCC80))
            }
        }

        composeRule.onNodeWithText("Prioridad alta").assertIsDisplayed()
    }
}
