package com.example.fidcard.ui.cardlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardListViewModel(private val repository: CardRepository) : ViewModel() {
    val uiState: StateFlow<CardListUiState> = repository.observeAll()
        .map { CardListUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())

    fun deleteCard(card: LoyaltyCard) {
        viewModelScope.launch { repository.delete(card) }
    }

    fun importCards(cards: List<LoyaltyCard>) {
        viewModelScope.launch { repository.insertAll(cards) }
    }
}

fun cardListViewModelFactory(repository: CardRepository) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardListViewModel(repository) as T
        }
    }
