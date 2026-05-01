import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useJournalStore } from '../store/journalStore'
import type { JournalEntry, Hypothesis, Decision, Prediction, HypothesisDomain, DecisionType } from '../types'
import { format, addDays } from 'date-fns'

const newEntry = (): JournalEntry => ({
  id: crypto.randomUUID(),
  date: new Date().toISOString().split('T')[0],
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  hypotheses: [],
  decisions: [],
  predictions: [],
  contextNote: '',
  energyLevel: 7,
  tags: [],
  isDirty: true,
})

export default function EntryPage() {
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const { entries, saveEntry } = useJournalStore()

  const [entry, setEntry] = useState<JournalEntry | null>(null)
  const [saving, setSaving] = useState(false)
  const [activeSection, setActiveSection] = useState<'hypothesis' | 'decision' | 'prediction' | null>(null)

  useEffect(() => {
    if (id) {
      const found = entries.find(e => e.id === id)
      setEntry(found ?? null)
    } else {
      // Check if today already has an entry
      const today = new Date().toISOString().split('T')[0]
      const todayEntry = entries.find(e => e.date === today)
      setEntry(todayEntry ?? newEntry())
    }
  }, [id, entries])

  if (!entry) return <div style={{ color: 'var(--slate)', padding: 40 }}>Loading…</div>

  const update = (patch: Partial<JournalEntry>) =>
    setEntry(e => e ? { ...e, ...patch, updatedAt: new Date().toISOString() } : e)

  const handleSave = async () => {
    if (!entry) return
    setSaving(true)
    await saveEntry(entry)
    setSaving(false)
    navigate('/')
  }

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 28 }}>
        <div>
          <div className="label" style={{ marginBottom: 4 }}>Entry</div>
          <h1 className="heading">{format(new Date(entry.date + 'T12:00:00'), 'EEEE d MMMM yyyy')}</h1>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-ghost" onClick={() => navigate(-1)}>Cancel</button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : 'Save entry'}
          </button>
        </div>
      </div>

      {/* Context note */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="label" style={{ marginBottom: 10 }}>Context</div>
        <textarea
          className="textarea"
          placeholder="What's the broader context of today? What's on your mind?"
          value={entry.contextNote}
          onChange={e => update({ contextNote: e.target.value })}
          style={{ minHeight: 100 }}
        />
        <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="label">Energy</span>
          <input
            type="range" min={1} max={10}
            className="confidence-slider"
            style={{ flex: 1 }}
            value={entry.energyLevel}
            onChange={e => update({ energyLevel: Number(e.target.value) })}
          />
          <span className="mono" style={{ color: 'var(--gold)', minWidth: 20 }}>{entry.energyLevel}</span>
        </div>
      </div>

      {/* Hypotheses */}
      <Section
        title="Hypotheses"
        color="var(--sage)"
        label="H"
        count={entry.hypotheses.length}
        isOpen={activeSection === 'hypothesis'}
        onToggle={() => setActiveSection(s => s === 'hypothesis' ? null : 'hypothesis')}
      >
        <HypothesisForm onAdd={h => {
          update({ hypotheses: [...entry.hypotheses, h] })
          setActiveSection(null)
        }} />
        {entry.hypotheses.map(h => (
          <div key={h.id} className="card" style={{ marginTop: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span className="chip chip-sage">{h.domain}</span>
              <span className="mono" style={{ color: 'var(--gold)', fontSize: 12 }}>{h.confidence}%</span>
            </div>
            <div style={{ fontSize: 14, marginTop: 6 }}>{h.statement}</div>
            <button style={{ marginTop: 8, fontSize: 12, color: 'var(--signal)', background: 'none', border: 'none', cursor: 'pointer' }}
              onClick={() => update({ hypotheses: entry.hypotheses.filter(x => x.id !== h.id) })}>
              Remove
            </button>
          </div>
        ))}
      </Section>

      {/* Decisions */}
      <Section
        title="Decisions"
        color="var(--gold)"
        label="D"
        count={entry.decisions.length}
        isOpen={activeSection === 'decision'}
        onToggle={() => setActiveSection(s => s === 'decision' ? null : 'decision')}
      >
        <DecisionForm onAdd={d => {
          update({ decisions: [...entry.decisions, d] })
          setActiveSection(null)
        }} />
        {entry.decisions.map(d => (
          <div key={d.id} className="card" style={{ marginTop: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span className="chip chip-gold">{d.decisionType}</span>
              <span style={{ fontSize: 12, color: 'var(--slate)' }}>{d.reversible ? 'Reversible' : 'Irreversible'}</span>
            </div>
            <div style={{ fontSize: 14, fontWeight: 500, marginTop: 6 }}>{d.statement}</div>
            <div style={{ fontSize: 13, color: 'var(--slate)', marginTop: 4 }}>{d.rationale}</div>
            <button style={{ marginTop: 8, fontSize: 12, color: 'var(--signal)', background: 'none', border: 'none', cursor: 'pointer' }}
              onClick={() => update({ decisions: entry.decisions.filter(x => x.id !== d.id) })}>
              Remove
            </button>
          </div>
        ))}
      </Section>

      {/* Predictions */}
      <Section
        title="Predictions"
        color="var(--signal)"
        label="P"
        count={entry.predictions.length}
        isOpen={activeSection === 'prediction'}
        onToggle={() => setActiveSection(s => s === 'prediction' ? null : 'prediction')}
      >
        <PredictionForm onAdd={p => {
          update({ predictions: [...entry.predictions, p] })
          setActiveSection(null)
        }} />
        {entry.predictions.map(p => (
          <div key={p.id} className="card" style={{ marginTop: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span className="mono" style={{ fontSize: 12, color: 'var(--signal)' }}>Due {format(new Date(p.deadline + 'T12:00:00'), 'd MMM yyyy')}</span>
              <span className="mono" style={{ color: 'var(--gold)', fontSize: 12 }}>{p.confidence}%</span>
            </div>
            <div style={{ fontSize: 14, fontWeight: 500, marginTop: 6 }}>{p.statement}</div>
            <div style={{ fontSize: 13, color: 'var(--slate)', marginTop: 4 }}>Expected: {p.expectedOutcome}</div>
            <button style={{ marginTop: 8, fontSize: 12, color: 'var(--signal)', background: 'none', border: 'none', cursor: 'pointer' }}
              onClick={() => update({ predictions: entry.predictions.filter(x => x.id !== p.id) })}>
              Remove
            </button>
          </div>
        ))}
      </Section>
    </div>
  )
}

// ──────────────────────────────────────────────────
// Section wrapper
// ──────────────────────────────────────────────────

function Section({ title, color, label, count, isOpen, onToggle, children }: {
  title: string; color: string; label: string; count: number
  isOpen: boolean; onToggle: () => void; children: React.ReactNode
}) {
  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }} onClick={onToggle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ color, fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>{label}</span>
          <span style={{ fontWeight: 500 }}>{title}</span>
          {count > 0 && <span style={{ fontSize: 12, color: 'var(--slate)' }}>{count}</span>}
        </div>
        <span style={{ color: 'var(--slate)', fontSize: 20, transform: isOpen ? 'rotate(45deg)' : 'none', transition: '180ms' }}>+</span>
      </div>
      {isOpen && <div style={{ marginTop: 16 }}>{children}</div>}
    </div>
  )
}

// ──────────────────────────────────────────────────
// Mini-forms
// ──────────────────────────────────────────────────

function HypothesisForm({ onAdd }: { onAdd: (h: Hypothesis) => void }) {
  const [statement, setStatement] = useState('')
  const [domain, setDomain] = useState<HypothesisDomain>('BUSINESS')
  const [confidence, setConfidence] = useState(60)
  const domains: HypothesisDomain[] = ['BUSINESS', 'PERSONAL', 'PEOPLE', 'MARKET', 'TECHNICAL', 'PHILOSOPHICAL']

  const submit = () => {
    if (!statement.trim()) return
    onAdd({ id: crypto.randomUUID(), statement, domain, confidence, validatedAt: null, wasCorrect: null })
    setStatement('')
  }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 12 }}>
      <textarea className="textarea" placeholder="I believe that…" value={statement} onChange={e => setStatement(e.target.value)} style={{ minHeight: 70 }} />
      <div style={{ display: 'flex', gap: 10 }}>
        <select className="select" value={domain} onChange={e => setDomain(e.target.value as HypothesisDomain)}>
          {domains.map(d => <option key={d}>{d}</option>)}
        </select>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--slate)', marginBottom: 4 }}>
            <span>Confidence</span><span className="mono" style={{ color: 'var(--gold)' }}>{confidence}%</span>
          </div>
          <input type="range" min={0} max={100} className="confidence-slider" value={confidence} onChange={e => setConfidence(Number(e.target.value))} />
        </div>
      </div>
      <button className="btn btn-ghost" onClick={submit} style={{ alignSelf: 'flex-end' }}>Add hypothesis</button>
    </div>
  )
}

