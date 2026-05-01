import { Link, useNavigate } from 'react-router-dom'
import { useJournalStore } from '../store/journalStore'
import { format, isToday, isPast, addWeeks } from 'date-fns'
import { fr } from 'date-fns/locale'
import type { JournalEntry } from '../types'

export default function HomePage() {
  const { entries, stats } = useJournalStore()
  const navigate = useNavigate()

  const today = new Date().toISOString().split('T')[0]
  const todayEntry = entries.find(e => e.date === today)

  // Pending prediction reviews
  const pendingPredictions = entries.filter(e =>
    e.predictions.some(p => p.wasCorrect === null && isPast(new Date(p.deadline + 'T23:59:59')))
  ).length

  // Pending decision reviews
  const pendingDecisions = entries.filter(e =>
    e.decisions.some(d => {
      if (d.reviewedAt) return false
      const due = addWeeks(new Date(e.date), d.reviewAfterWeeks ?? 4)
      return isPast(due)
    })
  ).length

  const totalPending = pendingPredictions + pendingDecisions

  // Nudge: 3+ entries without any review loop closed
  const recentNoReview = entries.slice(0, 5).every(e =>
    e.decisions.every(d => !d.reviewedAt) &&
    e.predictions.every(p => p.wasCorrect === null)
  ) && entries.length >= 3

  return (
    <div>
      {/* Stats */}
      {stats && stats.totalEntries > 0 && (
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-value">{stats.totalEntries}</div>
            <div className="stat-label">Entries</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{Math.round(stats.predictionAccuracy * 100)}%</div>
            <div className="stat-label">Prediction accuracy</div>
          </div>
          <div className="stat-card">
            <div className="stat-value" style={{ color: 'var(--correct)' }}>{stats.streakDays}</div>
            <div className="stat-label">Day streak</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">
              {stats.wouldRepeatRate > 0 ? `${Math.round(stats.wouldRepeatRate * 100)}%` : '—'}
            </div>
            <div className="stat-label">Decisions repeated</div>
          </div>
        </div>
      )}

      {/* Nudge banner: pending reviews */}
      {totalPending > 0 && (
        <div className="card clickable" onClick={() => navigate('/review')} style={{
          borderColor: 'color-mix(in srgb, var(--signal) 40%, transparent)',
          marginBottom: 16, display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer'
        }}>
          <div>
            <div style={{ color: 'var(--signal)', fontWeight: 600, marginBottom: 2 }}>
              {pendingPredictions > 0 && `${pendingPredictions} prediction${pendingPredictions > 1 ? 's' : ''}`}
              {pendingPredictions > 0 && pendingDecisions > 0 && ' · '}
              {pendingDecisions > 0 && `${pendingDecisions} decision${pendingDecisions > 1 ? 's' : ''}`}
              {' '}to review
            </div>
            <div className="label">Close the loop</div>
          </div>
          <span style={{ color: 'var(--signal)', fontSize: 20 }}>→</span>
        </div>
      )}

      {/* Soft nudge: 3 entries with no review at all */}
      {recentNoReview && totalPending === 0 && (
        <div className="card" onClick={() => navigate('/review')} style={{
          borderColor: 'color-mix(in srgb, var(--gold) 20%, transparent)',
          marginBottom: 16, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          cursor: 'pointer', opacity: 0.85
        }}>
          <div>
            <div style={{ color: 'var(--gold)', fontWeight: 500, marginBottom: 2, fontSize: 14 }}>
              You have entries without any closed loop yet
            </div>
            <div className="label">Review your past decisions and predictions</div>
          </div>
          <span style={{ color: 'var(--gold)', fontSize: 18 }}>→</span>
        </div>
      )}

      {/* Header row */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <h1 className="heading">
          {format(new Date(), 'EEEE d MMMM', { locale: fr })}
        </h1>
        <Link to={todayEntry ? `/entry/${todayEntry.id}` : '/entry'} className="btn btn-primary">
          {todayEntry ? 'Continue today' : '+ New entry'}
        </Link>
      </div>

      {/* Entry list */}
      {entries.length === 0 ? (
        <div className="empty-state">
          <div className="icon">✦</div>
          <h3>Your first entry awaits</h3>
          <p>Hypotheses. Decisions. Predictions.</p>
          <br />
          <Link to="/entry" className="btn btn-primary">Begin</Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {entries.map(entry => <EntryCard key={entry.id} entry={entry} />)}
        </div>
      )}
    </div>
  )
}

function EntryCard({ entry }: { entry: JournalEntry }) {
  const dateObj = new Date(entry.date + 'T12:00:00')
  const isEntryToday = isToday(dateObj)

  const pendingDecisionReviews = entry.decisions.filter(d => {
    if (d.reviewedAt) return false
    const due = addWeeks(new Date(entry.date), d.reviewAfterWeeks ?? 4)
    return isPast(due)
  }).length

  return (
    <Link to={`/entry/${entry.id}`} style={{ textDecoration: 'none' }}>
      <div className="card clickable">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
          <span className="mono" style={{ color: 'var(--gold)', fontSize: 12 }}>
            {isEntryToday ? 'Today' : format(dateObj, 'd MMM yyyy')}
          </span>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            {pendingDecisionReviews > 0 && (
              <span title={`${pendingDecisionReviews} decision${pendingDecisionReviews > 1 ? 's' : ''} to review`}
                style={{ fontSize: 11, color: 'var(--gold)' }}>↺</span>
            )}
            {entry.isDirty && (
              <span title="Not synced" style={{ fontSize: 11, color: 'var(--slate)' }}>◌</span>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {entry.hypotheses.length > 0 && <span className="chip chip-sage">{entry.hypotheses.length} H</span>}
          {entry.decisions.length > 0 && <span className="chip chip-gold">{entry.decisions.length} D</span>}
          {entry.predictions.length > 0 && <span className="chip chip-signal">{entry.predictions.length} P</span>}
        </div>

        {entry.contextNote && (
          <p style={{ marginTop: 10, fontSize: 14, color: 'var(--slate)',
            overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
            {entry.contextNote}
          </p>
        )}
      </div>
    </Link>
  )
}
