import { useState } from 'react'
import { useJournalStore } from '../store/journalStore'
import { isPast, format, addWeeks } from 'date-fns'
import type { JournalEntry, Prediction, Decision } from '../types'

export default function ReviewPage() {
  const { entries, stats, saveEntry } = useJournalStore()
  const [tab, setTab] = useState<'predictions' | 'decisions' | 'analytics'>('predictions')

  const pendingPredictions = entries.filter(e =>
    e.predictions.some(p =>
      p.wasCorrect === null &&
      isPast(new Date(p.deadline + 'T23:59:59'))
    )
  )

  const pendingDecisions = entries.filter(e =>
    e.decisions.some(d => {
      if (d.reviewedAt) return false
      const entryDate = new Date(e.date)
      const due = addWeeks(entryDate, d.reviewAfterWeeks ?? 4)
      return isPast(due)
    })
  )

  const reviewedPredictions = entries.filter(e =>
    e.predictions.some(p => p.wasCorrect !== null)
  )
  const reviewedDecisions = entries.filter(e =>
    e.decisions.some(d => d.reviewedAt !== null)
  )

  return (
    <div>
      <h1 className="heading" style={{ marginBottom: 6 }}>Review</h1>
      <p style={{ color: 'var(--slate)', marginBottom: 24, fontSize: 14 }}>
        Close the loop. Were you right?
      </p>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 28, borderBottom: '1px solid var(--ink-muted)', paddingBottom: 0 }}>
        {(['predictions', 'decisions', 'analytics'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)} style={{
            padding: '8px 16px', border: 'none', background: 'none', cursor: 'pointer',
            fontSize: 13, fontWeight: 500, letterSpacing: '0.3px',
            color: tab === t ? 'var(--gold)' : 'var(--slate)',
            borderBottom: tab === t ? '2px solid var(--gold)' : '2px solid transparent',
            marginBottom: -1, transition: '180ms',
            textTransform: 'capitalize'
          }}>
            {t}
            {t === 'predictions' && pendingPredictions.length > 0 && (
              <span style={{ marginLeft: 6, background: 'var(--signal)', color: '#fff',
                borderRadius: 10, padding: '1px 6px', fontSize: 10 }}>
                {pendingPredictions.length}
              </span>
            )}
            {t === 'decisions' && pendingDecisions.length > 0 && (
              <span style={{ marginLeft: 6, background: 'var(--gold)', color: 'var(--ink)',
                borderRadius: 10, padding: '1px 6px', fontSize: 10 }}>
                {pendingDecisions.length}
              </span>
            )}
          </button>
        ))}
      </div>

      {tab === 'predictions' && (
        <PredictionsTab
          pending={pendingPredictions}
          reviewed={reviewedPredictions}
          entries={entries}
          onSave={saveEntry}
        />
      )}
      {tab === 'decisions' && (
        <DecisionsTab
          pending={pendingDecisions}
          reviewed={reviewedDecisions}
          onSave={saveEntry}
        />
      )}
      {tab === 'analytics' && <AnalyticsTab stats={stats} />}
    </div>
  )
}

// ──────────────────────────────────────────────
// Predictions tab
// ──────────────────────────────────────────────

