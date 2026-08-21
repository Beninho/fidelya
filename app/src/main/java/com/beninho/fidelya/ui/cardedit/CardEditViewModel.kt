package com.beninho.fidelya.ui.cardedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CardEditViewModel(
    private val repository: CardRepository,
    private val logoStore: LogoStore,
    private val cardId: Long,
    prefilledCardNumber: String? = null,
    prefilledFormat: String? = null
) : ViewModel() {
    private val isSaving = AtomicBoolean(false)
    private val _uiState = MutableStateFlow(
        CardEditUiState(
            cardNumber = prefilledCardNumber ?: "",
            barcodeFormat = prefilledFormat ?: "QR_CODE"
        )
    )
    val uiState: StateFlow<CardEditUiState> = _uiState

    init {
        // Le formulaire ouvert depuis un scan arrive avec le numéro déjà lu : on
        // signale le doublon tout de suite plutôt qu'après la saisie du nom.
        if (!prefilledCardNumber.isNullOrBlank()) {
            viewModelScope.launch {
                repository.findDuplicate(prefilledCardNumber.trim(), excludeId())?.let { dup ->
                    _uiState.update { it.copy(duplicate = dup) }
                }
            }
        }

        if (cardId > 0) {
            viewModelScope.launch {
                repository.getById(cardId)?.let { card ->
                    _uiState.update {
                        it.copy(
                            storeName = card.storeName,
                            cardNumber = card.cardNumber,
                            barcodeFormat = card.barcodeFormat,
                            backgroundColor = card.backgroundColor,
                            logoEmoji = card.logoEmoji ?: "",
                            logoPath = card.logoUri
                        )
                    }
                }
            }
        }
    }

    fun onStoreNameChange(value: String) =
        _uiState.update { it.copy(storeName = value, storeNameError = null) }
    // Un numéro retouché invalide l'acceptation précédente : sans ce reset, on
    // pourrait accepter un doublon puis coller un autre numéro déjà pris.
    fun onCardNumberChange(value: String) =
        _uiState.update {
            it.copy(
                cardNumber = value,
                cardNumberError = null,
                duplicate = null,
                duplicateAccepted = false
            )
        }
    fun onFormatChange(value: String) =
        _uiState.update { it.copy(barcodeFormat = value) }
    fun onColorChange(value: String) =
        _uiState.update { it.copy(backgroundColor = value) }
    fun onEmojiChange(value: String) =
        _uiState.update { it.copy(logoEmoji = value) }

    /**
     * Recopie l'image choisie dans le stockage interne. L'ancien fichier part
     * dans la foulée : sans ça, changer de logo laisserait un orphelin.
     */
    fun onLogoPicked(source: Uri) {
        viewModelScope.launch {
            val stored = logoStore.store(source) ?: return@launch
            val previous = _uiState.value.logoPath
            _uiState.update { it.copy(logoPath = stored) }
            if (previous != stored) logoStore.delete(previous)
        }
    }

    fun onLogoCleared() {
        val previous = _uiState.value.logoPath
        _uiState.update { it.copy(logoPath = null) }
        logoStore.delete(previous)
    }

    fun save() {
        val s = _uiState.value
        val storeNameError = if (s.storeName.isBlank()) "Le nom est obligatoire" else null
        val cardNumberError = if (s.cardNumber.isBlank()) "Le numéro est obligatoire" else null

        if (storeNameError != null || cardNumberError != null) {
            _uiState.update { it.copy(storeNameError = storeNameError, cardNumberError = cardNumberError) }
            return
        }

        if (!isSaving.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                if (!s.duplicateAccepted) {
                    // La recherche est dans la coroutine d'enregistrement, pas avant :
                    // ça ferme la fenêtre entre le contrôle et l'insert.
                    val dup = repository.findDuplicate(s.cardNumber.trim(), excludeId())
                    if (dup != null) {
                        _uiState.update { it.copy(duplicate = dup, duplicateFromSave = true) }
                        return@launch
                    }
                }

                repository.save(
                    LoyaltyCard(
                        id = if (cardId > 0) cardId else 0,
                        storeName = s.storeName.trim(),
                        cardNumber = s.cardNumber.trim(),
                        barcodeFormat = s.barcodeFormat,
                        backgroundColor = s.backgroundColor,
                        logoUri = s.logoPath,
                        logoEmoji = s.logoEmoji.ifBlank { null }
                    )
                )
                _uiState.update { it.copy(isSaved = true) }
            } finally {
                isSaving.set(false)
            }
        }
    }

    /** « Enregistrer quand même » : on ferme l'alerte et on relance l'enregistrement. */
    fun onDuplicateAccepted() {
        _uiState.update { it.copy(duplicate = null, duplicateAccepted = true) }
        save()
    }

    /** « Annuler » : l'alerte se ferme, le formulaire reste ouvert pour corriger. */
    fun onDuplicateDismissed() {
        _uiState.update { it.copy(duplicate = null, duplicateFromSave = false) }
    }

    /**
     * « Supprimer l'autre carte » : c'est celle-ci qu'on garde. Si l'alerte venait
     * d'un enregistrement, il reprend tout seul — le doublon n'existe plus.
     */
    fun onDeleteDuplicate() {
        val state = _uiState.value
        val dup = state.duplicate ?: return
        val resumeSave = state.duplicateFromSave
        _uiState.update { it.copy(duplicate = null, duplicateFromSave = false) }
        viewModelScope.launch {
            repository.delete(dup)
            logoStore.delete(dup.logoUri)
            if (resumeSave) save()
        }
    }

    /**
     * « Supprimer cette carte » : l'autre fait déjà le travail. Réservé à
     * l'édition — en création, il n'y a rien en base à supprimer, `Annuler` suffit.
     */
    fun onDeleteSelf() {
        if (cardId <= 0) return
        _uiState.update { it.copy(duplicate = null, duplicateFromSave = false) }
        viewModelScope.launch {
            val card = repository.getById(cardId) ?: return@launch
            repository.delete(card)
            // Deux chemins possibles : celui en base, et celui d'un logo choisi
            // dans le formulaire mais pas encore enregistré.
            logoStore.delete(card.logoUri)
            val pending = _uiState.value.logoPath
            if (pending != card.logoUri) logoStore.delete(pending)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun onDeletedConsumed() {
        _uiState.update { it.copy(isDeleted = false) }
    }

    fun onSavedConsumed() {
        _uiState.update { it.copy(isSaved = false) }
    }

    /** En édition, la carte modifiée ne doit pas se signaler comme son propre doublon. */
    private fun excludeId(): Long = if (cardId > 0) cardId else 0L
}

fun cardEditViewModelFactory(
    repository: CardRepository,
    logoStore: LogoStore,
    cardId: Long,
    prefilledCardNumber: String?,
    prefilledFormat: String?
) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardEditViewModel(
            repository, logoStore, cardId, prefilledCardNumber, prefilledFormat
        ) as T
    }
}
