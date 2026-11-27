# ADR 0003: Use self hosted Keycloak for identity

- **Status:** Accepted
- **Date:** 2026-11-27
- **Decision owners:** KamerBiz project
- **Related issue/PR:** #10

## Context

KamerBiz needs secure sign in for business owners and sales agents. A person may be an agent across several independent businesses and may also have one or more business memberships. Authentication must be reliable without making the first release expensive or operationally complex.

The application already uses React, TypeScript, Spring Boot, PostgreSQL, and Spring Security. The identity solution must use standard OpenID Connect so that identity provider changes do not require changes to business domain rules.

## Decision

Use self hosted Keycloak as the initial OpenID Connect identity provider.

The browser uses the official Keycloak JavaScript adapter with Authorization Code flow and PKCE using S256. Spring Security validates signed access tokens, issuer, expiry, and the KamerBiz API audience.

KamerBiz stores its own user profile keyed by the identity issuer and subject. KamerBiz also owns platform roles, business records, business memberships, and all business authorization decisions in PostgreSQL.

Keycloak owns credentials, authentication sessions, email verification, and password recovery. It does not own KamerBiz business membership or tenant authorization.

## Alternatives considered

- **Logto Cloud:** Good developer experience and a managed service, with a self hosted option. The required API resource feature is not included in the published free plan, so it creates a recurring cost before the product needs managed identity operations.
- **Auth0:** Mature managed identity with strong ecosystem support. The free tier and production features must be reviewed as usage grows, and it introduces recurring provider dependence.
- **Supabase Auth:** Convenient managed authentication and database tooling. KamerBiz already selected PostgreSQL and Spring Boot as its domain backend, so using a second managed data platform for identity would add another operational and conceptual boundary.
- **Custom Spring Security authentication:** Avoids an external identity server but makes KamerBiz responsible for password storage, credential recovery, verification, abuse prevention, and session security. This is unnecessary security work.
- **Self hosted Keycloak:** Selected because it is open source, supports standard OIDC, has an official browser adapter, and can be operated locally without a per user identity subscription.

## Consequences

### Positive

- No per user identity provider subscription for the initial deployment.
- Credentials and authentication flows are delegated to a mature identity product.
- Standard OIDC keeps the application independent of Keycloak specific APIs.
- A global user can participate as an agent and in several business memberships.
- Identity infrastructure can be moved to managed Keycloak or another OIDC provider without changing the domain model.

### Trade offs

- KamerBiz owns production deployment, upgrades, monitoring, backups, recovery, and availability.
- Production requires PostgreSQL for Keycloak, TLS, a stable public hostname, SMTP, secret management, and tested backup restoration.
- The local development container uses development mode and an embedded database. It is not suitable for production.
- Identity availability becomes a dependency for sign in and token renewal.

## Security and privacy considerations

- Browser authentication uses Authorization Code flow with PKCE S256. The browser client is public and has no client secret.
- Access tokens stay in memory. Do not persist tokens in local storage or session storage.
- The API accepts access tokens only after signature, issuer, expiry, and audience validation.
- The API audience is explicit and distinct from the browser client identifier.
- The issuer and subject pair is the stable external identity key. Email is a profile attribute and may change.
- Tenant authorization is enforced by KamerBiz using the authenticated identity and a server side membership lookup.
- Never trust a business identifier supplied by a browser as proof of access.
- Do not put Keycloak admin credentials or production secrets in source control.
- Enable verified email and configure SMTP before public registration. Review password, brute force, MFA, and recovery policies before launch.
- Restrict production redirect URIs and web origins to the deployed application domains.

## API request security

The browser sends short lived access tokens explicitly in the Authorization header. The API does not authenticate requests with ambient cookies or HTTP sessions. Spring Security therefore keeps CSRF protection enabled by default and excludes only the stateless API and actuator paths from CSRF token checks. If a future browser flow uses cookies for authentication, its endpoints must retain CSRF protection and use an appropriate CSRF token mechanism.

## Provider exit strategy

Keep issuer, JWK Set URI, audience, and browser client settings in external configuration. Keep the local user key as issuer plus subject. If the identity provider changes, add an explicit account linking or migration process. Never silently merge accounts based on matching email addresses.

## Follow up

- Configure production Keycloak with PostgreSQL, TLS, SMTP, backups, monitoring, and a recovery procedure before public launch.
- Review identity policies and abuse controls before opening registration.
- Add business onboarding and invitation flows in their own issues.
