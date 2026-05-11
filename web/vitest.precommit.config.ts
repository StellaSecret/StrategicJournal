import { defineConfig } from 'vitest/config'

/**
 * Vitest config used ONLY by the pre-commit hook.
 * Pre-commit runs from the repo root, so patterns must be prefixed with web/.
 *
 * CI uses `npm run test` inside web/ which picks up vite.config.ts directly
 * and doesn't use this file.
 */
export default defineConfig({
  test: {
    environment: 'node',
    include: [
      'web/src/**/*.test.ts',
      'web/src/**/*.test.tsx',
      'web/src/**/*.spec.ts',
      'web/src/**/*.spec.tsx',
    ],
    exclude: [
      '**/node_modules/**',
      '**/dist/**',
      'e2e/**',
    ],
  },
})
