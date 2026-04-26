package com.strategicjournal.domain.repository

import com.strategicjournal.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {

    /** Stream all entries, ordered by date descending */
    fun observeEntries(): Flow<List<JournalEntry>>

    /** Stream entries with pending predictions to review */
    fun observePendingReviews(): Flow<List<JournalEntry>>

    suspend fun getEntry(id: String): JournalEntry?

    suspend fun getEntryByDate(date: String): JournalEntry?

    suspend fun saveEntry(entry: JournalEntry)

    suspend fun deleteEntry(id: String)

    /** Push all dirty entries to Google Drive */
    suspend fun syncToDrive(): SyncResult

    /** Pull latest from Google Drive and merge locally */
    suspend fun syncFromDrive(): SyncResult

    fun observeSyncState(): Flow<SyncState>
}

sealed class SyncResult {
    data class Success(val pushed: Int, val pulled: Int) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class LastSync(val timestamp: String, val success: Boolean) : SyncState()
    object NotAuthenticated : SyncState()
}
