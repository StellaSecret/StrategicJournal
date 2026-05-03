package com.stellasecret.strategicjournal.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun buildDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("StrategicJournal").build()
    }

    suspend fun uploadEntry(
        entryId: String,
        date: String,
        jsonContent: String,
        existingFileId: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val drive = buildDriveService() ?: return@withContext null
            val content = ByteArrayInputStream(jsonContent.toByteArray())
            val mediaContent = com.google.api.client.http.InputStreamContent("application/json", content)

            if (existingFileId != null) {
                drive.files().update(existingFileId, null, mediaContent).execute()
                Timber.d("Drive: updated entry $entryId")
                existingFileId
            } else {
                val fileMetadata = File().apply {
                    name = "entry_${date}_${entryId}.json"
                    parents = listOf("appDataFolder")
                }
                val file = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                Timber.d("Drive: created entry $entryId → ${file.id}")
                file.id
            }
        } catch (e: Exception) {
            Timber.e(e, "Drive upload failed for entry $entryId")
            null
        }
    }

    suspend fun listEntryFiles(): List<DriveFile> = withContext(Dispatchers.IO) {
        try {
            val drive = buildDriveService() ?: return@withContext emptyList()
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, modifiedTime)")
                .setQ("name contains 'entry_'")
                .execute()
            result.files.map { DriveFile(it.id, it.name, it.modifiedTime?.toStringRfc3339()) }
        } catch (e: Exception) {
            Timber.e(e, "Drive list failed")
            emptyList()
        }
    }

    suspend fun downloadEntry(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val drive = buildDriveService() ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            Timber.e(e, "Drive download failed for fileId $fileId")
            null
        }
    }

    suspend fun deleteEntry(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = buildDriveService() ?: return@withContext false
            drive.files().delete(fileId).execute()
            true
        } catch (e: Exception) {
            Timber.e(e, "Drive delete failed for fileId $fileId")
            false
        }
    }

    fun isAuthenticated(): Boolean =
        GoogleSignIn.getLastSignedInAccount(context) != null
}

data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String?
)
