package com.example.ui.screens.wishlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.BookEntity
import com.example.data.model.ReadingStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WishlistViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository

    val wishlistBooks: StateFlow<List<BookEntity>> = repository.wishlistBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToWishlist(title: String, author: String, genre: String, notes: String) {
        viewModelScope.launch {
            val book = BookEntity(
                title = title,
                author = author,
                format = "TXT",
                filePath = "",
                description = notes,
                genre = genre,
                status = ReadingStatus.WISHLIST.name,
                personalNotes = notes,
                dateAdded = System.currentTimeMillis()
            )
            repository.insertBook(book)
        }
    }

    fun startReading(bookId: Long) {
        viewModelScope.launch {
            repository.updateBookStatus(bookId, ReadingStatus.CURRENTLY_READING)
        }
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
}
