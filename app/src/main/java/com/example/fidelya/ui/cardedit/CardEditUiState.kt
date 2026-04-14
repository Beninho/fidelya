package com.example.fidelya.ui.cardedit

data class CardEditUiState(
    val storeName: String = "",
    val cardNumber: String = "",
    val barcodeFormat: String = "QR_CODE",
    val backgroundColor: String = "#5C6BC0",
    val logoEmoji: String = "",
    val storeNameError: String? = null,
    val cardNumberError: String? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)
