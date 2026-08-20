package com.beninho.fidelya.ui.reorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.data.logo.LogoStore
import com.beninho.fidelya.data.order.CardOrderStore
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.ui.cardlist.CardListViewModel
import com.beninho.fidelya.ui.cardlist.cardListViewModelFactory
import com.beninho.fidelya.ui.components.CardLogo
import com.beninho.fidelya.ui.theme.ModernistAlpha
import com.beninho.fidelya.ui.theme.ModernistBlockButton
import com.beninho.fidelya.ui.theme.ModernistDivider
import com.beninho.fidelya.ui.theme.ModernistListRow
import com.beninho.fidelya.ui.theme.ModernistScreenHeader
import com.beninho.fidelya.ui.theme.ModernistSpace
import com.beninho.fidelya.ui.theme.ModernistSquareIconButton
import com.beninho.fidelya.ui.theme.cardForegroundColor
import com.beninho.fidelya.ui.theme.parseCardColor

/**
 * Écran 08 de la maquette — « Ordre des cartes ».
 *
 * Le glissement à l'appui long existait déjà dans la grille, mais sans aucun
 * indice visuel : personne ne le découvrait. Cet écran rend l'opération
 * explicite, avec une flèche par direction et une sortie franche.
 */
@Composable
fun ReorderScreen(
    repository: CardRepository,
    cardOrderStore: CardOrderStore,
    logoStore: LogoStore,
    onDone: () -> Unit,
    vm: CardListViewModel = viewModel(
        factory = cardListViewModelFactory(repository, cardOrderStore, logoStore)
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ModernistScreenHeader(
                title = "Ordre des cartes",
                backLabel = "Mes cartes",
                onBack = onDone
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                ModernistDivider()
                ModernistBlockButton(
                    text = "Terminé",
                    onClick = onDone,
                    modifier = Modifier.padding(ModernistSpace.s4)
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Les deux premières cartes sont celles qu'on sort le plus souvent : " +
                    "gardez-les en tête de liste.",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ModernistDivider()
            LazyColumn(Modifier.weight(1f)) {
                items(state.cards, key = { it.id }) { card ->
                    val index = state.cards.indexOfFirst { it.id == card.id }
                    val bg = parseCardColor(card.backgroundColor)
                    ModernistListRow(
                        title = card.storeName,
                        leading = {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(bg, RectangleShape)
                            ) {
                                CardLogo(
                                    logoPath = card.logoUri,
                                    fallbackText = card.logoEmoji
                                        ?: card.storeName.take(1).uppercase(),
                                    fallbackColor = cardForegroundColor(bg),
                                    size = 34.dp,
                                    fallbackFontSize = 15.sp
                                )
                            }
                        },
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(ModernistSpace.s2)) {
                                ModernistSquareIconButton(
                                    icon = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Monter ${card.storeName}",
                                    onClick = { vm.moveUp(index) },
                                    enabled = index > 0
                                )
                                ModernistSquareIconButton(
                                    icon = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Descendre ${card.storeName}",
                                    onClick = { vm.moveDown(index) },
                                    enabled = index < state.cards.lastIndex
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
