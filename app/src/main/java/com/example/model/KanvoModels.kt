package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AspectRatioPreset(
    val label: String,
    val subtitle: String,
    val widthPx: Int,
    val heightPx: Int
) {
    SQUARE_1_1("1:1 Square", "Social Post • 1080 × 1080", 1080, 1080),
    STORY_9_16("9:16 Story", "Vertical Reel • 1080 × 1920", 1080, 1920),
    POSTER_3_4("3:4 Poster", "Portrait Flyer • 1200 × 1600", 1200, 1600),
    BANNER_16_9("16:9 Banner", "Widescreen • 1920 × 1080", 1920, 1080),
    PRESENTATION_4_3("4:3 Deck", "Slide Card • 1600 × 1200", 1600, 1200)
}

enum class PhotoFilterPreset(val displayName: String) {
    NONE("Original"),
    WARM("Warm"),
    COOL("Cool"),
    VIVID("Vivid"),
    MONO("Mono"),
    SEPIA("Sepia")
}

enum class KanvoShapeType(val displayName: String) {
    RECTANGLE("Rectangle"),
    ROUNDED_RECT("Card"),
    CIRCLE("Circle"),
    PILL("Pill"),
    STAR("Star"),
    TRIANGLE("Triangle"),
    DIAMOND("Diamond"),
    BADGE("Badge")
}

enum class KanvoTextAlign {
    START,
    CENTER,
    END
}

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    PNG("png", "image/png", "PNG (Lossless)"),
    JPEG("jpg", "image/jpeg", "JPEG (Compact)")
}

sealed interface CanvasLayer {
    val id: String
    val name: String
    val xFraction: Float
    val yFraction: Float
    val widthFraction: Float
    val heightFraction: Float
    val rotationDegrees: Float
    val opacity: Float
    val isLocked: Boolean
    val isVisible: Boolean

    fun withTransform(
        xFraction: Float = this.xFraction,
        yFraction: Float = this.yFraction,
        widthFraction: Float = this.widthFraction,
        heightFraction: Float = this.heightFraction,
        rotationDegrees: Float = this.rotationDegrees
    ): CanvasLayer

    fun withLock(isLocked: Boolean): CanvasLayer
    fun withVisibility(isVisible: Boolean): CanvasLayer
    fun withOpacity(opacity: Float): CanvasLayer
    fun duplicate(): CanvasLayer
}

data class TextLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Heading",
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.5f,
    override val widthFraction: Float = 0.72f,
    override val heightFraction: Float = 0.18f,
    override val rotationDegrees: Float = 0f,
    override val opacity: Float = 1f,
    override val isLocked: Boolean = false,
    override val isVisible: Boolean = true,
    val text: String = "Double tap to edit",
    val fontFamilyKey: String = "montserrat",
    val fontSizeSp: Float = 38f,
    val colorHex: String = "#18191F",
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isUppercase: Boolean = false,
    val textAlign: KanvoTextAlign = KanvoTextAlign.CENTER,
    val letterSpacingEm: Float = 0f,
    val lineHeightMultiplier: Float = 1.15f,
    val shadowEnabled: Boolean = false,
    val pillBackgroundHex: String? = null
) : CanvasLayer {
    override fun withTransform(
        xFraction: Float,
        yFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        rotationDegrees: Float
    ): CanvasLayer = copy(
        xFraction = xFraction,
        yFraction = yFraction,
        widthFraction = widthFraction,
        heightFraction = heightFraction,
        rotationDegrees = rotationDegrees
    )

    override fun withLock(isLocked: Boolean): CanvasLayer = copy(isLocked = isLocked)
    override fun withVisibility(isVisible: Boolean): CanvasLayer = copy(isVisible = isVisible)
    override fun withOpacity(opacity: Float): CanvasLayer = copy(opacity = opacity)
    override fun duplicate(): CanvasLayer = copy(
        id = UUID.randomUUID().toString(),
        name = "$name copy",
        xFraction = (xFraction + 0.04f).coerceAtMost(0.9f),
        yFraction = (yFraction + 0.04f).coerceAtMost(0.9f),
        isLocked = false
    )
}

