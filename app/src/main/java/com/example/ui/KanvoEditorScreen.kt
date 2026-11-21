package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ThemeMode
import com.example.ui.theme.LocalKanvoColors
import com.example.viewmodel.ActiveInspectorSheet
import com.example.viewmodel.KanvoUiState
import com.example.viewmodel.KanvoViewModel

@Composable
fun KanvoEditorScreen(
    uiState: KanvoUiState,
    themeMode: ThemeMode,
    viewModel: KanvoViewModel
) {
    val project = uiState.activeProject ?: return
    val activePage = uiState.activePage
    val kanvoColors = LocalKanvoColors.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        viewModel.closeEditorToHub()
    }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importPhotoFromPicker(uri)
            }
        }
    )

    val launchPhotoPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.pendingShareIntent) {
        val shareIntent = uiState.pendingShareIntent
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Share Kanvo design"))
            viewModel.clearPendingShareIntent()
        }
    }

    Scaffold(
        containerColor = kanvoColors.studioDesk,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideLayout = maxWidth >= 600.dp
            val sidebarWidth = if (maxWidth >= 960.dp) 310.dp else 264.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Studio Header
                EditorTopStudioBar(
                    projectTitle = project.title,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    themeMode = themeMode,
                    isLayersOpen = uiState.isLayersSidebarOpen,
                    isWideLayout = isWideLayout,
                    onBackHome = viewModel::closeEditorToHub,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onToggleLayers = viewModel::toggleLayersSidebar,
                    onAddText = { viewModel.addTextLayer() },
                    onAddPhoto = launchPhotoPicker,
                    onOpenShapes = { viewModel.openSheet(ActiveInspectorSheet.SHAPES_ADD) },
                    onOpenBackground = { viewModel.openSheet(ActiveInspectorSheet.BACKGROUND) },
                    onCycleTheme = viewModel::cycleThemeMode,
                    onOpenExport = { viewModel.openSheet(ActiveInspectorSheet.EXPORT_DIALOG) }
                )

                // Contextual Formatting Toolbar (matches reference top toolbar)
                ContextualFormattingBar(
                    selectedLayer = uiState.selectedLayer,
                    isStyleCopyActive = uiState.copiedStyle != null,
                    onOpenSheet = viewModel::openSheet,
                    onUpdateText = viewModel::updateSelectedText,
                    onCycleTextAlign = viewModel::cycleTextAlignment,
                    onToggleCopyStyle = viewModel::toggleCopyStyleMode
                )

                // Main Studio Workspace (Sidebar + Artboard)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isWideLayout) {
                        AnimatedVisibility(
                            visible = uiState.isLayersSidebarOpen,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally()
                        ) {
                            Row {
                                LayersPanelContent(
                                    page = activePage,
                                    selectedLayerId = uiState.selectedLayerId,
                                    searchQuery = uiState.layerSearchQuery,
                                    onSearchQueryChange = viewModel::setLayerSearchQuery,
                                    onSelectLayer = viewModel::selectLayer,
                                    onToggleLock = viewModel::toggleLayerLock,
                                    onToggleVisibility = viewModel::toggleLayerVisibility,
                                    onMoveLayerOrder = viewModel::moveLayerOrder,
                                    onToggleBackgroundLock = viewModel::toggleBackgroundLock,
                                    onOpenBackgroundSheet = {
                                        viewModel.openSheet(ActiveInspectorSheet.BACKGROUND)
                                    },
                                    onCollapse = viewModel::toggleLayersSidebar,
                                    modifier = Modifier
                                        .width(sidebarWidth)
                                        .fillMaxHeight()
                                )
                                VerticalDivider(color = kanvoColors.borderSubtle)
                            }
                        }
                    }

                    // Center Canvas & Bottom Page Strip
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        InteractiveCanvasWorkspace(
                            page = activePage,
                            canvasWidthPx = project.canvasWidth,
                            canvasHeightPx = project.canvasHeight,
                            selectedLayerId = uiState.selectedLayerId,
                            viewportScale = uiState.viewportScale,
                            viewportOffsetX = uiState.viewportOffsetX,
                            viewportOffsetY = uiState.viewportOffsetY,
                            snapGuides = uiState.snapGuides,
                            onViewportChange = viewModel::updateViewport,
                            onResetViewport = viewModel::resetViewport,
                            onSelectLayer = viewModel::selectLayer,
                            onDoubleTapText = { layerId ->
                                viewModel.selectLayer(layerId)
                                viewModel.openSheet(ActiveInspectorSheet.TEXT_EDIT)
                            },
                            onBeginGesture = viewModel::beginGestureTransform,
                            onUpdateLayerTransform = viewModel::updateLayerGestureTransform,
                            onEndGesture = viewModel::endGestureTransform,
                            onEditSelected = {
                                when (uiState.selectedLayer) {
                                    is com.example.model.TextLayer -> viewModel.openSheet(ActiveInspectorSheet.TEXT_EDIT)
                                    is com.example.model.ImageLayer -> viewModel.openSheet(ActiveInspectorSheet.PHOTO_ADJUST)
                                    else -> Unit
                                }
                            },
                            onToggleLockSelected = {
                                uiState.selectedLayerId?.let(viewModel::toggleLayerLock)
                            },
                            onDuplicateSelected = viewModel::duplicateSelectedLayer,
                            onDeleteSelected = {
                                uiState.selectedLayerId?.let(viewModel::deleteLayer)
                            },
                            onMoreSelected = {
                                viewModel.openSheet(ActiveInspectorSheet.POSITION_ALIGN)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        // Bottom Page Management Strip ("+ Add page" + Page dropdown, matching reference!)
                        PageManagementBar(
                            pageCount = project.pages.size,
                            activePageIndex = uiState.activePageIndex,
                            activePageName = activePage?.name ?: "Page 1",
                            onAddPage = viewModel::addPage,
                            onSelectPage = viewModel::selectPage,
                            onDuplicatePage = viewModel::duplicateActivePage,
                            onDeletePage = viewModel::deleteActivePage,
                            modifier = if (isWideLayout) Modifier.navigationBarsPadding() else Modifier
                        )
                    }
                }

                // Compact Phone Bottom Tool Dock
                if (!isWideLayout) {
                    CompactMobileToolDock(
                        onAddText = { viewModel.addTextLayer() },
                        onAddPhoto = launchPhotoPicker,
                        onOpenShapes = { viewModel.openSheet(ActiveInspectorSheet.SHAPES_ADD) },
                        onOpenLayers = { viewModel.openSheet(ActiveInspectorSheet.LAYERS_SHEET) },
                        onOpenBackground = { viewModel.openSheet(ActiveInspectorSheet.BACKGROUND) }
                    )
                }
            }
        }
    }

    // Inspector Bottom Sheets & Export Dialog
    KanvoInspectorSheetsHost(
        activeSheet = uiState.activeSheet,
        page = activePage,
        selectedLayer = uiState.selectedLayer,
        pageCount = project.pages.size,
        searchQuery = uiState.layerSearchQuery,
        isExporting = uiState.isExporting,
        onDismiss = viewModel::closeSheet,
        onSearchQueryChange = viewModel::setLayerSearchQuery,
        onSelectLayer = viewModel::selectLayer,
        onToggleLock = viewModel::toggleLayerLock,
        onToggleVisibility = viewModel::toggleLayerVisibility,
        onMoveLayerOrder = viewModel::moveLayerOrder,
        onBringToFront = viewModel::bringToFront,
        onSendToBack = viewModel::sendToBack,
        onAlignSelected = viewModel::alignSelectedLayer,
        onToggleBackgroundLock = viewModel::toggleBackgroundLock,
        onUpdateText = viewModel::updateSelectedText,
        onUpdateImage = viewModel::updateSelectedImage,
        onApplyFilter = viewModel::applyPhotoFilter,
        onAddShape = { shapeType -> viewModel.addShapeLayer(shapeType) },
        onUpdateShape = viewModel::updateSelectedShape,
        onSetBackground = viewModel::setPageBackground,
        onUpdateOpacity = viewModel::updateSelectedOpacity,
        onExportToGallery = viewModel::exportToGallery,
        onShareCurrent = viewModel::shareCurrentPage
    )
}

