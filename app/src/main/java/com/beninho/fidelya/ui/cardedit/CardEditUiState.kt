package com.beninho.fidelya.ui.cardedit

import com.beninho.fidelya.domain.color.DEFAULT_CARD_COLOR
import com.beninho.fidelya.domain.model.LoyaltyCard

data class CardEditUiState(
    val storeName: String = "",
    val cardNumber: String = "",
    val barcodeFormat: String = "QR_CODE",
    val backgroundColor: String = DEFAULT_CARD_COLOR,
    val logoEmoji: String = "",
    /** Chemin local du logo, `null` si la carte n'en a pas. */
    val logoPath: String? = null,
    val storeNameError: String? = null,
    val cardNumberError: String? = null,
    /** La carte qui porte déjà ce numéro ; non nul = l'alerte de doublon est affichée. */
    val duplicate: LoyaltyCard? = null,
    /** L'utilisateur a accepté le doublon : le prochain enregistrement passe outre. */
    val duplicateAccepted: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)
