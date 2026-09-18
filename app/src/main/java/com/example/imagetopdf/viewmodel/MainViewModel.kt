package com.example.imagetopdf.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagetopdf.data.DocumentHistoryRepository
import com.example.imagetopdf.data.DraftRepository
import com.example.imagetopdf.data.ThemePreferences
import com.example.imagetopdf.engine.PdfGeneratorEngine
import com.example.imagetopdf.engine.PdfMath
import com.example.imagetopdf.model.DraftRecord
import com.example.imagetopdf.model.FilterType
import com.example.imagetopdf.model.PageItem
import com.example.imagetopdf.model.PageOrientation
import com.example.imagetopdf.model.PdfConfig
import com.example.imagetopdf.model.PdfRecord
import com.example.imagetopdf.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiState(
    val pages: List<PageItem> = emptyList(),
    val selectedPageIds: Set<String> = emptySet(),
    val previewPageIndex: Int? = null,
    val filterTargetPage: PageItem? = null,
    val isFilterSheetVisible: Boolean = false,
    val isConfigDialogVisible: Boolean = false,
    val isSuccessDialogVisible: Boolean = false,
    val isReorderMode: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val generationProgress: Pair<Int, Int> = Pair(0, 0),
    val lastGeneratedRecord: PdfRecord? = null,
    val pdfConfig: PdfConfig = PdfConfig(),
    val errorMessage: String? = null,
    /** True when user presses back on HOME with images loaded — prompts Discard/Draft */
    val showDiscardDialog: Boolean = false,
    /** True when user double-taps back on empty HOME — prompts Exit confirmation */
    val showExitConfirmDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentHistoryRepository(application)
    val historyRecords: StateFlow<List<PdfRecord>> = repository.historyFlow

    private val draftRepository = DraftRepository(application)
    val drafts: StateFlow<List<DraftRecord>> = draftRepository.draftsFlow

    private val themePreferences = ThemePreferences(application)
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val defaultName = "Doc_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(pdfConfig = it.pdfConfig.copy(fileName = defaultName)) }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newItems = uris.map { uri -> PageItem(originalUri = uri) }
        _uiState.update { current ->
            current.copy(
                pages = current.pages + newItems,
                errorMessage = null
            )
        }
    }

    fun addCapturedImage(uri: Uri) {
        val newItem = PageItem(originalUri = uri)
        _uiState.update { current ->
            current.copy(
                pages = current.pages + newItem,
                errorMessage = null
            )
        }
    }

    fun removePage(id: String) {
        _uiState.update { current ->
            val updated = current.pages.filterNot { it.id == id }
            current.copy(
                pages = updated,
                previewPageIndex = if (updated.isEmpty()) null else current.previewPageIndex?.coerceAtMost(updated.size - 1)
            )
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            if (fromIndex !in current.pages.indices || toIndex !in current.pages.indices) return@update current
            val mutable = current.pages.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            current.copy(pages = mutable)
        }
    }

    fun rotatePage(id: String, degrees: Int = 90) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                if (page.id == id) {
                    page.copy(rotation = (page.rotation + degrees) % 360)
                } else {
                    page
                }
            }
            current.copy(pages = updated)
        }
    }

    fun rotateAllPages(degrees: Int = 90) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                page.copy(rotation = (page.rotation + degrees) % 360)
            }
            current.copy(pages = updated)
        }
    }

    fun updatePageTransform(pageId: String, scale: Float, panX: Float, panY: Float, fillPage: Boolean) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(
                        scale = scale.coerceIn(PdfMath.SCALE_MIN, PdfMath.SCALE_MAX),
                        panOffsetX = panX.coerceIn(PdfMath.PAN_MIN, PdfMath.PAN_MAX),
                        panOffsetY = panY.coerceIn(PdfMath.PAN_MIN, PdfMath.PAN_MAX),
                        fillPage = fillPage
                    )
                } else page
            }
            current.copy(pages = updated)
        }
    }

    fun resetPageTransform(pageId: String) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                if (page.id == pageId) {
                    page.copy(scale = 1.0f, panOffsetX = 0f, panOffsetY = 0f, fillPage = false)
                } else page
            }
            current.copy(pages = updated)
        }
    }

    fun openFilterSheet(page: PageItem?) {
        _uiState.update { it.copy(filterTargetPage = page, isFilterSheetVisible = true) }
    }

    fun closeFilterSheet() {
        _uiState.update { it.copy(filterTargetPage = null, isFilterSheetVisible = false) }
    }

    fun applyFilterToTarget(filterType: FilterType) {
        val target = _uiState.value.filterTargetPage
        if (target == null) {
            applyFilterToAll(filterType)
            return
        }
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                if (page.id == target.id) page.copy(filterType = filterType) else page
            }
            current.copy(pages = updated, isFilterSheetVisible = false, filterTargetPage = null)
        }
    }

    fun applyFilterToAll(filterType: FilterType) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                page.copy(filterType = filterType)
            }
            current.copy(pages = updated, isFilterSheetVisible = false, filterTargetPage = null)
        }
    }

    fun openPreview(pageIndex: Int) {
        _uiState.update { it.copy(previewPageIndex = pageIndex) }
    }

    fun closePreview() {
        _uiState.update { it.copy(previewPageIndex = null) }
    }

    fun toggleReorderMode() {
        _uiState.update { it.copy(isReorderMode = !it.isReorderMode) }
    }

    fun openConfigDialog() {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        if (state.isGeneratingPdf) return
        _uiState.update { it.copy(isConfigDialogVisible = true) }
    }

    fun closeConfigDialog() {
        _uiState.update { it.copy(isConfigDialogVisible = false) }
    }

    fun updatePdfConfig(config: PdfConfig) {
        _uiState.update { it.copy(pdfConfig = config) }
    }

    fun setDocumentOrientation(orientation: PageOrientation) {
        _uiState.update { it.copy(pdfConfig = it.pdfConfig.copy(orientation = orientation)) }
    }

    fun generatePdf(context: Context) {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        if (state.isGeneratingPdf) return

        val pages = state.pages
        val config = state.pdfConfig
        _uiState.update {
            it.copy(
                isConfigDialogVisible = false,
                isGeneratingPdf = true,
                generationProgress = Pair(0, pages.size),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val record = PdfGeneratorEngine.generatePdf(
                    context = context,
                    pages = pages,
                    config = config,
                    onProgress = { current, total ->
                        _uiState.update { it.copy(generationProgress = Pair(current, total)) }
                    }
                )
                repository.addRecord(record)
                _uiState.update {
                    it.copy(
                        isGeneratingPdf = false,
                        isSuccessDialogVisible = true,
                        lastGeneratedRecord = record
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingPdf = false,
                        errorMessage = "PDF Generation failed: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun closeSuccessDialog() {
        _uiState.update { it.copy(isSuccessDialogVisible = false) }
    }

    fun clearAllPages() {
        _uiState.update {
            val defaultName = "Doc_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            it.copy(
                pages = emptyList(),
                selectedPageIds = emptySet(),
                previewPageIndex = null,
                filterTargetPage = null,
                isReorderMode = false,
                pdfConfig = it.pdfConfig.copy(fileName = defaultName)
            )
        }
    }

    fun togglePageSelection(id: String) {
        _uiState.update { current ->
            val updated = if (current.selectedPageIds.contains(id)) {
                current.selectedPageIds - id
            } else {
                current.selectedPageIds + id
            }
            current.copy(selectedPageIds = updated)
        }
    }

    fun selectAllPages() {
        _uiState.update { current ->
            current.copy(selectedPageIds = current.pages.map { it.id }.toSet())
        }
    }

    fun clearPageSelection() {
        _uiState.update { current ->
            current.copy(selectedPageIds = emptySet())
        }
    }

    fun batchDeleteSelected() {
        _uiState.update { current ->
            val remaining = current.pages.filterNot { current.selectedPageIds.contains(it.id) }
            current.copy(pages = remaining, selectedPageIds = emptySet())
        }
    }

    fun batchRotateSelected(degrees: Int = 90) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                if (current.selectedPageIds.contains(page.id)) {
                    page.copy(rotation = (page.rotation + degrees) % 360)
                } else {
                    page
                }
            }
            current.copy(pages = updated)
        }
    }

    fun batchFilterSelected(filterType: FilterType) {
        _uiState.update { current ->
            val updated = current.pages.map { page ->
                if (current.selectedPageIds.contains(page.id)) {
                    page.copy(filterType = filterType)
                } else {
                    page
                }
            }
            current.copy(pages = updated)
        }
    }

    fun duplicatePage(id: String) {
        _uiState.update { current ->
            val index = current.pages.indexOfFirst { it.id == id }
            if (index == -1) return@update current
            val original = current.pages[index]
            val duplicate = original.copy(
                id = java.util.UUID.randomUUID().toString()
            )
            val mutable = current.pages.toMutableList()
            mutable.add(index + 1, duplicate)
            current.copy(pages = mutable)
        }
    }

    fun deleteHistoryRecord(record: PdfRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun toggleTheme() {
        val current = themeMode.value
        val next = when (current) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        themePreferences.setThemeMode(next)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Back-press guard: Discard / Draft ─────────────────────────────────────────
    fun showDiscardDialog() = _uiState.update { it.copy(showDiscardDialog = true) }
    fun dismissDiscardDialog() = _uiState.update { it.copy(showDiscardDialog = false) }
    /** Discard all current pages and return to empty home */
    fun discardAllPages() = _uiState.update { it.copy(pages = emptyList(), selectedPageIds = emptySet(), showDiscardDialog = false) }

    /**
     * Save the current pages as a named draft, then clear them.
     * Also takes persistable URI permission for each gallery URI so they survive process death.
     */
    fun saveDraft() {
        val currentPages = _uiState.value.pages
        if (currentPages.isEmpty()) return
        val draftName = "Draft_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val draft = DraftRecord(
            name = draftName,
            pageUris = currentPages.map { it.originalUri?.toString() ?: "" },
            pageRotations = currentPages.map { it.rotation },
            pageFilters = currentPages.map { it.filterType.name }
        )
        // Try to take persistable URI permission so gallery URIs survive app restart
        val cr = getApplication<Application>().contentResolver
        currentPages.forEach { page ->
            try {
                page.originalUri?.let { uri ->
                    cr.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (_: Exception) { /* camera/FileProvider URIs don't need this */ }
        }
        viewModelScope.launch {
            draftRepository.saveDraft(draft)
        }
        // Clear pages after saving
        _uiState.update { it.copy(pages = emptyList(), selectedPageIds = emptySet(), showDiscardDialog = false) }
    }

    /** Load a draft back as the current session pages */
    fun loadDraft(draft: DraftRecord) {
        val pages = draft.pageUris.mapIndexedNotNull { index, uriStr ->
            val uri = try { Uri.parse(uriStr) } catch (_: Exception) { null } ?: return@mapIndexedNotNull null
            val rotation = draft.pageRotations.getOrElse(index) { 0 }
            val filterName = draft.pageFilters.getOrElse(index) { FilterType.ORIGINAL.name }
            val filter = try { FilterType.valueOf(filterName) } catch (_: Exception) { FilterType.ORIGINAL }
            PageItem(originalUri = uri, rotation = rotation, filterType = filter)
        }
        _uiState.update { it.copy(pages = pages, selectedPageIds = emptySet()) }
    }

    fun deleteDraft(draft: DraftRecord) {
        viewModelScope.launch { draftRepository.deleteDraft(draft.id) }
    }

    // ── Back-press guard: Exit confirmation ───────────────────────────────────
    fun showExitConfirmDialog() = _uiState.update { it.copy(showExitConfirmDialog = true) }
    fun dismissExitConfirmDialog() = _uiState.update { it.copy(showExitConfirmDialog = false) }
}
