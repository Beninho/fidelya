package com.beninho.fidelya.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.beninho.fidelya.data.logo.Logos
import kotlin.math.max

/**
 * Un logo décodé, et ce qu'on sait de son fond.
 *
 * [transparent] change le cadrage : une image opaque est cadrée, un glyphe ne
 * l'est pas et ne doit rien perdre.
 */
private class DecodedLogo(val image: ImageBitmap, val transparent: Boolean)

/** En dessous, l'image laisse voir ce qu'il y a derrière : c'est un glyphe, pas une photo. */
private const val OPAQUE_ALPHA = 250

/** Côté de la grille d'échantillonnage : 32 × 32 pixels lus, quelle que soit l'image. */
private const val SAMPLE_SIDE = 32

/**
 * Vrai si l'image laisse voir le fond de la carte.
 *
 * L'image est échantillonnée sur une grille de 32 × 32 au plus : un fond
 * transparent occupe toujours une large part de l'image. Le coût ne dépend donc
 * pas de la taille du fichier choisi, qui peut venir de l'appareil photo.
 *
 * `hasAlpha` ne suffit pas : un PNG opaque décodé en ARGB_8888 le renvoie vrai.
 * Il sert seulement à écarter d'emblée les formats sans canal alpha.
 *
 * `internal` pour que `CardLogoTransparencyTest` éprouve l'heuristique directement :
 * son résultat ne se lit pas dans l'arbre Compose.
 */
internal fun Bitmap.hasTransparentPixels(): Boolean {
    if (!hasAlpha()) return false

    val stepX = max(1, width / SAMPLE_SIDE)
    val stepY = max(1, height / SAMPLE_SIDE)

    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            if (getPixel(x, y) ushr 24 and 0xFF < OPAQUE_ALPHA) return true
            x += stepX
        }
        y += stepY
    }
    return false
}

/**
 * Le logo d'une enseigne, ou son initiale à défaut.
 *
 * Décodé avec `BitmapFactory` plutôt que par une bibliothèque de chargement :
 * le fichier est local, de la taille d'une vignette, et une dépendance de plus
 * ne se justifie pas. `Logos.resolve` filtre les chemins étrangers — un backup
 * venu d'un autre téléphone en porte, et on retombe alors sur l'initiale.
 *
 * L'image est rendue telle quelle : Modernist désature les photos, mais un logo
 * d'enseigne se reconnaît à sa couleur, et c'est ce qui rend la liste balayable.
 *
 * Deux cadrages selon l'image, cf. [hasTransparentPixels] :
 *  - opaque, on la suppose cadrée : elle remplit le carré en `Crop`.
 *  - transparente, on la suppose être un glyphe : `Fit` pour ne rien rogner d'un
 *    logo large, et une marge pour ne pas le coller au bord.
 *
 * Rien n'est peint sous le glyphe : ce qu'on voit à travers est la couleur
 * choisie pour la carte, que l'appelant pose derrière — c'est elle qui identifie
 * la carte, et une plaque neutre l'aurait masquée au seul endroit où le regard
 * se pose. Un logo sombre sur un pas sombre de la palette y perd donc en
 * lisibilité : le choix de la couleur reste à l'utilisateur.
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
    val logo = remember(logoPath) {
        Logos.resolve(context, logoPath)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                DecodedLogo(bitmap.asImageBitmap(), bitmap.hasTransparentPixels())
            }
        }
    }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (logo != null) {
            Image(
                bitmap = logo.image,
                contentDescription = null,
                contentScale = if (logo.transparent) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .padding(if (logo.transparent) size * LOGO_INSET else 0.dp)
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

/** La marge d'un glyphe dans son carré, en part du côté. */
private const val LOGO_INSET = 0.12f
