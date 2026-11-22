package com.example.tecnoluna.repository

import com.example.tecnoluna.model.QrResult
import com.example.tecnoluna.model.Reglas
import com.example.tecnoluna.model.ResultadoQr
import com.example.tecnoluna.model.data.local.AppDatabase
import com.example.tecnoluna.model.data.local.Usuario

// Relaciona el texto leído desde un QR con una solicitud guardada en la base de datos
class QrRepository(db: AppDatabase) {

    private val dao = db.solicitudDao()

    // Busca un código con forma SOL-1234 dentro del texto.
    // Así funciona con QR que traen solo el código o con el enlace de seguimiento completo.
    private val patronCodigo = Regex("SOL-\\d+", RegexOption.IGNORE_CASE)

    suspend fun processQrContent(content: String, usuario: Usuario): QrResult {
        val texto = content.trim()
        val codigo = patronCodigo.find(texto)?.value?.uppercase()
            ?: return QrResult(texto, null, ResultadoQr.NO_ES_SOLICITUD)      // el QR no trae un código de solicitud

        val solicitud = dao.obtener(codigo)
            ?: return QrResult(texto, codigo, ResultadoQr.NO_ENCONTRADA)      // el código no existe

        if (!Reglas.puedeVer(usuario, solicitud)) {
            return QrResult(texto, codigo, ResultadoQr.SIN_ACCESO)            // existe, pero es de otra persona
        }
        return QrResult(texto, codigo, ResultadoQr.ENCONTRADA, solicitud)     // todo bien
    }
}
