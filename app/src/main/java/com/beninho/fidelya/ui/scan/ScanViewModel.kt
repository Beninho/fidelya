package com.beninho.fidelya.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beninho.fidelya.barcode.SupportedBarcodeFormats
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Scanning : ScanState()
    object Timeout : ScanState()
    data class Detected(val value: String, val format: String) : ScanState()
}

class ScanViewModel : ViewModel() {
    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state
    private val detected = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        viewModelScope.launch {
            delay(10_000)
            if (!detected.get()) _state.value = ScanState.Timeout
        }
    }

    fun onBarcodeDetected(barcode: Barcode) {
        if (!detected.compareAndSet(false, true)) return
        val value = barcode.rawValue ?: run { detected.set(false); return }
        // Keep scanning rather than storing the card under a format we cannot render back.
        val format = SupportedBarcodeFormats.nameOf(barcode.format)
            ?: run { detected.set(false); return }
        _state.value = ScanState.Detected(value, format)
    }

    fun resetTimeout() {
        if (!detected.get()) _state.value = ScanState.Scanning
    }
}
