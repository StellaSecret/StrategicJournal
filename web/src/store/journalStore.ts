import { create } from 'zustand'
import type { JournalEntry, CognitiveStats } from '../types'
import { computeStats } from '../types'
import { localDB } from '../storage/localDB'
import { driveSync } from '../storage/driveSync'

interface SyncStatus {
  state: 'idle' | 'syncing' | 'error' | 'success'
  lastSync: string | null
  message?: string
}

interface JournalStore {
  entries: JournalEntry[]
  stats: CognitiveStats | null
  syncStatus: SyncStatus
  isAuthenticated: boolean

  // Actions
  loadEntries: () => Promise<void>
  saveEntry: (entry: JournalEntry) => Promise<void>
  deleteEntry: (id: string) => Promise<void>
  syncToCloud: () => Promise<void>
  syncFromCloud: () => Promise<void>
  setAuthenticated: (token: string) => void
  signOut: () => void
}

export const useJournalStore = create<JournalStore>((set, get) => ({
  entries: [],
  stats: null,
  syncStatus: { state: 'idle', lastSync: null },
  isAuthenticated: false,

  loadEntries: async () => {
    const entries = await localDB.getAllEntries()
    const lastSync = await localDB.getLastSyncTime()
    set({
      entries,
      stats: computeStats(entries),
      syncStatus: {
        state: 'idle',
        lastSync,
      }
    })
  },

  saveEntry: async (entry) => {
    await localDB.saveEntry(entry)
    const entries = await localDB.getAllEntries()
    set({ entries, stats: computeStats(entries) })
  },

  deleteEntry: async (id) => {
    await localDB.deleteEntry(id)
    const entries = await localDB.getAllEntries()
    set({ entries, stats: computeStats(entries) })
  },

  syncToCloud: async () => {
    if (!get().isAuthenticated) return
    set(s => ({ syncStatus: { ...s.syncStatus, state: 'syncing' } }))
    try {
      const { pushed, errors } = await driveSync.syncToCloud()
      const entries = await localDB.getAllEntries()
      set({
        entries,
        stats: computeStats(entries),
        syncStatus: {
          state: errors.length > 0 ? 'error' : 'success',
          lastSync: new Date().toISOString(),
          message: errors.length > 0
            ? `${errors.length} errors during sync`
            : `${pushed} entries synced`
        }
      })
    } catch (e) {
      set(s => ({
        syncStatus: {
          ...s.syncStatus,
          state: 'error',
          message: (e as Error).message
        }
      }))
    }
  },

  syncFromCloud: async () => {
    if (!get().isAuthenticated) return
    set(s => ({ syncStatus: { ...s.syncStatus, state: 'syncing' } }))
    try {
      const { pulled, errors } = await driveSync.syncFromCloud()
      const entries = await localDB.getAllEntries()
      set({
        entries,
        stats: computeStats(entries),
        syncStatus: {
          state: errors.length > 0 ? 'error' : 'success',
          lastSync: new Date().toISOString(),
          message: `${pulled} entries pulled from Drive`
        }
      })
    } catch (e) {
      set(s => ({
        syncStatus: {
          ...s.syncStatus,
          state: 'error',
          message: (e as Error).message
        }
      }))
    }
  },

  setAuthenticated: (token: string) => {
    driveSync.setToken(token)
    set({ isAuthenticated: true })
    // Auto-pull on auth
    get().syncFromCloud()
  },

  signOut: () => {
    driveSync.setToken('')
    set({ isAuthenticated: false })
  },
}))
