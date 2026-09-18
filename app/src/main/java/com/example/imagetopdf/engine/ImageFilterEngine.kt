package com.example.imagetopdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.media.ExifInterface
import com.example.imagetopdf.model.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

object ImageFilterEngine {

    /**
     * Loads a high-fidelity Bitmap from a Uri, respecting EXIF orientation.
     * If maxDimension <= 0, loads full resolution with zero downsampling for crystal clear quality.
     */
    suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 0
    ): Bitmap = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // Check dimensions first if downsampling is requested
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        if (maxDimension > 0) {
            options.inJustDecodeBounds = true
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val originalMax = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (originalMax / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }
            options.inSampleSize = sampleSize
            options.inJustDecodeBounds = false
        }

        val decodedBitmap: Bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Failed to decode image from uri: $uri")

        // Correct EXIF orientation
        val exifOrientation = try {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val exifRotation = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        if (exifRotation != 0) {
            rotateBitmap(decodedBitmap, exifRotation)
        } else {
            decodedBitmap
        }
    }

    /**
     * Rotates a bitmap by a specified angle in degrees.
     */
    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Applies the selected filter to a Bitmap.
     * Uses hardware-accelerated Canvas operations with anti-aliasing and bilinear filtering.
     */
    suspend fun applyFilter(
        source: Bitmap,
        filterType: FilterType
    ): Bitmap = withContext(Dispatchers.Default) {
        when (filterType) {
            FilterType.ORIGINAL -> source

            FilterType.GRAYSCALE -> {
                val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
                result
            }

            FilterType.BW_DOCUMENT -> {
                // High-contrast Document Binarization filter
                // First converts to grayscale, then applies strong contrast & threshold to clean backgrounds and crispen text
                val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }

                // High contrast matrix: contrast scale = 3.2, offset to brighten paper background
                val contrast = 3.2f
                val translate = (-128f * (contrast - 1f)) + 30f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )

                contrastMatrix.preConcat(grayMatrix)
                paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                result
            }

            FilterType.MAGIC_COLOR -> {
                // Magic Color: boosted contrast, vibrant saturation, slight clarity lift
                val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                val satMatrix = ColorMatrix().apply { setSaturation(1.30f) }

                // Contrast +25%, brightness +10%
                val contrast = 1.25f
                val translate = (-128f * (contrast - 1f)) + 12f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )

                contrastMatrix.preConcat(satMatrix)
                paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                result
            }

            FilterType.SEPIA -> {
                val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                val sepiaMatrix = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(sepiaMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                result
            }
        }
    }
}
