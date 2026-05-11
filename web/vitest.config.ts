import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    // 'node' is vitest's built-in environment — no extra packages needed.
    // The existing unit tests (types.test.ts) test pure data functions
    // and don't require a DOM environment.
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
