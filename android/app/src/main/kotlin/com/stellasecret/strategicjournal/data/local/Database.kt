package com.stellasecret.strategicjournal.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────
// Entity
// ──────────────────────────────────────────────

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val createdAt: String,
    val updatedAt: String,
    val jsonPayload: String, // Full serialized JournalEntry as JSON
    val isDirty: Boolean = false,
    val driveFileId: String? = null,
)

// ──────────────────────────────────────────────
// DAO
// ──────────────────────────────────────────────

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE isDirty = 1")
    suspend fun getDirtyEntries(): List<JournalEntryEntity>

    /**
     * Entries that have predictions with deadline <= today and not yet reviewed.
     * We filter in the repository layer since predicates live in JSON.
     */
    @Query("SELECT * FROM journal_entries WHERE jsonPayload LIKE '%\"wasCorrect\":null%' ORDER BY date ASC")
    fun observeWithPendingReviews(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("UPDATE journal_entries SET isDirty = 0, driveFileId = :fileId WHERE id = :id")
    suspend fun markSynced(
        id: String,
        fileId: String,
    )

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}

// ──────────────────────────────────────────────
// Database
// ──────────────────────────────────────────────

@Database(
    entities = [JournalEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StrategicJournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        const val DATABASE_NAME = "strategic_journal.db"
    }
}
