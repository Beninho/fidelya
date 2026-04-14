# Add project specific ProGuard rules here.

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