@Composable
private fun PageManagementBar(
    pageCount: Int,
    activePageIndex: Int,
    activePageName: String,
    onAddPage: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onDuplicatePage: () -> Unit,
    onDeletePage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kanvoColors = LocalKanvoColors.current
    var dropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(10.dp),
            color = kanvoColors.panelSurface,
            border = BorderStroke(1.dp, kanvoColors.borderSubtle),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // + Add page primary bar button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(onClick = onAddPage)
                        .testTag("add_page_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = kanvoColors.inkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_page),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = kanvoColors.inkPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "($activePageName • ${activePageIndex + 1}/$pageCount)",
                        style = MaterialTheme.typography.labelMedium,
                        color = kanvoColors.inkSecondary
                    )
                }

                VerticalDivider(
                    color = kanvoColors.borderSubtle,
                    modifier = Modifier.height(28.dp)
                )

                // Page switcher / actions dropdown
                Box {
                    IconButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("page_switcher_dropdown")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Manage pages",
                            tint = kanvoColors.inkPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        for (i in 0 until pageCount) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (i == activePageIndex) "✓ Page ${i + 1} (Active)" else "Page ${i + 1}",
                                        fontWeight = if (i == activePageIndex) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    onSelectPage(i)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Duplicate current page") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                dropdownExpanded = false
                                onDuplicatePage()
                            }
                        )
                        if (pageCount > 1) {
                            DropdownMenuItem(
                                text = { Text("Delete current page") },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    dropdownExpanded = false
                                    onDeletePage()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMobileToolDock(
    onAddText: () -> Unit,
    onAddPhoto: () -> Unit,
    onOpenShapes: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenBackground: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = kanvoColors.panelSurface,
        shadowElevation = 6.dp
    ) {
        Column {
            HorizontalDivider(color = kanvoColors.borderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockItem(Icons.Default.Title, "Text", onAddText, "dock_text")
                DockItem(Icons.Default.AddPhotoAlternate, "Photos", onAddPhoto, "dock_photos")
                DockItem(Icons.Default.Category, "Shapes", onOpenShapes, "dock_shapes")
                DockItem(Icons.Default.Layers, "Layers", onOpenLayers, "dock_layers")
                DockItem(Icons.Default.Palette, "Backdrop", onOpenBackground, "dock_background")
            }
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val kanvoColors = LocalKanvoColors.current
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = kanvoColors.inkPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = kanvoColors.inkSecondary
        )
    }
}