function DecisionForm({ onAdd }: { onAdd: (d: Decision) => void }) {
  const [statement, setStatement] = useState('')
  const [rationale, setRationale] = useState('')
  const [type, setType] = useState<DecisionType>('STRATEGIC')
  const [reversible, setReversible] = useState(true)
  const types: DecisionType[] = ['STRATEGIC', 'TACTICAL', 'PERSONAL', 'FINANCIAL', 'RELATIONAL']

  const submit = () => {
    if (!statement.trim()) return
    onAdd({ id: crypto.randomUUID(), statement, rationale, alternatives: [], decisionType: type, reversible, tags: [], reviewAfterWeeks: 4, outcomeNote: null, wouldRepeat: null, outcomeRating: null, reviewedAt: null })
    setStatement(''); setRationale('')
  }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 12 }}>
      <textarea className="textarea" placeholder="I decided to…" value={statement} onChange={e => setStatement(e.target.value)} style={{ minHeight: 60 }} />
      <textarea className="textarea" placeholder="Because…" value={rationale} onChange={e => setRationale(e.target.value)} style={{ minHeight: 60 }} />
      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
        <select className="select" value={type} onChange={e => setType(e.target.value as DecisionType)}>
          {types.map(t => <option key={t}>{t}</option>)}
        </select>
        <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 13, color: 'var(--slate)', cursor: 'pointer', whiteSpace: 'nowrap' }}>
          <input type="checkbox" checked={reversible} onChange={e => setReversible(e.target.checked)} />
          Reversible
        </label>
      </div>
      <button className="btn btn-ghost" onClick={submit} style={{ alignSelf: 'flex-end' }}>Add decision</button>
    </div>
  )
}

