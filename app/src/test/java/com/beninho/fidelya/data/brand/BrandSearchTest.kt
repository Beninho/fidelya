package com.beninho.fidelya.data.brand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandSearchTest {

    private val catalog = listOf(
        Brand("Carrefour", "Carrefour Pass", 1, "#0D60A3"),
        Brand("Casino", "Club Casino", 2, "#14712F"),
        Brand("Decathlon", "Carte Decathlon", 3, "#0A4474"),
        Brand("Nocibé", "Carte Nocibé", 4, "#DA69B9"),
        Brand("H&M", "H&M Club", 5, "#EE6952"),
        Brand("Air France", "Flying Blue", 6, "#062D4F")
    )

    @Test
    fun `saisie vide ne propose rien`() {
        assertTrue(brandSuggestions("", catalog).isEmpty())
        assertTrue(brandSuggestions("   ", catalog).isEmpty())
    }

    @Test
    fun `les correspondances en debut de nom passent devant`() {
        val names = brandSuggestions("ca", catalog).map { it.name }
        // Decathlon contient « ca » mais ne commence pas par là.
        assertEquals(listOf("Carrefour", "Casino", "Decathlon"), names)
    }

    @Test
    fun `les accents et la casse sont ignores`() {
        assertEquals(listOf("Nocibé"), brandSuggestions("NOCIBE", catalog).map { it.name })
        assertEquals(listOf("Nocibé"), brandSuggestions("nocibé", catalog).map { it.name })
    }

    @Test
    fun `la ponctuation du nom est ignoree`() {
        assertEquals(listOf("H&M"), brandSuggestions("hm", catalog).map { it.name })
        assertEquals(listOf("H&M"), brandSuggestions("h&m", catalog).map { it.name })
    }

    @Test
    fun `le mot Carte du programme ne compte pas`() {
        // Sans quoi « ca » remonterait toutes les « Carte X » du catalogue.
        assertTrue(brandSuggestions("ca", catalog).none { it.name == "Nocibé" })
    }

    @Test
    fun `le nom du programme est cherche aussi`() {
        // C'est « Flying Blue » qui est imprimé sur la carte, pas « Air France ».
        assertEquals(listOf("Air France"), brandSuggestions("flying", catalog).map { it.name })
    }

    @Test
    fun `la liste est bornee`() {
        assertEquals(2, brandSuggestions("c", catalog, limit = 2).size)
    }

    @Test
    fun `une enseigne inconnue ne propose rien`() {
        assertTrue(brandSuggestions("boulangerie du coin", catalog).isEmpty())
    }

    @Test
    fun `le catalogue embarque repond a une saisie partielle`() {
        assertTrue(brandSuggestions("carr").any { it.name == "Carrefour" })
    }
}
