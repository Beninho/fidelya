package com.example.fidelya.ui.carddetail

import com.example.fidelya.domain.model.LoyaltyCard

data class CardDetailUiState(
    val card: LoyaltyCard? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)
