package com.beninho.fidelya

import android.app.Application
import com.beninho.fidelya.data.db.AppDatabase
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.order.CardOrderStoreImpl
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.data.repository.CardRepositoryImpl

class FidelyaApp : Application() {
    val repository: CardRepository by lazy {
        CardRepositoryImpl(AppDatabase.getInstance(this).loyaltyCardDao())
    }
    val cardOrderStore: CardOrderStore by lazy {
        CardOrderStoreImpl(this)
    }
}
