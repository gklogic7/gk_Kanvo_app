package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CanvasExporter
import com.example.data.DesignRepository
import com.example.data.KanvoDatabase
import com.example.data.MediaHelper
import com.example.data.ThemePreferencesRepository
import com.example.model.AspectRatioPreset
import com.example.model.CanvasLayer
import com.example.model.CopiedStyle
import com.example.model.DesignPage
import com.example.model.DesignProject
import com.example.model.ExportFormat
import com.example.model.ImageLayer
import com.example.model.KanvoShapeType
import com.example.model.KanvoTextAlign
import com.example.model.PhotoFilterPreset
import com.example.model.ShapeLayer
import com.example.model.SnapGuideState
import com.example.model.TextLayer
import com.example.model.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

enum class ActiveInspectorSheet {
    NONE,
    TEXT_EDIT,
    PHOTO_ADJUST,
    SHAPES_ADD,
    BACKGROUND,
    POSITION_ALIGN,
    TEXT_EFFECTS,
    TEXT_SPACING,
    OPACITY,
    LAYERS_SHEET,
    EXPORT_DIALOG
}

data class KanvoUiState(
    val activeProject: DesignProject? = null,
    val activePageIndex: Int = 0,
    val selectedLayerId: String? = null,
    val layerSearchQuery: String = "",
    val isLayersSidebarOpen: Boolean = true,
    val copiedStyle: CopiedStyle? = null,
    val viewportScale: Float = 1f,
    val viewportOffsetX: Float = 0f,
    val viewportOffsetY: Float = 0f,
    val snapGuides: SnapGuideState = SnapGuideState(),
    val activeSheet: ActiveInspectorSheet = ActiveInspectorSheet.NONE,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val snackbarMessage: String? = null,
    val pendingShareIntent: Intent? = null
) {
    val activePage: DesignPage?
        get() = activeProject?.pages?.getOrNull(activePageIndex)

    val selectedLayer: CanvasLayer?
        get() = activePage?.layers?.firstOrNull { it.id == selectedLayerId }
}

class KanvoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KanvoDatabase.getInstance(application)
    private val repository = DesignRepository(database.designProjectDao())
    private val themePrefs = ThemePreferencesRepository(application)

    val savedProjects: StateFlow<List<DesignProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = themePrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.LIGHT)

    private val _uiState = MutableStateFlow(KanvoUiState())
    val uiState: StateFlow<KanvoUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<DesignProject>()
    private val redoStack = ArrayDeque<DesignProject>()
    private var autoSaveJob: Job? = null

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePrefs.setThemeMode(mode)
        }
    }

    fun cycleThemeMode() {
        val next = when (themeMode.value) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    fun createBlankProject(
        preset: AspectRatioPreset? = null,
        customWidth: Int? = null,
        customHeight: Int? = null,
        title: String? = null
    ) {
        val w = (customWidth ?: preset?.widthPx ?: 1080).coerceIn(320, 4096)
        val h = (customHeight ?: preset?.heightPx ?: 1080).coerceIn(320, 4096)
        val ratioLabel = preset?.label ?: "${w}×${h} Custom"
        val projectTitle = title?.takeIf { it.isNotBlank() } ?: "Design • $ratioLabel"

        val newProject = DesignProject(
            id = UUID.randomUUID().toString(),
            title = projectTitle,
            aspectRatioLabel = ratioLabel,
            canvasWidth = w,
            canvasHeight = h,
            pages = listOf(DesignPage(name = "Page 1", backgroundColorHex = "#FFFFFF")),
            updatedAt = System.currentTimeMillis()
        )
        openProjectIntoEditor(newProject)
        persistProjectNow(newProject)
    }

    fun openExistingProject(project: DesignProject) {
        openProjectIntoEditor(project)
    }

    private fun openProjectIntoEditor(project: DesignProject) {
        undoStack.clear()
        redoStack.clear()
        _uiState.value = KanvoUiState(
            activeProject = project,
            activePageIndex = 0,
            selectedLayerId = null,
            isLayersSidebarOpen = true,
            canUndo = false,
            canRedo = false
        )
    }

    fun closeEditorToHub() {
        val current = _uiState.value.activeProject
        if (current != null) {
            persistProjectNow(current)
        }
        undoStack.clear()
        redoStack.clear()
        _uiState.value = KanvoUiState()
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun duplicateProject(project: DesignProject) {
        val copy = project.copy(
            id = UUID.randomUUID().toString(),
            title = "${project.title} (Copy)",
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val thumb = CanvasExporter.saveProjectThumbnail(getApplication(), copy)
            repository.saveProject(copy.copy(thumbnailPath = thumb))
        }
    }

    fun renameProject(newTitle: String) {
        val clean = newTitle.trim().ifBlank { "Untitled Design" }
        mutateProject(recordHistory = true) { project ->
            project.copy(title = clean)
        }
    }

    // --- Navigation & Panels ---

    fun toggleLayersSidebar() {
        _uiState.update { it.copy(isLayersSidebarOpen = !it.isLayersSidebarOpen) }
    }

    fun setLayerSearchQuery(query: String) {
        _uiState.update { it.copy(layerSearchQuery = query) }
    }

    fun openSheet(sheet: ActiveInspectorSheet) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = ActiveInspectorSheet.NONE) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearPendingShareIntent() {
        _uiState.update { it.copy(pendingShareIntent = null) }
    }

    // --- Viewport Zoom/Pan ---

    fun updateViewport(scale: Float, offsetX: Float, offsetY: Float) {
        _uiState.update {
            it.copy(
                viewportScale = scale.coerceIn(0.45f, 3.0f),
                viewportOffsetX = offsetX.coerceIn(-1200f, 1200f),
                viewportOffsetY = offsetY.coerceIn(-1200f, 1200f)
            )
        }
    }

    fun resetViewport() {
        _uiState.update {
            it.copy(viewportScale = 1f, viewportOffsetX = 0f, viewportOffsetY = 0f)
        }
    }

    // --- Undo / Redo ---

    fun undo() {
        val current = _uiState.value.activeProject ?: return
        if (undoStack.isEmpty()) return
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        val safePageIdx = _uiState.value.activePageIndex.coerceIn(0, (previous.pages.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(
                activeProject = previous,
                activePageIndex = safePageIdx,
                selectedLayerId = previous.pages.getOrNull(safePageIdx)?.layers
                    ?.firstOrNull { l -> l.id == it.selectedLayerId }?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutoSave(previous)
    }

    fun redo() {
        val current = _uiState.value.activeProject ?: return
        if (redoStack.isEmpty()) return
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        val safePageIdx = _uiState.value.activePageIndex.coerceIn(0, (next.pages.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(
                activeProject = next,
                activePageIndex = safePageIdx,
                selectedLayerId = next.pages.getOrNull(safePageIdx)?.layers
                    ?.firstOrNull { l -> l.id == it.selectedLayerId }?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutoSave(next)
    }

    // --- Layer Selection & Creation ---

    fun selectLayer(layerId: String?) {
        val state = _uiState.value
        val copied = state.copiedStyle
        if (layerId != null && copied != null && layerId != state.selectedLayerId) {
            applyCopiedStyleToLayer(layerId, copied)
            return
        }
        _uiState.update { it.copy(selectedLayerId = layerId) }
    }

    fun addTextLayer(
        initialText: String = "Add a heading",
        fontSizeSp: Float = 36f,
        fontFamilyKey: String = "montserrat",
        isBold: Boolean = true
    ) {
        val newLayer = TextLayer(
            name = initialText.take(24),
            text = initialText,
            fontSizeSp = fontSizeSp,
            fontFamilyKey = fontFamilyKey,
            isBold = isBold
        )
        appendLayerToActivePage(newLayer, openTextEditor = true)
    }

    fun addShapeLayer(shapeType: KanvoShapeType, fillColorHex: String = "#7C3AED") {
        val newLayer = ShapeLayer(
            name = shapeType.displayName,
            shapeType = shapeType,
            fillColorHex = fillColorHex,
            widthFraction = if (shapeType == KanvoShapeType.PILL) 0.44f else 0.30f,
            heightFraction = if (shapeType == KanvoShapeType.PILL) 0.14f else 0.30f,
            cornerRadiusDp = when (shapeType) {
                KanvoShapeType.ROUNDED_RECT -> 20f
                KanvoShapeType.PILL -> 50f
                else -> 0f
            }
        )
        appendLayerToActivePage(newLayer)
        closeSheet()
    }

    fun importPhotoFromPicker(uri: Uri) {
        viewModelScope.launch {
            val result = MediaHelper.importImageToAppStorage(getApplication(), uri)
            if (result == null) {
                _uiState.update { it.copy(snackbarMessage = "Could not load selected image.") }
                return@launch
            }
            val project = _uiState.value.activeProject ?: return@launch
            val canvasAspect = project.canvasWidth.toFloat() / project.canvasHeight.toFloat().coerceAtLeast(1f)
            val widthFrac = 0.56f
            val heightFrac = ((widthFrac * canvasAspect) / result.aspectRatio).coerceIn(0.18f, 0.82f)
            val newLayer = ImageLayer(
                name = "Photo",
                localMediaPath = result.localPath,
                widthFraction = widthFrac,
                heightFraction = heightFrac
            )
            appendLayerToActivePage(newLayer)
        }
    }

    private fun appendLayerToActivePage(layer: CanvasLayer, openTextEditor: Boolean = false) {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(layers = page.layers + layer)
        }
        _uiState.update {
            it.copy(
                selectedLayerId = layer.id,
                activeSheet = if (openTextEditor) ActiveInspectorSheet.TEXT_EDIT else it.activeSheet
            )
        }
    }

    // --- Direct Manipulation (Gestures, Drag, Resize, Rotate) ---

    fun beginGestureTransform() {
        val current = _uiState.value.activeProject ?: return
        pushUndoSnapshot(current)
    }

    fun updateLayerGestureTransform(
        layerId: String,
        xFraction: Float,
        yFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        rotationDegrees: Float,
        enableSnap: Boolean = true
    ) {
        var snappedX = xFraction.coerceIn(0.05f, 0.95f)
        var snappedY = yFraction.coerceIn(0.05f, 0.95f)
        var snapV = false
        var snapH = false

        if (enableSnap) {
            if (abs(snappedX - 0.5f) < 0.018f) {
                snappedX = 0.5f
                snapV = true
            }
            if (abs(snappedY - 0.5f) < 0.018f) {
                snappedY = 0.5f
                snapH = true
            }
        }

        _uiState.update { it.copy(snapGuides = SnapGuideState(verticalCenter = snapV, horizontalCenter = snapH)) }

        mutateActivePage(recordHistory = false) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == layerId && !layer.isLocked) {
                        layer.withTransform(
                            xFraction = snappedX,
                            yFraction = snappedY,
                            widthFraction = widthFraction.coerceIn(0.10f, 1.4f),
                            heightFraction = heightFraction.coerceIn(0.06f, 1.4f),
                            rotationDegrees = ((rotationDegrees % 360f) + 360f) % 360f
                        )
                    } else layer
                }
            )
        }
    }

    fun endGestureTransform() {
        _uiState.update { it.copy(snapGuides = SnapGuideState()) }
    }

    // --- Layer Actions ---

    fun toggleLayerLock(layerId: String) {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == layerId) layer.withLock(!layer.isLocked) else layer
                }
            )
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == layerId) layer.withVisibility(!layer.isVisible) else layer
                }
            )
        }
    }

    fun duplicateSelectedLayer() {
        val selected = _uiState.value.selectedLayer ?: return
        val copy = selected.duplicate()
        mutateActivePage(recordHistory = true) { page ->
            page.copy(layers = page.layers + copy)
        }
        _uiState.update { it.copy(selectedLayerId = copy.id) }
    }

    fun deleteLayer(layerId: String) {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(layers = page.layers.filterNot { it.id == layerId })
        }
        if (_uiState.value.selectedLayerId == layerId) {
            _uiState.update { it.copy(selectedLayerId = null, activeSheet = ActiveInspectorSheet.NONE) }
        }
    }

    fun moveLayerOrder(layerId: String, moveUp: Boolean) {
        mutateActivePage(recordHistory = true) { page ->
            val list = page.layers.toMutableList()
            val index = list.indexOfFirst { it.id == layerId }
            if (index == -1) return@mutateActivePage page
            val target = if (moveUp) index + 1 else index - 1
            if (target in list.indices) {
                val item = list.removeAt(index)
                list.add(target, item)
            }
            page.copy(layers = list)
        }
    }

    fun bringToFront(layerId: String) {
        mutateActivePage(recordHistory = true) { page ->
            val target = page.layers.firstOrNull { it.id == layerId } ?: return@mutateActivePage page
            page.copy(layers = page.layers.filterNot { it.id == layerId } + target)
        }
    }

    fun sendToBack(layerId: String) {
        mutateActivePage(recordHistory = true) { page ->
            val target = page.layers.firstOrNull { it.id == layerId } ?: return@mutateActivePage page
            page.copy(layers = listOf(target) + page.layers.filterNot { it.id == layerId })
        }
    }

    fun alignSelectedLayer(horizontal: Float? = null, vertical: Float? = null) {
        val selected = _uiState.value.selectedLayer ?: return
        if (selected.isLocked) return
        mutateActivePage(recordHistory = true) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == selected.id) {
                        layer.withTransform(
                            xFraction = horizontal ?: layer.xFraction,
                            yFraction = vertical ?: layer.yFraction
                        )
                    } else layer
                }
            )
        }
    }

    fun updateSelectedOpacity(opacity: Float) {
        val selected = _uiState.value.selectedLayer ?: return
        mutateActivePage(recordHistory = false) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == selected.id) layer.withOpacity(opacity.coerceIn(0.05f, 1f)) else layer
                }
            )
        }
    }

    // --- Copy & Paste Style (Paint Roller) ---

    fun toggleCopyStyleMode() {
        val currentCopied = _uiState.value.copiedStyle
        if (currentCopied != null) {
            _uiState.update { it.copy(copiedStyle = null, snackbarMessage = "Style painter cancelled") }
            return
        }
        val selected = _uiState.value.selectedLayer ?: return
        val style = when (selected) {
            is TextLayer -> CopiedStyle(
                fontFamilyKey = selected.fontFamilyKey,
                fontSizeSp = selected.fontSizeSp,
                colorHex = selected.colorHex,
                isBold = selected.isBold,
                isItalic = selected.isItalic,
                isUnderlined = selected.isUnderlined,
                isStrikethrough = selected.isStrikethrough,
                isUppercase = selected.isUppercase,
                textAlign = selected.textAlign,
                letterSpacingEm = selected.letterSpacingEm,
                shadowEnabled = selected.shadowEnabled,
                pillBackgroundHex = selected.pillBackgroundHex,
                opacity = selected.opacity
            )
            is ShapeLayer -> CopiedStyle(
                shapeFillHex = selected.fillColorHex,
                cornerRadiusDp = selected.cornerRadiusDp,
                opacity = selected.opacity
            )
            is ImageLayer -> CopiedStyle(
                cornerRadiusDp = selected.cornerRadiusDp,
                opacity = selected.opacity
            )
        }
        _uiState.update {
            it.copy(
                copiedStyle = style,
                snackbarMessage = "Style copied — tap another element to apply"
            )
        }
    }

    private fun applyCopiedStyleToLayer(targetLayerId: String, style: CopiedStyle) {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id != targetLayerId || layer.isLocked) return@map layer
                    when (layer) {
                        is TextLayer -> layer.copy(
                            fontFamilyKey = style.fontFamilyKey ?: layer.fontFamilyKey,
                            fontSizeSp = style.fontSizeSp ?: layer.fontSizeSp,
                            colorHex = style.colorHex ?: layer.colorHex,
                            isBold = style.isBold ?: layer.isBold,
                            isItalic = style.isItalic ?: layer.isItalic,
                            isUnderlined = style.isUnderlined ?: layer.isUnderlined,
                            isStrikethrough = style.isStrikethrough ?: layer.isStrikethrough,
                            isUppercase = style.isUppercase ?: layer.isUppercase,
                            textAlign = style.textAlign ?: layer.textAlign,
                            letterSpacingEm = style.letterSpacingEm ?: layer.letterSpacingEm,
                            shadowEnabled = style.shadowEnabled ?: layer.shadowEnabled,
                            pillBackgroundHex = style.pillBackgroundHex,
                            opacity = style.opacity
                        )
                        is ShapeLayer -> layer.copy(
                            fillColorHex = style.shapeFillHex ?: layer.fillColorHex,
                            cornerRadiusDp = style.cornerRadiusDp ?: layer.cornerRadiusDp,
                            opacity = style.opacity
                        )
                        is ImageLayer -> layer.copy(
                            cornerRadiusDp = style.cornerRadiusDp ?: layer.cornerRadiusDp,
                            opacity = style.opacity
                        )
                    }
                }
            )
        }
        _uiState.update {
            it.copy(
                selectedLayerId = targetLayerId,
                copiedStyle = null,
                snackbarMessage = "Style applied"
            )
        }
    }

    // --- Text Layer Mutations ---

    fun updateSelectedText(transform: (TextLayer) -> TextLayer) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        mutateActivePage(recordHistory = true) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == selectedId && layer is TextLayer && !layer.isLocked) {
                        val updated = transform(layer)
                        updated.copy(name = updated.text.lineSequence().firstOrNull()?.take(26).orEmpty().ifBlank { "Text" })
                    } else layer
                }
            )
        }
    }

    fun cycleTextAlignment() {
        updateSelectedText { text ->
            val next = when (text.textAlign) {
                KanvoTextAlign.START -> KanvoTextAlign.CENTER
                KanvoTextAlign.CENTER -> KanvoTextAlign.END
                KanvoTextAlign.END -> KanvoTextAlign.START
            }
            text.copy(textAlign = next)
        }
    }

    // --- Image Layer Mutations ---

    fun updateSelectedImage(recordHistory: Boolean = true, transform: (ImageLayer) -> ImageLayer) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        mutateActivePage(recordHistory = recordHistory) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == selectedId && layer is ImageLayer && !layer.isLocked) {
                        transform(layer)
                    } else layer
                }
            )
        }
    }

    fun applyPhotoFilter(preset: PhotoFilterPreset) {
        updateSelectedImage(recordHistory = true) { it.copy(filterPreset = preset) }
    }

    // --- Shape Layer Mutations ---

    fun updateSelectedShape(recordHistory: Boolean = true, transform: (ShapeLayer) -> ShapeLayer) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        mutateActivePage(recordHistory = recordHistory) { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    if (layer.id == selectedId && layer is ShapeLayer && !layer.isLocked) {
                        transform(layer)
                    } else layer
                }
            )
        }
    }

    // --- Page & Background Management ---

    fun setPageBackground(colorHex: String, gradientEndHex: String? = null) {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(
                backgroundColorHex = colorHex,
                backgroundGradientEndHex = gradientEndHex
            )
        }
    }

    fun toggleBackgroundLock() {
        mutateActivePage(recordHistory = true) { page ->
            page.copy(isBackgroundLocked = !page.isBackgroundLocked)
        }
    }

    fun addPage() {
        mutateProject(recordHistory = true) { project ->
            val nextIndex = project.pages.size + 1
            val newPage = DesignPage(
                name = "Page $nextIndex",
                backgroundColorHex = "#FFFFFF"
            )
            project.copy(pages = project.pages + newPage)
        }
        val lastIdx = (_uiState.value.activeProject?.pages?.size ?: 1) - 1
        _uiState.update { it.copy(activePageIndex = lastIdx, selectedLayerId = null) }
    }

    fun duplicateActivePage() {
        val idx = _uiState.value.activePageIndex
        mutateProject(recordHistory = true) { project ->
            val current = project.pages.getOrNull(idx) ?: return@mutateProject project
            val copiedPage = current.copy(
                id = UUID.randomUUID().toString(),
                name = "${current.name} (Copy)",
                layers = current.layers.map { it.duplicate().let { dup ->
                    when (dup) {
                        is TextLayer -> dup.copy(xFraction = it.xFraction, yFraction = it.yFraction, name = it.name)
                        is ImageLayer -> dup.copy(xFraction = it.xFraction, yFraction = it.yFraction, name = it.name)
                        is ShapeLayer -> dup.copy(xFraction = it.xFraction, yFraction = it.yFraction, name = it.name)
                    }
                } }
            )
            val updated = project.pages.toMutableList().apply { add(idx + 1, copiedPage) }
            project.copy(pages = updated)
        }
        _uiState.update { it.copy(activePageIndex = idx + 1, selectedLayerId = null) }
    }

    fun deleteActivePage() {
        val project = _uiState.value.activeProject ?: return
        if (project.pages.size <= 1) {
            _uiState.update { it.copy(snackbarMessage = "A design must keep at least 1 page.") }
            return
        }
        val idx = _uiState.value.activePageIndex
        mutateProject(recordHistory = true) { p ->
            val remaining = p.pages.filterIndexed { i, _ -> i != idx }
            p.copy(pages = remaining)
        }
        val nextIdx = idx.coerceAtMost((_uiState.value.activeProject?.pages?.size ?: 1) - 1)
        _uiState.update { it.copy(activePageIndex = nextIdx, selectedLayerId = null) }
    }

    fun selectPage(index: Int) {
        val count = _uiState.value.activeProject?.pages?.size ?: return
        if (index in 0 until count) {
            _uiState.update { it.copy(activePageIndex = index, selectedLayerId = null) }
        }
    }

    // --- Export & Share ---

    fun exportToGallery(format: ExportFormat, exportAllPages: Boolean) {
        val project = _uiState.value.activeProject ?: return
        val activeIdx = _uiState.value.activePageIndex
        val indices = if (exportAllPages) project.pages.indices.toList() else listOf(activeIdx)

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val uris = CanvasExporter.exportPagesToGallery(getApplication(), project, indices, format)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    activeSheet = ActiveInspectorSheet.NONE,
                    snackbarMessage = if (uris.isNotEmpty()) {
                        "Saved ${uris.size} ${format.extension.uppercase()} image${if (uris.size > 1) "s" else ""} to Pictures/Kanvo"
                    } else {
                        "Export failed — please try again"
                    }
                )
            }
        }
    }

    fun shareCurrentPage(format: ExportFormat) {
        val project = _uiState.value.activeProject ?: return
        val activeIdx = _uiState.value.activePageIndex
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val intent = CanvasExporter.createShareIntent(getApplication(), project, activeIdx, format)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    activeSheet = ActiveInspectorSheet.NONE,
                    pendingShareIntent = intent
                )
            }
        }
    }

    // --- Internal Mutation & Persistence ---

    private fun mutateActivePage(recordHistory: Boolean, transform: (DesignPage) -> DesignPage) {
        val idx = _uiState.value.activePageIndex
        mutateProject(recordHistory = recordHistory) { project ->
            val pages = project.pages.mapIndexed { i, page ->
                if (i == idx) transform(page) else page
            }
            project.copy(pages = pages)
        }
    }

    private fun mutateProject(recordHistory: Boolean, transform: (DesignProject) -> DesignProject) {
        val current = _uiState.value.activeProject ?: return
        if (recordHistory) {
            pushUndoSnapshot(current)
        }
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        _uiState.update {
            it.copy(
                activeProject = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutoSave(updated)
    }

    private fun pushUndoSnapshot(snapshot: DesignProject) {
        if (undoStack.lastOrNull() == snapshot) return
        undoStack.addLast(snapshot)
        if (undoStack.size > 40) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _uiState.update {
            it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false)
        }
    }

    private fun scheduleAutoSave(project: DesignProject) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(450)
            persistProjectNow(project)
        }
    }

    private fun persistProjectNow(project: DesignProject) {
        viewModelScope.launch {
            val thumb = CanvasExporter.saveProjectThumbnail(getApplication(), project)
            val withThumb = if (thumb != null) project.copy(thumbnailPath = thumb) else project
            repository.saveProject(withThumb)
            _uiState.update { state ->
                if (state.activeProject?.id == withThumb.id) {
                    state.copy(activeProject = state.activeProject.copy(thumbnailPath = withThumb.thumbnailPath))
                } else state
            }
        }
    }
}
