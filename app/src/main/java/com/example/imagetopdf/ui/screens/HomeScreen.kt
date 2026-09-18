package com.example.imagetopdf.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.imagetopdf.theme.MoonIndigo
import com.example.imagetopdf.theme.SunAmber
import com.example.imagetopdf.theme.ThemeMode
import com.example.imagetopdf.ui.components.AboutAndPrivacyDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imagetopdf.model.FilterType
import com.example.imagetopdf.model.PageItem
import com.example.imagetopdf.model.PageOrientation

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    pages: List<PageItem>,
    selectedPageIds: Set<String> = emptySet(),
    historyCount: Int,
    documentOrientation: PageOrientation = PageOrientation.PORTRAIT,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onToggleTheme: () -> Unit = {},
    onSetThemeMode: (ThemeMode) -> Unit = {},
    onToggleOrientation: (PageOrientation) -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBatchDelete: () -> Unit = {},
    onBatchRotate: () -> Unit = {},
    onDuplicatePage: (String) -> Unit = {},
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onPageClick: (Int) -> Unit,
    onRotatePage: (String) -> Unit,
    onRotateAll: () -> Unit,
    onFilterPage: (PageItem) -> Unit,
    onFilterAll: () -> Unit,
    onDeletePage: (String) -> Unit,
    onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorderClick: () -> Unit,
    onClearAll: () -> Unit,
    onOpenHistory: () -> Unit,
    onConvertClick: () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "themeRotation"
    )

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = selectedPageIds.isNotEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "TopAppBarTransition"
            ) { isSelecting ->
                if (isSelecting) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        },
                        title = {
                            Text(
                                text = "${selectedPageIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    if (selectedPageIds.size == pages.size) onClearSelection() else onSelectAll()
                                }
                            ) {
                                Icon(
                                    Icons.Default.SelectAll,
                                    contentDescription = if (selectedPageIds.size == pages.size) "Deselect All" else "Select All"
                                )
                            }
                            IconButton(onClick = onBatchRotate) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Selected")
                            }
                            IconButton(onClick = onBatchDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ImageToPdf free",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Studio Quality • Private by Design",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        actions = {
                            // Theme toggle button with 1-tap quick toggle and long-press for options menu
                            var showThemeMenu by remember { mutableStateOf(false) }
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = onToggleTheme,
                                            onLongClick = { showThemeMenu = true }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Crossfade(targetState = isDark, label = "ThemeIconCrossfade") { dark ->
                                        Icon(
                                            imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = "Theme: ${themeMode.name}. Tap to switch, hold for menu",
                                            tint = if (dark) MoonIndigo else SunAmber,
                                            modifier = Modifier.rotate(rotationAngle)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showThemeMenu,
                                    onDismissRequest = { showThemeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Light Mode") },
                                        leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, tint = SunAmber) },
                                        trailingIcon = {
                                            if (themeMode == ThemeMode.LIGHT) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            onSetThemeMode(ThemeMode.LIGHT)
                                            showThemeMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dark Mode") },
                                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, tint = MoonIndigo) },
                                        trailingIcon = {
                                            if (themeMode == ThemeMode.DARK) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            onSetThemeMode(ThemeMode.DARK)
                                            showThemeMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("System Default") },
                                        leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) },
                                        trailingIcon = {
                                            if (themeMode == ThemeMode.SYSTEM) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            onSetThemeMode(ThemeMode.SYSTEM)
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }

                            // History button
                            IconButton(onClick = onOpenHistory) {
                                Box {
                                    Icon(Icons.Default.History, contentDescription = "History")
                                    if (historyCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (historyCount > 9) "9+" else "$historyCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Clear all button (only when items exist)
                            if (pages.isNotEmpty()) {
                                IconButton(onClick = onClearAll) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Clear all",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (pages.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${pages.size} Page${if (pages.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High-Fidelity Output",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = onConvertClick,
                            modifier = Modifier
                                .height(48.dp)
                                .width(180.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Build PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (pages.isEmpty()) {
            EmptyHomeContent(
                paddingValues = paddingValues,
                onCamera = onLaunchCamera,
                onGallery = onLaunchGallery,
                onOpenHistory = onOpenHistory
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar Header (Add more, Filter all, Rotate all, Reorder)
                item(span = { GridItemSpan(2) }) {
                    StagingToolbar(
                        pageCount = pages.size,
                        documentOrientation = documentOrientation,
                        onToggleOrientation = onToggleOrientation,
                        onAddCamera = onLaunchCamera,
                        onAddGallery = onLaunchGallery,
                        onFilterAll = onFilterAll,
                        onRotateAll = onRotateAll,
                        onReorder = onReorderClick
                    )
                }

                // Grid of Page Cards
                itemsIndexed(pages, key = { _, item -> item.id }) { index, item ->
                    val isSelected = selectedPageIds.contains(item.id)
                    PageGridCard(
                        modifier = Modifier.animateItem(),
                        pageItem = item,
                        pageNumber = index + 1,
                        isSelected = isSelected,
                        isFirst = index == 0,
                        isLast = index == pages.size - 1,
                        onCardClick = {
                            if (selectedPageIds.isNotEmpty()) {
                                onToggleSelect(item.id)
                            } else {
                                onPageClick(index)
                            }
                        },
                        onToggleSelect = { onToggleSelect(item.id) },
                        onDuplicate = { onDuplicatePage(item.id) },
                        onRotate = { onRotatePage(item.id) },
                        onFilter = { onFilterPage(item) },
                        onDelete = { onDeletePage(item.id) },
                        onMoveBackward = { if (index > 0) onMovePage(index, index - 1) },
                        onMoveForward = { if (index < pages.size - 1) onMovePage(index, index + 1) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHomeContent(
    paddingValues: PaddingValues,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Visual Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Turn Photos into Clean PDFs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Capture physical documents or import gallery photos. Rendered locally on your device with complete privacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Primary Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Camera Button
            Button(
                onClick = onCamera,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Scan Document", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Gallery Button
            FilledTonalButton(
                onClick = onGallery,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Import Photos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feature Highlights Pills
        FeaturePill(icon = Icons.Default.HighQuality, title = "Lossless Clarity", desc = "Preserves every detail at full sensor resolution")
        Spacer(modifier = Modifier.height(8.dp))
        FeaturePill(icon = Icons.Default.FilterVintage, title = "Document Filters", desc = "Crisp B&W binarization, grayscale & magic tone boost")
        Spacer(modifier = Modifier.height(8.dp))
        FeaturePill(icon = Icons.Default.SwapVert, title = "Seamless Reordering", desc = "Rearrange, duplicate, or rotate pages on the fly")
        Spacer(modifier = Modifier.height(8.dp))
        FeaturePill(icon = Icons.Default.Security, title = "Private by Design", desc = "Zero cloud, zero uploads — entirely on your device")
    }
}

@Composable
fun FeaturePill(icon: ImageVector, title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StagingToolbar(
    pageCount: Int,
    documentOrientation: PageOrientation,
    onToggleOrientation: (PageOrientation) -> Unit,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onFilterAll: () -> Unit,
    onRotateAll: () -> Unit,
    onReorder: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Pages ($pageCount)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onAddCamera,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onAddGallery,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Batch action chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReorder,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reorder", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onFilterAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filter All", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRotateAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate All", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PDF Orientation Format Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Document Format:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = documentOrientation == PageOrientation.PORTRAIT,
                        onClick = { onToggleOrientation(PageOrientation.PORTRAIT) },
                        label = { Text("Portrait", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.StayCurrentPortrait,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = documentOrientation == PageOrientation.LANDSCAPE,
                        onClick = { onToggleOrientation(PageOrientation.LANDSCAPE) },
                        label = { Text("Landscape", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.StayCurrentLandscape,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PageGridCard(
    pageItem: PageItem,
    pageNumber: Int,
    isSelected: Boolean = false,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onRotate: () -> Unit,
    onFilter: () -> Unit,
    onDelete: () -> Unit,
    onMoveBackward: () -> Unit,
    onMoveForward: () -> Unit
) {
    val colorFilter = remember(pageItem.filterType) {
        getColorFilterForFilterType(pageItem.filterType)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header with selection checkbox, page badge, filter indicator, duplicate and delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleSelect,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Deselect" else "Select",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "#$pageNumber",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (pageItem.filterType != FilterType.ORIGINAL) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = pageItem.filterType.displayName,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Thumbnail Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pageItem.originalUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(pageItem.rotation.toFloat())
                )

                // Magnifying zoom overlay indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = "Zoom",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move backward
                FilledTonalIconButton(
                    onClick = onMoveBackward,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(14.dp))
                }

                // Filter button
                FilledTonalIconButton(
                    onClick = onFilter,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Filter", modifier = Modifier.size(14.dp))
                }

                // Rotate button
                FilledTonalIconButton(
                    onClick = onRotate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(14.dp))
                }

                // Move forward
                FilledTonalIconButton(
                    onClick = onMoveForward,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
