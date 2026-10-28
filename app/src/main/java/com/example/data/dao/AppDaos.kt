package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BookEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ReadingGoalEntity
import com.example.data.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastOpenedTimestamp DESC, dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE status = :status ORDER BY lastOpenedTimestamp DESC, dateAdded DESC")
    fun getBooksByStatus(status: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = 'CURRENTLY_READING' OR (currentPage > 1 AND status != 'COMPLETED' AND status != 'ABANDONED') ORDER BY lastOpenedTimestamp DESC LIMIT 10")
    fun getContinueReadingBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = 'COMPLETED' ORDER BY dateCompleted DESC LIMIT :limit")
    fun getRecentlyCompletedBooks(limit: Int = 10): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = 'WISHLIST' ORDER BY dateAdded DESC")
    fun getWishlistBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title = :title AND author = :author LIMIT 1")
    suspend fun findBookByTitleAndAuthor(title: String, author: String): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath LIMIT 1")
    suspend fun findBookByFilePath(filePath: String): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    fun getTotalBookCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'COMPLETED'")
    fun getCompletedBookCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'CURRENTLY_READING'")
    fun getCurrentlyReadingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>): List<Long>

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC, createdAt DESC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksForBook(bookId: Long)
}

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE startTime >= :sinceTimestamp ORDER BY startTime ASC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("SELECT SUM(durationMillis) FROM reading_sessions")
    fun getTotalReadingTimeMillis(): Flow<Long?>
}

@Dao
interface ReadingGoalDao {
    @Query("SELECT * FROM reading_goals WHERE id = 1 LIMIT 1")
    fun getGoal(): Flow<ReadingGoalEntity?>

    @Query("SELECT * FROM reading_goals WHERE id = 1 LIMIT 1")
    suspend fun getGoalSync(): ReadingGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: ReadingGoalEntity)
}
