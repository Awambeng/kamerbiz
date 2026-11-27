# Security baseline

This document defines minimum security practices for every KamerBiz contributor. Controls should be implemented incrementally and verified before production use.

## Secure development

- Follow OWASP ASVS principles for web application controls and OWASP Top 10 risk categories.
- Keep dependencies current; review Dependabot alerts and security updates.
- Run static analysis and dependency vulnerability checks in CI. Add secret scanning and push protection in repository settings.
- Use pinned GitHub Actions versions (full commit SHA) for production workflows; review action updates.
- Give CI and deploy identities only the permissions they need. Set workflow permissions to read only by default.
- Protect the default branch with pull requests, required CI, and approval where supported.

## Identity and access

- Use Keycloak through standard OpenID Connect for local identity development.
- Browser authentication uses Authorization Code flow with PKCE S256. The browser client has no client secret.
- Keep access tokens in memory. Do not persist tokens in local storage or session storage.
- The API validates access token signatures, issuer, expiry, and the KamerBiz API audience.
- Identify external users by the issuer and subject pair. Email is a profile attribute, not an identity key.
- Keep platform roles separate from business membership roles.
- Enforce business membership using the authenticated identity and a server side lookup. Never trust a business identifier supplied by the browser as proof of access.
- The local Keycloak setup uses development mode and an embedded database. It is not suitable for production.
- Before public launch, configure Keycloak with PostgreSQL, TLS, SMTP, verified email, secure password recovery, brute force protection, monitoring, backups, and tested recovery procedures.

## Application controls

- Deny by default. Authenticate users and authorize every operation server side.
- Enforce tenant isolation using trusted server side tenant context; never trust tenant IDs supplied by the browser alone.
- Use secure password hashing and established authentication libraries; never implement cryptography yourself.
- Protect sessions and cookies with Secure, HttpOnly, and appropriate SameSite settings; apply CSRF protection where cookie authentication is used.
- Validate input, encode output, use parameterized database access, and set request, body, and upload limits.
- Apply rate limits to authentication, public checkout, referral tracking, and other abuse prone endpoints.
- Use TLS in all non local environments and secure headers including an appropriate Content Security Policy.
- Keep payment evidence private. Authorize every upload and download. Never store MoMo or Orange Money PINs or customer banking credentials.
- Keep audit events for sensitive actions without recording secrets or full payment credentials.
- Use generic client facing errors; do not expose stack traces or internal identifiers.

## Secrets and data

- No secrets in Git, issue comments, screenshots, logs, test fixtures, or frontend bundles.
- Commit .env.example with placeholders only; keep real .env files ignored.
- Encrypt data in transit and use managed encryption at rest in hosted environments.
- Define retention and deletion rules for personal data and uploaded evidence before launch.
- Use synthetic data in development and tests.

## Security review checklist

For each relevant PR ask: Can this cross a tenant boundary? Is authorization enforced server side? Is untrusted input validated? Could sensitive data leak through logs, errors, or URLs? Are uploads safe? Are rate limits and abuse cases relevant? Are tests covering denial as well as success?

## Incident handling

If a secret is committed, revoke or rotate it immediately; deleting the file or rewriting history is not sufficient. For suspected exposure or compromise, stop affected access, preserve evidence, notify maintainers privately, and track remediation in a restricted issue.
