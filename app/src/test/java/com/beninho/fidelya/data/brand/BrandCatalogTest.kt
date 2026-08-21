package com.beninho.fidelya.data.brand

import com.beninho.fidelya.domain.color.MODERNIST_CARD_COLORS
import com.beninho.fidelya.domain.color.nearestModernistColor
import com.beninho.fidelya.domain.color.parseHexChannels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le catalogue est généré par `scripts/brand_logos.py`. Ces tests éprouvent ce
 * que la génération peut casser sans que rien ne compile de travers : un nom en
 * double qui rendrait deux suggestions indistinguables, un drawable manquant,
 * une couleur qu'on ne saurait pas peindre.
 */
class BrandCatalogTest {

    @Test
    fun `catalogue non vide`() {
        assertTrue(BRAND_CATALOG.size >= 50)
    }

    @Test
    fun `chaque enseigne a un nom unique`() {
        val names = BRAND_CATALOG.map { simplifyBrandName(it.name) }
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun `chaque enseigne porte un logo et un programme`() {
        BRAND_CATALOG.forEach { brand ->
            assertNotEquals("logo manquant pour ${brand.name}", 0, brand.logo)
            assertTrue("programme vide pour ${brand.name}", brand.program.isNotBlank())
            assertTrue("secteur vide pour ${brand.name}", brand.sector.isNotBlank())
        }
    }

    @Test
    fun `chaque logo a une reference de drawable distincte`() {
        val logos = BRAND_CATALOG.map { it.logo }
        assertEquals(logos.size, logos.distinct().size)
    }

    @Test
    fun `la couleur de chaque enseigne tombe sur un pas de la palette`() {
        BRAND_CATALOG.forEach { brand ->
            assertNotNull("couleur illisible pour ${brand.name}", parseHexChannels(brand.color))
            assertTrue(
                "fond hors palette pour ${brand.name}",
                nearestModernistColor(brand.color) in MODERNIST_CARD_COLORS
            )
        }
    }
}
