package com.example.imagetopdf.engine

import com.example.imagetopdf.model.FilterType
import com.example.imagetopdf.model.PageSizeOption
import com.example.imagetopdf.model.PageOrientation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfMath {

    const val SCALE_MIN = 0.5f
    const val SCALE_MAX = 4.0f
    const val PAN_MIN = -1.5f
    const val PAN_MAX = 1.5f
    const val WATERMARK_ANGLE_DEGREES = -45f
    const val WATERMARK_MAX_LENGTH = 40

    private const val LUMA_R = 0.213f
    private const val LUMA_G = 0.715f
    private const val LUMA_B = 0.072f

    fun computeSampleSize(originalMax: Int, maxDimension: Int): Int {
        if (maxDimension <= 0) return 1
        var sampleSize = 1
        while (originalMax / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun sanitizeBaseName(raw: String, maxLength: Int = 120): String {
        var name = raw.trim()
        if (name.endsWith(".pdf", ignoreCase = true)) name = name.dropLast(4)
        name = name.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        name = name.trimEnd('.')
        if (name.length > maxLength) name = name.substring(0, maxLength)
        return name.trim()
    }

    fun uniqueFileName(dir: File, rawName: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = sanitizeBaseName(rawName).ifEmpty { "Document_$timestamp" }
        val candidate = File(dir, "$base.pdf")
        if (!candidate.exists()) return candidate.name
        var counter = 2
        while (true) {
            val next = File(dir, "${base}_$counter.pdf")
            if (!next.exists()) return next.name
            counter++
        }
    }

    fun resolveIsLandscape(orientation: PageOrientation, imgW: Int, imgH: Int): Boolean =
        when (orientation) {
            PageOrientation.PORTRAIT -> false
            PageOrientation.LANDSCAPE -> true
            PageOrientation.AUTO -> imgW > imgH
        }

    fun pageSizeDimensions(pageSize: PageSizeOption, isLandscape: Boolean): Pair<Float, Float> =
        when (pageSize) {
            PageSizeOption.A4 -> if (isLandscape) Pair(842f, 595f) else Pair(595f, 842f)
            PageSizeOption.LETTER -> if (isLandscape) Pair(792f, 612f) else Pair(612f, 792f)
            PageSizeOption.FIT_TO_IMAGE -> Pair(0f, 0f)
        }

    fun baseScale(fillPage: Boolean, availW: Float, availH: Float, imgW: Int, imgH: Int): Float {
        val scaleX = availW / imgW.toFloat()
        val scaleY = availH / imgH.toFloat()
        return if (fillPage) max(scaleX, scaleY) else min(scaleX, scaleY)
    }

    // ── Filter matrices (pure-JVM, testable) ────────────────────────────────

    fun saturationMatrix(sat: Float): FloatArray {
        val invSat = 1f - sat
        val r = LUMA_R * invSat
        val g = LUMA_G * invSat
        val b = LUMA_B * invSat
        return floatArrayOf(
            r + sat, g, b, 0f, 0f,
            r, g + sat, b, 0f, 0f,
            r, g, b + sat, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun contrastScaleMatrix(scale: Float, translate: Float): FloatArray = floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    )

    fun multiplyColorMatrix(left: FloatArray, right: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            val lr = row * 5
            for (col in 0 until 4) {
                var v = 0f
                for (k in 0 until 4) v += left[lr + k] * right[k * 5 + col]
                out[lr + col] = v
            }
            var off = left[lr + 4]
            for (k in 0 until 4) off += left[lr + k] * right[k * 5 + 4]
            out[lr + 4] = off
        }
        return out
    }

    fun colorMatrixArrayFor(filterType: FilterType): FloatArray? = when (filterType) {
        FilterType.ORIGINAL -> null
        FilterType.GRAYSCALE -> saturationMatrix(0f)
        FilterType.BW_DOCUMENT -> multiplyColorMatrix(
            contrastScaleMatrix(3.2f, (-128f * (3.2f - 1f)) + 30f),
            saturationMatrix(0f)
        )
        FilterType.MAGIC_COLOR -> multiplyColorMatrix(
            contrastScaleMatrix(1.25f, (-128f * (1.25f - 1f)) + 12f),
            saturationMatrix(1.30f)
        )
        FilterType.SEPIA -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
