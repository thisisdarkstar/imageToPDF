package com.example.imagetopdf.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.imagetopdf.model.PdfRecord
import com.example.imagetopdf.ui.components.FilterSheet
import com.example.imagetopdf.ui.components.PdfConfigDialog
import com.example.imagetopdf.ui.components.PdfProgressDialog
import com.example.imagetopdf.ui.components.PdfSuccessDialog
import com.example.imagetopdf.ui.screens.HistoryScreen
import com.example.imagetopdf.ui.screens.HomeScreen
import com.example.imagetopdf.ui.screens.PreviewScreen
import com.example.imagetopdf.ui.screens.ReorderScreen
import com.example.imagetopdf.ui.screens.SplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.example.imagetopdf.theme.ThemeMode
import com.example.imagetopdf.viewmodel.MainViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class AppScreen {
    SPLASH,
    HOME,
    HISTORY,
    REORDER,
    PREVIEW
}


@Composable
fun ImageToPdfApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val historyRecords by viewModel.historyRecords.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(false) }
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Multi-select / Single-select PhotoPicker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraUri != null) {
            viewModel.addCapturedImage(currentCameraUri!!)
        }
    }

    fun launchCameraActual() {
        try {
            val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val tempFile = File.createTempFile("scan_", ".jpg", photosDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            currentCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraActual()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraActual()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun sharePdf(record: PdfRecord) {
        val file = File(record.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, record.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
    }

    fun openPdf(record: PdfRecord) {
        val file = File(record.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_LONG).show()
        }
    }

    fun savePdfToDownloads(record: PdfRecord) {
        val sourceFile = File(record.filePath)
        if (!sourceFile.exists()) {
            Toast.makeText(context, "PDF file does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, record.fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ImageToPDF")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                    }
                    Toast.makeText(context, "Saved to Downloads/ImageToPDF/${record.fileName}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to create file in Downloads", Toast.LENGTH_SHORT).show()
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ImageToPDF")
                downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, record.fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                Toast.makeText(context, "Saved to Downloads/ImageToPDF/${record.fileName}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving to Downloads: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf(record: PdfRecord) {
        val file = File(record.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "PDF file does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing service unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(record.fileName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(record.pageCount)
                    .build()
                callback?.onLayoutFinished(info, newAttributes != oldAttributes)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                try {
                    val input = FileInputStream(file)
                    val output = FileOutputStream(destination?.fileDescriptor)
                    input.use { inStream ->
                        output.use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        try {
            printManager.print(
                "ImageToPDF_${record.fileName}",
                printAdapter,
                PrintAttributes.Builder().build()
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start print job: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val currentScreen = when {
        showSplash -> AppScreen.SPLASH
        showHistory -> AppScreen.HISTORY
        uiState.isReorderMode -> AppScreen.REORDER
        uiState.previewPageIndex != null -> AppScreen.PREVIEW
        else -> AppScreen.HOME
    }

    val animatedBgColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "appBackgroundAnim"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = animatedBgColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    when {
                        // Splash -> Home: smooth crossfade + subtle scale
                        initialState == AppScreen.SPLASH -> {
                            (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.96f, animationSpec = tween(400)))
                                .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.04f, animationSpec = tween(300)))
                        }
                        // Home <-> History: slide horizontally
                        targetState == AppScreen.HISTORY -> {
                            (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { -it / 3 } + fadeOut(animationSpec = tween(200)))
                        }
                        initialState == AppScreen.HISTORY -> {
                            (slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(250)) { it } + fadeOut(animationSpec = tween(200)))
                        }
                        // Home <-> Preview: studio zoom & fade
                        targetState == AppScreen.PREVIEW -> {
                            (scaleIn(initialScale = 0.92f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)))
                                .togetherWith(scaleOut(targetScale = 1.05f, animationSpec = tween(250)) + fadeOut(animationSpec = tween(200)))
                        }
                        initialState == AppScreen.PREVIEW -> {
                            (scaleIn(initialScale = 1.05f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)))
                                .togetherWith(scaleOut(targetScale = 0.92f, animationSpec = tween(250)) + fadeOut(animationSpec = tween(200)))
                        }
                        // Home <-> Reorder: slide vertically
                        targetState == AppScreen.REORDER -> {
                            (slideInVertically(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutVertically(animationSpec = tween(250)) { -it / 4 } + fadeOut(animationSpec = tween(200)))
                        }
                        initialState == AppScreen.REORDER -> {
                            (slideInVertically(animationSpec = tween(300)) { -it / 4 } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutVertically(animationSpec = tween(250)) { it } + fadeOut(animationSpec = tween(200)))
                        }
                        else -> fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    }
                },
                label = "ScreenNavigationTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.SPLASH -> {
                        SplashScreen(onFinish = { showSplash = false })
                    }

                    AppScreen.HISTORY -> {
                        HistoryScreen(
                            records = historyRecords,
                            onOpenPdf = { openPdf(it) },
                            onSharePdf = { sharePdf(it) },
                            onSaveToDownloads = { savePdfToDownloads(it) },
                            onPrintPdf = { printPdf(it) },
                            onDeletePdf = { viewModel.deleteHistoryRecord(it) },
                            onBack = { showHistory = false }
                        )
                    }

                    AppScreen.REORDER -> {
                        ReorderScreen(
                            pages = uiState.pages,
                            onMovePage = { from, to -> viewModel.movePage(from, to) },
                            onDone = { viewModel.toggleReorderMode() }
                        )
                    }

                    AppScreen.PREVIEW -> {
                        PreviewScreen(
                            pages = uiState.pages,
                            initialPageIndex = uiState.previewPageIndex ?: 0,
                            currentOrientation = uiState.pdfConfig.orientation,
                            onToggleOrientation = { viewModel.setDocumentOrientation(it) },
                            onUpdateTransform = { id, scale, x, y, fill ->
                                viewModel.updatePageTransform(id, scale, x, y, fill)
                            },
                            onResetTransform = { viewModel.resetPageTransform(it) },
                            onClose = { viewModel.closePreview() },
                            onRotatePage = { viewModel.rotatePage(it) },
                            onOpenFilterSheet = { viewModel.openFilterSheet(it) },
                            onDeletePage = { viewModel.removePage(it) }
                        )
                    }

                    AppScreen.HOME -> {
                        HomeScreen(
                            pages = uiState.pages,
                            selectedPageIds = uiState.selectedPageIds,
                            historyCount = historyRecords.size,
                            documentOrientation = uiState.pdfConfig.orientation,
                            themeMode = themeMode,
                            onToggleTheme = { viewModel.toggleTheme() },
                            onSetThemeMode = { viewModel.setThemeMode(it) },
                            onToggleOrientation = { viewModel.setDocumentOrientation(it) },
                            onToggleSelect = { viewModel.togglePageSelection(it) },
                            onSelectAll = { viewModel.selectAllPages() },
                            onClearSelection = { viewModel.clearPageSelection() },
                            onBatchDelete = { viewModel.batchDeleteSelected() },
                            onBatchRotate = { viewModel.batchRotateSelected() },
                            onDuplicatePage = { viewModel.duplicatePage(it) },
                            onLaunchCamera = { launchCamera() },
                            onLaunchGallery = { launchGallery() },
                            onPageClick = { viewModel.openPreview(it) },
                            onRotatePage = { viewModel.rotatePage(it) },
                            onRotateAll = { viewModel.rotateAllPages() },
                            onFilterPage = { viewModel.openFilterSheet(it) },
                            onFilterAll = { viewModel.openFilterSheet(null) },
                            onDeletePage = { viewModel.removePage(it) },
                            onMovePage = { from, to -> viewModel.movePage(from, to) },
                            onReorderClick = { viewModel.toggleReorderMode() },
                            onClearAll = { viewModel.clearAllPages() },
                            onOpenHistory = { showHistory = true },
                            onConvertClick = { viewModel.openConfigDialog() }
                        )
                    }
                }
            }

            // Filter Bottom Sheet
            if (uiState.isFilterSheetVisible) {
                FilterSheet(
                    targetPage = uiState.filterTargetPage,
                    totalPages = uiState.pages.size,
                    onApplyToTarget = { viewModel.applyFilterToTarget(it) },
                    onApplyToAll = { viewModel.applyFilterToAll(it) },
                    onDismiss = { viewModel.closeFilterSheet() }
                )
            }

            // PDF Configuration Dialog
            if (uiState.isConfigDialogVisible) {
                PdfConfigDialog(
                    initialConfig = uiState.pdfConfig,
                    pageCount = uiState.pages.size,
                    onConfirm = { config ->
                        viewModel.updatePdfConfig(config)
                        viewModel.generatePdf(context)
                    },
                    onDismiss = { viewModel.closeConfigDialog() }
                )
            }

            // Progress Dialog during PDF generation
            if (uiState.isGeneratingPdf) {
                PdfProgressDialog(
                    current = uiState.generationProgress.first,
                    total = uiState.generationProgress.second
                )
            }

            // Success Dialog
            if (uiState.isSuccessDialogVisible && uiState.lastGeneratedRecord != null) {
                PdfSuccessDialog(
                    record = uiState.lastGeneratedRecord!!,
                    onOpenPdf = { openPdf(it) },
                    onSharePdf = { sharePdf(it) },
                    onSaveToDownloads = { savePdfToDownloads(it) },
                    onPrintPdf = { printPdf(it) },
                    onDismiss = { viewModel.closeSuccessDialog() }
                )
            }

            // Error Toast / Banner
            if (uiState.errorMessage != null) {
                Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
}
