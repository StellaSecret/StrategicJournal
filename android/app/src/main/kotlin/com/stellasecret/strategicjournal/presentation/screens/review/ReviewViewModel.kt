package com.stellasecret.strategicjournal.presentation.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellasecret.strategicjournal.domain.model.Decision
import com.stellasecret.strategicjournal.domain.model.JournalEntry
import com.stellasecret.strategicjournal.domain.model.Prediction
import com.stellasecret.strategicjournal.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class EntryPrediction(
    val entry: JournalEntry,
    val prediction: Prediction,
)

data class EntryDecision(
    val entry: JournalEntry,
    val decision: Decision,
)

data class ReviewUiState(
    val pendingPredictions: List<EntryPrediction> = emptyList(),
    val reviewedPredictions: List<EntryPrediction> = emptyList(),
    val pendingDecisions: List<EntryDecision> = emptyList(),
    val reviewedDecisions: List<EntryDecision> = emptyList(),
    val nextDecisionReviewInfo: String = "",
    // Analytics
    val predictionAccuracy: Float = 0f,
    val wouldRepeatRate: Float = 0f,
    val avgOutcomeRating: Float = 0f,
    val calibrationScore: Float = 0f,
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ReviewUiState())
        val state: StateFlow<ReviewUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                repository.observeEntries().collect { entries ->
                    _state.value = buildState(entries)
                }
            }
        }

        private fun buildState(entries: List<JournalEntry>): ReviewUiState {
            val today = LocalDate.now()

            // ── Predictions ──────────────────────────────
            val pendingPredictions = mutableListOf<EntryPrediction>()
            val reviewedPredictions = mutableListOf<EntryPrediction>()

            for (entry in entries) {
                for (prediction in entry.predictions) {
                    val deadline = LocalDate.parse(prediction.deadline)
                    when {
                        prediction.wasCorrect != null ->
                            reviewedPredictions.add(EntryPrediction(entry, prediction))
                        deadline.isBefore(today) || deadline == today ->
                            pendingPredictions.add(EntryPrediction(entry, prediction))
                    }
                }
            }

            // ── Decisions ────────────────────────────────
            val pendingDecisions = mutableListOf<EntryDecision>()
            val reviewedDecisions = mutableListOf<EntryDecision>()

            for (entry in entries) {
                val entryDate = LocalDate.parse(entry.date)
                for (decision in entry.decisions) {
                    val reviewDue = entryDate.plusWeeks(decision.reviewAfterWeeks.toLong())
                    when {
                        decision.reviewedAt != null ->
                            reviewedDecisions.add(EntryDecision(entry, decision))
                        !reviewDue.isAfter(today) ->
                            pendingDecisions.add(EntryDecision(entry, decision))
                    }
                }
            }

            // ── Analytics ────────────────────────────────
            val reviewed = reviewedPredictions.map { it.prediction }
            val predictionAccuracy =
                if (reviewed.isNotEmpty()) {
                    reviewed.count { it.wasCorrect == true }.toFloat() / reviewed.size
                } else {
                    0f
                }

            val reviewedDec = reviewedDecisions.map { it.decision }
            val wouldRepeatRate =
                if (reviewedDec.isNotEmpty()) {
                    reviewedDec.count { it.wouldRepeat == true }.toFloat() / reviewedDec.size
                } else {
                    0f
                }

            val ratedDec = reviewedDec.filter { it.outcomeRating != null }
            val avgOutcomeRating =
                if (ratedDec.isNotEmpty()) {
                    ratedDec.sumOf { it.outcomeRating!!.toDouble() }.toFloat() / ratedDec.size
                } else {
                    0f
                }

            val avgConf =
                if (reviewed.isNotEmpty()) {
                    reviewed.sumOf { it.confidence.toDouble() }.toFloat() / reviewed.size / 100f
                } else {
                    0f
                }
            val calibrationScore = 1f - kotlin.math.abs(avgConf - predictionAccuracy)

            // Find the soonest upcoming decision review
            val nextReview =
                entries
                    .flatMap { entry ->
                        entry.decisions
                            .filter { it.reviewedAt == null }
                            .map { d -> LocalDate.parse(entry.date).plusWeeks(d.reviewAfterWeeks.toLong()) }
                    }.filter { it.isAfter(today) }
                    .minOrNull()

            val nextReviewInfo =
                if (nextReview != null) {
                    val daysUntil = today.until(nextReview, ChronoUnit.DAYS)
                    "Next review due in $daysUntil day${if (daysUntil == 1L) "" else "s"} ($nextReview)"
                } else {
                    ""
                }

            return ReviewUiState(
                pendingPredictions = pendingPredictions,
                reviewedPredictions = reviewedPredictions,
                pendingDecisions = pendingDecisions,
                reviewedDecisions = reviewedDecisions,
                nextDecisionReviewInfo = nextReviewInfo,
                predictionAccuracy = predictionAccuracy,
                wouldRepeatRate = wouldRepeatRate,
                avgOutcomeRating = avgOutcomeRating,
                calibrationScore = calibrationScore,
            )
        }

        fun reviewPrediction(
            entryId: String,
            predictionId: String,
            wasCorrect: Boolean,
            actualOutcome: String,
        ) {
            viewModelScope.launch {
                val entry = repository.getEntry(entryId) ?: return@launch
                val updated =
                    entry.copy(
                        updatedAt = LocalDateTime.now().toString(),
                        predictions =
                            entry.predictions.map { p ->
                                if (p.id == predictionId) {
                                    p.copy(
                                        wasCorrect = wasCorrect,
                                        actualOutcome = actualOutcome.ifBlank { null },
                                        reviewedAt = LocalDateTime.now().toString(),
                                    )
                                } else {
                                    p
                                }
                            },
                    )
                repository.saveEntry(updated)
            }
        }

        fun reviewDecision(
            entryId: String,
            decisionId: String,
            outcomeNote: String,
            wouldRepeat: Boolean,
            rating: Int,
        ) {
            viewModelScope.launch {
                val entry = repository.getEntry(entryId) ?: return@launch
                val updated =
                    entry.copy(
                        updatedAt = LocalDateTime.now().toString(),
                        decisions =
                            entry.decisions.map { d ->
                                if (d.id == decisionId) {
                                    d.copy(
                                        outcomeNote = outcomeNote.ifBlank { null },
                                        wouldRepeat = wouldRepeat,
                                        outcomeRating = rating,
                                        reviewedAt = LocalDateTime.now().toString(),
                                    )
                                } else {
                                    d
                                }
                            },
                    )
                repository.saveEntry(updated)
            }
        }
    }
