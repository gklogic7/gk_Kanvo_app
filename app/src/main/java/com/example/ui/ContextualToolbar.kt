package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImagesearchRoller
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CanvasLayer
import com.example.model.ImageLayer
import com.example.model.KanvoTextAlign
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.ThemeMode
import com.example.model.parseHexColor
import com.example.ui.theme.KanvoCanvasFonts
import com.example.ui.theme.LocalKanvoColors
import com.example.viewmodel.ActiveInspectorSheet

@Composable
fun EditorTopStudioBar(
    projectTitle: String,
    canUndo: Boolean,
    canRedo: Boolean,
    themeMode: ThemeMode,
    isLayersOpen: Boolean,
    isWideLayout: Boolean,
    onBackHome: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleLayers: () -> Unit,
    onAddText: () -> Unit,
    onAddPhoto: () -> Unit,
    onOpenShapes: () -> Unit,
    onOpenBackground: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenExport: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = kanvoColors.panelSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Home, Layers Toggle (if collapsed or wide), Undo/Redo
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackHome,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("editor_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.cd_back_home),
                        tint = kanvoColors.inkPrimary
                    )
                }

                if (isWideLayout && !isLayersOpen) {
                    IconButton(
                        onClick = onToggleLayers,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("expand_layers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = stringResource(R.string.cd_expand_layers),
                            tint = kanvoColors.selectionAccent
                        )
                    }
                }

                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.undo),
                        tint = if (canUndo) kanvoColors.inkPrimary else kanvoColors.inkSecondary.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.redo),
                        tint = if (canRedo) kanvoColors.inkPrimary else kanvoColors.inkSecondary.copy(alpha = 0.4f)
                    )
                }

                if (isWideLayout) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = kanvoColors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            // Center Quick-Add Tools on Medium/Expanded Tablets
            if (isWideLayout) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickToolChip(Icons.Default.Title, "Text", onAddText, "add_text_chip")
                    QuickToolChip(Icons.Default.AddPhotoAlternate, "Photos", onAddPhoto, "add_photo_chip")
                    QuickToolChip(Icons.Default.Category, "Shapes", onOpenShapes, "add_shape_chip")
                    QuickToolChip(Icons.Default.Palette, "Background", onOpenBackground, "bg_chip")
                }
            }

            // Right: Theme + Export
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCycleTheme,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("editor_theme_button")
                ) {
                    val icon = when (themeMode) {
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.cd_toggle_theme),
                        tint = kanvoColors.inkSecondary
                    )
                }

                Spacer(Modifier.width(4.dp))

                Button(
                    onClick = onOpenExport,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("export_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = kanvoColors.selectionAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.export_design))
                }
            }
        }
    }
}

