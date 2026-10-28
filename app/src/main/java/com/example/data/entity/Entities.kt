package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val format: String, // "PDF", "EPUB", "TXT"
    val filePath: String,
    val coverPath: String? = null,
    val description: String = "",
    val genre: String = "General",
    val tags: String = "",
    val status: String = "NOT_STARTED", // From ReadingStatus
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val epubChapterIndex: Int = 0,
    val epubScrollProgress: Float = 0f,
    val progressPercent: Float = 0f,
    val lastOpenedTimestamp: Long = 0L,
    val totalReadingTimeMillis: Long = 0L,
    val personalRating: Float = 0f,
    val personalNotes: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val dateCompleted: Long? = null,
    val fileSizeBytes: Long = 0L,
    val isSampleBook: Boolean = false
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val chapterTitle: String? = null,
    val title: String,
    val snippet: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val pagesRead: Int = 0
)

@Entity(tableName = "reading_goals")
data class ReadingGoalEntity(
    @PrimaryKey
    val id: Long = 1,
    val dailyMinutesGoal: Int = 30,
    val monthlyBooksGoal: Int = 2
)
