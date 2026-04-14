package com.beninho.fidelya.ui.cardlist

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beninho.fidelya.backup.BackupManager
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CardListViewModel(
    private val repository: CardRepository,
    private val cardOrderStore: CardOrderStore
) : ViewModel() {

    val uiState: StateFlow<CardListUiState> =
        repository.observeAll()
            .combine(cardOrderStore.orderFlow) { cards, orderedIds ->
                CardListUiState(applyOrder(cards, orderedIds))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())

    fun onMove(from: Int, to: Int) {
        // Reads last-emitted state; rapid successive moves may lose intermediary order until DataStore round-trip completes
        val current = uiState.value.cards.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        current.add(to, current.removeAt(from))
        viewModelScope.launch { cardOrderStore.save(current.map { it.id }) }
    }

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

    private fun applyOrder(cards: List<LoyaltyCard>, orderedIds: List<Long>): List<LoyaltyCard> {
        if (orderedIds.isEmpty()) return cards
        val indexMap = orderedIds.withIndex().associate { (i, id) -> id to i }
        return cards.sortedBy { indexMap[it.id] ?: orderedIds.size }
    }
}

fun cardListViewModelFactory(repository: CardRepository, cardOrderStore: CardOrderStore) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CardListViewModel(repository, cardOrderStore) as T
        }
    }
