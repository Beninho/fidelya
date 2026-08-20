package com.beninho.fidelya.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beninho.fidelya.ui.theme.Archivo
import com.beninho.fidelya.ui.theme.ModernistSpace

/**
 * Écran 01 de la maquette — le seul écran de l'app entièrement en accent.
 *
 * Ne s'affiche qu'au premier lancement : la suite est pilotée par
 * `AppSettings.onboardingSeen`.
 */
@Composable
fun OnboardingScreen(
    onScan: () -> Unit,
    onManualEntry: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onPrimary

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            // Pas de Scaffold ici : l'écran porte lui-même les insets système.
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 34.dp, bottom = 24.dp)
    ) {
        Text(
            text = "FIDELYA",
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 2.86.sp,
            color = ink
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = ModernistSpace.s4),
            thickness = 2.dp,
            color = ink.copy(alpha = 0.6f)
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Toutes vos cartes,\nun seul geste.",
                fontFamily = Archivo,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 46.sp,
                lineHeight = 47.sp,
                letterSpacing = (-1.38).sp,
                color = ink
            )
            Text(
                text = "Scannez une carte de fidélité, elle est prête à la caisse. " +
                    "Pas de compte, pas de publicité, tout reste sur le téléphone.",
                modifier = Modifier.padding(top = 18.dp).widthIn(max = 300.dp),
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = ink
            )
        }
        // L'accent occupe déjà le fond : le bouton principal s'inverse.
        Text(
            text = "Scanner ma première carte",
            modifier = Modifier
                .fillMaxWidth()
                .background(ink)
                .clickable(onClick = onScan)
                .padding(16.dp),
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Saisir le numéro à la main",
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .border(2.dp, ink, RectangleShape)
                .clickable(onClick = onManualEntry)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Start,
            color = ink
        )
    }
}
