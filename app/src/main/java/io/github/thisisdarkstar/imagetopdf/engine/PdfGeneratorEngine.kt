package io.github.thisisdarkstar.imagetopdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import io.github.thisisdarkstar.imagetopdf.model.ImageQuality
import io.github.thisisdarkstar.imagetopdf.model.PageItem
import io.github.thisisdarkstar.imagetopdf.model.PageSizeOption
import io.github.thisisdarkstar.imagetopdf.model.PdfConfig
import io.github.thisisdarkstar.imagetopdf.model.PdfRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
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

        val plainBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            for (index in pages.indices) {
                val pageItem = pages[index]
                val pageNumber = index + 1
                onProgress(pageNumber, totalPages)

                val uri = pageItem.originalUri ?: continue

                // 1. Load bitmap (with OOM fallback chain handled by ImageFilterEngine)
                var bitmap: Bitmap? = null
                try {
                    bitmap = ImageFilterEngine.loadBitmap(
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

                    // 3. Determine page dimensions in points (72 pt per inch)
                    val (pageWidth, pageHeight) = if (config.pageSize == PageSizeOption.FIT_TO_IMAGE) {
                        Pair(bitmap.width.toFloat(), bitmap.height.toFloat())
                    } else {
                        val isLandscape = PdfMath.resolveIsLandscape(
                            config.orientation, bitmap.width, bitmap.height
                        )
                        PdfMath.pageSizeDimensions(config.pageSize, isLandscape)
                    }

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
                    val baseScale = PdfMath.baseScale(
                        pageItem.fillPage,
                        availableWidth,
                        availableHeight,
                        bitmap.width,
                        bitmap.height
                    )

                    val totalScale = baseScale * pageItem.scale.coerceIn(PdfMath.SCALE_MIN, PdfMath.SCALE_MAX)
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
                    // The filter is applied by the paint color-filter directly onto the page —
                    // this avoids allocating a second full-resolution bitmap per page.
                    val imagePaint = ImageFilterEngine.filterPaint(pageItem.filterType) ?: plainBitmapPaint
                    canvas.drawBitmap(bitmap, null, destRect, imagePaint)
                    canvas.restore()

                    // 4. Draw Watermark if enabled
                    if (config.watermark.enabled && config.watermark.text.isNotBlank()) {
                        val wmText = config.watermark.text.trim().take(PdfMath.WATERMARK_MAX_LENGTH)
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
                            canvas.rotate(PdfMath.WATERMARK_ANGLE_DEGREES, centerX, centerY)
                        }
                        canvas.drawText(wmText, centerX, centerY + (wmPaint.textSize / 3f), wmPaint)
                        canvas.restore()
                    }

                    // 5. Draw page number if requested
                    if (config.addPageNumbers) {
                        val pageStr = "Page $pageNumber of $totalPages"
                        val footerY = pageHeight - (margin / 2f).coerceAtLeast(14f)
                        canvas.drawText(pageStr, pageWidth / 2f, footerY, textPaint)
                    }

                    pdfDocument.finishPage(page)
                } finally {
                    bitmap?.recycle()
                }
            }

            // Prepare output file (sanitized + collision-proof name)
            val outputDir = File(context.filesDir, "pdfs").apply {
                if (!exists()) mkdirs()
            }
            val fileName = PdfMath.uniqueFileName(outputDir, config.fileName)
            val targetFile = File(outputDir, fileName)
            FileOutputStream(targetFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }

            PdfRecord(
                fileName = fileName,
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                pageCount = totalPages
            )
        } finally {
            pdfDocument.close()
        }
    }
}