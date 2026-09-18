package com.example.imagetopdf.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imagetopdf.model.FilterType
import com.example.imagetopdf.model.PageItem
import com.example.imagetopdf.model.PageOrientation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PreviewScreen(
    pages: List<PageItem>,
    initialPageIndex: Int,
    currentOrientation: PageOrientation,
    onToggleOrientation: (PageOrientation) -> Unit,
    onUpdateTransform: (pageId: String, scale: Float, panX: Float, panY: Float, fillPage: Boolean) -> Unit,
    onResetTransform: (String) -> Unit,
    onClose: () -> Unit,
    onRotatePage: (String) -> Unit,
    onOpenFilterSheet: (PageItem) -> Unit,
    onDeletePage: (String) -> Unit
) {
    if (pages.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, pages.size - 1),
        pageCount = { pages.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pages.getOrNull(pagerState.currentPage) ?: pages.first()

    val isLandscapeDoc = currentOrientation == PageOrientation.LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Page ${pagerState.currentPage + 1} of ${pages.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isLandscapeDoc) "Document: Landscape" else "Document: Portrait",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                // Orientation Switcher Toggle
                IconButton(onClick = {
                    val nextOrientation = if (isLandscapeDoc) PageOrientation.PORTRAIT else PageOrientation.LANDSCAPE
                    onToggleOrientation(nextOrientation)
                }) {
                    Icon(
                        if (isLandscapeDoc) Icons.Default.StayCurrentLandscape else Icons.Default.StayCurrentPortrait,
                        contentDescription = "Toggle Orientation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Filter action
                IconButton(onClick = { onOpenFilterSheet(currentPage) }) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Filter",
                        tint = Color.White
                    )
                }

                // Rotate action
                IconButton(onClick = { onRotatePage(currentPage.id) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate",
                        tint = Color.White
                    )
                }

                // Delete action
                IconButton(onClick = { onDeletePage(currentPage.id) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.85f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A1A)
            )
        )

        // Main Interactive Document Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val pageItem = pages[pageIndex]
                PageSheetEditor(
                    pageItem = pageItem,
                    pageNumber = pageIndex + 1,
                    totalPages = pages.size,
                    orientation = currentOrientation,
                    onTransformChanged = { scale, panX, panY, fillPage ->
                        onUpdateTransform(pageItem.id, scale, panX, panY, fillPage)
                    }
                )
            }

            // Quick Page Navigation arrows on left and right for easy flipping
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (pagerState.currentPage < pages.size - 1) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Layout & Position Adjustment Controls Bar
        Surface(
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Action Buttons: Fit, Fill, Zoom-, Zoom+, Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fit Center
                    OutlinedButton(
                        onClick = {
                            onUpdateTransform(currentPage.id, 1.0f, 0f, 0f, false)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Center", fontSize = 11.sp, color = Color.White)
                    }

                    // Fill Page
                    OutlinedButton(
                        onClick = {
                            onUpdateTransform(currentPage.id, 1.0f, 0f, 0f, true)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fill", fontSize = 11.sp, color = Color.White)
                    }

                    // Zoom Out
                    FilledTonalIconButton(
                        onClick = {
                            val newScale = (currentPage.scale - 0.15f).coerceIn(0.5f, 4.0f)
                            onUpdateTransform(currentPage.id, newScale, currentPage.panOffsetX, currentPage.panOffsetY, false)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                    }

                    // Scale Percentage Display
                    Text(
                        text = "${(currentPage.scale * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    // Zoom In
                    FilledTonalIconButton(
                        onClick = {
                            val newScale = (currentPage.scale + 0.15f).coerceIn(0.5f, 4.0f)
                            onUpdateTransform(currentPage.id, newScale, currentPage.panOffsetX, currentPage.panOffsetY, false)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }

                    // Reset
                    IconButton(
                        onClick = { onResetTransform(currentPage.id) },
                        modifier = Modifier.size(34.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Slide / Position Nudge Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Slide:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Left
                    FilledTonalIconButton(
                        onClick = {
                            val newPanX = (currentPage.panOffsetX - 0.12f).coerceIn(-1.5f, 1.5f)
                            onUpdateTransform(currentPage.id, currentPage.scale, newPanX, currentPage.panOffsetY, false)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Slide Left", modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Up
                    FilledTonalIconButton(
                        onClick = {
                            val newPanY = (currentPage.panOffsetY - 0.12f).coerceIn(-1.5f, 1.5f)
                            onUpdateTransform(currentPage.id, currentPage.scale, currentPage.panOffsetX, newPanY, false)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Slide Up", modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Down
                    FilledTonalIconButton(
                        onClick = {
                            val newPanY = (currentPage.panOffsetY + 0.12f).coerceIn(-1.5f, 1.5f)
                            onUpdateTransform(currentPage.id, currentPage.scale, currentPage.panOffsetX, newPanY, false)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Slide Down", modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Right
                    FilledTonalIconButton(
                        onClick = {
                            val newPanX = (currentPage.panOffsetX + 0.12f).coerceIn(-1.5f, 1.5f)
                            onUpdateTransform(currentPage.id, currentPage.scale, newPanX, currentPage.panOffsetY, false)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Slide Right", modifier = Modifier.size(15.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Gesture Tip Text
                Text(
                    text = "Tip: Drag image to slide • Pinch or tap +/- to resize",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Bottom Thumbnail Strip
        Surface(
            color = Color(0xFF141414),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(pages) { index, item ->
                        val isSelected = index == pagerState.currentPage
                        ThumbnailItem(
                            pageItem = item,
                            pageNumber = index + 1,
                            isSelected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageSheetEditor(
    pageItem: PageItem,
    pageNumber: Int,
    totalPages: Int,
    orientation: PageOrientation,
    onTransformChanged: (scale: Float, panX: Float, panY: Float, fillPage: Boolean) -> Unit
) {
    val isLandscape = orientation == PageOrientation.LANDSCAPE
    // Standard A4 aspect ratio (595 x 842 pt)
    val pageRatio = if (isLandscape) (842f / 595f) else (595f / 842f)

    val colorFilter = remember(pageItem.filterType) {
        getColorFilterForFilterType(pageItem.filterType)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val availableHeightPx = with(density) { maxHeight.toPx() }

        // Paper Page Sheet (Realistic document representation)
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .aspectRatio(pageRatio)
                .fillMaxSize()
                .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds() // Ensure image never overflows the page boundary!
                    .pointerInput(pageItem.id, pageItem.scale, pageItem.panOffsetX, pageItem.panOffsetY) {
                        detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                            val newScale = if (zoom != 1f) (pageItem.scale * zoom).coerceIn(0.5f, 4.0f) else pageItem.scale
                            // Pan relative to sheet dimensions
                            val panDeltaX = pan.x / (availableWidthPx * 0.35f)
                            val panDeltaY = pan.y / (availableHeightPx * 0.35f)
                            val newPanX = (pageItem.panOffsetX + panDeltaX).coerceIn(-1.5f, 1.5f)
                            val newPanY = (pageItem.panOffsetY + panDeltaY).coerceIn(-1.5f, 1.5f)
                            onTransformChanged(newScale, newPanX, newPanY, false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Printable Margins Guide (subtle border)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Actual Image with live scale, translation and rotation applied
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(pageItem.originalUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page content",
                        contentScale = if (pageItem.fillPage) ContentScale.Crop else ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (pageItem.fillPage) 1f else pageItem.scale,
                                scaleY = if (pageItem.fillPage) 1f else pageItem.scale,
                                translationX = pageItem.panOffsetX * (availableWidthPx * 0.25f),
                                translationY = pageItem.panOffsetY * (availableHeightPx * 0.25f),
                                rotationZ = pageItem.rotation.toFloat()
                            )
                    )
                }

                // Page Number Watermark Preview at bottom of sheet
                Text(
                    text = "Page $pageNumber of $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ThumbnailItem(
    pageItem: PageItem,
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val colorFilter = remember(pageItem.filterType) {
        getColorFilterForFilterType(pageItem.filterType)
    }

    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(pageItem.originalUri)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = Modifier
                .fillMaxSize()
                .rotate(pageItem.rotation.toFloat())
        )

        // Page number badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$pageNumber",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getColorFilterForFilterType(filterType: FilterType): ColorFilter? {
    return when (filterType) {
        FilterType.ORIGINAL -> null
        FilterType.GRAYSCALE -> {
            val cm = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(cm)
        }
        FilterType.BW_DOCUMENT -> {
            val androidGray = android.graphics.ColorMatrix().apply { setSaturation(0f) }
            val contrast = 3.2f
            val translate = (-128f * (contrast - 1f)) + 30f
            val androidContrast = android.graphics.ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            androidContrast.preConcat(androidGray)
            ColorFilter.colorMatrix(ColorMatrix(androidContrast.array))
        }
        FilterType.MAGIC_COLOR -> {
            val androidSat = android.graphics.ColorMatrix().apply { setSaturation(1.30f) }
            val contrast = 1.25f
            val translate = (-128f * (contrast - 1f)) + 12f
            val androidContrast = android.graphics.ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            androidContrast.preConcat(androidSat)
            ColorFilter.colorMatrix(ColorMatrix(androidContrast.array))
        }
        FilterType.SEPIA -> {
            val cm = ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(cm)
        }
    }
}
