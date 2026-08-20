package com.beninho.fidelya.ui.cardedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.barcode.SupportedBarcodeFormats
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.ui.components.CardLogo
import com.beninho.fidelya.ui.theme.CardPaletteRows
import com.beninho.fidelya.ui.theme.ModernistBlockButton
import com.beninho.fidelya.ui.theme.ModernistDivider
import com.beninho.fidelya.ui.theme.ModernistAlpha
import com.beninho.fidelya.ui.theme.ModernistSectionLabel
import com.beninho.fidelya.ui.theme.ModernistSelectField
import com.beninho.fidelya.ui.theme.ModernistSpace
import com.beninho.fidelya.ui.theme.ModernistTextField
import com.beninho.fidelya.ui.theme.cardForegroundColor
import com.beninho.fidelya.ui.theme.parseCardColor

val PALETTE_ROWS = CardPaletteRows
val FORMATS = SupportedBarcodeFormats.names.toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditScreen(
    cardId: Long,
    repository: CardRepository,
    logoStore: LogoStore,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    prefilledCardNumber: String? = null,
    prefilledFormat: String? = null,
    vm: CardEditViewModel = viewModel(
        factory = cardEditViewModelFactory(
            repository, logoStore, cardId, prefilledCardNumber, prefilledFormat
        )
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Sélecteur de photos système : aucune permission de stockage à demander.
    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(vm::onLogoPicked) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            vm.onSavedConsumed()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
          // `.nav` porte une bordure basse de 2px en `--color-divider`.
          Column {
            TopAppBar(
                title = { Text(if (cardId > 0) "Modifier la carte" else "Nouvelle carte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
            ModernistDivider()
          }
        },
        bottomBar = {
          // `.btn-block` reste pleine largeur, aligné à gauche et en accent plein ;
          // seule sa position change. Inline, la grille de neuf familles le poussait
          // sous la ligne de flottaison : on cliquait « Enregistrer » sans voir les
          // erreurs de saisie, restées en haut du formulaire. Le filet reprend `.hr`.
          Column(Modifier.navigationBarsPadding()) {
            ModernistDivider()
            ModernistBlockButton(
                text = "Enregistrer",
                onClick = vm::save,
                modifier = Modifier.padding(ModernistSpace.s4)
            )
          }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernistTextField(
                value = state.storeName,
                onValueChange = vm::onStoreNameChange,
                label = "Nom du magasin",
                error = state.storeNameError,
                modifier = Modifier.fillMaxWidth()
            )
            ModernistTextField(
                value = state.cardNumber,
                onValueChange = vm::onCardNumberChange,
                label = "Numéro de carte",
                error = state.cardNumberError,
                modifier = Modifier.fillMaxWidth()
            )
            ModernistTextField(
                value = state.logoEmoji,
                onValueChange = vm::onEmojiChange,
                label = "Emoji (optionnel)",
                modifier = Modifier.fillMaxWidth()
            )
            var formatExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it }
            ) {
                ModernistSelectField(
                    value = state.barcodeFormat,
                    label = "Format du code",
                    trailing = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false }
                ) {
                    FORMATS.forEach { fmt ->
                        DropdownMenuItem(
                            text = { Text(fmt) },
                            onClick = { vm.onFormatChange(fmt); formatExpanded = false }
                        )
                    }
                }
            }
            ModernistSectionLabel("Logo et couleur")
            Row(
                horizontalArrangement = Arrangement.spacedBy(ModernistSpace.s3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val slotColor = parseCardColor(state.backgroundColor)
                Box(
                    Modifier
                        .size(64.dp)
                        .background(slotColor, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RectangleShape)
                        .clickable {
                            logoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CardLogo(
                        logoPath = state.logoPath,
                        fallbackText = state.logoEmoji.ifBlank {
                            state.storeName.take(1).uppercase().ifBlank { "+" }
                        },
                        fallbackColor = cardForegroundColor(slotColor),
                        size = 64.dp,
                        fallbackFontSize = 26.sp
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (state.logoPath == null) "Choisir un logo"
                        else "Remplacer le logo",
                        modifier = Modifier.clickable {
                            logoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.logoPath != null) {
                        Text(
                            text = "Retirer",
                            modifier = Modifier
                                .padding(top = ModernistSpace.s1)
                                .clickable(onClick = vm::onLogoCleared),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Le logo n'est jamais téléversé : il est stocké avec la carte " +
                            "sur le téléphone.",
                        modifier = Modifier.padding(top = ModernistSpace.s1),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ModernistAlpha.Muted)
                    )
                }
            }
            // Une ligne par famille de teintes, du pas clair au pas soutenu : la grille
            // se lit comme la palette dont elle est tirée. Un FlowRow ne le garantirait
            // pas — sa coupure dépend de la largeur d'écran et scinderait les familles.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PALETTE_ROWS.forEach { family ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        family.forEach { hex ->
                            val color = parseCardColor(hex)
                            val selected = state.backgroundColor == hex
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(color, RectangleShape)
                                    // Liseré permanent : plusieurs pas tombent sur le fond du
                                    // thème (#2D2B2B est exactement Neutral900 en sombre) et
                                    // disparaîtraient sans lui. La sélection reprend la couleur
                                    // du texte de la carte : sur les pas clairs de la rampe, un
                                    // liseré blanc serait invisible.
                                    .border(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) cardForegroundColor(color)
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        RectangleShape
                                    )
                                    .clickable { vm.onColorChange(hex) }
                            )
                        }
                    }
                }
            }
        }
    }
}
