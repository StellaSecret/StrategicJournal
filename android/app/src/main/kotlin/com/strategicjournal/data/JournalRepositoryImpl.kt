package com.strategicjournal.data

import com.strategicjournal.data.local.JournalEntryDao
import com.strategicjournal.data.local.JournalEntryEntity
import com.strategicjournal.data.remote.GoogleDriveDataSource
import com.strategicjournal.domain.model.JournalEntry
import com.strategicjournal.domain.repository.JournalRepository
import com.strategicjournal.domain.repository.SyncResult
import com.strategicjournal.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao,
    private val driveDataSource: GoogleDriveDataSource,
    private val json: Json
) : JournalRepository {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    override fun observeEntries(): Flow<List<JournalEntry>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain(json) } }

    override fun observePendingReviews(): Flow<List<JournalEntry>> =
        dao.observeWithPendingReviews().map { entities ->
            entities.map { it.toDomain(json) }.filter { entry ->
                entry.predictions.any { it.wasCorrect == null }
            }
        }

    override suspend fun getEntry(id: String): JournalEntry? =
        dao.getById(id)?.toDomain(json)

    override suspend fun getEntryByDate(date: String): JournalEntry? =
        dao.getByDate(date)?.toDomain(json)

    override suspend fun saveEntry(entry: JournalEntry) {
        val entity = entry.toEntity(json, isDirty = true)
        dao.upsert(entity)
    }

    override suspend fun deleteEntry(id: String) {
        dao.deleteById(id)
    }

    override suspend fun syncToDrive(): SyncResult {
        if (!driveDataSource.isAuthenticated()) return SyncResult.Error("Not authenticated")

        _syncState.value = SyncState.Syncing
        var pushed = 0

        return try {
            val dirtyEntries = dao.getDirtyEntries()
            for (entity in dirtyEntries) {
                val fileId = driveDataSource.uploadEntry(
                    entryId = entity.id,
                    date = entity.date,
                    jsonContent = entity.jsonPayload,
                    existingFileId = entity.driveFileId
                )
                if (fileId != null) {
                    dao.markSynced(entity.id, fileId)
                    pushed++
                }
            }

            val ts = LocalDateTime.now().toString()
            _syncState.value = SyncState.LastSync(ts, success = true)
            SyncResult.Success(pushed = pushed, pulled = 0)
        } catch (e: Exception) {
            Timber.e(e, "Sync to Drive failed")
            _syncState.value = SyncState.LastSync(LocalDateTime.now().toString(), success = false)
            SyncResult.Error(e.message ?: "Unknown error", e)
        }
    }

    override suspend fun syncFromDrive(): SyncResult {
        if (!driveDataSource.isAuthenticated()) return SyncResult.Error("Not authenticated")

        _syncState.value = SyncState.Syncing
        var pulled = 0

        return try {
            val driveFiles = driveDataSource.listEntryFiles()
            for (driveFile in driveFiles) {
                val jsonContent = driveDataSource.downloadEntry(driveFile.id) ?: continue
                val entry = json.decodeFromString<JournalEntry>(jsonContent)
                val existing = dao.getById(entry.id)

                // Simple last-write-wins merge: Drive wins if local is not dirty
                if (existing == null || !existing.isDirty) {
                    dao.upsert(
                        entry.toEntity(json, isDirty = false)
                            .copy(driveFileId = driveFile.id)
                    )
                    pulled++
                }
            }

            val ts = LocalDateTime.now().toString()
            _syncState.value = SyncState.LastSync(ts, success = true)
            SyncResult.Success(pushed = 0, pulled = pulled)
        } catch (e: Exception) {
            Timber.e(e, "Sync from Drive failed")
            _syncState.value = SyncState.LastSync(LocalDateTime.now().toString(), success = false)
            SyncResult.Error(e.message ?: "Unknown error", e)
        }
    }

    override fun observeSyncState(): Flow<SyncState> = _syncState
}

// ──────────────────────────────────────────────
// Mappers
// ──────────────────────────────────────────────

private fun JournalEntryEntity.toDomain(json: Json): JournalEntry =
    json.decodeFromString<JournalEntry>(jsonPayload).copy(
        isDirty = isDirty,
        driveFileId = driveFileId
    )

private fun JournalEntry.toEntity(json: Json, isDirty: Boolean): JournalEntryEntity =
    JournalEntryEntity(
        id = id,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        jsonPayload = json.encodeToString(this),
        isDirty = isDirty,
        driveFileId = driveFileId
    )
