# KamerBiz

**Stronger businesses. A stronger Cameroon.**

KamerBiz is building simple, accessible tools to help Cameroonian small businesses sell, manage and grow. The first release focuses on businesses selling physical products. English is the default product language, with French support.

## Project status

**Stage:** Foundation implementation. Product discovery and MVP decisions are documented; the application foundation is being established.

## Start here

1. [Project overview](docs/01-project/project-overview.md)
2. [MVP scope](docs/02-product/mvp-scope.md)
3. [Key workflows](docs/02-product/key-workflows.md)
4. [Technical overview](docs/04-technical/technical-overview.md)
5. [Development workflow](docs/05-delivery/development-workflow.md)
6. [Security baseline](docs/04-technical/security-baseline.md)
7. [Contributing and onboarding](CONTRIBUTING.md)
8. [Architecture Decision Records](docs/06-decisions/README.md)

## Repository structure

- `apps/web`: React + TypeScript web experience.
- `services/api`: Java 21 + Spring Boot modular monolith API.
- `docs`: product, technical, delivery and architecture documentation.
- `.github`: issue and PR templates, ownership, dependency updates and CI.

## Local development

Prerequisites: Git, Node.js 22+, npm, Java 21, Maven 3.9+, and Docker Compose.

1. Copy local configuration: `cp .env.example .env`.
2. Start PostgreSQL, Keycloak, and local object storage: `docker compose up -d postgres keycloak minio minio-init`.
3. Start the API: `cd services/api && mvn spring-boot:run`.
4. In another terminal, install and start the web app: `cd apps/web && npm ci && npm run dev`.
5. Open the Vite URL printed in the terminal. API health: `http://localhost:8080/api/v1/health`.

Product images use the local MinIO S3 compatible service. The bucket is created automatically and product images are publicly readable for storefront display. Production must use a managed S3 compatible bucket and configure its public read policy or CDN URL through the S3 environment variables.

The local Keycloak realm enables self registration and password recovery for development. Email verification is disabled locally. Configure SMTP and enable email verification before public launch. Never reuse local credentials outside a local machine.

The local Keycloak container uses development mode and an embedded database. Production requires a hardened Keycloak deployment with PostgreSQL persistence, TLS, SMTP, managed secrets, backups, monitoring, and recovery testing.

## Engineering workflow

Every change starts with a GitHub Issue. Work moves through **Backlog → Ready → In Progress → Review → Done**. Implement on an issue branch and merge through a reviewed PR. An issue is Done only after merge and acceptance criteria are met. See [development workflow](docs/05-delivery/development-workflow.md).

## Security

Read [SECURITY.md](SECURITY.md) and the [security baseline](docs/04-technical/security-baseline.md). Report vulnerabilities privately. Repository administrators must enable secret scanning, push protection, Dependabot security updates, and branch protection with required checks in GitHub settings.

## Product principles

- Mobile first and simple to use.
- English by default, French supported.
- Tenant data isolation is mandatory.
- Customer payments go directly to vendors in the MVP; KamerBiz does not hold funds.
- A payment screenshot is evidence to review, not proof of payment.
