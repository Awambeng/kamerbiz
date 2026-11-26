# Sales-agent network

## Purpose

The KamerBiz sales-agent network allows any person to promote eligible products from multiple KamerBiz businesses through one agent account and earn commission on qualifying sales.

It gives vendors an additional customer-acquisition channel without requiring agents to buy or hold stock.

## Product principles

0. **Direct sales always work:** The agent network is an additional sales channel, never a requirement for using a KamerBiz store. Customers can discover a store independently and place direct orders without clicking an agent link. Vendors can sell on KamerBiz without joining the agent network.
1. **Open participation:** Any person can create an agent account. No vendor invitation, vendor approval or manual identity validation is required for basic participation.
2. **One account, many businesses:** An agent can promote eligible products from multiple independent vendors.
3. **Vendor control:** Vendors decide which products are promoted, set commission terms and remain responsible for price, stock, payment and fulfilment.
4. **No stock ownership by agents:** Agents promote products; they do not purchase or store inventory on behalf of KamerBiz.
5. **Transparent attribution:** Vendors and agents can see which order items were attributed and how commission was calculated.
6. **No customer-money custody:** In the initial release, customers pay vendors directly and vendors pay agents directly.
7. **No misleading promises:** Agents must use the current product details and must not invent discounts, availability, delivery guarantees or product claims.

## Roles and permissions

### Agent

Can:

- Create and manage one agent profile.
- Browse products enabled for agent promotion across businesses.
- View vendor-defined commission terms and promotion conditions.
- Generate and copy referral links.
- Share product information and referral links.
- View their own attributed orders and commission records.
- View their own payout history.

Cannot:

- Change product price, stock, descriptions or images.
- Change vendor commission terms.
- Confirm customer payments.
- Accept or reject vendor orders.
- Promise delivery or availability on behalf of the vendor.
- View another agent's private performance or commission records.

### Vendor

Can:

- Enable or disable individual products for agent promotion.
- Set and update commission terms for future referrals.
- Define product promotion conditions.
- View agent-attributed orders and commission liabilities for their own business.
- Record commission payouts.
- Pause promotion of a product or report abuse.

Cannot:

- Rewrite historical attribution or silently change commission terms already snapshotted for an attributed order item.
- Access private data belonging to another vendor.

### Platform administrator

Can:

- Investigate abuse reports and platform integrity issues.
- Apply platform-level restrictions under documented moderation rules.
- Review audit events where authorised.

Administrator moderation is not a vendor approval gate. The default agent journey remains self-service and open.

## Product eligibility

A product is discoverable to agents only when:

- The vendor has enabled agent promotion for it.
- The product is active and available for ordering.
- The vendor has supplied a valid commission term.
- The product's promotion conditions are available to the agent.

Disabling promotion prevents new referral links or new attributions according to the platform rules. It must not delete or alter existing valid order attributions.

## Referral links and attribution

Each referral link should contain an opaque, unguessable referral token. Do not expose sequential database identifiers or trust an agent-supplied vendor, product or commission amount as authoritative.

Attribution should be stored server-side and attached to the relevant order item at checkout. The order must retain:

- Agent account identifier.
- Vendor and product identifiers.
- Referral token or attribution record.
- Attribution timestamp and source context where available.
- Commission basis and rate/amount snapshot.
- Currency and calculated commission.
- Attribution rule/version used.

### Agreed first-release attribution policy

- **Reusable links:** One referral link can be used by unlimited customers and can generate multiple eligible sales.
- **Attribution window:** 30 days from a valid referral click. A customer may return and buy again within that window without clicking the link again.
- **Last valid referral wins:** If the same browser/customer context follows valid links from different agents for the same product, the latest valid agent referral receives attribution.
- **Direct visits:** A direct visit to the store does not erase an unexpired valid referral. A purchase with no valid referral remains a direct sale.
- **Per-item recording:** Attribution is resolved server-side and copied to the relevant order item at checkout. A click is not a sale and does not itself create payable commission.
- **Repeat purchases:** Repeat purchases of the referred product within the 30-day window can earn commission, provided each order item meets payment and fulfilment eligibility rules.
- **Browser/device limitation:** The first release uses a secure first-party browser identifier. Attribution may not survive cookie deletion, private browsing or switching devices. Do not promise cross-device attribution.
- **Product scope:** The proposed first-release default is that a product link attributes purchases of that same product only. Whether it should also cover other eligible products from the same vendor remains an open product decision.
- **Promotion disabled:** Disabling promotion stops new referrals. The proposed default is to honour an attribution already recorded until its 30-day expiry; confirm this before implementation.