@Composable
fun ContextualFormattingBar(
    selectedLayer: CanvasLayer?,
    isStyleCopyActive: Boolean,
    onOpenSheet: (ActiveInspectorSheet) -> Unit,
    onUpdateText: ((TextLayer) -> TextLayer) -> Unit,
    onCycleTextAlign: () -> Unit,
    onToggleCopyStyle: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = kanvoColors.panelSurface
    ) {
        Column {
            HorizontalDivider(color = kanvoColors.borderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (selectedLayer) {
                    is TextLayer -> {
                        val fontOption = KanvoCanvasFonts.get(selectedLayer.fontFamilyKey)
                        // Font & Size Pill
                        ToolbarActionPill(
                            label = "${fontOption.displayName} • ${selectedLayer.fontSizeSp.toInt()}",
                            isActive = false,
                            onClick = { onOpenSheet(ActiveInspectorSheet.TEXT_EDIT) },
                            testTag = "toolbar_font_button"
                        )

                        ToolbarVerticalSeparator()

                        // Text Color Swatch ("A" with color underline)
                        ToolbarColorSwatchButton(
                            hex = selectedLayer.colorHex,
                            label = "A",
                            onClick = { onOpenSheet(ActiveInspectorSheet.TEXT_EDIT) }
                        )

                        // Bold
                        ToolbarToggleIcon(
                            icon = Icons.Default.FormatBold,
                            contentDescription = "Bold",
                            isActive = selectedLayer.isBold,
                            onClick = { onUpdateText { it.copy(isBold = !it.isBold) } },
                            testTag = "toolbar_bold"
                        )
                        // Italic
                        ToolbarToggleIcon(
                            icon = Icons.Default.FormatItalic,
                            contentDescription = "Italic",
                            isActive = selectedLayer.isItalic,
                            onClick = { onUpdateText { it.copy(isItalic = !it.isItalic) } },
                            testTag = "toolbar_italic"
                        )
                        // Underline
                        ToolbarToggleIcon(
                            icon = Icons.Default.FormatUnderlined,
                            contentDescription = "Underline",
                            isActive = selectedLayer.isUnderlined,
                            onClick = { onUpdateText { it.copy(isUnderlined = !it.isUnderlined) } },
                            testTag = "toolbar_underline"
                        )
                        // Strikethrough
                        ToolbarToggleIcon(
                            icon = Icons.Default.FormatStrikethrough,
                            contentDescription = "Strikethrough",
                            isActive = selectedLayer.isStrikethrough,
                            onClick = { onUpdateText { it.copy(isStrikethrough = !it.isStrikethrough) } },
                            testTag = "toolbar_strikethrough"
                        )
                        // Case (aA)
                        ToolbarTextChipToggle(
                            label = "aA",
                            isActive = selectedLayer.isUppercase,
                            onClick = { onUpdateText { it.copy(isUppercase = !it.isUppercase) } },
                            testTag = "toolbar_uppercase"
                        )

                        ToolbarVerticalSeparator()

                        // Alignment
                        val alignIcon = when (selectedLayer.textAlign) {
                            KanvoTextAlign.START -> Icons.AutoMirrored.Filled.FormatAlignLeft
                            KanvoTextAlign.CENTER -> Icons.Default.FormatAlignCenter
                            KanvoTextAlign.END -> Icons.AutoMirrored.Filled.FormatAlignRight
                        }
                        ToolbarToggleIcon(
                            icon = alignIcon,
                            contentDescription = "Alignment",
                            isActive = false,
                            onClick = onCycleTextAlign,
                            testTag = "toolbar_align"
                        )

                        // Spacing
                        ToolbarToggleIcon(
                            icon = Icons.Default.FormatLineSpacing,
                            contentDescription = "Letter and line spacing",
                            isActive = false,
                            onClick = { onOpenSheet(ActiveInspectorSheet.TEXT_SPACING) },
                            testTag = "toolbar_spacing"
                        )

                        // Opacity
                        ToolbarToggleIcon(
                            icon = Icons.Default.Opacity,
                            contentDescription = "Transparency",
                            isActive = selectedLayer.opacity < 0.99f,
                            onClick = { onOpenSheet(ActiveInspectorSheet.OPACITY) },
                            testTag = "toolbar_opacity"
                        )

                        ToolbarVerticalSeparator()

                        // Effects, Position, Copy Style (matching reference top bar)
                        ToolbarActionPill(
                            label = "Effects",
                            isActive = selectedLayer.shadowEnabled || selectedLayer.pillBackgroundHex != null,
                            onClick = { onOpenSheet(ActiveInspectorSheet.TEXT_EFFECTS) },
                            testTag = "toolbar_effects"
                        )
                        ToolbarActionPill(
                            label = "Position",
                            isActive = false,
                            onClick = { onOpenSheet(ActiveInspectorSheet.POSITION_ALIGN) },
                            testTag = "toolbar_position"
                        )
                        ToolbarToggleIcon(
                            icon = Icons.Default.ImagesearchRoller,
                            contentDescription = "Copy style",
                            isActive = isStyleCopyActive,
                            onClick = onToggleCopyStyle,
                            testTag = "toolbar_copy_style"
                        )
                    }

                    is ImageLayer -> {
                        ToolbarActionPill(
                            label = "Filters • ${selectedLayer.filterPreset.displayName}",
                            isActive = selectedLayer.filterPreset.name != "NONE",
                            icon = Icons.Default.AutoAwesome,
                            onClick = { onOpenSheet(ActiveInspectorSheet.PHOTO_ADJUST) },
                            testTag = "toolbar_photo_filters"
                        )
                        ToolbarActionPill(
                            label = "Adjust",
                            isActive = false,
                            icon = Icons.Default.Tune,
                            onClick = { onOpenSheet(ActiveInspectorSheet.PHOTO_ADJUST) },
                            testTag = "toolbar_photo_adjust"
                        )
                        ToolbarToggleIcon(
                            icon = Icons.Default.Opacity,
                            contentDescription = "Transparency",
                            isActive = selectedLayer.opacity < 0.99f,
                            onClick = { onOpenSheet(ActiveInspectorSheet.OPACITY) },
                            testTag = "toolbar_opacity"
                        )
                        ToolbarActionPill(
                            label = "Position",
                            isActive = false,
                            onClick = { onOpenSheet(ActiveInspectorSheet.POSITION_ALIGN) },
                            testTag = "toolbar_position"
                        )
                        ToolbarToggleIcon(
                            icon = Icons.Default.ImagesearchRoller,
                            contentDescription = "Copy style",
                            isActive = isStyleCopyActive,
                            onClick = onToggleCopyStyle,
                            testTag = "toolbar_copy_style"
                        )
                    }

                    is ShapeLayer -> {
                        ToolbarColorSwatchButton(
                            hex = selectedLayer.fillColorHex,
                            label = null,
                            onClick = { onOpenSheet(ActiveInspectorSheet.SHAPES_ADD) }
                        )
                        ToolbarActionPill(
                            label = selectedLayer.shapeType.displayName,
                            isActive = false,
                            onClick = { onOpenSheet(ActiveInspectorSheet.SHAPES_ADD) },
                            testTag = "toolbar_shape_style"
                        )
                        ToolbarToggleIcon(
                            icon = Icons.Default.Opacity,
                            contentDescription = "Transparency",
                            isActive = selectedLayer.opacity < 0.99f,
                            onClick = { onOpenSheet(ActiveInspectorSheet.OPACITY) },
                            testTag = "toolbar_opacity"
                        )
                        ToolbarActionPill(
                            label = "Position",
                            isActive = false,
                            onClick = { onOpenSheet(ActiveInspectorSheet.POSITION_ALIGN) },
                            testTag = "toolbar_position"
                        )
                        ToolbarToggleIcon(
                            icon = Icons.Default.ImagesearchRoller,
                            contentDescription = "Copy style",
                            isActive = isStyleCopyActive,
                            onClick = onToggleCopyStyle,
                            testTag = "toolbar_copy_style"
                        )
                    }

                    null -> {
                        Text(
                            text = "Tap an element on canvas or in Layers to customize",
                            style = MaterialTheme.typography.bodyMedium,
                            color = kanvoColors.inkSecondary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = kanvoColors.borderSubtle)
        }
    }
}

@Composable
private fun QuickToolChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val kanvoColors = LocalKanvoColors.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = kanvoColors.panelElevated,
        border = BorderStroke(1.dp, kanvoColors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = kanvoColors.inkPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = kanvoColors.inkPrimary
            )
        }
    }
}

