package com.example.ui.screens.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BookNestApplication
import com.example.data.entity.BookEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.model.FontFamilyPreference
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingStatus
import com.example.parser.EpubChapter
import com.example.parser.EpubParser
import com.example.parser.TxtBookContent
import com.example.parser.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BookNestApplication).repository

    private val _bookId = MutableStateFlow<Long?>(null)
    val bookId: StateFlow<Long?> = _bookId.asStateFlow()

    val book: StateFlow<BookEntity?> = _bookId.flatMapLatest { id ->
        if (id != null) repository.getBookById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookId.flatMapLatest { id ->
        if (id != null) repository.getBookmarksForBook(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reader UI Settings
    private val _readerSettings = MutableStateFlow(ReaderSettings())
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    // Current Reading State
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _epubChapters = MutableStateFlow<List<EpubChapter>>(emptyList())
    val epubChapters: StateFlow<List<EpubChapter>> = _epubChapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _txtContent = MutableStateFlow<TxtBookContent?>(null)
    val txtContent: StateFlow<TxtBookContent?> = _txtContent.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    // Session tracking
    private var sessionStartTime: Long = System.currentTimeMillis()
    private var startPage: Int = 1

    fun loadBook(id: Long) {
        if (_bookId.value == id) return
        _bookId.value = id
        sessionStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            val loaded = repository.getBookByIdSync(id) ?: return@launch
            _currentPage.value = loaded.currentPage.coerceAtLeast(1)
            _totalPages.value = loaded.totalPages.coerceAtLeast(1)
            startPage = loaded.currentPage

            val file = File(loaded.filePath)
            if (file.exists()) {
                withContext(Dispatchers.IO) {
                    when (loaded.format.uppercase()) {
                        "EPUB" -> {
                            val chapters = EpubParser.loadChapters(file)
                            _epubChapters.value = chapters
                            _totalPages.value = chapters.size.coerceAtLeast(1)
                            _currentChapterIndex.value = loaded.epubChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
                            _currentPage.value = (_currentChapterIndex.value + 1).coerceAtLeast(1)
                        }
                        "TXT" -> {
                            val parsed = TxtParser.parse(file)
                            _txtContent.value = parsed
                            _totalPages.value = parsed.chapters.size.coerceAtLeast(1)
                            val chapterIdx = (loaded.currentPage - 1).coerceIn(0, (parsed.chapters.size - 1).coerceAtLeast(0))
                            _currentChapterIndex.value = chapterIdx
                        }
                        "PDF" -> {
                            // PDF pages handled by PdfRenderer
                        }
                    }
                }
            }
        }
    }

    fun toggleControls() {
        _isControlsVisible.value = !_isControlsVisible.value
    }

    fun setControlsVisible(visible: Boolean) {
        _isControlsVisible.value = visible
    }

    fun onPageChanged(page: Int, total: Int) {
        val clamped = page.coerceIn(1, total.coerceAtLeast(1))
        _currentPage.value = clamped
        _totalPages.value = total

        // Check if reached end of book
        if (clamped >= total && total > 1 && book.value?.status != ReadingStatus.COMPLETED.name) {
            _showCompletionDialog.value = true
        }

        persistProgressDebounced(clamped, total)
    }

    fun onChapterSelected(chapterIndex: Int) {
        val maxIndex = (_epubChapters.value.size - 1).coerceAtLeast(0)
        val clamped = chapterIndex.coerceIn(0, maxIndex)
        _currentChapterIndex.value = clamped
        _currentPage.value = clamped + 1
        persistProgressDebounced(clamped + 1, _epubChapters.value.size)
    }

    fun onTxtChapterSelected(chapterIndex: Int) {
        val chapters = _txtContent.value?.chapters ?: return
        val clamped = chapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
        _currentChapterIndex.value = clamped
        _currentPage.value = clamped + 1
        persistProgressDebounced(clamped + 1, chapters.size)
    }

    private fun persistProgressDebounced(page: Int, total: Int) {
        val id = _bookId.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sessionElapsed = now - sessionStartTime
            repository.updateReadingProgress(
                bookId = id,
                currentPage = page,
                totalPages = total,
                chapterIndex = _currentChapterIndex.value,
                scrollProgress = 0f,
                additionalReadingTimeMillis = sessionElapsed
            )
            sessionStartTime = now // Reset session window after persisting incremental time
        }
    }

    fun markBookCompleted() {
        val id = _bookId.value ?: return
        _showCompletionDialog.value = false
        viewModelScope.launch {
            repository.updateBookStatus(id, ReadingStatus.COMPLETED)
        }
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    fun updateTheme(theme: ReaderTheme) {
        _readerSettings.value = _readerSettings.value.copy(theme = theme)
    }

    fun updateFontSize(sizeSp: Float) {
        _readerSettings.value = _readerSettings.value.copy(fontSizeSp = sizeSp.coerceIn(12f, 32f))
    }

    fun updateFontFamily(family: FontFamilyPreference) {
        _readerSettings.value = _readerSettings.value.copy(fontFamily = family)
    }

    fun updateLineSpacing(spacing: Float) {
        _readerSettings.value = _readerSettings.value.copy(lineHeightMultiplier = spacing)
    }

    fun addBookmark(snippet: String = "") {
        val id = _bookId.value ?: return
        val page = _currentPage.value
        val title = "Bookmark at Page $page"
        viewModelScope.launch {
            repository.addBookmark(
                bookId = id,
                pageNumber = page,
                chapterTitle = "Chapter ${_currentChapterIndex.value + 1}",
                title = title,
                snippet = snippet.take(120)
            )
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    fun endReadingSession() {
        val id = _bookId.value ?: return
        val endTime = System.currentTimeMillis()
        val duration = endTime - sessionStartTime
        val pagesRead = (_currentPage.value - startPage).coerceAtLeast(0)

        viewModelScope.launch {
            repository.recordReadingSession(
                bookId = id,
                startTime = sessionStartTime,
                endTime = endTime,
                pagesRead = pagesRead
            )
            repository.updateReadingProgress(
                bookId = id,
                currentPage = _currentPage.value,
                totalPages = _totalPages.value,
                chapterIndex = _currentChapterIndex.value,
                scrollProgress = 0f,
                additionalReadingTimeMillis = duration
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        endReadingSession()
    }
}
