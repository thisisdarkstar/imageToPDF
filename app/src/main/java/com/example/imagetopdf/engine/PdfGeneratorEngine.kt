package com.example.imagetopdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.imagetopdf.model.ImageQuality
import com.example.imagetopdf.model.PageItem
import com.example.imagetopdf.model.PageMargin
import com.example.imagetopdf.model.PageOrientation
import com.example.imagetopdf.model.PageSizeOption
import com.example.imagetopdf.model.PdfConfig
import com.example.imagetopdf.model.PdfRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object PdfGeneratorEngine {

    /**
     * Converts a list of PageItems into a high-quality PDF document.
     * Reports progress via the onProgress callback on each page.
     */
    suspend fun generatePdf(
        context: Context,
        pages: List<PageItem>,
        config: PdfConfig,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfRecord = withContext(Dispatchers.IO) {
        require(pages.isNotEmpty()) { "Cannot create PDF with zero pages" }

        val totalPages = pages.size
        val pdfDocument = PdfDocument()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            for (index in pages.indices) {
                val pageItem = pages[index]
                val pageNumber = index + 1
                onProgress(pageNumber, totalPages)

                val uri = pageItem.originalUri ?: continue

                // 1. Load full resolution bitmap
                var bitmap = ImageFilterEngine.loadBitmap(
                    context = context,
                    uri = uri,
                    maxDimension = config.quality.maxDimension
                )

                // 2. Rotate if needed
                if (pageItem.rotation != 0) {
                    val rotated = ImageFilterEngine.rotateBitmap(bitmap, pageItem.rotation)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }

                // 3. Apply selected filter
                val filtered = ImageFilterEngine.applyFilter(bitmap, pageItem.filterType)
                if (filtered != bitmap) {
                    bitmap.recycle()
                    bitmap = filtered
                }

                // 4. Determine page dimensions in points (72 pt per inch)
                val (pageWidth, pageHeight) = calculatePageDimensions(bitmap, config)

                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth.toInt(),
                    pageHeight.toInt(),
                    pageNumber
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background: clean white
                canvas.drawColor(Color.WHITE)

                val margin = config.margin.marginPt
                val footerReserved = if (config.addPageNumbers) 24f else 0f

                val availableWidth = max(1f, pageWidth - (2 * margin))
                val availableHeight = max(1f, pageHeight - (2 * margin) - footerReserved)

                // Base scale calculation (Fit vs Fill)
                val baseScale = if (pageItem.fillPage) {
                    max(
                        availableWidth / bitmap.width.toFloat(),
                        availableHeight / bitmap.height.toFloat()
                    )
                } else {
                    min(
                        availableWidth / bitmap.width.toFloat(),
                        availableHeight / bitmap.height.toFloat()
                    )
                }

                val totalScale = baseScale * pageItem.scale.coerceIn(0.5f, 5.0f)
                val scaledWidth = bitmap.width * totalScale
                val scaledHeight = bitmap.height * totalScale

                // Centered coordinates
                val baseLeft = margin + ((availableWidth - scaledWidth) / 2f)
                val baseTop = margin + ((availableHeight - scaledHeight) / 2f)

                // Apply slide / pan offsets
                val panX = pageItem.panOffsetX * (availableWidth / 2f)
                val panY = pageItem.panOffsetY * (availableHeight / 2f)
                val finalLeft = baseLeft + panX
                val finalTop = baseTop + panY

                // Clip to printable bounds so zoomed/panned images don't overlap margins or footer
                canvas.save()
                val clipRect = RectF(margin, margin, pageWidth - margin, pageHeight - margin - footerReserved)
                canvas.clipRect(clipRect)

                val destRect = RectF(finalLeft, finalTop, finalLeft + scaledWidth, finalTop + scaledHeight)
                canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
                canvas.restore()

                // 5. Draw Watermark if enabled
                if (config.watermark.enabled && config.watermark.text.isNotBlank()) {
                    val wmText = config.watermark.text.trim()
                    val wmPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = config.watermark.color.argbColor
                        alpha = (config.watermark.opacity.coerceIn(0.05f, 0.8f) * 255).roundToInt()
                        textAlign = Paint.Align.CENTER
                    }

                    val baseFontSize = if (config.watermark.isDiagonal) {
                        (pageWidth * 0.12f).coerceIn(24f, 68f)
                    } else {
                        (pageWidth * 0.08f).coerceIn(18f, 50f)
                    }
                    wmPaint.textSize = baseFontSize

                    val centerX = pageWidth / 2f
                    val centerY = (pageHeight - footerReserved) / 2f

                    canvas.save()
                    if (config.watermark.isDiagonal) {
                        canvas.rotate(-40f, centerX, centerY)
                    }
                    canvas.drawText(wmText, centerX, centerY + (wmPaint.textSize / 3f), wmPaint)
                    canvas.restore()
                }

                // 6. Draw page number if requested
                if (config.addPageNumbers) {
                    val pageStr = "Page $pageNumber of $totalPages"
                    val footerY = pageHeight - (margin / 2f).coerceAtLeast(14f)
                    canvas.drawText(pageStr, pageWidth / 2f, footerY, textPaint)
                }

                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            // Prepare output file
            val outputDir = File(context.filesDir, "pdfs").apply {
                if (!exists()) mkdirs()
            }

            val sanitizedName = if (config.fileName.isNotBlank()) {
                config.fileName.trim().replace(Regex("[^a-zA-Z0-9_.-]"), "_").let {
                    if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
                }
            } else {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                "Document_$timestamp.pdf"
            }

            val targetFile = File(outputDir, sanitizedName)
            FileOutputStream(targetFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }

            PdfRecord(
                fileName = sanitizedName,
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                pageCount = totalPages
            )
        } finally {
            pdfDocument.close()
        }
    }

    private fun calculatePageDimensions(bitmap: Bitmap, config: PdfConfig): Pair<Float, Float> {
        return when (config.pageSize) {
            PageSizeOption.FIT_TO_IMAGE -> {
                Pair(bitmap.width.toFloat(), bitmap.height.toFloat())
            }
            PageSizeOption.A4 -> {
                orientDimensions(595f, 842f, bitmap, config.orientation)
            }
            PageSizeOption.LETTER -> {
                orientDimensions(612f, 792f, bitmap, config.orientation)
            }
        }
    }

    private fun orientDimensions(
        portraitWidth: Float,
        portraitHeight: Float,
        bitmap: Bitmap,
        orientation: PageOrientation
    ): Pair<Float, Float> {
        val isLandscape = when (orientation) {
            PageOrientation.PORTRAIT -> false
            PageOrientation.LANDSCAPE -> true
            PageOrientation.AUTO -> bitmap.width > bitmap.height
        }
        return if (isLandscape) {
            Pair(max(portraitWidth, portraitHeight), min(portraitWidth, portraitHeight))
        } else {
            Pair(min(portraitWidth, portraitHeight), max(portraitWidth, portraitHeight))
        }
    }
}
