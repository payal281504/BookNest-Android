package com.example.data.repository

import com.example.data.dao.BookDao
import com.example.data.dao.BookmarkDao
import com.example.data.dao.ReadingGoalDao
import com.example.data.dao.ReadingSessionDao
import com.example.data.entity.BookEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ReadingGoalEntity
import com.example.data.entity.ReadingSessionEntity
import com.example.data.model.ReadingStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookRepository(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val readingSessionDao: ReadingSessionDao,
    private val readingGoalDao: ReadingGoalDao
) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val continueReadingBooks: Flow<List<BookEntity>> = bookDao.getContinueReadingBooks()
    val wishlistBooks: Flow<List<BookEntity>> = bookDao.getWishlistBooks()
    val totalBooksCount: Flow<Int> = bookDao.getTotalBookCount()
    val completedBooksCount: Flow<Int> = bookDao.getCompletedBookCount()
    val currentlyReadingCount: Flow<Int> = bookDao.getCurrentlyReadingCount()
    val totalReadingTimeMillis: Flow<Long?> = readingSessionDao.getTotalReadingTimeMillis()
    val allSessions: Flow<List<ReadingSessionEntity>> = readingSessionDao.getAllSessions()
    val readingGoal: Flow<ReadingGoalEntity?> = readingGoalDao.getGoal()

    fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun getBookByIdSync(id: Long): BookEntity? = bookDao.getBookByIdSync(id)

    fun getBooksByStatus(status: ReadingStatus): Flow<List<BookEntity>> =
        bookDao.getBooksByStatus(status.name)

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookId)

    fun getSessionsSince(timestamp: Long): Flow<List<ReadingSessionEntity>> =
        readingSessionDao.getSessionsSince(timestamp)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun updateReadingProgress(
        bookId: Long,
        currentPage: Int,
        totalPages: Int,
        chapterIndex: Int = 0,
        scrollProgress: Float = 0f,
        additionalReadingTimeMillis: Long = 0L
    ) {
        val book = bookDao.getBookByIdSync(bookId) ?: return
        val effectiveTotal = if (totalPages > 0) totalPages else book.totalPages.coerceAtLeast(1)
        val clampedPage = currentPage.coerceIn(1, effectiveTotal)
        val calculatedPercent = ((clampedPage.toFloat() / effectiveTotal.toFloat()) * 100f).coerceIn(0f, 100f)

        val newStatus = when {
            calculatedPercent >= 100f -> ReadingStatus.COMPLETED.name
            book.status == ReadingStatus.NOT_STARTED.name || book.status == ReadingStatus.WISHLIST.name ->
                ReadingStatus.CURRENTLY_READING.name
            else -> book.status
        }

        val completedDate = if (newStatus == ReadingStatus.COMPLETED.name && book.dateCompleted == null) {
            System.currentTimeMillis()
        } else book.dateCompleted

        val updated = book.copy(
            currentPage = clampedPage,
            totalPages = effectiveTotal,
            epubChapterIndex = chapterIndex,
            epubScrollProgress = scrollProgress,
            progressPercent = calculatedPercent,
            status = newStatus,
            lastOpenedTimestamp = System.currentTimeMillis(),
            totalReadingTimeMillis = book.totalReadingTimeMillis + additionalReadingTimeMillis,
            dateCompleted = completedDate
        )
        bookDao.updateBook(updated)
    }

    suspend fun updateBookStatus(bookId: Long, newStatus: ReadingStatus) {
        val book = bookDao.getBookByIdSync(bookId) ?: return
        val completedDate = if (newStatus == ReadingStatus.COMPLETED) {
            book.dateCompleted ?: System.currentTimeMillis()
        } else null

        val updated = book.copy(
            status = newStatus.name,
            dateCompleted = completedDate,
            currentPage = if (newStatus == ReadingStatus.COMPLETED) book.totalPages else book.currentPage,
            progressPercent = if (newStatus == ReadingStatus.COMPLETED) 100f else book.progressPercent
        )
        bookDao.updateBook(updated)
    }

    suspend fun recordReadingSession(
        bookId: Long,
        startTime: Long,
        endTime: Long,
        pagesRead: Int
    ) {
        val duration = (endTime - startTime).coerceAtLeast(0)
        if (duration < 3000) return // Ignore sessions under 3 seconds
        val session = ReadingSessionEntity(
            bookId = bookId,
            startTime = startTime,
            endTime = endTime,
            durationMillis = duration,
            pagesRead = pagesRead
        )
        readingSessionDao.insertSession(session)
    }

    suspend fun addBookmark(
        bookId: Long,
        pageNumber: Int,
        chapterTitle: String? = null,
        title: String,
        snippet: String = ""
    ): Long {
        val bookmark = BookmarkEntity(
            bookId = bookId,
            pageNumber = pageNumber,
            chapterTitle = chapterTitle,
            title = title,
            snippet = snippet
        )
        return bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmarkId: Long) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    suspend fun updateBookDetails(
        bookId: Long,
        title: String,
        author: String,
        description: String,
        genre: String,
        tags: String,
        rating: Float,
        notes: String
    ) {
        val book = bookDao.getBookByIdSync(bookId) ?: return
        val updated = book.copy(
            title = title,
            author = author,
            description = description,
            genre = genre,
            tags = tags,
            personalRating = rating,
            personalNotes = notes
        )
        bookDao.updateBook(updated)
    }

    suspend fun deleteBook(bookId: Long, deleteInternalFile: Boolean = false) {
        val book = bookDao.getBookByIdSync(bookId)
        if (book != null) {
            bookmarkDao.deleteBookmarksForBook(bookId)
            bookDao.deleteBookById(bookId)
            if (deleteInternalFile) {
                try {
                    val file = File(book.filePath)
                    if (file.exists() && file.parentFile?.name == "books") {
                        file.delete()
                    }
                    book.coverPath?.let {
                        val coverFile = File(it)
                        if (coverFile.exists()) coverFile.delete()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun findExistingBook(title: String, author: String): BookEntity? {
        return bookDao.findBookByTitleAndAuthor(title, author)
    }

    suspend fun saveReadingGoal(dailyMinutes: Int, monthlyBooks: Int) {
        val goal = ReadingGoalEntity(
            id = 1,
            dailyMinutesGoal = dailyMinutes,
            monthlyBooksGoal = monthlyBooks
        )
        readingGoalDao.insertOrUpdateGoal(goal)
    }
}
