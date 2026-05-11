package com.stellasecret.strategicjournal.data

import android.content.Context
import androidx.room.Room
import com.stellasecret.strategicjournal.data.local.JournalEntryDao
import com.stellasecret.strategicjournal.data.local.StrategicJournalDatabase
import com.stellasecret.strategicjournal.domain.repository.JournalRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): StrategicJournalDatabase =
        Room
            .databaseBuilder(
                context,
                StrategicJournalDatabase::class.java,
                StrategicJournalDatabase.DATABASE_NAME,
            ).build()

    @Provides
    fun provideJournalEntryDao(db: StrategicJournalDatabase): JournalEntryDao = db.journalEntryDao()

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository
}
