import { openDB, DBSchema, IDBPDatabase } from 'idb'
import type { JournalEntry } from '../types'

interface JournalDB extends DBSchema {
  entries: {
    key: string
    value: JournalEntry
    indexes: {
      'by-date': string
      'by-dirty': number
    }
  }
  meta: {
    key: string
    value: string
  }
}

let db: IDBPDatabase<JournalDB> | null = null

async function getDB(): Promise<IDBPDatabase<JournalDB>> {
  if (db) return db
  db = await openDB<JournalDB>('strategic-journal', 1, {
    upgrade(db) {
      const store = db.createObjectStore('entries', { keyPath: 'id' })
      store.createIndex('by-date', 'date')
      store.createIndex('by-dirty', 'isDirty')
      db.createObjectStore('meta')
    }
  })
  return db
}

export const localDB = {
  async getAllEntries(): Promise<JournalEntry[]> {
    const db = await getDB()
    const all = await db.getAll('entries')
    return all.sort((a, b) => b.date.localeCompare(a.date))
  },

  async getEntry(id: string): Promise<JournalEntry | undefined> {
    const db = await getDB()
    return db.get('entries', id)
  },

  async getEntryByDate(date: string): Promise<JournalEntry | undefined> {
    const db = await getDB()
    const index = db.transaction('entries').store.index('by-date')
    return index.get(date)
  },

  async saveEntry(entry: JournalEntry): Promise<void> {
    const db = await getDB()
    await db.put('entries', { ...entry, isDirty: true })
  },

  async markSynced(id: string, driveFileId: string): Promise<void> {
    const db = await getDB()
    const entry = await db.get('entries', id)
    if (entry) {
      await db.put('entries', { ...entry, isDirty: false, driveFileId })
    }
  },

  async getDirtyEntries(): Promise<JournalEntry[]> {
    const db = await getDB()
    const all = await db.getAll('entries')
    return all.filter(e => e.isDirty)
  },

  async deleteEntry(id: string): Promise<void> {
    const db = await getDB()
    await db.delete('entries', id)
  },

  async getLastSyncTime(): Promise<string | null> {
    const db = await getDB()
    return (await db.get('meta', 'lastSync')) ?? null
  },

  async setLastSyncTime(time: string): Promise<void> {
    const db = await getDB()
    await db.put('meta', time, 'lastSync')
  }
}
