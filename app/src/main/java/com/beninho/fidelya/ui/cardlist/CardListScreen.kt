package com.beninho.fidelya.ui.cardlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import com.beninho.fidelya.ui.components.CardLogo
import com.beninho.fidelya.ui.theme.*

/** `'···· ' + numéro sans espaces, quatre derniers` — le masque de la maquette. */
fun maskCardNumber(number: String): String =
    "···· " + number.filterNot { it.isWhitespace() }.takeLast(4)

/**
 * Écran 03 de la maquette — la liste dense.
 *
 * Trois choses la distinguent de la grille qu'elle remplace : chaque enseigne
 * porte son logo et sa couleur sur une ligne lisible d'un coup d'œil, la
 * recherche est toujours visible, et l'appui long ouvre directement le
 * code-barres. Ce dernier point libère le geste qui servait à réordonner : c'est
 * désormais le rôle de l'écran 08, atteignable par « Ordre ».
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardListScreen(
    repository: CardRepository,
    cardOrderStore: CardOrderStore,
    logoStore: LogoStore,
    onCardClick: (Long) -> Unit,
    onCardCheckout: (Long) -> Unit,
    onAddClick: () -> Unit,
    onManualEntry: () -> Unit,
    onReorder: () -> Unit,
    onSettings: () -> Unit,
    vm: CardListViewModel = viewModel(
        factory = cardListViewModelFactory(repository, cardOrderStore, logoStore)
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val visible = remember(state.cards, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) state.cards
        else state.cards.filter { it.storeName.lowercase().contains(q) }
    }
    val countLabel = when (val n = state.cards.size) {
        0 -> "0 carte"
        1 -> "1 carte"
        else -> "$n cartes"
    }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Mes cartes",
                        fontFamily = Archivo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.22).sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ModernistTextButton("Ordre", onReorder)
                        ModernistTextButton("Réglages", onSettings)
                    }
                }
                if (state.cards.isNotEmpty()) {
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp)) {
                        ModernistSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = "Chercher une enseigne",
                            trailing = { ModernistCountLabel(countLabel) }
                        )
                        Row(
                            Modifier.padding(top = ModernistSpace.s2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .width(14.dp)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Appui long sur une carte = code-barres direct",
                                modifier = Modifier.padding(start = 6.dp),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                ModernistStrongRule()
            }
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                ModernistStrongRule()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        ModernistBlockButton(text = "Ajouter une carte", onClick = onAddClick)
                    }
                    ModernistOutlinedButton("Saisir", onManualEntry)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                // Écran 02 de la maquette — l'état vide est celui de la liste.
                state.cards.isEmpty() -> EmptyState(onAddClick = onAddClick, onManualEntry = onManualEntry)

                visible.isEmpty() -> Text(
                    text = "Aucune enseigne ne correspond à cette recherche.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 26.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn {
                    items(visible, key = { it.id }) { card ->
                        CardRow(
                            card = card,
                            onClick = { onCardClick(card.id) },
                            onLongClick = { onCardCheckout(card.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit, onManualEntry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Aucune carte pour l'instant",
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            lineHeight = 29.sp
        )
        Text(
            text = "Ajoutez la première : le scan reconnaît le format tout seul " +
                "(EAN-13, QR, Code 128…).",
            modifier = Modifier.padding(top = 14.dp),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ModernistDivider(Modifier.padding(vertical = 20.dp))
        ModernistBlockButton(text = "Scanner une carte", onClick = onAddClick)
        ModernistOutlinedButton(
            text = "Saisir manuellement",
            onClick = onManualEntry,
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardRow(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = parseCardColor(card.backgroundColor)

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(modernistRowSurface())
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(end = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Le liseré de couleur : ce qui rend la liste balayable de l'œil.
            Box(Modifier.width(6.dp).height(52.dp).background(bg, RectangleShape))
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .size(44.dp)
                    .background(bg, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                CardLogo(
                    logoPath = card.logoUri,
                    fallbackText = card.logoEmoji ?: card.storeName.take(1).uppercase(),
                    // La maquette pose du blanc pur ; Modernist ne le fait jamais sur
                    // une couleur, et l'encre calculée tient le contraste sur la rampe.
                    fallbackColor = cardForegroundColor(bg),
                    size = 44.dp,
                    fallbackFontSize = 19.sp
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = card.storeName,
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = maskCardNumber(card.cardNumber),
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ModernistTag(card.barcodeFormat)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(start = 10.dp).size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(thickness = 1.dp, color = modernistRowDividerColor())
    }
}
