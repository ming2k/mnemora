package com.hihusky.mnemora.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.repository.BookRepository
import com.hihusky.mnemora.data.repository.StudySessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordsViewModel
    @Inject
    constructor(
        private val studySessionRepository: StudySessionRepository,
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecordsUiState())
        val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                studySessionRepository.getAllSessions().collect { sessions ->
                    val bookMap = mutableMapOf<Int, Book>()
                    sessions.map { it.bookId }.toSet().forEach { bookId ->
                        bookRepository.getBookById(bookId)?.let { bookMap[bookId] = it }
                    }
                    _uiState.update {
                        it.copy(sessions = sessions, bookMap = bookMap, isLoading = false)
                    }
                }
            }
        }
    }

data class RecordsUiState(
    val sessions: List<StudySessionEntity> = emptyList(),
    val bookMap: Map<Int, Book> = emptyMap(),
    val isLoading: Boolean = true,
)
