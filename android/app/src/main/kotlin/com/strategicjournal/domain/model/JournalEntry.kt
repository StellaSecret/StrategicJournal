package com.strategicjournal.domain.model

import kotlinx.serialization.Serializable

/**
 * Core journal entry model.
 *
 * A Strategic Journal entry is structured around:
 * - HYPOTHESES: What do I believe to be true today?
 * - DECISIONS: What did I decide and why?
 * - PREDICTIONS: What do I expect to happen (measurable, time-bound)?
 */
@Serializable
data class JournalEntry(
    val id: String,
    val date: String,           // ISO-8601: "2024-01-15"
    val createdAt: String,      // ISO-8601 datetime
    val updatedAt: String,

    val hypotheses: List<Hypothesis> = emptyList(),
    val decisions: List<Decision> = emptyList(),
    val predictions: List<Prediction> = emptyList(),

    val contextNote: String = "",
    val energyLevel: Int = 5,   // 1-10
    val tags: List<String> = emptyList(),

    // Sync state (not serialized to Drive — set locally)
    val isDirty: Boolean = false,
    val driveFileId: String? = null
)

@Serializable
data class Hypothesis(
    val id: String,
    val statement: String,
    val domain: HypothesisDomain,
    val confidence: Int,            // 0-100
    val validatedAt: String? = null,
    val wasCorrect: Boolean? = null
)

@Serializable
data class Decision(
    val id: String,
    val statement: String,
    val rationale: String,
    val alternatives: List<String> = emptyList(),
    val decisionType: DecisionType,
    val reversible: Boolean = true,
    val tags: List<String> = emptyList(),

    // Review fields — filled weeks later
    val outcomeNote: String? = null,        // what actually happened
    val wouldRepeat: Boolean? = null,       // would you make the same call again?
    val outcomeRating: Int? = null,         // 1-5: how good was the outcome
    val reviewAfterWeeks: Int = 4,          // when to prompt review (default 4 weeks)
    val reviewedAt: String? = null
)

@Serializable
data class Prediction(
    val id: String,
    val statement: String,
    val expectedOutcome: String,
    val deadline: String,           // ISO-8601 date
    val confidence: Int,            // 0-100
    val tags: List<String> = emptyList(),
    val actualOutcome: String? = null,
    val wasCorrect: Boolean? = null,
    val reviewedAt: String? = null,
    val score: Int? = null          // Brier-style scoring (future)
)

@Serializable
enum class HypothesisDomain {
    BUSINESS, PERSONAL, PEOPLE, MARKET, TECHNICAL, PHILOSOPHICAL
}

@Serializable
enum class DecisionType {
    STRATEGIC, TACTICAL, PERSONAL, FINANCIAL, RELATIONAL
}

// ──────────────────────────────────────────────
// Analytics models
// ──────────────────────────────────────────────

/**
 * Aggregated stats computed from all entries.
 * Computed client-side — no backend needed.
 */
data class CognitiveStats(
    val totalEntries: Int,
    val streakDays: Int,

    // Prediction calibration
    val predictionAccuracy: Float,          // 0.0–1.0
    val averageConfidenceWhenRight: Float,
    val averageConfidenceWhenWrong: Float,
    val calibrationScore: Float,            // 1.0 = perfectly calibrated
    val calibrationBuckets: List<CalibrationBucket>, // for the chart

    // Decision analytics
    val decisionsReviewed: Int,
    val wouldRepeatRate: Float,             // % of reviewed decisions you'd repeat
    val avgOutcomeRating: Float,            // 1–5
    val decisionAccuracyByType: Map<DecisionType, Float>, // wouldRepeat rate per type

    // Tag intelligence
    val predictionAccuracyByTag: Map<String, TagStats>,
    val decisionQualityByTag: Map<String, TagStats>,

    // Nudge flag
    val pendingDecisionReviews: Int,        // decisions past reviewAfterWeeks with no review
    val pendingPredictionReviews: Int
)

/** One bucket in the calibration chart: e.g. "70-79% confidence → 65% accuracy" */
data class CalibrationBucket(
    val confidenceMidpoint: Int,    // 10, 20, 30 ... 90
    val predictedRate: Float,       // midpoint / 100
    val actualRate: Float,          // real accuracy in this bucket
    val sampleSize: Int
)

data class TagStats(
    val tag: String,
    val accuracy: Float,            // for predictions: % correct; for decisions: % wouldRepeat
    val sampleSize: Int
)
