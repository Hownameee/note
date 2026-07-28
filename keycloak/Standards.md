# 🛡️ Authentication & Authorization Standards

## 1. OAuth & OAuth 2.0

OAuth (Open Authorization) is an open standard for access delegation. Simply put, it allows you to grant a third-party application limited access to your resources on another site without actually giving them your password.

### 🚨 The Problem: The "Password Anti-Pattern"

Before OAuth, the only way to connect different applications was to share credentials. For example, your mobile app would ask for your Instagram username and password, store them, and then use them to make API calls on your behalf.

This created several massive problems:

1. **Security Risk**: If your mobile app gets hacked, or if their backend server is compromised, your actual Instagram password is stolen. You would then have to change your Instagram password and log in again on every app that used it.
2. **Lack of Control**: You could not revoke access for just one application. If you wanted to stop using a specific app, you had to revoke access to *all* applications that used your password.
3. **One-Size-Fits-All**: You had to give the app full access to your account. You could not limit it to only "read my photos" without giving it permission to "delete my photos."
4. **Limited Scope**: It only worked if the service provider had an API and allowed third-party access. If they didn't, you couldn't integrate with them.

> OAuth was created specifically to solve these problems by providing a secure, standardized way to delegate access without sharing credentials.

### 🔑 How OAuth Solves It: The "Valet Key" Approach

OAuth solves this by introducing a valet key approach. When you give a valet your car key, it can only drive the car, cannot open the trunk or the glovebox. OAuth does this for software by separating the ecosystem into distinct roles:

- **Resource Owner**: The user who owns the data (e.g., your image).
- **Client**: The application that wants to access the data (e.g., wants to access image on drive).
- **Authorization Server**: The server that issues access tokens (e.g., Google login system).
- **Resource Server**: The server that hosts the data (e.g., Google drive).

Instead of trading passwords, OAuth uses tokens:

- **Access Token**: A token that is used to access the protected resources. It is a short-lived credential that proves the Client has permission to access specific data (defined by restricted "scopes," like `read-only-photos`).
- **Refresh Token**: A longer-lived credential used strictly to fetch a new Access Token when the old one expires, saving you from having to log in every hour.

### 🔄 The Flow (Authorization Code Flow)

1. **Client Requests Access (Browser Redirect)**: You click "Import from Google Drive" in the Photo App. The app redirects your browser to the Authorization Server (Google), passing its `client_id` and the requested scopes (e.g., read-only access to photos).
2. **User Authenticates and Consents**: You log into Google. Google displays a consent screen: *"Photo App wants to access your photos."* You click "Allow."
3. **Server Returns an Authorization Code**: Google redirects your browser back to the Photo App, passing along a short-lived, single-use Authorization Code in the URL.
4. **Client Exchanges Code for Tokens (Backend to Backend)**: The Photo App's backend server secretly contacts Google's server. It hands over the Authorization Code along with its private `client_secret` to prove its identity.
5. **Tokens are Issued**: Google validates the code and secret. If they match, Google responds to the Photo App's backend with an Access Token (and usually a Refresh Token).
6. **Client Accesses the API**: The Photo App attaches the Access Token to an API request and sends it to Google Drive. Google Drive validates the token, sees it has photo-reading privileges, and returns your images.

### ⚙️ How This Works in Keycloak

Keycloak is a powerful, open-source Identity and Access Management (IAM) system. In the architecture described above, Keycloak acts as the **Authorization Server**.

When you build a system and deploy Keycloak, it handles the heavy lifting of user management so you don't have to build login screens or password hashing yourself.

- **Clients**: You register your various applications (your web frontend, your mobile app, your backend API) as "Clients" inside Keycloak.
- **Realms and Users**: Keycloak manages your user database inside isolated environments called "Realms." It handles checking the password, enforcing Two-Factor Authentication (2FA), and managing active sessions.
- **Token Issuance**: Once a user successfully logs in, Keycloak generates the Access and Refresh JWTs (JSON Web Tokens) and hands them back to your application.

Crucially, Keycloak implements OpenID Connect (OIDC), which is an identity layer built directly on top of OAuth 2.0.

| Protocol | Primary Purpose | Core Credential | Keycloak's Role |
| :--- | :--- | :--- | :--- |
| **OAuth 2.0** | **Authorization**: Proves what the app is allowed to do. | Access Token | Verifies permissions, evaluates scopes, and issues access tokens. |
| **OIDC** | **Authentication**: Proves who the user is. | ID Token | Verifies identity and provides user profile data (like email or name) to the app. |

> By using Keycloak, your applications never see a user's password; they only see the tokens Keycloak provides, maintaining the exact security boundary OAuth 2.0 was designed to create.

---

## 2. OpenID Connect (OIDC)

If OAuth 2.0 provides the "Valet key" for authorization, OpenID Connect (OIDC) provides the "Driver's license" for identity.

OIDC is not a replacement for OAuth 2.0; it is an identity layer built directly on top of it. It patches the biggest hole in raw OAuth 2.0: the fact that the client application has no standardized way to know who just logged in.

### 🪪 How OIDC Works: The ID Token

In standard OAuth 2.0, the Access Token is an opaque string meant only for the Resource Server (the API). The client application isn't supposed to read it or understand it.

