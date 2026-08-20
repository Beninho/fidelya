package com.beninho.fidelya.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.beninho.fidelya.data.logo.Logos
import com.beninho.fidelya.ui.theme.modernistGrayscaleFilter

/**
 * Le logo d'une enseigne, ou son initiale à défaut.
 *
 * Décodé avec `BitmapFactory` plutôt que par une bibliothèque de chargement :
 * le fichier est local, de la taille d'une vignette, et une dépendance de plus
 * ne se justifie pas. `Logos.resolve` filtre les chemins étrangers — un backup
 * venu d'un autre téléphone en porte, et on retombe alors sur l'initiale.
 */
@Composable
fun CardLogo(
    logoPath: String?,
    fallbackText: String,
    fallbackColor: Color,
    size: Dp,
    fallbackFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(logoPath) {
        Logos.resolve(context, logoPath)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }
    }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = modernistGrayscaleFilter(),
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                text = fallbackText,
                color = fallbackColor,
                fontSize = fallbackFontSize
            )
        }
    }
}
