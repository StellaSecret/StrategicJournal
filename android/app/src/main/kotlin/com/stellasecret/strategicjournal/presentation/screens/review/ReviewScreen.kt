package com.stellasecret.strategicjournal.presentation.screens.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stellasecret.strategicjournal.domain.model.Decision
import com.stellasecret.strategicjournal.domain.model.JournalEntry
import com.stellasecret.strategicjournal.domain.model.Prediction
import com.stellasecret.strategicjournal.presentation.theme.JournalColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(ReviewTab.PREDICTIONS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Tab row
            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = JournalColors.Gold
            ) {
                ReviewTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(t.label, style = MaterialTheme.typography.labelMedium)
                                val badge = when (t) {
                                    ReviewTab.PREDICTIONS -> state.pendingPredictions.size
                                    ReviewTab.DECISIONS -> state.pendingDecisions.size
                                    ReviewTab.ANALYTICS -> 0
                                }
                                if (badge > 0) {
                                    Badge(containerColor = if (t == ReviewTab.DECISIONS) JournalColors.Gold else JournalColors.Signal) {
                                        Text("$badge", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            when (tab) {
                ReviewTab.PREDICTIONS -> PredictionsTab(state, viewModel)
                ReviewTab.DECISIONS -> DecisionsTab(state, viewModel)
                ReviewTab.ANALYTICS -> AnalyticsTab(state)
            }
        }
    }
}

enum class ReviewTab(val label: String) {
    PREDICTIONS("Predictions"),
    DECISIONS("Decisions"),
    ANALYTICS("Analytics")
}

// ──────────────────────────────────────────────
// Predictions tab
// ──────────────────────────────────────────────

@Composable
private fun PredictionsTab(state: ReviewUiState, viewModel: ReviewViewModel) {
    if (state.pendingPredictions.isEmpty() && state.reviewedPredictions.isEmpty()) {
        EmptyState("No predictions to review yet")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.pendingPredictions.isNotEmpty()) {
            item { SectionLabel("Pending") }
            items(state.pendingPredictions, key = { it.prediction.id }) { item ->
                PredictionReviewCard(entry = item.entry, prediction = item.prediction, onReview = viewModel::reviewPrediction)
            }
        }
        if (state.reviewedPredictions.isNotEmpty()) {
            item { SectionLabel("Reviewed") }
            items(state.reviewedPredictions, key = { it.prediction.id }) { item ->
                ReviewedPredictionCard(item.prediction)
            }
        }
    }
}

@Composable
private fun PredictionReviewCard(entry: JournalEntry, prediction: Prediction, onReview: (String, String, Boolean, String) -> Unit) {
    var outcome by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.date, style = MaterialTheme.typography.labelSmall, color = JournalColors.Gold)
            Spacer(Modifier.height(6.dp))
            Text(prediction.statement, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("Expected: ${prediction.expectedOutcome}", style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
            Spacer(Modifier.height(4.dp))
            Text("Confidence: ${prediction.confidence}%", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = outcome,
                onValueChange = { outcome = it },
                placeholder = { Text("What actually happened? (optional)", color = JournalColors.Slate) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JournalColors.Gold, unfocusedBorderColor = JournalColors.InkMuted)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { saving = true; onReview(entry.id, prediction.id, true, outcome) },
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Correct)
                ) { Text("✓ Correct") }
                Button(
                    onClick = { saving = true; onReview(entry.id, prediction.id, false, outcome) },
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Wrong)
                ) { Text("✗ Wrong") }
            }
        }
    }
}

