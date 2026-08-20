package com.beninho.fidelya.domain.color

import kotlin.math.cbrt
import kotlin.math.pow

/**
 * La grille de fonds proposée au choix, une ligne par famille de teintes.
 *
 * Kotlin pur, sans dépendance Android ni Compose : la couche `data` s'en sert
 * pour migrer les cartes déjà enregistrées, la couche `ui` pour le sélecteur.
 *
 * Le thème « Modernist » est monochrome, mais un fond de carte n'est pas une
 * couleur de thème : c'est de la donnée utilisateur, dont le rôle est de
 * distinguer les cartes d'un coup d'œil. On garde donc la *construction* de
 * Modernist et on l'étend à huit familles de teintes.
 *
 * Construction, en OKLCH :
 *  - Luminosité : l'échelle partagée des rampes Modernist, pas 300/500/700/900
 *    (L = 0.870 / 0.680 / 0.481 / 0.291). Les pas 100 et 200 sont écartés : au
 *    delà de L 0.93 le gamut sRGB ne laisse plus assez de chroma et toutes les
 *    teintes y seraient indistinctement blanches.
 *  - Teintes : 31.5° — celle de l'accent Modernist — puis 70, 110, 148, 192,
 *    250, 295 et 340°.
 *  - Chroma : constant par pas (0.090 / 0.170 / 0.130 / 0.075), écrêté au
 *    gamut. Modernist pousse sa rampe accent au chroma maximal, mais ce maximum
 *    varie du simple au quadruple selon la teinte : repris tel quel il donne un
 *    arc-en-ciel de néons disparates. À chroma constant les huit familles se
 *    lisent comme un seul jeu — et le pas 300 du rouge retombe exactement sur
 *    le #FFC4B8 de la rampe d'origine, ce qui vérifie la construction.
 *
 * La famille neutre garde en plus le pas 100 (#F8F4F4) : c'est la seule où un
 * quasi-blanc a du sens, et sans lui un fond blanc n'aurait plus d'équivalent.
 *
 * Les 37 pas passent WCAG AA (≥ 4.5:1, pire cas 5.3:1) avec l'encre que leur
 * choisit `cardForegroundColor`.
 */
val CARD_COLOR_ROWS: List<List<String>> = listOf(
    listOf("#FFC4B8", "#EE6952", "#993B2C", "#4A1A12"), // rouge — teinte de l'accent Modernist
    listOf("#FBCA93", "#D18501", "#825100", "#3F2500"), // ambre
    listOf("#D7DA94", "#9F9F00", "#626200", "#2E2E00"), // olive
    listOf("#ACE5B3", "#39B457", "#14712F", "#083514"), // vert
    listOf("#89E7E3", "#01AFAB", "#006C6A", "#003332"), // turquoise
    listOf("#B4D8FF", "#319CFC", "#0D60A3", "#062D4F"), // bleu
    listOf("#D7CCFF", "#A17FF5", "#644B9E", "#2F224C"), // violet
    listOf("#FDBDE7", "#DA69B9", "#8C3B74", "#431A37"), // magenta
    listOf("#F8F4F4", "#D7D3D3", "#9B9797", "#605D5D", "#2D2B2B") // neutres, rampe Modernist telle quelle
)

/**
 * Les pas de la première palette Modernist : rampes accent, accent secondaire
 * et neutres au complet.
 *
 * Ils ne sont plus proposés au choix, mais restent des fonds *valides* — une
 * carte enregistrée quand le sélecteur était monochrome en porte encore un.
 * Les conserver ici évite une migration v2 → v3 et permet à
 * [nearestModernistColor] de les renvoyer sans qu'aucun fond ne sorte de la
 * palette.
 */
val LEGACY_CARD_COLORS: List<String> = listOf(
    "#FFF2EF", "#FFE0D9", "#FFC4B8", "#FF9783", "#FF563C",
    "#DD2B0F", "#AE1800", "#7C1405", "#4D170E",
    "#EF6853", "#C94B39", "#9E3526", "#71261B",
    "#F8F4F4", "#EAE7E7", "#D7D3D3", "#BAB6B6", "#9B9797",
    "#7D7979", "#605D5D", "#444141", "#2D2B2B"
)

/**
 * Tous les fonds acceptés : la grille du sélecteur, plus l'héritage monochrome.
 *
 * Sert à valider un fond, pas à en choisir un : [nearestModernistColor] ne vise
 * que la grille. Un pas hérité reste donc lisible et affichable, mais n'est
 * plus une destination — sinon le rouge de l'ancienne rampe accent capterait
 * des teintes que la grille sait désormais respecter (un rose Material tombait
 * sur #DD2B0F au lieu du magenta).
 */
val MODERNIST_CARD_COLORS: List<String> =
    (CARD_COLOR_ROWS.flatten() + LEGACY_CARD_COLORS).distinct()

/** Les seuls fonds que [nearestModernistColor] peut renvoyer. */
private val remapTargets: List<String> = CARD_COLOR_ROWS.flatten()

/** Fond par défaut d'une nouvelle carte : le pas 500 de la famille rouge. */
const val DEFAULT_CARD_COLOR = "#EE6952"

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

private val targetsOklab: List<Triple<Double, Double, Double>> =
    remapTargets.map { hex ->
        val (r, g, b) = requireNotNull(parseHexChannels(hex)) { "Couleur de palette invalide : $hex" }
        toOklab(r, g, b)
    }

/**
 * Renvoie le pas de la grille le plus proche de [hex], au sens de l'écart perçu.
 *
 * La grille couvrant désormais huit teintes, la teinte d'origine survit au
 * remappage : un fond bleu tombe sur un bleu, un vert sur un vert. Seules les
 * couleurs très saturées perdent un peu de leur éclat, le chroma de la grille
 * étant volontairement uniforme.
 *
 * Une couleur illisible retombe sur [DEFAULT_CARD_COLOR].
 */
fun nearestModernistColor(hex: String): String {
    val (r, g, b) = parseHexChannels(hex) ?: return DEFAULT_CARD_COLOR
    val (l, a, bb) = toOklab(r, g, b)

    var best = 0
    var bestDistance = Double.MAX_VALUE
    targetsOklab.forEachIndexed { index, (pl, pa, pb) ->
        val dl = l - pl
        val da = a - pa
        val db = bb - pb
        val distance = dl * dl + da * da + db * db
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return remapTargets[best]
}
