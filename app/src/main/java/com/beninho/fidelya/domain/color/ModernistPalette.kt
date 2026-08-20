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
 * Modernist et on l'étend à douze familles de teintes.
 *
 * Construction, en OKLCH :
 *  - Luminosité : l'échelle partagée des rampes Modernist, pas
 *    300/400/500/700/800/900 (L = 0.870 / 0.780 / 0.680 / 0.481 / 0.378 /
 *    0.291). Les pas 100 et 200 sont écartés : au delà de L 0.93 le gamut sRGB
 *    ne laisse plus assez de chroma et toutes les teintes y seraient
 *    indistinctement blanches. Le pas 600 est écarté pour une autre raison : à
 *    L 0.580 la luminance relative frôle 0.2, le point où les deux encres se
 *    valent (~3.85:1), et aucune des deux n'y tient AA. Le trou entre les pas
 *    500 et 700 n'est donc pas un oubli, c'est la zone morte du contraste.
 *  - Teintes : 31.5° — celle de l'accent Modernist — puis 70, 110, 148, 192,
 *    221, 250, 272, 295, 318, 340 et 6°. Les quatre dernières sont venues
 *    subdiviser les quatre plus grands écarts de la grille à huit familles,
 *    d'où leur concentration du cyan au rose : les teintes chaudes et vertes y
 *    étaient déjà les plus serrées. Écart minimal 22.5°, maximal 44°.
 *  - Chroma : constant par pas (0.090 / 0.130 / 0.170 / 0.130 / 0.100 / 0.075),
 *    écrêté au gamut. Modernist pousse sa rampe accent au chroma maximal, mais
 *    ce maximum varie du simple au quadruple selon la teinte : repris tel quel
 *    il donne un arc-en-ciel de néons disparates. À chroma constant les douze
 *    familles se lisent comme un seul jeu — et les pas 300 et 400 du rouge
 *    retombent exactement sur les #FFC4B8 et #FF9783 de la rampe d'origine, ce
 *    qui vérifie la construction.
 *
 * La famille neutre garde en plus le pas 100 (#F8F4F4) : c'est la seule où un
 * quasi-blanc a du sens, et sans lui un fond blanc n'aurait plus d'équivalent.
 *
 * Les 37 pas de la grille à huit familles sont reconduits à l'identique : une
 * carte enregistrée avant l'extension garde un fond que le sélecteur sait
 * encore désigner.
 *
 * Les 79 pas passent WCAG AA (≥ 4.5:1, pire cas 5.3:1) avec l'encre que leur
 * choisit `cardForegroundColor`.
 */
val CARD_COLOR_ROWS: List<List<String>> = listOf(
    listOf("#FFC4B8", "#FF9783", "#EE6952", "#993B2C", "#6D291E", "#4A1A12"), // rouge — teinte de l'accent Modernist
    listOf("#FBCA93", "#ECA751", "#D18501", "#825100", "#5D3900", "#3F2500"), // ambre
    listOf("#D7DA94", "#BCBE53", "#9F9F00", "#626200", "#454500", "#2E2E00"), // olive
    listOf("#ACE5B3", "#7ACE87", "#39B457", "#14712F", "#0E5020", "#083514"), // vert
    listOf("#89E7E3", "#1ED1CC", "#01AFAB", "#006C6A", "#004D4B", "#003332"), // turquoise
    listOf("#8EE2FE", "#38CAF1", "#00A9CE", "#006981", "#004A5C", "#00313E"), // cyan
    listOf("#B4D8FF", "#7BBDFF", "#319CFC", "#0D60A3", "#0A4474", "#062D4F"), // bleu
    listOf("#C5D3FF", "#9EB3FF", "#758EFF", "#4556A6", "#303C76", "#1F2850"), // indigo
    listOf("#D7CCFF", "#BDA7FF", "#A17FF5", "#644B9E", "#473571", "#2F224C"), // violet
    listOf("#EDC2FB", "#DA9CED", "#C372DB", "#7B428D", "#572E64", "#3B1E43"), // fuchsia
    listOf("#FDBDE7", "#EE95D1", "#DA69B9", "#8C3B74", "#632952", "#431A37"), // magenta
    listOf("#FFC1CD", "#FD92AA", "#EB6488", "#973853", "#6C263A", "#491926"), // rose
    // Neutres : la rampe Modernist telle quelle, aux mêmes pas que les familles
    // chromatiques, plus le quasi-blanc du pas 100.
    listOf("#F8F4F4", "#D7D3D3", "#BAB6B6", "#9B9797", "#605D5D", "#444141", "#2D2B2B")
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
 * La grille couvrant désormais douze teintes, la teinte d'origine survit au
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
