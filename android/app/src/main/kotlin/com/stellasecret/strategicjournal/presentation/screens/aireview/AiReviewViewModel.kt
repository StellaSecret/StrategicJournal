package com.stellasecret.strategicjournal.presentation.screens.aireview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellasecret.strategicjournal.data.ai.AiReviewRepository
import com.stellasecret.strategicjournal.data.ai.AiReviewRepository.GenerateResult
import com.stellasecret.strategicjournal.domain.model.AiReview
import com.stellasecret.strategicjournal.domain.model.ReviewPeriod
import com.stellasecret.strategicjournal.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiReviewUiState(
    val isLoading: Boolean = false,
    val remainingGenerations: Int = ReviewPeriod.entries.size, // default optimistic
    val lastReview: AiReview? = null,
    val selectedPeriod: ReviewPeriod = ReviewPeriod.WEEKLY,
    val error: String? = null,
    val rateLimitError: Boolean = false
)

@HiltViewModel
class AiReviewViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val aiReviewRepository: AiReviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AiReviewUiState())
    val state: StateFlow<AiReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                aiReviewRepository.observeLastReview(),
                aiReviewRepository.observeRateLimit()
            ) { lastReview, rateLimit ->
                val cutoff = java.time.LocalDateTime.now().minusDays(7).toString()
                val recentCount = rateLimit.generationTimestamps.count { it >= cutoff }
                val remaining = (com.stellasecret.strategicjournal.domain.model.ReviewRateLimit.MAX_PER_WEEK - recentCount)
                    .coerceAtLeast(0)
                Pair(lastReview, remaining)
            }.collect { (lastReview, remaining) ->
                _state.value = _state.value.copy(
                    lastReview = lastReview,
                    remainingGenerations = remaining
                )
            }
        }
    }

    fun selectPeriod(period: ReviewPeriod) {
        _state.value = _state.value.copy(selectedPeriod = period, error = null, rateLimitError = false)
    }

    fun generateReview() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, rateLimitError = false)

            val entries = journalRepository.observeEntries()
                .let { flow ->
                    var result = emptyList<com.stellasecret.strategicjournal.domain.model.JournalEntry>()
                    val job = launch { flow.collect { result = it } }
                    // Collect first emission only
                    kotlinx.coroutines.delay(100)
                    job.cancel()
                    result
                }

            when (val result = aiReviewRepository.generateReview(entries, _state.value.selectedPeriod)) {
                is GenerateResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        lastReview = result.review
                    )
                }
                is GenerateResult.RateLimitExceeded -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        rateLimitError = true,
                        remainingGenerations = 0
                    )
                }
                is GenerateResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null, rateLimitError = false)
    }
}
