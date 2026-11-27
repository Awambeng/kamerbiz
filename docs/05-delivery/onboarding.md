# New contributor onboarding

This guide is designed so a new contributor can get oriented without needing a private walkthrough.

## 1. Understand the product

Read the README, product overview, MVP scope, key workflows, and sales-agent network documentation. Treat documented MVP decisions as the current baseline; use the open-questions document for unresolved details.

## 2. Understand the architecture

Read the technical overview, project structure, security baseline, and accepted ADRs. The backend is a modular monolith; do not introduce infrastructure or distributed systems without an issue and an ADR where appropriate.

## 3. Prepare your machine

Install Git, Node.js 22+, npm, Java 21, Maven 3.9+, and Docker Compose. Clone the repository and check out the branch associated with your assigned issue.

## 4. Run the system

Copy the local environment template with `cp .env.example .env`. Start PostgreSQL and Keycloak with `docker compose up -d postgres keycloak`. The Keycloak container imports the KamerBiz development realm automatically. Spring Boot does not load the root `.env` file by itself. Start the API from `services/api`, exporting the root environment values into the shell first: `set -a; . ../../.env; set +a; mvn spring-boot:run`. The API also has local defaults in `application.yml`, but values in `.env` only override them when exported into the API process environment. In another terminal, install and start the frontend from `apps/web` with `npm ci && npm run dev`. Vite is configured to read the root `.env` file. Open the Vite URL printed in the terminal and confirm the API responds at `http://localhost:8080/api/v1/health`.

The realm also imports a local test account: username `test-user`, password `TestUser123!`. Use this account for local sign in only; it is not a production credential. You can also register a separate local account through the Keycloak login page.

The local realm supports self registration and password recovery. Email verification is disabled for local development. The local Keycloak administrator credentials are development only and must never be reused outside a local machine. Configure SMTP and enable email verification before public launch. Production requires a hardened Keycloak deployment with PostgreSQL persistence, TLS, managed secrets, backups, monitoring, and recovery testing.

## 5. Configure identity and web URLs

The root `.env` file supplies local configuration. The values below are development defaults from `.env.example`. Keep the API and browser settings aligned when changing the Keycloak host, realm, web port, or client.

| Variable                  | Used by | Purpose                                                                                                                         |
| ------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `KEYCLOAK_ISSUER_URI`     | API     | Expected issuer claim for access tokens. It identifies the KamerBiz realm, for example `http://localhost:8081/realms/kamerbiz`. |
| `KEYCLOAK_JWK_SET_URI`    | API     | Keycloak endpoint from which the API obtains public keys to verify access token signatures.                                     |
| `KEYCLOAK_API_AUDIENCE`   | API     | Required audience in access tokens. The local realm adds `kamerbiz-api` to tokens for the API.                                  |
| `WEB_ORIGIN`              | API     | Allowed browser origin for cross origin requests. Set the origin only, with scheme and host and optional port, not a path.      |
| `VITE_KEYCLOAK_URL`       | Web app | Base URL of the Keycloak server, for example `http://localhost:8081`.                                                           |
| `VITE_KEYCLOAK_REALM`     | Web app | Keycloak realm used for sign in, locally `kamerbiz`.                                                                            |
| `VITE_KEYCLOAK_CLIENT_ID` | Web app | Public OpenID Connect client used by the browser app, locally `kamerbiz-web`.                                                   |

The imported local realm currently allows the web client redirect pattern `http://localhost:5173/*`, the web origin `http://localhost:5173`, and the matching post logout redirect. If you change the Vite port, update all of these together:

1. Set `WEB_ORIGIN` in the root `.env` to the new origin, then export the root `.env` values when starting the API with `set -a; . ../../.env; set +a` from `services/api`. Editing `.env` alone does not change the environment of an already running API process.
2. Update the web client's `redirectUris`, `webOrigins`, and `post.logout.redirect.uris` in `infra/keycloak/kamerbiz-realm.json`.
3. Keep `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, and `VITE_KEYCLOAK_CLIENT_ID` aligned with the Keycloak server and imported client. These usually do not change when only the web port changes.

Restart the API and web development server after changing environment values. The realm import is applied when the Keycloak development container first initializes its data volume. If you change the realm JSON after that volume already exists, update the client in the Keycloak admin console or recreate the local Keycloak volume when it is safe to discard its data.

## 6. Pick up work

Find an issue on the project board. Confirm it is Ready and assigned to you. Read acceptance criteria and dependencies; ask questions in the issue before starting if anything is unclear. Move it to In Progress when you begin.

## 7. Deliver

Create a branch named from the issue, make focused changes, run tests, and open a PR using the template. Link the issue. Move the issue to Review when the PR is ready for review. Address review comments and merge only after approval and checks. The issue becomes Done only after merge.

## 8. Make the project easier for the next person

Update docs when setup, behavior, architecture, or workflow changes. Add ADRs for consequential decisions. Keep issues small, searchable, and actionable. Never commit secrets or real personal/payment data.
