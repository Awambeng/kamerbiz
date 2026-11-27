# Decision log

This file records important project decisions so the team can understand what was agreed and why.

Use these statuses:

- **Proposed:** An idea that still needs discussion.
- **Accepted:** The team has agreed to use it.
- **Needs validation:** The choice must be checked with users or through research.
- **Replaced:** A newer decision has taken its place.

| Date       | Decision                                                                                                          | Status                          | Reason or next step                                                                                           |
| ---------- | ----------------------------------------------------------------------------------------------------------------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| 2026-11-25 | Use KamerBiz as the working brand name                                                                            | Accepted as working name        | Check domain, social handle and trademark availability                                                        |
| 2026-11-25 | Use hope as the main emotional direction                                                                          | Accepted                        | Keep the brand helpful, positive and honest                                                                   |
| 2026-11-25 | Start with businesses selling physical products                                                                   | Proposed, needs validation      | Begin with fashion, perfume, cosmetics and general product sellers                                            |
| 2026-11-25 | Build one platform with connected Sell, Manage and Grow areas                                                     | Accepted direction              | Keep one joined-up experience                                                                                 |
| 2026-11-25 | Start with manual payment review                                                                                  | Accepted direction              | Business checks its own payment account; screenshot alone is not proof                                        |
| 2026-11-25 | Support English and French, with English as default                                                               | Accepted                        | Apply this across customer, vendor and agent experiences                                                      |
| 2026-11-25 | Prefer a simple application structure for the first release                                                       | Accepted                        | Formalised in [ADR-0001](ADR-0001-modular-monolith.md)                                                        |
| 2026-11-26 | Agent registration is self-service and does not require vendor invitation, approval or manual identity validation | Accepted product direction      | Keep participation open and reduce onboarding friction                                                        |
| 2026-11-26 | One agent account can promote eligible products from multiple businesses                                          | Accepted product direction      | Agent identity is global to KamerBiz, not vendor-specific                                                     |
| 2026-11-26 | Vendors control product eligibility and commission terms                                                          | Accepted product direction      | Agents promote only products vendors have enabled; vendors retain commercial control                          |
| 2026-11-26 | Commission is attributed and calculated per order item                                                            | Accepted product direction      | Each product line can have its own attribution and commission terms                                           |
| 2026-11-26 | KamerBiz records but does not hold or transfer agent commission funds in the initial release                      | Accepted product direction      | Vendor pays agent directly; platform records payout evidence                                                  |
| 2026-11-26 | Direct customer orders must work without agent referral links                                                     | Accepted product decision       | The agent network is an additional sales channel, never a requirement for a vendor to sell                    |
| 2026-11-26 | Referral links are reusable and can generate eligible sales for multiple customers                                | Accepted product decision       | Agents earn for each qualifying sale they generate, not only the first click or first customer                |
| 2026-11-26 | Referral attribution lasts 30 days from a valid referral click                                                    | Accepted product decision       | Customers may return and buy again within the window without clicking the link again                          |
| 2026-11-26 | The latest valid agent referral wins when multiple agents refer the same product                                  | Accepted product decision       | Provides a clear last-valid-referral rule; direct visits do not erase an unexpired referral                   |
| 2026-11-26 | One vendor per customer order in the first release                                                                | Accepted product decision       | Keeps payment, order review and fulfilment manageable; customers can place separate orders with other vendors |
| 2026-11-26 | Agent accounts are global and may coexist with vendor roles under one user identity                               | Accepted architecture direction | One person can participate in multiple KamerBiz roles without duplicate accounts                              |
| 2026-11-26 | Use a modular monolith and PostgreSQL for the initial architecture                                                | Accepted technical decision     | Recorded in [ADR-0001](ADR-0001-modular-monolith.md)                                                          |
| 2026-11-27 | Use React + TypeScript + Vite, Java 21 + Spring Boot, PostgreSQL and Flyway                                       | Accepted technical decision     | Recorded in [ADR-0002](ADR-0002-application-stack.md)                                                         |
| 2026-11-27 | Track all work through GitHub Issues and PRs using Backlog → Ready → In Progress → Review → Done                  | Accepted delivery decision      | Definition of Ready/Done and delivery ceremonies are in the development workflow                              |
| 2026-11-26 | Market research plan is not being expanded as part of this documentation change                                   | Accepted                        | Proceed with product refinement as requested                                                                  |

## Adding a decision

Add a row when a choice affects product scope, users, brand, money, security or technical direction. Include the date, decision, status and reason. Do not silently remove important decisions. For consequential technical decisions, create a full ADR using the [ADR process](README.md).
