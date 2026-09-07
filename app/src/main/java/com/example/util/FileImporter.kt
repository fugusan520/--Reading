package com.example.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileImporter {

    private val NOVEL_PRESET_COLORS = listOf(
        "#2E3842", "#1F3A4B", "#353D2F", "#4A3B32", "#3B2E42", "#2B3D41"
    )

    suspend fun importUris(
        context: Context,
        uris: List<Uri>,
        targetFolderId: Long?
    ): List<BookEntity> = withContext(Dispatchers.IO) {
        val importedBooks = mutableListOf<BookEntity>()

        for (uri in uris) {
            try {
                // Try to take persistable URI permission
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: Exception) {
                    // Ignore if not supported by provider
                }

                val displayName = queryFileName(context, uri) ?: "未命名书籍_${System.currentTimeMillis()}"
                val lowerName = displayName.lowercase()
                val mimeType = context.contentResolver.getType(uri) ?: ""

                // Check if it's an archive
                if (lowerName.endsWith(".zip") || lowerName.endsWith(".7z") || lowerName.endsWith(".rar") ||
                    lowerName.endsWith(".cbz") || lowerName.endsWith(".cbr")
                ) {
                    val result = ArchiveExtractor.extractArchive(context, uri, displayName)
                    val bookType = if (result.isComic) BookType.COMIC else BookType.NOVEL
                    var coverPath: String? = null

                    if (result.isComic) {
                        // Find first image for cover
                        val firstImg = result.chapters.firstOrNull()?.files?.firstOrNull { it.isFile }
                        coverPath = firstImg?.absolutePath
                    }

                    val totalCount = if (result.isComic) {
                        result.chapters.sumOf { it.files.size }.coerceAtLeast(1)
                    } else {
                        result.chapters.size.coerceAtLeast(1)
                    }

                    importedBooks.add(
                        BookEntity(
                            title = result.detectedTitle,
                            uriString = uri.toString(),
                            localFilePath = result.rootDir.absolutePath,
                            bookType = bookType,
                            folderId = targetFolderId,
                            coverColorHex = NOVEL_PRESET_COLORS.random(),
                            coverImagePath = coverPath,
                            fileFormat = displayName.substringAfterLast('.', "ZIP").uppercase(),
                            isArchiveExtracted = true,
                            totalPages = totalCount
                        )
                    )
                } else if (lowerName.endsWith(".pdf") || mimeType == "application/pdf") {
                    // Comic PDF
                    // Generate cover thumbnail
                    val pdfMgr = PdfManager(context)
                    val pageCount = pdfMgr.open(uri.toString(), null)
                    var coverPath: String? = null
                    val thumb = pdfMgr.renderPage(0, 360)
                    if (thumb != null) {
                        coverPath = saveCoverBitmap(context, thumb, displayName)
                    }
                    pdfMgr.close()

                    importedBooks.add(
                        BookEntity(
                            title = displayName.substringBeforeLast('.'),
                            uriString = uri.toString(),
                            localFilePath = null,
                            bookType = BookType.COMIC,
                            folderId = targetFolderId,
                            coverImagePath = coverPath,
                            fileFormat = "PDF",
                            totalPages = pageCount.coerceAtLeast(1)
                        )
                    )
                } else if (
                    lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".webp") ||
                    lowerName.endsWith(".bmp") || mimeType.startsWith("image/")
                ) {
                    // Single comic image
                    importedBooks.add(
                        BookEntity(
                            title = displayName.substringBeforeLast('.'),
                            uriString = uri.toString(),
                            localFilePath = null,
                            bookType = BookType.COMIC,
                            folderId = targetFolderId,
                            coverImagePath = uri.toString(),
                            fileFormat = displayName.substringAfterLast('.', "IMG").uppercase(),
                            totalPages = 1
                        )
                    )
                } else {
                    // Default to Novel (TXT / UMD / JAR or text/plain)
                    importedBooks.add(
                        BookEntity(
                            title = displayName.substringBeforeLast('.'),
                            uriString = uri.toString(),
                            localFilePath = null,
                            bookType = BookType.NOVEL,
                            folderId = targetFolderId,
                            coverColorHex = NOVEL_PRESET_COLORS.random(),
                            fileFormat = displayName.substringAfterLast('.', "TXT").uppercase(),
                            totalPages = 1
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext importedBooks
    }

    private fun saveCoverBitmap(context: Context, bitmap: Bitmap, name: String): String? {
        return try {
            val coverDir = File(context.filesDir, "covers")
            if (!coverDir.exists()) coverDir.mkdirs()
            val coverFile = File(coverDir, "cover_${System.currentTimeMillis()}.png")
            FileOutputStream(coverFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        return cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
