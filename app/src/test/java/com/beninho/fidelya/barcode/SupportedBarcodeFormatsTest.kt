package com.beninho.fidelya.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedBarcodeFormatsTest {

    /**
     * The regression that started this: the ITF-14 card in
     * `src/test/resources/barcodes/skewed_itf_89390077251428.jpg` was never detected because ITF was
     * absent from the scanner options.
     */
    @Test
    fun `itf is scannable and renderable`() {
        assertTrue("ITF missing from the stored format names", "ITF" in SupportedBarcodeFormats.names)
        assertEquals("ITF", SupportedBarcodeFormats.nameOf(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF))
        assertEquals(BarcodeFormat.ITF, SupportedBarcodeFormats.zxingFormatOf("ITF"))
    }

    @Test
    fun `every scanned format can be rendered back`() {
        SupportedBarcodeFormats.mlKitFormats.forEach { mlKitFormat ->
            val name = SupportedBarcodeFormats.nameOf(mlKitFormat)
            assertNotNull("no stored name for ML Kit format $mlKitFormat", name)
            assertNotNull("no ZXing writer format for $name", SupportedBarcodeFormats.zxingFormatOf(name!!))
        }
    }

    @Test
    fun `mlKitFormats matches the declared names`() {
        assertEquals(SupportedBarcodeFormats.names.size, SupportedBarcodeFormats.mlKitFormats.size)
        assertEquals(
            SupportedBarcodeFormats.mlKitFormats.size,
            SupportedBarcodeFormats.mlKitFormats.toSet().size,
        )
    }

    /** ZXing must actually be able to write every format we claim to render. */
    @Test
    fun `zxing can encode every supported format`() {
        val samples = mapOf(
            "QR_CODE" to "89390077251428",
            "EAN_13" to "9902172745486",
            "EAN_8" to "96385074",
            "UPC_A" to "036000291452",
            "UPC_E" to "04252614",
            "CODE_128" to "89390077251428",
            "CODE_39" to "FIDELYA123",
            "CODE_93" to "FIDELYA123",
            "ITF" to "89390077251428",
            "CODABAR" to "A12345B",
            "PDF_417" to "89390077251428",
            "DATA_MATRIX" to "89390077251428",
            "AZTEC" to "89390077251428",
        )
        assertEquals(
            "sample payload missing for a supported format",
            SupportedBarcodeFormats.names,
            samples.keys,
        )
        samples.forEach { (name, payload) ->
            val format = SupportedBarcodeFormats.zxingFormatOf(name)!!
            val matrix = MultiFormatWriter().encode(payload, format, 300, 120)
            assertTrue("$name encoded to an empty matrix", matrix.width > 0 && matrix.height > 0)
        }
    }

    @Test
    fun `unknown format is rejected instead of silently mapped`() {
        assertNull(SupportedBarcodeFormats.zxingFormatOf("MAXICODE"))
        assertNull(SupportedBarcodeFormats.nameOf(-1))
    }
}
