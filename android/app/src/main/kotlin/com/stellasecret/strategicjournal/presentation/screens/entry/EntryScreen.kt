package com.stellasecret.strategicjournal.presentation.screens.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stellasecret.strategicjournal.domain.model.*
import com.stellasecret.strategicjournal.presentation.theme.JournalColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    onBack: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Navigate back after save
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EntryEvent.Saved -> onBack()
            }
        }
    }

    val entry = state.entry

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        entry?.date ?: LocalDate.now().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = JournalColors.Gold,
                        )
                    } else {
                        TextButton(onClick = { viewModel.saveEntry() }) {
                            Text("Save", color = JournalColors.Gold, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = JournalColors.Gold)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Context note
            item {
                ContextSection(
                    note = entry.contextNote,
                    energyLevel = entry.energyLevel,
                    onNoteChange = viewModel::updateContextNote,
                    onEnergyChange = viewModel::updateEnergyLevel,
                )
            }

            // Hypotheses section
            item {
                SectionHeader(
                    label = "H",
                    title = "Hypotheses",
                    color = JournalColors.Sage,
                    count = entry.hypotheses.size,
                )
            }
            items(entry.hypotheses) { h ->
                HypothesisChip(h, onRemove = {
                    viewModel.removeHypothesis(h.id)
                })
            }
            item { AddHypothesisCard(onAdd = viewModel::addHypothesis) }

            // Decisions section
            item {
                SectionHeader(
                    label = "D",
                    title = "Decisions",
                    color = JournalColors.Gold,
                    count = entry.decisions.size,
                )
            }
            items(entry.decisions) { d ->
                DecisionChip(d, onRemove = { viewModel.removeDecision(d.id) })
            }
            item { AddDecisionCard(onAdd = viewModel::addDecision) }

            // Predictions section
            item {
                SectionHeader(
                    label = "P",
                    title = "Predictions",
                    color = JournalColors.Signal,
                    count = entry.predictions.size,
                )
            }
            items(entry.predictions) { p ->
                PredictionChip(p, onRemove = { viewModel.removePrediction(p.id) })
            }
            item { AddPredictionCard(onAdd = viewModel::addPrediction) }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ──────────────────────────────────────────────
// Context section
// ──────────────────────────────────────────────

@Composable
private fun ContextSection(
    note: String,
    energyLevel: Int,
    onNoteChange: (String) -> Unit,
    onEnergyChange: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Context", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text("What's on your mind today?", color = JournalColors.Slate) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JournalColors.Gold,
                        unfocusedBorderColor = JournalColors.InkMuted,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Energy", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
                Slider(
                    value = energyLevel.toFloat(),
                    onValueChange = { onEnergyChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = JournalColors.Gold,
                            activeTrackColor = JournalColors.Gold,
                        ),
                )
                Text(
                    "$energyLevel",
                    style = MaterialTheme.typography.labelSmall,
                    color = JournalColors.Gold,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Section header
// ──────────────────────────────────────────────

@Composable
private fun SectionHeader(
    label: String,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    count: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        if (count > 0) {
            Text("$count", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
        }
    }
}

// ──────────────────────────────────────────────
// Item chips
// ──────────────────────────────────────────────

@Composable
private fun HypothesisChip(
    h: Hypothesis,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DomainChip(h.domain)
                    ConfidenceChip(h.confidence)
                }
                Spacer(Modifier.height(6.dp))
                Text(h.statement, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = JournalColors.Slate,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun DecisionChip(
    d: Decision,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChip(d.decisionType.name, JournalColors.Gold)
                    if (!d.reversible) SmallChip("Irreversible", JournalColors.Signal)
                }
                Spacer(Modifier.height(6.dp))
                Text(d.statement, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(d.rationale, style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = JournalColors.Slate,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun PredictionChip(
    p: Prediction,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChip("Due ${p.deadline}", JournalColors.Signal)
                    ConfidenceChip(p.confidence)
                }
                Spacer(Modifier.height(6.dp))
                Text(p.statement, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(p.expectedOutcome, style = MaterialTheme.typography.bodySmall, color = JournalColors.Slate)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = JournalColors.Slate,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Add forms (collapsible)
// ──────────────────────────────────────────────

@Composable
private fun AddHypothesisCard(onAdd: (String, HypothesisDomain, Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var statement by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf(HypothesisDomain.BUSINESS) }
    var confidence by remember { mutableStateOf(60f) }

    AddCard(label = "Add hypothesis", expanded = expanded, onToggle = { expanded = !expanded }) {
        OutlinedTextField(
            value = statement,
            onValueChange = { statement = it },
            placeholder = { Text("I believe that…", color = JournalColors.Slate) },
            modifier = Modifier.fillMaxWidth(),
            colors = journalTextFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        EnumDropdown(
            options = HypothesisDomain.entries,
            selected = domain,
            label = { it.name },
            onSelect = { domain = it },
        )
        Spacer(Modifier.height(8.dp))
        ConfidenceSlider(value = confidence, onChange = { confidence = it })
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (statement.isNotBlank()) {
                    onAdd(statement, domain, confidence.toInt())
                    statement = ""
                    expanded = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Sage),
        ) { Text("Add") }
    }
}

@Composable
private fun AddDecisionCard(onAdd: (String, String, DecisionType, Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var statement by remember { mutableStateOf("") }
    var rationale by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DecisionType.STRATEGIC) }
    var reversible by remember { mutableStateOf(true) }

    AddCard(label = "Add decision", expanded = expanded, onToggle = { expanded = !expanded }) {
        OutlinedTextField(
            value = statement,
            onValueChange = { statement = it },
            placeholder = { Text("I decided to…", color = JournalColors.Slate) },
            modifier = Modifier.fillMaxWidth(),
            colors = journalTextFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = rationale,
            onValueChange = { rationale = it },
            placeholder = { Text("Because…", color = JournalColors.Slate) },
            modifier = Modifier.fillMaxWidth(),
            colors = journalTextFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        EnumDropdown(
            options = DecisionType.entries,
            selected = type,
            label = { it.name },
            onSelect = { type = it },
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = reversible,
                onCheckedChange = { reversible = it },
                colors = CheckboxDefaults.colors(checkedColor = JournalColors.Gold),
            )
            Text("Reversible", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                if (statement.isNotBlank()) {
                    onAdd(statement, rationale, type, reversible)
                    statement = ""
                    rationale = ""
                    expanded = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Gold),
        ) { Text("Add", color = JournalColors.Ink) }
    }
}

@Composable
private fun AddPredictionCard(onAdd: (String, String, String, Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var statement by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var confidence by remember { mutableStateOf(65f) }

    AddCard(label = "Add prediction", expanded = expanded, onToggle = { expanded = !expanded }) {
        OutlinedTextField(
            value = statement,
            onValueChange = { statement = it },
            placeholder = { Text("I predict that…", color = JournalColors.Slate) },
            modifier = Modifier.fillMaxWidth(),
            colors = journalTextFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = expected,
            onValueChange = { expected = it },
            placeholder = { Text("The measurable outcome will be…", color = JournalColors.Slate) },
            modifier = Modifier.fillMaxWidth(),
            colors = journalTextFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = deadline,
            onValueChange = { deadline = it },
            label = { Text("Deadline (YYYY-MM-DD)", color = JournalColors.Slate) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = journalTextFieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        ConfidenceSlider(value = confidence, onChange = { confidence = it })
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (statement.isNotBlank()) {
                    onAdd(statement, expected, deadline, confidence.toInt())
                    statement = ""
                    expected = ""
                    expanded = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = JournalColors.Signal),
        ) { Text("Add") }
    }
}

// ──────────────────────────────────────────────
// Reusable small components
// ──────────────────────────────────────────────

@Composable
private fun AddCard(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = JournalColors.Slate)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.Add,
                    contentDescription = null,
                    tint = JournalColors.Slate,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun ConfidenceSlider(
    value: Float,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Confidence", style = MaterialTheme.typography.labelSmall, color = JournalColors.Slate)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = JournalColors.Gold, activeTrackColor = JournalColors.Gold),
        )
        Text(
            "${value.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = JournalColors.Gold,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = JournalColors.Slate),
        ) {
            Text(label(selected))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(label(opt)) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DomainChip(domain: HypothesisDomain) = SmallChip(domain.name, JournalColors.Sage)

@Composable
private fun ConfidenceChip(confidence: Int) = SmallChip("$confidence%", JournalColors.Gold)

@Composable
private fun SmallChip(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun journalTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = JournalColors.Gold,
        unfocusedBorderColor = JournalColors.InkMuted,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = JournalColors.Gold,
    )