function PredictionsTab({ pending, reviewed, onSave }: {
  pending: JournalEntry[], reviewed: JournalEntry[],
  entries: JournalEntry[], onSave: (e: JournalEntry) => Promise<void>
}) {
  if (pending.length === 0 && reviewed.length === 0) {
    return <div className="empty-state"><div className="icon">◎</div><h3>No predictions yet</h3>
      <p>Add predictions in your entries — they'll appear here when their deadline arrives.</p></div>
  }
  return (
    <div>
      {pending.length > 0 && (
        <section style={{ marginBottom: 40 }}>
          <div className="label" style={{ marginBottom: 14 }}>Pending review</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {pending.map(entry =>
              entry.predictions
                .filter(p => p.wasCorrect === null && isPast(new Date(p.deadline + 'T23:59:59')))
                .map(p => (
                  <PredictionReviewCard key={p.id} entry={entry} prediction={p} onSave={onSave} />
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
              e.predictions.filter(p => p.wasCorrect !== null).map(p => (
                <div key={p.id} className="card" style={{ opacity: 0.7 }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
                    <div>
                      <div style={{ fontSize: 14, marginBottom: 4 }}>{p.statement}</div>
                      <div className="label">{format(new Date(p.deadline + 'T12:00:00'), 'd MMM yyyy')} · {p.confidence}% confidence</div>
                    </div>
                    <span style={{ color: p.wasCorrect ? 'var(--correct)' : 'var(--wrong)', fontWeight: 700, fontSize: 20 }}>
                      {p.wasCorrect ? '✓' : '✗'}
                    </span>
                  </div>
                  {p.actualOutcome && <div style={{ marginTop: 8, fontSize: 13, color: 'var(--slate)' }}>→ {p.actualOutcome}</div>}
                </div>
              ))
            )}
          </div>
        </section>
      )}
    </div>
  )
}

function PredictionReviewCard({ entry, prediction, onSave }: {
  entry: JournalEntry, prediction: Prediction, onSave: (e: JournalEntry) => Promise<void>
}) {
  const [actualOutcome, setActualOutcome] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async (wasCorrect: boolean) => {
    setSubmitting(true)
    const updated: Prediction = { ...prediction, wasCorrect, actualOutcome: actualOutcome || null, reviewedAt: new Date().toISOString() }
    await onSave({ ...entry, updatedAt: new Date().toISOString(), predictions: entry.predictions.map(p => p.id === prediction.id ? updated : p) })
    setSubmitting(false)
  }

  return (
    <div className="card" style={{ borderColor: 'color-mix(in srgb, var(--gold) 30%, transparent)' }}>
      <div className="label" style={{ marginBottom: 8 }}>{format(new Date(entry.date + 'T12:00:00'), 'd MMM yyyy')} · {prediction.confidence}% confidence</div>
      <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 4 }}>{prediction.statement}</div>
      <div style={{ fontSize: 13, color: 'var(--slate)', marginBottom: 14 }}>Expected: {prediction.expectedOutcome}</div>
      <div className="field" style={{ marginBottom: 14 }}>
        <label>What actually happened?</label>
        <textarea className="textarea" style={{ minHeight: 60 }} placeholder="Optional" value={actualOutcome} onChange={e => setActualOutcome(e.target.value)} />
      </div>
      <div style={{ display: 'flex', gap: 10 }}>
        <button className="btn" disabled={submitting} onClick={() => submit(true)}
          style={{ flex: 1, background: 'color-mix(in srgb, var(--correct) 15%, transparent)', color: 'var(--correct)', border: '1px solid color-mix(in srgb, var(--correct) 30%, transparent)' }}>
          ✓ Correct
        </button>
        <button className="btn" disabled={submitting} onClick={() => submit(false)}
          style={{ flex: 1, background: 'color-mix(in srgb, var(--wrong) 15%, transparent)', color: 'var(--wrong)', border: '1px solid color-mix(in srgb, var(--wrong) 30%, transparent)' }}>
          ✗ Wrong
        </button>
      </div>
    </div>
  )
}

// ──────────────────────────────────────────────
// Decisions tab
// ──────────────────────────────────────────────

function DecisionsTab({ pending, reviewed, onSave }: {
  pending: JournalEntry[], reviewed: JournalEntry[], onSave: (e: JournalEntry) => Promise<void>
}) {
  if (pending.length === 0 && reviewed.length === 0) {
    return <div className="empty-state"><div className="icon">◎</div><h3>No decisions to review</h3>
      <p>Decisions will appear here once their review window has passed.</p></div>
  }
  return (
    <div>
      {pending.length > 0 && (
        <section style={{ marginBottom: 40 }}>
          <div className="label" style={{ marginBottom: 14 }}>Pending review</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {pending.map(entry =>
              entry.decisions
                .filter(d => {
                  if (d.reviewedAt) return false
                  const due = addWeeks(new Date(entry.date), d.reviewAfterWeeks ?? 4)
                  return isPast(due)
                })
                .map(d => (
                  <DecisionReviewCard key={d.id} entry={entry} decision={d} onSave={onSave} />
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
              e.decisions.filter(d => d.reviewedAt !== null).map(d => (
                <div key={d.id} className="card" style={{ opacity: 0.7 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 500, marginBottom: 4 }}>{d.statement}</div>
                      <div className="label">{d.decisionType} · {format(new Date(e.date + 'T12:00:00'), 'd MMM yyyy')}</div>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                      <span style={{ color: d.wouldRepeat ? 'var(--correct)' : 'var(--wrong)', fontWeight: 700 }}>
                        {d.wouldRepeat ? '↺ Repeat' : '✗ Avoid'}
                      </span>
                      {d.outcomeRating && <StarRating value={d.outcomeRating} readonly />}
                    </div>
                  </div>
                  {d.outcomeNote && <div style={{ marginTop: 8, fontSize: 13, color: 'var(--slate)' }}>→ {d.outcomeNote}</div>}
                </div>
              ))
            )}
          </div>
        </section>
      )}
    </div>
  )
}

function DecisionReviewCard({ entry, decision, onSave }: {
  entry: JournalEntry, decision: Decision, onSave: (e: JournalEntry) => Promise<void>
}) {
  const [outcomeNote, setOutcomeNote] = useState('')
  const [wouldRepeat, setWouldRepeat] = useState<boolean | null>(null)
  const [rating, setRating] = useState<number>(3)
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    if (wouldRepeat === null) return
    setSubmitting(true)
    const updated: Decision = {
      ...decision,
      outcomeNote: outcomeNote || null,
      wouldRepeat,
      outcomeRating: rating,
      reviewedAt: new Date().toISOString()
    }
    await onSave({ ...entry, updatedAt: new Date().toISOString(), decisions: entry.decisions.map(d => d.id === decision.id ? updated : d) })
    setSubmitting(false)
  }

  return (
    <div className="card" style={{ borderColor: 'color-mix(in srgb, var(--gold) 30%, transparent)' }}>
      <div className="label" style={{ marginBottom: 6 }}>{decision.decisionType} · {format(new Date(entry.date + 'T12:00:00'), 'd MMM yyyy')}</div>
      <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 4 }}>{decision.statement}</div>
      <div style={{ fontSize: 13, color: 'var(--slate)', marginBottom: 16 }}>Rationale: {decision.rationale}</div>

      <div className="field" style={{ marginBottom: 14 }}>
        <label>What actually happened?</label>
        <textarea className="textarea" style={{ minHeight: 60 }} placeholder="Outcome note..." value={outcomeNote} onChange={e => setOutcomeNote(e.target.value)} />
      </div>

      <div style={{ marginBottom: 16 }}>
        <div className="label" style={{ marginBottom: 8 }}>Outcome quality</div>
        <StarRating value={rating} onChange={setRating} />
      </div>

      <div style={{ marginBottom: 16 }}>
        <div className="label" style={{ marginBottom: 8 }}>Would you make the same call again?</div>
        <div style={{ display: 'flex', gap: 10 }}>
          {[true, false].map(v => (
            <button key={String(v)} onClick={() => setWouldRepeat(v)}
              style={{
                flex: 1, padding: '8px 0', borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 500,
                border: `1px solid ${wouldRepeat === v ? (v ? 'var(--correct)' : 'var(--wrong)') : 'var(--ink-muted)'}`,
                background: wouldRepeat === v ? (v ? 'color-mix(in srgb, var(--correct) 15%, transparent)' : 'color-mix(in srgb, var(--wrong) 15%, transparent)') : 'transparent',
                color: wouldRepeat === v ? (v ? 'var(--correct)' : 'var(--wrong)') : 'var(--slate)',
                transition: '180ms'
              }}>
              {v ? '↺ Yes, repeat' : '✗ No, avoid'}
            </button>
          ))}
        </div>
      </div>

      <button className="btn btn-primary" onClick={submit} disabled={submitting || wouldRepeat === null}
        style={{ width: '100%', justifyContent: 'center', opacity: wouldRepeat === null ? 0.4 : 1 }}>
        {submitting ? 'Saving…' : 'Save review'}
      </button>
    </div>
  )
}

