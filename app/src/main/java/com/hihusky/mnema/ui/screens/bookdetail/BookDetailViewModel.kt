package com.hihusky.mnema.ui.screens.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.CollectionEntity
import com.hihusky.mnema.data.model.Collection
import com.hihusky.mnema.data.model.CollectionBehavior
import com.hihusky.mnema.data.model.CollectionKind
import com.hihusky.mnema.data.model.Node
import com.hihusky.mnema.data.repository.DatabaseRepository
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
                val collections = dbRepository.getAllCollections()
                val stats = dbRepository.getSrsStats(bookId)
                val answers = dbRepository.getUserAnswers(bookId)
                val markedCount = dbRepository.getMarkedQuestions(bookId).size
                val wrongCount = dbRepository.getWrongQuestionIds(bookId).size
                val dueCount = dbRepository.getSrsDueQuestionIds(bookId).size
                val totalQuestions = book?.totalQuestions ?: 0
                val answeredCount = answers.size
                val unansweredCount = totalQuestions - answeredCount

                _uiState.update {
                    it.copy(
                        book = book,
                        nodes = nodes,
                        collections = collections,
                        srsStats = stats,
                        answeredCount = answeredCount,
                        markedCount = markedCount,
                        wrongCount = wrongCount,
                        unansweredCount = unansweredCount.coerceAtLeast(0),
                        dueCount = dueCount,
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
}

data class BookDetailUiState(
    val book: com.hihusky.mnema.data.model.Book? = null,
    val nodes: List<Node> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val srsStats: com.hihusky.mnema.data.model.SrsStats = com.hihusky.mnema.data.model.SrsStats(),
    val answeredCount: Int = 0,
    val markedCount: Int = 0,
    val wrongCount: Int = 0,
    val unansweredCount: Int = 0,
    val dueCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
