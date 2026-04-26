package com.hihusky.mnemora.ui.screens.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.model.CollectionSummary
import com.hihusky.mnemora.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
    }

    fun loadCollections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val summaries = dbRepository.getAllCollectionSummaries()
                _uiState.update { it.copy(summaries = summaries, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createCollection(name: String, description: String = "") {
        _uiState.update {
            it.copy(error = "Create collections from a package detail screen.")
        }
    }

    fun deleteCollection(collectionId: Int) {
        viewModelScope.launch {
            try {
                dbRepository.deleteCollection(collectionId)
                loadCollections()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CollectionsUiState(
    val summaries: List<CollectionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
