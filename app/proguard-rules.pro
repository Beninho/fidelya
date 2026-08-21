# Règles ProGuard/R8 spécifiques au projet.
#
# Les bibliothèques que nous utilisons embarquent déjà leurs propres règles
# « consumer » (Room, CameraX, ML Kit, Play Services) et R8 fournit en interne
# celles de kotlinx.serialization. Les dupliquer ici bloquerait l'optimisation
# sans rien protéger de plus : on ne déclare donc que ce qui n'est couvert par
# aucune bibliothèque.
#
# ZXing n'expose pas de règles consumer, mais nous l'utilisons uniquement via
# MultiFormatWriter/BarcodeFormat en appels directs (aucune réflexion) : R8 peut
# le réduire librement.

# Supprime les logs debug en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
