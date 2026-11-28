# KamerBiz database design

**Status:** Logical target model. Section 2 documents the physical MVP schema currently created by Flyway.  
**Recommended database:** PostgreSQL  
**Migration tool:** Flyway

This document translates the domain model into a practical first-release database structure. It is intentionally a logical design: column types, exact indexes and constraints should be reviewed alongside the first Flyway migrations.

## 1. Database design principles

- Use a relational model and explicit foreign keys.
- Use UUIDs (or another non-sequential public-safe identifier) for internal entity IDs. Public referral tokens and order tracking tokens must be separate, high-entropy values.
- Store money as integer amounts in the currency's smallest unit (for XAF, whole FCFA) or an appropriately constrained `NUMERIC`; never use floating-point types.
- Store an explicit ISO currency code with monetary values.
- Store timestamps as `TIMESTAMPTZ` in UTC.
- Use `created_at` and `updated_at` on mutable business records.
- Prefer status history/event tables for payment review, commission and payout changes.
- Use soft archival for catalogue/business records where historical orders may still reference them; do not cascade-delete financial history.
- Add `business_id` to vendor-owned records and index it as the leading column for tenant-scoped access patterns.

## 2. Current physical MVP schema

The following tables and columns reflect the current implementation. The proposed target model in section 3 is broader and includes planned agent, payment, fulfilment and commission capabilities.

| Implemented table     | Current responsibility                   | Important details                                                                                                                          |
| --------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `user_account`        | Global identity profile                  | Identity issuer and subject, email, display name and verification state.                                                                   |
| `business_account`    | Business tenant and public store profile | Business name, unique slug, description, logo, contact details, address, city and region. There is no separate physical `store` table yet. |
| `business_membership` | Vendor team access                       | Business, user and role with a unique business and user pair.                                                                              |
| `product`             | Business catalogue                       | Business scoped product code, name, description, category, price, stock, image URL and archive flag.                                       |
| `customer_order`      | Guest order and tracking                 | Business, reference, tracking token hash, customer and delivery snapshots, order and fulfilment statuses, total and timestamps.            |
| `customer_order_item` | Order line snapshots                     | Product reference, product name, unit price, quantity and line total.                                                                      |
| `order_status_event`  | Order audit history                      | Business and order, event type, old status, new status, optional actor user and creation timestamp.                                        |

Implementation details:

- Flyway V3 adds the public slug and store profile fields directly to `business_account`. A separate `store` table and a `published` flag are not implemented. A store is currently resolved by its business slug, and the public catalogue includes non archived products.
- Product prices are stored as `NUMERIC(14,2)`. Order totals, order item unit prices and line totals use `NUMERIC(18,2)` so the maximum supported cart can be represented without overflow.
- Order tracking tokens are generated as high entropy random values. Only their SHA 256 hexadecimal hashes are stored in `customer_order.tracking_token_hash`. The raw token is returned in the private tracking URL and is not stored.
- The unique constraint on `tracking_token_hash` already creates its lookup index. There is no additional tracking index.
- An order belongs to exactly one business. Customer contact and fulfilment details are stored as order snapshots. Customer accounts are not required.
- `order_status_event` records order creation, vendor acceptance or rejection, and fulfilment transitions. Guest checkout creation has no actor user. Vendor initiated transitions store the authenticated member.
- The current order schema does not include payment submissions, payment evidence, currency columns, agent attribution or commission records. Those remain future capabilities.
- Current amounts are presented as FCFA. Multi currency support is not implemented.
- Public storefront and checkout errors use a JSON response with `status`, `error` and `message`. Unknown stores or tracking tokens return 404, invalid checkout input returns 400, and stock conflicts return 409. These errors are handled at the public controller boundary and do not expose an OAuth2 bearer challenge.
- The existing `NUMERIC(14,2)` product price supports the configured maximum of `999999999999.99`. The `NUMERIC(18,2)` order total and line amount columns support the full allowed cart of 50 distinct lines with up to 100 units each at that maximum price.

