package com.example.ui.screens.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.BookEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.model.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository

    private val _bookId = MutableStateFlow<Long?>(null)

    val book: StateFlow<BookEntity?> = _bookId.flatMapLatest { id ->
        if (id != null) repository.getBookById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookId.flatMapLatest { id ->
        if (id != null) repository.getBookmarksForBook(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBookId(id: Long) {
        _bookId.value = id
    }

    fun updateStatus(status: ReadingStatus) {
        val id = _bookId.value ?: return
        viewModelScope.launch {
            repository.updateBookStatus(id, status)
        }
    }

    fun updateDetails(
        title: String,
        author: String,
        description: String,
        genre: String,
        tags: String,
        rating: Float,
        notes: String,
        status: ReadingStatus
    ) {
        val id = _bookId.value ?: return
        viewModelScope.launch {
            repository.updateBookDetails(id, title, author, description, genre, tags, rating, notes)
            repository.updateBookStatus(id, status)
        }
    }

    fun updateNotesAndRating(notes: String, rating: Float) {
        val current = book.value ?: return
        viewModelScope.launch {
            repository.updateBookDetails(
                current.id,
                current.title,
                current.author,
                current.description,
                current.genre,
                current.tags,
                rating,
                notes
            )
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    fun deleteBook(onDeleted: () -> Unit) {
        val id = _bookId.value ?: return
        viewModelScope.launch {
            repository.deleteBook(id, deleteInternalFile = true)
            onDeleted()
        }
    }
}
