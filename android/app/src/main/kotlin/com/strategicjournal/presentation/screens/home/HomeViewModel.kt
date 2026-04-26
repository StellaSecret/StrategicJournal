package com.strategicjournal.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strategicjournal.domain.model.JournalEntry
import com.strategicjournal.domain.repository.JournalRepository
import com.strategicjournal.domain.repository.SyncResult
import com.strategicjournal.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: JournalRepository
) : ViewModel() {

    val entries: StateFlow<List<JournalEntry>> = repository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingReviews: StateFlow<List<JournalEntry>> = repository.observePendingReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncState: StateFlow<SyncState> = repository.observeSyncState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncState.Idle)

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent

    fun syncNow() {
        viewModelScope.launch {
            val result = repository.syncToDrive()
            if (result is SyncResult.Error) {
                _uiEvent.emit(HomeUiEvent.ShowError(result.message))
            }
        }
    }

    fun todayEntryId(): String? {
        val today = LocalDate.now().toString()
        return entries.value.firstOrNull { it.date == today }?.id
    }
}

sealed class HomeUiEvent {
    data class ShowError(val message: String) : HomeUiEvent()
}
