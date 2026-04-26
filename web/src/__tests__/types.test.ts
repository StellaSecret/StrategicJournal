import { describe, it, expect } from 'vitest'
import { computeStats } from '../types'

describe('computeStats', () => {
  it('returns zero stats for empty entries', () => {
    const stats = computeStats([])
    expect(stats.totalEntries).toBe(0)
    expect(stats.predictionAccuracy).toBe(0)
    expect(stats.streakDays).toBe(0)
  })

  it('calculates prediction accuracy correctly', () => {
    const entry = {
      id: '1',
      date: '2024-01-01',
      createdAt: '',
      updatedAt: '',
      hypotheses: [],
      decisions: [],
      predictions: [
        { id: 'p1', statement: 'A', expectedOutcome: 'B', deadline: '2024-01-01',
          confidence: 80, wasCorrect: true, actualOutcome: null, reviewedAt: null, score: null },
        { id: 'p2', statement: 'C', expectedOutcome: 'D', deadline: '2024-01-01',
          confidence: 60, wasCorrect: false, actualOutcome: null, reviewedAt: null, score: null },
      ],
      contextNote: '',
      energyLevel: 7,
      tags: [],
    }
    const stats = computeStats([entry])
    expect(stats.predictionAccuracy).toBe(0.5)
    expect(stats.totalEntries).toBe(1)
  })
})
