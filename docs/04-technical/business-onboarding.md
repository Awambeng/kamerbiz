# Business onboarding and store foundation

## Business account

A business is the tenant boundary for vendor data. Each business has a unique public slug and one or more memberships. The user who creates a business is assigned the OWNER role in the same database transaction.

## Business profile

The initial profile stores the business name, description, logo URL, phone, WhatsApp number, email, address, city, region and country. Logo upload is not part of this issue. The profile stores a URL placeholder until managed object storage is implemented.

## Public store address

The business slug is unique across KamerBiz and URL safe. It is generated from the business name during creation. A later profile edit may change the slug, provided it is not already in use. The slug is a public identifier, never an authorization credential.

## Authorization

Business access is resolved from the authenticated OIDC issuer and subject and the persisted business membership. Owners and managers can edit the business profile. Staff can view it. Browser supplied business identifiers never grant access by themselves.

## API

- `POST /api/v1/businesses` creates a business and owner membership.
- `GET /api/v1/businesses` lists the authenticated user's memberships.
- `GET /api/v1/businesses/{businessId}` returns a business profile to an authorized member.
- `PATCH /api/v1/businesses/{businessId}` updates a profile for an owner or manager.

## Data migration

Migration V3 adds the profile fields and unique slug to the existing business_account table. Existing rows receive a stable generated slug before the slug becomes mandatory.
