package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookType {
    NOVEL,
    COMIC
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uriString: String,
    val localFilePath: String? = null,
    val bookType: BookType,
    val folderId: Long? = null, // null for root shelf
    val isPinned: Boolean = false,
    val progressPercent: Float = 0f,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val lastReadPosition: Long = 0L, // character offset or page
    val lastReadChapterTitle: String? = null,
    val coverColorHex: String? = null,
    val coverImagePath: String? = null,
    val fileFormat: String = "TXT",
    val isArchiveExtracted: Boolean = false,
    val addedTime: Long = System.currentTimeMillis(),
    val lastReadTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null, // null for level 1; parentFolderId for level 2
    val bookType: BookType,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterTitle: String,
    val characterOffset: Long = 0L,
    val pageIndex: Int = 0,
    val snippet: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
