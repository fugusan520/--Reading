package com.example.ui.reader

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import com.example.data.model.BookmarkEntity
import com.example.data.model.ComicReaderConfig
import com.example.data.model.NovelColorPalettes
import com.example.data.model.NovelReaderConfig
import com.example.data.model.PageTurnMode
import com.example.data.repository.BookRepository
import com.example.data.repository.ReaderPreferences
import com.example.util.ImageProcessor
import com.example.util.NovelParser
import com.example.util.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReaderUiState(
    val book: BookEntity? = null,
    val isLoading: Boolean = true,
    val isMenuVisible: Boolean = false,
    val isChapterDrawerVisible: Boolean = false,
    val isSettingsDrawerVisible: Boolean = false,
    val isOptionsDrawerVisible: Boolean = false,
    // Novel specific
    val novelConfig: NovelReaderConfig = NovelReaderConfig(),
    val novelChapters: List<NovelParser.ChapterInfo> = emptyList(),
    val novelPages: List<String> = emptyList(),
    val novelPageOffsets: List<Int> = emptyList(),
    val novelParagraphs: List<String> = emptyList(),
    val novelParagraphOffsets: List<Int> = emptyList(),
    val currentNovelPageIndex: Int = 0,
    val currentChapterTitle: String = "",
    val currentScrollCharOffset: Long = 0L,
    // Comic specific
    val comicConfig: ComicReaderConfig = ComicReaderConfig(),
    val comicPageCount: Int = 0,
    val currentComicPageIndex: Int = 0,
    val comicImageFiles: List<File> = emptyList(),
    val isPdf: Boolean = false,
    // Bookmarks
    val bookmarks: List<BookmarkEntity> = emptyList()
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookRepository = BookRepository(AppDatabase.getInstance(application))
    val preferences: ReaderPreferences = ReaderPreferences(application)
    private var pdfManager: PdfManager? = null
    private var bookmarkJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(
        ReaderUiState(
            novelConfig = preferences.loadNovelConfig(),
            comicConfig = preferences.loadComicConfig()
        )
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var fullNovelRawText: String = ""

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                novelConfig = preferences.loadNovelConfig(),
                comicConfig = preferences.loadComicConfig()
            )
            val book = repository.getBookById(bookId)
            if (book == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            // Load saved reading progress from Room data layer
            val progress = repository.getReadingProgress(bookId)

            _uiState.value = _uiState.value.copy(book = book)

            if (book.bookType == BookType.NOVEL) {
                loadNovel(book, progress)
            } else {
                loadComic(book, progress)
            }

            // Load bookmarks in separate independent job (B1 Fix)
            bookmarkJob?.cancel()
            bookmarkJob = viewModelScope.launch {
                repository.getBookmarks(bookId).collect { bms ->
                    _uiState.value = _uiState.value.copy(bookmarks = bms)
                }
            }
        }
    }

    private suspend fun loadNovel(book: BookEntity, progress: com.example.data.model.ReadingProgressEntity?) = withContext(Dispatchers.IO) {
        val text = NovelParser.loadNovelText(getApplication(), book.uriString, book.localFilePath)
        fullNovelRawText = text
        val parsed = NovelParser.parseNovel(text)
        val paginated = NovelParser.paginateWithOffsets(text, charsPerPage = 650)
        val pages = paginated.pages
        val offsets = paginated.pageStartOffsets

        // Resume exactly where the user left off
        val targetPage = when {
            progress != null && progress.characterOffset > 0L -> {
                NovelParser.findPageForCharOffset(offsets, progress.characterOffset)
            }
            progress != null && progress.currentPage > 0 -> {
                (progress.currentPage - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            }
            book.lastReadPosition > 0L -> {
                NovelParser.findPageForCharOffset(offsets, book.lastReadPosition)
            }
            else -> {
                (book.currentPage - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            }
        }

        val currentChapter = findChapterForPage(targetPage, pages, parsed.chapters, offsets)

        // Split text into paragraphs for vertical scroll mode (Requirement 2.1)
        val rawParagraphs = text.split("\n")
        val paragraphs = ArrayList<String>(rawParagraphs.size)
        val paragraphOffsets = ArrayList<Int>(rawParagraphs.size)
        var runningOffset = 0
        for (para in rawParagraphs) {
            val trimmed = para.trimEnd('\r')
            paragraphs.add(trimmed)
            paragraphOffsets.add(runningOffset)
            runningOffset += para.length + 1
        }

        val initialCharOffset = when {
            progress != null && progress.characterOffset > 0L -> progress.characterOffset
            book.lastReadPosition > 0L -> book.lastReadPosition
            else -> offsets.getOrNull(targetPage)?.toLong() ?: 0L
        }

        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                novelChapters = parsed.chapters,
                novelPages = pages,
                novelPageOffsets = offsets,
                novelParagraphs = paragraphs,
                novelParagraphOffsets = paragraphOffsets,
                currentNovelPageIndex = targetPage,
                currentScrollCharOffset = initialCharOffset,
                currentChapterTitle = currentChapter?.title ?: (parsed.chapters.firstOrNull()?.title ?: "正文")
            )
        }
    }

    private suspend fun loadComic(book: BookEntity, progress: com.example.data.model.ReadingProgressEntity?) = withContext(Dispatchers.IO) {
        val isPdfFormat = book.fileFormat == "PDF" || book.uriString.endsWith(".pdf", true)

        if (isPdfFormat) {
            val pdf = PdfManager(getApplication())
            pdfManager = pdf
            val pages = pdf.open(book.uriString, book.localFilePath)
            val pageToResume = progress?.currentPage ?: book.currentPage
            val startPage = (pageToResume - 1).coerceIn(0, (pages - 1).coerceAtLeast(0))
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPdf = true,
                    comicPageCount = pages,
                    currentComicPageIndex = startPage
                )
            }
        } else {
            // Check files in unpacked directory or single file
            val imageFiles = mutableListOf<File>()
            val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

            if (!book.localFilePath.isNullOrEmpty()) {
                val dir = File(book.localFilePath)
                if (dir.exists()) {
                    val files = dir.walkTopDown()
                        .filter { it.isFile && it.extension.lowercase() in imageExtensions }
                        .sortedBy { it.name }
                        .toList()
                    imageFiles.addAll(files)
                }
            }

            val pageCount = if (imageFiles.isNotEmpty()) imageFiles.size else 1
            val pageToResume = progress?.currentPage ?: book.currentPage
            val startPage = (pageToResume - 1).coerceIn(0, (pageCount - 1).coerceAtLeast(0))

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPdf = false,
                    comicImageFiles = imageFiles,
                    comicPageCount = pageCount,
                    currentComicPageIndex = startPage
                )
            }
        }
    }

    private fun findChapterForPage(
        pageIndex: Int,
        pages: List<String>,
        chapters: List<NovelParser.ChapterInfo>,
        offsets: List<Int>
    ): NovelParser.ChapterInfo? {
        if (chapters.isEmpty()) return null
        val charPos = offsets.getOrNull(pageIndex) ?: (pageIndex * 650)
        return chapters.lastOrNull { it.startCharIndex <= charPos } ?: chapters.firstOrNull()
    }

    suspend fun renderPdfPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? {
        val pdf = pdfManager ?: return null
        val raw = pdf.renderPage(pageIndex, targetWidth) ?: return null
        val config = _uiState.value.comicConfig
        val processedList = ImageProcessor.processComicImage(
            raw,
            config.autoCropWhiteBorders,
            config.autoSplitDoublePage
        )
        return processedList.firstOrNull() ?: raw
    }

    fun loadComicBitmap(pageIndex: Int): Bitmap? {
        val state = _uiState.value
        if (pageIndex < 0 || pageIndex >= state.comicImageFiles.size) return null
        val file = state.comicImageFiles[pageIndex]
        return try {
            val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val config = state.comicConfig
            val processedList = ImageProcessor.processComicImage(
                original,
                config.autoCropWhiteBorders,
                config.autoSplitDoublePage
            )
            processedList.firstOrNull() ?: original
        } catch (_: Exception) {
            null
        }
    }

    fun toggleMenu() {
        _uiState.value = _uiState.value.copy(
            isMenuVisible = !_uiState.value.isMenuVisible,
            isChapterDrawerVisible = false,
            isSettingsDrawerVisible = false,
            isOptionsDrawerVisible = false
        )
    }

    fun hideMenu() {
        _uiState.value = _uiState.value.copy(
            isMenuVisible = false,
            isChapterDrawerVisible = false,
            isSettingsDrawerVisible = false,
            isOptionsDrawerVisible = false
        )
    }

    fun toggleChapterDrawer() {
        _uiState.value = _uiState.value.copy(
            isChapterDrawerVisible = !_uiState.value.isChapterDrawerVisible,
            isSettingsDrawerVisible = false,
            isOptionsDrawerVisible = false
        )
    }

    fun toggleSettingsDrawer() {
        _uiState.value = _uiState.value.copy(
            isSettingsDrawerVisible = !_uiState.value.isSettingsDrawerVisible,
            isChapterDrawerVisible = false,
            isOptionsDrawerVisible = false
        )
    }

    fun toggleOptionsDrawer() {
        _uiState.value = _uiState.value.copy(
            isOptionsDrawerVisible = !_uiState.value.isOptionsDrawerVisible,
            isChapterDrawerVisible = false,
            isSettingsDrawerVisible = false
        )
    }

    // Navigation for Novel
    fun nextNovelPage() {
        val current = _uiState.value.currentNovelPageIndex
        val max = _uiState.value.novelPages.size - 1
        if (current < max) {
            setNovelPage(current + 1)
        }
    }

    fun previousNovelPage() {
        val current = _uiState.value.currentNovelPageIndex
        if (current > 0) {
            setNovelPage(current - 1)
        }
    }

    fun setNovelPage(index: Int) {
        val state = _uiState.value
        val clamped = index.coerceIn(0, (state.novelPages.size - 1).coerceAtLeast(0))
        val currentChapter = findChapterForPage(clamped, state.novelPages, state.novelChapters, state.novelPageOffsets)
        val progress = if (state.novelPages.isNotEmpty()) ((clamped + 1).toFloat() / state.novelPages.size) else 0f
        val charOffset = state.novelPageOffsets.getOrNull(clamped)?.toLong() ?: (clamped * 650L)

        _uiState.value = state.copy(
            currentNovelPageIndex = clamped,
            currentScrollCharOffset = charOffset,
            currentChapterTitle = currentChapter?.title ?: state.currentChapterTitle
        )

        // Save progress to Room database layer
        state.book?.let { book ->
            viewModelScope.launch {
                repository.updateReadingProgress(
                    id = book.id,
                    bookType = BookType.NOVEL,
                    page = clamped + 1,
                    total = state.novelPages.size,
                    progress = progress,
                    position = charOffset,
                    chapterTitle = currentChapter?.title
                )
            }
        }
    }

    fun updateNovelScrollPosition(charOffset: Long) {
        val state = _uiState.value
        val book = state.book ?: return
        val currentChapter = state.novelChapters.lastOrNull { it.startCharIndex <= charOffset } ?: state.novelChapters.firstOrNull()
        val chapterTitle = currentChapter?.title ?: state.currentChapterTitle
        val totalLength = fullNovelRawText.length.coerceAtLeast(1)
        val progress = (charOffset.toFloat() / totalLength).coerceIn(0f, 1f)

        // Map char offset to roughly novel page index for consistent stats
        val targetPage = if (state.novelPageOffsets.isNotEmpty()) {
            NovelParser.findPageForCharOffset(state.novelPageOffsets, charOffset)
        } else 0

        _uiState.value = state.copy(
            currentScrollCharOffset = charOffset,
            currentNovelPageIndex = targetPage,
            currentChapterTitle = chapterTitle
        )

        viewModelScope.launch {
            repository.updateReadingProgress(
                id = book.id,
                bookType = BookType.NOVEL,
                page = targetPage + 1,
                total = state.novelPages.size.coerceAtLeast(1),
                progress = progress,
                position = charOffset,
                chapterTitle = chapterTitle
            )
        }
    }

    fun jumpToNovelChapter(chapter: NovelParser.ChapterInfo) {
        val targetPage = NovelParser.findPageForCharOffset(_uiState.value.novelPageOffsets, chapter.startCharIndex.toLong())
        setNovelPage(targetPage)
        _uiState.value = _uiState.value.copy(
            currentScrollCharOffset = chapter.startCharIndex.toLong(),
            isChapterDrawerVisible = false,
            isMenuVisible = false
        )
    }

    // Navigation for Comic
    fun nextComicPage() {
        val current = _uiState.value.currentComicPageIndex
        val max = _uiState.value.comicPageCount - 1
        if (current < max) {
            setComicPage(current + 1)
        }
    }

    fun previousComicPage() {
        val current = _uiState.value.currentComicPageIndex
        if (current > 0) {
            setComicPage(current - 1)
        }
    }

    fun setComicPage(index: Int) {
        val state = _uiState.value
        val clamped = index.coerceIn(0, (state.comicPageCount - 1).coerceAtLeast(0))
        val progress = if (state.comicPageCount > 0) ((clamped + 1).toFloat() / state.comicPageCount) else 0f

        _uiState.value = state.copy(currentComicPageIndex = clamped)

        // Save progress to Room database layer
        state.book?.let { book ->
            viewModelScope.launch {
                repository.updateReadingProgress(
                    id = book.id,
                    bookType = BookType.COMIC,
                    page = clamped + 1,
                    total = state.comicPageCount,
                    progress = progress,
                    position = clamped.toLong(),
                    chapterTitle = "第 ${clamped + 1} 页"
                )
            }
        }
    }

    /**
     * Explicitly flushes and saves the current reading progress to Room.
     * Called when exiting reader, disposing view, or switching screens.
     */
    fun saveCurrentProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            if (book.bookType == BookType.NOVEL) {
                val clamped = state.currentNovelPageIndex.coerceIn(0, (state.novelPages.size - 1).coerceAtLeast(0))
                val charOffset = if (state.novelConfig.pageTurnMode == PageTurnMode.VERTICAL_SCROLL) {
                    state.currentScrollCharOffset
                } else {
                    state.novelPageOffsets.getOrNull(clamped)?.toLong() ?: (clamped * 650L)
                }
                val totalLength = fullNovelRawText.length.coerceAtLeast(1)
                val progress = if (state.novelConfig.pageTurnMode == PageTurnMode.VERTICAL_SCROLL) {
                    (charOffset.toFloat() / totalLength).coerceIn(0f, 1f)
                } else if (state.novelPages.isNotEmpty()) {
                    ((clamped + 1).toFloat() / state.novelPages.size)
                } else 0f

                repository.updateReadingProgress(
                    id = book.id,
                    bookType = BookType.NOVEL,
                    page = clamped + 1,
                    total = state.novelPages.size,
                    progress = progress,
                    position = charOffset,
                    chapterTitle = state.currentChapterTitle
                )
            } else {
                val clamped = state.currentComicPageIndex.coerceIn(0, (state.comicPageCount - 1).coerceAtLeast(0))
                val progress = if (state.comicPageCount > 0) ((clamped + 1).toFloat() / state.comicPageCount) else 0f
                repository.updateReadingProgress(
                    id = book.id,
                    bookType = BookType.COMIC,
                    page = clamped + 1,
                    total = state.comicPageCount,
                    progress = progress,
                    position = clamped.toLong(),
                    chapterTitle = "第 ${clamped + 1} 页"
                )
            }
        }
    }

    // Novel visual controls
    fun updateNovelConfig(block: (NovelReaderConfig) -> NovelReaderConfig) {
        val updated = block(_uiState.value.novelConfig)
        _uiState.value = _uiState.value.copy(novelConfig = updated)
        preferences.saveNovelConfig(updated)
    }

    fun updateNovelBrightness(brightness: Float) {
        val current = _uiState.value.novelConfig
        val updated = if (current.isNightMode) {
            current.copy(nightBrightness = brightness.coerceIn(0.01f, 0.20f))
        } else {
            current.copy(normalBrightness = brightness.coerceIn(0.01f, 1.0f))
        }
        _uiState.value = _uiState.value.copy(novelConfig = updated)
        preferences.saveNovelConfig(updated)
    }

    fun toggleNovelNightMode() {
        val current = _uiState.value.novelConfig
        val updated = current.copy(isNightMode = !current.isNightMode)
        _uiState.value = _uiState.value.copy(novelConfig = updated)
        preferences.saveNovelConfig(updated)
    }

    fun setNovelPageTurnMode(mode: PageTurnMode) {
        val updated = _uiState.value.novelConfig.copy(pageTurnMode = mode)
        _uiState.value = _uiState.value.copy(novelConfig = updated)
        preferences.saveNovelConfig(updated)
    }

    // Comic visual controls
    fun updateComicConfig(block: (ComicReaderConfig) -> ComicReaderConfig) {
        val updated = block(_uiState.value.comicConfig)
        _uiState.value = _uiState.value.copy(comicConfig = updated)
        preferences.saveComicConfig(updated)
    }

    fun updateComicBrightness(brightness: Float) {
        val current = _uiState.value.comicConfig
        val updated = if (current.isNightMode) {
            current.copy(nightBrightness = brightness.coerceIn(0.01f, 0.20f))
        } else {
            current.copy(normalBrightness = brightness.coerceIn(0.01f, 1.0f))
        }
        _uiState.value = _uiState.value.copy(comicConfig = updated)
        preferences.saveComicConfig(updated)
    }

    fun toggleComicNightMode() {
        val current = _uiState.value.comicConfig
        val updated = current.copy(isNightMode = !current.isNightMode)
        _uiState.value = _uiState.value.copy(comicConfig = updated)
        preferences.saveComicConfig(updated)
    }

    fun setComicPageTurnMode(mode: PageTurnMode) {
        val updated = _uiState.value.comicConfig.copy(pageTurnMode = mode)
        _uiState.value = _uiState.value.copy(comicConfig = updated)
        preferences.saveComicConfig(updated)
    }

    fun updateComicZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.5f, 5.0f)
        val updated = _uiState.value.comicConfig.copy(zoomScale = clamped)
        _uiState.value = _uiState.value.copy(comicConfig = updated)
        preferences.saveComicConfig(updated)
    }

    // Bookmarks (B1 Fix)
    fun addBookmark() = toggleBookmark()

    fun toggleBookmark() {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            if (book.bookType == BookType.NOVEL) {
                val page = state.currentNovelPageIndex
                val existing = state.bookmarks.find { it.pageIndex == page }
                if (existing != null) {
                    repository.deleteBookmark(existing.id)
                } else {
                    val pageText = state.novelPages.getOrNull(page) ?: ""
                    val snippet = pageText.take(50).replace("\n", " ")
                    val offset = state.novelPageOffsets.getOrNull(page)?.toLong() ?: (page * 650L)
                    repository.addBookmark(
                        BookmarkEntity(
                            bookId = book.id,
                            chapterTitle = state.currentChapterTitle.ifBlank { "第 ${page + 1} 页" },
                            characterOffset = offset,
                            pageIndex = page,
                            snippet = snippet.ifBlank { "书签 第 ${page + 1} 页" }
                        )
                    )
                }
            } else {
                val page = state.currentComicPageIndex
                val existing = state.bookmarks.find { it.pageIndex == page }
                if (existing != null) {
                    repository.deleteBookmark(existing.id)
                } else {
                    repository.addBookmark(
                        BookmarkEntity(
                            bookId = book.id,
                            chapterTitle = "第 ${page + 1} 页",
                            characterOffset = page.toLong(),
                            pageIndex = page,
                            snippet = "漫画书签 第 ${page + 1} 页"
                        )
                    )
                }
            }
        }
    }

    fun jumpToBookmark(bookmark: BookmarkEntity) {
        val state = _uiState.value
        val book = state.book ?: return
        if (book.bookType == BookType.NOVEL) {
            val targetPage = if (state.novelPageOffsets.isNotEmpty()) {
                NovelParser.findPageForCharOffset(state.novelPageOffsets, bookmark.characterOffset)
            } else {
                bookmark.pageIndex
            }
            setNovelPage(targetPage)
            _uiState.value = _uiState.value.copy(
                currentScrollCharOffset = bookmark.characterOffset,
                isChapterDrawerVisible = false,
                isMenuVisible = false
            )
        } else {
            setComicPage(bookmark.pageIndex)
            _uiState.value = _uiState.value.copy(
                isChapterDrawerVisible = false,
                isMenuVisible = false
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfManager?.close()
        pdfManager = null
        bookmarkJob?.cancel()
    }
}
