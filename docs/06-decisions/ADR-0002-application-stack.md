# ADR-0002: Use React, TypeScript, Spring Boot and PostgreSQL

- **Status:** Accepted
- **Date:** 2026-11-27
- **Decision owners:** KamerBiz project
- **Related issue/PR:** #1

## Context
The first release needs a maintainable web application and API that can be developed and operated by a small team. The stack should support a mobile-first UI, strong typing, relational business data, migrations, and mature security libraries.

## Decision
Use React with TypeScript and Vite for the web application; Java 21 and Spring Boot for the API; PostgreSQL as the relational database; and Flyway for schema migrations. Use Maven for backend builds and npm for frontend dependencies.

## Alternatives considered
- Full-stack JavaScript — fewer languages, but the team has strong Java/Spring experience and the backend benefits from the Spring ecosystem.
- Server-rendered monolith — viable, but a separate web application and API provide a clear interface for future clients.
- NoSQL-first persistence — not selected because the initial domain has relational ownership, orders, products, payments, and commission records.

## Consequences
### Positive
- Mature ecosystems and broad contributor familiarity.
- Strong typing and clear separation between UI and API.
- PostgreSQL transactions and constraints support important business invariants.

### Negative / trade-offs
- Two build ecosystems must be maintained.
- Frontend and backend contracts need explicit API documentation and compatibility discipline.

## Security and privacy considerations
Use Spring Security as the backend enforcement point. Keep credentials out of source control and browser bundles. Validate requests, apply tenant-level authorization, use least-privilege database credentials, and manage schema changes through reviewed migrations.

## Follow-up
Add OpenAPI generation, testcontainers-based integration tests, dependency lockfiles, and stricter CI quality gates in dedicated issues.
