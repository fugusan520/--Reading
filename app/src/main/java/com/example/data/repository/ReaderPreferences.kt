package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ComicReaderConfig
import com.example.data.model.NovelReaderConfig
import com.example.data.model.PageTurnMode

class ReaderPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("reader_preferences_v3", Context.MODE_PRIVATE)

    companion object {
        // App-level
        private const val KEY_SKIP_DETAILS_PAGE = "key_skip_details_page"

        // Novel Keys
        private const val KEY_NOVEL_FONT_SIZE = "key_novel_font_size"
        private const val KEY_NOVEL_LETTER_SPACING = "key_novel_letter_spacing"
        private const val KEY_NOVEL_LINE_HEIGHT = "key_novel_line_height"
        private const val KEY_NOVEL_PARAGRAPH_SPACING = "key_novel_paragraph_spacing"
        private const val KEY_NOVEL_PADDING_HORIZONTAL = "key_novel_padding_horizontal"
        private const val KEY_NOVEL_PAGE_TURN_MODE = "key_novel_page_turn_mode"
        private const val KEY_NOVEL_BG_COLOR = "key_novel_bg_color"
        private const val KEY_NOVEL_TEXT_COLOR = "key_novel_text_color"
        private const val KEY_NOVEL_IS_NIGHT_MODE = "key_novel_is_night_mode"
        private const val KEY_NOVEL_NORMAL_BRIGHTNESS = "key_novel_normal_brightness"
        private const val KEY_NOVEL_NIGHT_BRIGHTNESS = "key_novel_night_brightness"
        private const val KEY_NOVEL_SHOW_STATUS_BAR = "key_novel_show_status_bar"
        private const val KEY_NOVEL_STATUS_BAR_TOP_RIGHT = "key_novel_status_bar_top_right"
        private const val KEY_NOVEL_STATUS_BAR_OPAQUE = "key_novel_status_bar_opaque"

        // Comic Keys
        private const val KEY_COMIC_PAGE_TURN_MODE = "key_comic_page_turn_mode"
        private const val KEY_COMIC_PADDING_HORIZONTAL = "key_comic_padding_horizontal"
        private const val KEY_COMIC_ZOOM_SCALE = "key_comic_zoom_scale"
        private const val KEY_COMIC_IS_NIGHT_MODE = "key_comic_is_night_mode"
        private const val KEY_COMIC_NORMAL_BRIGHTNESS = "key_comic_normal_brightness"
        private const val KEY_COMIC_NIGHT_BRIGHTNESS = "key_comic_night_brightness"
        private const val KEY_COMIC_AUTO_CROP = "key_comic_auto_crop"
        private const val KEY_COMIC_AUTO_SPLIT = "key_comic_auto_split"
        private const val KEY_COMIC_SHOW_STATUS_BAR = "key_comic_show_status_bar"
        private const val KEY_COMIC_STATUS_BAR_TOP_RIGHT = "key_comic_status_bar_top_right"
        private const val KEY_COMIC_STATUS_BAR_OPAQUE = "key_comic_status_bar_opaque"
    }

    // 1.2 Skip details page toggle (default false: show details page by default)
    fun getSkipDetailsPage(): Boolean {
        return prefs.getBoolean(KEY_SKIP_DETAILS_PAGE, false)
    }

    fun setSkipDetailsPage(skip: Boolean) {
        prefs.edit().putBoolean(KEY_SKIP_DETAILS_PAGE, skip).apply()
    }

    // Novel Config
    fun loadNovelConfig(): NovelReaderConfig {
        val pageTurnModeStr = prefs.getString(KEY_NOVEL_PAGE_TURN_MODE, PageTurnMode.LEFT_TO_RIGHT.name)
            ?: PageTurnMode.LEFT_TO_RIGHT.name
        val pageTurnMode = try {
            PageTurnMode.valueOf(pageTurnModeStr)
        } catch (_: Exception) {
            PageTurnMode.LEFT_TO_RIGHT
        }

        return NovelReaderConfig(
            backgroundColorHex = prefs.getString(KEY_NOVEL_BG_COLOR, "#F5E6D3") ?: "#F5E6D3",
            textColorHex = prefs.getString(KEY_NOVEL_TEXT_COLOR, "#2D2620") ?: "#2D2620",
            isNightMode = prefs.getBoolean(KEY_NOVEL_IS_NIGHT_MODE, false),
            normalBrightness = prefs.getFloat(KEY_NOVEL_NORMAL_BRIGHTNESS, 0.5f).coerceIn(0.01f, 1.0f),
            nightBrightness = prefs.getFloat(KEY_NOVEL_NIGHT_BRIGHTNESS, 0.10f).coerceIn(0.01f, 0.20f),
            fontSizeSp = prefs.getFloat(KEY_NOVEL_FONT_SIZE, 18f).coerceIn(12f, 32f),
            letterSpacing = prefs.getFloat(KEY_NOVEL_LETTER_SPACING, 0.05f).coerceIn(0.0f, 1.0f),
            lineHeightMultiplier = prefs.getFloat(KEY_NOVEL_LINE_HEIGHT, 1.8f).coerceIn(1.0f, 3.0f),
            paragraphSpacingMultiplier = prefs.getFloat(KEY_NOVEL_PARAGRAPH_SPACING, 1.0f).coerceIn(0.5f, 3.0f),
            paddingHorizontalDp = prefs.getFloat(KEY_NOVEL_PADDING_HORIZONTAL, 16f).coerceIn(0f, 80f),
            pageTurnMode = pageTurnMode,
            showStatusBarUi = prefs.getBoolean(KEY_NOVEL_SHOW_STATUS_BAR, true),
            statusBarAtTopRight = prefs.getBoolean(KEY_NOVEL_STATUS_BAR_TOP_RIGHT, true),
            statusBarOpaqueBlack = prefs.getBoolean(KEY_NOVEL_STATUS_BAR_OPAQUE, false)
        )
    }

    fun saveNovelConfig(config: NovelReaderConfig) {
        prefs.edit()
            .putString(KEY_NOVEL_BG_COLOR, config.backgroundColorHex)
            .putString(KEY_NOVEL_TEXT_COLOR, config.textColorHex)
            .putBoolean(KEY_NOVEL_IS_NIGHT_MODE, config.isNightMode)
            .putFloat(KEY_NOVEL_NORMAL_BRIGHTNESS, config.normalBrightness)
            .putFloat(KEY_NOVEL_NIGHT_BRIGHTNESS, config.nightBrightness)
            .putFloat(KEY_NOVEL_FONT_SIZE, config.fontSizeSp)
            .putFloat(KEY_NOVEL_LETTER_SPACING, config.letterSpacing)
            .putFloat(KEY_NOVEL_LINE_HEIGHT, config.lineHeightMultiplier)
            .putFloat(KEY_NOVEL_PARAGRAPH_SPACING, config.paragraphSpacingMultiplier)
            .putFloat(KEY_NOVEL_PADDING_HORIZONTAL, config.paddingHorizontalDp)
            .putString(KEY_NOVEL_PAGE_TURN_MODE, config.pageTurnMode.name)
            .putBoolean(KEY_NOVEL_SHOW_STATUS_BAR, config.showStatusBarUi)
            .putBoolean(KEY_NOVEL_STATUS_BAR_TOP_RIGHT, config.statusBarAtTopRight)
            .putBoolean(KEY_NOVEL_STATUS_BAR_OPAQUE, config.statusBarOpaqueBlack)
            .apply()
    }

    // Comic Config
    fun loadComicConfig(): ComicReaderConfig {
        val pageTurnModeStr = prefs.getString(KEY_COMIC_PAGE_TURN_MODE, PageTurnMode.LEFT_TO_RIGHT.name)
            ?: PageTurnMode.LEFT_TO_RIGHT.name
        val pageTurnMode = try {
            PageTurnMode.valueOf(pageTurnModeStr)
        } catch (_: Exception) {
            PageTurnMode.LEFT_TO_RIGHT
        }

        return ComicReaderConfig(
            isNightMode = prefs.getBoolean(KEY_COMIC_IS_NIGHT_MODE, false),
            normalBrightness = prefs.getFloat(KEY_COMIC_NORMAL_BRIGHTNESS, 0.5f).coerceIn(0.01f, 1.0f),
            nightBrightness = prefs.getFloat(KEY_COMIC_NIGHT_BRIGHTNESS, 0.10f).coerceIn(0.01f, 0.20f),
            paddingHorizontalDp = prefs.getFloat(KEY_COMIC_PADDING_HORIZONTAL, 0f).coerceIn(0f, 80f),
            pageTurnMode = pageTurnMode,
            autoCropWhiteBorders = prefs.getBoolean(KEY_COMIC_AUTO_CROP, true),
            autoSplitDoublePage = prefs.getBoolean(KEY_COMIC_AUTO_SPLIT, true),
            zoomScale = prefs.getFloat(KEY_COMIC_ZOOM_SCALE, 1.0f).coerceIn(0.5f, 5.0f),
            showStatusBarUi = prefs.getBoolean(KEY_COMIC_SHOW_STATUS_BAR, true),
            statusBarAtTopRight = prefs.getBoolean(KEY_COMIC_STATUS_BAR_TOP_RIGHT, true),
            statusBarOpaqueBlack = prefs.getBoolean(KEY_COMIC_STATUS_BAR_OPAQUE, false)
        )
    }

    fun saveComicConfig(config: ComicReaderConfig) {
        prefs.edit()
            .putBoolean(KEY_COMIC_IS_NIGHT_MODE, config.isNightMode)
            .putFloat(KEY_COMIC_NORMAL_BRIGHTNESS, config.normalBrightness)
            .putFloat(KEY_COMIC_NIGHT_BRIGHTNESS, config.nightBrightness)
            .putFloat(KEY_COMIC_PADDING_HORIZONTAL, config.paddingHorizontalDp)
            .putString(KEY_COMIC_PAGE_TURN_MODE, config.pageTurnMode.name)
            .putBoolean(KEY_COMIC_AUTO_CROP, config.autoCropWhiteBorders)
            .putBoolean(KEY_COMIC_AUTO_SPLIT, config.autoSplitDoublePage)
            .putFloat(KEY_COMIC_ZOOM_SCALE, config.zoomScale)
            .putBoolean(KEY_COMIC_SHOW_STATUS_BAR, config.showStatusBarUi)
            .putBoolean(KEY_COMIC_STATUS_BAR_TOP_RIGHT, config.statusBarAtTopRight)
            .putBoolean(KEY_COMIC_STATUS_BAR_OPAQUE, config.statusBarOpaqueBlack)
            .apply()
    }
}
