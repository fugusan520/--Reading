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
    // Novel specific
    val novelConfig: NovelReaderConfig = NovelReaderConfig(),
    val novelChapters: List<NovelParser.ChapterInfo> = emptyList(),
    val novelPages: List<String> = emptyList(),
    val currentNovelPageIndex: Int = 0,
    val currentChapterTitle: String = "",
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
    private var pdfManager: PdfManager? = null

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var fullNovelRawText: String = ""

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val book = repository.getBookById(bookId)
            if (book == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(book = book)

            if (book.bookType == BookType.NOVEL) {
                loadNovel(book)
            } else {
                loadComic(book)
            }

            // Load bookmarks
            repository.getBookmarks(bookId).collect { bms ->
                _uiState.value = _uiState.value.copy(bookmarks = bms)
            }
        }
    }

    private suspend fun loadNovel(book: BookEntity) = withContext(Dispatchers.IO) {
        val text = NovelParser.loadNovelText(getApplication(), book.uriString, book.localFilePath)
        fullNovelRawText = text
        val parsed = NovelParser.parseNovel(text)
        val pages = NovelParser.paginateText(text, charsPerPage = 650)

        val targetPage = (book.currentPage - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val currentChapter = findChapterForPage(targetPage, pages, parsed.chapters)

        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                novelChapters = parsed.chapters,
                novelPages = pages,
                currentNovelPageIndex = targetPage,
                currentChapterTitle = currentChapter?.title ?: (parsed.chapters.firstOrNull()?.title ?: "正文")
            )
        }
    }

    private suspend fun loadComic(book: BookEntity) = withContext(Dispatchers.IO) {
        val isPdfFormat = book.fileFormat == "PDF" || book.uriString.endsWith(".pdf", true)

        if (isPdfFormat) {
            val pdf = PdfManager(getApplication())
            pdfManager = pdf
            val pages = pdf.open(book.uriString, book.localFilePath)
            val startPage = (book.currentPage - 1).coerceIn(0, (pages - 1).coerceAtLeast(0))
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
            val startPage = (book.currentPage - 1).coerceIn(0, (pageCount - 1).coerceAtLeast(0))

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
        chapters: List<NovelParser.ChapterInfo>
    ): NovelParser.ChapterInfo? {
        if (chapters.isEmpty()) return null
        val approxCharPos = pageIndex * 650
        return chapters.lastOrNull { it.startCharIndex <= approxCharPos } ?: chapters.firstOrNull()
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
            isSettingsDrawerVisible = false
        )
    }

    fun hideMenu() {
        _uiState.value = _uiState.value.copy(
            isMenuVisible = false,
            isChapterDrawerVisible = false,
            isSettingsDrawerVisible = false
        )
    }

    fun toggleChapterDrawer() {
        _uiState.value = _uiState.value.copy(
            isChapterDrawerVisible = !_uiState.value.isChapterDrawerVisible,
            isSettingsDrawerVisible = false
        )
    }

    fun toggleSettingsDrawer() {
        _uiState.value = _uiState.value.copy(
            isSettingsDrawerVisible = !_uiState.value.isSettingsDrawerVisible,
            isChapterDrawerVisible = false
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
        val currentChapter = findChapterForPage(clamped, state.novelPages, state.novelChapters)
        val progress = if (state.novelPages.isNotEmpty()) (clamped.toFloat() / state.novelPages.size) else 0f

        _uiState.value = state.copy(
            currentNovelPageIndex = clamped,
            currentChapterTitle = currentChapter?.title ?: state.currentChapterTitle
        )

        // Save progress to DB
        state.book?.let { book ->
            viewModelScope.launch {
                repository.updateReadingProgress(
                    id = book.id,
                    page = clamped + 1,
                    total = state.novelPages.size,
                    progress = progress,
                    position = (clamped * 650).toLong(),
                    chapterTitle = currentChapter?.title
                )
            }
        }
    }

    fun jumpToNovelChapter(chapter: NovelParser.ChapterInfo) {
        val approxPage = (chapter.startCharIndex / 650).coerceIn(0, (_uiState.value.novelPages.size - 1).coerceAtLeast(0))
        setNovelPage(approxPage)
        _uiState.value = _uiState.value.copy(isChapterDrawerVisible = false, isMenuVisible = false)
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
        val progress = if (state.comicPageCount > 0) (clamped.toFloat() / state.comicPageCount) else 0f

        _uiState.value = state.copy(currentComicPageIndex = clamped)

        state.book?.let { book ->
            viewModelScope.launch {
                repository.updateReadingProgress(
                    id = book.id,
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
        _uiState.value = _uiState.value.copy(novelConfig = block(_uiState.value.novelConfig))
    }

    fun toggleNovelNightMode() {
        val current = _uiState.value.novelConfig
        _uiState.value = _uiState.value.copy(
            novelConfig = current.copy(isNightMode = !current.isNightMode)
        )
    }

    fun setNovelPageTurnMode(mode: PageTurnMode) {
        _uiState.value = _uiState.value.copy(
            novelConfig = _uiState.value.novelConfig.copy(pageTurnMode = mode)
        )
    }

    // Comic visual controls
    fun updateComicConfig(block: (ComicReaderConfig) -> ComicReaderConfig) {
        _uiState.value = _uiState.value.copy(comicConfig = block(_uiState.value.comicConfig))
    }

    fun toggleComicNightMode() {
        val current = _uiState.value.comicConfig
        _uiState.value = _uiState.value.copy(
            comicConfig = current.copy(isNightMode = !current.isNightMode)
        )
    }

    fun setComicPageTurnMode(mode: PageTurnMode) {
        _uiState.value = _uiState.value.copy(
            comicConfig = _uiState.value.comicConfig.copy(pageTurnMode = mode)
        )
    }

    fun updateComicZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.5f, 5.0f)
        _uiState.value = _uiState.value.copy(
            comicConfig = _uiState.value.comicConfig.copy(zoomScale = clamped)
        )
    }

    // Bookmarks
    fun addBookmark() {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            if (book.bookType == BookType.NOVEL) {
                val pageText = state.novelPages.getOrNull(state.currentNovelPageIndex) ?: ""
                val snippet = pageText.take(40).replace("\n", " ")
                repository.addBookmark(
                    BookmarkEntity(
                        bookId = book.id,
                        chapterTitle = state.currentChapterTitle,
                        characterOffset = (state.currentNovelPageIndex * 650).toLong(),
                        pageIndex = state.currentNovelPageIndex,
                        snippet = snippet
                    )
                )
            } else {
                repository.addBookmark(
                    BookmarkEntity(
                        bookId = book.id,
                        chapterTitle = "第 ${state.currentComicPageIndex + 1} 页",
                        characterOffset = state.currentComicPageIndex.toLong(),
                        pageIndex = state.currentComicPageIndex,
                        snippet = "漫画书签 第 ${state.currentComicPageIndex + 1} 页"
                    )
                )
            }
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
    }
}
