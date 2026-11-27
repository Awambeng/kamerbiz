# New contributor onboarding

This guide is designed so a new contributor can get oriented without needing a private walkthrough.

## 1. Understand the product

Read the README, product overview, MVP scope, key workflows, and sales-agent network documentation. Treat documented MVP decisions as the current baseline; use the open-questions document for unresolved details.

## 2. Understand the architecture

Read the technical overview, project structure, security baseline, and accepted ADRs. The backend is a modular monolith; do not introduce infrastructure or distributed systems without an issue and an ADR where appropriate.

## 3. Prepare your machine

Install Git, Node.js 22+, npm, Java 21, Maven 3.9+, and Docker Compose. Clone the repository and check out the branch associated with your assigned issue.

## 4. Run the system

Start PostgreSQL with `docker compose up -d postgres`. Start the API from `services/api` with `mvn spring-boot:run`. Start the frontend from `apps/web` with `npm install && npm run dev`. Confirm the API responds at `/api/v1/health`.

## 5. Pick up work

Find an issue on the project board. Confirm it is Ready and assigned to you. Read acceptance criteria and dependencies; ask questions in the issue before starting if anything is unclear. Move it to In Progress when you begin.

## 6. Deliver

Create a branch named from the issue, make focused changes, run tests, and open a PR using the template. Link the issue. Move the issue to Review when the PR is ready for review. Address review comments and merge only after approval and checks. The issue becomes Done only after merge.

## 7. Make the project easier for the next person

Update docs when setup, behavior, architecture, or workflow changes. Add ADRs for consequential decisions. Keep issues small, searchable, and actionable. Never commit secrets or real personal/payment data.
