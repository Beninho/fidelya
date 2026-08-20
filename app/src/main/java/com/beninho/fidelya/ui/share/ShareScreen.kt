package com.beninho.fidelya.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.share.CardShareCodec
import com.beninho.fidelya.domain.share.SharedCard
import com.beninho.fidelya.domain.share.toShared
import com.beninho.fidelya.ui.carddetail.CardDetailViewModel
import com.beninho.fidelya.ui.carddetail.cardDetailViewModelFactory
import com.beninho.fidelya.ui.carddetail.encodeBarcode
import com.beninho.fidelya.ui.theme.Archivo
import com.beninho.fidelya.ui.theme.ModernistCodeInk
import com.beninho.fidelya.ui.theme.ModernistCodeSurface
import com.beninho.fidelya.ui.theme.ModernistKeyValue
import com.beninho.fidelya.ui.theme.ModernistOutlinedButton
import com.beninho.fidelya.ui.theme.ModernistSpace
import com.beninho.fidelya.ui.theme.ModernistStrongRule
import com.beninho.fidelya.ui.theme.ModernistTextButton
import com.beninho.fidelya.ui.theme.modernistDividerColor
import kotlinx.coroutines.delay

/**
 * Écran 10 de la maquette — « Partager ».
 *
 * Le code affiché est réémis dès qu'il expire : la maquette annonce « expire
 * dans 10 min », ce qui protège un code photographié ou laissé traîner, mais
 * n'a aucune raison de rendre l'écran inutile sous les yeux de l'utilisateur.
 * C'est le téléphone destinataire qui refuse un code périmé.
 */
@Composable
fun ShareScreen(
    cardId: Long,
    repository: CardRepository,
    onBack: () -> Unit,
    vm: CardDetailViewModel = viewModel(factory = cardDetailViewModelFactory(repository, cardId))
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var issuedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var remainingMs by remember { mutableLongStateOf(SharedCard.VALIDITY_MS) }

    LaunchedEffect(issuedAt) {
        while (true) {
            val elapsed = System.currentTimeMillis() - issuedAt
            if (elapsed >= SharedCard.VALIDITY_MS) {
                issuedAt = System.currentTimeMillis()  // réémission, le code reste scannable
                break
            }
            remainingMs = SharedCard.VALIDITY_MS - elapsed
            delay(1_000)
        }
    }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernistTextButton("Retour", onBack)
                    ModernistTextButton(
                        text = "Expire dans ${(remainingMs / 60_000) + 1} min",
                        onClick = { issuedAt = System.currentTimeMillis() },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ModernistStrongRule()
            }
        }
    ) { padding ->
        val card = state.card
        if (card == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Carte introuvable", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val link = remember(card.id, issuedAt) { CardShareCodec.encode(card.toShared(issuedAt)) }
        val qr = remember(link) { encodeBarcode(link, "QR_CODE", 600, 600) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(
                    text = "Partager ${card.storeName}",
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 30.sp
                )
                Text(
                    text = "Faites scanner ce code depuis l'écran d'ajout de l'autre téléphone. " +
                        "La carte arrive avec son numéro, son format et sa couleur.",
                    modifier = Modifier.padding(top = ModernistSpace.s2),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Blanc dans les deux thèmes, comme le cadre du code-barres.
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(ModernistCodeSurface)
                    .border(1.dp, modernistDividerColor(), RectangleShape)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "Code de partage",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(210.dp)
                    )
                } else {
                    Text("Code impossible à générer", fontSize = 13.sp, color = ModernistCodeInk)
                }
            }
            Column {
                ModernistKeyValue("Carte", card.storeName)
                ModernistKeyValue("Numéro", card.cardNumber, lastInGroup = true)
            }
            ModernistOutlinedButton(
                text = "Copier le lien à la place",
                onClick = { clipboard.setText(AnnotatedString(link)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Le code ne contient que le nom, le numéro, le format et la couleur. " +
                    "Ni le logo, ni quoi que ce soit d'autre ne quitte le téléphone.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(thickness = 1.dp, color = modernistDividerColor())
        }
    }
}
