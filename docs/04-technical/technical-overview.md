# Technical overview

## Purpose

This document records the selected technical direction for the first KamerBiz release. Consequential changes must be discussed in an issue and recorded as ADRs.

## Starting architecture

KamerBiz uses a **modular monolith** for the backend and a separate web frontend. This keeps development and operations simple while preserving domain boundaries.

| Area             | Selected technology                                                  | Purpose                                          |
| ---------------- | -------------------------------------------------------------------- | ------------------------------------------------ |
| Web interface    | React, TypeScript and Vite                                           | Responsive, mobile-first user experience         |
| Styling          | CSS foundation; Tailwind can be introduced with a dedicated UI issue | Consistent and maintainable UI                   |
| Backend          | Java 21 and Spring Boot                                              | Business rules and API                           |
| Database         | PostgreSQL                                                           | Business, catalogue, order and financial records |
| Database changes | Flyway                                                               | Versioned schema migrations                      |
| Login and access | Keycloak, OIDC and Spring Security                                   | Authentication and API authorization             |
| File storage     | S3-compatible object storage (future integration)                    | Product images and private payment evidence      |
| API description  | OpenAPI (planned)                                                    | Document frontend/backend contract               |
| Tests            | JUnit; Testcontainers planned for integration coverage               | Verify business rules and database behaviour     |
| Build and checks | Maven, npm, Docker Compose and GitHub Actions                        | Repeatable development and CI                    |

See [ADR-0001](../06-decisions/ADR-0001-modular-monolith.md), [ADR-0002](../06-decisions/ADR-0002-application-stack.md), and [ADR 0003](../06-decisions/ADR-0003-identity-provider.md).

## Repository layout

- `apps/web` - React + TypeScript application.
- `services/api` - Spring Boot modular monolith.
- `docs` - product, technical, delivery and architecture documentation.
- `.github` - issue/PR templates, CI and dependency automation.

See [project structure and module conventions](project-structure.md).

## Application modules

Keep these as internal modules in the modular monolith:

- **Identity and access:** users, business memberships, roles and agent profiles.
- **Business and store:** business settings, public store and payment instructions.
- **Catalogue and inventory:** categories, products, images and stock movements.
- **Customer and checkout:** guest customer details, vendor-specific carts and order creation.
- **Orders and fulfilment:** vendor order review, status changes, pickup and delivery tracking.
- **Payments:** customer payment submissions and vendor manual review.
- **Agent network:** eligible product discovery, referral links and server-side attribution.
- **Commissions and payouts:** item-level commission calculation, history and vendor-recorded external payouts.
- **Reporting:** direct and agent-attributed performance summaries.
- **Administration and audit:** moderation, audit history and platform operations.

Modules communicate through explicit application services and domain events where useful. Do not introduce microservices, Kafka or Kubernetes for the MVP without a demonstrated need.

## Data model and domain rules

See:

- [Business onboarding](business-onboarding.md)
- [Domain model](domain-model.md)
- [Database design](database-design.md)

Important rules:

- A customer can order directly from a public store without an agent referral.
- The first release enforces one vendor per order.
- Agent identity is global to KamerBiz and may span multiple vendors.
- Referral attribution is optional, reusable and recorded server-side.
- The agreed attribution window is 30 days from a valid referral click.
- Commission is recorded per attributed order item and its terms are snapshotted.
- Vendor data is tenant-isolated; an agent sees only their own agent records and the minimum order information needed.

## Data separation

Every private business record must belong to a business account. The backend must check access on every request. Hiding another business's data in the user interface is not enough.

Use database constraints to prevent relationships across businesses, especially between orders and products. The agent profile is global, but vendor-owned product, order, payment and payout records remain scoped to their business.

## Security

The initial security requirements are documented in the [security baseline](security-baseline.md). At minimum:

- Store passwords safely using trusted security tools.
- Check access on the backend and scope vendor queries by the authorised business.
- Keep payment evidence private and authorize every upload/download.
- Use hard-to-guess order tracking links and referral tokens.
- Record important actions such as payment approval and commission changes.
- Do not store payment PINs or bank login details.
- Keep secrets out of source code.
- Back up the database and test that backups can be restored.
- Do not expose customer phone numbers, delivery addresses or payment evidence to agents by default.

Keycloak owns credentials and authentication sessions. Spring Security validates API access tokens. KamerBiz owns local user profiles, platform roles, business memberships, and tenant authorization. The current local Keycloak configuration is for development only. Production requires a hardened deployment, TLS, SMTP, managed secrets, backups, monitoring, and recovery testing.

## Payment handling

At first, customers pay the business directly. KamerBiz records the payment details and the business owner's review decision. KamerBiz does not hold customer funds or agent commission funds.

Do not assume that automatic payment checks are available for every personal or merchant payment number. Research provider rules and access before planning this feature.

## Technical principles

- Start with a simple system.
- Avoid separate services unless there is a clear need.
- Keep business rules in the backend.
- Test important order, attribution, tenant-isolation and payment rules.
- Make the interface usable on mobile devices.
- Keep technical choices documented in ADRs.
