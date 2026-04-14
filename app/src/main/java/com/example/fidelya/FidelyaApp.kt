package com.example.fidelya

import android.app.Application
import com.example.fidelya.data.db.AppDatabase
import com.example.fidelya.data.order.CardOrderStore
import com.example.fidelya.data.order.CardOrderStoreImpl
import com.example.fidelya.data.repository.CardRepository
import com.example.fidelya.data.repository.CardRepositoryImpl

class FidelyaApp : Application() {
    val repository: CardRepository by lazy {
        CardRepositoryImpl(AppDatabase.getInstance(this).loyaltyCardDao())
    }
    val cardOrderStore: CardOrderStore by lazy {
        CardOrderStoreImpl(this)
    }
}
