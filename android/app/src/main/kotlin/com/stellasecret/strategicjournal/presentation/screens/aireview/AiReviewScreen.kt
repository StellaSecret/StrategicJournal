package com.stellasecret.strategicjournal.presentation.screens.aireview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stellasecret.strategicjournal.domain.model.AiReview
import com.stellasecret.strategicjournal.domain.model.DecisionSentiment
import com.stellasecret.strategicjournal.domain.model.RecurringTheme
import com.stellasecret.strategicjournal.domain.model.ReviewPeriod
import com.stellasecret.strategicjournal.domain.model.ReviewedDecision
import com.stellasecret.strategicjournal.presentation.theme.JournalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(
    onBack: () -> Unit,
    viewModel: AiReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = JournalColors.Gold,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "AI Review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Period selector + generate button
            item {
                GenerateCard(
                    state = state,
                    onPeriodSelect = viewModel::selectPeriod,
                    onGenerate = viewModel::generateReview,
                )
            }

            // Error banners
            if (state.error != null) {
                item { ErrorBanner(message = state.error!!, onDismiss = viewModel::dismissError) }
            }
            if (state.rateLimitError) {
                item { RateLimitBanner() }
            }

            // Review content
            state.lastReview?.let { review ->
                item { ReviewHeader(review) }

                if (review.topDecisions.isNotEmpty()) {
                    item { SectionTitle("Top Decisions") }
                    items(review.topDecisions) { decision -> DecisionItem(decision) }
                }

                if (review.recurringThemes.isNotEmpty()) {
                    item { SectionTitle("Recurring Themes") }
                    items(review.recurringThemes) { theme -> ThemeItem(theme) }
                }

                review.predictionSummary?.let { ps ->
                    if (ps.total > 0) {
                        item {
                            SectionTitle("Prediction Summary")
                            PredictionSummaryCard(ps)
                        }
                    }
                }

                if (review.invalidatedHypotheses.isNotEmpty()) {
                    item { SectionTitle("Invalidated Hypotheses") }
                    items(review.invalidatedHypotheses) { h -> BulletItem(h) }
                }

                if (review.openLoops.isNotEmpty()) {
                    item { SectionTitle("Open Loops") }
                    items(review.openLoops) { loop -> BulletItem(loop, accent = JournalColors.Signal) }
                }

                // Raw fallback if structured parsing failed
                if (review.topDecisions.isEmpty() &&
                    review.recurringThemes.isEmpty() &&
                    review.rawMarkdown.isNotBlank()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                review.rawMarkdown,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = JournalColors.Slate,
                            )
                        }
                    }
                }
            }

            // Empty state
            if (state.lastReview == null && !state.isLoading) {
                item { EmptyReviewState() }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Generate card
// ──────────────────────────────────────────────

@Composable
private fun GenerateCard(
    state: AiReviewUiState,
    onPeriodSelect: (ReviewPeriod) -> Unit,
    onGenerate: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Period selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewPeriod.entries.forEach { period ->
                    val selected = state.selectedPeriod == period
                    FilterChip(
                        selected = selected,
                        onClick = { onPeriodSelect(period) },
                        label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JournalColors.Gold,
                                selectedLabelColor = JournalColors.Ink,
                            ),
                    )
                }
            }

            // Quota indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) { index ->
                    val filled = index < state.remainingGenerations
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (filled) JournalColors.Gold else JournalColors.InkMuted),
                    )
                }
                Text(
                    "${state.remainingGenerations}/3 generations remaining this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = JournalColors.Slate,
                )
            }

            // Generate button
            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && state.remainingGenerations > 0,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = JournalColors.Gold,
                        disabledContainerColor = JournalColors.InkMuted,
                    ),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = JournalColors.Ink,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Analyzing...", color = JournalColors.Ink)
                } else {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = JournalColors.Ink,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generate ${state.selectedPeriod.name.lowercase()} review", color = JournalColors.Ink)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Review sections
// ──────────────────────────────────────────────

@Composable
private fun ReviewHeader(review: AiReview) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "${review.periodType.name.lowercase().replaceFirstChar { it.uppercase() }} Review",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${review.periodStart} → ${review.periodEnd}",
                style = MaterialTheme.typography.labelSmall,
                color = JournalColors.Slate,
            )
        }
        Text(
            "${review.entryCount} entries",
            style = MaterialTheme.typography.labelSmall,
            color = JournalColors.Gold,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = JournalColors.Slate,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun DecisionItem(decision: ReviewedDecision) {
    val accentColor =
        when (decision.sentiment) {
            DecisionSentiment.POSITIVE -> JournalColors.Correct
            DecisionSentiment.NEGATIVE -> JournalColors.Wrong
            else -> JournalColors.Slate
        }
    val sentimentIcon =
        when (decision.sentiment) {
            DecisionSentiment.POSITIVE -> "↑"
            DecisionSentiment.NEGATIVE -> "↓"
            else -> "→"
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(sentimentIcon, color = accentColor, style = MaterialTheme.typography.titleMedium)
            Column {
                Text(decision.statement, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                decision.outcome?.let {
                    Text("→ $it", style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
                }
            }
        }
    }
}

@Composable
private fun ThemeItem(theme: RecurringTheme) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.theme, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    theme.referenceDates.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = JournalColors.Slate,
                )
            }
            Surface(
                color = JournalColors.Gold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    "×${theme.occurrences}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = JournalColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PredictionSummaryCard(ps: com.stellasecret.strategicjournal.domain.model.PredictionSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItem("Total", "${ps.total}")
                StatItem("Resolved", "${ps.resolved}")
                ps.accuracy?.let { StatItem("Accuracy", "${(it * 100).toInt()}%") }
                ps.averageConfidence?.let { StatItem("Avg confidence", "${(it * 100).toInt()}%") }
            }
            ps.calibrationNote?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = JournalColors.Signal)
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = JournalColors.Gold,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
    }
}

@Composable
private fun BulletItem(
    text: String,
    accent: Color = JournalColors.Slate,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = accent, style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ──────────────────────────────────────────────
// Error / empty states
// ──────────────────────────────────────────────

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JournalColors.Wrong.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = JournalColors.Wrong,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text("Dismiss", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun RateLimitBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = JournalColors.Gold.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Weekly limit reached",
                style = MaterialTheme.typography.labelMedium,
                color = JournalColors.Gold,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "You've used all 3 generations for this week. Resets on a 7-day rolling basis.",
                style = MaterialTheme.typography.bodySmall,
                color = JournalColors.Slate,
            )
        }
    }
}

@Composable
private fun EmptyReviewState() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = JournalColors.InkMuted,
                modifier = Modifier.size(40.dp),
            )
            Text("No review generated yet", style = MaterialTheme.typography.bodyMedium, color = JournalColors.Slate)
            Text(
                "Select a period and hit Generate.",
                style = MaterialTheme.typography.bodySmall,
                color = JournalColors.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
