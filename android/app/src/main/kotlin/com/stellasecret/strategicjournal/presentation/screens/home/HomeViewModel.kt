package com.stellasecret.strategicjournal.presentation.screens.home

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellasecret.strategicjournal.domain.model.JournalEntry
import com.stellasecret.strategicjournal.domain.repository.JournalRepository
import com.stellasecret.strategicjournal.domain.repository.SyncResult
import com.stellasecret.strategicjournal.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
        private val driveDataSource: com.stellasecret.strategicjournal.data.remote.GoogleDriveDataSource,
    ) : ViewModel() {
        val entries: StateFlow<List<JournalEntry>> =
            repository
                .observeEntries()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val pendingReviews: StateFlow<List<JournalEntry>> =
            repository
                .observePendingReviews()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val syncState: StateFlow<SyncState> =
            repository
                .observeSyncState()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncState.Idle)

        private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
        val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent

        // Check auth directly from GoogleSignIn — reliable source of truth
        private val _isDriveAuthenticated = MutableStateFlow(driveDataSource.isAuthenticated())
        val isDriveAuthenticated: StateFlow<Boolean> = _isDriveAuthenticated.asStateFlow()

        init {
            // Observe auth state changes triggered by sign-in/sign-out in MainActivity
            viewModelScope.launch {
                var previousAuthState = driveDataSource.isAuthenticated()
                snapshotFlow { com.stellasecret.strategicjournal.MainActivity.authStateChanged.value }
                    .collect {
                        val isNowAuthenticated = driveDataSource.isAuthenticated()
                        _isDriveAuthenticated.value = isNowAuthenticated

                        // Just signed in → pull data from Drive
                        if (isNowAuthenticated && !previousAuthState) {
                            timber.log.Timber.d("Drive auth: signed in, pulling data...")
                            val result = repository.syncFromDrive()
                            timber.log.Timber.d("Drive pull result: $result")
                        }
                        previousAuthState = isNowAuthenticated
                    }
            }
        }

        fun signInWithGoogle() {
            // Trigger is handled by the Activity via GoogleSignIn intent
            // ViewModel emits an event that the screen catches
            viewModelScope.launch {
                _uiEvent.emit(HomeUiEvent.RequestGoogleSignIn)
            }
        }

        fun signOutGoogle() {
            viewModelScope.launch {
                _uiEvent.emit(HomeUiEvent.SignOutGoogle)
            }
        }

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
    data class ShowError(
        val message: String,
    ) : HomeUiEvent()

    object RequestGoogleSignIn : HomeUiEvent()

    object SignOutGoogle : HomeUiEvent()
}