data class ImageLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Photo",
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.5f,
    override val widthFraction: Float = 0.55f,
    override val heightFraction: Float = 0.45f,
    override val rotationDegrees: Float = 0f,
    override val opacity: Float = 1f,
    override val isLocked: Boolean = false,
    override val isVisible: Boolean = true,
    val localMediaPath: String,
    val filterPreset: PhotoFilterPreset = PhotoFilterPreset.NONE,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val cornerRadiusDp: Float = 0f
) : CanvasLayer {
    override fun withTransform(
        xFraction: Float,
        yFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        rotationDegrees: Float
    ): CanvasLayer = copy(
        xFraction = xFraction,
        yFraction = yFraction,
        widthFraction = widthFraction,
        heightFraction = heightFraction,
        rotationDegrees = rotationDegrees
    )

    override fun withLock(isLocked: Boolean): CanvasLayer = copy(isLocked = isLocked)
    override fun withVisibility(isVisible: Boolean): CanvasLayer = copy(isVisible = isVisible)
    override fun withOpacity(opacity: Float): CanvasLayer = copy(opacity = opacity)
    override fun duplicate(): CanvasLayer = copy(
        id = UUID.randomUUID().toString(),
        name = "$name copy",
        xFraction = (xFraction + 0.04f).coerceAtMost(0.9f),
        yFraction = (yFraction + 0.04f).coerceAtMost(0.9f),
        isLocked = false
    )
}

data class ShapeLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Shape",
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.5f,
    override val widthFraction: Float = 0.34f,
    override val heightFraction: Float = 0.24f,
    override val rotationDegrees: Float = 0f,
    override val opacity: Float = 1f,
    override val isLocked: Boolean = false,
    override val isVisible: Boolean = true,
    val shapeType: KanvoShapeType = KanvoShapeType.ROUNDED_RECT,
    val fillColorHex: String = "#7C3AED",
    val strokeColorHex: String? = null,
    val strokeWidthDp: Float = 0f,
    val cornerRadiusDp: Float = 16f,
    val badgeLabel: String = ""
) : CanvasLayer {
    override fun withTransform(
        xFraction: Float,
        yFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        rotationDegrees: Float
    ): CanvasLayer = copy(
        xFraction = xFraction,
        yFraction = yFraction,
        widthFraction = widthFraction,
        heightFraction = heightFraction,
        rotationDegrees = rotationDegrees
    )

    override fun withLock(isLocked: Boolean): CanvasLayer = copy(isLocked = isLocked)
    override fun withVisibility(isVisible: Boolean): CanvasLayer = copy(isVisible = isVisible)
    override fun withOpacity(opacity: Float): CanvasLayer = copy(opacity = opacity)
    override fun duplicate(): CanvasLayer = copy(
        id = UUID.randomUUID().toString(),
        name = "$name copy",
        xFraction = (xFraction + 0.04f).coerceAtMost(0.9f),
        yFraction = (yFraction + 0.04f).coerceAtMost(0.9f),
        isLocked = false
    )
}

data class DesignPage(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Page 1",
    val backgroundColorHex: String = "#FFFFFF",
    val backgroundGradientEndHex: String? = null,
    val isBackgroundLocked: Boolean = true,
    val layers: List<CanvasLayer> = emptyList()
)

data class DesignProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Design",
    val aspectRatioLabel: String = "1:1 Square",
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val pages: List<DesignPage> = listOf(DesignPage()),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null
)

data class CopiedStyle(
    val fontFamilyKey: String? = null,
    val fontSizeSp: Float? = null,
    val colorHex: String? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderlined: Boolean? = null,
    val isStrikethrough: Boolean? = null,
    val isUppercase: Boolean? = null,
    val textAlign: KanvoTextAlign? = null,
    val letterSpacingEm: Float? = null,
    val shadowEnabled: Boolean? = null,
    val pillBackgroundHex: String? = null,
    val opacity: Float = 1f,
    val shapeFillHex: String? = null,
    val cornerRadiusDp: Float? = null
)

data class SnapGuideState(
    val verticalCenter: Boolean = false,
    val horizontalCenter: Boolean = false
)

fun parseHexColor(hex: String?, fallback: Color = Color.Black): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val parsed = when (cleaned.length) {
            6 -> (0xFF000000L or cleaned.toLong(16)).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> return fallback
        }
        Color(parsed)
    } catch (_: Exception) {
        fallback
    }
}

fun parseHexColorInt(hex: String?, fallback: Int = android.graphics.Color.BLACK): Int {
    if (hex.isNullOrBlank()) return fallback
    return try {
        android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    } catch (_: Exception) {
        fallback
    }
}
