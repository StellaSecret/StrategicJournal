import { Link, useNavigate } from 'react-router-dom'
import { useJournalStore } from '../store/journalStore'
import { format, isToday, isPast } from 'date-fns'
import { fr } from 'date-fns/locale'
import type { JournalEntry } from '../types'

export default function HomePage() {
  const { entries, stats } = useJournalStore()
  const navigate = useNavigate()

  const today = new Date().toISOString().split('T')[0]
  const todayEntry = entries.find(e => e.date === today)
  const pendingReviews = entries.filter(e =>
    e.predictions.some(p => p.wasCorrect === null && isPast(new Date(p.deadline)))
  )

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
            <div className="stat-value" style={{ fontSize: '24px' }}>
              {Math.round(stats.calibrationScore * 100)}%
            </div>
            <div className="stat-label">Calibration</div>
          </div>
        </div>
      )}

      {/* Pending reviews banner */}
      {pendingReviews.length > 0 && (
        <div
          className="card clickable"
          style={{
            borderColor: 'color-mix(in srgb, var(--signal) 40%, transparent)',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer'
          }}
          onClick={() => navigate('/review')}
        >
          <div>
            <div style={{ color: 'var(--signal)', fontWeight: 600, marginBottom: 2 }}>
              {pendingReviews.length} prediction{pendingReviews.length > 1 ? 's' : ''} to review
            </div>
            <div className="label">Close the loop</div>
          </div>
          <span style={{ color: 'var(--signal)', fontSize: 20 }}>→</span>
        </div>
      )}

      {/* Header row */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <h1 className="heading">
          {format(new Date(), "EEEE d MMMM", { locale: fr })}
        </h1>
        <Link
          to={todayEntry ? `/entry/${todayEntry.id}` : '/entry'}
          className="btn btn-primary"
        >
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
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {entries.map(entry => (
            <EntryCard key={entry.id} entry={entry} />
          ))}
        </div>
      )}
    </div>
  )
}

function EntryCard({ entry }: { entry: JournalEntry }) {
  const dateObj = new Date(entry.date + 'T12:00:00')
  const isEntryToday = isToday(dateObj)

  return (
    <Link to={`/entry/${entry.id}`} style={{ textDecoration: 'none' }}>
      <div className="card clickable">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <span className="mono" style={{ color: 'var(--gold)', fontSize: '12px' }}>
            {isEntryToday ? 'Today' : format(dateObj, 'd MMM yyyy')}
          </span>
          <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
            {entry.isDirty && (
              <span title="Not synced" style={{ fontSize: '11px', color: 'var(--slate)' }}>◌</span>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          {entry.hypotheses.length > 0 && (
            <span className="chip chip-sage">{entry.hypotheses.length} H</span>
          )}
          {entry.decisions.length > 0 && (
            <span className="chip chip-gold">{entry.decisions.length} D</span>
          )}
          {entry.predictions.length > 0 && (
            <span className="chip chip-signal">{entry.predictions.length} P</span>
          )}
        </div>

        {entry.contextNote && (
          <p style={{
            marginTop: '10px',
            fontSize: '14px',
            color: 'var(--slate)',
            overflow: 'hidden',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical'
          }}>
            {entry.contextNote}
          </p>
        )}
      </div>
    </Link>
  )
}
