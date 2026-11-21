package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.R
import com.example.data.KanvoImageFilters
import com.example.model.CanvasLayer
import com.example.model.DesignPage
import com.example.model.ImageLayer
import com.example.model.KanvoShapeType
import com.example.model.KanvoTextAlign
import com.example.model.ShapeLayer
import com.example.model.SnapGuideState
import com.example.model.TextLayer
import com.example.model.parseHexColor
import com.example.ui.theme.KanvoCanvasFonts
import com.example.ui.theme.LocalKanvoColors
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun InteractiveCanvasWorkspace(
    page: DesignPage?,
    canvasWidthPx: Int,
    canvasHeightPx: Int,
    selectedLayerId: String?,
    viewportScale: Float,
    viewportOffsetX: Float,
    viewportOffsetY: Float,
    snapGuides: SnapGuideState,
    onViewportChange: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    onResetViewport: () -> Unit,
    onSelectLayer: (String?) -> Unit,
    onDoubleTapText: (String) -> Unit,
    onBeginGesture: () -> Unit,
    onUpdateLayerTransform: (
        layerId: String,
        xFraction: Float,
        yFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        rotationDegrees: Float
    ) -> Unit,
    onEndGesture: () -> Unit,
    onEditSelected: () -> Unit,
    onToggleLockSelected: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoreSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kanvoColors = LocalKanvoColors.current
    val currentScale by rememberUpdatedState(viewportScale)
    val currentOffsetX by rememberUpdatedState(viewportOffsetX)
    val currentOffsetY by rememberUpdatedState(viewportOffsetY)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(kanvoColors.studioDesk)
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                onViewportChange(
                                    currentScale * zoom,
                                    currentOffsetX + pan.x,
                                    currentOffsetY + pan.y
                                )
                                event.changes.forEach { it.consume() }
                            }
                        }
                        if (event.changes.all { !it.pressed }) break
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onSelectLayer(null) })
            }
    ) {
        if (page != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val aspect = canvasWidthPx.toFloat() / canvasHeightPx.toFloat().coerceAtLeast(1f)
                val density = LocalDensity.current
                val availW = with(density) { maxWidth.toPx() }
                val availH = with(density) { maxHeight.toPx() }

                val (artWpx, artHpx) = remember(availW, availH, aspect) {
                    val byWidthH = availW / aspect
                    if (byWidthH <= availH) {
                        availW to byWidthH
                    } else {
                        (availH * aspect) to availH
                    }
                }

                val artWDp = with(density) { artWpx.toDp() }
                val artHDp = with(density) { artHpx.toDp() }

                // Artboard + Overlays scaled & panned by viewport transform
                Box(
                    modifier = Modifier
                        .size(artWDp, artHDp)
                        .graphicsLayer {
                            scaleX = viewportScale
                            scaleY = viewportScale
                            translationX = viewportOffsetX
                            translationY = viewportOffsetY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Artboard Surface (Clipped to canvas bounds)
                    val bgStart = parseHexColor(page.backgroundColorHex, Color.White)
                    val bgEnd = page.backgroundGradientEndHex?.let { parseHexColor(it, bgStart) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(10.dp, RoundedCornerShape(2.dp))
                            .then(
                                if (bgEnd != null) {
                                    Modifier.background(Brush.linearGradient(listOf(bgStart, bgEnd)))
                                } else {
                                    Modifier.background(bgStart)
                                }
                            )
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onSelectLayer(null) })
                            }
                    ) {
                        val scaleFactor = artWpx / 380f
                        page.layers.forEach { layer ->
                            if (layer.isVisible) {
                                CanvasLayerNode(
                                    layer = layer,
                                    isSelected = layer.id == selectedLayerId,
                                    artboardWpx = artWpx,
                                    artboardHpx = artHpx,
                                    scaleFactor = scaleFactor,
                                    onSelect = { onSelectLayer(layer.id) },
                                    onDoubleTap = {
                                        if (layer is TextLayer) onDoubleTapText(layer.id)
                                    },
                                    onBeginGesture = onBeginGesture,
                                    onUpdateTransform = onUpdateLayerTransform,
                                    onEndGesture = onEndGesture
                                )
                            }
                        }

                        // Smart Center Snap Guides
                        if (snapGuides.verticalCenter || snapGuides.horizontalCenter) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (snapGuides.verticalCenter) {
                                    drawLine(
                                        color = kanvoColors.snapGuide,
                                        start = Offset(size.width / 2f, 0f),
                                        end = Offset(size.width / 2f, size.height),
                                        strokeWidth = 2f
                                    )
                                }
                                if (snapGuides.horizontalCenter) {
                                    drawLine(
                                        color = kanvoColors.snapGuide,
                                        start = Offset(0f, size.height / 2f),
                                        end = Offset(size.width, size.height / 2f),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                    }

                    // Unclipped Selection Chrome Overlay (handles + rotation knob + floating pill)
                    val selected = page.layers.firstOrNull { it.id == selectedLayerId && it.isVisible }
                    if (selected != null) {
                        SelectedLayerOverlay(
                            layer = selected,
                            artboardWpx = artWpx,
                            artboardHpx = artHpx,
                            onBeginGesture = onBeginGesture,
                            onUpdateTransform = onUpdateLayerTransform,
                            onEndGesture = onEndGesture,
                            onEdit = onEditSelected,
                            onToggleLock = onToggleLockSelected,
                            onDuplicate = onDuplicateSelected,
                            onDelete = onDeleteSelected,
                            onMore = onMoreSelected
                        )
                    }
                }
            }
        }

        // Bottom-Right Floating Zoom Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            color = kanvoColors.panelSurface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, kanvoColors.borderSubtle),
            shadowElevation = 3.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        onViewportChange(viewportScale - 0.15f, viewportOffsetX, viewportOffsetY)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = kanvoColors.inkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                TextButton(
                    onClick = onResetViewport,
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "${(viewportScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = kanvoColors.inkPrimary
                    )
                }
                IconButton(
                    onClick = {
                        onViewportChange(viewportScale + 0.15f, viewportOffsetX, viewportOffsetY)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = kanvoColors.inkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasLayerNode(
    layer: CanvasLayer,
    isSelected: Boolean,
    artboardWpx: Float,
    artboardHpx: Float,
    scaleFactor: Float,
    onSelect: () -> Unit,
    onDoubleTap: () -> Unit,
    onBeginGesture: () -> Unit,
    onUpdateTransform: (String, Float, Float, Float, Float, Float) -> Unit,
    onEndGesture: () -> Unit
) {
    val density = LocalDensity.current
    val widthDp = with(density) { (layer.widthFraction * artboardWpx).toDp() }
    val heightDp = with(density) { (layer.heightFraction * artboardHpx).toDp() }
    val offsetX = (layer.xFraction - layer.widthFraction / 2f) * artboardWpx
    val offsetY = (layer.yFraction - layer.heightFraction / 2f) * artboardHpx

    val latestLayer by rememberUpdatedState(layer)
    val latestW by rememberUpdatedState(artboardWpx)
    val latestH by rememberUpdatedState(artboardHpx)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .graphicsLayer {
                rotationZ = layer.rotationDegrees
                alpha = layer.opacity
            }
            .pointerInput(layer.id) {
                detectTapGestures(
                    onTap = { onSelect() },
                    onDoubleTap = {
                        onSelect()
                        onDoubleTap()
                    }
                )
            }
            .pointerInput(layer.id, layer.isLocked) {
                if (!layer.isLocked) {
                    detectDragGestures(
                        onDragStart = {
                            onSelect()
                            onBeginGesture()
                        },
                        onDragEnd = { onEndGesture() },
                        onDragCancel = { onEndGesture() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val cur = latestLayer
                            val rad = (cur.rotationDegrees * PI / 180.0)
                            val dx = (dragAmount.x * cos(rad) - dragAmount.y * sin(rad)).toFloat()
                            val dy = (dragAmount.x * sin(rad) + dragAmount.y * cos(rad)).toFloat()
                            onUpdateTransform(
                                cur.id,
                                cur.xFraction + dx / latestW.coerceAtLeast(1f),
                                cur.yFraction + dy / latestH.coerceAtLeast(1f),
                                cur.widthFraction,
                                cur.heightFraction,
                                cur.rotationDegrees
                            )
                        }
                    )
                }
            }
            .testTag("canvas_layer_${layer.id}"),
        contentAlignment = Alignment.Center
    ) {
        when (layer) {
            is ShapeLayer -> ShapeLayerContent(layer = layer)
            is ImageLayer -> ImageLayerContent(layer = layer)
            is TextLayer -> TextLayerContent(layer = layer, scaleFactor = scaleFactor)
        }
    }
}

@Composable
private fun ShapeLayerContent(layer: ShapeLayer) {
    val fill = parseHexColor(layer.fillColorHex, Color(0xFF7C3AED))
    val stroke = layer.strokeColorHex?.let { parseHexColor(it) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val w = size.width
        val h = size.height
        when (layer.shapeType) {
            KanvoShapeType.RECTANGLE -> path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
            KanvoShapeType.ROUNDED_RECT -> {
                val r = (layer.cornerRadiusDp * density).coerceAtMost(min(w, h) / 2f)
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, r, r)
                )
            }
            KanvoShapeType.CIRCLE -> path.addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
            KanvoShapeType.PILL -> {
                val r = min(w, h) / 2f
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, r, r)
                )
            }
            KanvoShapeType.TRIANGLE -> {
                path.moveTo(w / 2f, 0f)
                path.lineTo(w, h)
                path.lineTo(0f, h)
                path.close()
            }
            KanvoShapeType.DIAMOND -> {
                path.moveTo(w / 2f, 0f)
                path.lineTo(w, h / 2f)
                path.lineTo(w / 2f, h)
                path.lineTo(0f, h / 2f)
                path.close()
            }
            KanvoShapeType.STAR -> buildPolygonStarPath(path, w, h, 5, 0.45f)
            KanvoShapeType.BADGE -> buildPolygonStarPath(path, w, h, 12, 0.82f)
        }

        drawPath(path = path, color = fill)
        if (stroke != null && layer.strokeWidthDp > 0f) {
            drawPath(
                path = path,
                color = stroke,
                style = Stroke(width = layer.strokeWidthDp * density)
            )
        }
    }
}