Applied Flyway migrations:

1. `V1__initial_schema.sql` - initial database schema.
2. `V2__identity_and_business_memberships.sql` - user accounts, platform roles, business accounts and memberships.
3. `V3__business_profile_and_store_slug.sql` - business profile, contact and public slug fields.
4. `V4__product_catalog.sql` - product catalogue.
5. `V5__product_import_batches.sql` - product import batch tracking.
6. `V6__product_image_filename.sql` - product image filename mapping.
7. `V7__product_image_cleanup.sql` - durable product image cleanup.
8. `V8__customer_orders.sql` - guest orders and order items.
9. `V9__order_status_events.sql` - order and fulfilment audit history.

## 3. Proposed target tables

### Identity and access

| Table                 | Important fields                                                                           | Notes                                                                  |
| --------------------- | ------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `app_user`            | `id`, `email`, `phone`, `password_hash` or identity-provider subject, `status`, timestamps | Global login identity. Do not duplicate a user for agent/vendor roles. |
| `business`            | `id`, `name`, `slug`, `status`, `default_currency`, timestamps                             | Tenant root.                                                           |
| `business_membership` | `id`, `business_id`, `user_id`, `role`, `status`, timestamps                               | Unique `(business_id, user_id)`. Roles such as owner/admin/staff.      |
| `agent_profile`       | `id`, `user_id`, `display_name`, `status`, timestamps                                      | Unique `user_id`; global across vendors.                               |

### Store, catalogue and stock

| Table                | Important fields                                                                                                                           | Notes                                                                                                                                                                                            |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `store`              | `id`, `business_id`, `public_slug`, `published`, `locale_default`, store settings                                                          | One store per business initially; unique `business_id` and `public_slug`.                                                                                                                        |
| `product_category`   | `id`, `business_id`, `name`, `status`                                                                                                      | Business-scoped.                                                                                                                                                                                 |
| `product`            | `id`, `business_id`, `category_id`, `sku`, `name`, `description`, `price_amount`, `currency`, `stock_quantity`, `status`                   | Unique `(business_id, id)` and optionally `(business_id, sku)`.                                                                                                                                  |
| `product_image`      | `id`, `business_id`, `product_id`, `object_key`, `sort_order`                                                                              | Private or public object access must be deliberate; never store raw file bytes in the row.                                                                                                       |
| `inventory_movement` | `id`, `business_id`, `product_id`, `movement_type`, `quantity_delta`, `reason`, `actor_user_id`, timestamp                                 | Auditable stock adjustments; do not rely only on a mutable stock number.                                                                                                                         |
| `product_promotion`  | `id`, `business_id`, `product_id`, `enabled`, `commission_type`, `commission_value`, `currency`, `terms_version`, `conditions`, timestamps | One current promotion configuration per product in the MVP. Changes increment `terms_version`. Consider a separate immutable terms-version table if terms history must be independently queried. |

### Referral and attribution

| Table                  | Important fields                                                                                                                                                                               | Notes                                                                                                                      |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `referral_link`        | `id`, `agent_profile_id`, `business_id`, `product_id`, `product_promotion_id`, `token_hash`, `status`, `created_at`                                                                            | Reusable link. Unique token hash; store a hash of the opaque token where feasible. Product/vendor relationship must match. |
| `referral_attribution` | `id`, `agent_profile_id`, `business_id`, `product_id`, `referral_link_id`, `visitor_key_hash`, optional `customer_id`, `clicked_at`, `expires_at`, `policy_version`, `terms_version`, `status` | Server-side attribution. Latest valid referral wins for the same product and visitor context. Do not expose visitor key.   |

### Customers, orders and fulfilment

