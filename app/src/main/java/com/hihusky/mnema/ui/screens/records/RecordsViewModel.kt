package com.hihusky.mnema.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import com.hihusky.mnema.data.model.Book
import com.hihusky.mnema.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dbRepository.getAllSessions().collect { sessions ->
                val bookMap = mutableMapOf<Int, Book>()
                sessions.map { it.bookId }.toSet().forEach { bookId ->
                    dbRepository.getBookById(bookId)?.let { bookMap[bookId] = it }
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
    val isLoading: Boolean = true
)
