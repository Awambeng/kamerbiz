# New contributor onboarding

This guide is designed so a new contributor can get oriented without needing a private walkthrough.

## 1. Understand the product

Read the README, product overview, MVP scope, key workflows, and sales-agent network documentation. Treat documented MVP decisions as the current baseline; use the open-questions document for unresolved details.

## 2. Understand the architecture

Read the technical overview, project structure, security baseline, and accepted ADRs. The backend is a modular monolith; do not introduce infrastructure or distributed systems without an issue and an ADR where appropriate.

## 3. Prepare your machine

Install Git, Node.js 22+, npm, Java 21, Maven 3.9+, and Docker Compose. Clone the repository and check out the branch associated with your assigned issue.

## 4. Run the system

Copy the local environment template with `cp .env.example .env`. Start PostgreSQL and Keycloak with `docker compose up -d postgres keycloak`. The Keycloak container imports the KamerBiz development realm automatically. Start the API from `services/api` with `mvn spring-boot:run`. In another terminal, install and start the frontend from `apps/web` with `npm ci && npm run dev`. Open the Vite URL printed in the terminal and confirm the API responds at `http://localhost:8080/api/v1/health`.

The local realm supports self registration and password recovery. Email verification is disabled for local development. The local Keycloak administrator credentials are development only and must never be reused outside a local machine. Configure SMTP and enable email verification before public launch. Production requires a hardened Keycloak deployment with PostgreSQL persistence, TLS, managed secrets, backups, monitoring, and recovery testing.

## 5. Pick up work

Find an issue on the project board. Confirm it is Ready and assigned to you. Read acceptance criteria and dependencies; ask questions in the issue before starting if anything is unclear. Move it to In Progress when you begin.

## 6. Deliver

Create a branch named from the issue, make focused changes, run tests, and open a PR using the template. Link the issue. Move the issue to Review when the PR is ready for review. Address review comments and merge only after approval and checks. The issue becomes Done only after merge.

## 7. Make the project easier for the next person

Update docs when setup, behavior, architecture, or workflow changes. Add ADRs for consequential decisions. Keep issues small, searchable, and actionable. Never commit secrets or real personal/payment data.