| Table                | Important fields                                                                                                                                                                                        | Notes                                                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `customer`           | `id`, `business_id`, `name`, `phone`, optional `email`, timestamps                                                                                                                                      | Business-scoped. Customer account is not required. Avoid assuming a phone number is globally unique.                        |
| `customer_order`     | `id`, `business_id`, `customer_id` nullable, `order_number`, `status`, `currency`, totals, customer/contact/address snapshots, `tracking_token_hash`, timestamps                                        | Exactly one vendor. Unique `(business_id, order_number)` and unique tracking token hash.                                    |
| `order_item`         | `id`, `business_id`, `order_id`, `product_id`, product name/price snapshots, `quantity`, line totals, nullable `referral_attribution_id`, nullable `agent_profile_id`, commission terms snapshot fields | Product must belong to the same business as the order. Attribution optional.                                                |
| `fulfilment`         | `id`, `business_id`, `order_id`, `status`, method, delivery/pickup details, timestamps                                                                                                                  | MVP can start with one fulfilment per order; add item-level fulfilment records if partial fulfilment is required at launch. |
| `order_status_event` | `id`, `business_id`, `order_id`, `old_status`, `new_status`, `actor_user_id`, `note`, timestamp                                                                                                         | Audit order state changes.                                                                                                  |

### Payment

| Table                   | Important fields                                                                                                                             | Notes                                                                                                 |
| ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `vendor_payment_method` | `id`, `business_id`, `method_type`, `display_name`, `payment_number`, `instructions`, `enabled`                                              | Store only the payment details needed to instruct customers. Never store PINs or account credentials. |
| `payment_submission`    | `id`, `business_id`, `order_id`, `method_id`, `amount`, `currency`, `transaction_reference`, `evidence_object_key`, `status`, `submitted_at` | Evidence object is private. A submission is not proof of payment.                                     |
| `payment_review`        | `id`, `business_id`, `payment_submission_id`, `reviewer_user_id`, `decision`, `note`, `reviewed_at`                                          | Append-only decision history; reviewer must have access to the business.                              |

### Commission and vendor payouts

| Table                | Important fields                                                                                                                                                                                                                | Notes                                                                                                                        |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `agent_commission`   | `id`, `business_id`, `agent_profile_id`, `order_id`, `order_item_id`, `currency`, `commission_type_snapshot`, `commission_value_snapshot`, `basis_amount`, `quantity_snapshot`, `original_amount`, `current_status`, timestamps | At most one base commission per order item for the initial model; enforce a unique key on `order_item_id` where appropriate. |
| `commission_event`   | `id`, `business_id`, `agent_commission_id`, `event_type`, `amount_delta`, `from_status`, `to_status`, `reason`, `actor_user_id`, timestamp                                                                                      | Append-only history for payable, paid, reversed, disputed and adjustment events.                                             |
| `vendor_payout`      | `id`, `business_id`, `agent_profile_id`, `amount`, `currency`, `method`, `external_reference`, `note`, `recorded_by_user_id`, `paid_at`, timestamps                                                                             | Records an external vendor-to-agent payment; does not move money through KamerBiz.                                           |
| `vendor_payout_item` | `id`, `vendor_payout_id`, `agent_commission_id`, `allocated_amount`                                                                                                                                                             | Allows a payout to cover one or more commissions and supports partial settlement if the product policy permits it.           |

### Platform audit

| Table         | Important fields                                                                                                        | Notes                                                                                                      |
| ------------- | ----------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `audit_event` | `id`, nullable `business_id`, nullable `actor_user_id`, `event_type`, `entity_type`, `entity_id`, `metadata`, timestamp | Do not put secrets, PINs, full payment evidence or unnecessary personal data in metadata. Restrict access. |

## 4. Critical constraints and indexes

Implement these in the database where possible, in addition to backend validation:

