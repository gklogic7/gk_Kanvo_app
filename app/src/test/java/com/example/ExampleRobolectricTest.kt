package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.CanvasExporter
import com.example.model.AspectRatioPreset
import com.example.model.DesignPage
import com.example.model.KanvoSerializer
import com.example.model.KanvoShapeType
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.ThemeMode
import com.example.ui.LaunchHubScreen
import com.example.ui.theme.KanvoTheme
import com.example.viewmodel.KanvoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name is Kanvo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kanvo", appName)
    }

    @Test
    fun `serializer preserves multi-page layers accurately`() {
        val pages = listOf(
            DesignPage(
                name = "Cover",
                backgroundColorHex = "#FAF6F0",
                layers = listOf(
                    TextLayer(
                        name = "MINDSET",
                        text = "MINDSET",
                        fontFamilyKey = "montserrat",
                        colorHex = "#C84B31",
                        isBold = true,
                        isUppercase = true
                    ),
                    ShapeLayer(
                        name = "Accent Star",
                        shapeType = KanvoShapeType.STAR,
                        fillColorHex = "#7C3AED"
                    )
                )
            ),
            DesignPage(name = "Page 2", backgroundColorHex = "#1E1B4B")
        )

        val encoded = KanvoSerializer.encodePages(pages)
        val decoded = KanvoSerializer.decodePages(encoded)

        assertEquals(2, decoded.size)
        assertEquals("Cover", decoded[0].name)
        assertEquals(2, decoded[0].layers.size)
        val decodedText = decoded[0].layers[0] as TextLayer
        assertEquals("MINDSET", decodedText.text)
        assertTrue(decodedText.isBold)
        assertTrue(decodedText.isUppercase)
        val decodedShape = decoded[0].layers[1] as ShapeLayer
        assertEquals(KanvoShapeType.STAR, decodedShape.shapeType)
    }

    @Test
    fun `viewModel supports project creation, layering, copy style, undo redo, and pages`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = KanvoViewModel(app)

        vm.createBlankProject(preset = AspectRatioPreset.POSTER_3_4)
        assertNotNull(vm.uiState.value.activeProject)
        assertEquals(1200, vm.uiState.value.activeProject?.canvasWidth)

        vm.addTextLayer(initialText = "MINDSET", fontSizeSp = 44f, isBold = true)
        vm.updateSelectedText { it.copy(colorHex = "#C84B31") }
        val firstLayerId = vm.uiState.value.selectedLayerId!!

        vm.addTextLayer(initialText = "Subtitle", fontSizeSp = 22f, isBold = false)
        val secondLayerId = vm.uiState.value.selectedLayerId!!

        // Select first, copy style, tap second to apply
        vm.selectLayer(firstLayerId)
        vm.toggleCopyStyleMode()
        vm.selectLayer(secondLayerId)

        val styledSecond = vm.uiState.value.selectedLayer as TextLayer
        assertEquals("#C84B31", styledSecond.colorHex)
        assertTrue(styledSecond.isBold)

        // Undo restores original unstyled subtitle
        assertTrue(vm.uiState.value.canUndo)
        vm.undo()
        val revertedSecond = vm.uiState.value.activePage?.layers?.last() as TextLayer
        assertFalse(revertedSecond.isBold)

        // Multi-page management
        vm.addPage()
        assertEquals(2, vm.uiState.value.activeProject?.pages?.size)
        assertEquals(1, vm.uiState.value.activePageIndex)

        vm.deleteActivePage()
        assertEquals(1, vm.uiState.value.activeProject?.pages?.size)
    }

    @Test
    fun `canvas exporter renders bitmap cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val page = DesignPage(
            name = "Export Test",
            backgroundColorHex = "#FFFFFF",
            backgroundGradientEndHex = "#EDE9FE",
            layers = listOf(
                ShapeLayer(shapeType = KanvoShapeType.ROUNDED_RECT, fillColorHex = "#7C3AED"),
                TextLayer(text = "KANVO", colorHex = "#FFFFFF", isBold = true)
            )
        )
        val bmp = CanvasExporter.renderPageBitmap(context, page, 320, 320)
        assertEquals(320, bmp.width)
        assertEquals(320, bmp.height)
        bmp.recycle()
    }

    @Test
    fun `launch hub renders aspect ratio presets and triggers creation`() {
        var createdPreset: AspectRatioPreset? = null
        composeTestRule.setContent {
            KanvoTheme(themeMode = ThemeMode.LIGHT) {
                LaunchHubScreen(
                    projects = emptyList(),
                    themeMode = ThemeMode.LIGHT,
                    onCreateFromPreset = { createdPreset = it },
                    onCreateCustom = { _, _, _ -> },
                    onOpenProject = {},
                    onDuplicateProject = {},
                    onDeleteProject = {},
                    onCycleTheme = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Kanvo").assertExists()
        composeTestRule.onNodeWithTag("preset_square_1_1").performClick()
        assertEquals(AspectRatioPreset.SQUARE_1_1, createdPreset)
    }
}
