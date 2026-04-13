package com.example.fidcard.ui.cardlist

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fidcard.backup.BackupManager
import com.example.fidcard.data.repository.CardRepository
import com.example.fidcard.domain.model.LoyaltyCard
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    repository: CardRepository,
    onCardClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    vm: CardListViewModel = viewModel(factory = cardListViewModelFactory(repository))
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
                val cards = BackupManager.import(context, uri)
                vm.importCards(cards)
            }.onFailure {
                Toast.makeText(context, "Fichier invalide", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes cartes") },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Exporter") },
                            onClick = {
                                menuExpanded = false
                                exportLauncher.launch("fidcard_backup.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Importer") },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        if (state.cards.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucune carte. Appuyez sur + pour scanner.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(state.cards, key = { it.id }) { card ->
                    LoyaltyCardItem(
                        card = card,
                        onClick = { onCardClick(card.id) },
                        onDelete = { vm.deleteCard(card) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoyaltyCardItem(card: LoyaltyCard, onClick: () -> Unit, onDelete: () -> Unit) {
    val bgColor = runCatching {
        Color(android.graphics.Color.parseColor(card.backgroundColor))
    }.getOrDefault(Color.Gray)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = card.logoEmoji ?: card.storeName.take(1).uppercase(),
                fontSize = 28.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    card.storeName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "···· ${card.cardNumber.takeLast(4)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
