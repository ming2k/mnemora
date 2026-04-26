package com.hihusky.mnemora.ui.screens.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val bookId: Int = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val book = dbRepository.getBookById(bookId)
                val nodes = dbRepository.getNodes(bookId)
                val collections = dbRepository.getCollectionsByBook(bookId)
                val sessions = dbRepository.getSessionsByBookOnce(bookId)
                val stats = dbRepository.getSrsStats(bookId)
                val answers = dbRepository.getUserAnswers(bookId)
                val answeredCount = answers.size

                _uiState.update {
                    it.copy(
                        book = book,
                        nodes = nodes,
                        collections = collections,
                        sessions = sessions,
                        srsStats = stats,
                        answeredCount = answeredCount,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createCollection(name: String, description: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dbRepository.insertCollection(
                CollectionEntity(
                    bookId = bookId,
                    kind = CollectionKind.Custom.name.lowercase(),
                    behavior = CollectionBehavior.Manual.name.lowercase(),
                    name = name,
                    description = description.ifBlank { null },
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )
            loadData()
        }
    }

    fun deleteBook(onDeleted: () -> Unit) {
        viewModelScope.launch {
            dbRepository.deleteBook(bookId)
            onDeleted()
        }
    }
}

data class BookDetailUiState(
    val book: com.hihusky.mnemora.data.model.Book? = null,
    val nodes: List<Node> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val srsStats: com.hihusky.mnemora.data.model.SrsStats = com.hihusky.mnemora.data.model.SrsStats(),
    val answeredCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
