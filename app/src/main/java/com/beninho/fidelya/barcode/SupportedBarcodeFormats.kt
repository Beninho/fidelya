package com.beninho.fidelya.barcode

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat

/**
 * Single source of truth for the barcode symbologies the app supports.
 *
 * The scanner options, the persisted format string and the on-screen rendering all have to agree: a
 * format the scanner accepts but the renderer cannot draw produces a card that is impossible to show
 * at the till, and a format missing from the scanner options is never detected at all (which is how
 * ITF-14 loyalty cards used to fail to import).
 *
 * The name is what gets stored on the card, so entries must not be renamed.
 */
object SupportedBarcodeFormats {

    private val byName: Map<String, Pair<Int, BarcodeFormat>> = linkedMapOf(
        "QR_CODE" to (Barcode.FORMAT_QR_CODE to BarcodeFormat.QR_CODE),
        "EAN_13" to (Barcode.FORMAT_EAN_13 to BarcodeFormat.EAN_13),
        "EAN_8" to (Barcode.FORMAT_EAN_8 to BarcodeFormat.EAN_8),
        "UPC_A" to (Barcode.FORMAT_UPC_A to BarcodeFormat.UPC_A),
        "UPC_E" to (Barcode.FORMAT_UPC_E to BarcodeFormat.UPC_E),
        "CODE_128" to (Barcode.FORMAT_CODE_128 to BarcodeFormat.CODE_128),
        "CODE_39" to (Barcode.FORMAT_CODE_39 to BarcodeFormat.CODE_39),
        "CODE_93" to (Barcode.FORMAT_CODE_93 to BarcodeFormat.CODE_93),
        "ITF" to (Barcode.FORMAT_ITF to BarcodeFormat.ITF),
        "CODABAR" to (Barcode.FORMAT_CODABAR to BarcodeFormat.CODABAR),
        "PDF_417" to (Barcode.FORMAT_PDF417 to BarcodeFormat.PDF_417),
        "DATA_MATRIX" to (Barcode.FORMAT_DATA_MATRIX to BarcodeFormat.DATA_MATRIX),
        "AZTEC" to (Barcode.FORMAT_AZTEC to BarcodeFormat.AZTEC),
    )

    /** Stored format names, in declaration order. */
    val names: Set<String> get() = byName.keys

    /** ML Kit format flags to hand to `BarcodeScannerOptions.setBarcodeFormats`. */
    val mlKitFormats: IntArray = byName.values.map { it.first }.toIntArray()

    /** Stored name for a detected ML Kit barcode, or null if the app does not support it. */
    fun nameOf(mlKitFormat: Int): String? =
        byName.entries.firstOrNull { it.value.first == mlKitFormat }?.key

    /** ZXing format used to render a stored card, or null if the app does not support it. */
    fun zxingFormatOf(name: String): BarcodeFormat? = byName[name]?.second
}
