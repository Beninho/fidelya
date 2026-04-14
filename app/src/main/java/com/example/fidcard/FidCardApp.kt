package com.example.fidcard

import android.app.Application
import com.example.fidcard.data.db.AppDatabase
import com.example.fidcard.data.order.CardOrderStore
import com.example.fidcard.data.order.CardOrderStoreImpl
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.data.repository.CardRepositoryImpl

class FidCardApp : Application() {
    val repository: CardRepository by lazy {
        CardRepositoryImpl(AppDatabase.getInstance(this).loyaltyCardDao())
    }
    val cardOrderStore: CardOrderStore by lazy {
        CardOrderStoreImpl(this)
    }
}
