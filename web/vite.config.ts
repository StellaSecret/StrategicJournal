import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],

  // For GitHub Pages: set VITE_GITHUB_PAGES_BASE to your repo name (e.g. 'StrategicJournal').
  // GITHUB_REPOSITORY is a built-in runner variable that cannot be blanked out in workflow env:
  // blocks, so we use a separate opt-in variable instead.
  base: process.env.VITE_GITHUB_PAGES_BASE
    ? `/${process.env.VITE_GITHUB_PAGES_BASE}/`
    : '/',

  build: {
    outDir: 'dist',
    sourcemap: true,
    rollupOptions: {
      output: {
        // Vite 8 / Rolldown requires manualChunks to be a function, not an object.
        manualChunks(id) {
          if (
            id.includes('node_modules/react') ||
            id.includes('node_modules/react-dom') ||
            id.includes('node_modules/react-router-dom')
          ) {
            return 'vendor'
          }
          if (id.includes('node_modules/zustand')) {
            return 'store'
          }
          if (id.includes('node_modules/date-fns') || id.includes('node_modules/idb')) {
            return 'utils'
          }
        },
      },
    },
  },

  define: {
    __APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || 'dev'),
  },
})
