import type { JournalEntry } from '../types'
import { localDB } from './localDB'

const DRIVE_API = 'https://www.googleapis.com/drive/v3'
const UPLOAD_API = 'https://www.googleapis.com/upload/drive/v3'
const SPACE = 'appDataFolder'

/**
 * Google Drive sync service for the web app.
 * Uses appDataFolder (hidden, app-private) — no Drive permission prompt.
 * Token is managed by Google Identity Services (GIS).
 */
export class DriveSync {
  private accessToken: string | null = null

  setToken(token: string) {
    this.accessToken = token
  }

  isAuthenticated(): boolean {
    return this.accessToken !== null
  }

  private headers() {
    return {
      Authorization: `Bearer ${this.accessToken}`,
      'Content-Type': 'application/json',
    }
  }

  /** List all entry files from appDataFolder */
  async listFiles(): Promise<Array<{ id: string; name: string; modifiedTime: string }>> {
    const url = `${DRIVE_API}/files?spaces=${SPACE}&fields=files(id,name,modifiedTime)&q=name+contains+'entry_'`
    const res = await fetch(url, { headers: this.headers() })
    if (!res.ok) throw new Error(`Drive list failed: ${res.status}`)
    const data = await res.json()
    return data.files ?? []
  }

  /** Download a file's JSON content */
  async downloadFile(fileId: string): Promise<JournalEntry> {
    const res = await fetch(`${DRIVE_API}/files/${fileId}?alt=media`, {
      headers: this.headers()
    })
    if (!res.ok) throw new Error(`Drive download failed: ${res.status}`)
    return res.json()
  }

  /** Upload (create or update) a journal entry */
  async uploadEntry(entry: JournalEntry): Promise<string> {
    const body = JSON.stringify(entry)

    if (entry.driveFileId) {
      // Update existing
      const res = await fetch(
        `${UPLOAD_API}/files/${entry.driveFileId}?uploadType=media`,
        {
          method: 'PATCH',
          headers: {
            Authorization: `Bearer ${this.accessToken}`,
            'Content-Type': 'application/json',
          },
          body,
        }
      )
      if (!res.ok) throw new Error(`Drive update failed: ${res.status}`)
      return entry.driveFileId
    } else {
      // Create new — multipart upload with metadata
      const metadata = {
        name: `entry_${entry.date}_${entry.id}.json`,
        parents: [SPACE],
        mimeType: 'application/json',
      }

      const boundary = '----DriveUploadBoundary'
      const multipart = [
        `--${boundary}`,
        'Content-Type: application/json; charset=UTF-8',
        '',
        JSON.stringify(metadata),
        `--${boundary}`,
        'Content-Type: application/json',
        '',
        body,
        `--${boundary}--`,
      ].join('\r\n')

      const res = await fetch(
        `${UPLOAD_API}/files?uploadType=multipart&fields=id`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${this.accessToken}`,
            'Content-Type': `multipart/related; boundary=${boundary}`,
          },
          body: multipart,
        }
      )
      if (!res.ok) throw new Error(`Drive create failed: ${res.status}`)
      const data = await res.json()
      return data.id
    }
  }

  async deleteFile(fileId: string): Promise<void> {
    await fetch(`${DRIVE_API}/files/${fileId}`, {
      method: 'DELETE',
      headers: this.headers(),
    })
  }

  // ──────────────────────────────────────────────
  // Full sync orchestration
  // ──────────────────────────────────────────────

  async syncToCloud(): Promise<{ pushed: number; errors: string[] }> {
    const dirty = await localDB.getDirtyEntries()
    const errors: string[] = []
    let pushed = 0

    for (const entry of dirty) {
      try {
        const fileId = await this.uploadEntry(entry)
        await localDB.markSynced(entry.id, fileId)
        pushed++
      } catch (e) {
        errors.push(`Failed to sync ${entry.id}: ${(e as Error).message}`)
      }
    }

    if (errors.length === 0) {
      await localDB.setLastSyncTime(new Date().toISOString())
    }

    return { pushed, errors }
  }

  async syncFromCloud(): Promise<{ pulled: number; errors: string[] }> {
    const files = await this.listFiles()
    const errors: string[] = []
    let pulled = 0

    for (const file of files) {
      try {
        const entry = await this.downloadFile(file.id)
        const local = await localDB.getEntry(entry.id)
        // Last-write-wins: Drive wins if local is not dirty
        if (!local || !local.isDirty) {
          await localDB.saveEntry({ ...entry, isDirty: false, driveFileId: file.id })
          pulled++
        }
      } catch (e) {
        errors.push(`Failed to pull ${file.id}: ${(e as Error).message}`)
      }
    }

    return { pulled, errors }
  }
}

export const driveSync = new DriveSync()