OIDC fixes this by introducing a new token specifically for the client application: the **ID Token**.

- **Always a JWT**: The ID Token is always formatted as a JSON Web Token (JWT).
- **Contains Claims**: The client app can decode this JWT to read standard "claims" about the user, such as their unique subject identifier (`sub`), name, email, and the exact time they logged in (`iat`).
- **Cryptographically Signed**: The Authorization Server signs the ID Token, allowing the client application to verify that the token hasn't been tampered with and was legitimately issued by the server.

### 🦊 OIDC in Keycloak

Keycloak is fundamentally designed as an OIDC Identity Provider (IdP). When you use Keycloak, OIDC is the primary language it speaks to your applications.

Here is how Keycloak handles the OIDC specifications:

- **The Discovery Endpoint**: OIDC defines a standard URL (`/.well-known/openid-configuration`). Keycloak hosts this at the Realm level. Your applications can query this single URL to automatically discover where the login page is, where to exchange tokens, and what encryption algorithms Keycloak supports.
- **Client Scopes**: Keycloak has an entire tab dedicated to "Client Scopes." This is where you configure exactly which database attributes (like a user's department, timezone, or custom roles) get mapped into the OIDC ID Token as claims.
- **JWKS (JSON Web Key Set)**: Keycloak publishes its public cryptographic keys via a standard endpoint. This allows your backend services and API Gateways to download the public keys once, cache them, and instantly verify incoming ID and Access Tokens locally at high speed, without having to ping the Keycloak server for every single API request.

---

## 3. PKCE (Proof Key for Code Exchange)

The Authorization Code Flow with PKCE (Proof Key for Code Exchange, pronounced "pixy") is the modern gold standard for securing applications that cannot safely store a backend secret—specifically Single-Page Applications (SPAs like React, Vue) and native Mobile Apps.

### 🔓 The Problem: The Public Client Vulnerability

In the standard Authorization Code flow, your backend server trades an `authorization_code` and a hardcoded `client_secret` to get an Access Token. The secret proves that the request is genuinely coming from your application.

Mobile apps and SPAs are considered **Public Clients**. Because their code is downloaded directly to a user's device or browser, you cannot put a `client_secret` inside them—hackers could easily extract it.

Without a secret, a vulnerability opens up:

1. You log in on your phone. The Auth Server sends the `authorization_code` back to your app via a custom URL scheme (e.g., `myapp://callback`).
2. A malicious app installed on your phone registers the exact same `myapp://callback` scheme.
3. The malicious app intercepts the code, sends it to the Auth Server, and steals your Access Token.

### 🔐 The Solution: A Dynamic, One-Time Secret

PKCE solves this by generating a brand-new, temporary "secret" for every single login attempt. It uses a one-way mathematical hash (usually SHA-256) to create a lock and a key.

- **Code Verifier (The Key)**: A random, cryptographically secure string of characters generated by the client application right as the user clicks "login."
- **Code Challenge (The Lock)**: The application takes the Code Verifier, runs it through a SHA-256 hash, and converts it to a safe string (Base64-URL encoded). Because hashing is a one-way street, no one can guess the Verifier just by looking at the Challenge.

### 🔄 The PKCE Flow (Step-by-Step)

Here is how the interaction happens between your SPA/Mobile App and the Authorization Server (like Keycloak):

1. **The Client Prepares the Lock and Key**: When the user clicks "Login," your application instantly generates the `code_verifier` (e.g., `my-super-secret-random-string`) and hashes it to create the `code_challenge` (e.g., `XyZ123...`).
2. **The Client Sends the Lock (Authorization Request)**: The application redirects the user's browser to the Auth Server's login page. In the URL, it includes standard parameters plus the new lock: *"Hey Keycloak, it's the Mobile App. The user wants to log in. Also, please hold onto this `code_challenge` (`XyZ123...`) for later."*
3. **The User Logs In**: The user enters their credentials. Keycloak verifies their identity and saves the `code_challenge` in its temporary memory, associating it with this specific login session.
4. **The Server Returns the Code**: Keycloak redirects back to the application (via the `myapp://callback` URL) and hands over the short-lived `authorization_code`.
5. **The Client Sends the Key (Token Request)**: The application takes that `authorization_code` and sends it directly to Keycloak's token endpoint. Crucially, it **does not** use a `client_secret`. Instead, it sends the original, unhashed Code Verifier: *"Hey Keycloak, here is the `authorization_code`. To prove I am the one who originally asked for it, here is the raw `code_verifier` (`my-super-secret-random-string`)."*
6. **The Server Validates and Issues Tokens**: Keycloak takes the `code_verifier`, runs it through the exact same SHA-256 hashing algorithm, and checks the result. *"Does the hash of this verifier match the `code_challenge` I saved in Step 3?"*
   - If **YES**, Keycloak knows with absolute certainty that the app requesting the token is the exact same app that initiated the login. It issues the Access and ID tokens.

### 🛡️ Why the Malicious App Fails

If that malicious app intercepts the `authorization_code` at Step 4 and tries to trade it for a token, Keycloak will say: *"Great, you have the code. Now give me the `code_verifier`."* Because the malicious app wasn't the one that generated the random string in Step 1, it doesn't have the verifier. The token request is denied, and the user's data remains perfectly safe.
