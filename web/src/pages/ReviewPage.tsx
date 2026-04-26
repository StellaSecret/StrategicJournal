import { useState } from 'react'
import { useJournalStore } from '../store/journalStore'
import { isPast, format } from 'date-fns'
import type { JournalEntry, Prediction } from '../types'

export default function ReviewPage() {
  const { entries, saveEntry } = useJournalStore()

  // All entries with at least one unreviewed past prediction
  const toReview = entries.filter(e =>
    e.predictions.some(p =>
      p.wasCorrect === null &&
      p.wasCorrect !== false &&
      isPast(new Date(p.deadline + 'T23:59:59'))
    )
  )

  const reviewed = entries.filter(e =>
    e.predictions.some(p => p.wasCorrect !== null)
  )

  if (toReview.length === 0 && reviewed.length === 0) {
    return (
      <div className="empty-state">
        <div className="icon">◎</div>
        <h3>No predictions yet</h3>
        <p>Add predictions in your entries — they'll appear here for review when their deadline arrives.</p>
      </div>
    )
  }

  return (
    <div>
      <h1 className="heading" style={{ marginBottom: 8 }}>Review</h1>
      <p style={{ color: 'var(--slate)', marginBottom: 28, fontSize: 14 }}>
        Close the loop. Were you right?
      </p>

      {toReview.length > 0 && (
        <section style={{ marginBottom: 40 }}>
          <div className="label" style={{ marginBottom: 14 }}>Pending review</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {toReview.map(entry =>
              entry.predictions
                .filter(p => p.wasCorrect === null && isPast(new Date(p.deadline + 'T23:59:59')))
                .map(prediction => (
                  <PredictionReviewCard
                    key={prediction.id}
                    entry={entry}
                    prediction={prediction}
                    onReview={saveEntry}
                  />
                ))
            )}
          </div>
        </section>
      )}

      {reviewed.length > 0 && (
        <section>
          <div className="label" style={{ marginBottom: 14 }}>Reviewed</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {reviewed.flatMap(e =>
              e.predictions
                .filter(p => p.wasCorrect !== null)
                .map(p => (
                  <div key={p.id} className="card" style={{ opacity: 0.7 }}>
                    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
                      <div>
                        <div style={{ fontSize: 14, marginBottom: 4 }}>{p.statement}</div>
                        <div className="label">{format(new Date(p.deadline), 'd MMM yyyy')} · {p.confidence}% confidence</div>
                      </div>
                      <span style={{
                        color: p.wasCorrect ? 'var(--correct)' : 'var(--wrong)',
                        fontWeight: 600,
                        fontSize: 18,
                        flexShrink: 0
                      }}>
                        {p.wasCorrect ? '✓' : '✗'}
                      </span>
                    </div>
                    {p.actualOutcome && (
                      <div style={{ marginTop: 8, fontSize: 13, color: 'var(--slate)' }}>
                        → {p.actualOutcome}
                      </div>
                    )}
                  </div>
                ))
            )}
          </div>
        </section>
      )}
    </div>
  )
}

function PredictionReviewCard({
  entry,
  prediction,
  onReview,
}: {
  entry: JournalEntry
  prediction: Prediction
  onReview: (entry: JournalEntry) => Promise<void>
}) {
  const [actualOutcome, setActualOutcome] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async (wasCorrect: boolean) => {
    setSubmitting(true)
    const updatedPrediction: Prediction = {
      ...prediction,
      wasCorrect,
      actualOutcome: actualOutcome || null,
      reviewedAt: new Date().toISOString(),
    }
    const updatedEntry: JournalEntry = {
      ...entry,
      updatedAt: new Date().toISOString(),
      predictions: entry.predictions.map(p =>
        p.id === prediction.id ? updatedPrediction : p
      ),
    }
    await onReview(updatedEntry)
    setSubmitting(false)
  }

  return (
    <div className="card" style={{ borderColor: 'color-mix(in srgb, var(--gold) 30%, transparent)' }}>
      <div className="label" style={{ marginBottom: 8 }}>
        {format(new Date(entry.date), 'd MMM yyyy')} · {prediction.confidence}% confidence
      </div>

      <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 4 }}>
        {prediction.statement}
      </div>
      <div style={{ fontSize: 13, color: 'var(--slate)', marginBottom: 14 }}>
        Expected: {prediction.expectedOutcome}
      </div>

      <div className="field" style={{ marginBottom: 14 }}>
        <label>What actually happened?</label>
        <textarea
          className="textarea"
          style={{ minHeight: 60 }}
          placeholder="Optional — note the actual outcome for future reference"
          value={actualOutcome}
          onChange={e => setActualOutcome(e.target.value)}
        />
      </div>

      <div style={{ display: 'flex', gap: 10 }}>
        <button
          className="btn"
          style={{
            background: 'color-mix(in srgb, var(--correct) 15%, transparent)',
            color: 'var(--correct)',
            border: '1px solid color-mix(in srgb, var(--correct) 30%, transparent)',
            flex: 1
          }}
          onClick={() => submit(true)}
          disabled={submitting}
        >
          ✓ Correct
        </button>
        <button
          className="btn"
          style={{
            background: 'color-mix(in srgb, var(--wrong) 15%, transparent)',
            color: 'var(--wrong)',
            border: '1px solid color-mix(in srgb, var(--wrong) 30%, transparent)',
            flex: 1
          }}
          onClick={() => submit(false)}
          disabled={submitting}
        >
          ✗ Wrong
        </button>
      </div>
    </div>
  )
}
