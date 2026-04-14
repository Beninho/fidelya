package com.beninho.fidelya.ui.cardedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CardEditViewModel(
    private val repository: CardRepository,
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
        if (cardId > 0) {
            viewModelScope.launch {
                repository.getById(cardId)?.let { card ->
                    _uiState.update {
                        it.copy(
                            storeName = card.storeName,
                            cardNumber = card.cardNumber,
                            barcodeFormat = card.barcodeFormat,
                            backgroundColor = card.backgroundColor,
                            logoEmoji = card.logoEmoji ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onStoreNameChange(value: String) =
        _uiState.update { it.copy(storeName = value, storeNameError = null) }
    fun onCardNumberChange(value: String) =
        _uiState.update { it.copy(cardNumber = value, cardNumberError = null) }
    fun onFormatChange(value: String) =
        _uiState.update { it.copy(barcodeFormat = value) }
    fun onColorChange(value: String) =
        _uiState.update { it.copy(backgroundColor = value) }
    fun onEmojiChange(value: String) =
        _uiState.update { it.copy(logoEmoji = value) }

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
                repository.save(
                    LoyaltyCard(
                        id = if (cardId > 0) cardId else 0,
                        storeName = s.storeName.trim(),
                        cardNumber = s.cardNumber.trim(),
                        barcodeFormat = s.barcodeFormat,
                        backgroundColor = s.backgroundColor,
                        logoEmoji = s.logoEmoji.ifBlank { null }
                    )
                )
                _uiState.update { it.copy(isSaved = true) }
            } finally {
                isSaving.set(false)
            }
        }
    }

    fun onSavedConsumed() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

fun cardEditViewModelFactory(
    repository: CardRepository,
    cardId: Long,
    prefilledCardNumber: String?,
    prefilledFormat: String?
) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardEditViewModel(repository, cardId, prefilledCardNumber, prefilledFormat) as T
    }
}
