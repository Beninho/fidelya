package com.beninho.fidelya

import android.app.Application
import com.beninho.fidelya.data.db.AppDatabase
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.logo.LogoStoreImpl
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.order.CardOrderStoreImpl
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.data.repository.CardRepositoryImpl
import com.beninho.fidelya.data.settings.SettingsStore
import com.beninho.fidelya.data.settings.SettingsStoreImpl

class FidelyaApp : Application() {
    val repository: CardRepository by lazy {
        CardRepositoryImpl(AppDatabase.getInstance(this).loyaltyCardDao())
    }
    val cardOrderStore: CardOrderStore by lazy {
        CardOrderStoreImpl(this)
    }
    val logoStore: LogoStore by lazy {
        LogoStoreImpl(this)
    }
    val settingsStore: SettingsStore by lazy {
        SettingsStoreImpl(this)
    }
}
