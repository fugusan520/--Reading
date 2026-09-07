package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SystemUiHelper {

    fun enterImmersiveSticky(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun exitImmersive(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun applyScreenBrightness(activity: Activity, brightnessPercent: Float, isNightMode: Boolean) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = if (isNightMode) {
            0.03f // 3% for extreme dark AMOLED mode
        } else {
            brightnessPercent.coerceIn(0.01f, 1.0f)
        }
        activity.window.attributes = layoutParams
    }

    fun restoreScreenBrightness(activity: Activity) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = layoutParams
    }

    fun getBatteryPercentage(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: run {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) (level * 100) / scale else 80
            }
        } catch (_: Exception) {
            80
        }
    }

    fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
