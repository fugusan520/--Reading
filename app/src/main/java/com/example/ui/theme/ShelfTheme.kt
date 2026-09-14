package com.example.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class ShelfThemePreset(
    val name: String,
    val primary: Color,
    val secondary: Color
)

object ShelfThemePresets {
    val presets = listOf(
        ShelfThemePreset("黑绿", Color(0xFF000000), Color(0xFFC7E8B0)),
        ShelfThemePreset("黑蓝", Color(0xFF000000), Color(0xFFA8C8E8)),
        ShelfThemePreset("白蓝", Color(0xFFFFFFFF), Color(0xFFA8C8E8)),
        ShelfThemePreset("白红", Color(0xFFFFFFFF), Color(0xFFE8A8A8)),
        ShelfThemePreset("深空灰", Color(0xFF1E1E1E), Color(0xFF8E8E8E))
    )

    private const val PREFS_NAME = "shelf_theme_prefs"
    private const val KEY_PRIMARY = "shelf_primary"
    private const val KEY_SECONDARY = "shelf_secondary"
    private const val KEY_PRESET = "shelf_preset_name"

    fun loadTheme(context: Context): ShelfThemePreset {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetName = sp.getString(KEY_PRESET, "黑绿") ?: "黑绿"
        val primaryInt = sp.getInt(KEY_PRIMARY, Color(0xFF000000).toArgb())
        val secondaryInt = sp.getInt(KEY_SECONDARY, Color(0xFFC7E8B0).toArgb())
        return ShelfThemePreset(presetName, Color(primaryInt), Color(secondaryInt))
    }

    fun saveTheme(context: Context, theme: ShelfThemePreset) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString(KEY_PRESET, theme.name)
            .putInt(KEY_PRIMARY, theme.primary.toArgb())
            .putInt(KEY_SECONDARY, theme.secondary.toArgb())
            .apply()
    }
}
