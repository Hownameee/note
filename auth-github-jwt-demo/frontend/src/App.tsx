const backendUrl = "http://localhost:8080";

export function App() {
  const currentUrl = new URL(window.location.href);
  const error = currentUrl.searchParams.get("error");
  const isLoginSuccess = currentUrl.searchParams.get("login") === "success";

  function continueWithGitHub() {
    window.location.href = `${backendUrl}/oauth2/authorization/github`;
  }

  return (
    <main className="page">
      <section className="panel">
        <div className="brand">OAuth2 + JWT</div>
        <h1>Sign in to the demo app</h1>
        <p className="subtitle">
          Authenticate with GitHub through the backend, then receive an app JWT.
        </p>

        <button className="githubButton" type="button" onClick={continueWithGitHub}>
          <span className="githubIcon" aria-hidden="true">
            GH
          </span>
          Continue with GitHub
        </button>

        {error ? <AuthError message={error} /> : null}
        {isLoginSuccess && !error ? <AuthSuccess /> : null}
      </section>
    </main>
  );
}

interface AuthErrorProps {
  message: string;
}

function AuthError({ message }: AuthErrorProps) {
  return <div className="errorBox">Login failed: {message}</div>;
}

function AuthSuccess() {
  return (
    <div className="tokenBox">
      <div className="successTitle">Login success</div>
      <div className="tokenLabel">JWT cookie</div>
      <code>AUTH_TOKEN cookie was set by the backend.</code>
    </div>
  );
}
