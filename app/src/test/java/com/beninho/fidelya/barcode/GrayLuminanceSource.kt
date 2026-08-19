package com.beninho.fidelya.barcode

import com.google.zxing.LuminanceSource
import java.awt.image.BufferedImage

/**
 * Minimal [LuminanceSource] over a [BufferedImage].
 *
 * ZXing ships one in `com.google.zxing:javase`, but the app only depends on `core`; reimplementing
 * the few methods the decoder needs keeps the test classpath aligned with production.
 */
class GrayLuminanceSource(image: BufferedImage) : LuminanceSource(image.width, image.height) {

    private val luminances = ByteArray(image.width * image.height).also { buffer ->
        val raster = image.raster
        val bands = raster.numBands
        val pixel = IntArray(bands)
        var index = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                raster.getPixel(x, y, pixel)
                val value = when {
                    bands >= 3 ->
                        // Same weights ZXing's own j2se source uses.
                        (pixel[0] * 306 + pixel[1] * 601 + pixel[2] * 117) shr 10
                    else -> pixel[0]
                }
                buffer[index++] = value.coerceIn(0, 255).toByte()
            }
        }
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        require(y in 0 until height) { "row out of bounds: $y" }
        val target = if (row == null || row.size < width) ByteArray(width) else row
        luminances.copyInto(target, 0, y * width, y * width + width)
        return target
    }

    override fun getMatrix(): ByteArray = luminances
}
