package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfManager(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempPdfFile: File? = null

    // Cache up to 16 rendered pages in memory
    private val bitmapCache = object : LruCache<Int, Bitmap>(16) {}

    suspend fun open(uriString: String, localPath: String?): Int = withContext(Dispatchers.IO) {
        close()
        try {
            val pfd = if (!localPath.isNullOrEmpty()) {
                val file = File(localPath)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                val uri = Uri.parse(uriString)
                context.contentResolver.openFileDescriptor(uri, "r")
                    ?: copyToTempAndOpen(uri)
            }
            fileDescriptor = pfd
            if (pfd != null) {
                pdfRenderer = PdfRenderer(pfd)
                return@withContext pdfRenderer?.pageCount ?: 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 0
    }

    private fun copyToTempAndOpen(uri: Uri): ParcelFileDescriptor? {
        val temp = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        tempPdfFile = temp
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        return ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    fun getPageCount(): Int = pdfRenderer?.pageCount ?: 0

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= getPageCount()) return@withContext null

        synchronized(bitmapCache) {
            bitmapCache.get(pageIndex)?.let { return@withContext it }
        }

        val renderer = pdfRenderer ?: return@withContext null
        try {
            synchronized(renderer) {
                renderer.openPage(pageIndex).use { page ->
                    val aspect = page.height.toFloat() / page.width.toFloat()
                    val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(100)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    // Canvas white background before rendering PDF page
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    synchronized(bitmapCache) {
                        bitmapCache.put(pageIndex, bitmap)
                    }
                    return@withContext bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun close() {
        try {
            synchronized(bitmapCache) {
                bitmapCache.evictAll()
            }
            pdfRenderer?.close()
            pdfRenderer = null
            fileDescriptor?.close()
            fileDescriptor = null
            tempPdfFile?.delete()
            tempPdfFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
