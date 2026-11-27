# Security baseline

This document defines minimum security practices for every KamerBiz contributor. Controls should be implemented incrementally and verified before production use.

## Secure development

- Follow OWASP ASVS principles for web application controls and OWASP Top 10 risk categories.
- Keep dependencies current; review Dependabot alerts and security updates.
- Run static analysis and dependency vulnerability checks in CI. Add secret scanning and push protection in repository settings.
- Use pinned GitHub Actions versions (full commit SHA) for production workflows; review action updates.
- Give CI and deploy identities only the permissions they need. Set workflow permissions to read-only by default.
- Protect the default branch with pull requests, required CI, and approval where supported.

## Application controls

- Deny by default. Authenticate users and authorize every operation server-side.
- Enforce tenant isolation using trusted server-side tenant context; never trust tenant IDs supplied by the browser alone.
- Use secure password hashing and established authentication libraries; never implement cryptography yourself.
- Protect sessions/cookies with Secure, HttpOnly, and appropriate SameSite settings; apply CSRF protection where cookie authentication is used.
- Validate input, encode output, use parameterized database access, and set request/body/upload limits.
- Apply rate limits to authentication, public checkout, referral tracking, and other abuse-prone endpoints.
- Use TLS in all non-local environments and secure headers including an appropriate Content-Security-Policy.
- Keep payment evidence private. Authorize every upload and download. Never store MoMo/Orange Money PINs or customer banking credentials.
- Keep audit events for sensitive actions without recording secrets or full payment credentials.
- Use generic client-facing errors; do not expose stack traces or internal identifiers.

## Secrets and data

- No secrets in Git, issue comments, screenshots, logs, test fixtures, or frontend bundles.
- Commit `.env.example` with placeholders only; keep real `.env` files ignored.
- Encrypt data in transit and use managed encryption at rest in hosted environments.
- Define retention and deletion rules for personal data and uploaded evidence before launch.
- Use synthetic data in development and tests.

## Security review checklist

For each relevant PR ask: Can this cross a tenant boundary? Is authorization enforced server-side? Is untrusted input validated? Could sensitive data leak through logs/errors/URLs? Are uploads safe? Are rate limits/abuse cases relevant? Are tests covering denial as well as success?

## Incident handling

If a secret is committed, revoke/rotate it immediately; deleting the file or rewriting history is not sufficient. For suspected exposure or compromise, stop affected access, preserve evidence, notify maintainers privately, and track remediation in a restricted issue.

## Current API authentication limitation

The current API security configuration uses HTTP Basic authentication and local default
credentials to support foundation development. This is a development-only baseline, not a
production-ready identity or authorization design. Do not deploy it to a shared, staging, or
production environment. Before any non-local deployment, replace it with an approved
authentication approach, enforce role and tenant-level authorization, use managed secrets,
and require TLS.
