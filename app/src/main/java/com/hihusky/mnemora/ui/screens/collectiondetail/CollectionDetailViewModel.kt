package com.hihusky.mnemora.ui.screens.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.repository.DatabaseRepository
import com.hihusky.mnemora.domain.service.PackageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dbRepository: DatabaseRepository,
    private val packageService: PackageService
) : ViewModel() {

    private val collectionId: Int = checkNotNull(savedStateHandle["collectionId"])

    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()

    init {
        loadCollection()
    }

    fun loadCollection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val collection = dbRepository.getCollectionById(collectionId)
                    ?: throw IllegalStateException("Collection not found")

                val book = dbRepository.getBookById(collection.bookId)
                val imageBasePath = book?.let { packageService.getPackageImagePath(it.filename) }

                val questions = dbRepository.getQuestionsByCollection(collectionId)

                _uiState.update {
                    it.copy(
                        collection = collection,
                        questions = questions,
                        imageBasePath = imageBasePath,
                        representativeBookId = collection.bookId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteCollection() {
        viewModelScope.launch {
            dbRepository.deleteCollection(collectionId)
            _uiState.update { it.copy(deleted = true) }
        }
    }

    fun removeQuestion(questionId: Int) {
        viewModelScope.launch {
            val bookId = _uiState.value.questions.find { it.id == questionId }?.bookId
                ?: _uiState.value.representativeBookId
                ?: return@launch
            dbRepository.deleteCollectionItem(collectionId, questionId)
            loadCollection()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CollectionDetailUiState(
    val collection: com.hihusky.mnemora.data.model.Collection? = null,
    val questions: List<Question> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleted: Boolean = false,
    val imageBasePath: String? = null,
    val representativeBookId: Int? = null
)
