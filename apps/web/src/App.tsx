import { useAuth } from "./auth/KeycloakProvider";

export default function App() {
  const { configured, authenticated, loading, displayName, error, signIn, signOut } = useAuth();

  return (
    <main className="page-shell">
      <nav className="topbar" aria-label="Main navigation">
        <a className="brand" href="/" aria-label="KamerBiz home">
          <span className="brand-mark">K</span>
          <span>KamerBiz</span>
        </a>
        <span className="language">
          EN <span aria-hidden="true">·</span> FR
        </span>
      </nav>
      <section className="hero">
        <div className="eyebrow">
          <span className="pulse" /> BUILDING FOR CAMEROONIAN BUSINESSES
        </div>
        <h1>
          Stronger businesses.
          <br />
          <span>A stronger Cameroon.</span>
        </h1>
        <p className="intro">
          Simple tools to help local businesses reach more customers, manage sales and grow with
          confidence.
        </p>
        <div className="actions">
          {loading ? (
            <span className="auth-status" role="status">
              Connecting securely...
            </span>
          ) : authenticated ? (
            <>
              <span className="auth-status">Signed in as {displayName ?? "KamerBiz user"}</span>
              <button className="primary" onClick={signOut}>
                Sign out
              </button>
            </>
          ) : configured ? (
            <button className="primary" onClick={signIn}>
              Sign in to KamerBiz
            </button>
          ) : (
            <span className="auth-status">Identity service is not configured yet.</span>
          )}
          <a className="secondary" href="https://github.com/Awambeng/kamerbiz">
            Explore the project
          </a>
        </div>
        {error && (
          <p className="auth-error" role="alert">
            {error}
          </p>
        )}
        <div className="trust-row">
          <span>MADE WITH LOCAL BUSINESSES IN MIND</span>
          <span className="flag" aria-label="Cameroon flag">
            <i />
            <i />
            <i />
          </span>
        </div>
      </section>
      <footer>
        <span>© {new Date().getFullYear()} KamerBiz</span>
        <span>Hope in action.</span>
      </footer>
    </main>
  );
}
