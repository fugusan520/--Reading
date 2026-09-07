package com.example.util

import android.graphics.Bitmap

object ImageProcessor {

    /**
     * Algorithm 1: Auto crop white borders.
     * Scans border pixels from top, bottom, left, and right.
     * If row/col pixels have luminance > 240 and exceed 10% of dimension, crop it.
     */
    fun autoCropWhiteBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 20 || height <= 20) return bitmap

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        val maxTopBottomCheck = (height * 0.25f).toInt()
        val maxLeftRightCheck = (width * 0.25f).toInt()

        // Scan from top
        for (y in 0 until maxTopBottomCheck) {
            if (isRowWhite(bitmap, y, width)) {
                top = y
            } else {
                break
            }
        }

        // Scan from bottom
        for (y in (height - 1) downTo (height - maxTopBottomCheck)) {
            if (isRowWhite(bitmap, y, width)) {
                bottom = y
            } else {
                break
            }
        }

        // Scan from left
        for (x in 0 until maxLeftRightCheck) {
            if (isColWhite(bitmap, x, top, bottom)) {
                left = x
            } else {
                break
            }
        }

        // Scan from right
        for (x in (width - 1) downTo (width - maxLeftRightCheck)) {
            if (isColWhite(bitmap, x, top, bottom)) {
                right = x
            } else {
                break
            }
        }

        val cropW = right - left + 1
        val cropH = bottom - top + 1

        // Only crop if border cut is meaningful (> 10% on at least one edge)
        val cutW = width - cropW
        val cutH = height - cropH
        if ((cutW > width * 0.05f || cutH > height * 0.05f) && cropW > 20 && cropH > 20) {
            return try {
                Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
            } catch (e: Exception) {
                bitmap
            }
        }

        return bitmap
    }

    private fun isRowWhite(bitmap: Bitmap, y: Int, width: Int): Boolean {
        var whiteCount = 0
        val step = (width / 50).coerceAtLeast(1)
        var sampled = 0
        for (x in 0 until width step step) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            if (luminance > 240) {
                whiteCount++
            }
            sampled++
        }
        return (whiteCount.toFloat() / sampled) > 0.90f
    }

    private fun isColWhite(bitmap: Bitmap, x: Int, startY: Int, endY: Int): Boolean {
        var whiteCount = 0
        val height = endY - startY + 1
        val step = (height / 50).coerceAtLeast(1)
        var sampled = 0
        for (y in startY..endY step step) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            if (luminance > 240) {
                whiteCount++
            }
            sampled++
        }
        return (whiteCount.toFloat() / sampled) > 0.90f
    }

    /**
     * Algorithm 2: Auto split double page.
     * When image width > height * 1.3, split down the middle axis into two pages.
     */
    fun splitDoublePage(bitmap: Bitmap): List<Bitmap> {
        val width = bitmap.width
        val height = bitmap.height
        if (width > height * 1.3f) {
            val halfW = width / 2
            return try {
                val left = Bitmap.createBitmap(bitmap, 0, 0, halfW, height)
                val right = Bitmap.createBitmap(bitmap, halfW, 0, width - halfW, height)
                listOf(left, right)
            } catch (e: Exception) {
                listOf(bitmap)
            }
        }
        return listOf(bitmap)
    }

    /**
     * Combined processing based on user settings.
     */
    fun processComicImage(
        original: Bitmap,
        autoCropBorders: Boolean,
        autoSplitDouble: Boolean
    ): List<Bitmap> {
        val cropped = if (autoCropBorders) autoCropWhiteBorders(original) else original
        return if (autoSplitDouble) {
            splitDoublePage(cropped)
        } else {
            listOf(cropped)
        }
    }
}
