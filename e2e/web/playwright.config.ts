import { defineConfig, devices } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/**
 * Playwright configuration for Strategic Journal web E2E tests.
 *
 * Run locally:  npx playwright test
 * In CI:        set CI=true — Playwright switches to 1 retry, no headed mode.
 *
 * WEB_DIR env var (set by CI) points to the web/ directory so vite preview
 * runs from the right place. Falls back to ../../web relative to this file.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
  ],

  use: {
    baseURL: 'http://localhost:4173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    storageState: undefined,
  },

  webServer: {
    command: 'npm run preview -- --port 4173',
    url: 'http://localhost:4173',
    cwd: process.env.WEB_DIR ?? path.resolve(__dirname, '../../web'),
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 7'] },
    },
  ],
})
