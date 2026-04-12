package com.example.fidcard.ui.cardedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardEditViewModel(
    private val repository: CardRepository,
    private val cardId: Long,
    prefilledCardNumber: String? = null,
    prefilledFormat: String? = null
) : ViewModel() {
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
        var hasError = false
        if (s.storeName.isBlank()) {
            _uiState.update { it.copy(storeNameError = "Le nom est obligatoire") }
            hasError = true
        }
        if (s.cardNumber.isBlank()) {
            _uiState.update { it.copy(cardNumberError = "Le numéro est obligatoire") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
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
        }
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
