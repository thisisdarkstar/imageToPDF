package com.example.imagetopdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import com.example.imagetopdf.model.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object ImageFilterEngine {

    /**
     * Loads a Bitmap from a Uri, respecting EXIF orientation.
     * Falls back to progressively lower max-dimensions on OutOfMemoryError.
     */
    suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 0
    ): Bitmap = withContext(Dispatchers.IO) {
        val candidates = buildList {
            if (maxDimension > 0) add(maxDimension)
            add(1600); add(800); add(512)
        }.distinct()

        var lastError: Throwable? = null
        for (dimension in candidates) {
            try {
                return@withContext decodeBitmapWithExif(context, uri, dimension)
            } catch (e: OutOfMemoryError) {
                lastError = e
                System.gc()
            }
        }
        throw lastError ?: RuntimeException("Failed to decode image from uri: $uri")
    }

    /**
     * Rotates a bitmap by the specified angle in degrees.
     */
    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Returns the 4×5 color-matrix array for the given [FilterType], or null for ORIGINAL.
     */
    fun colorMatrixArrayFor(filterType: FilterType): FloatArray? =
        PdfMath.colorMatrixArrayFor(filterType)

    /**
     * Returns a [Paint] whose [ColorMatrixColorFilter] applies the given filter,
     * or null for ORIGINAL (caller draws with a plain paint instead).
     */
    fun filterPaint(filterType: FilterType): Paint? {
        val array = PdfMath.colorMatrixArrayFor(filterType) ?: return null
        return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(array))
        }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun decodeBitmapWithExif(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap {
        val resolver = context.contentResolver

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        if (maxDimension > 0) {
            options.inJustDecodeBounds = true
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val originalMax = max(options.outWidth, options.outHeight)
            options.inSampleSize = PdfMath.computeSampleSize(originalMax, maxDimension)
            options.inJustDecodeBounds = false
        }

        val decodedBitmap: Bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Failed to decode image from uri: $uri")

        return try {
            val exifOrientation = resolver.openInputStream(uri)?.use { stream ->
                try {
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (_: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }
            } ?: ExifInterface.ORIENTATION_NORMAL

            val exifRotation = when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (exifRotation != 0) {
                val rotated = rotateBitmap(decodedBitmap, exifRotation)
                if (rotated != decodedBitmap) decodedBitmap.recycle()
                rotated
            } else {
                decodedBitmap
            }
        } catch (e: OutOfMemoryError) {
            decodedBitmap.recycle()
            throw e
        }
    }
}
