package com.beninho.fidelya.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beninho.fidelya.BuildConfig
import com.beninho.fidelya.R
import com.beninho.fidelya.data.brand.BRAND_CATALOG
import com.beninho.fidelya.ui.theme.Archivo
import com.beninho.fidelya.ui.theme.ModernistDivider
import com.beninho.fidelya.ui.theme.ModernistKeyValue
import com.beninho.fidelya.ui.theme.ModernistListRow
import com.beninho.fidelya.ui.theme.ModernistScreenHeader
import com.beninho.fidelya.ui.theme.ModernistSectionLabel
import com.beninho.fidelya.ui.theme.ModernistSpace

/** La page de soutien du projet, ouverte dans le navigateur. */
private const val KOFI_URL = "https://ko-fi.com/benlet"

/** Le dépôt public — la contrepartie vérifiable des promesses de cet écran. */
private const val SOURCE_URL = "https://github.com/Beninho/fidelya"

/** Le texte de la licence Archivo, que l'OFL 1.1 impose de livrer avec la police. */
private const val OFL_URL = "https://openfontlicense.org/"

/**
 * Écran « À propos », atteint depuis Réglages.
 *
 * Il n'est pas dans la maquette : il reprend donc les idiomes des autres écrans
 * — en-tête, `ModernistSectionLabel` pour les sections, `ModernistListRow` pour
 * les lignes cliquables — sans inventer de composant.
 *
 * Les affirmations de la section « Confidentialité » sont volontairement
 * littérales : l'app n'ouvre aucune connexion réseau, mais `allowBackup` est à
 * `true` dans le manifeste, donc la sauvegarde Android peut recopier la base
 * dans le Google Drive de l'utilisateur. Le texte le dit plutôt que de promettre
 * que rien ne quitte l'appareil.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var licenseShown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ModernistScreenHeader(
                title = "À propos",
                backLabel = "Réglages",
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
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    text = "FIDELYA",
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 2.86.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Toutes vos cartes,\nun seul geste.",
                    modifier = Modifier.padding(top = ModernistSpace.s3),
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    lineHeight = 29.sp,
                    letterSpacing = (-0.7).sp
                )
                Text(
                    text = "Fidelya rassemble vos cartes de fidélité sur votre téléphone : " +
                        "scannez le code-barres une fois, il est prêt à la caisse. " +
                        "Pas de compte, pas de publicité, pas de suivi.",
                    modifier = Modifier.padding(top = ModernistSpace.s3),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ModernistDivider()

            ModernistSectionLabel(
                text = "Confidentialité",
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
            )
            ModernistListRow(
                title = "Aucun compte",
                subtitle = "Rien à créer, rien à connecter. L'app s'ouvre et fonctionne."
            )
            ModernistListRow(
                title = "Aucun serveur",
                subtitle = "Fidelya n'ouvre aucune connexion réseau : ni analytics, " +
                    "ni publicité, ni synchronisation."
            )
            ModernistListRow(
                title = "Stockage privé à l'application",
                subtitle = "Cartes et logos vivent dans le dossier privé de l'app, " +
                    "que les autres applications ne peuvent pas lire."
            )
            ModernistListRow(
                title = "Une seule permission",
                subtitle = "La caméra, et seulement pendant le scan d'un code-barres. " +
                    "Aucune photo n'est enregistrée."
            )
            ModernistListRow(
                title = "Sauvegarde sous votre contrôle",
                subtitle = "L'export JSON va où vous le décidez. La sauvegarde " +
                    "automatique d'Android, si vous l'avez activée, peut aussi " +
                    "recopier vos cartes dans votre Google Drive."
            )

            ModernistSectionLabel(
                text = "Soutenir le projet",
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 10.dp)
            )
            ModernistListRow(
                title = "Offrir un café ☕",
                subtitle = "ko-fi.com/benlet — Fidelya est gratuite et sans publicité",
                onClick = { openUrl(context, KOFI_URL) }
            )
            ModernistListRow(
                title = "Code source",
                subtitle = "github.com/Beninho/fidelya — vérifiable, sous licence libre",
                onClick = { openUrl(context, SOURCE_URL) }
            )

            ModernistSectionLabel(
                text = "Mentions",
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 10.dp)
            )
            ModernistListRow(
                title = "Police Archivo",
                subtitle = if (licenseShown) {
                    "SIL Open Font License 1.1 — masquer le texte"
                } else {
                    "SIL Open Font License 1.1 — afficher le texte"
                },
                onClick = { licenseShown = !licenseShown }
            )
            if (licenseShown) {
                // Quelques kilo-octets lus à la composition : pas de quoi sortir du
                // thread principal, et le texte est libéré dès que la ligne se replie.
                val license = remember {
                    context.resources.openRawResource(R.raw.archivo_ofl)
                        .bufferedReader()
                        .use { it.readText() }
                }
                Text(
                    text = license,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ModernistListRow(
                    title = "Lire la licence en ligne",
                    subtitle = "openfontlicense.org",
                    onClick = { openUrl(context, OFL_URL) }
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                ModernistKeyValue(label = "Version", value = BuildConfig.VERSION_NAME)
                ModernistKeyValue(
                    label = "Enseignes embarquées",
                    value = BRAND_CATALOG.size.toString(),
                    lastInGroup = true
                )
            }
        }
    }
}

/**
 * Ouvre un lien externe. Un appareil sans navigateur — un émulateur nu, par
 * exemple — lève `ActivityNotFoundException` : mieux vaut un toast qu'un crash.
 */
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Aucune application pour ouvrir ce lien", Toast.LENGTH_SHORT).show()
    }
}
