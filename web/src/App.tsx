import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import { useJournalStore } from './store/journalStore'
import HomePage from './pages/HomePage'
import EntryPage from './pages/EntryPage'
import ReviewPage from './pages/ReviewPage'
import './app.css'

declare global {
  interface Window {
    google: {
      accounts: {
        oauth2: {
          initTokenClient: (config: {
            client_id: string
            scope: string
            callback: (response: { access_token: string }) => void
          }) => { requestAccessToken: () => void }
        }
      }
    }
  }
}

function GoogleAuthButton() {
  const { isAuthenticated, setAuthenticated, signOut } = useJournalStore()

  const requestToken = () => {
    const client = window.google.accounts.oauth2.initTokenClient({
      client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
      scope: 'https://www.googleapis.com/auth/drive.appdata',
      callback: (response) => {
        if (response.access_token) {
          setAuthenticated(response.access_token)
        }
      },
    })
    client.requestAccessToken()
  }

  if (isAuthenticated) {
    return (
      <button className="auth-btn signed-in" onClick={signOut}>
        <span className="dot green" /> Synced with Drive
      </button>
    )
  }

  return (
    <button className="auth-btn" onClick={requestToken}>
      Connect Google Drive
    </button>
  )
}

function Layout({ children }: { children: React.ReactNode }) {
  const { syncToCloud, syncStatus } = useJournalStore()

  return (
    <div className="layout">
      <header className="header">
        <div className="header-left">
          <span className="logo">✦</span>
          <span className="app-title">Strategic Journal</span>
        </div>
        <nav className="nav">
          <NavLink to="/" end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            Entries
          </NavLink>
          <NavLink to="/review" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            Review
          </NavLink>
        </nav>
        <div className="header-right">
          <button
            className="sync-btn"
            onClick={syncToCloud}
            disabled={syncStatus.state === 'syncing'}
            title={syncStatus.lastSync ? `Last sync: ${new Date(syncStatus.lastSync).toLocaleTimeString()}` : 'Not synced yet'}
          >
            {syncStatus.state === 'syncing' ? '↻' : '⇅'}
          </button>
          <GoogleAuthButton />
        </div>
      </header>
      <main className="main">{children}</main>
    </div>
  )
}

export default function App() {
  const loadEntries = useJournalStore((s) => s.loadEntries)

  // loadEntries is stable (zustand actions don't change between renders)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { loadEntries() }, [])

  const base = import.meta.env.BASE_URL

  return (
    <BrowserRouter basename={base}>
      <Layout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/entry/:id?" element={<EntryPage />} />
          <Route path="/review" element={<ReviewPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
