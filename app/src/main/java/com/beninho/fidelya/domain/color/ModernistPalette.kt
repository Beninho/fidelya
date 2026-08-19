package com.beninho.fidelya.domain.color

import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Les fonds de carte proposés, pris dans les rampes tonales du design system
 * « Modernist » (projet Claude Design du même nom, `styles.css`).
 *
 * Kotlin pur, sans dépendance Android ni Compose : la couche `data` s'en sert
 * pour migrer les cartes déjà enregistrées, la couche `ui` pour le sélecteur.
 *
 * Modernist est monochrome : les cartes ne se distinguent donc plus par la
 * teinte mais par la valeur. Les trois rampes partagent la même échelle de
 * luminosité, ce qui donne 22 pas nettement séparés à l'œil — les pas 100 à
 * 400 des deux rampes accent sont visuellement identiques et un seul jeu est
 * conservé.
 */
val MODERNIST_CARD_COLORS: List<String> = listOf(
    // Accent — du plus clair au plus soutenu
    "#FFF2EF", "#FFE0D9", "#FFC4B8", "#FF9783", "#FF563C",
    "#DD2B0F", "#AE1800", "#7C1405", "#4D170E",
    // Accent secondaire — seuls les pas qui divergent de la rampe accent
    "#EF6853", "#C94B39", "#9E3526", "#71261B",
    // Neutres
    "#F8F4F4", "#EAE7E7", "#D7D3D3", "#BAB6B6", "#9B9797",
    "#7D7979", "#605D5D", "#444141", "#2D2B2B"
)

/** Couleur de fond par défaut d'une nouvelle carte : le rouge de marque. */
const val DEFAULT_CARD_COLOR = "#DD2B0F"

/**
 * Décompose une couleur écrite `#RGB`, `#RRGGBB` ou `#AARRGGBB` en canaux
 * 0–255. L'alpha est ignoré : les fonds de carte sont toujours opaques.
 * Renvoie `null` si la chaîne n'est pas une couleur exploitable.
 */
internal fun parseHexChannels(hex: String): Triple<Int, Int, Int>? {
    val digits = hex.trim().removePrefix("#")
    if (digits.any { it.digitToIntOrNull(16) == null }) return null
    return when (digits.length) {
        3 -> digits.map { it.digitToInt(16) * 17 }
        6 -> (0..2).map { digits.substring(it * 2, it * 2 + 2).toInt(16) }
        8 -> (1..3).map { digits.substring(it * 2, it * 2 + 2).toInt(16) }
        else -> return null
    }.let { Triple(it[0], it[1], it[2]) }
}

/** Composante sRGB 0–255 vers linéaire 0–1. */
private fun toLinear(channel: Int): Double {
    val c = channel / 255.0
    return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

/**
 * Conversion vers OKLab. On compare les couleurs dans cet espace plutôt qu'en
 * RGB : l'écart euclidien y suit à peu près l'écart perçu, ce qui est la seule
 * façon d'obtenir un « pas le plus proche » qui ait du sens à l'œil.
 */
private fun toOklab(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
    val lr = toLinear(r)
    val lg = toLinear(g)
    val lb = toLinear(b)

    val l = cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
    val m = cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
    val s = cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)

    return Triple(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    )
}

private val paletteOklab: List<Triple<Double, Double, Double>> =
    MODERNIST_CARD_COLORS.map { hex ->
        val (r, g, b) = requireNotNull(parseHexChannels(hex)) { "Couleur de palette invalide : $hex" }
        toOklab(r, g, b)
    }

/**
 * Renvoie le pas de la palette Modernist le plus proche de [hex], au sens de
 * l'écart perçu.
 *
 * La teinte d'origine est perdue — Modernist n'a qu'une famille chromatique —
 * mais le rapport clair/soutenu est préservé : une couleur vive tombe sur un
 * pas accent, une couleur désaturée sur un neutre de valeur comparable.
 *
 * Une couleur illisible retombe sur [DEFAULT_CARD_COLOR].
 */
fun nearestModernistColor(hex: String): String {
    val (r, g, b) = parseHexChannels(hex) ?: return DEFAULT_CARD_COLOR
    val (l, a, bb) = toOklab(r, g, b)

    var best = 0
    var bestDistance = Double.MAX_VALUE
    paletteOklab.forEachIndexed { index, (pl, pa, pb) ->
        val dl = l - pl
        val da = a - pa
        val db = bb - pb
        val distance = dl * dl + da * da + db * db
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return MODERNIST_CARD_COLORS[best]
}
