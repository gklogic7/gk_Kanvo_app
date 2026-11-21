package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AspectRatioPreset
import com.example.model.DesignProject
import com.example.model.ThemeMode
import com.example.ui.theme.LocalKanvoColors
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun LaunchHubScreen(
    projects: List<DesignProject>,
    themeMode: ThemeMode,
    onCreateFromPreset: (AspectRatioPreset) -> Unit,
    onCreateCustom: (width: Int, height: Int, title: String) -> Unit,
    onOpenProject: (DesignProject) -> Unit,
    onDuplicateProject: (DesignProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onCycleTheme: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    var showCustomDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = kanvoColors.studioDesk
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val isWide = maxWidth >= 600.dp
            val columns = when {
                maxWidth >= 1000.dp -> 4
                maxWidth >= 700.dp -> 3
                maxWidth >= 480.dp -> 2
                else -> 1
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isWide) 28.dp else 16.dp, vertical = 16.dp)
            ) {
                // Top Brand Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF5B21B6),
                                            Color(0xFF7C3AED),
                                            Color(0xFFA855F7)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "K",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = kanvoColors.inkPrimary
                            )
                            Text(
                                text = stringResource(R.string.studio_tagline),
                                style = MaterialTheme.typography.labelMedium,
                                color = kanvoColors.inkSecondary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onCycleTheme,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("theme_cycle_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        val (icon, label) = when (themeMode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode to "Light"
                            ThemeMode.DARK -> Icons.Default.DarkMode to "Dark"
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness to "Auto"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.cd_toggle_theme),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Hero Aspect Ratio Picker Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = kanvoColors.panelSurface,
                    border = BorderStroke(1.dp, kanvoColors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = if (isWide) 24.dp else 16.dp,
                            vertical = 18.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.create_new_design),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = kanvoColors.inkPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.choose_aspect_ratio),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = kanvoColors.inkSecondary
                                )
                            }

                            OutlinedButton(
                                onClick = { showCustomDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("custom_size_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.custom_size))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(AspectRatioPreset.entries) { preset ->
                                AspectRatioPresetCard(
                                    preset = preset,
                                    onClick = { onCreateFromPreset(preset) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Recent Designs Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.recent_designs),
                        style = MaterialTheme.typography.titleMedium,
                        color = kanvoColors.inkPrimary
                    )
                    if (projects.isNotEmpty()) {
                        Text(
                            text = "${projects.size} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = kanvoColors.inkSecondary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (projects.isEmpty()) {
                    // Empty state
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        color = kanvoColors.panelSurface,
                        border = BorderStroke(1.dp, kanvoColors.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(kanvoColors.activePillBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = kanvoColors.selectionAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.empty_recent_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = kanvoColors.inkPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_recent_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = kanvoColors.inkSecondary,
                                modifier = Modifier.widthIn(max = 420.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onCreateFromPreset(AspectRatioPreset.SQUARE_1_1) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("empty_start_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("New 1:1 Design")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            SavedProjectCard(
                                project = project,
                                onOpen = { onOpenProject(project) },
                                onDuplicate = { onDuplicateProject(project) },
                                onDelete = { onDeleteProject(project.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomCanvasSizeDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { w, h, title ->
                showCustomDialog = false
                onCreateCustom(w, h, title)
            }
        )
    }
}

@Composable
private fun AspectRatioPresetCard(
    preset: AspectRatioPreset,
    onClick: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    val ratio = preset.widthPx.toFloat() / preset.heightPx.toFloat()

    Surface(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("preset_${preset.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = kanvoColors.panelElevated,
        border = BorderStroke(1.dp, kanvoColors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .height(68.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val frameModifier = if (ratio >= 1f) {
                    Modifier
                        .width(64.dp)
                        .aspectRatio(ratio)
                } else {
                    Modifier
                        .height(60.dp)
                        .aspectRatio(ratio)
                }
                Box(
                    modifier = frameModifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(kanvoColors.panelSurface)
                        .border(2.dp, kanvoColors.selectionAccent, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = kanvoColors.selectionAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelLarge,
                color = kanvoColors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = kanvoColors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedProjectCard(
    project: DesignProject,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val kanvoColors = LocalKanvoColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .testTag("saved_project_${project.id}"),
        shape = RoundedCornerShape(16.dp),
        color = kanvoColors.panelSurface,
        border = BorderStroke(1.dp, kanvoColors.borderSubtle)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(kanvoColors.panelElevated),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = project.thumbnailPath?.let { File(it) }
                if (thumbFile != null && thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = project.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                } else {
                    Text(
                        text = project.aspectRatioLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = kanvoColors.inkSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = kanvoColors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    val formattedDate = remember(project.updatedAt) {
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(project.updatedAt))
                    }
                    Text(
                        text = "${project.pages.size} pg • ${project.canvasWidth}×${project.canvasHeight} • $formattedDate",
                        style = MaterialTheme.typography.labelMedium,
                        color = kanvoColors.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Project options",
                            tint = kanvoColors.inkSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCanvasSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: (width: Int, height: Int, title: String) -> Unit
) {
    var title by remember { mutableStateOf("Custom Design") }
    var widthStr by remember { mutableStateOf("1080") }
    var heightStr by remember { mutableStateOf("1350") }

    val w = widthStr.toIntOrNull()
    val h = heightStr.toIntOrNull()
    val isValid = w != null && h != null && w in 320..4096 && h in 320..4096

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Design name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.width_px)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.height_px)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Supported range: 320 – 4096 px",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(w!!, h!!, title) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.create_canvas))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
