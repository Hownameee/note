import http from 'http';
import https from 'https';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// ── Load .env manually (no dotenv dependency) ────────────────────────────────
const __dir = dirname(fileURLToPath(import.meta.url));
try {
  const env = readFileSync(join(__dir, '.env'), 'utf8');
  for (const line of env.split('\n')) {
    const [key, ...val] = line.trim().split('=');
    if (key && !key.startsWith('#')) process.env[key] = val.join('=').trim();
  }
} catch {
  console.error('❌  .env file not found. Copy .env.example → .env and fill in your credentials.');
  process.exit(1);
}

const CLIENT_ID     = process.env.GITHUB_CLIENT_ID;
const CLIENT_SECRET = process.env.GITHUB_CLIENT_SECRET;
const PORT          = process.env.PORT || 3000;
const CALLBACK_URL  = `http://localhost:${PORT}/callback`;
const SCOPES        = 'user repo workflow';

if (!CLIENT_ID || !CLIENT_SECRET) {
  console.error('❌  GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET must be set in .env');
  process.exit(1);
}

// ── Exchange code → access_token ─────────────────────────────────────────────
function exchangeCode(code) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({ client_id: CLIENT_ID, client_secret: CLIENT_SECRET, code });
    const req = https.request({
      hostname: 'github.com',
      path: '/login/oauth/access_token',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Content-Length': Buffer.byteLength(body),
      },
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); }
        catch { reject(new Error('Failed to parse GitHub response')); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

// ── HTML pages ────────────────────────────────────────────────────────────────
const pageLogin = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>GitHub OAuth — Get Access Token</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      min-height: 100vh; display: flex; align-items: center; justify-content: center;
      background: #0d1117; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      color: #e6edf3;
    }
    .card {
      background: #161b22; border: 1px solid #30363d; border-radius: 12px;
      padding: 48px 40px; max-width: 440px; width: 90%; text-align: center;
    }
    .logo { font-size: 48px; margin-bottom: 16px; }
    h1 { font-size: 22px; font-weight: 600; margin-bottom: 8px; }
    p { color: #8b949e; font-size: 14px; margin-bottom: 32px; line-height: 1.6; }
    .scopes {
      background: #0d1117; border: 1px solid #30363d; border-radius: 8px;
      padding: 12px 16px; margin-bottom: 28px; text-align: left;
      font-size: 13px; color: #8b949e;
    }
    .scopes strong { color: #58a6ff; display: block; margin-bottom: 6px; }
    .scope-tag {
      display: inline-block; background: #1f6feb33; color: #58a6ff;
      border: 1px solid #1f6feb66; border-radius: 4px;
      padding: 2px 8px; font-size: 12px; font-family: monospace; margin: 2px;
    }
    a.btn {
      display: inline-flex; align-items: center; gap: 10px;
      background: #238636; color: #fff; text-decoration: none;
      padding: 12px 24px; border-radius: 8px; font-size: 15px; font-weight: 500;
      transition: background 0.2s;
    }
    a.btn:hover { background: #2ea043; }
    .footer { margin-top: 24px; font-size: 12px; color: #484f58; }
  </style>
</head>
<body>
  <div class="card">
    <div class="logo">⚡</div>
    <h1>GitHub Access Token</h1>
    <p>Authorize this app to get an OAuth2 access token you can use with <code>curl</code>.</p>
    <div class="scopes">
      <strong>Requested scopes:</strong>
      <span class="scope-tag">user</span>
      <span class="scope-tag">repo</span>
      <span class="scope-tag">workflow</span>
    </div>
    <a class="btn" href="https://github.com/login/oauth/authorize?client_id=${CLIENT_ID}&scope=${encodeURIComponent(SCOPES)}&redirect_uri=${encodeURIComponent(CALLBACK_URL)}">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
      </svg>
      Login with GitHub
    </a>
    <div class="footer">Token will be displayed after authorization.</div>
  </div>
</body>
</html>`;

function pageToken(token, scopes) {
  const exportCmd = `export ACCESS_TOKEN="${token}"`;
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>GitHub OAuth — Token Ready</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      min-height: 100vh; display: flex; align-items: center; justify-content: center;
      background: #0d1117; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      color: #e6edf3;
    }
    .card {
      background: #161b22; border: 1px solid #30363d; border-radius: 12px;
      padding: 48px 40px; max-width: 580px; width: 90%;
    }
    .status { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; }
    .dot { width: 10px; height: 10px; border-radius: 50%; background: #3fb950; flex-shrink: 0; }
    h1 { font-size: 20px; font-weight: 600; }
    .label { font-size: 12px; color: #8b949e; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 6px; margin-top: 20px; }
    .token-box {
      background: #0d1117; border: 1px solid #30363d; border-radius: 8px;
      padding: 14px 16px; font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 13px; color: #79c0ff; word-break: break-all; line-height: 1.5;
      position: relative;
    }
    .cmd-box {
      background: #0d1117; border: 1px solid #30363d; border-radius: 8px;
      padding: 14px 16px; font-family: 'SF Mono', 'Fira Code', monospace;
      font-size: 13px; color: #3fb950; word-break: break-all; line-height: 1.5;
    }
    .btn-row { display: flex; gap: 10px; margin-top: 8px; flex-wrap: wrap; }
    button {
      background: #21262d; border: 1px solid #30363d; color: #e6edf3;
      padding: 7px 14px; border-radius: 6px; font-size: 13px; cursor: pointer;
      transition: background 0.15s;
    }
    button:hover { background: #30363d; }
    button.copied { background: #1f6feb; border-color: #388bfd; }
    .scopes-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
    .scope-tag {
      background: #1f6feb22; color: #58a6ff; border: 1px solid #1f6feb55;
      border-radius: 4px; padding: 2px 8px; font-size: 12px; font-family: monospace;
    }
    .warning {
      background: #3d2400; border: 1px solid #6e4710; border-radius: 8px;
      padding: 12px 14px; font-size: 13px; color: #d29922; margin-top: 24px;
      line-height: 1.6;
    }
    a.back { display: inline-block; margin-top: 20px; color: #58a6ff; font-size: 13px; text-decoration: none; }
    a.back:hover { text-decoration: underline; }
  </style>
</head>
<body>
  <div class="card">
    <div class="status"><div class="dot"></div><h1>Access Token Ready</h1></div>

    <div class="label">Access Token</div>
    <div class="token-box" id="token">${token}</div>
    <div class="btn-row">
      <button onclick="copy('token', this)">Copy Token</button>
    </div>

    <div class="label">Export command (paste in terminal)</div>
    <div class="cmd-box" id="cmd">${exportCmd}</div>
    <div class="btn-row">
      <button onclick="copy('cmd', this)">Copy export command</button>
    </div>

    <div class="label">Scopes granted</div>
    <div class="scopes-row">${(scopes || SCOPES).split(/[ ,]+/).filter(Boolean).map(s => `<span class="scope-tag">${s}</span>`).join('')}</div>

    <div class="warning">
      ⚠️ Keep this token private. Anyone with this token can access your GitHub account with the scopes shown above.
    </div>

    <a class="back" href="/">← Get a new token</a>
  </div>
  <script>
    function copy(id, btn) {
      navigator.clipboard.writeText(document.getElementById(id).textContent.trim());
      btn.textContent = 'Copied!';
      btn.classList.add('copied');
      setTimeout(() => { btn.textContent = id === 'token' ? 'Copy Token' : 'Copy export command'; btn.classList.remove('copied'); }, 2000);
    }
  </script>
</body>
</html>`;
}

function pageError(msg) {
  return `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Error</title>
  <style>body{min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0d1117;font-family:sans-serif;color:#e6edf3;}
  .card{background:#161b22;border:1px solid #da3633;border-radius:12px;padding:40px;max-width:440px;width:90%;text-align:center;}
  h1{color:#f85149;margin-bottom:12px;}p{color:#8b949e;font-size:14px;}a{color:#58a6ff;}</style></head>
  <body><div class="card"><h1>❌ Error</h1><p>${msg}</p><br><a href="/">← Try again</a></div></body></html>`;
}

// ── HTTP Server ───────────────────────────────────────────────────────────────
const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);

  // Home page
  if (url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    return res.end(pageLogin);
  }

  // OAuth callback
  if (url.pathname === '/callback') {
    const code  = url.searchParams.get('code');
    const error = url.searchParams.get('error');

    if (error || !code) {
      res.writeHead(400, { 'Content-Type': 'text/html' });
      return res.end(pageError(error || 'No code received from GitHub.'));
    }

    try {
      const data = await exchangeCode(code);
      if (data.error) {
        res.writeHead(400, { 'Content-Type': 'text/html' });
        return res.end(pageError(`GitHub error: ${data.error_description || data.error}`));
      }
      res.writeHead(200, { 'Content-Type': 'text/html' });
      return res.end(pageToken(data.access_token, data.scope));
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'text/html' });
      return res.end(pageError(`Server error: ${err.message}`));
    }
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not found');
});

server.listen(PORT, () => {
  console.log(`\n✅  OAuth Token App running at http://localhost:${PORT}`);
  console.log(`    Open the URL above in your browser to get a token.\n`);
});
