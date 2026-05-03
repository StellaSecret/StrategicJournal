package com.stellasecret.strategicjournal.data.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.http.HttpRequestInitializer
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

private const val TAG = "SJ_DRIVE"

@Suppress("DEPRECATION")
@Singleton
class GoogleDriveDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Build Drive service using a raw OAuth2 token from GoogleAuthUtil.
     * This avoids GoogleAccountCredential which has internal GMS class conflicts.
     */
    private suspend fun buildDriveService(): Drive? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: run {
            Log.e(TAG, "buildDriveService: no signed-in account")
            return@withContext null
        }

        val androidAccount = account.account ?: run {
            Log.e(TAG, "buildDriveService: account.account is null")
            return@withContext null
        }

        try {
            // Get OAuth2 token directly — no internal GMS classes needed
            val scope = "oauth2:${DriveScopes.DRIVE_APPDATA}"
            val token = GoogleAuthUtil.getToken(context, androidAccount, scope)
            Log.d(TAG, "buildDriveService: token obtained for ${account.email}")

            val initializer = HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
                request.connectTimeout = 30_000
                request.readTimeout = 30_000
            }

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                initializer
            ).setApplicationName("StrategicJournal").build()
        } catch (e: Exception) {
            Log.e(TAG, "buildDriveService: failed to get token: ${e.message}", e)
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
            Log.d(TAG, "listEntryFiles: found ${result.files.size} files")
            result.files.map { DriveFile(it.id, it.name, it.modifiedTime?.toStringRfc3339()) }
        } catch (e: Exception) {
            Log.e(TAG, "listEntryFiles failed: ${e.message}", e)
            emptyList()
        }
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
            val mediaContent = com.google.api.client.http.InputStreamContent(
                "application/json", content
            )

            if (existingFileId != null) {
                drive.files().update(existingFileId, null, mediaContent).execute()
                Log.d(TAG, "uploadEntry: updated $entryId")
                existingFileId
            } else {
                val fileMetadata = File().apply {
                    name = "entry_${date}_${entryId}.json"
                    parents = listOf("appDataFolder")
                }
                val file = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                Log.d(TAG, "uploadEntry: created $entryId → ${file.id}")
                file.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadEntry failed for $entryId: ${e.message}", e)
            null
        }
    }

    suspend fun downloadEntry(fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val drive = buildDriveService() ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            Log.e(TAG, "downloadEntry failed for $fileId: ${e.message}", e)
            null
        }
    }

    suspend fun deleteEntry(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = buildDriveService() ?: return@withContext false
            drive.files().delete(fileId).execute()
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteEntry failed for $fileId: ${e.message}", e)
            false
        }
    }

    fun isAuthenticated(): Boolean =
        GoogleSignIn.getLastSignedInAccount(context) != null

    fun hasDriveScope(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        val driveScope = com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_APPDATA)
        return GoogleSignIn.hasPermissions(account, driveScope)
    }
}

data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String?
)
