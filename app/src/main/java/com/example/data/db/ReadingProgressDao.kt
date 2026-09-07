package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    fun getProgressFlow(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getProgressForBook(bookId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress ORDER BY lastReadTimestamp DESC")
    fun getAllProgress(): Flow<List<ReadingProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity): Long

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgressByBookId(bookId: Long)
}
