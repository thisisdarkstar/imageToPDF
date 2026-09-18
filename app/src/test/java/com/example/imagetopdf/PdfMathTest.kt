package com.example.imagetopdf

import com.example.imagetopdf.engine.PdfMath
import com.example.imagetopdf.model.FilterType
import com.example.imagetopdf.model.PageOrientation
import com.example.imagetopdf.model.PageSizeOption
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PdfMathTest {

    // ── Sample size ──────────────────────────────────────────────────────────

    @Test
    fun sampleSizeNoDownscaleWhenMaxIsZeroOrNegative() {
        assertEquals(1, PdfMath.computeSampleSize(4000, 0))
        assertEquals(1, PdfMath.computeSampleSize(4000, -1))
    }

    @Test
    fun sampleSizeIsPowerOfTwo() {
        assertEquals(1, PdfMath.computeSampleSize(2000, 1600))
        assertEquals(1, PdfMath.computeSampleSize(3000, 1600))
        assertEquals(2, PdfMath.computeSampleSize(3200, 1600))
        assertEquals(2, PdfMath.computeSampleSize(6000, 1600))
        assertEquals(8, PdfMath.computeSampleSize(13000, 1600))
    }

    // ── Filename sanitize ────────────────────────────────────────────────────

    @Test
    fun sanitizeReplacesUnsafeCharacters() {
        assertEquals("my_scan_.jpg", PdfMath.sanitizeBaseName("my scan?.jpg"))
        assertEquals("report_FINAL_", PdfMath.sanitizeBaseName("report@FINAL!"))
        assertEquals(".._.._secret", PdfMath.sanitizeBaseName("..\\..\\secret"))
    }

    @Test
    fun sanitizeStripsPdfExtensionCaseInsensitively() {
        assertEquals("report", PdfMath.sanitizeBaseName("report.pdf"))
        assertEquals("report", PdfMath.sanitizeBaseName("report.PDF"))
        assertEquals("report", PdfMath.sanitizeBaseName(" report.pdf "))
    }

    @Test
    fun sanitizeTruncatesLongNames() {
        assertEquals(120, PdfMath.sanitizeBaseName("A".repeat(300)).length)
    }

    @Test
    fun sanitizeBlankOrDotOnlyBecomesEmpty() {
        assertEquals("", PdfMath.sanitizeBaseName(""))
        assertEquals("", PdfMath.sanitizeBaseName("   "))
        assertEquals("", PdfMath.sanitizeBaseName("..."))
    }

    // ── Unique file names ────────────────────────────────────────────────────

    @Test
    fun uniqueFileNameAppendsSuffixOnCollision() {
        val dir = createTempDir()
        try {
            assertEquals("report.pdf", PdfMath.uniqueFileName(dir, "report.pdf"))
            File(dir, "report.pdf").writeText("x")
            assertEquals("report_2.pdf", PdfMath.uniqueFileName(dir, "report.pdf"))
            File(dir, "report_2.pdf").writeText("x")
            assertEquals("report_3.pdf", PdfMath.uniqueFileName(dir, "report"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun uniqueFileNameFallsBackToTimestampDefault() {
        val dir = createTempDir()
        try {
            val name = PdfMath.uniqueFileName(dir, "   ")
            assertTrue(name.startsWith("Document_"))
            assertTrue(name.endsWith(".pdf"))
        } finally {
            dir.deleteRecursively()
        }
    }

    // ── Page dimensions ──────────────────────────────────────────────────────

    @Test
    fun pageSizeDimensionsRespectOrientation() {
        assertEquals(Pair(595f, 842f), PdfMath.pageSizeDimensions(PageSizeOption.A4, false))
        assertEquals(Pair(842f, 595f), PdfMath.pageSizeDimensions(PageSizeOption.A4, true))
        assertEquals(Pair(612f, 792f), PdfMath.pageSizeDimensions(PageSizeOption.LETTER, false))
        assertEquals(Pair(792f, 612f), PdfMath.pageSizeDimensions(PageSizeOption.LETTER, true))
        assertEquals(Pair(0f, 0f), PdfMath.pageSizeDimensions(PageSizeOption.FIT_TO_IMAGE, false))
    }

    // ── Orientation resolution ───────────────────────────────────────────────

    @Test
    fun autoOrientationFollowsImageAspect() {
        assertTrue(PdfMath.resolveIsLandscape(PageOrientation.AUTO, 2000, 1000))
        assertTrue(!PdfMath.resolveIsLandscape(PageOrientation.AUTO, 1000, 2000))
        assertTrue(PdfMath.resolveIsLandscape(PageOrientation.LANDSCAPE, 1, 1))
        assertTrue(!PdfMath.resolveIsLandscape(PageOrientation.PORTRAIT, 9, 1))
    }

    // ── Base scale ───────────────────────────────────────────────────────────

    @Test
    fun fitScaleUsesSmallestRatio() {
        assertEquals(0.4f, PdfMath.baseScale(false, 100f, 40f, 200, 100), 0.001f)
    }

    @Test
    fun fillScaleUsesLargestRatio() {
        assertEquals(0.5f, PdfMath.baseScale(true, 100f, 40f, 200, 100), 0.001f)
    }

    // ── Filter matrices ──────────────────────────────────────────────────────

    @Test
    fun originalHasNoMatrix() {
        assertNull(PdfMath.colorMatrixArrayFor(FilterType.ORIGINAL))
    }

    @Test
    fun allFilterMatricesHave20Elements() {
        FilterType.values().forEach { type ->
            PdfMath.colorMatrixArrayFor(type)?.let { assertEquals(20, it.size) }
        }
    }

    @Test
    fun grayscaleMatrixUsesStandardLuma() {
        val m = PdfMath.colorMatrixArrayFor(FilterType.GRAYSCALE)!!
        assertEquals(0.213f, m[0], 1e-6f)
        assertEquals(0.715f, m[1], 1e-6f)
        assertEquals(0.072f, m[2], 1e-6f)
        assertEquals(0f, m[4], 1e-6f)
        assertEquals(m[0], m[5], 1e-6f)
        assertEquals(m[1], m[6], 1e-6f)
        assertEquals(m[2], m[7], 1e-6f)
    }

    @Test
    fun bwDocumentMatrixMatchesEngineConstants() {
        val m = PdfMath.colorMatrixArrayFor(FilterType.BW_DOCUMENT)!!
        assertEquals(3.2f * 0.213f, m[0], 1e-4f)
        assertEquals(3.2f * 0.715f, m[1], 1e-4f)
        assertEquals(3.2f * 0.072f, m[2], 1e-4f)
        assertEquals(-251.6f, m[4], 1e-2f)
        assertEquals(-251.6f, m[9], 1e-2f)
        assertEquals(-251.6f, m[14], 1e-2f)
    }

    @Test
    fun magicColorMatrixMatchesEngineConstants() {
        val m = PdfMath.colorMatrixArrayFor(FilterType.MAGIC_COLOR)!!
        assertEquals(1.25f * (0.213f * (1f - 1.3f) + 1.3f), m[0], 1e-4f)
        assertEquals(1.25f * (0.715f * (1f - 1.3f)), m[1], 1e-4f)
        assertEquals(1.25f * (0.072f * (1f - 1.3f)), m[2], 1e-4f)
        assertEquals(-20f, m[4], 1e-2f)
        assertEquals(-20f, m[9], 1e-2f)
        assertEquals(-20f, m[14], 1e-2f)
    }

    @Test
    fun sepiaMatrixKeepsOriginalLiterals() {
        val m = PdfMath.colorMatrixArrayFor(FilterType.SEPIA)!!
        assertEquals(0.393f, m[0], 1e-6f)
        assertEquals(0.769f, m[1], 1e-6f)
        assertEquals(0.168f, m[7], 1e-6f)
        assertEquals(1f, m[18], 1e-6f)
        assertEquals(0f, m[19], 1e-6f)
    }

    @Test
    fun multiplyingByIdentityIsNoOp() {
        val identity = PdfMath.contrastScaleMatrix(1f, 0f)
        val gray = PdfMath.saturationMatrix(0f)
        assertArrayEquals(gray, PdfMath.multiplyColorMatrix(identity, gray), 1e-6f)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun createTempDir(): File {
        val base = System.getProperty("java.io.tmpdir") ?: "."
        return File(base, "pdfmath_${System.nanoTime()}").apply { mkdirs() }
    }
}