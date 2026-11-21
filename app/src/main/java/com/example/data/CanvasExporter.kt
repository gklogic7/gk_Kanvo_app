package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import com.example.model.DesignPage
import com.example.model.DesignProject
import com.example.model.ExportFormat
import com.example.model.ImageLayer
import com.example.model.KanvoShapeType
import com.example.model.KanvoTextAlign
import com.example.model.PhotoFilterPreset
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.parseHexColorInt
import com.example.ui.theme.KanvoCanvasFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class ImportedMediaResult(
    val localPath: String,
    val aspectRatio: Float
)

object KanvoImageFilters {
    fun buildAndroidColorMatrix(
        preset: PhotoFilterPreset,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ): ColorMatrix {
        val result = ColorMatrix()

        // Preset matrix
        val presetMatrix = ColorMatrix()
        when (preset) {
            PhotoFilterPreset.NONE -> Unit
            PhotoFilterPreset.WARM -> presetMatrix.setScale(1.12f, 1.02f, 0.88f, 1f)
            PhotoFilterPreset.COOL -> presetMatrix.setScale(0.90f, 1.02f, 1.15f, 1f)
            PhotoFilterPreset.VIVID -> {
                presetMatrix.setSaturation(1.42f)
            }
            PhotoFilterPreset.MONO -> presetMatrix.setSaturation(0f)
            PhotoFilterPreset.SEPIA -> {
                presetMatrix.setSaturation(0f)
                val sepia = ColorMatrix().apply {
                    setScale(1.12f, 0.96f, 0.74f, 1f)
                }
                presetMatrix.postConcat(sepia)
            }
        }
        result.postConcat(presetMatrix)

        // Saturation
        if (saturation != 1f) {
            val satMatrix = ColorMatrix().apply { setSaturation(saturation.coerceIn(0f, 2f)) }
            result.postConcat(satMatrix)
        }

        // Contrast & Brightness (-0.5..0.5 mapped to -128..128)
        val c = contrast.coerceIn(0.4f, 2f)
        val t = (1f - c) * 127.5f + (brightness.coerceIn(-0.5f, 0.5f) * 255f)
        val bcMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.postConcat(bcMatrix)
        return result
    }
}

object MediaHelper {
    suspend fun importImageToAppStorage(context: Context, uri: Uri): ImportedMediaResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                val mediaDir = File(context.filesDir, "kanvo_media").apply { mkdirs() }
                val destFile = File(mediaDir, "img_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = false }
                    val decoded = BitmapFactory.decodeStream(input, null, boundsOpts)
                        ?: return@runCatching null
                    val maxDim = 2048
                    val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
                        val scale = min(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
                        Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * scale).toInt().coerceAtLeast(1),
                            (decoded.height * scale).toInt().coerceAtLeast(1),
                            true
                        ).also {
                            if (it !== decoded) decoded.recycle()
                        }
                    } else {
                        decoded
                    }
                    FileOutputStream(destFile).use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    val aspect = scaled.width.toFloat() / scaled.height.toFloat().coerceAtLeast(1f)
                    scaled.recycle()
                    ImportedMediaResult(destFile.absolutePath, aspect)
                }
            }.getOrNull()
        }
}

object CanvasExporter {

