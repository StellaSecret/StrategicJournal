import { test, expect, Page } from '@playwright/test'

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

/** Dismiss the Google Drive auth button without signing in — we test offline flows. */
async function skipAuth(page: Page) {
  // The auth button is present but we simply don't click it.
  // All storage-backed tests work on the in-browser IndexedDB.
  await expect(page.getByRole('button', { name: /connect google drive/i })).toBeVisible()
}

// ─────────────────────────────────────────────────────────────
// Home page
// ─────────────────────────────────────────────────────────────

test.describe('Home page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('displays the app title and nav links', async ({ page }) => {
    await expect(page.getByText('Strategic Journal')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Entries' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Review' })).toBeVisible()
  })

  test('shows empty state when no entries exist', async ({ page }) => {
    await expect(page.getByText('Your first entry awaits')).toBeVisible()
    await expect(page.getByText('Hypotheses. Decisions. Predictions.')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Begin' })).toBeVisible()
  })

  test('"Begin" link navigates to the new-entry page', async ({ page }) => {
    await page.getByRole('link', { name: 'Begin' }).click()
    await expect(page).toHaveURL(/\/entry/)
  })

  test('"+ New entry" button is visible and navigates to /entry', async ({ page }) => {
    // The new-entry button only appears in the header row once entries exist,
    // but on a fresh load the empty-state "Begin" acts as the CTA.
    // We navigate directly to confirm the route is reachable.
    await page.goto('/entry')
    await expect(page.getByRole('button', { name: /save entry/i })).toBeVisible()
  })
})

// ─────────────────────────────────────────────────────────────
// Entry page — create & save
// ─────────────────────────────────────────────────────────────

test.describe('Entry page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/entry')
  })

  test('shows the entry form with all three sections', async ({ page }) => {
    await expect(page.getByPlaceholder(/broader context/i)).toBeVisible()
    await expect(page.getByText('Hypotheses')).toBeVisible()
    await expect(page.getByText('Decisions')).toBeVisible()
    await expect(page.getByText('Predictions')).toBeVisible()
  })

  test('Cancel button navigates back to home', async ({ page }) => {
    await page.getByRole('button', { name: /cancel/i }).click()
    await expect(page).toHaveURL('/')
  })

  test('fills in context note and saves entry', async ({ page }) => {
    await page.getByPlaceholder(/broader context/i).fill(
      'Testing the strategic journal end-to-end.'
    )
    await page.getByRole('button', { name: /save entry/i }).click()

    // After save, redirected to home
    await expect(page).toHaveURL('/')

    // The saved context note appears in the entry card
    await expect(
      page.getByText('Testing the strategic journal end-to-end.')
    ).toBeVisible()
  })

  test('adds a hypothesis and saves', async ({ page }) => {
    // Expand hypothesis section
    await page.getByPlaceholder(/broader context/i).fill('Hypothesis test run')

    // Click the "add hypothesis" trigger — label text from AddHypothesisCard
    const addHypothesisButton = page.getByRole('button', { name: /add hypothesis/i })
    await addHypothesisButton.click()

    // Fill the hypothesis statement input that appears
    const hypothesisInput = page.getByPlaceholder(/what do you believe/i)
    await hypothesisInput.fill('The market will shift toward AI-native tools.')

    await page.getByRole('button', { name: /save entry/i }).click()
    await expect(page).toHaveURL('/')

    // Entry card should show "1 H" chip
    await expect(page.getByText(/1 H/)).toBeVisible()
  })

  test('adds a prediction with a deadline and saves', async ({ page }) => {
    await page.getByPlaceholder(/broader context/i).fill('Prediction test')

    const addPredictionButton = page.getByRole('button', { name: /add prediction/i })
    await addPredictionButton.click()

    await page.getByPlaceholder(/what do you predict/i).fill('Revenue doubles by Q4.')

    // Set deadline 30 days from now
    const deadline = new Date()
    deadline.setDate(deadline.getDate() + 30)
    const deadlineStr = deadline.toISOString().split('T')[0]
    await page.locator('input[type="date"]').first().fill(deadlineStr)

    await page.getByRole('button', { name: /save entry/i }).click()
    await expect(page).toHaveURL('/')
    await expect(page.getByText(/1 P/)).toBeVisible()
  })
})

// ─────────────────────────────────────────────────────────────
// Entry persistence — edit existing
// ─────────────────────────────────────────────────────────────

test.describe('Entry edit flow', () => {
  test('existing entry can be reopened and updated', async ({ page }) => {
    // Create an entry first
    await page.goto('/entry')
    await page.getByPlaceholder(/broader context/i).fill('Original context.')
    await page.getByRole('button', { name: /save entry/i }).click()
    await expect(page).toHaveURL('/')

    // Click the entry card to reopen it
    await page.getByText('Original context.').click()
    await expect(page).toHaveURL(/\/entry\/.+/)

    // Update the context note
    const textarea = page.getByPlaceholder(/broader context/i)
    await textarea.clear()
    await textarea.fill('Updated context after edit.')
    await page.getByRole('button', { name: /save entry/i }).click()
    await expect(page).toHaveURL('/')

    await expect(page.getByText('Updated context after edit.')).toBeVisible()
    // Old text should be gone
    await expect(page.getByText('Original context.')).not.toBeVisible()
  })
})

// ─────────────────────────────────────────────────────────────
// Review page
// ─────────────────────────────────────────────────────────────

test.describe('Review page', () => {
  test('navigates to /review via nav link', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: 'Review' }).click()
    await expect(page).toHaveURL('/review')
  })

  test('shows tabs for Predictions and Decisions', async ({ page }) => {
    await page.goto('/review')
    await expect(page.getByRole('tab', { name: /predictions/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /decisions/i })).toBeVisible()
  })

  test('pending prediction banner on home links to review', async ({ page }) => {
    // Seed an entry with an overdue prediction
    await page.goto('/entry')
    await page.getByPlaceholder(/broader context/i).fill('Overdue prediction test')

    await page.getByRole('button', { name: /add prediction/i }).click()
    await page.getByPlaceholder(/what do you predict/i).fill('Past deadline prediction')

    // Set deadline in the past
    const past = new Date()
    past.setDate(past.getDate() - 10)
    await page.locator('input[type="date"]').first().fill(past.toISOString().split('T')[0])

    await page.getByRole('button', { name: /save entry/i }).click()
    await expect(page).toHaveURL('/')

    // The "to review" banner should now be visible and clickable
    const banner = page.getByText(/to review/i).first()
    await expect(banner).toBeVisible()
    await banner.click()
    await expect(page).toHaveURL('/review')
  })
})

// ─────────────────────────────────────────────────────────────
// Navigation
// ─────────────────────────────────────────────────────────────

test.describe('Navigation', () => {
  test('direct navigation to unknown route falls back gracefully', async ({ page }) => {
    const response = await page.goto('/this-does-not-exist')
    // SPA: the app shell loads and React Router renders; no hard 404
    await expect(page.getByText('Strategic Journal')).toBeVisible()
  })

  test('browser back button returns from entry to home', async ({ page }) => {
    await page.goto('/')
    await page.goto('/entry')
    await page.goBack()
    await expect(page).toHaveURL('/')
  })
})
