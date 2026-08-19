# Optimisation taille build AAB

**Date:** 2026-04-14  
**Statut:** Approuvé

## Contexte

Le build release a `isMinifyEnabled = false` — R8 est entièrement désactivé. La dépendance `material-icons-extended` entière est embarquée. `coil-compose` est importé mais jamais utilisé dans l'UI (`logoUri` n'est jamais affiché). `accompanist-permissions` est déprécié et peut être remplacé par les APIs AndroidX natives.

## Objectif

Réduire la taille du AAB release. Gain estimé : **-40 à 60%**.

## Changements

### 1. `app/build.gradle.kts` — activer R8 + shrinking, supprimer dépendances inutiles

Dans le bloc `release` :
```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

Supprimer les dépendances :
- `implementation(libs.coil.compose)` — supprimée
- `implementation(libs.accompanist.permissions)` — supprimée

### 2. `gradle/libs.versions.toml` — supprimer entrées Coil et Accompanist

Supprimer dans `[versions]` :
- `coil = "2.7.0"`
- `accompanistPermissions = "0.37.0"`

Supprimer dans `[libraries]` :
- `coil-compose = ...`
- `accompanist-permissions = ...`

### 3. `app/proguard-rules.pro` — règles pour les libs utilisant la réflexion

```proguard
# Room
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * extends androidx.room.RoomDatabase { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.fidcard.**$$serializer { *; }
-keepclassmembers class com.example.fidcard.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.fidcard.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# Supprime les logs debug en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

### 4. `app/src/main/java/com/example/fidcard/ui/scan/ScanScreen.kt` — remplacer Accompanist

**Avant (Accompanist) :**
```kotlin
@OptIn(ExperimentalPermissionsApi::class, ...)
val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
// ...
when {
    cameraPermission.status.isGranted -> ...
    cameraPermission.status.shouldShowRationale -> ...
    else -> { LaunchedEffect(Unit) { cameraPermission.launchPermissionRequest() }; ... }
}
```

**Après (native AndroidX) :**

Enum local :
```kotlin
private enum class CameraPermission { GRANTED, SHOW_RATIONALE, DENIED }
```

État et launcher :
```kotlin
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
```

Logique `when` inchangée dans son comportement :
```kotlin
when (permissionState) {
    CameraPermission.GRANTED -> CameraPreview(onBarcode = vm::onBarcodeDetected)
    CameraPermission.SHOW_RATIONALE -> PermissionRationale(
        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
    )
    CameraPermission.DENIED -> {
        LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
        PermissionDenied(onSettings = { /* ouvrir Settings */ })
    }
}
```

Imports à supprimer :
- `com.google.accompanist.permissions.*`

Imports à ajouter :
- `android.app.Activity`
- `android.content.pm.PackageManager`
- `androidx.activity.compose.rememberLauncherForActivityResult`
- `androidx.activity.result.contract.ActivityResultContracts`
- `androidx.core.app.ActivityCompat`

`@OptIn(ExperimentalPermissionsApi::class)` supprimé.

## Fichiers modifiés

| Fichier | Type de changement |
|---|---|
| `app/build.gradle.kts` | Config (R8, shrinking, dépendances) |
| `gradle/libs.versions.toml` | Suppression entrées Coil + Accompanist |
| `app/proguard-rules.pro` | Ajout règles keep |
| `app/src/main/java/com/example/fidcard/ui/scan/ScanScreen.kt` | Remplacement Accompanist → native |

## Ce qui ne change pas

- Toute la logique métier de scan reste identique
- L'UI de `ScanScreen` reste identique
- Les 3 états de permission (granted / rationale / denied) restent les mêmes
- `CameraPreview`, `PermissionRationale`, `PermissionDenied` sont inchangés
