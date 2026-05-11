package com.stellasecret.strategicjournal.presentation.screens.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellasecret.strategicjournal.domain.model.*
import com.stellasecret.strategicjournal.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EntryViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val entryId: String? = savedStateHandle["entryId"]

        private val _state = MutableStateFlow(EntryUiState())
        val state: StateFlow<EntryUiState> = _state.asStateFlow()

        private val _events = MutableSharedFlow<EntryEvent>()
        val events: SharedFlow<EntryEvent> = _events

        init {
            loadEntry()
        }

        private fun loadEntry() {
            viewModelScope.launch {
                val entry =
                    if (entryId != null) {
                        repository.getEntry(entryId)
                    } else {
                        repository.getEntryByDate(LocalDate.now().toString())
                    }

                if (entry != null) {
                    _state.update { it.copy(entry = entry, isEditing = false) }
                } else {
                    _state.update {
                        it.copy(
                            entry =
                                JournalEntry(
                                    id = UUID.randomUUID().toString(),
                                    date = LocalDate.now().toString(),
                                    createdAt = LocalDateTime.now().toString(),
                                    updatedAt = LocalDateTime.now().toString(),
                                ),
                            isEditing = true,
                        )
                    }
                }
            }
        }

        fun addHypothesis(
            statement: String,
            domain: HypothesisDomain,
            confidence: Int,
        ) {
            val hypothesis =
                Hypothesis(
                    id = UUID.randomUUID().toString(),
                    statement = statement,
                    domain = domain,
                    confidence = confidence,
                )
            _state.update { s ->
                s.copy(
                    entry =
                        s.entry?.copy(
                            hypotheses = s.entry.hypotheses + hypothesis,
                            updatedAt = LocalDateTime.now().toString(),
                        ),
                )
            }
        }

        fun addDecision(
            statement: String,
            rationale: String,
            type: DecisionType,
            reversible: Boolean,
        ) {
            val decision =
                Decision(
                    id = UUID.randomUUID().toString(),
                    statement = statement,
                    rationale = rationale,
                    alternatives = emptyList(),
                    decisionType = type,
                    reversible = reversible,
                    tags = emptyList(),
                    reviewAfterWeeks = 4,
                )
            _state.update { s ->
                s.copy(
                    entry =
                        s.entry?.copy(
                            decisions = s.entry.decisions + decision,
                            updatedAt = LocalDateTime.now().toString(),
                        ),
                )
            }
        }

        fun addPrediction(
            statement: String,
            expectedOutcome: String,
            deadline: String,
            confidence: Int,
        ) {
            val prediction =
                Prediction(
                    id = UUID.randomUUID().toString(),
                    statement = statement,
                    expectedOutcome = expectedOutcome,
                    deadline = deadline,
                    confidence = confidence,
                    tags = emptyList(),
                )
            _state.update { s ->
                s.copy(
                    entry =
                        s.entry?.copy(
                            predictions = s.entry.predictions + prediction,
                            updatedAt = LocalDateTime.now().toString(),
                        ),
                )
            }
        }

        fun removeHypothesis(id: String) {
            _state.update { s ->
                s.copy(
                    entry =
                        s.entry?.copy(
                            hypotheses = s.entry.hypotheses.filter { it.id != id },
                        ),
                )
            }
        }

        fun removeDecision(id: String) {
            _state.update { s ->
                s.copy(
                    entry =
                        s.entry?.copy(
                            decisions = s.entry.decisions.filter { it.id != id },
                        ),
                )
            }
        }

        fun removePrediction(id: String) {
            _state.update { s ->
                s.copy(
                    entry =
                        s.entry?.copy(
                            predictions = s.entry.predictions.filter { it.id != id },
                        ),
                )
            }
        }

        fun updateContextNote(note: String) {
            _state.update { s ->
                s.copy(entry = s.entry?.copy(contextNote = note))
            }
        }

        fun updateEnergyLevel(level: Int) {
            _state.update { s ->
                s.copy(entry = s.entry?.copy(energyLevel = level))
            }
        }

        fun saveEntry() {
            viewModelScope.launch {
                val entry = _state.value.entry ?: return@launch
                _state.update { it.copy(isSaving = true) }
                repository.saveEntry(entry)
                _state.update { it.copy(isSaving = false) }
                _events.emit(EntryEvent.Saved)
            }
        }
    }

data class EntryUiState(
    val entry: JournalEntry? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val activeSheet: BottomSheetType? = null,
)

enum class BottomSheetType { HYPOTHESIS, DECISION, PREDICTION }

sealed class EntryEvent {
    object Saved : EntryEvent()
}
