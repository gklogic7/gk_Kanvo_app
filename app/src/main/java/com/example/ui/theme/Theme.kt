package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.ThemeMode

@Immutable
data class KanvoStudioColors(
    val studioDesk: Color,
    val panelSurface: Color,
    val panelElevated: Color,
    val borderSubtle: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val selectionAccent: Color,
    val activePillBackground: Color,
    val snapGuide: Color,
    val isDark: Boolean
)

val LocalKanvoColors = staticCompositionLocalOf {
    KanvoStudioColors(
        studioDesk = LightStudioDesk,
        panelSurface = LightPanelSurface,
        panelElevated = LightPanelElevated,
        borderSubtle = LightBorderSubtle,
        inkPrimary = LightInkPrimary,
        inkSecondary = LightInkSecondary,
        selectionAccent = LightVioletPrimary,
        activePillBackground = LightLavenderActive,
        snapGuide = LightSnapGuide,
        isDark = false
    )
}

private val KanvoLightColorScheme = lightColorScheme(
    primary = LightVioletPrimary,
    onPrimary = LightVioletOnPrimary,
    primaryContainer = LightLavenderActive,
    onPrimaryContainer = LightVioletPrimary,
    secondary = LightInkSecondary,
    onSecondary = Color.White,
    background = LightStudioDesk,
    onBackground = LightInkPrimary,
    surface = LightPanelSurface,
    onSurface = LightInkPrimary,
    surfaceVariant = LightPanelElevated,
    onSurfaceVariant = LightInkSecondary,
    outline = LightBorderSubtle,
    error = LightDanger
)

private val KanvoDarkColorScheme = darkColorScheme(
    primary = DarkVioletPrimary,
    onPrimary = DarkVioletOnPrimary,
    primaryContainer = DarkLavenderActive,
    onPrimaryContainer = DarkVioletPrimary,
    secondary = DarkInkSecondary,
    onSecondary = DarkStudioDesk,
    background = DarkStudioDesk,
    onBackground = DarkInkPrimary,
    surface = DarkPanelSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkPanelElevated,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkBorderSubtle,
    error = DarkDanger
)

private val KanvoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun KanvoTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val studioColors = if (useDark) {
        KanvoStudioColors(
            studioDesk = DarkStudioDesk,
            panelSurface = DarkPanelSurface,
            panelElevated = DarkPanelElevated,
            borderSubtle = DarkBorderSubtle,
            inkPrimary = DarkInkPrimary,
            inkSecondary = DarkInkSecondary,
            selectionAccent = DarkVioletPrimary,
            activePillBackground = DarkLavenderActive,
            snapGuide = DarkSnapGuide,
            isDark = true
        )
    } else {
        KanvoStudioColors(
            studioDesk = LightStudioDesk,
            panelSurface = LightPanelSurface,
            panelElevated = LightPanelElevated,
            borderSubtle = LightBorderSubtle,
            inkPrimary = LightInkPrimary,
            inkSecondary = LightInkSecondary,
            selectionAccent = LightVioletPrimary,
            activePillBackground = LightLavenderActive,
            snapGuide = LightSnapGuide,
            isDark = false
        )
    }

    val colorScheme = if (useDark) KanvoDarkColorScheme else KanvoLightColorScheme

    CompositionLocalProvider(LocalKanvoColors provides studioColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KanvoTypography,
            shapes = KanvoShapes,
            content = content
        )
    }
}
