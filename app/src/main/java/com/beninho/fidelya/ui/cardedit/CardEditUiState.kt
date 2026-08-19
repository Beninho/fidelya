package com.beninho.fidelya.ui.cardedit

import com.beninho.fidelya.domain.color.DEFAULT_CARD_COLOR

data class CardEditUiState(
    val storeName: String = "",
    val cardNumber: String = "",
    val barcodeFormat: String = "QR_CODE",
    val backgroundColor: String = DEFAULT_CARD_COLOR,
    val logoEmoji: String = "",
    val storeNameError: String? = null,
    val cardNumberError: String? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)
