package com.beninho.fidelya.data.brand

import java.text.Normalizer

/**
 * Le nombre de suggestions proposées sous le champ « Nom du magasin ».
 *
 * Cinq : au delà, la liste pousse le reste du formulaire hors de l'écran, et le
 * nom d'une enseigne se départage en deux ou trois lettres.
 */
const val BRAND_SUGGESTION_LIMIT = 5

/**
 * Les enseignes du catalogue qui répondent à ce qui est saisi.
 *
 * Trois rangs, dans cet ordre :
 *  1. le nom commence par la saisie — taper « ca » propose Carrefour et Casino ;
 *  2. le nom la contient — Micromania vient après ;
 *  3. un mot du programme commence par elle. C'est « Flying Blue » ou « Moi+ »
 *     qui est imprimé sur la carte, pas toujours le nom de l'enseigne, et c'est
 *     ce que l'utilisateur a sous les yeux.
 *
 * À rang égal, l'ordre du catalogue tranche — il va du plus au moins connu.
 */
fun brandSuggestions(
    query: String,
    catalog: List<Brand> = BRAND_CATALOG,
    limit: Int = BRAND_SUGGESTION_LIMIT
): List<Brand> {
    val needle = simplifyBrandName(query)
    if (needle.isEmpty()) return emptyList()

    val ranks = catalog.mapNotNull { brand ->
        val name = simplifyBrandName(brand.name)
        val rank = when {
            name.startsWith(needle) -> 0
            name.contains(needle) -> 1
            programWords(brand.program).any { it.startsWith(needle) } -> 2
            else -> return@mapNotNull null
        }
        rank to brand
    }
    return ranks.sortedBy { it.first }.map { it.second }.take(limit)
}

/**
 * Les mots du programme sur lesquels chercher.
 *
 * « Carte » est écarté : presque tous les programmes s'appellent « Carte X », et
 * une saisie de deux lettres remonterait alors tout le catalogue. Le mot est
 * cherché en préfixe, pas en sous-chaîne, pour la même raison.
 */
private fun programWords(program: String): List<String> =
    program.split(' ', '/', '—', '-')
        .map(::simplifyBrandName)
        .filter { it.isNotEmpty() && it != "carte" }

/**
 * Le nom réduit à ses lettres et ses chiffres, sans accent ni casse.
 *
 * « nocibe » doit trouver Nocibé, « h&m » doit trouver H&M, et un espace de trop
 * ne doit rien changer : on compare des suites de caractères alphanumériques.
 */
fun simplifyBrandName(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKD)
        .filterNot { it.isMark() }
        .lowercase()
        .filter { it.isLetterOrDigit() }

/** Vrai pour un diacritique détaché par la décomposition NFKD. */
private fun Char.isMark(): Boolean =
    Character.getType(this).let {
        it == Character.NON_SPACING_MARK.toInt() ||
            it == Character.COMBINING_SPACING_MARK.toInt() ||
            it == Character.ENCLOSING_MARK.toInt()
    }