@Composable
private fun ReviewedPredictionCard(prediction: Prediction) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(prediction.statement, style = MaterialTheme.typography.bodyMedium)
                if (!prediction.actualOutcome.isNullOrBlank()) {
                    Text("→ ${prediction.actualOutcome}", style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
                }
            }
            Text(
                if (prediction.wasCorrect == true) "✓" else "✗",
                color = if (prediction.wasCorrect == true) JournalColors.Correct else JournalColors.Wrong,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// ──────────────────────────────────────────────
// Decisions tab
// ──────────────────────────────────────────────

@Composable
private fun DecisionsTab(state: ReviewUiState, viewModel: ReviewViewModel) {
    if (state.pendingDecisions.isEmpty() && state.reviewedDecisions.isEmpty()) {
        EmptyState(
            message = "No decisions to review yet",
            subMessage = state.nextDecisionReviewInfo.ifBlank {
                "Decisions appear here after their review window passes (default: 4 weeks after entry)."
            }
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.pendingDecisions.isNotEmpty()) {
            item { SectionLabel("Pending") }
            items(state.pendingDecisions, key = { it.decision.id }) { item ->
                DecisionReviewCard(entry = item.entry, decision = item.decision, onReview = viewModel::reviewDecision)
            }
        }
        if (state.reviewedDecisions.isNotEmpty()) {
            item { SectionLabel("Reviewed") }
            items(state.reviewedDecisions, key = { it.decision.id }) { item ->
                ReviewedDecisionCard(item.decision)
            }
        }
    }
}

@Composable
private fun DecisionReviewCard(entry: JournalEntry, decision: Decision, onReview: (String, String, String, Boolean, Int) -> Unit) {
    var outcomeNote by remember { mutableStateOf("") }
    var wouldRepeat by remember { mutableStateOf<Boolean?>(null) }
    var rating by remember { mutableStateOf(3) }
    var saving by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.date, style = MaterialTheme.typography.labelSmall, color = JournalColors.Gold)
            Spacer(Modifier.height(6.dp))
            Text(decision.statement, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(decision.rationale, style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = outcomeNote,
                onValueChange = { outcomeNote = it },
                placeholder = { Text("What actually happened?", color = JournalColors.Slate) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JournalColors.Gold, unfocusedBorderColor = JournalColors.InkMuted)
            )
            Spacer(Modifier.height(12.dp))

            // Star rating
            Text("Outcome quality", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { star ->
                    Box(modifier = Modifier.clickable { rating = star }.padding(4.dp)) {
                        Text(
                            "★",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (star <= rating) JournalColors.Gold else JournalColors.InkMuted
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Would repeat
            Text("Would you make the same call again?", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { wouldRepeat = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (wouldRepeat == true) JournalColors.Correct.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        contentColor = if (wouldRepeat == true) JournalColors.Correct else JournalColors.Slate
                    )
                ) { Text("↺ Yes") }
                OutlinedButton(
                    onClick = { wouldRepeat = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (wouldRepeat == false) JournalColors.Wrong.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        contentColor = if (wouldRepeat == false) JournalColors.Wrong else JournalColors.Slate
                    )
                ) { Text("✗ No") }
            }
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val repeat = wouldRepeat ?: return@Button
                    saving = true
                    onReview(entry.id, decision.id, outcomeNote, repeat, rating)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving && wouldRepeat != null,
                colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Gold)
            ) { Text("Save review", color = JournalColors.Ink) }
        }
    }
}

@Composable
private fun ReviewedDecisionCard(decision: Decision) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(decision.statement, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (!decision.outcomeNote.isNullOrBlank()) {
                    Text("→ ${decision.outcomeNote}", style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (decision.wouldRepeat == true) "↺" else "✗",
                    color = if (decision.wouldRepeat == true) JournalColors.Correct else JournalColors.Wrong,
                    style = MaterialTheme.typography.titleMedium
                )
                decision.outcomeRating?.let { r ->
                    Text("$r★", style = MaterialTheme.typography.labelSmall, color = JournalColors.Gold)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Analytics tab (simple stats)
// ──────────────────────────────────────────────

@Composable
private fun AnalyticsTab(state: ReviewUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Based on your reviewed items", style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Prediction accuracy", "${(state.predictionAccuracy * 100).toInt()}%", modifier = Modifier.weight(1f))
                StatCard("Would repeat", "${(state.wouldRepeatRate * 100).toInt()}%", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Avg outcome", if (state.avgOutcomeRating > 0) "${"%.1f".format(state.avgOutcomeRating)}★" else "—", modifier = Modifier.weight(1f))
                StatCard("Calibration", "${(state.calibrationScore * 100).toInt()}%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = JournalColors.Gold, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun EmptyState(message: String, subMessage: String = "") {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = JournalColors.Slate)
            if (subMessage.isNotBlank()) {
                Text(subMessage, style = MaterialTheme.typography.bodySmall, color = JournalColors.InkMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
