// Mirrors the Kotlin domain models exactly
// so JSON from Drive is interoperable between Android and Web

export interface JournalEntry {
  id: string
  date: string          // 'YYYY-MM-DD'
  createdAt: string
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
  tags: string[]
  // Review fields
  outcomeNote?: string | null
  wouldRepeat?: boolean | null
  outcomeRating?: number | null   // 1-5
  reviewAfterWeeks: number        // default 4
  reviewedAt?: string | null
}

export interface Prediction {
  id: string
  statement: string
  expectedOutcome: string
  deadline: string      // 'YYYY-MM-DD'
  confidence: number    // 0-100
  tags: string[]
  actualOutcome?: string | null
  wasCorrect?: boolean | null
  reviewedAt?: string | null
  score?: number | null
}

export type HypothesisDomain =
  | 'BUSINESS' | 'PERSONAL' | 'PEOPLE'
  | 'MARKET' | 'TECHNICAL' | 'PHILOSOPHICAL'

export type DecisionType =
  | 'STRATEGIC' | 'TACTICAL' | 'PERSONAL' | 'FINANCIAL' | 'RELATIONAL'

// ──────────────────────────────────────────────
// Analytics
// ──────────────────────────────────────────────

export interface CalibrationBucket {
  confidenceMidpoint: number   // 10, 20, ... 90
  predictedRate: number
  actualRate: number
  sampleSize: number
}

export interface TagStats {
  tag: string
  accuracy: number
  sampleSize: number
}

export interface CognitiveStats {
  totalEntries: number
  streakDays: number
  // Predictions
  predictionAccuracy: number
  averageConfidenceWhenRight: number
  averageConfidenceWhenWrong: number
  calibrationScore: number
  calibrationBuckets: CalibrationBucket[]
  // Decisions
  decisionsReviewed: number
  wouldRepeatRate: number
  avgOutcomeRating: number
  decisionAccuracyByType: Partial<Record<DecisionType, number>>
  // Tags
  predictionAccuracyByTag: Record<string, TagStats>
  decisionQualityByTag: Record<string, TagStats>
  // Nudges
  pendingDecisionReviews: number
  pendingPredictionReviews: number
}

// ──────────────────────────────────────────────
// computeStats — runs entirely client-side
// ──────────────────────────────────────────────

