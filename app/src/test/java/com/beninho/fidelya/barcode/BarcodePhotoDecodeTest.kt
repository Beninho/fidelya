package com.beninho.fidelya.barcode

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Regression coverage for photos that failed to import through the scan screen.
 *
 * The fixtures under `src/test/resources/barcodes` are the actual photos reported by users. They are
 * decoded with ZXing (the JVM-friendly decoder already bundled for barcode rendering) so the case is
 * pinned down without an emulator; ML Kit cannot run on the JVM.
 */
class BarcodePhotoDecodeTest {

    /**
     * Ground truth for the reported photo: what it holds and which symbology it uses. The tilt is
     * not what breaks the import - a decoder reads the raw photo fine.
     */
    @Test
    fun `tilted card photo ground truth`() {
        val result = decode(loadFixture(TILTED_ITF14))

        assertNotNull("fixture no longer decodable", result)
        assertEquals("89390077251428", result!!.text)
        assertEquals("ITF", result.barcodeFormat.name)
    }

    @Test
    fun `tilted itf14 photo decodes after deskew and contrast stretch`() {
        val cleaned = BarcodeImagePreprocessor.clean(loadFixture(TILTED_ITF14))

        val result = decode(cleaned)

        assertNotNull("preprocessed photo still not decodable", result)
        assertEquals("89390077251428", result!!.text)
    }

    @Test
    fun `preprocessing recovers the photo tilt`() {
        val gray = BarcodeImagePreprocessor.toGrayscale(loadFixture(TILTED_ITF14))

        val skew = BarcodeImagePreprocessor.estimateSkewDegrees(gray)

        // The bars in the fixture lean a few degrees; the estimator has to find that, not 0.
        assert(skew in -8.0..-1.0) { "unexpected skew estimate: $skew" }
    }

    @Test
    fun `well framed ean13 card photo decodes after preprocessing`() {
        val cleaned = BarcodeImagePreprocessor.clean(loadFixture(PICARD_EAN13))

        val result = decode(cleaned)

        assertNotNull("Picard card photo not decodable", result)
        assertEquals("9902172745486", result!!.text)
    }

    private fun loadFixture(name: String): BufferedImage =
        checkNotNull(javaClass.getResourceAsStream("/barcodes/$name")) {
            "missing test fixture /barcodes/$name"
        }.use { ImageIO.read(it) }

    private fun decode(image: BufferedImage): Result? {
        val bitmap = BinaryBitmap(HybridBinarizer(GrayLuminanceSource(image)))
        val reader = MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.TRY_HARDER to true))
        }
        return try {
            reader.decodeWithState(bitmap)
        } catch (e: NotFoundException) {
            null
        }
    }

    private companion object {
        const val TILTED_ITF14 = "skewed_itf_89390077251428.jpg"
        const val PICARD_EAN13 = "picard_ean13_9902172745486.jpg"
    }
}
