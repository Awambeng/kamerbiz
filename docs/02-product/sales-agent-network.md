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

### Attribution policy to finalise

The implementation needs one explicit policy for:

- Attribution window duration.
- First-click versus last-eligible-click behaviour.
- Whether a customer's later direct visit retains an earlier referral.
- How multiple agents' links interact.
- Cross-device referrals, where browser-based attribution may not survive.
- Returns, partial refunds, partial fulfilment and cancelled order items.

Do not silently guess these rules in different parts of the application. Implement them in one documented attribution service and version the policy.

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
- Orders may contain items from different products and vendors; if multi-vendor checkout is not supported initially, enforce one vendor per order while keeping attribution and data models item-aware.
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
