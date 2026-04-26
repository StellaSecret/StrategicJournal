package com.strategicjournal.presentation.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strategicjournal.domain.model.JournalEntry
import com.strategicjournal.domain.repository.SyncState
import com.strategicjournal.presentation.theme.JournalColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEntry: (entryId: String?) -> Unit,
    onNavigateToReview: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val pendingReviews by viewModel.pendingReviews.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Strategic Journal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    SyncIndicator(syncState = syncState, onSync = viewModel::syncNow)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEntry(viewModel.todayEntryId()) },
                containerColor = JournalColors.Gold,
                contentColor = JournalColors.Ink
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New entry")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pending reviews banner
            if (pendingReviews.isNotEmpty()) {
                item {
                    PendingReviewBanner(
                        count = pendingReviews.size,
                        onClick = onNavigateToReview
                    )
                }
            }

            // Entries list
            items(entries, key = { it.id }) { entry ->
                EntryCard(
                    entry = entry,
                    onClick = { onNavigateToEntry(entry.id) }
                )
            }

            if (entries.isEmpty()) {
                item { EmptyState() }
            }
        }
    }
}

@Composable
private fun PendingReviewBanner(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = JournalColors.Signal.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = JournalColors.Signal
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$count prediction${if (count > 1) "s" else ""} to review",
                    style = MaterialTheme.typography.titleMedium,
                    color = JournalColors.Signal
                )
                Text(
                    "Time to close the loop",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = JournalColors.Signal)
        }
    }
}

@Composable
private fun EntryCard(entry: JournalEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = JournalColors.Gold
                )
                if (entry.isDirty) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Not synced",
                        tint = JournalColors.Slate,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Summary chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (entry.hypotheses.isNotEmpty()) {
                    StatChip(
                        count = entry.hypotheses.size,
                        label = "H",
                        color = JournalColors.Sage
                    )
                }
                if (entry.decisions.isNotEmpty()) {
                    StatChip(
                        count = entry.decisions.size,
                        label = "D",
                        color = JournalColors.Gold
                    )
                }
                if (entry.predictions.isNotEmpty()) {
                    StatChip(
                        count = entry.predictions.size,
                        label = "P",
                        color = JournalColors.Signal
                    )
                }
            }

            if (entry.contextNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    entry.contextNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun StatChip(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SyncIndicator(syncState: SyncState, onSync: () -> Unit) {
    when (syncState) {
        is SyncState.Syncing -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = JournalColors.Gold
        )
        else -> IconButton(onClick = onSync) {
            Icon(
                Icons.Default.Sync,
                contentDescription = "Sync",
                tint = if (syncState is SyncState.LastSync && !syncState.success)
                    JournalColors.Signal else JournalColors.Slate
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✦", fontSize = 32.sp, color = JournalColors.Gold)
        Text(
            "Begin your first entry",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Hypotheses. Decisions. Predictions.",
            style = MaterialTheme.typography.bodyMedium,
            color = JournalColors.Slate
        )
    }
}
