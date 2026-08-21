package com.beninho.fidelya

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beninho.fidelya.data.settings.AppSettings
import com.beninho.fidelya.data.settings.ThemeChoice
import com.beninho.fidelya.domain.model.LoyaltyCard
import com.beninho.fidelya.domain.share.CardShareCodec
import com.beninho.fidelya.ui.about.AboutScreen
import com.beninho.fidelya.ui.cardedit.CardEditScreen
import com.beninho.fidelya.ui.carddetail.CardDetailScreen
import com.beninho.fidelya.ui.cardlist.CardListScreen
import com.beninho.fidelya.ui.onboarding.OnboardingScreen
import com.beninho.fidelya.ui.reorder.ReorderScreen
import com.beninho.fidelya.ui.scan.ScanScreen
import com.beninho.fidelya.ui.settings.SettingsScreen
import com.beninho.fidelya.ui.share.ReceiveScreen
import com.beninho.fidelya.ui.share.ShareScreen
import com.beninho.fidelya.ui.theme.FidelyaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = applicationContext as FidelyaApp
            val settings by app.settingsStore.settingsFlow
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            FidelyaTheme(
                darkTheme = when (settings.theme) {
                    ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                    ThemeChoice.LIGHT -> false
                    ThemeChoice.DARK -> true
                }
            ) {
                // `onboardingSeen` vaut null tant que DataStore n'a pas répondu : on
                // attend, sinon l'accueil clignoterait au lancement de chaque session.
                val seen = settings.onboardingSeen
                if (seen == null) {
                    Surface(Modifier.fillMaxSize()) {}
                } else {
                    // Figé à la première composition : `startDestination` est une clé du
                    // graphe de navigation. Le laisser suivre la préférence reconstruisait
                    // le graphe au moment où l'accueil marque « vu », ce qui écrasait la
                    // navigation déclenchée dans le même geste — on atterrissait sur la
                    // liste vide au lieu du formulaire.
                    val startDestination = remember { if (seen) "cardList" else "onboarding" }
                    FidelyaNavHost(settings, startDestination)
                }
            }
        }
    }
}

@Composable
fun FidelyaNavHost(settings: AppSettings, startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as FidelyaApp
    val scope = rememberCoroutineScope()

    /** Supprime une carte et son logo — le fichier survivrait à la ligne en base. */
    fun deleteCard(card: LoyaltyCard) {
        scope.launch {
            app.repository.delete(card)
            app.logoStore.delete(card.logoUri)
        }
    }

    /** Une carte reçue par partage n'a pas d'identifiant : `save` l'insère. */
    fun addShared(card: LoyaltyCard) {
        scope.launch { app.repository.save(card) }
        navController.navigate("cardList") { popUpTo("cardList") { inclusive = true } }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onScan = {
                    scope.launch { app.settingsStore.markOnboardingSeen() }
                    navController.navigate("scan") { popUpTo("onboarding") { inclusive = true } }
                },
                onManualEntry = {
                    scope.launch { app.settingsStore.markOnboardingSeen() }
                    navController.navigate("cardEdit/-1") { popUpTo("onboarding") { inclusive = true } }
                }
            )
        }
        composable("cardList") {
            CardListScreen(
                repository = app.repository,
                cardOrderStore = app.cardOrderStore,
                logoStore = app.logoStore,
                onCardClick = { id -> navController.navigate("cardDetail/$id") },
                onCardCheckout = { id -> navController.navigate("cardDetail/$id?checkout=true") },
                onAddClick = { navController.navigate("scan") },
                onManualEntry = { navController.navigate("cardEdit/-1") },
                onReorder = { navController.navigate("reorder") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable(
            "cardDetail/{id}?checkout={checkout}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("checkout") { type = NavType.BoolType; defaultValue = false }
            )
        ) { back ->
            CardDetailScreen(
                cardId = back.arguments!!.getLong("id"),
                repository = app.repository,
                brightnessBoost = settings.brightnessBoost,
                startInCheckout = back.arguments?.getBoolean("checkout") == true,
                onEditClick = { id -> navController.navigate("cardEdit/$id") },
                onShareClick = { id -> navController.navigate("share/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "cardEdit/{id}?cardNumber={cardNumber}&format={format}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("cardNumber") { type = NavType.StringType; defaultValue = ""; nullable = true },
                navArgument("format") { type = NavType.StringType; defaultValue = "QR_CODE"; nullable = true }
            )
        ) { back ->
            CardEditScreen(
                cardId = back.arguments!!.getLong("id"),
                prefilledCardNumber = back.arguments?.getString("cardNumber")?.ifBlank { null },
                prefilledFormat = back.arguments?.getString("format"),
                repository = app.repository,
                logoStore = app.logoStore,
                onSaved = { navController.popBackStack("cardList", false) },
                onBack = { navController.popBackStack() },
                // Sans `popUpTo` : le retour depuis le détail ramène au formulaire
                // encore rempli, au cas où l'utilisateur voulait juste comparer.
                onOpenDuplicate = { id -> navController.navigate("cardDetail/$id") }
            )
        }
        composable("reorder") {
            ReorderScreen(
                repository = app.repository,
                cardOrderStore = app.cardOrderStore,
                logoStore = app.logoStore,
                onDone = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                repository = app.repository,
                cardOrderStore = app.cardOrderStore,
                logoStore = app.logoStore,
                theme = settings.theme,
                brightnessBoost = settings.brightnessBoost,
                onThemeChange = { choice -> scope.launch { app.settingsStore.saveTheme(choice) } },
                onBrightnessBoostChange = { enabled ->
                    scope.launch { app.settingsStore.saveBrightnessBoost(enabled) }
                },
                onAbout = { navController.navigate("about") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "share/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { back ->
            ShareScreen(
                cardId = back.arguments!!.getLong("id"),
                repository = app.repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "receive?payload={payload}",
            arguments = listOf(navArgument("payload") { type = NavType.StringType; defaultValue = "" })
        ) { back ->
            ReceiveScreen(
                payload = back.arguments?.getString("payload") ?: "",
                repository = app.repository,
                onAccept = ::addShared,
                onReject = { navController.popBackStack("cardList", false) },
                onOpenDuplicate = { id -> navController.navigate("cardDetail/$id") },
                onDeleteDuplicate = ::deleteCard
            )
        }
        composable("scan") {
            ScanScreen(
                onBarcodeDetected = { number, format ->
                    // Un QR de partage Fidelya ne mène pas au formulaire mais à l'écran 11.
                    if (CardShareCodec.decode(number) != null) {
                        navController.navigate("receive?payload=${Uri.encode(number)}") {
                            popUpTo("scan") { inclusive = true }
                        }
                    } else {
                        navController.navigate(
                            "cardEdit/-1?cardNumber=${Uri.encode(number)}&format=$format"
                        ) {
                            popUpTo("scan") { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
