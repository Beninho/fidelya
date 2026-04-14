package com.example.fidcard.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private enum class CameraPermission { GRANTED, SHOW_RATIONALE, DENIED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBarcodeDetected: (String, String) -> Unit,
    onBack: () -> Unit,
    vm: ScanViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    var permissionState by remember {
        mutableStateOf(
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> CameraPermission.GRANTED
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                    -> CameraPermission.SHOW_RATIONALE
                else -> CameraPermission.DENIED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionState = when {
            granted -> CameraPermission.GRANTED
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                -> CameraPermission.SHOW_RATIONALE
            else -> CameraPermission.DENIED
        }
    }

    LaunchedEffect(state) {
        if (state is ScanState.Detected) {
            val s = state as ScanState.Detected
            onBarcodeDetected(s.value, s.format)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner une carte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (permissionState) {
                CameraPermission.GRANTED ->
                    CameraPreview(onBarcode = vm::onBarcodeDetected)
                CameraPermission.SHOW_RATIONALE ->
                    PermissionRationale(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
                CameraPermission.DENIED -> {
                    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    PermissionDenied(onSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    })
                }
            }
            if (state is ScanState.Timeout) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aucun code détecté")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.resetTimeout(); onBack() }) {
                            Text("Saisie manuelle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(onBarcode: (Barcode) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE, Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_PDF417, Barcode.FORMAT_DATA_MATRIX
            ).build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }
    DisposableEffect(Unit) {
        onDispose { scanner.close() }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.let(onBarcode)
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }.onFailure { Log.e("ScanScreen", "Camera bind failed", it) }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "L'accès à la caméra est nécessaire pour scanner vos cartes.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Autoriser la caméra") }
    }
}

@Composable
fun PermissionDenied(onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Permission refusée. Activez l'accès caméra dans les paramètres.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSettings) { Text("Ouvrir les paramètres") }
    }
}
