package com.beninho.fidelya.domain.share

import com.beninho.fidelya.domain.model.LoyaltyCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * La charge utile d'un partage de carte, telle qu'elle voyage dans un QR.
 *
 * Champs courts à dessein : un QR tient d'autant mieux à l'écran qu'il porte peu
 * d'octets, et tout ce qui est ici doit rentrer dans un code lisible par une
 * caméra de téléphone à trente centimètres.
 *
 * Pas de logo : l'écran 11 de la maquette l'annonce — « le code ne contient que
 * le nom, le numéro, le format et la couleur ». L'écran 10 promet l'inverse,
 * mais une image ne tient pas dans un QR.
 */
@Serializable
data class SharedCard(
    /** Version de format, pour qu'un futur champ ne casse pas les téléphones d'aujourd'hui. */
    val v: Int = VERSION,
    /** Nom de l'enseigne. */
    val n: String,
    /** Numéro de carte. */
    val c: String,
    /** Format de code-barres. */
    val f: String,
    /** Couleur de fond. */
    val bg: String,
    /** Émission, en millisecondes epoch — c'est ce qui fait expirer le code. */
    val iat: Long
) {
    companion object {
        const val VERSION = 1

        /** « Expire dans 10 min », écran 10 de la maquette. */
        const val VALIDITY_MS = 10 * 60 * 1000L
    }
}

fun LoyaltyCard.toShared(now: Long): SharedCard = SharedCard(
    n = storeName,
    c = cardNumber,
    f = barcodeFormat,
    bg = backgroundColor,
    iat = now
)

fun SharedCard.toDomain(): LoyaltyCard = LoyaltyCard(
    storeName = n,
    cardNumber = c,
    barcodeFormat = f,
    backgroundColor = bg
)

/**
 * Le partage passe par une URI et non par du JSON nu : le même contenu sert au
 * QR et au bouton « Copier le lien à la place », et un lecteur de codes
 * générique y voit quelque chose d'exploitable plutôt qu'une accolade.
 */
object CardShareCodec {
    const val SCHEME = "fidelya"
    const val HOST = "card"
    private const val PARAM = "d"

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getUrlDecoder()

    fun encode(card: SharedCard): String {
        val payload = encoder.encodeToString(json.encodeToString(card).toByteArray())
        return "$SCHEME://$HOST?$PARAM=$payload"
    }

    /**
     * Renvoie `null` sur tout ce qui n'est pas un partage exploitable : autre
     * schéma, base64 tronquée, JSON invalide, version inconnue. Un QR lu au
     * hasard ne doit jamais produire une carte à moitié remplie.
     */
    fun decode(raw: String): SharedCard? {
        val trimmed = raw.trim()
        val prefix = "$SCHEME://$HOST?$PARAM="
        if (!trimmed.startsWith(prefix)) return null
        val payload = trimmed.removePrefix(prefix)
        if (payload.isEmpty()) return null
        return runCatching {
            json.decodeFromString<SharedCard>(String(decoder.decode(payload)))
        }.getOrNull()?.takeIf { it.v == SharedCard.VERSION && it.n.isNotBlank() && it.c.isNotBlank() }
    }

    /** Un code émis il y a plus de dix minutes n'est plus accepté. */
    fun isExpired(card: SharedCard, now: Long): Boolean =
        now - card.iat > SharedCard.VALIDITY_MS
}
