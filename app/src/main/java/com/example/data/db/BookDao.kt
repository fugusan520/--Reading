package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import com.example.data.model.BookType
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("""
        SELECT * FROM books 
        WHERE bookType = :bookType AND (:folderId IS NULL AND folderId IS NULL OR folderId = :folderId)
        ORDER BY isPinned DESC, lastReadTime DESC
    """)
    fun getBooks(bookType: BookType, folderId: Long?): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookFlow(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE books SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE books SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?)

    @Query("""
        UPDATE books 
        SET currentPage = :currentPage, totalPages = :totalPages, progressPercent = :progress, 
            lastReadPosition = :position, lastReadChapterTitle = :chapterTitle, lastReadTime = :time
        WHERE id = :id
    """)
    suspend fun updateReadingProgress(
        id: Long,
        currentPage: Int,
        totalPages: Int,
        progress: Float,
        position: Long,
        chapterTitle: String?,
        time: Long = System.currentTimeMillis()
    )

    @Query("UPDATE books SET folderId = :targetFolderId WHERE folderId = :folderId")
    suspend fun resetBooksFolder(folderId: Long, targetFolderId: Long?)

    @Query("SELECT COUNT(*) FROM books WHERE folderId = :folderId")
    suspend fun getBookCountInFolder(folderId: Long): Int
}