    suspend fun saveProjectThumbnail(context: Context, project: DesignProject): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val page = project.pages.firstOrNull() ?: return@runCatching null
                val maxThumb = 480f
                val scale = min(maxThumb / project.canvasWidth, maxThumb / project.canvasHeight)
                val w = (project.canvasWidth * scale).toInt().coerceAtLeast(120)
                val h = (project.canvasHeight * scale).toInt().coerceAtLeast(120)
                val bmp = renderPageBitmap(context, page, w, h)
                val thumbDir = File(context.filesDir, "kanvo_thumbs").apply { mkdirs() }
                val thumbFile = File(thumbDir, "thumb_${project.id}.png")
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                bmp.recycle()
                thumbFile.absolutePath
            }.getOrNull()
        }

    suspend fun exportPagesToGallery(
        context: Context,
        project: DesignProject,
        pageIndices: List<Int>,
        format: ExportFormat
    ): List<Uri> = withContext(Dispatchers.IO) {
        val savedUris = mutableListOf<Uri>()
        val safeTitle = project.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "KanvoDesign" }

        for (index in pageIndices) {
            val page = project.pages.getOrNull(index) ?: continue
            val bitmap = renderPageBitmap(context, page, project.canvasWidth, project.canvasHeight)
            val fileName = "${safeTitle}_p${index + 1}_${System.currentTimeMillis()}.${format.extension}"
            val compressFormat = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Kanvo")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(compressFormat, 95, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    savedUris.add(uri)
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(compressFormat, 95, out)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile
                )
                savedUris.add(uri)
            }
            bitmap.recycle()
        }
        savedUris
    }

    suspend fun createShareIntent(
        context: Context,
        project: DesignProject,
        pageIndex: Int,
        format: ExportFormat
    ): Intent? = withContext(Dispatchers.IO) {
        val page = project.pages.getOrNull(pageIndex) ?: return@withContext null
        val bitmap = renderPageBitmap(context, page, project.canvasWidth, project.canvasHeight)
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, "kanvo_share_${System.currentTimeMillis()}.${format.extension}")
        val compressFormat = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        FileOutputStream(file).use { out ->
            bitmap.compress(compressFormat, 95, out)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun renderPageBitmap(
        context: Context,
        page: DesignPage,
        width: Int,
        height: Int
    ): Bitmap {
        val safeW = width.coerceIn(64, 2400)
        val safeH = height.coerceIn(64, 2400)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        val startColor = parseHexColorInt(page.backgroundColorHex, android.graphics.Color.WHITE)
        val endHex = page.backgroundGradientEndHex
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (!endHex.isNullOrBlank()) {
            val endColor = parseHexColorInt(endHex, startColor)
            bgPaint.shader = LinearGradient(
                0f, 0f, safeW.toFloat(), safeH.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
        } else {
            bgPaint.color = startColor
        }
        canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), bgPaint)

        // Draw layers in z-order (bottom to top)
        val scaleFactor = safeW / 380f
        page.layers.filter { it.isVisible }.forEach { layer ->
            val cx = layer.xFraction * safeW
            val cy = layer.yFraction * safeH
            val lw = (layer.widthFraction * safeW).coerceAtLeast(16f)
            val lh = (layer.heightFraction * safeH).coerceAtLeast(16f)
            val rect = RectF(cx - lw / 2f, cy - lh / 2f, cx + lw / 2f, cy + lh / 2f)

            canvas.withRotation(layer.rotationDegrees, cx, cy) {
                when (layer) {
                    is ShapeLayer -> drawShapeLayer(this, layer, rect, scaleFactor)
                    is ImageLayer -> drawImageLayer(this, layer, rect, scaleFactor)
                    is TextLayer -> drawTextLayer(context, this, layer, rect, scaleFactor)
                }
            }
        }
        return bitmap
    }

    private fun drawShapeLayer(canvas: Canvas, layer: ShapeLayer, rect: RectF, scaleFactor: Float) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = parseHexColorInt(layer.fillColorHex, android.graphics.Color.MAGENTA)
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
        }
        val path = buildShapePath(layer.shapeType, rect, layer.cornerRadiusDp * scaleFactor)
        canvas.drawPath(path, fillPaint)

        if (!layer.strokeColorHex.isNullOrBlank() && layer.strokeWidthDp > 0f) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = parseHexColorInt(layer.strokeColorHex, android.graphics.Color.BLACK)
                strokeWidth = layer.strokeWidthDp * scaleFactor
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun buildShapePath(type: KanvoShapeType, rect: RectF, radiusPx: Float): Path {
        val path = Path()
        when (type) {
            KanvoShapeType.RECTANGLE -> path.addRect(rect, Path.Direction.CW)
            KanvoShapeType.ROUNDED_RECT -> {
                val r = radiusPx.coerceAtMost(min(rect.width(), rect.height()) / 2f)
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            KanvoShapeType.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            KanvoShapeType.PILL -> {
                val r = min(rect.width(), rect.height()) / 2f
                path.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            KanvoShapeType.TRIANGLE -> {
                path.moveTo(rect.centerX(), rect.top)
                path.lineTo(rect.right, rect.bottom)
                path.lineTo(rect.left, rect.bottom)
                path.close()
            }
            KanvoShapeType.DIAMOND -> {
                path.moveTo(rect.centerX(), rect.top)
                path.lineTo(rect.right, rect.centerY())
                path.lineTo(rect.centerX(), rect.bottom)
                path.lineTo(rect.left, rect.centerY())
                path.close()
            }
            KanvoShapeType.STAR -> addStarPath(path, rect, 5, 0.45f)
            KanvoShapeType.BADGE -> addStarPath(path, rect, 12, 0.82f)
        }
        return path
    }

    private fun addStarPath(path: Path, rect: RectF, points: Int, innerRatio: Float) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val rx = rect.width() / 2f
        val ry = rect.height() / 2f
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

    private fun drawImageLayer(canvas: Canvas, layer: ImageLayer, rect: RectF, scaleFactor: Float) {
        val file = File(layer.localMediaPath)
        if (!file.exists()) return
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            colorFilter = ColorMatrixColorFilter(
                KanvoImageFilters.buildAndroidColorMatrix(
                    layer.filterPreset,
                    layer.brightness,
                    layer.contrast,
                    layer.saturation
                )
            )
        }
        val radius = layer.cornerRadiusDp * scaleFactor
        canvas.withSave {
            if (radius > 0f) {
                val clipPath = Path().apply {
                    addRoundRect(rect, radius, radius, Path.Direction.CW)
                }
                clipPath(clipPath)
            }
            // CenterCrop fit
            val scale = maxOf(rect.width() / bmp.width, rect.height() / bmp.height)
            val drawW = bmp.width * scale
            val drawH = bmp.height * scale
            val drawLeft = rect.centerX() - drawW / 2f
            val drawTop = rect.centerY() - drawH / 2f
            val dest = RectF(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH)
            drawBitmap(bmp, null, dest, paint)
        }
        bmp.recycle()
    }

    private fun drawTextLayer(
        context: Context,
        canvas: Canvas,
        layer: TextLayer,
        rect: RectF,
        scaleFactor: Float
    ) {
        if (!layer.pillBackgroundHex.isNullOrBlank()) {
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parseHexColorInt(layer.pillBackgroundHex, android.graphics.Color.WHITE)
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            }
            val r = 14f * scaleFactor
            canvas.drawRoundRect(rect, r, r, pillPaint)
        }

        val fontOption = KanvoCanvasFonts.get(layer.fontFamilyKey)
        val baseTypeface = runCatching {
            ResourcesCompat.getFont(context, fontOption.fontResId)
        }.getOrNull() ?: Typeface.DEFAULT

        val style = when {
            layer.isBold && layer.isItalic -> Typeface.BOLD_ITALIC
            layer.isBold -> Typeface.BOLD
            layer.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val typeface = Typeface.create(baseTypeface, style)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = layer.fontSizeSp * scaleFactor
            color = parseHexColorInt(layer.colorHex, android.graphics.Color.BLACK)
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            letterSpacing = layer.letterSpacingEm
            isUnderlineText = layer.isUnderlined
            isStrikeThruText = layer.isStrikethrough
            if (layer.shadowEnabled) {
                setShadowLayer(4f * scaleFactor, 2f * scaleFactor, 3f * scaleFactor, 0x55000000)
            }
        }

        val displayText = if (layer.isUppercase) layer.text.uppercase() else layer.text
        val alignment = when (layer.textAlign) {
            KanvoTextAlign.START -> Layout.Alignment.ALIGN_NORMAL
            KanvoTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            KanvoTextAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val layoutWidth = rect.width().toInt().coerceAtLeast(16)
        val staticLayout = StaticLayout.Builder
            .obtain(displayText, 0, displayText.length, textPaint, layoutWidth)
            .setAlignment(alignment)
            .setLineSpacing(0f, layer.lineHeightMultiplier)
            .build()

        canvas.withSave {
            val yOffset = rect.top + ((rect.height() - staticLayout.height) / 2f).coerceAtLeast(0f)
            translate(rect.left, yOffset)
            staticLayout.draw(this)
        }
    }
}
