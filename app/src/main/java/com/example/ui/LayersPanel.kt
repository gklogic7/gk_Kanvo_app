package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CanvasLayer
import com.example.model.DesignPage
import com.example.model.ImageLayer
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.parseHexColor
import com.example.ui.theme.KanvoCanvasFonts
import com.example.ui.theme.LocalKanvoColors
import java.io.File

@Composable
fun LayersPanelContent(
    page: DesignPage?,
    selectedLayerId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onToggleLock: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onMoveLayerOrder: (String, Boolean) -> Unit,
    onToggleBackgroundLock: () -> Unit,
    onOpenBackgroundSheet: () -> Unit,
    onCollapse: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val kanvoColors = LocalKanvoColors.current
    val layersTopFirst = (page?.layers ?: emptyList()).asReversed()
    val filteredLayers = if (searchQuery.isBlank()) {
        layersTopFirst
    } else {
        layersTopFirst.filter { layer ->
            val q = searchQuery.trim().lowercase()
            layer.name.lowercase().contains(q) ||
                (layer is TextLayer && layer.text.lowercase().contains(q))
        }
    }

    Surface(
        modifier = modifier,
        color = kanvoColors.panelSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // Header ("Layers" + <<)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.layers_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = kanvoColors.inkPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                if (onCollapse != null) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("collapse_layers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowLeft,
                            contentDescription = stringResource(R.string.cd_collapse_layers),
                            tint = kanvoColors.inkSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_layers_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = kanvoColors.inkSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = kanvoColors.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = kanvoColors.selectionAccent,
                    unfocusedBorderColor = kanvoColors.borderSubtle,
                    focusedContainerColor = kanvoColors.panelElevated,
                    unfocusedContainerColor = kanvoColors.panelElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("layers_search_input")
            )

            Spacer(Modifier.height(12.dp))

            // Layers List
            if (filteredLayers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No elements yet.\nAdd text, photos, or shapes." else "No matching layers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = kanvoColors.inkSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredLayers, key = { it.id }) { layer ->
                        val isSelected = layer.id == selectedLayerId
                        LayerRowCard(
                            layer = layer,
                            isSelected = isSelected,
                            onClick = { onSelectLayer(layer.id) },
                            onToggleLock = { onToggleLock(layer.id) },
                            onToggleVisibility = { onToggleVisibility(layer.id) },
                            onMoveUp = { onMoveLayerOrder(layer.id, true) },
                            onMoveDown = { onMoveLayerOrder(layer.id, false) }
                        )
                    }
                }
            }

            HorizontalDivider(color = kanvoColors.borderSubtle, modifier = Modifier.padding(vertical = 8.dp))

            // Background Layer Row (Matches reference bottom row)
            if (page != null) {
                val bgStart = parseHexColor(page.backgroundColorHex)
                val bgEnd = page.backgroundGradientEndHex?.let { parseHexColor(it) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenBackgroundSheet() }
                        .testTag("background_layer_row"),
                    shape = RoundedCornerShape(12.dp),
                    color = kanvoColors.panelSurface,
                    border = BorderStroke(1.dp, kanvoColors.borderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (bgEnd != null) {
                                        Modifier.background(Brush.linearGradient(listOf(bgStart, bgEnd)))
                                    } else {
                                        Modifier.background(bgStart)
                                    }
                                )
                                .border(1.dp, kanvoColors.borderSubtle, RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.background_layer_name),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = kanvoColors.inkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onToggleBackgroundLock,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (page.isBackgroundLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                                contentDescription = stringResource(R.string.cd_lock_layer),
                                tint = kanvoColors.inkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerRowCard(
    layer: CanvasLayer,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    val bgColor = if (isSelected) kanvoColors.activePillBackground else kanvoColors.panelSurface
    val borderColor = if (isSelected) kanvoColors.selectionAccent else kanvoColors.borderSubtle
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (layer.isVisible) 1f else 0.55f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("layer_row_${layer.id}"),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                LayerThumbnail(layer = layer)

                Spacer(Modifier.width(10.dp))

                // Layer Title
                val label = when (layer) {
                    is TextLayer -> layer.text.lineSequence().firstOrNull()?.ifBlank { layer.name } ?: layer.name
                    is ImageLayer -> layer.name
                    is ShapeLayer -> layer.name
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = kanvoColors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Lock Toggle
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                        contentDescription = stringResource(R.string.cd_lock_layer),
                        tint = if (layer.isLocked) kanvoColors.selectionAccent else kanvoColors.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Visibility Toggle
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(R.string.cd_visibility_layer),
                        tint = if (layer.isVisible) kanvoColors.inkSecondary else kanvoColors.selectionAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order",
                        style = MaterialTheme.typography.labelMedium,
                        color = kanvoColors.inkSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Bring forward",
                            tint = kanvoColors.selectionAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Send backward",
                            tint = kanvoColors.selectionAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerThumbnail(layer: CanvasLayer) {
    val kanvoColors = LocalKanvoColors.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(kanvoColors.panelElevated)
            .border(1.dp, kanvoColors.borderSubtle, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (layer) {
            is TextLayer -> {
                val font = KanvoCanvasFonts.get(layer.fontFamilyKey)
                Text(
                    text = "T",
                    fontFamily = font.fontFamily,
                    fontSize = 18.sp,
                    fontWeight = if (layer.isBold) FontWeight.Bold else FontWeight.Normal,
                    color = parseHexColor(layer.colorHex, kanvoColors.inkPrimary)
                )
            }
            is ImageLayer -> {
                val file = File(layer.localMediaPath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = layer.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = kanvoColors.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            is ShapeLayer -> {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = parseHexColor(layer.fillColorHex, kanvoColors.selectionAccent),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
