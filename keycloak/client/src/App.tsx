import { useEffect, useRef, useState } from 'react';
import './App.css'

const KEYCLOAK_URL = 'http://localhost:10000';
const REALM = 'unihub';
const CLIENT_ID = 'frontend-app';
const REDIRECT_URI = `${window.location.origin}/callback`;

// ─── PKCE helpers ───────────────────────────────────────────
function base64UrlEncode(array: Uint8Array): string {
  return btoa(String.fromCharCode.apply(null, Array.from(array)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function generateCodeVerifier(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64UrlEncode(array);
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await window.crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
}

function tryDecodeJWT(value: string): object | null {
  const parts = value.split('.');
  if (parts.length !== 3) return null;
  try {
    return JSON.parse(atob(parts[1]));
  } catch {
    return null;
  }
}

// ─── Types ──────────────────────────────────────────────────
type LogFields = Record<string, string>;

type LogEntry = {
  id: number;
  method: 'GET' | 'POST' | 'REDIRECT';
  url: string;
  status: 'ok' | 'error' | 'info';
  statusText: string;
  timestamp: string;
  request?: LogFields;
  response?: LogFields;
  collapsed: boolean;
};

// ─── Step definitions ────────────────────────────────────────
const STEPS = [
  {
    num: 1,
    title: 'Generate PKCE Verifier & Challenge',
    desc: 'A random code_verifier is generated. SHA-256 hashes it into the code_challenge sent to Keycloak.',
  },
  {
    num: 2,
    title: 'Redirect to Keycloak Login',
    desc: 'User is redirected to the Keycloak /auth endpoint with the code_challenge.',
  },
  {
    num: 3,
    title: 'Handle Callback',
    desc: 'Keycloak redirects back with an authorization code in the URL query string.',
  },
  {
    num: 4,
    title: 'Exchange Code for Tokens',
    desc: 'POST to /token with the authorization code + original code_verifier to receive access & ID tokens.',
  },
  {
    num: 5,
    title: 'Authenticated',
    desc: 'Access token stored. User is logged in.',
  },
];

// ─── Field renderer ─────────────────────────────────────────
function FieldRow({ name, value }: { name: string; value: string }) {
  const [expanded, setExpanded] = useState(false);
  const decoded = tryDecodeJWT(value);
  const isJWT = decoded !== null;

  // Detect if the value is long (e.g. base64/token) to truncate
  const isLong = !isJWT && value.length > 80;

  return (
    <div className="field-row">
      <div className="field-key">{name}</div>
      <div className="field-value-wrap">
        {isJWT ? (
          <div className="field-jwt">
            <div
              className="field-jwt-pill"
              onClick={() => setExpanded(e => !e)}
              title="Click to expand JWT"
            >
              <span className="jwt-badge">JWT</span>
              <span className="field-value truncated">{value.substring(0, 32)}…</span>
              <span className="expand-arrow">{expanded ? '▲' : '▼'}</span>
            </div>
            {expanded && (
              <div className="field-jwt-decoded">
                {Object.entries(decoded as Record<string, unknown>).map(([k, v]) => (
                  <div key={k} className="jwt-claim-row">
                    <span className="jwt-claim-key">{k}</span>
                    <span className="jwt-claim-value">{typeof v === 'object' ? JSON.stringify(v) : String(v)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : isLong ? (
          <div
            className={`field-value ${expanded ? '' : 'truncated'}`}
            onClick={() => setExpanded(e => !e)}
            style={{ cursor: 'pointer' }}
            title="Click to expand"
          >
            {expanded ? value : `${value.substring(0, 80)}…`}
          </div>
        ) : (
          <div className="field-value">{value}</div>
        )}
      </div>
    </div>
  );
}

function FieldTable({ label, fields }: { label: string; fields: LogFields }) {
  return (
    <div className="field-section">
      <div className="log-section-label">{label}</div>
      <div className="field-table">
        {Object.entries(fields).map(([k, v]) => (
          <FieldRow key={k} name={k} value={v} />
        ))}
      </div>
    </div>
  );
}

// ─── App ─────────────────────────────────────────────────────
let _logId = 0;
const nowStr = () => new Date().toLocaleTimeString();

function App() {
  const [token, setToken] = useState<string>('');
  const [idToken, setIdToken] = useState<string>('');
  const [step, setStep] = useState<number>(0);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const logBottomRef = useRef<HTMLDivElement>(null);

  const addLog = (entry: Omit<LogEntry, 'id' | 'timestamp' | 'collapsed'>) => {
    const id = ++_logId;
    setLogs(prev => [...prev, { ...entry, id, timestamp: nowStr(), collapsed: false }]);
    setTimeout(() => logBottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 60);
    // Auto-collapse after 4s
    setTimeout(() => {
      setLogs(prev => prev.map(e => e.id === id ? { ...e, collapsed: true } : e));
    }, 4000);
    return id;
  };

  const updateLog = (id: number, patch: Partial<Omit<LogEntry, 'id'>>) => {
    setLogs(prev => prev.map(e => e.id === id ? { ...e, ...patch } : e));
  };

  const toggleCollapse = (id: number) => {
    setLogs(prev => prev.map(e => e.id === id ? { ...e, collapsed: !e.collapsed } : e));
  };

  const codeExchange = async (code: string, verifier: string) => {
    setStep(4);
    const url = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
    const bodyParams: LogFields = {
      grant_type: 'authorization_code',
      client_id: CLIENT_ID,
      redirect_uri: `${window.location.origin}/callback`,
      code,
      code_verifier: verifier,
    };

    // Log the request as a single entry, then update it in-place when the response arrives
    const logId = addLog({ method: 'POST', url, status: 'info', statusText: 'Sending…', request: bodyParams });

    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: Object.entries(bodyParams).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&'),
    });

    const data = await res.json();

    // Build response fields — include all keys from the JSON
    const responseFields: LogFields = {};
    for (const [k, v] of Object.entries(data)) {
      responseFields[k] = String(v);
    }

    // Update the existing entry instead of adding a new one
    updateLog(logId, {
      status: res.ok ? 'ok' : 'error',
      statusText: res.ok ? `${res.status} OK` : `${res.status} Error`,
      response: responseFields,
    });

    if (data.access_token) {
      setToken(data.access_token);
      window.history.replaceState({}, document.title, '/');
      setStep(5);
    }
    if (data.id_token) {
      setIdToken(data.id_token);
    }
  };

  const login = async () => {
    setStep(1);
    const verifier = generateCodeVerifier();
    const challenge = await generateCodeChallenge(verifier);
    sessionStorage.setItem('pkce_verifier', verifier);

    const authUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth`;
    const params = new URLSearchParams({
      client_id: CLIENT_ID,
      response_type: 'code',
      redirect_uri: REDIRECT_URI,
      scope: 'openid',
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });

    setStep(2);
    addLog({
      method: 'REDIRECT',
      url: `${authUrl}?${params.toString()}`,
      status: 'info',
      statusText: 'Redirect →',
      request: {
        client_id: CLIENT_ID,
        response_type: 'code',
        redirect_uri: REDIRECT_URI,
        scope: 'openid',
        code_challenge: challenge,         // full value — will auto-truncate in FieldRow
        code_challenge_method: 'S256',
      },
    });

    await new Promise(r => setTimeout(r, 4000)); // wait 4s so user can read the log
    window.location.href = `${authUrl}?${params.toString()}`;
  };

  const logout = () => {
    const logoutUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`;
    addLog({
      method: 'REDIRECT',
      url: `${logoutUrl}?id_token_hint=${idToken}&post_logout_redirect_uri=${window.location.origin}/`,
      status: 'info',
      statusText: 'Redirect →',
      request: {
        id_token_hint: idToken,            // full JWT — FieldRow will detect & decode it
        post_logout_redirect_uri: `${window.location.origin}/`,
      },
    });
    setToken('');
    setIdToken('');
    setStep(0);
    window.location.href = `${logoutUrl}?id_token_hint=${idToken}&post_logout_redirect_uri=${window.location.origin}/`;
  };

  useEffect(() => {
    const url = new URL(window.location.href);
    if (url.pathname === '/callback') {
      setStep(3);
      const code = url.searchParams.get('code');
      if (!code) { console.error('no code'); return; }
      const verifier = sessionStorage.getItem('pkce_verifier');
      if (!verifier) { console.error('no verifier'); return; }

      addLog({
        method: 'GET',
        url: url.toString(),
        status: 'ok',
        statusText: 'Callback received',
        response: {
          code,
          state: url.searchParams.get('state') ?? '(none)',
          session_state: url.searchParams.get('session_state') ?? '(none)',
        },
      });

      codeExchange(code, verifier);
    }
  }, []);

  const decodedToken = token ? tryDecodeJWT(token) : null;

  return (
    <div className="app-layout">
      {/* ── LEFT: Flow Panel ── */}
      <aside className="flow-panel">
        <div className="flow-header">
          <h1>OIDC / PKCE Flow</h1>
          <p>Interactive demo of the Authorization Code flow with PKCE using Keycloak</p>
        </div>

        <div className="flow-steps">
          {STEPS.map((s, i) => (
            <div key={s.num}>
              <div className={`step-item${step === s.num ? ' active' : ''}${step > s.num ? ' done' : ''}`}>
                <div className="step-num">{step > s.num ? '✓' : s.num}</div>
                <div className="step-content">
                  <h3>{s.title}</h3>
                  <p>{s.desc}</p>
                </div>
              </div>
              {i < STEPS.length - 1 && <div className="step-connector" />}
            </div>
          ))}
        </div>

        <div className="flow-actions">
          {token ? (
            <>
              <div className="token-badge">
                <span className="dot" />
                Authenticated
              </div>
              {decodedToken && (
                <div style={{ marginBottom: '12px' }}>
                  <b style={{ fontSize: '12px', color: 'var(--text-h)', display: 'block', marginBottom: 6 }}>Access Token Claims</b>
                  <div className="field-table" style={{ fontSize: '11px' }}>
                    {Object.entries(decodedToken as Record<string, unknown>).map(([k, v]) => (
                      <div key={k} className="field-row">
                        <div className="field-key">{k}</div>
                        <div className="field-value">{typeof v === 'object' ? JSON.stringify(v) : String(v)}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              <button className="btn-logout" onClick={logout}>Logout</button>
            </>
          ) : (
            <button className="btn-login" onClick={login} id="btn-login">
              Login with Keycloak
            </button>
          )}
        </div>
      </aside>

      {/* ── RIGHT: HTTP Log Panel ── */}
      <main className="log-panel">
        <div className="log-header">
          <h2>HTTP Log</h2>
          <button className="log-clear" onClick={() => setLogs([])}>Clear</button>
        </div>

        <div className="log-body">
          {logs.length === 0 && (
            <div className="log-empty">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M9 12h.01M15 12h.01M12 12h.01M3 12c0-4.97 4.03-9 9-9s9 4.03 9 9-4.03 9-9 9-9-4.03-9-9z" />
              </svg>
              <p>No HTTP traffic yet.<br />Click <b>Login</b> to start the flow.</p>
            </div>
          )}

          {logs.map(entry => (
            <div className="log-entry" key={entry.id}>
              <div className="log-entry-header" onClick={() => toggleCollapse(entry.id)}>
                <span className={`log-method ${entry.method}`}>{entry.method}</span>
                <span className="log-url" title={entry.url}>{entry.url}</span>
                <span className={`log-status ${entry.status}`}>{entry.statusText}</span>
                <span className="log-time">{entry.timestamp}</span>
                <span className="log-chevron">{entry.collapsed ? '›' : '⌄'}</span>
              </div>

              {!entry.collapsed && (
                <div className="log-entry-body">
                  {entry.request && <FieldTable label="Request Body / Params" fields={entry.request} />}
                  {entry.response && <FieldTable label="Response" fields={entry.response} />}
                  <div className="log-timer">
                    <div className="log-timer-bar" key={`timer-${entry.id}`} />
                  </div>
                </div>
              )}
            </div>
          ))}

          <div ref={logBottomRef} />
        </div>
      </main>
    </div>
  );
}

export default App
