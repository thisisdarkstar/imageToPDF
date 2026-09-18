package com.example.imagetopdf

import android.net.Uri
import com.example.imagetopdf.model.FilterType
import com.example.imagetopdf.model.ImageQuality
import com.example.imagetopdf.model.PageItem
import com.example.imagetopdf.model.PageMargin
import com.example.imagetopdf.model.PageOrientation
import com.example.imagetopdf.model.PageSizeOption
import com.example.imagetopdf.model.PdfConfig
import com.example.imagetopdf.model.WatermarkColor
import com.example.imagetopdf.model.WatermarkConfig
import com.example.imagetopdf.model.WatermarkPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ModelAndLogicTest {

    @Test
    fun testPdfConfigDefaults() {
        val config = PdfConfig()
        assertEquals(PageSizeOption.A4, config.pageSize)
        assertEquals(PageOrientation.PORTRAIT, config.orientation)
        assertEquals(PageMargin.COMPACT, config.margin)
        assertEquals(ImageQuality.CRYSTAL_CLEAR, config.quality)
        assertTrue(config.addPageNumbers)
        assertEquals(0, config.quality.maxDimension) // 0 means no downscaling for Crystal Clear
        assertEquals(100, config.quality.jpegQuality)
        assertFalse(config.watermark.enabled)
    }

    @Test
    fun testWatermarkConfig() {
        val wm = WatermarkConfig(
            enabled = true,
            text = "OFFICIAL COPY",
            preset = WatermarkPreset.CONFIDENTIAL,
            color = WatermarkColor.RED,
            opacity = 0.25f,
            isDiagonal = true
        )
        assertTrue(wm.enabled)
        assertEquals("OFFICIAL COPY", wm.text)
        assertEquals(WatermarkColor.RED, wm.color)
        assertEquals(0.25f, wm.opacity, 0.001f)
        assertTrue(wm.isDiagonal)

        // Test presets text values
        assertEquals("CONFIDENTIAL", WatermarkPreset.CONFIDENTIAL.label)
        assertEquals("DRAFT", WatermarkPreset.DRAFT.label)
        assertEquals("PAID", WatermarkPreset.PAID.label)
        assertEquals("SAMPLE", WatermarkPreset.SAMPLE.label)
    }

    @Test
    fun testPageItemCreation() {
        val item1 = PageItem()
        val item2 = PageItem()

        assertNotEquals(item1.id, item2.id)
        assertEquals(0, item1.rotation)
        assertEquals(FilterType.ORIGINAL, item1.filterType)
        assertEquals(1.0f, item1.scale, 0.001f)
        assertEquals(0f, item1.panOffsetX, 0.001f)
        assertEquals(0f, item1.panOffsetY, 0.001f)
        assertTrue(!item1.fillPage)
    }

    @Test
    fun testPageDuplicationLogic() {
        val page1 = PageItem(id = "1", rotation = 90, filterType = FilterType.BW_DOCUMENT)
        val page2 = PageItem(id = "2", rotation = 0, filterType = FilterType.ORIGINAL)
        val list = mutableListOf(page1, page2)

        // Duplicate page1
        val index = list.indexOfFirst { it.id == "1" }
        val duplicate = list[index].copy(id = UUID.randomUUID().toString())
        list.add(index + 1, duplicate)

        assertEquals(3, list.size)
        assertEquals("1", list[0].id)
        assertNotEquals("1", list[1].id)
        assertEquals(90, list[1].rotation)
        assertEquals(FilterType.BW_DOCUMENT, list[1].filterType)
        assertEquals("2", list[2].id)
    }

    @Test
    fun testBatchDeleteAndRotateLogic() {
        val page1 = PageItem(id = "1", rotation = 0)
        val page2 = PageItem(id = "2", rotation = 90)
        val page3 = PageItem(id = "3", rotation = 180)
        var list = listOf(page1, page2, page3)

        val selectedIds = setOf("1", "3")

        // Batch Rotate Selected by 90
        list = list.map { page ->
            if (selectedIds.contains(page.id)) {
                page.copy(rotation = (page.rotation + 90) % 360)
            } else {
                page
            }
        }
        assertEquals(90, list[0].rotation)
        assertEquals(90, list[1].rotation) // unchanged
        assertEquals(270, list[2].rotation)

        // Batch Delete Selected
        list = list.filterNot { selectedIds.contains(it.id) }
        assertEquals(1, list.size)
        assertEquals("2", list[0].id)
    }

    @Test
    fun testReorderingLogic() {
        val list = mutableListOf("Page1", "Page2", "Page3", "Page4")
        
        // Move "Page4" (index 3) to first (index 0)
        val item = list.removeAt(3)
        list.add(0, item)
        assertEquals(listOf("Page4", "Page1", "Page2", "Page3"), list)

        // Move "Page1" (now index 1) to last (index 3)
        val item2 = list.removeAt(1)
        list.add(3, item2)
        assertEquals(listOf("Page4", "Page2", "Page3", "Page1"), list)
    }

    @Test
    fun testPageSizeDimensions() {
        assertEquals(595f, PageSizeOption.A4.widthPt)
        assertEquals(842f, PageSizeOption.A4.heightPt)
        assertEquals(612f, PageSizeOption.LETTER.widthPt)
        assertEquals(792f, PageSizeOption.LETTER.heightPt)
        assertEquals(0f, PageSizeOption.FIT_TO_IMAGE.widthPt)
    }

    @Test
    fun testFilterTypesAvailable() {
        val filters = FilterType.values()
        assertEquals(5, filters.size)
        assertTrue(filters.contains(FilterType.ORIGINAL))
        assertTrue(filters.contains(FilterType.BW_DOCUMENT))
        assertTrue(filters.contains(FilterType.GRAYSCALE))
        assertTrue(filters.contains(FilterType.MAGIC_COLOR))
        assertTrue(filters.contains(FilterType.SEPIA))
    }

    @Test
    fun testThemeModeTransitions() {
        // Verify ThemeMode entries
        val modes = com.example.imagetopdf.theme.ThemeMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(com.example.imagetopdf.theme.ThemeMode.SYSTEM))
        assertTrue(modes.contains(com.example.imagetopdf.theme.ThemeMode.LIGHT))
        assertTrue(modes.contains(com.example.imagetopdf.theme.ThemeMode.DARK))

        // Verify toggle transition logic:
        // From SYSTEM -> DARK
        val nextFromSystem = when (com.example.imagetopdf.theme.ThemeMode.SYSTEM) {
            com.example.imagetopdf.theme.ThemeMode.LIGHT -> com.example.imagetopdf.theme.ThemeMode.DARK
            com.example.imagetopdf.theme.ThemeMode.DARK -> com.example.imagetopdf.theme.ThemeMode.LIGHT
            com.example.imagetopdf.theme.ThemeMode.SYSTEM -> com.example.imagetopdf.theme.ThemeMode.DARK
        }
        assertEquals(com.example.imagetopdf.theme.ThemeMode.DARK, nextFromSystem)

        // From DARK -> LIGHT
        val nextFromDark = when (com.example.imagetopdf.theme.ThemeMode.DARK) {
            com.example.imagetopdf.theme.ThemeMode.LIGHT -> com.example.imagetopdf.theme.ThemeMode.DARK
            com.example.imagetopdf.theme.ThemeMode.DARK -> com.example.imagetopdf.theme.ThemeMode.LIGHT
            com.example.imagetopdf.theme.ThemeMode.SYSTEM -> com.example.imagetopdf.theme.ThemeMode.DARK
        }
        assertEquals(com.example.imagetopdf.theme.ThemeMode.LIGHT, nextFromDark)

        // From LIGHT -> DARK
        val nextFromLight = when (com.example.imagetopdf.theme.ThemeMode.LIGHT) {
            com.example.imagetopdf.theme.ThemeMode.LIGHT -> com.example.imagetopdf.theme.ThemeMode.DARK
            com.example.imagetopdf.theme.ThemeMode.DARK -> com.example.imagetopdf.theme.ThemeMode.LIGHT
            com.example.imagetopdf.theme.ThemeMode.SYSTEM -> com.example.imagetopdf.theme.ThemeMode.DARK
        }
        assertEquals(com.example.imagetopdf.theme.ThemeMode.DARK, nextFromLight)
    }
}

