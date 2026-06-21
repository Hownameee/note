# GitHub OAuth2 + JWT Auth Demo

Small demo flow:

1. React frontend shows a `Continue with GitHub` button.
2. Button redirects to the backend at `/auth/github`.
3. Backend redirects the browser to GitHub through Spring Security OAuth2.
4. After login, GitHub sends the authorization `code` to the backend callback.
5. Backend receives GitHub's access token from Spring Security OAuth2.
6. Backend creates or updates the H2 user row with GitHub username and GitHub token.
7. Backend signs an app JWT using the saved `userId` as the JWT subject.
8. Backend stores the JWT in an HttpOnly `AUTH_TOKEN` cookie and redirects to frontend home.
9. Later user API requests must include the `AUTH_TOKEN` cookie to pass backend validation.

## GitHub OAuth App

Create a GitHub OAuth App with:

- Homepage URL: `http://localhost:9090`
- Authorization callback URL: `http://localhost:8080/auth/github/callback`

## Backend

```bash
cd auth-github-jwt-demo/backend
cp .env.example .env
./gradlew bootRun
```

Fill `backend/.env` with your GitHub OAuth app credentials before starting the backend. `./gradlew bootRun` loads this file automatically.

Backend runs on `http://localhost:8080`.

Spring Security routes:

- `GET /auth/github` starts the app login flow and redirects to Spring Security's `/oauth2/authorization/github`.
- GitHub redirects to `GET /auth/github/callback`.
- On success, the backend stores or updates the H2 `app_users` row with `username`, `user_id`, and `github_token`.
- The backend signs a demo JWT with `user_id` as the subject, stores it in an HttpOnly `AUTH_TOKEN` cookie, and redirects to `http://localhost:9090/?login=success`.
- `/api/users/**` requires a valid `AUTH_TOKEN` cookie.

H2 console:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:auth_demo`
- User: `sa`
- Password: empty

User API:

```bash
curl --cookie "AUTH_TOKEN=<jwt-cookie-value>" http://localhost:8080/api/users
curl --cookie "AUTH_TOKEN=<jwt-cookie-value>" http://localhost:8080/api/users/1
```

For browser requests from the frontend, include credentials so the cookie is sent:

```ts
fetch("http://localhost:8080/api/users", {
  credentials: "include",
});
```

## Frontend

```bash
cd auth-github-jwt-demo/frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:9090` for both `npm run dev` and `npm run preview`.

## Useful Endpoints

- `GET /auth/github` starts GitHub login through the backend.
- `GET /auth/github/callback` handles GitHub's OAuth redirect.
- `GET /api/users` validates the `AUTH_TOKEN` cookie and returns users.

## Notes

- This is a local demo, not a production session design.
- The demo stores the JWT in an HttpOnly cookie instead of putting it in the URL.
- Do not commit `.env` files.
