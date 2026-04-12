package com.example.fidcard.ui.carddetail

import com.example.fidcard.domain.model.LoyaltyCard

data class CardDetailUiState(
    val card: LoyaltyCard? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)
