package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"], unique = true)]
)
data class ReadingProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val bookType: BookType,
    val currentPage: Int = 1, // 1-based page index
    val totalPages: Int = 1,
    val progressPercent: Float = 0f,
    val characterOffset: Long = 0L,
    val chapterTitle: String? = null,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