function StarRating({ value, onChange, readonly }: { value: number, onChange?: (v: number) => void, readonly?: boolean }) {
  return (
    <div style={{ display: 'flex', gap: 4 }}>
      {[1, 2, 3, 4, 5].map(n => (
        <span key={n}
          onClick={() => !readonly && onChange?.(n)}
          style={{ fontSize: 20, cursor: readonly ? 'default' : 'pointer', color: n <= value ? 'var(--gold)' : 'var(--ink-muted)', transition: '180ms' }}>
          ★
        </span>
      ))}
    </div>
  )
}

// ──────────────────────────────────────────────
// Analytics tab
// ──────────────────────────────────────────────

function AnalyticsTab({ stats }: { stats: import('../types').CognitiveStats | null }) {
  if (!stats || stats.totalEntries === 0) {
    return <div className="empty-state"><div className="icon">◎</div><h3>Not enough data yet</h3>
      <p>Keep journaling — patterns will emerge after a few weeks.</p></div>
  }

  const buckets = stats.calibrationBuckets.filter(b => b.sampleSize > 0)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>

      {/* Calibration chart */}
      <section>
        <div className="label" style={{ marginBottom: 4 }}>Prediction calibration</div>
        <p style={{ fontSize: 13, color: 'var(--slate)', marginBottom: 16 }}>
          How well does your confidence match your actual accuracy?
          {' '}<span style={{ color: 'var(--gold)' }}>{Math.round(stats.calibrationScore * 100)}% calibrated</span>
        </p>
        {buckets.length > 0 ? (
          <CalibrationChart buckets={stats.calibrationBuckets} />
        ) : (
          <div style={{ color: 'var(--slate)', fontSize: 13 }}>Review more predictions to see your calibration curve.</div>
        )}
      </section>

      {/* Decision quality */}
      <section>
        <div className="label" style={{ marginBottom: 14 }}>Decision quality</div>
        <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
          <div className="stat-card">
            <div className="stat-value">{stats.decisionsReviewed}</div>
            <div className="stat-label">Reviewed</div>
          </div>
          <div className="stat-card">
            <div className="stat-value" style={{ color: stats.wouldRepeatRate >= 0.7 ? 'var(--correct)' : 'var(--wrong)' }}>
              {Math.round(stats.wouldRepeatRate * 100)}%
            </div>
            <div className="stat-label">Would repeat</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{stats.avgOutcomeRating > 0 ? stats.avgOutcomeRating.toFixed(1) : '—'}</div>
            <div className="stat-label">Avg rating /5</div>
          </div>
        </div>

        {/* By type */}
        {Object.keys(stats.decisionAccuracyByType).length > 0 && (
          <div style={{ marginTop: 16 }}>
            <div className="label" style={{ marginBottom: 10 }}>Would repeat — by type</div>
            {(Object.entries(stats.decisionAccuracyByType) as [string, number][]).map(([type, rate]) => (
              <div key={type} style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 13 }}>
                  <span style={{ color: 'var(--slate)' }}>{type}</span>
                  <span style={{ color: rate >= 0.7 ? 'var(--correct)' : 'var(--wrong)', fontWeight: 600 }}>
                    {Math.round(rate * 100)}%
                  </span>
                </div>
                <ProgressBar value={rate} color={rate >= 0.7 ? 'var(--correct)' : 'var(--signal)'} />
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Tag intelligence */}
      {Object.keys(stats.predictionAccuracyByTag).length > 0 && (
        <section>
          <div className="label" style={{ marginBottom: 14 }}>Prediction accuracy by tag</div>
          {Object.values(stats.predictionAccuracyByTag)
            .filter(s => s.sampleSize >= 2)
            .sort((a, b) => b.accuracy - a.accuracy)
            .map(s => (
              <div key={s.tag} style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 13 }}>
                  <span style={{ color: 'var(--slate)' }}>#{s.tag} <span style={{ fontSize: 11 }}>({s.sampleSize})</span></span>
                  <span style={{ color: s.accuracy >= 0.6 ? 'var(--correct)' : 'var(--wrong)', fontWeight: 600 }}>
                    {Math.round(s.accuracy * 100)}%
                  </span>
                </div>
                <ProgressBar value={s.accuracy} color={s.accuracy >= 0.6 ? 'var(--correct)' : 'var(--signal)'} />
              </div>
            ))}
        </section>
      )}

      {Object.keys(stats.decisionQualityByTag).length > 0 && (
        <section>
          <div className="label" style={{ marginBottom: 14 }}>Decision quality by tag</div>
          {Object.values(stats.decisionQualityByTag)
            .filter(s => s.sampleSize >= 2)
            .sort((a, b) => b.accuracy - a.accuracy)
            .map(s => (
              <div key={s.tag} style={{ marginBottom: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: 13 }}>
                  <span style={{ color: 'var(--slate)' }}>#{s.tag} <span style={{ fontSize: 11 }}>({s.sampleSize})</span></span>
                  <span style={{ color: s.accuracy >= 0.6 ? 'var(--correct)' : 'var(--wrong)', fontWeight: 600 }}>
                    {Math.round(s.accuracy * 100)}%
                  </span>
                </div>
                <ProgressBar value={s.accuracy} color={s.accuracy >= 0.6 ? 'var(--correct)' : 'var(--signal)'} />
              </div>
            ))}
        </section>
      )}
    </div>
  )
}

