package com.example.ui.screens.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.ReadingStatus
import com.example.parser.BookImporter
import com.example.parser.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    LAST_OPENED("Recently Read"),
    TITLE("Title (A-Z)"),
    AUTHOR("Author (A-Z)"),
    DATE_ADDED("Recently Added"),
    PROGRESS("Reading Progress"),
    READING_TIME("Reading Time")
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository
    private val importer = BookImporter(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ReadingStatus?>(null) // null = All
    val selectedStatusFilter: StateFlow<ReadingStatus?> = _selectedStatusFilter.asStateFlow()

    private val _selectedFormatFilter = MutableStateFlow<BookFormat?>(null) // null = All
    val selectedFormatFilter: StateFlow<BookFormat?> = _selectedFormatFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.LAST_OPENED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val filteredBooks: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        _searchQuery,
        _selectedStatusFilter,
        _selectedFormatFilter,
        _sortOption
    ) { books, query, status, format, sort ->
        var list = books

        // Filter by search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.genre.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                it.personalNotes.lowercase().contains(q)
            }
        }

        // Filter by status
        if (status != null) {
            list = list.filter { it.status == status.name }
        }

        // Filter by format
        if (format != null) {
            list = list.filter { it.format.equals(format.name, ignoreCase = true) }
        }

        // Sort
        when (sort) {
            SortOption.LAST_OPENED -> list.sortedByDescending { it.lastOpenedTimestamp }
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> list.sortedBy { it.author.lowercase() }
            SortOption.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
            SortOption.PROGRESS -> list.sortedByDescending { it.progressPercent }
            SortOption.READING_TIME -> list.sortedByDescending { it.totalReadingTimeMillis }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterSelected(status: ReadingStatus?) {
        _selectedStatusFilter.value = status
    }

    fun onFormatFilterSelected(format: BookFormat?) {
        _selectedFormatFilter.value = format
    }

    fun onSortOptionSelected(sort: SortOption) {
        _sortOption.value = sort
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
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

    fun importBooks(uris: List<Uri>, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            var count = 0
            for (uri in uris) {
                val res = importer.importFromUri(uri, skipDuplicateCheck = true)
                if (res is ImportResult.Success) {
                    repository.insertBook(res.book)
                    count++
                }
            }
            onFinished("Imported $count books")
        }
    }
}
