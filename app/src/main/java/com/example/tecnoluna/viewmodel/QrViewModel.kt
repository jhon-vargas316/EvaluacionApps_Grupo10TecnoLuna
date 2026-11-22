package com.example.tecnoluna.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tecnoluna.model.data.local.AppDatabase
import com.example.tecnoluna.repository.QrRepository
import com.example.tecnoluna.model.QrResult
import com.example.tecnoluna.model.data.local.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Guarda y expone el resultado del escaneo para que la pantalla lo observe
class QrViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = QrRepository(AppDatabase.getDatabase(app))

    // Versión privada (solo el ViewModel la cambia) y versión pública de solo lectura
    private val _qrResult = MutableStateFlow<QrResult?>(null)
    val qrResult: StateFlow<QrResult?> = _qrResult.asStateFlow()

    // La pantalla llama a esta función cuando la cámara detecta un QR
    fun onQrDetected(content: String, usuario: Usuario) {
        viewModelScope.launch { _qrResult.value = repository.processQrContent(content, usuario) }
    }

    // Borra el resultado para poder escanear otro código
    fun clearResult() { _qrResult.value = null }
}
