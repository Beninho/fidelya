package com.beninho.fidelya.ui.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beninho.fidelya.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CardDetailViewModel(
    private val repository: CardRepository,
    private val cardId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val card = repository.getById(cardId)
            _uiState.update { it.copy(card = card, isLoading = false) }
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            val card = _uiState.value.card ?: return@launch
            repository.delete(card)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun onDeletedConsumed() = _uiState.update { it.copy(isDeleted = false) }

    /**
     * Horodate le passage en caisse — c'est ce qui alimente « Dernier passage ».
     * L'état local est mis à jour dans la foulée : l'écran ne réinterroge pas la
     * base après un retour du mode caisse.
     */
    fun markUsed() {
        viewModelScope.launch {
            val card = _uiState.value.card ?: return@launch
            val now = System.currentTimeMillis()
            repository.markUsed(card.id)
            _uiState.update { it.copy(card = card.copy(lastUsedAt = now)) }
        }
    }
}

fun cardDetailViewModelFactory(repository: CardRepository, cardId: Long) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardDetailViewModel(repository, cardId) as T
        }
    }
