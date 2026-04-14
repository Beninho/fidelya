package com.example.fidelya.data.order

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.cardOrderDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "card_order")

interface CardOrderStore {
    val orderFlow: Flow<List<Long>>
    suspend fun save(ids: List<Long>)
}

class CardOrderStoreImpl(context: Context) : CardOrderStore {
    private val dataStore = context.applicationContext.cardOrderDataStore
    private val orderKey = stringPreferencesKey("ordered_ids")

    override val orderFlow: Flow<List<Long>> = dataStore.data.map { prefs ->
        val raw = prefs[orderKey] ?: return@map emptyList()
        runCatching { Json.decodeFromString<List<Long>>(raw) }.getOrDefault(emptyList())
    }

    override suspend fun save(ids: List<Long>) {
        dataStore.edit { prefs ->
            prefs[orderKey] = Json.encodeToString(ids)
        }
    }
}
