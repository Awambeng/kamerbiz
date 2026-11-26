# First release scope

The first release should deliver a useful business-management foundation while designing the data model and architecture to support an open, multi-business sales-agent network.

The agent network should not be implemented as a vendor-approved or invitation-only model. Self-service, cross-business agent participation is a core product requirement from the beginning.

## Included in the first release

### Business account and store

- Business owner registration and login.
- Basic business profile and public store link.
- Store contact, delivery and payment information.

### Products and stock

- Add, edit and archive products.
- Product name, description, price, images and categories.
- Basic stock quantity and availability.
- Vendor-controlled setting to make a product available or unavailable for agent promotion.
- Vendor-defined commission amount or rate for each promoted product.

### Customer storefront and orders

- Customers can browse a public store and place orders directly without an agent referral link. Direct selling must work even when a vendor has no agents or has not enabled any products for agent promotion.
- Public catalogue and product pages.
- Cart and guest checkout.
- Customer contact and delivery or pickup details.
- Order reference and private tracking link.
- Vendor order review, acceptance, rejection and status updates.

### Payments and fulfilment

- Vendor payment instructions for supported local payment methods.
- Customer submission of transaction reference and optional screenshot.
- Vendor payment confirmation or rejection after checking their own account.
- Separate order, payment and fulfilment statuses.
- Basic delivery and pickup management.

### Open sales-agent network

- Self-service agent registration and login.
- No vendor invitation, vendor approval or manual identity validation required for agent onboarding.
- One agent account can access eligible products from multiple businesses.
- Browse/search eligible products across participating businesses.
- Generate or copy a unique tracking link for an eligible product.
- Share product details and links outside KamerBiz.
- Agent dashboard showing attributed orders and commission status.
- Vendor view of orders attributed to agents.
- Agent commission terms captured when an agent link is generated or when a customer order is attributed, so later changes do not silently alter existing orders.

### Commission records

- Commission calculation recorded per order item.
- Commission states distinguish pending, payable, paid, reversed and disputed.
- Commission becomes payable only after the configured eligibility conditions are met, including confirmed payment and completed fulfilment.
- Cancelled, unpaid, rejected, refunded or returned items do not generate payable commission; reversals are recorded where applicable.
- Vendor records an external payout to the agent, including amount, date and reference or note.
- Agent can see payout history.

### Basic reports

- Orders placed, payments confirmed and orders completed.
- Direct versus agent-attributed orders.
- Product performance.
- Agent-attributed sales and commission summaries.
- Source attribution where it can be reliably captured.

### Languages

- English and French interface.

## Not included in the first release

- KamerBiz holding customer payments or agent commission funds.
- Automatic MoMo or Orange Money payment confirmation unless a suitable official integration is separately approved.
- Automatic commission payouts or wallet balances backed by stored funds.
- Vendor approval as a prerequisite for agent registration.
- Mandatory identity verification to access basic agent features.
- Agents changing vendor prices, stock or product descriptions.
- Agents accepting orders or confirming payments on behalf of vendors.
- Complex multi-level referral trees or recruiting commissions.
- Paid advertising marketplace, bidding or sponsored placement.
- Delivery driver marketplace.
- Full accounting, tax management or advanced loyalty programme.
- Native mobile applications.

## Important product and security rules

- Agent accounts are global platform accounts, not accounts belonging to one vendor.
- A user can act as a vendor, an agent, or both under one login where appropriate; roles and permissions must be explicit.
- A vendor can only access that vendor's private business data.
- An agent can only access their own private profile, links, attributed orders and commission records, plus public/eligible product information.
- Customers must not see private agent or vendor information.
- A vendor's decision to enable or disable a product for promotion must not erase historical attribution or commission records.
- Commission terms must be snapshotted for attributed sales so later edits apply only to future eligible referrals.
- A screenshot alone must never mark a payment as paid.
- Order, payment, fulfilment and commission states must be modelled separately.
- Referral attribution must be deterministic, auditable and resistant to simple URL tampering.
- Stock must be checked when an order is accepted or otherwise reserved, to reduce overselling.

## Scope changes

A new feature must solve a clear user problem, fit the product direction and have an agreed test for success. Otherwise, record it for a later release.
