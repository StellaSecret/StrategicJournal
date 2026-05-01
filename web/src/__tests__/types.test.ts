import { describe, it, expect } from 'vitest'
import { computeStats } from '../types'
import type { JournalEntry, Decision, Prediction } from '../types'

const makeEntry = (overrides: Partial<JournalEntry> = {}): JournalEntry => ({
  id: crypto.randomUUID(),
  date: '2024-01-01',
  createdAt: '',
  updatedAt: '',
  hypotheses: [],
  decisions: [],
  predictions: [],
  contextNote: '',
  energyLevel: 7,
  tags: [],
  ...overrides
})

const makePrediction = (overrides: Partial<Prediction> = {}): Prediction => ({
  id: crypto.randomUUID(),
  statement: 'Test prediction',
  expectedOutcome: 'Expected',
  deadline: '2024-01-01',
  confidence: 70,
  tags: [],
  actualOutcome: null,
  wasCorrect: null,
  reviewedAt: null,
  score: null,
  ...overrides
})

const makeDecision = (overrides: Partial<Decision> = {}): Decision => ({
  id: crypto.randomUUID(),
  statement: 'Test decision',
  rationale: 'Because',
  alternatives: [],
  decisionType: 'STRATEGIC',
  reversible: true,
  tags: [],
  reviewAfterWeeks: 4,
  outcomeNote: null,
  wouldRepeat: null,
  outcomeRating: null,
  reviewedAt: null,
  ...overrides
})

describe('computeStats', () => {
  it('returns zero stats for empty entries', () => {
    const stats = computeStats([])
    expect(stats.totalEntries).toBe(0)
    expect(stats.predictionAccuracy).toBe(0)
    expect(stats.streakDays).toBe(0)
    expect(stats.wouldRepeatRate).toBe(0)
    expect(stats.calibrationBuckets).toHaveLength(9)
  })

  it('calculates prediction accuracy correctly', () => {
    const entry = makeEntry({
      predictions: [
        makePrediction({ confidence: 80, wasCorrect: true }),
        makePrediction({ confidence: 60, wasCorrect: false }),
      ]
    })
    const stats = computeStats([entry])
    expect(stats.predictionAccuracy).toBe(0.5)
    expect(stats.totalEntries).toBe(1)
  })

  it('calculates decision wouldRepeatRate correctly', () => {
    const entry = makeEntry({
      decisions: [
        makeDecision({ wouldRepeat: true, outcomeRating: 4, reviewedAt: '2024-02-01', decisionType: 'STRATEGIC' }),
        makeDecision({ wouldRepeat: false, outcomeRating: 2, reviewedAt: '2024-02-01', decisionType: 'TACTICAL' }),
      ]
    })
    const stats = computeStats([entry])
    expect(stats.decisionsReviewed).toBe(2)
    expect(stats.wouldRepeatRate).toBe(0.5)
    expect(stats.avgOutcomeRating).toBe(3)
  })

  it('computes tag accuracy for predictions', () => {
    const entry = makeEntry({
      predictions: [
        makePrediction({ tags: ['startup'], wasCorrect: true }),
        makePrediction({ tags: ['startup'], wasCorrect: true }),
      ]
    })
    const stats = computeStats([entry])
    expect(stats.predictionAccuracyByTag['startup']?.accuracy).toBe(1)
    expect(stats.predictionAccuracyByTag['startup']?.sampleSize).toBe(2)
  })

  it('flags pending decision reviews', () => {
    const fiveWeeksAgo = new Date()
    fiveWeeksAgo.setDate(fiveWeeksAgo.getDate() - 35)
    const entry = makeEntry({
      date: fiveWeeksAgo.toISOString().split('T')[0],
      decisions: [makeDecision({ reviewAfterWeeks: 4, reviewedAt: null })]
    })
    const stats = computeStats([entry])
    expect(stats.pendingDecisionReviews).toBe(1)
  })
})
