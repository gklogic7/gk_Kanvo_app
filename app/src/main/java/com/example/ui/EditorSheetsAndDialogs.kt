package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AlignVerticalBottom
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.AlignVerticalTop
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CanvasLayer
import com.example.model.DesignPage
import com.example.model.ExportFormat
import com.example.model.ImageLayer
import com.example.model.KanvoShapeType
import com.example.model.PhotoFilterPreset
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.parseHexColor
import com.example.ui.theme.KanvoBackgroundGradients
import com.example.ui.theme.KanvoCanvasFonts
import com.example.ui.theme.KanvoPaletteSwatches
import com.example.ui.theme.LocalKanvoColors
import com.example.viewmodel.ActiveInspectorSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanvoInspectorSheetsHost(
    activeSheet: ActiveInspectorSheet,
    page: DesignPage?,
    selectedLayer: CanvasLayer?,
    pageCount: Int,
    searchQuery: String,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onMoveLayerOrder: (String, Boolean) -> Unit,
    onBringToFront: (String) -> Unit,
    onSendToBack: (String) -> Unit,
    onAlignSelected: (Float?, Float?) -> Unit,
    onToggleBackgroundLock: () -> Unit,
    onUpdateText: ((TextLayer) -> TextLayer) -> Unit,
    onUpdateImage: (Boolean, (ImageLayer) -> ImageLayer) -> Unit,
    onApplyFilter: (PhotoFilterPreset) -> Unit,
    onAddShape: (KanvoShapeType) -> Unit,
    onUpdateShape: (Boolean, (ShapeLayer) -> ShapeLayer) -> Unit,
    onSetBackground: (String, String?) -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onExportToGallery: (ExportFormat, Boolean) -> Unit,
    onShareCurrent: (ExportFormat) -> Unit
) {
    if (activeSheet == ActiveInspectorSheet.NONE) return

    if (activeSheet == ActiveInspectorSheet.EXPORT_DIALOG) {
        ExportDesignDialog(
            pageCount = pageCount,
            isExporting = isExporting,
            onDismiss = onDismiss,
            onExportGallery = onExportToGallery,
            onShare = onShareCurrent
        )
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalKanvoColors.current.panelSurface
    ) {
        when (activeSheet) {
            ActiveInspectorSheet.TEXT_EDIT -> {
                val textLayer = selectedLayer as? TextLayer
                if (textLayer != null) {
                    TextEditorSheetContent(
                        layer = textLayer,
                        onUpdateText = onUpdateText,
                        onDone = onDismiss
                    )
                }
            }
            ActiveInspectorSheet.TEXT_SPACING -> {
                val textLayer = selectedLayer as? TextLayer
                if (textLayer != null) {
                    TextSpacingSheetContent(layer = textLayer, onUpdateText = onUpdateText)
                }
            }
            ActiveInspectorSheet.TEXT_EFFECTS -> {
                val textLayer = selectedLayer as? TextLayer
                if (textLayer != null) {
                    TextEffectsSheetContent(layer = textLayer, onUpdateText = onUpdateText)
                }
            }
            ActiveInspectorSheet.PHOTO_ADJUST -> {
                val imgLayer = selectedLayer as? ImageLayer
                if (imgLayer != null) {
                    PhotoAdjustSheetContent(
                        layer = imgLayer,
                        onApplyFilter = onApplyFilter,
                        onUpdateImage = onUpdateImage
                    )
                }
            }
            ActiveInspectorSheet.SHAPES_ADD -> {
                ShapesSheetContent(
                    selectedShape = selectedLayer as? ShapeLayer,
                    onAddShape = onAddShape,
                    onUpdateShape = onUpdateShape
                )
            }
            ActiveInspectorSheet.BACKGROUND -> {
                BackgroundPickerSheetContent(
                    currentStartHex = page?.backgroundColorHex ?: "#FFFFFF",
                    currentEndHex = page?.backgroundGradientEndHex,
                    onSelectBackground = onSetBackground
                )
            }
            ActiveInspectorSheet.POSITION_ALIGN -> {
                if (selectedLayer != null) {
                    PositionAlignSheetContent(
                        layer = selectedLayer,
                        onAlign = onAlignSelected,
                        onBringToFront = { onBringToFront(selectedLayer.id) },
                        onSendToBack = { onSendToBack(selectedLayer.id) },
                        onMoveStep = { up -> onMoveLayerOrder(selectedLayer.id, up) }
                    )
                }
            }
            ActiveInspectorSheet.OPACITY -> {
                if (selectedLayer != null) {
                    OpacitySheetContent(
                        opacity = selectedLayer.opacity,
                        onOpacityChange = onUpdateOpacity
                    )
                }
            }
            ActiveInspectorSheet.LAYERS_SHEET -> {
                Box(modifier = Modifier.height(440.dp)) {
                    LayersPanelContent(
                        page = page,
                        selectedLayerId = selectedLayer?.id,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSelectLayer = { id ->
                            onSelectLayer(id)
                            onDismiss()
                        },
                        onToggleLock = onToggleLock,
                        onToggleVisibility = onToggleVisibility,
                        onMoveLayerOrder = onMoveLayerOrder,
                        onToggleBackgroundLock = onToggleBackgroundLock,
                        onOpenBackgroundSheet = { onDismiss() },
                        onCollapse = onDismiss
                    )
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun TextEditorSheetContent(
    layer: TextLayer,
    onUpdateText: ((TextLayer) -> TextLayer) -> Unit,
    onDone: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit Text & Typography", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onDone) { Text(stringResource(R.string.done)) }
        }

        OutlinedTextField(
            value = layer.text,
            onValueChange = { updated -> onUpdateText { it.copy(text = updated) } },
            label = { Text("Text content") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_editor_input")
        )

        Text("Font Family", style = MaterialTheme.typography.labelLarge, color = kanvoColors.inkSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(KanvoCanvasFonts.options) { fontOption ->
                val selected = fontOption.key == layer.fontFamilyKey
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateText { it.copy(fontFamilyKey = fontOption.key) } },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) kanvoColors.activePillBackground else kanvoColors.panelElevated,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) kanvoColors.selectionAccent else kanvoColors.borderSubtle
                    )
                ) {
                    Text(
                        text = fontOption.displayName,
                        fontFamily = fontOption.fontFamily,
                        color = if (selected) kanvoColors.selectionAccent else kanvoColors.inkPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Text(
            text = "Font Size: ${layer.fontSizeSp.toInt()} pt",
            style = MaterialTheme.typography.labelLarge,
            color = kanvoColors.inkSecondary
        )
        Slider(
            value = layer.fontSizeSp,
            onValueChange = { size -> onUpdateText { it.copy(fontSizeSp = size) } },
            valueRange = 14f..110f
        )

        Text("Text Color", style = MaterialTheme.typography.labelLarge, color = kanvoColors.inkSecondary)
        ColorSwatchRow(
            selectedHex = layer.colorHex,
            onSelectHex = { hex -> onUpdateText { it.copy(colorHex = hex) } }
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TextSpacingSheetContent(
    layer: TextLayer,
    onUpdateText: ((TextLayer) -> TextLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Text Spacing", style = MaterialTheme.typography.titleMedium)
        Text("Letter Spacing (${String.format("%.2f", layer.letterSpacingEm)})")
        Slider(
            value = layer.letterSpacingEm,
            onValueChange = { v -> onUpdateText { it.copy(letterSpacingEm = v) } },
            valueRange = -0.06f..0.4f
        )
        Text("Line Height (${String.format("%.2f", layer.lineHeightMultiplier)}×)")
        Slider(
            value = layer.lineHeightMultiplier,
            onValueChange = { v -> onUpdateText { it.copy(lineHeightMultiplier = v) } },
            valueRange = 0.85f..2.0f
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TextEffectsSheetContent(
    layer: TextLayer,
    onUpdateText: ((TextLayer) -> TextLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Text Effects", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Drop Shadow", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = layer.shadowEnabled,
                onCheckedChange = { enabled -> onUpdateText { it.copy(shadowEnabled = enabled) } }
            )
        }

        Text("Highlight Pill Background", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = layer.pillBackgroundHex == null,
                onClick = { onUpdateText { it.copy(pillBackgroundHex = null) } },
                label = { Text("None") }
            )
            KanvoPaletteSwatches.forEach { hex ->
                ColorDot(
                    hex = hex,
                    isSelected = layer.pillBackgroundHex.equals(hex, ignoreCase = true),
                    onClick = { onUpdateText { it.copy(pillBackgroundHex = hex) } }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PhotoAdjustSheetContent(
    layer: ImageLayer,
    onApplyFilter: (PhotoFilterPreset) -> Unit,
    onUpdateImage: (Boolean, (ImageLayer) -> ImageLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Photo Filters & Adjustments", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                onUpdateImage(true) {
                    it.copy(
                        filterPreset = PhotoFilterPreset.NONE,
                        brightness = 0f,
                        contrast = 1f,
                        saturation = 1f,
                        cornerRadiusDp = 0f
                    )
                }
            }) {
                Text("Reset")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PhotoFilterPreset.entries) { preset ->
                FilterChip(
                    selected = layer.filterPreset == preset,
                    onClick = { onApplyFilter(preset) },
                    label = { Text(preset.displayName) }
                )
            }
        }

        Text("Brightness (${(layer.brightness * 100).toInt()}%)")
        Slider(
            value = layer.brightness,
            onValueChange = { v -> onUpdateImage(false) { it.copy(brightness = v) } },
            valueRange = -0.35f..0.35f
        )

        Text("Contrast (${(layer.contrast * 100).toInt()}%)")
        Slider(
            value = layer.contrast,
            onValueChange = { v -> onUpdateImage(false) { it.copy(contrast = v) } },
            valueRange = 0.5f..1.8f
        )

        Text("Saturation (${(layer.saturation * 100).toInt()}%)")
        Slider(
            value = layer.saturation,
            onValueChange = { v -> onUpdateImage(false) { it.copy(saturation = v) } },
            valueRange = 0f..2f
        )

        Text("Corner Rounding (${layer.cornerRadiusDp.toInt()} dp)")
        Slider(
            value = layer.cornerRadiusDp,
            onValueChange = { v -> onUpdateImage(false) { it.copy(cornerRadiusDp = v) } },
            valueRange = 0f..80f
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ShapesSheetContent(
    selectedShape: ShapeLayer?,
    onAddShape: (KanvoShapeType) -> Unit,
    onUpdateShape: (Boolean, (ShapeLayer) -> ShapeLayer) -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selectedShape != null) {
            Text("Edit Shape & Badge", style = MaterialTheme.typography.titleMedium)
            Text("Fill Color", style = MaterialTheme.typography.labelLarge)
            ColorSwatchRow(
                selectedHex = selectedShape.fillColorHex,
                onSelectHex = { hex -> onUpdateShape(true) { it.copy(fillColorHex = hex) } }
            )
            Text("Border Color", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedShape.strokeColorHex == null,
                    onClick = { onUpdateShape(true) { it.copy(strokeColorHex = null, strokeWidthDp = 0f) } },
                    label = { Text("No border") }
                )
                KanvoPaletteSwatches.forEach { hex ->
                    ColorDot(
                        hex = hex,
                        isSelected = selectedShape.strokeColorHex.equals(hex, ignoreCase = true),
                        onClick = {
                            onUpdateShape(true) {
                                it.copy(
                                    strokeColorHex = hex,
                                    strokeWidthDp = if (it.strokeWidthDp <= 0f) 3f else it.strokeWidthDp
                                )
                            }
                        }
                    )
                }
            }
            if (selectedShape.shapeType == KanvoShapeType.ROUNDED_RECT) {
                Text("Corner Radius (${selectedShape.cornerRadiusDp.toInt()} dp)")
                Slider(
                    value = selectedShape.cornerRadiusDp,
                    onValueChange = { r -> onUpdateShape(false) { it.copy(cornerRadiusDp = r) } },
                    valueRange = 0f..64f
                )
            }
            HorizontalDivider(color = kanvoColors.borderSubtle)
        }

        Text("Add Shape or Badge", style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(210.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(KanvoShapeType.entries) { shapeType ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAddShape(shapeType) }
                        .testTag("shape_option_${shapeType.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = kanvoColors.panelElevated,
                    border = BorderStroke(1.dp, kanvoColors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shapeType.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = kanvoColors.inkPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundPickerSheetContent(
    currentStartHex: String,
    currentEndHex: String?,
    onSelectBackground: (String, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Page Background", style = MaterialTheme.typography.titleMedium)

        Text("Solid Colors", style = MaterialTheme.typography.labelLarge)
        ColorSwatchRow(
            selectedHex = if (currentEndHex == null) currentStartHex else "",
            onSelectHex = { hex -> onSelectBackground(hex, null) }
        )

        Text("Studio Gradients", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(KanvoBackgroundGradients) { pair ->
                val startHex = pair[0]
                val endHex = pair[1]
                val isSelected = currentStartHex.equals(startHex, true) && currentEndHex.equals(endHex, true)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(parseHexColor(startHex), parseHexColor(endHex))
                            )
                        )
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) LocalKanvoColors.current.selectionAccent else LocalKanvoColors.current.borderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectBackground(startHex, endHex) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PositionAlignSheetContent(
    layer: CanvasLayer,
    onAlign: (Float?, Float?) -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onMoveStep: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Position & Arrange", style = MaterialTheme.typography.titleMedium)

        Text("Layer Order", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBringToFront, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FlipToFront, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("To Front")
            }
            OutlinedButton(onClick = { onMoveStep(true) }, modifier = Modifier.weight(1f)) {
                Text("Forward")
            }
            OutlinedButton(onClick = { onMoveStep(false) }, modifier = Modifier.weight(1f)) {
                Text("Backward")
            }
            OutlinedButton(onClick = onSendToBack, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FlipToBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("To Back")
            }
        }

        Text("Align to Page", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onAlign(layer.widthFraction / 2f, null) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.AlignHorizontalLeft, contentDescription = "Align left") }
            OutlinedButton(
                onClick = { onAlign(0.5f, null) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.AlignHorizontalCenter, contentDescription = "Align center") }
            OutlinedButton(
                onClick = { onAlign(1f - layer.widthFraction / 2f, null) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.AlignHorizontalRight, contentDescription = "Align right") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onAlign(null, layer.heightFraction / 2f) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.AlignVerticalTop, contentDescription = "Align top") }
            OutlinedButton(
                onClick = { onAlign(null, 0.5f) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.AlignVerticalCenter, contentDescription = "Align middle") }
            OutlinedButton(
                onClick = { onAlign(null, 1f - layer.heightFraction / 2f) },
                modifier = Modifier.weight(1f)
            ) { Icon(Icons.Default.AlignVerticalBottom, contentDescription = "Align bottom") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OpacitySheetContent(
    opacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Transparency • ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = opacity,
            onValueChange = onOpacityChange,
            valueRange = 0.05f..1f
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExportDesignDialog(
    pageCount: Int,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExportGallery: (ExportFormat, Boolean) -> Unit,
    onShare: (ExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var exportAllPages by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Design") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("File Format", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.label) }
                        )
                    }
                }

                if (pageCount > 1) {
                    Text("Pages", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !exportAllPages,
                            onClick = { exportAllPages = false },
                            label = { Text("Current page") }
                        )
                        FilterChip(
                            selected = exportAllPages,
                            onClick = { exportAllPages = true },
                            label = { Text("All $pageCount pages") }
                        )
                    }
                }

                if (isExporting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Rendering high-resolution export…")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExportGallery(selectedFormat, exportAllPages) },
                enabled = !isExporting,
                modifier = Modifier.testTag("confirm_save_gallery_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.save_to_gallery))
            }
        },
        dismissButton = {
            Row {
                OutlinedButton(
                    onClick = { onShare(selectedFormat) },
                    enabled = !isExporting
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.share_image))
                }
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun ColorSwatchRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KanvoPaletteSwatches.forEach { hex ->
            ColorDot(
                hex = hex,
                isSelected = selectedHex.equals(hex, ignoreCase = true),
                onClick = { onSelectHex(hex) }
            )
        }
    }
}

@Composable
private fun ColorDot(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    val color = parseHexColor(hex)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) kanvoColors.selectionAccent else kanvoColors.borderSubtle,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            val checkTint = if (hex.equals("#FFFFFF", true) || hex.equals("#EAE6DF", true) || hex.equals("#FDE68A", true)) {
                Color.Black
            } else Color.White
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
