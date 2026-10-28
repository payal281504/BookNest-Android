package com.example.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.BookEntity
import com.example.data.entity.ReadingGoalEntity
import com.example.data.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DayActivity(
    val dayName: String,
    val dayOfMonth: Int,
    val minutesRead: Int,
    val isToday: Boolean
)

data class SessionLogItem(
    val session: ReadingSessionEntity,
    val bookTitle: String,
    val bookFormat: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository

    val readingGoal: StateFlow<ReadingGoalEntity> = repository.readingGoal.map {
        it ?: ReadingGoalEntity(id = 1, dailyMinutesGoal = 30, monthlyBooksGoal = 2)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingGoalEntity(id = 1, dailyMinutesGoal = 30, monthlyBooksGoal = 2))

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<ReadingSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly activity for last 7 days
    val weeklyActivity: StateFlow<List<DayActivity>> = repository.allSessions.map { sessions ->
        val list = mutableListOf<DayActivity>()
        val cal = Calendar.getInstance()
        val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Generate past 7 days up to today
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + 86400000L

            val dayMinutes = (sessions.filter { it.startTime in startOfDay until endOfDay }
                .sumOf { it.durationMillis } / 60000).toInt()

            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK) - 1
            list.add(
                DayActivity(
                    dayName = dayNames[dayOfWeek],
                    dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH),
                    minutesRead = dayMinutes,
                    isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                )
            )
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's reading minutes
    val todayMinutesRead: StateFlow<Int> = weeklyActivity.map { week ->
        week.firstOrNull { it.isToday }?.minutesRead ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Total stats
    val totalReadingTimeFormatted: StateFlow<String> = repository.totalReadingTimeMillis.map { millis ->
        val totalMins = ((millis ?: 0L) / 60000).toInt()
        val hours = totalMins / 60
        val mins = totalMins % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0m")

    val totalPagesRead: StateFlow<Int> = repository.allBooks.map { books ->
        books.sumOf { it.currentPage.coerceAtLeast(1) - 1 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCompletedBooks: StateFlow<Int> = repository.completedBooksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedThisMonth: StateFlow<Int> = repository.allBooks.map { books ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        books.count { it.status == "COMPLETED" && (it.dateCompleted ?: 0L) >= startOfMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Streak
    val streakDays: StateFlow<Int> = repository.allSessions.map { sessions ->
        calculateStreak(sessions.map { it.startTime })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Session log items with book titles
    val sessionLogs: StateFlow<List<SessionLogItem>> = combine(
        repository.allSessions,
        repository.allBooks
    ) { sessions, books ->
        val bookMap = books.associateBy { it.id }
        sessions.take(20).map { session ->
            val book = bookMap[session.bookId]
            SessionLogItem(
                session = session,
                bookTitle = book?.title ?: "Unknown Book",
                bookFormat = book?.format ?: "PDF"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateGoals(dailyMinutes: Int, monthlyBooks: Int) {
        viewModelScope.launch {
            repository.saveReadingGoal(dailyMinutes, monthlyBooks)
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
