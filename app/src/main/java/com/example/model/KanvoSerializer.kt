package com.example.model

import org.json.JSONArray
import org.json.JSONObject

object KanvoSerializer {

    fun encodePages(pages: List<DesignPage>): String {
        val array = JSONArray()
        pages.forEach { page ->
            val pageObj = JSONObject().apply {
                put("id", page.id)
                put("name", page.name)
                put("backgroundColorHex", page.backgroundColorHex)
                if (page.backgroundGradientEndHex != null) {
                    put("backgroundGradientEndHex", page.backgroundGradientEndHex)
                }
                put("isBackgroundLocked", page.isBackgroundLocked)
                val layersArray = JSONArray()
                page.layers.forEach { layer ->
                    layersArray.put(encodeLayer(layer))
                }
                put("layers", layersArray)
            }
            array.put(pageObj)
        }
        return array.toString()
    }

    fun decodePages(json: String): List<DesignPage> {
        if (json.isBlank()) return listOf(DesignPage())
        return try {
            val array = JSONArray(json)
            val pages = mutableListOf<DesignPage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val layersArr = obj.optJSONArray("layers") ?: JSONArray()
                val layers = mutableListOf<CanvasLayer>()
                for (j in 0 until layersArr.length()) {
                    decodeLayer(layersArr.getJSONObject(j))?.let { layers.add(it) }
                }
                pages.add(
                    DesignPage(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Page ${i + 1}"),
                        backgroundColorHex = obj.optString("backgroundColorHex", "#FFFFFF"),
                        backgroundGradientEndHex = if (obj.has("backgroundGradientEndHex") && !obj.isNull("backgroundGradientEndHex")) {
                            obj.optString("backgroundGradientEndHex")
                        } else null,
                        isBackgroundLocked = obj.optBoolean("isBackgroundLocked", true),
                        layers = layers
                    )
                )
            }
            if (pages.isEmpty()) listOf(DesignPage()) else pages
        } catch (_: Exception) {
            listOf(DesignPage())
        }
    }

    private fun encodeLayer(layer: CanvasLayer): JSONObject {
        val obj = JSONObject().apply {
            put("id", layer.id)
            put("name", layer.name)
            put("xFraction", layer.xFraction.toDouble())
            put("yFraction", layer.yFraction.toDouble())
            put("widthFraction", layer.widthFraction.toDouble())
            put("heightFraction", layer.heightFraction.toDouble())
            put("rotationDegrees", layer.rotationDegrees.toDouble())
            put("opacity", layer.opacity.toDouble())
            put("isLocked", layer.isLocked)
            put("isVisible", layer.isVisible)
        }
        when (layer) {
            is TextLayer -> {
                obj.put("type", "text")
                obj.put("text", layer.text)
                obj.put("fontFamilyKey", layer.fontFamilyKey)
                obj.put("fontSizeSp", layer.fontSizeSp.toDouble())
                obj.put("colorHex", layer.colorHex)
                obj.put("isBold", layer.isBold)
                obj.put("isItalic", layer.isItalic)
                obj.put("isUnderlined", layer.isUnderlined)
                obj.put("isStrikethrough", layer.isStrikethrough)
                obj.put("isUppercase", layer.isUppercase)
                obj.put("textAlign", layer.textAlign.name)
                obj.put("letterSpacingEm", layer.letterSpacingEm.toDouble())
                obj.put("lineHeightMultiplier", layer.lineHeightMultiplier.toDouble())
                obj.put("shadowEnabled", layer.shadowEnabled)
                if (layer.pillBackgroundHex != null) {
                    obj.put("pillBackgroundHex", layer.pillBackgroundHex)
                }
            }
            is ImageLayer -> {
                obj.put("type", "image")
                obj.put("localMediaPath", layer.localMediaPath)
                obj.put("filterPreset", layer.filterPreset.name)
                obj.put("brightness", layer.brightness.toDouble())
                obj.put("contrast", layer.contrast.toDouble())
                obj.put("saturation", layer.saturation.toDouble())
                obj.put("cornerRadiusDp", layer.cornerRadiusDp.toDouble())
            }
            is ShapeLayer -> {
                obj.put("type", "shape")
                obj.put("shapeType", layer.shapeType.name)
                obj.put("fillColorHex", layer.fillColorHex)
                if (layer.strokeColorHex != null) {
                    obj.put("strokeColorHex", layer.strokeColorHex)
                }
                obj.put("strokeWidthDp", layer.strokeWidthDp.toDouble())
                obj.put("cornerRadiusDp", layer.cornerRadiusDp.toDouble())
                obj.put("badgeLabel", layer.badgeLabel)
            }
        }
        return obj
    }

    private fun decodeLayer(obj: JSONObject): CanvasLayer? {
        val type = obj.optString("type")
        val id = obj.optString("id")
        val name = obj.optString("name", "Layer")
        val x = obj.optDouble("xFraction", 0.5).toFloat()
        val y = obj.optDouble("yFraction", 0.5).toFloat()
        val w = obj.optDouble("widthFraction", 0.5).toFloat()
        val h = obj.optDouble("heightFraction", 0.25).toFloat()
        val rot = obj.optDouble("rotationDegrees", 0.0).toFloat()
        val opacity = obj.optDouble("opacity", 1.0).toFloat()
        val locked = obj.optBoolean("isLocked", false)
        val visible = obj.optBoolean("isVisible", true)

        return when (type) {
            "text" -> TextLayer(
                id = id,
                name = name,
                xFraction = x,
                yFraction = y,
                widthFraction = w,
                heightFraction = h,
                rotationDegrees = rot,
                opacity = opacity,
                isLocked = locked,
                isVisible = visible,
                text = obj.optString("text", "Text"),
                fontFamilyKey = obj.optString("fontFamilyKey", "montserrat"),
                fontSizeSp = obj.optDouble("fontSizeSp", 36.0).toFloat(),
                colorHex = obj.optString("colorHex", "#18191F"),
                isBold = obj.optBoolean("isBold", true),
                isItalic = obj.optBoolean("isItalic", false),
                isUnderlined = obj.optBoolean("isUnderlined", false),
                isStrikethrough = obj.optBoolean("isStrikethrough", false),
                isUppercase = obj.optBoolean("isUppercase", false),
                textAlign = runCatching {
                    KanvoTextAlign.valueOf(obj.optString("textAlign", "CENTER"))
                }.getOrDefault(KanvoTextAlign.CENTER),
                letterSpacingEm = obj.optDouble("letterSpacingEm", 0.0).toFloat(),
                lineHeightMultiplier = obj.optDouble("lineHeightMultiplier", 1.15).toFloat(),
                shadowEnabled = obj.optBoolean("shadowEnabled", false),
                pillBackgroundHex = if (obj.has("pillBackgroundHex") && !obj.isNull("pillBackgroundHex")) {
                    obj.optString("pillBackgroundHex")
                } else null
            )
            "image" -> ImageLayer(
                id = id,
                name = name,
                xFraction = x,
                yFraction = y,
                widthFraction = w,
                heightFraction = h,
                rotationDegrees = rot,
                opacity = opacity,
                isLocked = locked,
                isVisible = visible,
                localMediaPath = obj.optString("localMediaPath"),
                filterPreset = runCatching {
                    PhotoFilterPreset.valueOf(obj.optString("filterPreset", "NONE"))
                }.getOrDefault(PhotoFilterPreset.NONE),
                brightness = obj.optDouble("brightness", 0.0).toFloat(),
                contrast = obj.optDouble("contrast", 1.0).toFloat(),
                saturation = obj.optDouble("saturation", 1.0).toFloat(),
                cornerRadiusDp = obj.optDouble("cornerRadiusDp", 0.0).toFloat()
            )
            "shape" -> ShapeLayer(
                id = id,
                name = name,
                xFraction = x,
                yFraction = y,
                widthFraction = w,
                heightFraction = h,
                rotationDegrees = rot,
                opacity = opacity,
                isLocked = locked,
                isVisible = visible,
                shapeType = runCatching {
                    KanvoShapeType.valueOf(obj.optString("shapeType", "ROUNDED_RECT"))
                }.getOrDefault(KanvoShapeType.ROUNDED_RECT),
                fillColorHex = obj.optString("fillColorHex", "#7C3AED"),
                strokeColorHex = if (obj.has("strokeColorHex") && !obj.isNull("strokeColorHex")) {
                    obj.optString("strokeColorHex")
                } else null,
                strokeWidthDp = obj.optDouble("strokeWidthDp", 0.0).toFloat(),
                cornerRadiusDp = obj.optDouble("cornerRadiusDp", 16.0).toFloat(),
                badgeLabel = obj.optString("badgeLabel", "")
            )
            else -> null
        }
    }
}