private fun buildPolygonStarPath(
    path: Path,
    w: Float,
    h: Float,
    points: Int,
    innerRatio: Float
) {
    val cx = w / 2f
    val cy = h / 2f
    val rx = w / 2f
    val ry = h / 2f
    val total = points * 2
    for (i in 0 until total) {
        val angle = (i * PI / points) - PI / 2.0
        val factor = if (i % 2 == 0) 1f else innerRatio
        val px = cx + (rx * factor * cos(angle)).toFloat()
        val py = cy + (ry * factor * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
}

@Composable
private fun ImageLayerContent(layer: ImageLayer) {
    val matrix = remember(layer.filterPreset, layer.brightness, layer.contrast, layer.saturation) {
        val androidMatrix = KanvoImageFilters.buildAndroidColorMatrix(
            layer.filterPreset,
            layer.brightness,
            layer.contrast,
            layer.saturation
        )
        ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
    }
    val file = File(layer.localMediaPath)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(layer.cornerRadiusDp.dp))
    ) {
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = layer.name,
                contentScale = ContentScale.Crop,
                colorFilter = matrix,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TextLayerContent(
    layer: TextLayer,
    scaleFactor: Float
) {
    val fontOption = KanvoCanvasFonts.get(layer.fontFamilyKey)
    val textColor = parseHexColor(layer.colorHex, Color.Black)
    val pillColor = layer.pillBackgroundHex?.let { parseHexColor(it) }
    val displayText = if (layer.isUppercase) layer.text.uppercase() else layer.text

    val decoration = when {
        layer.isUnderlined && layer.isStrikethrough ->
            TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        layer.isUnderlined -> TextDecoration.Underline
        layer.isStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    val align = when (layer.textAlign) {
        KanvoTextAlign.START -> TextAlign.Start
        KanvoTextAlign.CENTER -> TextAlign.Center
        KanvoTextAlign.END -> TextAlign.End
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (pillColor != null) {
                    Modifier
                        .clip(RoundedCornerShape((12f * scaleFactor).coerceAtLeast(6f).dp))
                        .background(pillColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = TextStyle(
                fontFamily = fontOption.fontFamily,
                fontSize = (layer.fontSizeSp * scaleFactor * 0.42f).coerceIn(8f, 140f).sp,
                fontWeight = if (layer.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (layer.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = decoration,
                letterSpacing = (layer.letterSpacingEm * 12f).sp,
                lineHeight = (layer.fontSizeSp * scaleFactor * 0.42f * layer.lineHeightMultiplier).coerceIn(10f, 180f).sp,
                color = textColor,
                textAlign = align,
                shadow = if (layer.shadowEnabled) {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(2f, 3f),
                        blurRadius = 6f
                    )
                } else null
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SelectedLayerOverlay(
    layer: CanvasLayer,
    artboardWpx: Float,
    artboardHpx: Float,
    onBeginGesture: () -> Unit,
    onUpdateTransform: (String, Float, Float, Float, Float, Float) -> Unit,
    onEndGesture: () -> Unit,
    onEdit: () -> Unit,
    onToggleLock: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    val density = LocalDensity.current
    val widthPx = layer.widthFraction * artboardWpx
    val heightPx = layer.heightFraction * artboardHpx
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    val offsetX = (layer.xFraction - layer.widthFraction / 2f) * artboardWpx
    val offsetY = (layer.yFraction - layer.heightFraction / 2f) * artboardHpx

    val latestLayer by rememberUpdatedState(layer)
    val latestW by rememberUpdatedState(artboardWpx)
    val latestH by rememberUpdatedState(artboardHpx)

    // Bounding Box + Corner/Edge Resize Handles + Rotation Knob
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(widthDp, heightDp)
                .graphicsLayer { rotationZ = layer.rotationDegrees }
                .border(
                    width = 2.dp,
                    color = if (layer.isLocked) kanvoColors.inkSecondary else kanvoColors.selectionAccent
                )
        ) {
            if (!layer.isLocked) {
                // 4 Corner Circle Resize Handles
                ResizeCornerHandle(
                    alignment = Alignment.TopStart,
                    offsetX = (-7).dp,
                    offsetY = (-7).dp,
                    onBegin = onBeginGesture,
                    onEnd = onEndGesture
                ) { dx, dy ->
                    val cur = latestLayer
                    val dw = -dx / latestW
                    val dh = -dy / latestH
                    onUpdateTransform(
                        cur.id,
                        cur.xFraction + dx / (2f * latestW),
                        cur.yFraction + dy / (2f * latestH),
                        cur.widthFraction + dw,
                        cur.heightFraction + dh,
                        cur.rotationDegrees
                    )
                }

                ResizeCornerHandle(
                    alignment = Alignment.TopEnd,
                    offsetX = 7.dp,
                    offsetY = (-7).dp,
                    onBegin = onBeginGesture,
                    onEnd = onEndGesture
                ) { dx, dy ->
                    val cur = latestLayer
                    val dw = dx / latestW
                    val dh = -dy / latestH
                    onUpdateTransform(
                        cur.id,
                        cur.xFraction + dx / (2f * latestW),
                        cur.yFraction + dy / (2f * latestH),
                        cur.widthFraction + dw,
                        cur.heightFraction + dh,
                        cur.rotationDegrees
                    )
                }

                ResizeCornerHandle(
                    alignment = Alignment.BottomStart,
                    offsetX = (-7).dp,
                    offsetY = 7.dp,
                    onBegin = onBeginGesture,
                    onEnd = onEndGesture
                ) { dx, dy ->
                    val cur = latestLayer
                    val dw = -dx / latestW
                    val dh = dy / latestH
                    onUpdateTransform(
                        cur.id,
                        cur.xFraction + dx / (2f * latestW),
                        cur.yFraction + dy / (2f * latestH),
                        cur.widthFraction + dw,
                        cur.heightFraction + dh,
                        cur.rotationDegrees
                    )
                }

                ResizeCornerHandle(
                    alignment = Alignment.BottomEnd,
                    offsetX = 7.dp,
                    offsetY = 7.dp,
                    onBegin = onBeginGesture,
                    onEnd = onEndGesture
                ) { dx, dy ->
                    val cur = latestLayer
                    val dw = dx / latestW
                    val dh = dy / latestH
                    onUpdateTransform(
                        cur.id,
                        cur.xFraction + dx / (2f * latestW),
                        cur.yFraction + dy / (2f * latestH),
                        cur.widthFraction + dw,
                        cur.heightFraction + dh,
                        cur.rotationDegrees
                    )
                }

                // Side Pill Handle (Right edge) matching reference screenshot
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 4.dp)
                        .size(width = 8.dp, height = 28.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(1.5.dp, kanvoColors.selectionAccent, RoundedCornerShape(4.dp))
                        .pointerInput(layer.id) {
                            detectDragGestures(
                                onDragStart = { onBeginGesture() },
                                onDragEnd = { onEndGesture() },
                                onDragCancel = { onEndGesture() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val cur = latestLayer
                                    onUpdateTransform(
                                        cur.id,
                                        cur.xFraction + dragAmount.x / (2f * latestW),
                                        cur.yFraction,
                                        cur.widthFraction + dragAmount.x / latestW,
                                        cur.heightFraction,
                                        cur.rotationDegrees
                                    )
                                }
                            )
                        }
                )

                // Dedicated Rotate Handle (↻) to the right of selection box (matching reference screenshot!)
                var rotationAccumulator by remember(layer.id) { mutableFloatStateOf(layer.rotationDegrees) }
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 38.dp)
                        .size(32.dp)
                        .pointerInput(layer.id) {
                            detectDragGestures(
                                onDragStart = {
                                    rotationAccumulator = latestLayer.rotationDegrees
                                    onBeginGesture()
                                },
                                onDragEnd = { onEndGesture() },
                                onDragCancel = { onEndGesture() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    rotationAccumulator += (dragAmount.y * 0.65f) - (dragAmount.x * 0.25f)
                                    val cur = latestLayer
                                    onUpdateTransform(
                                        cur.id,
                                        cur.xFraction,
                                        cur.yFraction,
                                        cur.widthFraction,
                                        cur.heightFraction,
                                        rotationAccumulator
                                    )
                                }
                            )
                        }
                        .testTag("rotate_handle"),
                    shape = CircleShape,
                    color = kanvoColors.panelSurface,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, kanvoColors.borderSubtle)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_rotate_handle),
                            tint = kanvoColors.inkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Floating Contextual Action Pill (matching reference floating white pill!)
        val showBelow = (layer.yFraction - layer.heightFraction / 2f) < 0.18f
        val pillYOffset = if (showBelow) {
            (offsetY + heightPx + with(density) { 14.dp.toPx() }).roundToInt()
        } else {
            (offsetY - with(density) { 54.dp.toPx() }).roundToInt()
        }
        val pillCenterX = (layer.xFraction * artboardWpx - with(density) { 112.dp.toPx() })
            .coerceIn(0f, (artboardWpx - with(density) { 224.dp.toPx() }).coerceAtLeast(0f))
            .roundToInt()

        Surface(
            modifier = Modifier
                .offset { IntOffset(pillCenterX, pillYOffset) }
                .testTag("floating_action_pill"),
            shape = RoundedCornerShape(24.dp),
            color = kanvoColors.panelSurface,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, kanvoColors.borderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (layer is TextLayer || layer is ImageLayer) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("pill_edit_button")
                    ) {
                        Icon(
                            imageVector = if (layer is TextLayer) Icons.Default.Edit else Icons.Default.Tune,
                            contentDescription = if (layer is TextLayer) "Edit text" else "Adjust photo",
                            tint = kanvoColors.inkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("pill_lock_button")
                ) {
                    Icon(
                        imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Outlined.LockOpen,
                        contentDescription = stringResource(R.string.cd_lock_layer),
                        tint = if (layer.isLocked) kanvoColors.selectionAccent else kanvoColors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("pill_duplicate_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.cd_duplicate_layer),
                        tint = kanvoColors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("pill_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_layer),
                        tint = kanvoColors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMore,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("pill_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = kanvoColors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ResizeCornerHandle(
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    onBegin: () -> Unit,
    onEnd: () -> Unit,
    onDelta: (dx: Float, dy: Float) -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(16.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, kanvoColors.selectionAccent, CircleShape)
            .pointerInput(alignment) {
                detectDragGestures(
                    onDragStart = { onBegin() },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDelta(dragAmount.x, dragAmount.y)
                    }
                )
            }
    )
}
