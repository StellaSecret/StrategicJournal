package com.strategicjournal.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Core journal entry model.
 *
 * A Strategic Journal entry is structured around:
 * - HYPOTHESES: What do I believe to be true today?
 * - DECISIONS: What did I decide and why?
 * - PREDICTIONS: What do I expect to happen (measurable, time-bound)?
 * - REVIEW: Was I right? What did I learn?
 */
@Serializable
data class JournalEntry(
    val id: String,
    val date: String, // ISO-8601: "2024-01-15"
    val createdAt: String, // ISO-8601 datetime
    val updatedAt: String,

    // Core structured sections
    val hypotheses: List<Hypothesis> = emptyList(),
    val decisions: List<Decision> = emptyList(),
    val predictions: List<Prediction> = emptyList(),

    // Free-form context
    val contextNote: String = "",

    // Metadata
    val energyLevel: Int = 5, // 1-10
    val tags: List<String> = emptyList(),

    // Sync state (not serialized to Drive)
    val isDirty: Boolean = false,
    val driveFileId: String? = null
)

@Serializable
data class Hypothesis(
    val id: String,
    val statement: String,
    val domain: HypothesisDomain,
    val confidence: Int, // 0-100
    val validatedAt: String? = null, // when reviewed
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
    val outcomeNote: String? = null, // filled during review
    val reviewedAt: String? = null
)

@Serializable
data class Prediction(
    val id: String,
    val statement: String,
    val expectedOutcome: String,
    val deadline: String, // ISO-8601 date
    val confidence: Int, // 0-100
    val actualOutcome: String? = null,
    val wasCorrect: Boolean? = null,
    val reviewedAt: String? = null,
    val score: Int? = null // Brier-style scoring
)

@Serializable
enum class HypothesisDomain {
    BUSINESS, PERSONAL, PEOPLE, MARKET, TECHNICAL, PHILOSOPHICAL
}

@Serializable
enum class DecisionType {
    STRATEGIC, TACTICAL, PERSONAL, FINANCIAL, RELATIONAL
}

/**
 * Aggregated stats computed from entries for the review screen.
 */
data class CognitiveStats(
    val totalEntries: Int,
    val predictionAccuracy: Float, // 0.0 - 1.0
    val averageConfidenceWhenRight: Float,
    val averageConfidenceWhenWrong: Float,
    val calibrationScore: Float, // how well confidence matches accuracy
    val topDomains: List<Pair<HypothesisDomain, Int>>,
    val decisionVolume: Map<DecisionType, Int>,
    val streakDays: Int
)
