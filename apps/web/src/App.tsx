export default function App() {
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
          <a className="primary" href="mailto:hello@kamerbiz.com">
            Get in touch <span aria-hidden="true">↗</span>
          </a>
          <a className="secondary" href="https://github.com/Awambeng/kamerbiz">
            Explore the project
          </a>
        </div>
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
