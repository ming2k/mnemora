package com.hihusky.mnemora.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.ImportResult
import com.hihusky.mnemora.data.repository.BookRepository
import com.hihusky.mnemora.data.repository.SettingsRepository
import com.hihusky.mnemora.data.repository.StudySessionRepository
import com.hihusky.mnemora.domain.service.PackageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val studySessionRepository: StudySessionRepository,
    private val packageService: PackageService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var booksCollectorJob: kotlinx.coroutines.Job? = null

    init {
        collectBooks()
    }

    private fun collectBooks() {
        booksCollectorJob?.cancel()
        booksCollectorJob = viewModelScope.launch {
            bookRepository.getBooksFlow(_uiState.value.searchQuery).collect { books ->
                val sessions = studySessionRepository.getActiveSessionsPerMode(books.map { it.id })
                _uiState.update {
                    it.copy(books = books, activeSessions = sessions, isLoading = false, error = null)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        collectBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val books = bookRepository.getBooks()
                val sessions = studySessionRepository.getActiveSessionsPerMode(books.map { it.id })
                _uiState.update { it.copy(books = books, activeSessions = sessions, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun importPackage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(importStatus = "Importing...", importProgress = null) }
            val result = packageService.importPackage(uri) { status, progress ->
                _uiState.update { it.copy(importStatus = status, importProgress = progress) }
            }
            when (result) {
                is ImportResult.Success -> {
                    loadBooks()
                    _uiState.update { it.copy(importStatus = null, importProgress = null, importSuccess = result.packageName) }
                }
                is ImportResult.Error -> {
                    _uiState.update { it.copy(importStatus = null, importProgress = null, importError = result.errorMessage) }
                }
                ImportResult.Cancelled -> {
                    _uiState.update { it.copy(importStatus = null, importProgress = null) }
                }
            }
        }
    }

    fun dismissImportSuccess() {
        _uiState.update { it.copy(importSuccess = null) }
    }

    fun dismissImportError() {
        _uiState.update { it.copy(importError = null) }
    }

    fun deleteBook(bookId: Int) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
            loadBooks()
        }
    }

    fun reorderBooks(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = _uiState.value.books.toMutableList()
            val moved = current.removeAt(fromIndex)
            val targetIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
            current.add(targetIndex, moved)
            current.forEachIndexed { index, book ->
                bookRepository.updateBookSortOrder(book.id, index)
            }
            _uiState.update { it.copy(books = current) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class HomeUiState(
    val books: List<Book> = emptyList(),
    val activeSessions: Map<Int, Map<String, StudySessionEntity>> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val importStatus: String? = null,
    val importProgress: Float? = null,
    val importSuccess: String? = null,
    val importError: String? = null
)
