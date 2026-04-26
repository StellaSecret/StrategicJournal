// Mirrors the Kotlin domain models exactly
// so JSON from Drive is interoperable between Android and Web

export interface JournalEntry {
  id: string
  date: string          // 'YYYY-MM-DD'
  createdAt: string     // ISO datetime
  updatedAt: string
  hypotheses: Hypothesis[]
  decisions: Decision[]
  predictions: Prediction[]
  contextNote: string
  energyLevel: number   // 1-10
  tags: string[]
  isDirty?: boolean
  driveFileId?: string | null
}

export interface Hypothesis {
  id: string
  statement: string
  domain: HypothesisDomain
  confidence: number    // 0-100
  validatedAt?: string | null
  wasCorrect?: boolean | null
}

export interface Decision {
  id: string
  statement: string
  rationale: string
  alternatives: string[]
  decisionType: DecisionType
  reversible: boolean
  outcomeNote?: string | null
  reviewedAt?: string | null
}

export interface Prediction {
  id: string
  statement: string
  expectedOutcome: string
  deadline: string      // 'YYYY-MM-DD'
  confidence: number    // 0-100
  actualOutcome?: string | null
  wasCorrect?: boolean | null
  reviewedAt?: string | null
  score?: number | null
}

export type HypothesisDomain =
  | 'BUSINESS'
  | 'PERSONAL'
  | 'PEOPLE'
  | 'MARKET'
  | 'TECHNICAL'
  | 'PHILOSOPHICAL'

export type DecisionType =
  | 'STRATEGIC'
  | 'TACTICAL'
  | 'PERSONAL'
  | 'FINANCIAL'
  | 'RELATIONAL'

export interface CognitiveStats {
  totalEntries: number
  predictionAccuracy: number
  averageConfidenceWhenRight: number
  averageConfidenceWhenWrong: number
  calibrationScore: number
  streakDays: number
}

// Computed from entries — derives accuracy, calibration, etc.
export function computeStats(entries: JournalEntry[]): CognitiveStats {
  const allPredictions = entries.flatMap(e => e.predictions)
  const reviewed = allPredictions.filter(p => p.wasCorrect !== null && p.wasCorrect !== undefined)
  const correct = reviewed.filter(p => p.wasCorrect === true)

  const accuracy = reviewed.length > 0 ? correct.length / reviewed.length : 0

  const avgConfRight = correct.length > 0
    ? correct.reduce((sum, p) => sum + p.confidence, 0) / correct.length
    : 0

  const wrong = reviewed.filter(p => p.wasCorrect === false)
  const avgConfWrong = wrong.length > 0
    ? wrong.reduce((sum, p) => sum + p.confidence, 0) / wrong.length
    : 0

  // Simple calibration: how close was avg confidence to actual accuracy
  const avgConf = reviewed.length > 0
    ? reviewed.reduce((sum, p) => sum + p.confidence, 0) / reviewed.length
    : 0
  const calibration = 1 - Math.abs(avgConf / 100 - accuracy)

  // Streak: consecutive days with entries
  const dates = [...new Set(entries.map(e => e.date))].sort().reverse()
  let streak = 0
  const today = new Date().toISOString().split('T')[0]
  let current = today
  for (const date of dates) {
    if (date === current) {
      streak++
      const d = new Date(current)
      d.setDate(d.getDate() - 1)
      current = d.toISOString().split('T')[0]
    } else {
      break
    }
  }

  return {
    totalEntries: entries.length,
    predictionAccuracy: accuracy,
    averageConfidenceWhenRight: avgConfRight,
    averageConfidenceWhenWrong: avgConfWrong,
    calibrationScore: calibration,
    streakDays: streak,
  }
}
