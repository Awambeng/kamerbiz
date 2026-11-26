# Technical overview

## Purpose

This document records the proposed technical direction for the first KamerBiz release. It should be reviewed before production implementation.

## Suggested starting architecture

Build one application with clear internal modules. This is simpler to develop and operate than splitting the first version into many separate services.

| Area              | Suggested technology      | Why                                                    |
| ----------------- | ------------------------- | ------------------------------------------------------ |
| Web interface     | React and TypeScript      | Build a responsive user interface                      |
| Styling           | Tailwind CSS              | Keep visual styles consistent                          |
| Backend           | Java and Spring Boot      | Handle business rules and data                         |
| Database          | PostgreSQL                | Store business, catalogue, order and financial records |
| Database changes  | Flyway                    | Track database updates                                 |
| Login and access  | Spring Security           | Protect accounts and actions                           |
| File storage      | S3-compatible storage     | Store product images and private payment evidence      |
| API description   | OpenAPI                   | Explain how the frontend uses the backend              |
| Tests             | JUnit and Testcontainers  | Test business rules and database behaviour             |
| Build and release | Docker and GitHub Actions | Build and check changes                                |

The final hosting provider and cost plan still need to be chosen.

## Application modules

Keep these as internal modules in a modular monolith initially:

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

Modules should communicate through explicit application services and domain events where useful. Do not introduce microservices, Kafka or Kubernetes for the MVP without a demonstrated need.

## Data model and domain rules

See:

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

## Security basics

- Store passwords safely using trusted security tools.
- Use secure sessions or tokens.
- Check access on the backend and scope vendor queries by the authorised business.
- Keep payment evidence private.
- Use hard-to-guess order tracking links and referral tokens.
- Limit what a tracking link reveals.
- Record important actions such as payment approval and commission changes.
- Do not store payment PINs or bank login details.
- Keep secrets out of source code.
- Back up the database and test that backups can be restored.
- Do not expose customer phone numbers, delivery addresses or payment evidence to agents by default.

## Payment handling

At first, customers pay the business directly. KamerBiz records the payment details and the business owner's review decision. KamerBiz does not hold customer funds or agent commission funds.

Do not assume that automatic payment checks are available for every personal or merchant payment number. Research provider rules and access before planning this feature.

## Technical principles

- Start with a simple system.
- Avoid separate services unless there is a clear need.
- Keep business rules in the backend.
- Test important order, attribution, tenant-isolation and payment rules.
- Make the interface usable on mobile devices.
- Keep technical choices documented.
