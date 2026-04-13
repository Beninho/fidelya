package com.example.fidcard.ui.carddetail

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fidcard.data.repository.CardRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

fun encodeBarcode(content: String, format: String, width: Int, height: Int): Bitmap? {
    val zxingFormat = when (format) {
        "QR_CODE" -> BarcodeFormat.QR_CODE
        "EAN_13" -> BarcodeFormat.EAN_13
        "EAN_8" -> BarcodeFormat.EAN_8
        "CODE_128" -> BarcodeFormat.CODE_128
        "CODE_39" -> BarcodeFormat.CODE_39
        "PDF_417" -> BarcodeFormat.PDF_417
        "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
        else -> BarcodeFormat.QR_CODE
    }
    return runCatching {
        val matrix = MultiFormatWriter().encode(content, zxingFormat, width, height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) for (y in 0 until height)
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        bmp
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    repository: CardRepository,
    onEditClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: CardDetailViewModel = viewModel(
        factory = cardDetailViewModelFactory(repository, cardId)
    )
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var checkoutMode by remember { mutableStateOf(false) }
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            vm.onDeletedConsumed()
            onBack()
        }
    }

    DisposableEffect(checkoutMode) {
        val wasCheckout = checkoutMode  // capture at setup time so onDispose sees the right value
        val window = (context as? android.app.Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }

        if (wasCheckout && window != null && controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val lp = window.attributes
            lp.screenBrightness = 1f
            window.attributes = lp
        }

        onDispose {
            if (wasCheckout && window != null && controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    if (checkoutMode) {
        CheckoutOverlay(
            cardNumber = state.card?.cardNumber ?: "",
            format = state.card?.barcodeFormat ?: "QR_CODE",
            onDismiss = { checkoutMode = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.card?.storeName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    state.card?.let { card ->
                        IconButton(onClick = { onEditClick(card.id) }) {
                            Icon(Icons.Default.Edit, "Modifier")
                        }
                    }
                }
            )
        }
    ) { padding ->
        state.card?.let { card ->
            val bgColor = runCatching {
                Color(android.graphics.Color.parseColor(card.backgroundColor))
            }.getOrDefault(Color.Gray)
            val barcodeBitmap = remember(card.cardNumber, card.barcodeFormat) {
                encodeBarcode(card.cardNumber, card.barcodeFormat, 600, 200)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    Modifier.fillMaxWidth().height(180.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Box(Modifier.fillMaxSize().padding(20.dp)) {
                        Text(
                            card.logoEmoji ?: card.storeName.take(1).uppercase(),
                            fontSize = 40.sp,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                        Column(Modifier.align(Alignment.BottomStart)) {
                            Text(
                                card.storeName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                card.cardNumber,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                barcodeBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Code-barres",
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
                Text(card.cardNumber, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { checkoutMode = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Présenter en caisse")
                }
            }
        } ?: if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Carte introuvable", style = MaterialTheme.typography.bodyLarge)
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }
            Text(cardNumber, fontSize = 20.sp, color = Color.Black)
            Text("Appuyez pour quitter", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
