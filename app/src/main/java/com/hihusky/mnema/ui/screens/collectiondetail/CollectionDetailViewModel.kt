package com.hihusky.mnema.ui.screens.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.model.Question
import com.hihusky.mnema.data.repository.DatabaseRepository
import com.hihusky.mnema.domain.service.PackageService
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

                val items = dbRepository.getCollectionItems(collectionId)
                val representativeBookId = items.firstOrNull()?.sourceBookId
                val book = representativeBookId?.let { dbRepository.getBookById(it) }
                val imageBasePath = book?.let { packageService.getPackageImagePath(it.filename) }

                val poolQuestions = dbRepository.getPoolQuestionsByCollection(collectionId)
                val questions = dbRepository.poolQuestionsToDomain(poolQuestions)

                _uiState.update {
                    it.copy(
                        collection = collection,
                        questions = questions,
                        imageBasePath = imageBasePath,
                        representativeBookId = representativeBookId,
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
            loadCollection()
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CollectionDetailUiState(
    val collection: com.hihusky.mnema.data.model.Collection? = null,
    val questions: List<Question> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleted: Boolean = false,
    val imageBasePath: String? = null,
    val representativeBookId: Int? = null
)
