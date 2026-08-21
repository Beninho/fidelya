package com.beninho.fidelya.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

/**
 * Le logo embarqué d'une enseigne du catalogue, en vignette.
 *
 * `painterResource` ne garde rien d'une recomposition à l'autre pour une image
 * bitmap : la liste des suggestions changeant à chaque caractère tapé, les WebP
 * de 512 px se redécodaient en entier — près d'un mégaoctet chacun — pour une
 * vignette de quelques dizaines de pixels. On décode donc à la taille voulue,
 * une seule fois, et on garde le résultat.
 */
@Composable
fun BrandLogo(@DrawableRes logo: Int, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sidePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(logo, sidePx) { BrandLogoCache.get(context, logo, sidePx) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    } else {
        // Un décodage raté est improbable — la ressource est dans l'APK — mais
        // la vignette reste dessinée plutôt que de laisser un trou dans la ligne.
        Image(
            painter = painterResource(logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    }
}

/**
 * Les vignettes déjà décodées, par ressource et par taille demandée.
 *
 * Le cache vit avec le processus : les suggestions reviennent d'une carte à
 * l'autre, et une vignette réduite ne pèse que quelques dizaines de kilooctets.
 */
private object BrandLogoCache {
    /** De quoi tenir plus que les cinq suggestions affichées, sans garder tout le catalogue. */
    private const val MAX_ENTRIES = 24

    private val cache = LruCache<Key, ImageBitmap>(MAX_ENTRIES)

    private data class Key(val resId: Int, val sidePx: Int)

    fun get(context: Context, @DrawableRes resId: Int, sidePx: Int): ImageBitmap? {
        if (sidePx <= 0) return null
        val key = Key(resId, sidePx)
        cache.get(key)?.let { return it }
        val decoded = decode(context, resId, sidePx) ?: return null
        cache.put(key, decoded)
        return decoded
    }

    /**
     * Décode la ressource au plus près de la taille voulue.
     *
     * `inSampleSize` ne divise que par des puissances de deux : on prend la plus
     * grande qui laisse encore la vignette assez fine, l'affichage réduira le
     * reste.
     */
    private fun decode(context: Context, @DrawableRes resId: Int, sidePx: Int): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        val source = maxOf(bounds.outWidth, bounds.outHeight)
        if (source <= 0) return null

        var sample = 1
        while (source / (sample * 2) >= sidePx) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeResource(context.resources, resId, options)?.asImageBitmap()
    }
}
