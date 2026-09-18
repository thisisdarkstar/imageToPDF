package com.example.imagetopdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagetopdf.model.ImageQuality
import com.example.imagetopdf.model.PageMargin
import com.example.imagetopdf.model.PageOrientation
import com.example.imagetopdf.model.PageSizeOption
import com.example.imagetopdf.model.PdfConfig
import com.example.imagetopdf.model.WatermarkColor
import com.example.imagetopdf.model.WatermarkConfig
import com.example.imagetopdf.model.WatermarkPreset
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Security

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PdfConfigDialog(
    initialConfig: PdfConfig,
    pageCount: Int,
    onConfirm: (PdfConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fileName by remember { mutableStateOf(initialConfig.fileName) }
    var selectedPageSize by remember { mutableStateOf(initialConfig.pageSize) }
    var selectedOrientation by remember { mutableStateOf(initialConfig.orientation) }
    var selectedMargin by remember { mutableStateOf(initialConfig.margin) }
    var selectedQuality by remember { mutableStateOf(initialConfig.quality) }
    var addPageNumbers by remember { mutableStateOf(initialConfig.addPageNumbers) }

    // Watermark State
    var watermarkEnabled by remember { mutableStateOf(initialConfig.watermark.enabled) }
    var watermarkText by remember { mutableStateOf(initialConfig.watermark.text) }
    var watermarkPreset by remember { mutableStateOf(initialConfig.watermark.preset) }
    var watermarkColor by remember { mutableStateOf(initialConfig.watermark.color) }
    var watermarkOpacity by remember { mutableStateOf(initialConfig.watermark.opacity) }
    var watermarkDiagonal by remember { mutableStateOf(initialConfig.watermark.isDiagonal) }

    val estimatedKb = when (selectedQuality) {
        ImageQuality.CRYSTAL_CLEAR -> pageCount * 450
        ImageQuality.BALANCED -> pageCount * 160
        ImageQuality.COMPACT -> pageCount * 65
    }
    val estimatedSizeStr = if (estimatedKb >= 1000) {
        String.format(java.util.Locale.US, "%.1f MB", estimatedKb / 1024f)
    } else {
        "$estimatedKb KB"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PDF Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$pageCount page${if (pageCount > 1) "s" else ""} ready to compile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " • Est. ~$estimatedSizeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // File Name Input
            Text(
                text = "Document File Name",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                singleLine = true,
                placeholder = { Text("Enter document name") },
                suffix = { Text(".pdf", color = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Image Quality / Resolution
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HighQuality,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Resolution & Quality",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageQuality.values().forEach { quality ->
                    val isSelected = quality == selectedQuality
                    OptionCard(
                        title = quality.displayName,
                        isSelected = isSelected,
                        onClick = { selectedQuality = quality }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Page Size Selection
            Text(
                text = "Page Format",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PageSizeOption.values().forEach { size ->
                    val isSelected = size == selectedPageSize
                    ChipOption(
                        label = size.displayName.split(" ")[0] + if (size == PageSizeOption.FIT_TO_IMAGE) " (Exact)" else "",
                        isSelected = isSelected,
                        onClick = { selectedPageSize = size }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Orientation Selection
            Text(
                text = "Document Orientation",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keeps all pages uniformly oriented for a clean document look",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PageOrientation.values().forEach { orientation ->
                    val isSelected = orientation == selectedOrientation
                    val label = when (orientation) {
                        PageOrientation.PORTRAIT -> "Portrait"
                        PageOrientation.LANDSCAPE -> "Landscape"
                        PageOrientation.AUTO -> "Auto"
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ChipOption(
                            label = label,
                            isSelected = isSelected,
                            onClick = { selectedOrientation = orientation },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Margin Selection
            Text(
                text = "Page Margins",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PageMargin.values().forEach { margin ->
                    val isSelected = margin == selectedMargin
                    Box(modifier = Modifier.weight(1f)) {
                        ChipOption(
                            label = margin.displayName.split(" ")[0],
                            isSelected = isSelected,
                            onClick = { selectedMargin = margin },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Page Numbers Checkbox
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { addPageNumbers = !addPageNumbers }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addPageNumbers,
                        onCheckedChange = { addPageNumbers = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Add Page Numbers",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Watermark & Security Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { watermarkEnabled = !watermarkEnabled },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = watermarkEnabled,
                            onCheckedChange = { watermarkEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Document Watermark Stamp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (watermarkEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        // Preset chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WatermarkPreset.values().forEach { preset ->
                                val isSelected = preset == watermarkPreset
                                ChipOption(
                                    label = preset.label,
                                    isSelected = isSelected,
                                    onClick = {
                                        watermarkPreset = preset
                                        if (preset != WatermarkPreset.CUSTOM) {
                                            watermarkText = preset.label
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = {
                                watermarkText = it
                                watermarkPreset = WatermarkPreset.CUSTOM
                            },
                            singleLine = true,
                            label = { Text("Watermark Text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Style & Angle
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ChipOption(
                                    label = "Diagonal (45°)",
                                    isSelected = watermarkDiagonal,
                                    onClick = { watermarkDiagonal = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ChipOption(
                                    label = "Horizontal",
                                    isSelected = !watermarkDiagonal,
                                    onClick = { watermarkDiagonal = false },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        // Color Selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WatermarkColor.values().forEach { color ->
                                val isSelected = color == watermarkColor
                                Box(modifier = Modifier.weight(1f)) {
                                    ChipOption(
                                        label = color.displayName.split(" ")[0],
                                        isSelected = isSelected,
                                        onClick = { watermarkColor = color },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Generate Button
            Button(
                onClick = {
                    onConfirm(
                        PdfConfig(
                            fileName = fileName,
                            pageSize = selectedPageSize,
                            orientation = selectedOrientation,
                            margin = selectedMargin,
                            quality = selectedQuality,
                            addPageNumbers = addPageNumbers,
                            watermark = WatermarkConfig(
                                enabled = watermarkEnabled,
                                text = if (watermarkText.isNotBlank()) watermarkText else "CONFIDENTIAL",
                                preset = watermarkPreset,
                                color = watermarkColor,
                                opacity = watermarkOpacity,
                                isDiagonal = watermarkDiagonal
                            )
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Document",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ChipOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
