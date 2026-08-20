package com.beninho.fidelya.ui.carddetail

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beninho.fidelya.barcode.SupportedBarcodeFormats
import com.beninho.fidelya.data.repository.CardRepository
import com.beninho.fidelya.ui.components.CardLogo
import com.beninho.fidelya.ui.theme.*
import com.google.zxing.MultiFormatWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun encodeBarcode(content: String, format: String, width: Int, height: Int): Bitmap? {
    // Falling back to QR_CODE here would silently render a barcode the till cannot read.
    val zxingFormat = SupportedBarcodeFormats.zxingFormatOf(format) ?: return null
    return runCatching {
        val matrix = MultiFormatWriter().encode(content, zxingFormat, width, height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) for (y in 0 until height)
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        bmp
    }.getOrNull()
}

private val dateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

private fun formatDate(epochMillis: Long): String =
    dateFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/**
 * Écran 04 de la maquette — le détail d'une carte.
 *
 * [startInCheckout] sert l'appui long de la liste : « code-barres direct », sans
 * passer par cet écran.
 */
@Composable
fun CardDetailScreen(
    cardId: Long,
    repository: CardRepository,
    brightnessBoost: Boolean,
    startInCheckout: Boolean = false,
    onEditClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: CardDetailViewModel = viewModel(
        factory = cardDetailViewModelFactory(repository, cardId)
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var checkoutMode by remember { mutableStateOf(startInCheckout) }
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            vm.onDeletedConsumed()
            onBack()
        }
    }

    // Un passage en caisse est ce qui alimente « Dernier passage ».
    LaunchedEffect(checkoutMode, state.card?.id) {
        if (checkoutMode && state.card != null) vm.markUsed()
    }

    DisposableEffect(checkoutMode) {
        val wasCheckout = checkoutMode  // capture at setup time so onDispose sees the right value
        val window = (context as? android.app.Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }

        val activity = context as? android.app.Activity
        if (wasCheckout && window != null && controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // « Luminosité maximale en caisse » — réglable, écran 09 de la maquette.
            if (brightnessBoost) {
                val lp = window.attributes
                lp.screenBrightness = 1f
                window.attributes = lp
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }

        onDispose {
            if (wasCheckout && window != null && controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars())
                if (brightnessBoost) {
                    val lp = window.attributes
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    window.attributes = lp
                }
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    if (checkoutMode) {
        CheckoutOverlay(
            cardNumber = state.card?.cardNumber ?: "",
            format = state.card?.barcodeFormat ?: "QR_CODE",
            onDismiss = { if (startInCheckout) onBack() else checkoutMode = false }
        )
        return
    }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernistTextButton("Mes cartes", onBack)
                    state.card?.let { card ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            ModernistTextButton("Modifier", onClick = { onEditClick(card.id) })
                            ModernistTextButton("Partager", onClick = { onShareClick(card.id) })
                        }
                    }
                }
                ModernistStrongRule()
            }
        }
    ) { padding ->
        val card = state.card
        if (card == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator()
                else Text("Carte introuvable", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val bgColor = parseCardColor(card.backgroundColor)
        val barcodeBitmap = remember(card.cardNumber, card.barcodeFormat) {
            encodeBarcode(card.cardNumber, card.barcodeFormat, 600, 200)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.width(6.dp).height(74.dp).background(bgColor, RectangleShape))
                Box(
                    Modifier
                        .padding(start = 14.dp)
                        .size(74.dp)
                        .background(bgColor, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CardLogo(
                        logoPath = card.logoUri,
                        fallbackText = card.logoEmoji ?: card.storeName.take(1).uppercase(),
                        fallbackColor = cardForegroundColor(bgColor),
                        size = 74.dp,
                        fallbackFontSize = 32.sp
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = card.storeName,
                        fontFamily = Archivo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.26).sp
                    )
                    Text(
                        text = card.cardNumber,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Blanc dans les deux thèmes : c'est un lecteur optique qui lit, et il
            // lui faut des barres noires sur blanc. Le texte posé dessus est donc
            // encré explicitement.
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(ModernistCodeSurface)
                    .border(1.dp, modernistDividerColor(), RectangleShape)
                    .padding(16.dp)
            ) {
                ModernistSectionLabel("Code-barres", color = ModernistCodeInk)
                if (barcodeBitmap != null) {
                    Image(
                        bitmap = barcodeBitmap.asImageBitmap(),
                        contentDescription = "Code-barres",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Ce numéro n'est pas encodable en ${card.barcodeFormat}.",
                        modifier = Modifier.padding(top = 10.dp),
                        fontSize = 13.sp,
                        color = ModernistCodeInk
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { checkoutMode = true }
                    .padding(horizontal = 16.dp, vertical = 17.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Présenter en caisse",
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column {
                ModernistKeyValue("Format", card.barcodeFormat)
                ModernistKeyValue("Ajoutée le", formatDate(card.createdAt))
                ModernistKeyValue(
                    label = "Dernier passage",
                    value = card.lastUsedAt?.let(::formatDate) ?: "Jamais",
                    lastInGroup = true
                )
            }
        }
    }
}

@Composable
fun CheckoutOverlay(cardNumber: String, format: String, onDismiss: () -> Unit) {
    val barcodeBitmap = remember(cardNumber, format) {
        encodeBarcode(cardNumber, format, 900, 300)
    }
    Box(
        Modifier
            .fillMaxSize()
            // Blanc et noir purs, indépendants du thème : c'est un scanner qui lit.
            .background(Color.White)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            barcodeBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Code caisse",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(cardNumber, fontSize = 20.sp, color = Color.Black)
            Text(
                text = "Appuyez n'importe où pour quitter",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
