package com.example.fidcard.ui.cardlist

import com.example.fidcard.domain.model.LoyaltyCard

data class CardListUiState(val cards: List<LoyaltyCard> = emptyList())
