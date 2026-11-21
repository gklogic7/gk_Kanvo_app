package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KanvoEditorScreen
import com.example.ui.LaunchHubScreen
import com.example.ui.theme.KanvoTheme
import com.example.viewmodel.KanvoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: KanvoViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            KanvoTheme(themeMode = themeMode) {
                Crossfade(
                    targetState = uiState.activeProject != null,
                    label = "kanvo_screen_crossfade"
                ) { inEditor ->
                    if (inEditor) {
                        KanvoEditorScreen(
                            uiState = uiState,
                            themeMode = themeMode,
                            viewModel = viewModel
                        )
                    } else {
                        LaunchHubScreen(
                            projects = savedProjects,
                            themeMode = themeMode,
                            onCreateFromPreset = { preset ->
                                viewModel.createBlankProject(preset = preset)
                            },
                            onCreateCustom = { w, h, title ->
                                viewModel.createBlankProject(
                                    customWidth = w,
                                    customHeight = h,
                                    title = title
                                )
                            },
                            onOpenProject = viewModel::openExistingProject,
                            onDuplicateProject = viewModel::duplicateProject,
                            onDeleteProject = viewModel::deleteProject,
                            onCycleTheme = viewModel::cycleThemeMode
                        )
                    }
                }
            }
        }
    }
}
