package com.stellasecret.strategicjournal.data.ai

import com.stellasecret.strategicjournal.domain.model.JournalEntry
import com.stellasecret.strategicjournal.domain.model.ReviewPeriod

object ReviewPromptBuilder {
    fun build(
        entries: List<JournalEntry>,
        period: ReviewPeriod,
    ): String {
        val periodLabel = if (period == ReviewPeriod.WEEKLY) "past 7 days" else "past 30 days"
        val entriesBlock = formatEntries(entries)

        return """
You are a cognitive performance analyst reviewing a structured strategic journal.
Your role is analytical, not therapeutic. Be precise, evidence-based, and direct.

PERIOD: $periodLabel (${entries.size} entries)

JOURNAL ENTRIES:
$entriesBlock

---

Analyze these entries and respond ONLY with a valid JSON object matching this exact schema.
Do not include markdown fences, preamble, or any text outside the JSON.

{
  "topDecisions": [
    {
      "statement": "string — the decision as stated",
      "outcome": "string or null — outcome if explicitly mentioned, null otherwise",
      "sentiment": "POSITIVE | NEGATIVE | NEUTRAL | UNKNOWN"
    }
  ],
  "invalidatedHypotheses": [
    "string — hypothesis that was explicitly invalidated or contradicted, with date"
  ],
  "recurringThemes": [
    {
      "theme": "string — concise theme label (3-6 words max)",
      "occurrences": number,
      "referenceDates": ["YYYY-MM-DD"]
    }
  ],
  "predictionSummary": {
    "total": number,
    "resolved": number,
    "accuracy": number or null,
    "averageConfidence": number or null,
    "calibrationNote": "string or null — e.g. 'overconfident by ~15%' or null"
  },
  "openLoops": [
    "string — unresolved topic or deferred decision, with date reference"
  ]
}

STRICT RULES:
- Evidence only. Every item must trace back to a specific entry.
- Quote entry dates (YYYY-MM-DD) when referencing specific items.
- recurringThemes: minimum 2 occurrences to qualify. No singleton themes.
- openLoops: only topics explicitly flagged as unresolved or deferred.
- Do NOT infer emotions. Do NOT generate generic advice.
- Do NOT use therapy language ("you seem", "you feel", "you might want to").
- If a section has no data, return an empty array [] or null.
- predictionSummary.accuracy: float 0.0-1.0, or null if no resolved predictions.
- If total entries < 3, set all arrays to [] and add a single openLoop: "Insufficient data for pattern detection."
            """.trimIndent()
    }

    private fun formatEntries(entries: List<JournalEntry>): String =
        entries.joinToString("\n\n") { entry ->
            buildString {
                appendLine("=== ${entry.date} ===")

                if (entry.contextNote.isNotBlank()) {
                    appendLine("Context: ${entry.contextNote}")
                }

                if (entry.energyLevel in 1..10) {
                    appendLine("Energy: ${entry.energyLevel}/10")
                }

                if (entry.decisions.isNotEmpty()) {
                    appendLine("DECISIONS:")
                    entry.decisions.forEach { d ->
                        appendLine("  • [${d.decisionType}] ${d.statement}")
                        if (d.rationale.isNotBlank()) appendLine("    Rationale: ${d.rationale}")
                        if (!d.alternatives.isNullOrEmpty()) {
                            appendLine(
                                "    Alternatives considered: ${d.alternatives.joinToString(", ")}",
                            )
                        }
                        if (d.reversible) appendLine("    Reversible: yes")
                        if (!d.outcomeNote.isNullOrBlank()) appendLine("    Outcome: ${d.outcomeNote}")
                        if (d.wouldRepeat != null) appendLine("    Would repeat: ${d.wouldRepeat}")
                    }
                }

                if (entry.hypotheses.isNotEmpty()) {
                    appendLine("HYPOTHESES:")
                    entry.hypotheses.forEach { h ->
                        appendLine("  • [${h.domain}] ${h.statement} (confidence: ${h.confidence}%)")
                        if (h.wasCorrect != null) appendLine("    Validated: ${h.wasCorrect}")
                    }
                }

                if (entry.predictions.isNotEmpty()) {
                    appendLine("PREDICTIONS:")
                    entry.predictions.forEach { p ->
                        appendLine("  • ${p.statement}")
                        appendLine(
                            "    Expected: ${p.expectedOutcome} | Deadline: ${p.deadline} | Confidence: ${p.confidence}%",
                        )
                        if (p.wasCorrect !=
                            null
                        ) {
                            appendLine(
                                "    Outcome: ${if (p.wasCorrect) "correct" else "wrong"} — ${p.actualOutcome ?: ""}",
                            )
                        }
                    }
                }

                if (entry.tags.isNotEmpty()) {
                    appendLine("Tags: ${entry.tags.joinToString(", ")}")
                }
            }
        }
}
