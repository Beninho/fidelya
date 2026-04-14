package com.beninho.fidelya.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        val format = when (barcode.format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_PDF417 -> "PDF_417"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            else -> "QR_CODE"
        }
        _state.value = ScanState.Detected(value, format)
    }

    fun resetTimeout() {
        if (!detected.get()) _state.value = ScanState.Scanning
    }
}