export function computeStats(entries: JournalEntry[]): CognitiveStats {
  const today = new Date().toISOString().split('T')[0]

  // ── Streak ──────────────────────────────────
  const dates = [...new Set(entries.map(e => e.date))].sort().reverse()
  let streak = 0
  let current = today
  for (const date of dates) {
    if (date === current) {
      streak++
      const d = new Date(current)
      d.setDate(d.getDate() - 1)
      current = d.toISOString().split('T')[0]
    } else break
  }

  // ── Predictions ─────────────────────────────
  const allPredictions = entries.flatMap(e => e.predictions)
  const reviewed = allPredictions.filter(p => p.wasCorrect !== null && p.wasCorrect !== undefined)
  const correct = reviewed.filter(p => p.wasCorrect === true)
  const wrong = reviewed.filter(p => p.wasCorrect === false)

  const accuracy = reviewed.length > 0 ? correct.length / reviewed.length : 0
  const avgConfRight = correct.length > 0
    ? correct.reduce((s, p) => s + p.confidence, 0) / correct.length : 0
  const avgConfWrong = wrong.length > 0
    ? wrong.reduce((s, p) => s + p.confidence, 0) / wrong.length : 0
  const avgConf = reviewed.length > 0
    ? reviewed.reduce((s, p) => s + p.confidence, 0) / reviewed.length : 0
  const calibrationScore = 1 - Math.abs(avgConf / 100 - accuracy)

  // ── Calibration buckets (10 buckets of 10%) ──
  const calibrationBuckets: CalibrationBucket[] = []
  for (let mid = 10; mid <= 90; mid += 10) {
    const lo = mid - 5, hi = mid + 5
    const bucket = reviewed.filter(p => p.confidence >= lo && p.confidence < hi)
    calibrationBuckets.push({
      confidenceMidpoint: mid,
      predictedRate: mid / 100,
      actualRate: bucket.length > 0
        ? bucket.filter(p => p.wasCorrect).length / bucket.length : 0,
      sampleSize: bucket.length
    })
  }

  // ── Pending prediction reviews ───────────────
  const pendingPredictionReviews = allPredictions.filter(p =>
    p.wasCorrect === null && p.wasCorrect !== false &&
    new Date(p.deadline + 'T23:59:59') < new Date()
  ).length

  // ── Decisions ────────────────────────────────
  const allDecisions = entries.flatMap(e => e.decisions)
  const reviewedDecisions = allDecisions.filter(d => d.reviewedAt !== null)
  const wouldRepeatDecisions = reviewedDecisions.filter(d => d.wouldRepeat === true)

  const wouldRepeatRate = reviewedDecisions.length > 0
    ? wouldRepeatDecisions.length / reviewedDecisions.length : 0

  const ratedDecisions = reviewedDecisions.filter(d => d.outcomeRating !== null)
  const avgOutcomeRating = ratedDecisions.length > 0
    ? ratedDecisions.reduce((s, d) => s + (d.outcomeRating ?? 0), 0) / ratedDecisions.length : 0

  // wouldRepeat rate by DecisionType
  const decisionAccuracyByType: Partial<Record<DecisionType, number>> = {}
  const types: DecisionType[] = ['STRATEGIC', 'TACTICAL', 'PERSONAL', 'FINANCIAL', 'RELATIONAL']
  for (const type of types) {
    const typed = reviewedDecisions.filter(d => d.decisionType === type)
    if (typed.length > 0) {
      decisionAccuracyByType[type] = typed.filter(d => d.wouldRepeat).length / typed.length
    }
  }

  // Pending decision reviews (past reviewAfterWeeks, not yet reviewed)
  const pendingDecisionReviews = allDecisions.filter(d => {
    if (d.reviewedAt !== null) return false
    const entry = entries.find(e => e.decisions.some(dec => dec.id === d.id))
    if (!entry) return false
    const dueDate = new Date(entry.date)
    dueDate.setDate(dueDate.getDate() + (d.reviewAfterWeeks ?? 4) * 7)
    return dueDate < new Date()
  }).length

  // ── Tag intelligence ─────────────────────────
  const predictionAccuracyByTag: Record<string, TagStats> = {}
  const decisionQualityByTag: Record<string, TagStats> = {}

  // Prediction accuracy by tag
  for (const p of reviewed) {
    for (const tag of (p.tags ?? [])) {
      if (!predictionAccuracyByTag[tag]) {
        predictionAccuracyByTag[tag] = { tag, accuracy: 0, sampleSize: 0 }
      }
      const s = predictionAccuracyByTag[tag]
      s.accuracy = (s.accuracy * s.sampleSize + (p.wasCorrect ? 1 : 0)) / (s.sampleSize + 1)
      s.sampleSize++
    }
  }

  // Decision quality (wouldRepeat) by tag
  for (const d of reviewedDecisions) {
    for (const tag of (d.tags ?? [])) {
      if (!decisionQualityByTag[tag]) {
        decisionQualityByTag[tag] = { tag, accuracy: 0, sampleSize: 0 }
      }
      const s = decisionQualityByTag[tag]
      s.accuracy = (s.accuracy * s.sampleSize + (d.wouldRepeat ? 1 : 0)) / (s.sampleSize + 1)
      s.sampleSize++
    }
  }

  return {
    totalEntries: entries.length,
    streakDays: streak,
    predictionAccuracy: accuracy,
    averageConfidenceWhenRight: avgConfRight,
    averageConfidenceWhenWrong: avgConfWrong,
    calibrationScore,
    calibrationBuckets,
    decisionsReviewed: reviewedDecisions.length,
    wouldRepeatRate,
    avgOutcomeRating,
    decisionAccuracyByType,
    predictionAccuracyByTag,
    decisionQualityByTag,
    pendingDecisionReviews,
    pendingPredictionReviews,
  }
}
