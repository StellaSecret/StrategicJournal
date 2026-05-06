package com.stellasecret.strategicjournal.domain.model

import kotlinx.serialization.Serializable

/**
 * Result of an AI-generated strategic review.
 * Stored locally after generation to avoid burning quota on re-renders.
 */
@Serializable
data class AiReview(
    val id: String,
    val generatedAt: String,        // ISO-8601 datetime
    val periodType: ReviewPeriod,
    val periodStart: String,        // ISO-8601 date
    val periodEnd: String,          // ISO-8601 date
    val entryCount: Int,

    // Structured output from Gemini
    val topDecisions: List<ReviewedDecision> = emptyList(),
    val invalidatedHypotheses: List<String> = emptyList(),
    val recurringThemes: List<RecurringTheme> = emptyList(),
    val predictionSummary: PredictionSummary? = null,
    val openLoops: List<String> = emptyList(),

    // Raw markdown fallback if parsing fails
    val rawMarkdown: String = "",

    // Usage tracking
    val usageCount: Int = 1         // always 1 at creation
)

@Serializable
enum class ReviewPeriod { WEEKLY, MONTHLY }

@Serializable
data class ReviewedDecision(
    val statement: String,
    val outcome: String?,           // null if not yet reviewed
    val sentiment: DecisionSentiment
)

@Serializable
enum class DecisionSentiment { POSITIVE, NEGATIVE, NEUTRAL, UNKNOWN }

@Serializable
data class RecurringTheme(
    val theme: String,
    val occurrences: Int,
    val referenceDates: List<String>    // ISO-8601 dates where theme appears
)

@Serializable
data class PredictionSummary(
    val total: Int,
    val resolved: Int,
    val accuracy: Float?,               // null if no resolved predictions
    val averageConfidence: Float?,
    val calibrationNote: String?        // e.g. "overconfident by ~15%"
)

/**
 * Rate-limit state persisted via DataStore.
 * Max 3 generations per 7-day rolling window.
 */
@Serializable
data class ReviewRateLimit(
    val generationTimestamps: List<String> = emptyList()  // ISO-8601 datetimes
) {
    companion object {
        const val MAX_PER_WEEK = 3
    }
}
