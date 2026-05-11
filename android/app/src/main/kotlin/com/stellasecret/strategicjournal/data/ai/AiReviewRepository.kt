package com.stellasecret.strategicjournal.data.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stellasecret.strategicjournal.domain.model.AiReview
import com.stellasecret.strategicjournal.domain.model.DecisionSentiment
import com.stellasecret.strategicjournal.domain.model.JournalEntry
import com.stellasecret.strategicjournal.domain.model.PredictionSummary
import com.stellasecret.strategicjournal.domain.model.RecurringTheme
import com.stellasecret.strategicjournal.domain.model.ReviewPeriod
import com.stellasecret.strategicjournal.domain.model.ReviewRateLimit
import com.stellasecret.strategicjournal.domain.model.ReviewedDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiReviewRepository
    @Inject
    constructor(
        private val geminiApi: GeminiApi,
        private val dataStore: DataStore<Preferences>,
        private val json: Json,
        private val apiKeyProvider: GeminiApiKeyProvider,
    ) {
        companion object {
            private val KEY_RATE_LIMIT = stringPreferencesKey("ai_review_rate_limit")
            private val KEY_LAST_REVIEW = stringPreferencesKey("ai_last_review")
        }

        // ──────────────────────────────────────────────
        // Rate limit
        // ──────────────────────────────────────────────

        fun observeRateLimit(): Flow<ReviewRateLimit> =
            dataStore.data.map { prefs ->
                prefs[KEY_RATE_LIMIT]
                    ?.let { runCatching { json.decodeFromString<ReviewRateLimit>(it) }.getOrNull() }
                    ?: ReviewRateLimit()
            }

        /** Returns how many generations remain this week (0–3). */
        suspend fun remainingGenerations(): Int {
            val limit = observeRateLimit().first()
            val cutoff = LocalDateTime.now().minusDays(7).toString()
            val recent = limit.generationTimestamps.filter { it >= cutoff }
            return (ReviewRateLimit.MAX_PER_WEEK - recent.size).coerceAtLeast(0)
        }

        private suspend fun recordGeneration() {
            dataStore.edit { prefs ->
                val current =
                    prefs[KEY_RATE_LIMIT]
                        ?.let { runCatching { json.decodeFromString<ReviewRateLimit>(it) }.getOrNull() }
                        ?: ReviewRateLimit()

                // Keep only timestamps within the past 7 days + the new one
                val cutoff = LocalDateTime.now().minusDays(7).toString()
                val updated =
                    current.copy(
                        generationTimestamps =
                            current.generationTimestamps
                                .filter { it >= cutoff } + LocalDateTime.now().toString(),
                    )
                prefs[KEY_RATE_LIMIT] = json.encodeToString(ReviewRateLimit.serializer(), updated)
            }
        }

        // ──────────────────────────────────────────────
        // Last review cache (simple — one review stored)
        // ──────────────────────────────────────────────

        fun observeLastReview(): Flow<AiReview?> =
            dataStore.data.map { prefs ->
                prefs[KEY_LAST_REVIEW]
                    ?.let { runCatching { json.decodeFromString<AiReview>(it) }.getOrNull() }
            }

        private suspend fun saveReview(review: AiReview) {
            dataStore.edit { prefs ->
                prefs[KEY_LAST_REVIEW] = json.encodeToString(AiReview.serializer(), review)
            }
        }

        // ──────────────────────────────────────────────
        // Generation
        // ──────────────────────────────────────────────

        sealed class GenerateResult {
            data class Success(
                val review: AiReview,
            ) : GenerateResult()

            data class RateLimitExceeded(
                val remaining: Int = 0,
            ) : GenerateResult()

            data class Error(
                val message: String,
                val cause: Throwable? = null,
            ) : GenerateResult()
        }

        suspend fun generateReview(
            entries: List<JournalEntry>,
            period: ReviewPeriod,
        ): GenerateResult {
            if (remainingGenerations() <= 0) {
                return GenerateResult.RateLimitExceeded()
            }

            val periodEntries = filterByPeriod(entries, period)
            if (periodEntries.isEmpty()) {
                return GenerateResult.Error("No entries found for the selected period.")
            }

            return try {
                val prompt = ReviewPromptBuilder.build(periodEntries, period)
                val request =
                    GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    )

                val response =
                    geminiApi.generateContent(
                        apiKey = apiKeyProvider.key,
                        request = request,
                    )

                val rawText = response.extractText()
                Timber.d("Gemini raw response: $rawText")

                val review = parseResponse(rawText, periodEntries, period)
                recordGeneration()
                saveReview(review)

                GenerateResult.Success(review)
            } catch (e: Exception) {
                Timber.e(e, "Gemini generation failed")
                GenerateResult.Error(e.message ?: "Unknown error", e)
            }
        }

        // ──────────────────────────────────────────────
        // Period filtering
        // ──────────────────────────────────────────────

        private fun filterByPeriod(
            entries: List<JournalEntry>,
            period: ReviewPeriod,
        ): List<JournalEntry> {
            val cutoff =
                when (period) {
                    ReviewPeriod.WEEKLY -> LocalDate.now().minusDays(7)
                    ReviewPeriod.MONTHLY -> LocalDate.now().minusDays(30)
                }
            return entries.filter { LocalDate.parse(it.date) >= cutoff }
        }

        // ──────────────────────────────────────────────
        // Response parsing
        // ──────────────────────────────────────────────

        private fun parseResponse(
            rawText: String,
            entries: List<JournalEntry>,
            period: ReviewPeriod,
        ): AiReview {
            val periodStart =
                when (period) {
                    ReviewPeriod.WEEKLY -> LocalDate.now().minusDays(7)
                    ReviewPeriod.MONTHLY -> LocalDate.now().minusDays(30)
                }

            // Strip potential markdown fences just in case the model doesn't comply
            val cleaned =
                rawText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

            return try {
                val root: JsonObject = json.parseToJsonElement(cleaned).jsonObject

                val topDecisions =
                    root["topDecisions"]?.jsonArray?.map { el ->
                        val obj = el.jsonObject
                        ReviewedDecision(
                            statement = obj["statement"]?.jsonPrimitive?.content ?: "",
                            outcome = obj["outcome"]?.jsonPrimitive?.content?.takeIf { it != "null" },
                            sentiment =
                                runCatching {
                                    DecisionSentiment.valueOf(obj["sentiment"]?.jsonPrimitive?.content ?: "UNKNOWN")
                                }.getOrDefault(DecisionSentiment.UNKNOWN),
                        )
                    } ?: emptyList()

                val invalidated =
                    root["invalidatedHypotheses"]
                        ?.jsonArray
                        ?.map { it.jsonPrimitive.content } ?: emptyList()

                val themes =
                    root["recurringThemes"]?.jsonArray?.map { el ->
                        val obj = el.jsonObject
                        RecurringTheme(
                            theme = obj["theme"]?.jsonPrimitive?.content ?: "",
                            occurrences = obj["occurrences"]?.jsonPrimitive?.intOrNull ?: 0,
                            referenceDates =
                                obj["referenceDates"]
                                    ?.jsonArray
                                    ?.map { it.jsonPrimitive.content } ?: emptyList(),
                        )
                    } ?: emptyList()

                val predSummary =
                    root["predictionSummary"]?.jsonObject?.let { ps ->
                        PredictionSummary(
                            total = ps["total"]?.jsonPrimitive?.intOrNull ?: 0,
                            resolved = ps["resolved"]?.jsonPrimitive?.intOrNull ?: 0,
                            accuracy = ps["accuracy"]?.jsonPrimitive?.floatOrNull,
                            averageConfidence = ps["averageConfidence"]?.jsonPrimitive?.floatOrNull,
                            calibrationNote = ps["calibrationNote"]?.jsonPrimitive?.content?.takeIf { it != "null" },
                        )
                    }

                val openLoops =
                    root["openLoops"]
                        ?.jsonArray
                        ?.map { it.jsonPrimitive.content } ?: emptyList()

                AiReview(
                    id = UUID.randomUUID().toString(),
                    generatedAt = LocalDateTime.now().toString(),
                    periodType = period,
                    periodStart = periodStart.toString(),
                    periodEnd = LocalDate.now().toString(),
                    entryCount = entries.size,
                    topDecisions = topDecisions,
                    invalidatedHypotheses = invalidated,
                    recurringThemes = themes,
                    predictionSummary = predSummary,
                    openLoops = openLoops,
                    rawMarkdown = rawText,
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse Gemini JSON, falling back to raw")
                // Graceful fallback: store raw text, display as-is in UI
                AiReview(
                    id = UUID.randomUUID().toString(),
                    generatedAt = LocalDateTime.now().toString(),
                    periodType = period,
                    periodStart = periodStart.toString(),
                    periodEnd = LocalDate.now().toString(),
                    entryCount = entries.size,
                    rawMarkdown = rawText,
                )
            }
        }
    }

/** Provides the Gemini API key — injected separately so it can be swapped (BuildConfig, etc.) */
interface GeminiApiKeyProvider {
    val key: String
}
