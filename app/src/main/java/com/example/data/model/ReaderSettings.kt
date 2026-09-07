package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PageTurnMode(val label: String) {
    VERTICAL_SCROLL("上下滚动"),
    LEFT_TO_RIGHT("从左往右"),
    RIGHT_TO_LEFT("从右往左")
}

data class NovelReaderConfig(
    val backgroundColorHex: String = "#F5E6D3",
    val textColorHex: String = "#2D2620",
    val isNightMode: Boolean = false,
    val normalBrightness: Float = 0.5f, // 0.01f to 1.0f
    val nightBrightness: Float = 0.10f, // 0.01f to 0.20f
    val fontSizeSp: Float = 18f, // 12 to 32
    val letterSpacing: Float = 0.05f, // 0.0 to 1.0
    val lineHeightMultiplier: Float = 1.8f, // 1.0 to 3.0
    val paragraphSpacingMultiplier: Float = 1.0f, // 0.5 to 3.0
    val paddingHorizontalDp: Float = 16f, // 0 to 80 dp
    val pageTurnMode: PageTurnMode = PageTurnMode.LEFT_TO_RIGHT,
    val showStatusBarUi: Boolean = true,
    val statusBarAtTopRight: Boolean = true,
    val statusBarOpaqueBlack: Boolean = false
) {
    val brightnessPercent: Float
        get() = if (isNightMode) nightBrightness.coerceIn(0.01f, 0.20f) else normalBrightness.coerceIn(0.01f, 1.0f)

    val currentBgColor: Color
        get() = if (isNightMode) Color.Black else try {
            Color(android.graphics.Color.parseColor(backgroundColorHex))
        } catch (_: Exception) {
            Color(0xFFF5E6D3)
        }

    val currentTextColor: Color
        get() = if (isNightMode) Color(0xFF888888) else try {
            Color(android.graphics.Color.parseColor(textColorHex))
        } catch (_: Exception) {
            Color(0xFF2D2620)
        }
}

data class ComicReaderConfig(
    val isNightMode: Boolean = false,
    val normalBrightness: Float = 0.5f, // 0.01f to 1.0f
    val nightBrightness: Float = 0.10f, // 0.01f to 0.20f
    val paddingHorizontalDp: Float = 0f, // 0 to 80 dp
    val pageTurnMode: PageTurnMode = PageTurnMode.LEFT_TO_RIGHT,
    val autoCropWhiteBorders: Boolean = true,
    val autoSplitDoublePage: Boolean = true,
    val zoomScale: Float = 1.0f,
    val showStatusBarUi: Boolean = true,
    val statusBarAtTopRight: Boolean = true,
    val statusBarOpaqueBlack: Boolean = false
) {
    val brightnessPercent: Float
        get() = if (isNightMode) nightBrightness.coerceIn(0.01f, 0.20f) else normalBrightness.coerceIn(0.01f, 1.0f)
}

object NovelColorPalettes {
    data class Palette(val name: String, val bgHex: String, val textHex: String)

    val presets = listOf(
        Palette("纯黑", "#000000", "#A0A0A0"),
        Palette("高级灰", "#808080", "#1A1A1A"),
        Palette("纯白", "#FFFFFF", "#1E1E1E"),
        Palette("护眼绿", "#C7EDCC", "#1F3B20"),
        Palette("米黄", "#F5E6D3", "#2C2016")
    )
}
