package com.beninho.fidelya.ui.carddetail

import com.beninho.fidelya.domain.model.LoyaltyCard

data class CardDetailUiState(
    val card: LoyaltyCard? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)
