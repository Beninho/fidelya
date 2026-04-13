package com.example.fidcard.ui.cardlist

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fidcard.backup.BackupManager
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

    fun exportCards(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val json = BackupManager.export(repository.getAll())
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
        }
    }
}

fun cardListViewModelFactory(repository: CardRepository) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardListViewModel(repository) as T
        }
    }