Use one backend attribution service and version the policy. Never trust agent IDs, product IDs or commission values supplied by the browser as authoritative.

## Commission lifecycle

Commission is calculated per order item, not only per order.

Suggested lifecycle:

- **Pending:** Attributed order item exists, but eligibility conditions are not yet met.
- **Payable:** Payment is confirmed and the item is fulfilled/completed under the applicable policy.
- **Paid:** Vendor has recorded payment to the agent.
- **Reversed:** A qualifying cancellation, refund or return invalidated some or all of the commission.
- **Disputed:** Vendor or agent has raised a question requiring review.

The commission record should preserve the original calculation and record adjustments as separate events. Avoid deleting or overwriting financial history.

For partial refunds or partial returns, reverse only the affected item or affected commission portion where the business rules permit.

## Commission terms

Vendors may define commission as:

- A fixed amount per qualifying unit; or
- A percentage of the eligible product sale amount.

The exact calculation basis must be shown to the agent before promotion. The system must specify how discounts, delivery fees, taxes, quantity changes and refunds affect the calculation.

When a referral is attributed, snapshot the applicable terms. Changes made later apply to new referrals only.

## Initial settlement model

KamerBiz records commissions but does not hold funds or execute payouts in the initial release.

1. Vendor reviews payable commission.
2. Vendor pays the agent directly using an agreed method.
3. Vendor records amount, date, payment method and reference/note.
4. Agent sees the payout record.
5. Any mismatch can be marked disputed and reviewed.

Do not represent a commission as paid merely because the vendor clicked a generic confirmation without recording the payout event.

## Abuse and operational safeguards

Open registration does not mean unrestricted platform behaviour. The platform should support:

- Rate limits for account creation and referral-link generation.
- Abuse reporting and platform moderation.
- Detection of repeated self-referrals or suspicious order patterns where feasible.
- Audit history for commission and attribution changes.
- Protection against referral-token guessing or tampering.
- Clear terms prohibiting spam, deceptive promotion and false product claims.
- Vendor ability to pause a product's participation in the network.

These safeguards should not become a manual approval process for every new agent.

## Data and architecture implications

- Agent identity is global to KamerBiz, not nested under one vendor tenant.
- Vendor business data remains tenant-isolated.
- A many-to-many relationship exists between agents and vendors through attributed activity, not through vendor-owned agent accounts.
- Products have vendor-controlled agent-promotion settings and commission terms.
- Referral links map to server-side attribution records.
- The first release enforces one vendor per order. A customer can place separate orders with different vendors; a mixed-vendor cart must be separated into vendor-specific checkouts.
- Attribution is optional and stored per order item. Direct store visits and direct orders work without any agent link.
- The 30-day repeat-purchase attribution is browser/device-based in the first release; cross-device attribution is not promised.
- Commission records reference order items and preserve snapshots.
- Financial state changes are append-only or auditable.
- Agent dashboards aggregate only the signed-in agent's own records across vendors.

## Success measures

- Number of agents who create an account.
- Number of agents who share at least one eligible product.
- Number of vendors who enable products for promotion.
- Click-to-order conversion, where measurable.
- Attributed orders with confirmed payment.
- Completed attributed order value.
- Commission payable and paid.
- Repeat activity by agents and vendors.
- Disputes, cancellations, returns and suspected abuse.

Do not treat clicks, sign-ups or placed orders as successful sales. Track confirmed payments and completed fulfilment separately.
