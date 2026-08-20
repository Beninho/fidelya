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
    /**
     * L'alerte a été levée par un enregistrement, pas à l'ouverture. Supprimer le
     * doublon reprend alors l'enregistrement là où il s'était arrêté.
     */
    val duplicateFromSave: Boolean = false,
    /** La carte éditée a été supprimée : l'écran doit revenir à la liste. */
    val isDeleted: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)
