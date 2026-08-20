package com.beninho.fidelya.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "settings")

/**
 * Le choix d'apparence. `SYSTEM` par défaut : c'est la convention Android, et le
 * seul état qui ne surprend personne au premier lancement.
 *
 * À noter : le design system « Modernist » est `band: "light"` et ne définit pas
 * de thème sombre — celui de l'app est une extension maison, ce réglage aussi.
 * La maquette ne le prévoit pas non plus.
 */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    /** « Luminosité maximale en caisse » : remonte l'écran pendant l'affichage du code. */
    val brightnessBoost: Boolean = true,
    /**
     * L'écran 01 ne se montre qu'une fois. `null` signifie « on ne sait pas
     * encore » : la préférence n'est pas relue, et afficher l'accueil avant de
     * l'avoir lue ferait clignoter l'onboarding à chaque lancement.
     */
    val onboardingSeen: Boolean? = null
)

interface SettingsStore {
    val settingsFlow: Flow<AppSettings>
    suspend fun saveTheme(choice: ThemeChoice)
    suspend fun saveBrightnessBoost(enabled: Boolean)
    suspend fun markOnboardingSeen()
}

class SettingsStoreImpl(context: Context) : SettingsStore {
    private val dataStore = context.applicationContext.settingsDataStore
    private val themeKey = stringPreferencesKey("theme_choice")
    private val brightnessKey = booleanPreferencesKey("brightness_boost")
    private val onboardingKey = booleanPreferencesKey("onboarding_seen")

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            // Une valeur inconnue (downgrade, préférence corrompue) retombe sur le système.
            theme = prefs[themeKey]?.let { raw -> ThemeChoice.entries.firstOrNull { it.name == raw } }
                ?: ThemeChoice.SYSTEM,
            brightnessBoost = prefs[brightnessKey] ?: true,
            onboardingSeen = prefs[onboardingKey] ?: false
        )
    }

    override suspend fun saveTheme(choice: ThemeChoice) {
        dataStore.edit { prefs -> prefs[themeKey] = choice.name }
    }

    override suspend fun saveBrightnessBoost(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[brightnessKey] = enabled }
    }

    override suspend fun markOnboardingSeen() {
        dataStore.edit { prefs -> prefs[onboardingKey] = true }
    }
}
