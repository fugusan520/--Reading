package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.BookType
import com.example.data.model.FolderEntity
import com.example.data.model.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

class BookRepository(private val db: AppDatabase) {
    private val bookDao = db.bookDao()
    private val folderDao = db.folderDao()
    private val bookmarkDao = db.bookmarkDao()
    private val readingProgressDao = db.readingProgressDao()

    fun getBooks(bookType: BookType, folderId: Long?): Flow<List<BookEntity>> =
        bookDao.getBooks(bookType, folderId)

    fun getFolders(bookType: BookType, parentFolderId: Long?): Flow<List<FolderEntity>> =
        folderDao.getFolders(bookType, parentFolderId)

    fun getBookFlow(id: Long): Flow<BookEntity?> = bookDao.getBookFlow(id)

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun setCustomTitle(bookId: Long, customTitle: String?) =
        bookDao.setCustomTitle(bookId, customTitle?.trim()?.takeIf { it.isNotEmpty() })

    suspend fun deleteBook(id: Long) = bookDao.deleteBookById(id)

    suspend fun togglePin(id: Long, currentPin: Boolean) =
        bookDao.setPinned(id, !currentPin)

    suspend fun moveToFolder(bookId: Long, folderId: Long?) =
        bookDao.moveToFolder(bookId, folderId)

    suspend fun createFolder(name: String, type: BookType, parentFolderId: Long?): Long {
        val folder = FolderEntity(
            name = name,
            parentFolderId = parentFolderId,
            bookType = type
        )
        return folderDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folderId: Long, parentFolderId: Long?) {
        // As required: move contained books back to parent folder (or root if parent is null)
        bookDao.resetBooksFolder(folderId, parentFolderId)
        // Also move any child folders back
        val childFolders = folderDao.getChildFolders(folderId)
        for (child in childFolders) {
            folderDao.deleteFolder(child)
            bookDao.resetBooksFolder(child.id, parentFolderId)
        }
        folderDao.deleteFolderById(folderId)
    }

    fun getReadingProgressFlow(bookId: Long): Flow<ReadingProgressEntity?> =
        readingProgressDao.getProgressFlow(bookId)

    suspend fun getReadingProgress(bookId: Long): ReadingProgressEntity? =
        readingProgressDao.getProgressForBook(bookId)

    suspend fun updateReadingProgress(
        id: Long,
        bookType: BookType,
        page: Int,
        total: Int,
        progress: Float,
        position: Long,
        chapterTitle: String?
    ) {
        val now = System.currentTimeMillis()
        val progressEntity = ReadingProgressEntity(
            bookId = id,
            bookType = bookType,
            currentPage = page,
            totalPages = total,
            progressPercent = progress,
            characterOffset = position,
            chapterTitle = chapterTitle,
            lastReadTimestamp = now
        )
        readingProgressDao.saveProgress(progressEntity)

        bookDao.updateReadingProgress(id, page, total, progress, position, chapterTitle, now)
    }

    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarks(bookId)

    suspend fun addBookmark(bookmark: BookmarkEntity): Long =
        bookmarkDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteBookmarkById(id)
}
