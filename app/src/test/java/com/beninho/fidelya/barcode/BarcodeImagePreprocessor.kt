package com.beninho.fidelya.barcode

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * JVM-only image preprocessing used to reproduce real-world scan failures in unit tests.
 *
 * Photos of loyalty cards are rarely axis-aligned: the barcode is tilted a few degrees, the paper
 * is grey rather than white and the print is slightly blurred. Decoders tolerate that badly, so
 * this helper reproduces the cleanup a scanner pipeline is expected to perform before decoding:
 * grayscale -> deskew -> contrast stretch.
 *
 * Uses java.awt, so it is deliberately kept in the test source set (not usable on Android).
 */
object BarcodeImagePreprocessor {

    /** Angles (degrees) explored when looking for the barcode tilt. */
    private const val MAX_SKEW_DEGREES = 20.0
    private const val SKEW_STEP_DEGREES = 0.25

    /** Longest side of the downscaled copy used for the angle search. */
    private const val SKEW_PROBE_MAX_DIMENSION = 600

    /** Full pipeline: grayscale, deskew, contrast stretch. */
    fun clean(source: BufferedImage): BufferedImage =
        stretchContrast(deskew(toGrayscale(source)))

    fun toGrayscale(source: BufferedImage): BufferedImage {
        val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_BYTE_GRAY)
        val g = out.createGraphics()
        g.drawImage(source, 0, 0, null)
        g.dispose()
        return out
    }

    /**
     * Rotates the image so the bars become vertical.
     *
     * Barcode bars produce strong horizontal gradients. When the bars are vertical, summing those
     * gradients per column yields a very "peaky" profile; when they are tilted the peaks smear out.
     * Maximising the variance of that column profile therefore recovers the tilt angle.
     */
    fun deskew(gray: BufferedImage): BufferedImage {
        val angle = estimateSkewDegrees(gray)
        return if (abs(angle) < SKEW_STEP_DEGREES) gray else rotate(gray, -angle)
    }

    fun estimateSkewDegrees(gray: BufferedImage): Double {
        // The angle search rotates the image ~160 times, so run it on a small copy.
        val probe = scaleToMaxDimension(gray, SKEW_PROBE_MAX_DIMENSION)
        var bestAngle = 0.0
        var bestScore = -1.0
        var angle = -MAX_SKEW_DEGREES
        while (angle <= MAX_SKEW_DEGREES) {
            val score = columnGradientVariance(if (angle == 0.0) probe else rotate(probe, -angle))
            if (score > bestScore) {
                bestScore = score
                bestAngle = angle
            }
            angle += SKEW_STEP_DEGREES
        }
        return bestAngle
    }

    fun rotate(source: BufferedImage, degrees: Double): BufferedImage {
        val radians = Math.toRadians(degrees)
        val cos = abs(cos(radians))
        val sin = abs(sin(radians))
        val width = (source.width * cos + source.height * sin).roundToInt()
        val height = (source.width * sin + source.height * cos).roundToInt()

        val out = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val g = out.createGraphics()
        // Fill with white so the corners introduced by the rotation read as quiet zone, not as bars.
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        val transform = AffineTransform().apply {
            translate((width - source.width) / 2.0, (height - source.height) / 2.0)
            rotate(radians, source.width / 2.0, source.height / 2.0)
        }
        g.drawImage(source, transform, null)
        g.dispose()
        return out
    }

    /**
     * Linear contrast stretch that clips the darkest/brightest [clipPercent] of pixels, pushing a
     * washed-out photo (grey paper, dark grey ink) back to a full black-to-white range.
     */
    fun stretchContrast(gray: BufferedImage, clipPercent: Double = 2.0): BufferedImage {
        val histogram = IntArray(256)
        for (y in 0 until gray.height) {
            for (x in 0 until gray.width) {
                histogram[gray.raster.getSample(x, y, 0)]++
            }
        }
        val total = gray.width.toLong() * gray.height
        val clip = (total * clipPercent / 100.0).toLong()

        var low = 0
        var seen = 0L
        while (low < 255 && seen + histogram[low] < clip) {
            seen += histogram[low]
            low++
        }
        var high = 255
        seen = 0L
        while (high > low && seen + histogram[high] < clip) {
            seen += histogram[high]
            high--
        }
        if (high <= low) return gray

        val lut = IntArray(256) { value ->
            ((value - low) * 255.0 / (high - low)).roundToInt().coerceIn(0, 255)
        }
        val out = BufferedImage(gray.width, gray.height, BufferedImage.TYPE_BYTE_GRAY)
        for (y in 0 until gray.height) {
            for (x in 0 until gray.width) {
                out.raster.setSample(x, y, 0, lut[gray.raster.getSample(x, y, 0)])
            }
        }
        return out
    }

    fun scaleToMaxDimension(source: BufferedImage, maxDimension: Int): BufferedImage {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxDimension) return source
        val scale = maxDimension.toDouble() / longest
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        val out = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val g = out.createGraphics()
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        g.drawImage(source, 0, 0, width, height, null)
        g.dispose()
        return out
    }

    private fun columnGradientVariance(gray: BufferedImage): Double {
        val raster = gray.raster
        val profile = DoubleArray(gray.width - 1)
        for (x in 0 until gray.width - 1) {
            var sum = 0.0
            for (y in 0 until gray.height) {
                sum += abs(raster.getSample(x + 1, y, 0) - raster.getSample(x, y, 0))
            }
            profile[x] = sum
        }
        val mean = profile.average()
        return profile.sumOf { (it - mean) * (it - mean) } / profile.size
    }
}
