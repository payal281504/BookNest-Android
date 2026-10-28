package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.ReadingGoalEntity
import com.example.parser.StarterBooksGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository
    private val context = application.applicationContext

    val readingGoal: StateFlow<ReadingGoalEntity> = repository.readingGoal.map {
        it ?: ReadingGoalEntity(id = 1, dailyMinutesGoal = 30, monthlyBooksGoal = 2)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingGoalEntity(id = 1, dailyMinutesGoal = 30, monthlyBooksGoal = 2))

    val totalBooks: StateFlow<Int> = repository.totalBooksCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _storageSizeFormatted = MutableStateFlow("Calculating...")
    val storageSizeFormatted: StateFlow<String> = _storageSizeFormatted.asStateFlow()

    init {
        calculateStorage()
    }

    fun calculateStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            val booksDir = File(context.filesDir, "books")
            val coversDir = File(context.filesDir, "covers")
            val totalBytes = getDirSize(booksDir) + getDirSize(coversDir)
            val mb = totalBytes.toDouble() / (1024 * 1024)
            val formatted = if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", totalBytes.toDouble() / 1024)
            _storageSizeFormatted.value = formatted
        }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        return dir.listFiles()?.sumOf { if (it.isDirectory) getDirSize(it) else it.length() } ?: 0L
    }

    fun updateGoals(dailyMinutes: Int, monthlyBooks: Int) {
        viewModelScope.launch {
            repository.saveReadingGoal(dailyMinutes, monthlyBooks)
        }
    }

    fun restoreStarterBooks(onFinished: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val starter = StarterBooksGenerator.generateStarterBooks(context)
                for (book in starter) {
                    val existing = repository.findExistingBook(book.title, book.author)
                    if (existing == null) {
                        repository.insertBook(book)
                    }
                }
            }
            calculateStorage()
            onFinished()
        }
    }
}
