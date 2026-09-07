package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SystemUiHelper {

    private var focusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null

    @Suppress("DEPRECATION")
    fun enterImmersiveSticky(activity: Activity) {
        val window = activity.window
        val decorView = window.decorView

        // 1. Ensure android:fitsSystemWindows="false" on the root layout & decorView
        WindowCompat.setDecorFitsSystemWindows(window, false)
        decorView.fitsSystemWindows = false
        activity.findViewById<View>(android.R.id.content)?.fitsSystemWindows = false

        // 2. Extend into display cutout area (notch/camera hole) without blank margins
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val layoutParams = window.attributes
            layoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = layoutParams
        }

        // 3. Flags: SYSTEM_UI_FLAG_IMMERSIVE_STICKY, SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN, etc.
        val flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        decorView.systemUiVisibility = flags

        // 4. Ensure flags are maintained even if user interactions or notifications trigger visibility changes
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decorView.systemUiVisibility = flags
            }
        }

        // 5. Keep immersive sticky on window focus changes
        if (focusListener != null) {
            decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
        focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                decorView.systemUiVisibility = flags
                WindowCompat.getInsetsController(window, decorView).let { controller ->
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

        // 6. WindowInsetsControllerCompat API
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    @Suppress("DEPRECATION")
    fun exitImmersive(activity: Activity) {
        val window = activity.window
        val decorView = window.decorView

        if (focusListener != null) {
            decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            focusListener = null
        }
        decorView.setOnSystemUiVisibilityChangeListener(null)
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun applyScreenBrightness(activity: Activity, brightnessPercent: Float, isNightMode: Boolean) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = if (isNightMode) {
            brightnessPercent.coerceIn(0.01f, 0.20f) // 1% ~ 20% range for dark/low-light mode
        } else {
            brightnessPercent.coerceIn(0.01f, 1.0f) // 1% ~ 100% range for normal mode
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
