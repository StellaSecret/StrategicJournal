package com.stellasecret.strategicjournal.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.stellasecret.strategicjournal.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

// DataStore extension — one instance per app
private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_prefs")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideAiDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.aiDataStore

    @Provides
    @Singleton
    @GeminiRetrofit
    fun provideGeminiOkHttp(): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                },
            ).build()

    @Provides
    @Singleton
    fun provideGeminiApi(
        @GeminiRetrofit okHttpClient: OkHttpClient,
        json: Json,
    ): GeminiApi =
        Retrofit
            .Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)

    @Provides
    @Singleton
    fun provideGeminiApiKeyProvider(): GeminiApiKeyProvider =
        object : GeminiApiKeyProvider {
            // Store your key in local.properties as GEMINI_API_KEY=...
            // and expose it via BuildConfig in app/build.gradle.kts:
            //   buildConfigField("String", "GEMINI_API_KEY", "\"${properties["GEMINI_API_KEY"]}\"")
            override val key: String get() = BuildConfig.GEMINI_API_KEY
        }
}
