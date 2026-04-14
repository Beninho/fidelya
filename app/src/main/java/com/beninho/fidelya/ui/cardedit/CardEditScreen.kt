package com.beninho.fidelya.ui.cardedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.data.repository.CardRepository

val PALETTE = listOf(
    // Rouges / Roses
    "#E53935", "#E91E63", "#F06292",
    // Oranges
    "#F57C00", "#FF7043",
    // Jaunes
    "#F9A825", "#FDD835",
    // Verts
    "#388E3C", "#43A047", "#00897B",
    // Bleus
    "#1565C0", "#039BE5", "#00838F",
    // Violets / Indigo
    "#5C6BC0", "#7B1FA2", "#9C27B0",
    // Marrons / Gris chauds
    "#4E342E", "#795548",
    // Gris / Ardoise
    "#37474F", "#546E7A",
    // Noir / Blanc
    "#212121", "#FAFAFA"
)
val FORMATS = listOf("QR_CODE", "EAN_13", "EAN_8", "CODE_128", "CODE_39", "PDF_417", "DATA_MATRIX")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardEditScreen(
    cardId: Long,
    repository: CardRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    prefilledCardNumber: String? = null,
    prefilledFormat: String? = null,
    vm: CardEditViewModel = viewModel(
        factory = cardEditViewModelFactory(repository, cardId, prefilledCardNumber, prefilledFormat)
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            vm.onSavedConsumed()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (cardId > 0) "Modifier la carte" else "Nouvelle carte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
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
            OutlinedTextField(
                value = state.storeName,
                onValueChange = vm::onStoreNameChange,
                label = { Text("Nom du magasin") },
                isError = state.storeNameError != null,
                supportingText = state.storeNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.cardNumber,
                onValueChange = vm::onCardNumberChange,
                label = { Text("Numéro de carte") },
                isError = state.cardNumberError != null,
                supportingText = state.cardNumberError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.logoEmoji,
                onValueChange = vm::onEmojiChange,
                label = { Text("Emoji (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Format du code", style = MaterialTheme.typography.labelMedium)
            var formatExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.barcodeFormat,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
            Text("Couleur de fond", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PALETTE.forEach { hex ->
                    val color = runCatching {
                        Color(android.graphics.Color.parseColor(hex))
                    }.getOrDefault(Color.Gray)
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(color, CircleShape)
                            .then(
                                if (state.backgroundColor == hex)
                                    Modifier.border(3.dp, Color.White, CircleShape)
                                else
                                    Modifier
                            )
                            .clickable { vm.onColorChange(hex) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) {
                Text("Enregistrer")
            }
        }
    }
}
