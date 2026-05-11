import { defineConfig, devices } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/**
 * The app is served with a base path when GITHUB_REPOSITORY is set
 * (e.g. /StrategicJournal/ on GitHub Pages / vite preview in CI).
 * We detect this from the environment and set baseURL accordingly
 * so all page.goto('/entry') calls resolve correctly.
 */
const repo = process.env.GITHUB_REPOSITORY
const basePath = repo ? `/${repo.split('/')[1]}` : ''
const baseURL = `http://localhost:4173${basePath}`

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
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    storageState: undefined,
  },

  webServer: {
    // In CI, PLAYWRIGHT_WEB_SERVER_COMMAND skips the build (dist already exists).
    // Locally, we build first to ensure dist/ is present.
    command: process.env.PLAYWRIGHT_WEB_SERVER_COMMAND
      ?? 'npm run build && npm run preview -- --port 4173',
    // Always check the root — vite preview responds at / regardless of base path.
    url: 'http://localhost:4173',
    cwd: process.env.WEB_DIR ?? path.resolve(__dirname, '../../web'),
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
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
