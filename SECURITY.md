# Security Policy

## Reporting a vulnerability

Do not report security vulnerabilities in public GitHub Issues. Contact the repository maintainers privately through GitHub's **Report a vulnerability** feature (Security tab), where available. Include affected versions/commit, impact, reproduction steps, and any suggested mitigation. Do not include real customer data or secrets.

## Security expectations

- Never commit credentials, tokens, private keys, production data, or payment evidence.
- Use environment variables or a managed secret store; commit only safe examples such as `.env.example`.
- Validate and normalize untrusted input on the server.
- Enforce authentication, authorization, and tenant ownership on every protected operation.
- Use least privilege for application identities, CI tokens, cloud roles, and database accounts.
- Keep dependencies patched; review automated dependency and code-scanning alerts.
- Avoid sensitive data in logs, errors, URLs, analytics, and test fixtures.
- Treat uploaded files as untrusted: validate size/type, generate storage names, store privately, and authorize downloads.
- Never treat a payment screenshot as proof of payment; vendor verification remains authoritative.
- Security-sensitive changes require explicit tests and reviewer attention.

This policy is an initial project baseline and will evolve as the system is implemented.
