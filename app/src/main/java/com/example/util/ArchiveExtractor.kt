package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ArchiveExtractor {

    data class ExtractedResult(
        val rootDir: File,
        val detectedTitle: String,
        val isComic: Boolean,
        val chapters: List<ExtractedChapter>
    )

    data class ExtractedChapter(
        val title: String,
        val files: List<File>,
        val isImageChapter: Boolean
    )

    fun extractArchive(context: Context, uri: Uri, fileName: String): ExtractedResult {
        val unpackBaseDir = File(context.cacheDir, "unpacked")
        if (!unpackBaseDir.exists()) unpackBaseDir.mkdirs()

        val safeName = fileName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
        val destDir = File(unpackBaseDir, "${safeName}_${System.currentTimeMillis()}")
        destDir.mkdirs()

        val lowerName = fileName.lowercase()
        if (lowerName.endsWith(".7z")) {
            extract7z(context, uri, destDir)
        } else {
            // Default ZIP / CBZ / EPUB / JAR extractor
            extractZip(context, uri, destDir)
        }

        return analyzeDirectory(destDir, safeName)
    }

    private fun extractZip(context: Context, uri: Uri, destDir: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val entryName = entry.name
                    // Prevent path traversal
                    if (!entryName.contains("..")) {
                        val file = File(destDir, entryName)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun extract7z(context: Context, uri: Uri, destDir: File) {
        // Copy 7z temp file because SevenZFile requires a SeekableByteChannel / File
        val tempFile = File(destDir, "temp_archive.7z")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            val sevenZFile = SevenZFile(tempFile)
            val buffer = ByteArray(8192)
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                if (!entry.name.contains("..")) {
                    val file = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            var count: Int
                            while (sevenZFile.read(buffer).also { count = it } > 0) {
                                fos.write(buffer, 0, count)
                            }
                        }
                    }
                }
                entry = sevenZFile.nextEntry
            }
            sevenZFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun analyzeDirectory(dir: File, fallbackTitle: String): ExtractedResult {
        val allFiles = dir.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.toList()
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")
        val textExtensions = setOf("txt", "md", "html", "htm")

        val imageFiles = allFiles.filter { it.extension.lowercase() in imageExtensions }
        val textFiles = allFiles.filter { it.extension.lowercase() in textExtensions }

        // Mixed folder handling (Requirement 4): If images exist, prioritize as Comic
        val isComic = imageFiles.isNotEmpty() || allFiles.any { it.extension.lowercase() == "pdf" }

        val chapters = mutableListOf<ExtractedChapter>()

        // Check if there are subdirectories inside
        val subDirs = dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList()

        if (subDirs.isNotEmpty()) {
            for (sub in subDirs) {
                val subFiles = sub.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.sortedBy { it.name }.toList()
                val chapterFiles = if (isComic) {
                    subFiles.filter { it.extension.lowercase() in imageExtensions }
                } else {
                    subFiles.filter { it.extension.lowercase() in textExtensions }
                }
                if (chapterFiles.isNotEmpty()) {
                    chapters.add(
                        ExtractedChapter(
                            title = sub.name,
                            files = chapterFiles,
                            isImageChapter = isComic
                        )
                    )
                }
            }
        }

        // If no subdirs had chapters, take the root files as a single chapter
        if (chapters.isEmpty()) {
            val validFiles = if (isComic) {
                imageFiles.sortedBy { it.name }
            } else {
                textFiles.sortedBy { it.name }
            }
            chapters.add(
                ExtractedChapter(
                    title = fallbackTitle,
                    files = validFiles,
                    isImageChapter = isComic
                )
            )
        }

        return ExtractedResult(
            rootDir = dir,
            detectedTitle = fallbackTitle,
            isComic = isComic,
            chapters = chapters
        )
    }
}