function PredictionForm({ onAdd }: { onAdd: (p: Prediction) => void }) {
  const [statement, setStatement] = useState('')
  const [expectedOutcome, setExpectedOutcome] = useState('')
  const [deadline, setDeadline] = useState(format(addDays(new Date(), 30), 'yyyy-MM-dd'))
  const [confidence, setConfidence] = useState(65)

  const submit = () => {
    if (!statement.trim()) return
    onAdd({ id: crypto.randomUUID(), statement, expectedOutcome, deadline, confidence, tags: [], actualOutcome: null, wasCorrect: null, reviewedAt: null, score: null })
    setStatement(''); setExpectedOutcome('')
  }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 12 }}>
      <textarea className="textarea" placeholder="I predict that…" value={statement} onChange={e => setStatement(e.target.value)} style={{ minHeight: 60 }} />
      <textarea className="textarea" placeholder="The measurable outcome will be…" value={expectedOutcome} onChange={e => setExpectedOutcome(e.target.value)} style={{ minHeight: 60 }} />
      <div style={{ display: 'flex', gap: 10 }}>
        <div className="field" style={{ flex: 1 }}>
          <label>Deadline</label>
          <input type="date" className="input" value={deadline} onChange={e => setDeadline(e.target.value)} />
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--slate)', marginBottom: 4 }}>
            <span>Confidence</span><span className="mono" style={{ color: 'var(--gold)' }}>{confidence}%</span>
          </div>
          <input type="range" min={0} max={100} className="confidence-slider" value={confidence} onChange={e => setConfidence(Number(e.target.value))} style={{ marginTop: 14 }} />
        </div>
      </div>
      <button className="btn btn-ghost" onClick={submit} style={{ alignSelf: 'flex-end' }}>Add prediction</button>
    </div>
  )
}
