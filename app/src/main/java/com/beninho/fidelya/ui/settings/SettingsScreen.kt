package com.beninho.fidelya.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.backup.BackupManager
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.data.settings.ThemeChoice
import com.beninho.fidelya.ui.cardlist.CardListViewModel
import com.beninho.fidelya.ui.cardlist.cardListViewModelFactory
import com.beninho.fidelya.ui.theme.Archivo
import com.beninho.fidelya.ui.theme.ModernistListRow
import com.beninho.fidelya.ui.theme.ModernistScreenHeader
import com.beninho.fidelya.ui.theme.ModernistSegmented
import com.beninho.fidelya.ui.theme.ModernistSpace
import com.beninho.fidelya.ui.theme.ModernistSquareToggle
import com.beninho.fidelya.ui.theme.modernistDividerColor
import kotlinx.coroutines.launch

/** L'ordre des libellés suit celui de [ThemeChoice], pour que l'index du segment soit l'ordinal. */
private val THEME_LABELS = listOf("Système", "Clair", "Sombre")

/**
 * Écran 09 de la maquette — « Réglages ». C'est là que la maquette place
 * Exporter et Importer, qui vivaient jusqu'ici dans le menu `...` de la liste.
 *
 * Le réglage d'apparence, en revanche, n'est pas prévu par la maquette : le
 * design system Modernist est `band: "light"` et n'a pas de thème sombre. Il est
 * rendu avec `.seg`, le contrôle segmenté du design system, faute d'un idiome
 * dédié au choix à trois états.
 */
@Composable
fun SettingsScreen(
    repository: CardRepository,
    cardOrderStore: CardOrderStore,
    logoStore: LogoStore,
    theme: ThemeChoice,
    brightnessBoost: Boolean,
    onThemeChange: (ThemeChoice) -> Unit,
    onBrightnessBoostChange: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    vm: CardListViewModel = viewModel(
        factory = cardListViewModelFactory(repository, cardOrderStore, logoStore)
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        vm.exportCards(uri, context.contentResolver)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                vm.importCards(BackupManager.import(context, uri))
            }.onFailure {
                Toast.makeText(context, "Fichier invalide", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val countLabel = when (val n = state.cards.size) {
        0 -> "aucune carte"
        1 -> "1 carte"
        else -> "$n cartes"
    }

    Scaffold(
        topBar = {
            ModernistScreenHeader(
                title = "Réglages",
                backLabel = "Mes cartes",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    text = "Apparence",
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "« Système » suit le réglage Android",
                    modifier = Modifier.padding(top = 2.dp, bottom = ModernistSpace.s2),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ModernistSegmented(
                    options = THEME_LABELS,
                    selectedIndex = theme.ordinal,
                    onSelect = { index -> onThemeChange(ThemeChoice.entries[index]) }
                )
            }
            HorizontalDivider(thickness = 1.dp, color = modernistDividerColor())

            ModernistListRow(
                title = "Luminosité maximale en caisse",
                subtitle = "Remonte l'écran pendant l'affichage du code",
                trailing = {
                    ModernistSquareToggle(
                        checked = brightnessBoost,
                        onCheckedChange = onBrightnessBoostChange,
                        contentDescription = "Luminosité maximale en caisse"
                    )
                }
            )
            ModernistListRow(
                title = "Exporter mes cartes",
                subtitle = "Fichier JSON — $countLabel",
                onClick = { exportLauncher.launch("fidelya_backup.json") }
            )
            ModernistListRow(
                title = "Importer un fichier",
                subtitle = "Fusionne avec les cartes existantes",
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )
            ModernistListRow(
                title = "À propos",
                subtitle = "Confidentialité, soutien au projet, mentions",
                onClick = onAbout
            )
            // Formulation alignée sur l'écran « À propos », qui détaille les nuances :
            // ce sont les *cartes* qui ne bougent pas, ML Kit ayant sa propre
            // télémétrie que l'app ne contrôle pas.
            Text(
                text = "Aucun compte, aucun serveur Fidelya : vos cartes ne " +
                    "quittent pas le téléphone.",
                modifier = Modifier.padding(18.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
