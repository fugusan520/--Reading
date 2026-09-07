package com.example.util

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

object NovelParser {

    data class ChapterInfo(
        val index: Int,
        val title: String,
        val startCharIndex: Int,
        val endCharIndex: Int
    )

    data class ParsedNovel(
        val fullText: String,
        val chapters: List<ChapterInfo>
    )

    private val CHAPTER_PATTERN = Pattern.compile(
        "^[\\s　]*(?:第[0-9一二三四五六七八九十百千两]+[章回节卷集部篇话]|Chapter\\s+[0-9]+|序章|序言|前言|后记|尾声|楔子|番外|引子|终章)[^\\n\\r]*",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    )

    fun loadNovelText(context: Context, uriString: String, localPath: String?): String {
        return try {
            val bytes = if (!localPath.isNullOrEmpty()) {
                File(localPath).readBytes()
            } else {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { readAllBytes(it) } ?: ByteArray(0)
            }

            if (bytes.isEmpty()) return "无内容"

            val lowerPath = (localPath ?: uriString).lowercase()
            if (lowerPath.endsWith(".jar")) {
                parseJar(bytes)
            } else if (lowerPath.endsWith(".umd")) {
                parseUmd(bytes)
            } else {
                decodeTextWithCharsetDetection(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "读取文本失败: ${e.message}"
        }
    }

    private fun readAllBytes(inputStream: InputStream): ByteArray {
        val buffer = ByteArray(8192)
        val baos = ByteArrayOutputStream()
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            baos.write(buffer, 0, read)
        }
        return baos.toByteArray()
    }

    fun decodeTextWithCharsetDetection(bytes: ByteArray): String {
        // Check for BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }

        // Try strict UTF-8
        try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes))
            return String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // If UTF-8 decoding failed, try GB18030 / GBK which is standard for Chinese TXT novels
            try {
                val gbkCharset = Charset.forName("GB18030")
                return String(bytes, gbkCharset)
            } catch (_: Exception) {
                return String(bytes, StandardCharsets.UTF_8)
            }
        }
    }

    private fun parseJar(bytes: ByteArray): String {
        try {
            val zis = ZipInputStream(bytes.inputStream())
            var entry = zis.nextEntry
            val textBuilder = StringBuilder()
            while (entry != null) {
                if (!entry.isDirectory && (entry.name.endsWith(".txt", true) || entry.name.endsWith(".htm", true))) {
                    val entryBytes = readAllBytes(zis)
                    textBuilder.append(decodeTextWithCharsetDetection(entryBytes))
                    textBuilder.append("\n\n")
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            if (textBuilder.isNotEmpty()) return textBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return decodeTextWithCharsetDetection(bytes)
    }

    private fun parseUmd(bytes: ByteArray): String {
        // UMD (Universal Mobile Document) format has compressed blocks or plain text headers
        // Simple heuristic extraction of readable strings or decompressed blocks
        try {
            val text = decodeTextWithCharsetDetection(bytes)
            val filtered = text.filter { it == '\n' || it == '\r' || it == '\t' || it in '\u4e00'..'\u9fa5' || it in ' '..'~' }
            if (filtered.length > 50) return filtered
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return decodeTextWithCharsetDetection(bytes)
    }

    fun parseNovel(fullText: String): ParsedNovel {
        val chapters = mutableListOf<ChapterInfo>()
        val matcher = CHAPTER_PATTERN.matcher(fullText)
        while (matcher.find()) {
            val start = matcher.start()
            val title = matcher.group().trim()
            if (title.isNotEmpty() && title.length < 60) {
                chapters.add(
                    ChapterInfo(
                        index = 0,
                        title = title,
                        startCharIndex = start,
                        endCharIndex = fullText.length
                    )
                )
            }
        }

        // Sort chapters by starting position
        chapters.sortBy { it.startCharIndex }

        val finalizedChapters = mutableListOf<ChapterInfo>()
        if (chapters.isNotEmpty()) {
            for (i in chapters.indices) {
                val current = chapters[i]
                val end = if (i + 1 < chapters.size) chapters[i + 1].startCharIndex else fullText.length
                finalizedChapters.add(
                    current.copy(
                        index = i + 1,
                        endCharIndex = end
                    )
                )
            }
        } else {
            // Default single chapter
            finalizedChapters.add(
                ChapterInfo(
                    index = 1,
                    title = "正文",
                    startCharIndex = 0,
                    endCharIndex = fullText.length
                )
            )
        }

        return ParsedNovel(fullText, finalizedChapters)
    }

    /**
     * Splits full text or chapter text into pages with roughly [charsPerPage] characters.
     */
    fun paginateText(text: String, charsPerPage: Int = 650): List<String> {
        if (text.isEmpty()) return listOf("")
        val pages = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + charsPerPage).coerceAtMost(text.length)
            if (end < text.length) {
                // Try to find a paragraph break or sentence punctuation near the boundary
                val nextNewline = text.indexOf('\n', end - 50)
                if (nextNewline in (end - 50)..(end + 50) && nextNewline < text.length) {
                    end = nextNewline + 1
                }
            }
            pages.add(text.substring(start, end))
            start = end
        }
        return if (pages.isEmpty()) listOf(text) else pages
    }
}
