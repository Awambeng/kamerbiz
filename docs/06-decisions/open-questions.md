# Open questions

These questions need answers before the related behaviour is implemented. They do not change accepted product decisions recorded in `decision-log.md`.

## Agent attribution

- Should a referral for one product earn commission only when that same product is purchased, or should it also apply to other eligible products from the same vendor during the 30-day window? **Proposed first-release default: same referred product only.**
- Should a valid attribution survive if the vendor disables the product's agent promotion before the 30-day window expires? **Proposed default: honour already-recorded attribution until expiry; stop new referrals.**
- What should happen when the same customer uses different browsers/devices? **First-release limitation: browser/device-based attribution; cross-device matching is not promised.**
- Should account-based attribution be introduced later for customers who sign in, and what consent and privacy rules would apply?
- How should self-referrals and suspicious repeat orders be detected without blocking legitimate customers?

## Commission calculation

- Is percentage commission calculated on product subtotal before or after discounts?
- Are delivery fees and taxes excluded from commission?
- Is fixed commission calculated per unit or per order item?
- How are quantity changes, partial fulfilment, partial refunds and returns handled?
- Can vendors use fixed and percentage commissions on different products?
- Can vendors pay commissions in parts, and should a minimum payout threshold exist?

## Vendor and agent operations

- Which fields and conditions must a vendor provide before a product can be promoted?
- Can vendors pause an individual agent for abuse without introducing an approval gate for everyone?
- What dispute process should exist for missing or incorrect commission records?
- Which payout methods and payout references should be recorded?

## Customer and checkout experience

- How should a cart containing products from several vendors be presented? **Accepted first-release rule: separate vendor-specific checkouts and orders.**
- Which customer contact fields are mandatory for guest checkout in each fulfilment scenario?
- What is the order cancellation policy after payment has been confirmed?

## Platform operations

- What moderation actions are available for spam, fraud or deceptive promotion?
- What account recovery and contact channels are required?
- What retention period applies to referral, order and commission audit records?
- What privacy notice and consent are needed for browser-based referral attribution?

## Business model

- Would vendors pay a subscription, a commission-related fee or another model?
- Should agent network access be included in the base plan or offered as a paid capability?

## Technology and operations

- Where should the platform be hosted?
- What will hosting and file storage cost?
- What backup, monitoring and support process is needed?
- What data should be kept, and for how long?

## How to close a question

Record the answer, evidence, date and decision in the decision log when the team agrees.
