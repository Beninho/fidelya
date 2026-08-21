package com.beninho.fidelya.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.domain.model.LoyaltyCard
import com.beninho.fidelya.domain.share.CardShareCodec
import com.beninho.fidelya.domain.share.toDomain
import com.beninho.fidelya.ui.theme.*

/**
 * Écran 11 de la maquette — « Recevoir une carte ».
 *
 * L'écran ne s'affiche qu'après vérification : un code illisible ou périmé mène
 * à un refus explicite plutôt qu'à un formulaire à moitié rempli. La maquette
 * montre aussi une ligne « Partagée par Camille — Pixel 8 » : la charge utile ne
 * transporte aucun expéditeur — et ne doit pas en transporter — donc la ligne
 * n'est pas reprise.
 */
@Composable
fun ReceiveScreen(
    payload: String,
    repository: CardRepository,
    onAccept: (LoyaltyCard) -> Unit,
    onReject: () -> Unit,
    onOpenDuplicate: (Long) -> Unit = {},
    /** Supprime la carte locale qui fait doublon — la base et le logo avec. */
    onDeleteDuplicate: (LoyaltyCard) -> Unit = {}
) {
    val now = remember { System.currentTimeMillis() }
    val shared = remember(payload) { CardShareCodec.decode(payload) }
    val expired = shared != null && CardShareCodec.isExpired(shared, now)

    // Cet écran est déjà l'étape de confirmation : le doublon ne s'annonce donc
    // pas à l'ouverture mais à l'instant de l'ajout.
    var duplicate by remember(payload) { mutableStateOf<LoyaltyCard?>(null) }
    LaunchedEffect(shared, expired) {
        duplicate = if (shared != null && !expired) {
            repository.findDuplicate(shared.toDomain().cardNumber)
        } else {
            null
        }
    }
    var duplicateShown by remember(payload) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernistTextButton("Mes cartes", onReject)
                    ModernistTextButton(
                        text = if (shared == null || expired) "Code refusé" else "Code lu",
                        onClick = {},
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ModernistStrongRule()
            }
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                ModernistDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (shared != null && !expired) {
                        Box(Modifier.weight(1f)) {
                            ModernistBlockButton(
                                text = "Ajouter à mes cartes",
                                onClick = {
                                    if (duplicate != null) duplicateShown = true
                                    else onAccept(shared.toDomain())
                                }
                            )
                        }
                        ModernistOutlinedButton("Refuser", onReject)
                    } else {
                        Box(Modifier.weight(1f)) {
                            ModernistBlockButton(text = "Retour à mes cartes", onClick = onReject)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (shared == null || expired) {
                Text(
                    text = if (expired) "Ce code a expiré" else "Code illisible",
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 30.sp
                )
                Text(
                    text = if (expired) {
                        "Un code de partage n'est valable que dix minutes. Demandez à " +
                            "l'expéditeur d'en afficher un nouveau."
                    } else {
                        "Ce code ne vient pas de Fidelya, ou il a été abîmé au scan. " +
                            "Rien n'a été enregistré."
                    },
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val card = shared.toDomain()
            val bg = parseCardColor(card.backgroundColor)

            Column {
                ModernistSectionLabel("Carte partagée")
                Text(
                    text = card.storeName,
                    modifier = Modifier.padding(top = 6.dp),
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 30.sp
                )
                Text(
                    text = "Envoyée depuis un autre téléphone. Vérifiez le numéro avant " +
                        "d'ajouter : la carte sera enregistrée telle quelle.",
                    modifier = Modifier.padding(top = ModernistSpace.s2),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, modernistDividerColor(), RectangleShape)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(6.dp).height(56.dp).background(bg, RectangleShape))
                Box(
                    Modifier
                        .padding(start = 14.dp)
                        .size(56.dp)
                        .background(bg, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.storeName.take(1).uppercase(),
                        fontFamily = Archivo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 23.sp,
                        color = cardForegroundColor(bg)
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        text = card.storeName,
                        fontFamily = Archivo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = card.cardNumber,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ModernistTag(card.barcodeFormat)
            }

            Column {
                ModernistKeyValue("Transfert", "Local, hors ligne", lastInGroup = true)
            }

            Text(
                text = "Le code ne contient que le nom, le numéro, le format et la couleur. " +
                    "Rien n'est envoyé sur un serveur.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val dup = duplicate
    if (duplicateShown && dup != null && shared != null) {
        ModernistDialog(
            title = "Carte déjà enregistrée",
            body = "La carte « ${dup.storeName} » porte déjà le numéro ${dup.cardNumber}. " +
                "Vous pouvez l'ajouter quand même, ou supprimer celle qui fait doublon.",
            confirmLabel = "Ajouter quand même",
            onConfirm = {
                duplicateShown = false
                onAccept(shared.toDomain())
            },
            dismissLabel = "Annuler",
            onDismiss = { duplicateShown = false },
            secondaryActions = listOf(
                ModernistDialogAction(
                    label = "Voir la carte existante",
                    onClick = { onOpenDuplicate(dup.id) }
                ),
                ModernistDialogAction(
                    label = "Supprimer la carte existante",
                    onClick = {
                        onDeleteDuplicate(dup)
                        // L'alerte n'a plus de raison d'être : la carte locale
                        // vient de partir, l'ajout peut suivre sans avertissement.
                        duplicate = null
                        duplicateShown = false
                    },
                    destructive = true
                )
            )
        )
    }
}
