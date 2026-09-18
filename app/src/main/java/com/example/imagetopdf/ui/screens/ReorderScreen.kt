package com.example.imagetopdf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imagetopdf.model.PageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderScreen(
    pages: List<PageItem>,
    onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
    onDone: () -> Unit
) {
    // Tap-to-swap selection state
    var selectedIndexForSwap by remember { mutableStateOf<Int?>(null) }
    // Long-press hold-to-reorder state
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rearrange Pages",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                draggingIndex != null -> "Moving Page #${draggingIndex!! + 1}..."
                                selectedIndexForSwap != null -> "Tap another page to swap with #${selectedIndexForSwap!! + 1}"
                                else -> "Drag cards to reorder or tap arrows"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (draggingIndex != null || selectedIndexForSwap != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Done")
                    }
                },
                actions = {
                    Button(
                        onClick = onDone,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Informative Header Banner
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DragIndicator,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Press & hold any page to drag it, or tap the arrows / swap mode below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Grid Items with Long-Press Drag & Drop and Arrow Controls
                itemsIndexed(pages, key = { _, item -> item.id }) { index, item ->
                    val isSwapSelected = selectedIndexForSwap == index
                    val isCurrentlyDragging = draggingIndex == index

                    ReorderCard(
                        pageItem = item,
                        pageNumber = index + 1,
                        isFirst = index == 0,
                        isLast = index == pages.size - 1,
                        isSwapSelected = isSwapSelected,
                        isDragging = isCurrentlyDragging,
                        onDragActiveChange = { active ->
                            draggingIndex = if (active) index else null
                        },
                        onDragSwapRelative = { deltaRow, deltaCol ->
                            val targetIndex = (index + (deltaRow * 2) + deltaCol).coerceIn(0, pages.size - 1)
                            if (targetIndex != index) {
                                onMovePage(index, targetIndex)
                                draggingIndex = targetIndex
                            }
                        },
                        onCardClick = {
                            if (selectedIndexForSwap == null) {
                                selectedIndexForSwap = index
                            } else if (selectedIndexForSwap == index) {
                                selectedIndexForSwap = null
                            } else {
                                onMovePage(selectedIndexForSwap!!, index)
                                selectedIndexForSwap = null
                            }
                        },
                        onMoveBackward = {
                            if (index > 0) onMovePage(index, index - 1)
                        },
                        onMoveForward = {
                            if (index < pages.size - 1) onMovePage(index, index + 1)
                        },
                        onMoveToStart = {
                            if (index > 0) onMovePage(index, 0)
                        },
                        onMoveToEnd = {
                            if (index < pages.size - 1) onMovePage(index, pages.size - 1)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderCard(
    pageItem: PageItem,
    pageNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isSwapSelected: Boolean,
    isDragging: Boolean,
    onDragActiveChange: (Boolean) -> Unit,
    onDragSwapRelative: (deltaRow: Int, deltaCol: Int) -> Unit,
    onCardClick: () -> Unit,
    onMoveBackward: () -> Unit,
    onMoveForward: () -> Unit,
    onMoveToStart: () -> Unit,
    onMoveToEnd: () -> Unit
) {
    val borderColor = when {
        isDragging -> MaterialTheme.colorScheme.primary
        isSwapSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val colorFilter = remember(pageItem.filterType) {
        getColorFilterForFilterType(pageItem.filterType)
    }

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var dragAccumulatedX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                isSwapSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 12.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 2f else 1f)
            .graphicsLayer {
                if (isDragging) {
                    scaleX = 1.05f
                    scaleY = 1.05f
                    translationX = dragAccumulatedX * 0.6f
                    translationY = dragAccumulatedY * 0.6f
                } else {
                    scaleX = 1f
                    scaleY = 1f
                    translationX = 0f
                    translationY = 0f
                }
            }
            .border(
                width = if (isDragging || isSwapSelected) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .onSizeChanged { cardSize = it }
            .pointerInput(pageItem.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onDragActiveChange(true)
                        dragAccumulatedX = 0f
                        dragAccumulatedY = 0f
                    },
                    onDragEnd = {
                        onDragActiveChange(false)
                        dragAccumulatedX = 0f
                        dragAccumulatedY = 0f
                    },
                    onDragCancel = {
                        onDragActiveChange(false)
                        dragAccumulatedX = 0f
                        dragAccumulatedY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatedX += dragAmount.x
                        dragAccumulatedY += dragAmount.y

                        val thresholdX = if (cardSize.width > 0) cardSize.width * 0.45f else 180f
                        val thresholdY = if (cardSize.height > 0) cardSize.height * 0.45f else 220f

                        var deltaRow = 0
                        var deltaCol = 0

                        if (dragAccumulatedX > thresholdX) {
                            deltaCol = 1
                            dragAccumulatedX = 0f
                        } else if (dragAccumulatedX < -thresholdX) {
                            deltaCol = -1
                            dragAccumulatedX = 0f
                        }

                        if (dragAccumulatedY > thresholdY) {
                            deltaRow = 1
                            dragAccumulatedY = 0f
                        } else if (dragAccumulatedY < -thresholdY) {
                            deltaRow = -1
                            dragAccumulatedY = 0f
                        }

                        if (deltaRow != 0 || deltaCol != 0) {
                            onDragSwapRelative(deltaRow, deltaCol)
                        }
                    }
                )
            }
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header with page number badge and Drag Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDragging || isSwapSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Page $pageNumber",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDragging || isSwapSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSwapSelected) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Swap Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Swap",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // Subtle drag handle icon indicating holdability
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = "Hold to drag",
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thumbnail Image with rotation & filter
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reorder Controls Row (Arrow Keys preserved as requested)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move to First
                FilledTonalIconButton(
                    onClick = onMoveToStart,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardDoubleArrowUp,
                        contentDescription = "Move to First",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Move backward
                FilledTonalIconButton(
                    onClick = onMoveBackward,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Move backward",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Move forward
                FilledTonalIconButton(
                    onClick = onMoveForward,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move forward",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Move to Last
                FilledTonalIconButton(
                    onClick = onMoveToEnd,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardDoubleArrowDown,
                        contentDescription = "Move to Last",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
