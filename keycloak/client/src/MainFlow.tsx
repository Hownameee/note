import { useEffect, useState } from 'react';
import './App.css'

const KEYCLOAK_URL = 'http://localhost:10000';
const REALM = 'unihub';
const CLIENT_ID = 'frontend-app';
const REDIRECT_URI = `${window.location.origin}/callback`;

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

function decodeTokenJWT(token: string) {
    const base64 = token.split('.')[1];
    console.log(JSON.parse(atob(base64)))
    return JSON.parse(atob(base64));
}

function App() {
    const [token, setToken] = useState<string>('');
    const [idToken, setIdToken] = useState<string>(''); // Store ID token for logout hint

    const codeExchange = async (code: string, verifier: string) => {
        const res = await fetch(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `grant_type=authorization_code` +
                `&client_id=${CLIENT_ID}` +
                `&redirect_uri=${window.location.origin}/callback` +
                `&code=${code}` +
                `&code_verifier=${verifier}`,
        })
        const data = await res.json();

        if (data.access_token) {
            setToken(data.access_token);
            window.history.replaceState({}, document.title, '/');
        }
        // use for logout hint
        if (data.id_token) {
            setIdToken(data.id_token);
        }
    }

    const login = async () => {
        // gen code
        const verifier = generateCodeVerifier();
        // hash and send this
        const challenge = await generateCodeChallenge(verifier);
        // store this for later to verify
        sessionStorage.setItem('pkce_verifier', verifier);

        window.location.href = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth` +
            `?client_id=${CLIENT_ID}` +
            `&response_type=code` +
            `&redirect_uri=${REDIRECT_URI}` +
            `&scope=openid` +
            `&code_challenge=${challenge}` +
            `&code_challenge_method=S256`;
    }

    const logout = () => {
        setToken('');
        setIdToken('');
        window.location.href = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout` +
            `?id_token_hint=${idToken}` +
            `&post_logout_redirect_uri=${window.location.origin}/`;
    }

    useEffect(() => {
        // check if we are on callback?
        const url = new URL(window.location.href);
        if (url.pathname === '/callback') {
            const code = url.searchParams.get('code');
            if (!code) {
                console.error('no code');
                return;
            }
            const verifier = sessionStorage.getItem('pkce_verifier');
            if (!verifier) {
                console.error('no verifier');
                return;
            }
            codeExchange(code, verifier);
        }
    }, []);

    return (
        <>
            {token !== '' ? (<button onClick={() => logout()}>Logout</button>) : (<button onClick={() => login()}>Login</button>)}

            {token !== '' && (() => {
                const decode = decodeTokenJWT(token);
                return (
                    <div>
                        <p>Token: {token}</p>
                        <p>Decode: {JSON.stringify(decode)}</p>
                    </div>
                )
            })()}
        </>
    )
}

export default App
