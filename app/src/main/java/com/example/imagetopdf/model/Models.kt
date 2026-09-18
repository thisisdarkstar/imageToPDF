package com.example.imagetopdf.model

import android.net.Uri
import java.util.UUID

enum class FilterType(val displayName: String, val description: String) {
    ORIGINAL("Original", "Natural vibrant colors"),
    BW_DOCUMENT("B&W Clean", "Crisp document scan with deep black text"),
    GRAYSCALE("Grayscale", "Smooth studio-grade monochrome"),
    MAGIC_COLOR("Magic Color", "Vibrant tone boost with shadow clarity"),
    SEPIA("Sepia", "Warm vintage parchment tone")
}

enum class PageSizeOption(val displayName: String, val widthPt: Float, val heightPt: Float) {
    A4("A4 Standard (210 x 297 mm)", 595f, 842f),
    LETTER("US Letter (8.5 x 11 in)", 612f, 792f),
    FIT_TO_IMAGE("Fit to Image (Exact Dimensions)", 0f, 0f)
}

enum class PageOrientation(val displayName: String) {
    PORTRAIT("Portrait (Standard)"),
    LANDSCAPE("Landscape (Wide)"),
    AUTO("Auto (Match Images)")
}

enum class PageMargin(val displayName: String, val marginPt: Float) {
    NONE("No Margin (0 mm)", 0f),
    COMPACT("Compact (6 mm)", 18f),
    STANDARD("Standard (12 mm)", 36f)
}

enum class ImageQuality(val displayName: String, val jpegQuality: Int, val maxDimension: Int) {
    CRYSTAL_CLEAR("Ultra HD • Original (100%)", 100, 0), // 0 means no downscaling
    BALANCED("Balanced • Sharp (85%)", 85, 2400),
    COMPACT("Compact • Web Ready (65%)", 65, 1600)
}

enum class WatermarkPreset(val label: String) {
    CONFIDENTIAL("CONFIDENTIAL"),
    DRAFT("DRAFT"),
    ORIGINAL("ORIGINAL"),
    PAID("PAID"),
    SAMPLE("SAMPLE"),
    CUSTOM("CUSTOM")
}

enum class WatermarkColor(val displayName: String, val argbColor: Int) {
    GRAY("Classic Gray", 0xFF6B7280.toInt()),
    RED("Stamp Red", 0xFFDC2626.toInt()),
    BLUE("Security Blue", 0xFF2563EB.toInt())
}

data class WatermarkConfig(
    val enabled: Boolean = false,
    val text: String = "CONFIDENTIAL",
    val preset: WatermarkPreset = WatermarkPreset.CONFIDENTIAL,
    val color: WatermarkColor = WatermarkColor.GRAY,
    val opacity: Float = 0.20f,
    val isDiagonal: Boolean = true
)

data class PageItem(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: Uri? = null,
    val rotation: Int = 0,
    val filterType: FilterType = FilterType.ORIGINAL,
    val scale: Float = 1.0f,         // 1.0f = default fit, up to 3.0f
    val panOffsetX: Float = 0f,     // Normalized offset (-1.0f to 1.0f)
    val panOffsetY: Float = 0f,     // Normalized offset (-1.0f to 1.0f)
    val fillPage: Boolean = false   // Whether to crop to fill page or fit inside margins
)

data class PdfConfig(
    val fileName: String = "",
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val margin: PageMargin = PageMargin.COMPACT,
    val quality: ImageQuality = ImageQuality.CRYSTAL_CLEAR,
    val addPageNumbers: Boolean = true,
    val watermark: WatermarkConfig = WatermarkConfig()
)

data class PdfRecord(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A saved draft — a named snapshot of the current image selection
 * that can be resumed later from the Home screen.
 */
data class DraftRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Uri.toString() for each page — stored as plain strings */
    val pageUris: List<String>,
    /** Rotation in degrees for each page, parallel to pageUris */
    val pageRotations: List<Int>,
    /** FilterType.name for each page, parallel to pageUris */
    val pageFilters: List<String>,
    val savedAt: Long = System.currentTimeMillis()
) {
    /** Convenience — number of images in this draft */
    val imageCount: Int get() = pageUris.size
}
