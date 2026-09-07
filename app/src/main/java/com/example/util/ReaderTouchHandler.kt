package com.example.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

enum class ReaderClickZone {
    LEFT_PREV,
    CENTER_MENU,
    RIGHT_NEXT,
    EDGE_SAFE_ZONE
}

fun Modifier.readerClickZones(
    onPrevious: () -> Unit,
    onToggleMenu: () -> Unit,
    onNext: () -> Unit
): Modifier = this.pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startX = down.position.x
        val startY = down.position.y
        var totalDistance = 0f
        var lastX = startX
        var lastY = startY

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val currentChange = event.changes.find { it.id == down.id }

            if (currentChange == null || !currentChange.pressed) {
                // Pointer up!
                if (totalDistance < touchSlop) {
                    val width = size.width.toFloat()
                    if (width > 0f) {
                        val xRatio = lastX / width
                        when {
                            // 0% ~ 5%: Safe zone for system back swipe gesture
                            xRatio < 0.05f -> {}
                            // 5% ~ 30%: Previous page
                            xRatio < 0.30f -> onPrevious()
                            // 30% ~ 70%: Menu popup
                            xRatio < 0.70f -> onToggleMenu()
                            // 70% ~ 95%: Next page
                            xRatio < 0.95f -> onNext()
                            // 95% ~ 100%: Safe zone for system gesture
                            else -> {}
                        }
                    }
                }
                break
            } else {
                val dx = currentChange.position.x - lastX
                val dy = currentChange.position.y - lastY
                totalDistance += hypot(dx, dy)
                lastX = currentChange.position.x
                lastY = currentChange.position.y
            }
        }
    }
}
