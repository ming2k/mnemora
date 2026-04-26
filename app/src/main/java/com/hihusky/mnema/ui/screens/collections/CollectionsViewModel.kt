package com.hihusky.mnema.ui.screens.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.CollectionEntity
import com.hihusky.mnema.data.model.Collection
import com.hihusky.mnema.data.model.CollectionBehavior
import com.hihusky.mnema.data.model.CollectionKind
import com.hihusky.mnema.data.repository.DatabaseRepository
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
                val collections = dbRepository.getAllCollections()
                _uiState.update {
                    it.copy(
                        collections = collections,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun createCollection(name: String, description: String = "") {
        viewModelScope.launch {
            try {
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
                loadCollections()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
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
    val collections: List<Collection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
