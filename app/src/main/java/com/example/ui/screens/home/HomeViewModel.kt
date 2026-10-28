package com.example.ui.screens.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.BookEntity
import com.example.data.model.ReadingStatus
import com.example.parser.BookImporter
import com.example.parser.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class ImportState {
    data object Idle : ImportState()
    data class Importing(val current: Int, val total: Int) : ImportState()
    data class Success(val message: String) : ImportState()
    data class Duplicate(val existing: BookEntity, val candidate: BookEntity) : ImportState()
    data class Error(val error: String) : ImportState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository
    private val importer = BookImporter(application)

    val continueReadingBooks: StateFlow<List<BookEntity>> = repository.continueReadingBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistBooks: StateFlow<List<BookEntity>> = repository.wishlistBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBooksCount: StateFlow<Int> = repository.totalBooksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedBooksCount: StateFlow<Int> = repository.completedBooksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentlyReadingCount: StateFlow<Int> = repository.currentlyReadingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalReadingTimeFormatted: StateFlow<String> = repository.totalReadingTimeMillis.map { millis ->
        val totalMins = ((millis ?: 0L) / 60000).toInt()
        val hours = totalMins / 60
        val mins = totalMins % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0m")

    // Daily minutes read today
    private val startOfToday: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayMinutesRead: StateFlow<Int> = repository.getSessionsSince(startOfToday).map { sessions ->
        (sessions.sumOf { it.durationMillis } / 60000).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val readingGoalMinutes: StateFlow<Int> = repository.readingGoal.map {
        it?.dailyMinutesGoal ?: 30
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    // Current Streak (consecutive days with sessions)
    val streakDays: StateFlow<Int> = repository.allSessions.map { sessions ->
        calculateStreak(sessions.map { it.startTime })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private var pendingCandidate: BookEntity? = null

    fun importBooks(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importState.value = ImportState.Importing(1, uris.size)
            var successCount = 0

            for ((index, uri) in uris.withIndex()) {
                _importState.value = ImportState.Importing(index + 1, uris.size)
                val result = importer.importFromUri(
                    uri = uri,
                    skipDuplicateCheck = false,
                    existingBookChecker = { title, author ->
                        repository.findExistingBook(title, author)
                    }
                )

                when (result) {
                    is ImportResult.Success -> {
                        repository.insertBook(result.book)
                        successCount++
                    }
                    is ImportResult.Duplicate -> {
                        pendingCandidate = result.newBookCandidate
                        _importState.value = ImportState.Duplicate(result.existingBook, result.newBookCandidate)
                        return@launch
                    }
                    is ImportResult.Failure -> {
                        _importState.value = ImportState.Error("Failed to import ${result.fileName}: ${result.error}")
                        return@launch
                    }
                }
            }

            _importState.value = ImportState.Success("Successfully imported $successCount book(s) into your library!")
        }
    }

    fun replaceDuplicate() {
        val candidate = pendingCandidate ?: return
        viewModelScope.launch {
            repository.insertBook(candidate)
            pendingCandidate = null
            _importState.value = ImportState.Success("Replaced duplicate book successfully.")
        }
    }

    fun skipDuplicate() {
        pendingCandidate = null
        _importState.value = ImportState.Idle
    }

    fun clearImportState() {
        _importState.value = ImportState.Idle
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            repository.deleteBook(bookId, deleteInternalFile = true)
        }
    }

    fun updateBookDetails(
        bookId: Long,
        title: String,
        author: String,
        description: String,
        genre: String,
        tags: String,
        rating: Float,
        notes: String,
        status: ReadingStatus
    ) {
        viewModelScope.launch {
            repository.updateBookDetails(bookId, title, author, description, genre, tags, rating, notes)
            repository.updateBookStatus(bookId, status)
        }
    }

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val dayTimestamps = timestamps.map { ts ->
            Calendar.getInstance().apply {
                timeInMillis = ts
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.distinct().sortedDescending()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.timeInMillis

        // Streak is active if user read today or yesterday
        if (dayTimestamps.firstOrNull() != today && dayTimestamps.firstOrNull() != yesterday) {
            return 0
        }

        var streak = 0
        var currentExpectedDay = dayTimestamps.first()
        for (day in dayTimestamps) {
            if (day == currentExpectedDay) {
                streak++
                val nextDayCal = Calendar.getInstance().apply {
                    timeInMillis = currentExpectedDay
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                currentExpectedDay = nextDayCal.timeInMillis
            } else if (day < currentExpectedDay) {
                break
            }
        }
        return streak
    }
}
