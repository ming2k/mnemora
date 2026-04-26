package com.hihusky.mnema.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import com.hihusky.mnema.data.model.Book
import com.hihusky.mnema.data.model.ImportResult
import com.hihusky.mnema.data.repository.DatabaseRepository
import com.hihusky.mnema.data.repository.SettingsRepository
import com.hihusky.mnema.domain.service.PackageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dbRepository: DatabaseRepository,
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
            dbRepository.getBooksFlow(_uiState.value.searchQuery).collect { books ->
                val sessions = mutableMapOf<Int, StudySessionEntity>()
                books.forEach { book ->
                    dbRepository.getMostRecentActiveSession(book.id)?.let {
                        sessions[book.id] = it
                    }
                }
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
                val books = dbRepository.getBooks()
                val sessions = mutableMapOf<Int, StudySessionEntity>()
                books.forEach { book ->
                    dbRepository.getMostRecentActiveSession(book.id)?.let {
                        sessions[book.id] = it
                    }
                }
                _uiState.update { it.copy(books = books, activeSessions = sessions, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun getSessionsByBook(bookId: Int) = dbRepository.getSessionsByBook(bookId)

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
            dbRepository.deleteBook(bookId)
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
                dbRepository.updateBookSortOrder(book.id, index)
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
    val activeSessions: Map<Int, StudySessionEntity> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val importStatus: String? = null,
    val importProgress: Float? = null,
    val importSuccess: String? = null,
    val importError: String? = null
)