@Composable
private fun ToolbarToggleIcon(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val kanvoColors = LocalKanvoColors.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) kanvoColors.activePillBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) kanvoColors.selectionAccent else kanvoColors.inkPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarTextChipToggle(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val kanvoColors = LocalKanvoColors.current
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) kanvoColors.activePillBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isActive) kanvoColors.selectionAccent else kanvoColors.inkPrimary
        )
    }
}

@Composable
private fun ToolbarActionPill(
    label: String,
    isActive: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    testTag: String
) {
    val kanvoColors = LocalKanvoColors.current
    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) kanvoColors.activePillBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) kanvoColors.selectionAccent else kanvoColors.inkPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) kanvoColors.selectionAccent else kanvoColors.inkPrimary
        )
    }
}

@Composable
private fun ToolbarColorSwatchButton(
    hex: String,
    label: String?,
    onClick: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    val color = parseHexColor(hex)
    Column(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = kanvoColors.inkPrimary
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
                    .border(0.5.dp, kanvoColors.borderSubtle, RoundedCornerShape(2.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.5.dp, kanvoColors.borderSubtle, CircleShape)
            )
        }
    }
}

@Composable
private fun ToolbarVerticalSeparator() {
    val kanvoColors = LocalKanvoColors.current
    VerticalDivider(
        color = kanvoColors.borderSubtle,
        modifier = Modifier
            .height(22.dp)
            .padding(horizontal = 4.dp)
    )
}