1. `business_membership`: unique `(business_id, user_id)`.
2. `agent_profile`: unique `user_id`.
3. `store`: unique `business_id` and unique `public_slug`.
4. `product`: unique `(business_id, id)`; optionally unique `(business_id, sku)` when SKU is supplied.
5. `product_promotion`: unique `(business_id, product_id)` for the single current promotion configuration.
6. `referral_link`: unique `token_hash`; foreign keys must ensure the promotion, product and business match.
7. `referral_attribution`: index `(visitor_key_hash, product_id, clicked_at DESC)` and `(agent_profile_id, clicked_at DESC)`; expiry must be after click time.
8. `customer_order`: unique `(business_id, order_number)` and unique `tracking_token_hash`.
9. `order_item`: enforce `(business_id, order_id)` references the order and `(business_id, product_id)` references the product. This prevents a product from another vendor entering an order.
10. `agent_commission`: unique `order_item_id` for the initial one-base-commission-per-item model; index `(business_id, current_status)` and `(agent_profile_id, current_status)`.
11. `vendor_payout_item`: prevent allocating more than the commission's remaining payable amount; enforce transactionally in the service layer and, where possible, with constraints.
12. All tenant-owned tables: indexes beginning with `business_id` for common lookups.

Use transactions when creating an order and its order items, attaching attribution, decrementing/reserving stock, recording payment reviews, and allocating commission payouts.

## 5. Attribution and order creation transaction

At checkout, the backend should:

1. Resolve the vendor-specific cart and reject products belonging to another vendor.
2. Re-check product status, current price and stock.
3. Resolve valid server-side attribution for each product using the browser visitor key and 30-day expiry.
4. Create the order with one `business_id` and snapshot customer/contact/delivery details.
5. Create each order item with product and price snapshots.
6. For attributed items, copy the agent, attribution reference, commission terms version and calculation inputs onto the item.
7. Create the pending commission record (or create it when order acceptance/payment rules require it, while preserving attribution snapshot).
8. Commit the order and related records atomically.

Do not accept `agent_id`, commission rate or vendor identity from the browser as authoritative checkout input.

## 6. Commission calculation and settlement

The first-release calculation service should accept an immutable input snapshot and return a deterministic amount. Do not spread commission calculations across controllers or frontend code.

Before implementation, agree:

- Percentage commission basis: before or after product discounts.
- Whether delivery charges and taxes are excluded.
- Whether fixed commission is per unit or per order item.
- Treatment of quantity changes, partial fulfilment, partial refunds and returns.
- Whether a vendor can pay a commission in parts.

A payout must allocate only payable commission amounts. Recording a payout must be transactional and idempotent so retries do not double-settle the same amount.

## 7. Data access and tenant isolation

- Every vendor API query must include the authorised `business_id` in the repository/service query.
- Never load an order by ID alone and then assume the user may access it.
- Use composite foreign keys for relationships that must remain inside one business.
- Agent APIs must scope commission and referral queries to the authenticated user's `agent_profile_id`.
- Agents must not see customer phone numbers, full delivery addresses or payment evidence by default.
- Public storefront APIs expose only published store and active public product fields.
- Consider PostgreSQL Row-Level Security as defence in depth, but do not treat it as a substitute for correct application authorization.

## 8. Future migration planning

The implemented migrations are listed in section 2. Future migrations should introduce the remaining target model in small, forward only steps, without rewriting or renumbering migrations that have already been applied.

Likely future areas include separate store publication settings, customer records, payment methods and submissions, explicit fulfilment records, referral links and attribution, agent commissions, vendor payouts and broader audit events. Add each only when its workflow is being implemented.

Keep each migration small and reviewed. Use Testcontainers to verify foreign keys, uniqueness, tenant boundaries, transaction behaviour and commission calculations against PostgreSQL.

## 9. Not decided by this schema

This logical model deliberately does not settle product policy for cross-device attribution, attribution extending from one product to other products from the same vendor, commission basis, partial returns, payout thresholds or the full identity-verification policy. Track these in `docs/06-decisions/open-questions.md` before implementing the relevant behaviour.