function CalibrationChart({ buckets }: { buckets: import('../types').CalibrationBucket[] }) {
  const chartH = 180
  const chartW = 360
  const pad = { top: 16, right: 16, bottom: 32, left: 36 }
  const innerW = chartW - pad.left - pad.right
  const innerH = chartH - pad.top - pad.bottom

  const xStep = innerW / 8  // 9 buckets (10..90)

  const toX = (mid: number) => pad.left + ((mid - 10) / 10) * xStep
  const toY = (rate: number) => pad.top + innerH - rate * innerH

  const hasSamples = buckets.some(b => b.sampleSize > 0)

  return (
    <svg viewBox={`0 0 ${chartW} ${chartH}`} style={{ width: '100%', maxWidth: chartW, overflow: 'visible' }}>
      {/* Grid */}
      {[0, 0.25, 0.5, 0.75, 1].map(v => (
        <g key={v}>
          <line x1={pad.left} x2={chartW - pad.right} y1={toY(v)} y2={toY(v)} stroke="#2C2C2C" strokeWidth="0.5" />
          <text x={pad.left - 6} y={toY(v) + 4} textAnchor="end" fontSize="9" fill="#6B7B8D">{Math.round(v * 100)}</text>
        </g>
      ))}

      {/* Perfect calibration line (diagonal) */}
      <line
        x1={toX(10)} y1={toY(0.1)}
        x2={toX(90)} y2={toY(0.9)}
        stroke="#D4A853" strokeWidth="1" strokeDasharray="4,4" strokeOpacity="0.4"
      />

      {/* Actual bars */}
      {hasSamples && buckets.map(b => {
        if (b.sampleSize === 0) return null
        const x = toX(b.confidenceMidpoint)
        const barH = b.actualRate * innerH
        const barW = xStep * 0.55
        return (
          <g key={b.confidenceMidpoint}>
            <rect
              x={x - barW / 2} y={toY(b.actualRate)}
              width={barW} height={barH}
              fill="var(--gold)" fillOpacity="0.6" rx="2"
            />
            <text x={x} y={chartH - pad.bottom + 12} textAnchor="middle" fontSize="9" fill="#6B7B8D">
              {b.confidenceMidpoint}
            </text>
          </g>
        )
      })}

      {/* Axis labels */}
      <text x={chartW / 2} y={chartH} textAnchor="middle" fontSize="9" fill="#6B7B8D">Confidence %</text>
      <text x={10} y={chartH / 2} textAnchor="middle" fontSize="9" fill="#6B7B8D"
        transform={`rotate(-90, 10, ${chartH / 2})`}>Accuracy</text>

      {/* Legend */}
      <line x1={chartW - 80} y1={pad.top + 8} x2={chartW - 65} y2={pad.top + 8} stroke="#D4A853" strokeDasharray="4,4" strokeOpacity="0.4" />
      <text x={chartW - 62} y={pad.top + 12} fontSize="8" fill="#6B7B8D">perfect</text>
      <rect x={chartW - 80} y={pad.top + 18} width={10} height={8} fill="var(--gold)" fillOpacity="0.6" rx="1" />
      <text x={chartW - 66} y={pad.top + 26} fontSize="8" fill="#6B7B8D">actual</text>
    </svg>
  )
}

function ProgressBar({ value, color }: { value: number, color: string }) {
  return (
    <div style={{ height: 4, background: 'var(--ink-muted)', borderRadius: 2, overflow: 'hidden' }}>
      <div style={{ width: `${Math.round(value * 100)}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.4s ease' }} />
    </div>
  )
}
