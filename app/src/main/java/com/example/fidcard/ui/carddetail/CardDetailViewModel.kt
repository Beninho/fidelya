package com.example.fidcard.ui.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fidcard.data.repository.CardRepository
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
            _uiState.value.card?.let { repository.delete(it) }
            _uiState.update { it.copy(isDeleted = true) }
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
