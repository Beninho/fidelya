package com.beninho.fidelya.domain.share

import com.beninho.fidelya.domain.model.LoyaltyCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardShareTest {

    private val card = LoyaltyCard(
        id = 7,
        storeName = "Carrefour",
        cardNumber = "1234567890123",
        barcodeFormat = "EAN_13",
        backgroundColor = "#EE6952",
        logoUri = "/data/user/0/com.beninho.fidelya/files/logos/abc.img",
        logoEmoji = "shopping"
    )

    private val now = 1_700_000_000_000L

    @Test
    fun roundTripPreservesWhatTheCodeCarries() {
        val decoded = CardShareCodec.decode(CardShareCodec.encode(card.toShared(now)))!!

        assertEquals("Carrefour", decoded.n)
        assertEquals("1234567890123", decoded.c)
        assertEquals("EAN_13", decoded.f)
        assertEquals("#EE6952", decoded.bg)
        assertEquals(now, decoded.iat)
    }

    /** La promesse faite à l'utilisateur sur l'écran 11 : ni logo, ni identifiant. */
    @Test
    fun theCodeCarriesNeitherLogoNorIdentity() {
        val encoded = CardShareCodec.encode(card.toShared(now))
        val decodedJson = String(
            java.util.Base64.getUrlDecoder().decode(encoded.removePrefix("fidelya://card?d="))
        )

        assertFalse("le chemin du logo ne doit pas voyager", decodedJson.contains("logos"))
        assertFalse(decodedJson.contains("abc.img"))
        assertFalse("l'emoji ne fait pas partie du format", decodedJson.contains("shopping"))
        assertEquals(0L, CardShareCodec.decode(encoded)!!.toDomain().id)
    }

    @Test
    fun decodedCardBecomesAFreshLocalCard() {
        val restored = CardShareCodec.decode(CardShareCodec.encode(card.toShared(now)))!!.toDomain()

        assertEquals(0L, restored.id)
        assertEquals("Carrefour", restored.storeName)
        assertNull(restored.logoUri)
        assertNull(restored.lastUsedAt)
    }

    @Test
    fun aLinkIsWhatGoesInTheCode() {
        assertTrue(CardShareCodec.encode(card.toShared(now)).startsWith("fidelya://card?d="))
    }

    @Test
    fun garbageNeverProducesAHalfFilledCard() {
        listOf(
            "",
            "   ",
            "bonjour",
            "https://example.com",
            "fidelya://card",
            "fidelya://card?d=",
            "fidelya://card?d=!!!pas-du-base64!!!",
            "fidelya://autre?d=eyJhIjoxfQ",
            "fidelya://card?d=eyJhIjoxfQ"
        ).forEach { raw ->
            assertNull("« $raw » ne devrait rien produire", CardShareCodec.decode(raw))
        }
    }

    @Test
    fun aCardWithoutNameOrNumberIsRejected() {
        val blank = SharedCard(n = "  ", c = "", f = "EAN_13", bg = "#EE6952", iat = now)
        assertNull(CardShareCodec.decode(CardShareCodec.encode(blank)))
    }

    @Test
    fun anUnknownFormatVersionIsRejected() {
        val future = SharedCard(v = 99, n = "Fnac", c = "1", f = "QR_CODE", bg = "#EE6952", iat = now)
        assertNull(CardShareCodec.decode(CardShareCodec.encode(future)))
    }

    @Test
    fun theCodeExpiresAfterTenMinutes() {
        val shared = card.toShared(now)

        assertFalse(CardShareCodec.isExpired(shared, now))
        assertFalse(CardShareCodec.isExpired(shared, now + 9 * 60_000L))
        assertTrue(CardShareCodec.isExpired(shared, now + 10 * 60_000L + 1))
    }
}
