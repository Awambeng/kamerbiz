# Technical overview

## Purpose

This document records a suggested technical direction. It is not a final design. Confirm the choices before building the production system.

## Suggested starting architecture

Build one application with clear internal sections. This is simpler to develop and operate than splitting the first version into many separate services.

| Area              | Suggested technology      | Why                                        |
| ----------------- | ------------------------- | ------------------------------------------ |
| Web interface     | React and TypeScript      | Build a responsive user interface          |
| Styling           | Tailwind CSS              | Keep visual styles consistent              |
| Backend           | Java and Spring Boot      | Handle business rules and data             |
| Database          | PostgreSQL                | Store business and order data              |
| Database changes  | Flyway                    | Track database updates                     |
| Login and access  | Spring Security           | Protect accounts and actions               |
| File storage      | S3-compatible storage     | Store product images and payment evidence  |
| API description   | OpenAPI                   | Explain how the frontend uses the backend  |
| Tests             | JUnit and Testcontainers  | Test business rules and database behaviour |
| Build and release | Docker and GitHub Actions | Build and check changes                    |

The final hosting provider and cost plan still need to be chosen.

## Main parts of the system

- Public storefront.
- Vendor dashboard.
- Platform administration.
- Backend API.
- Database.
- Private file storage.
- Email notifications.
- Background tasks where needed.

## Data separation

Many businesses will use the same platform. Each business's data must be separated.

Every private business record must belong to a business account. The backend must check access on every request. Hiding another business's data in the user interface is not enough.

## Security basics

- Store passwords safely using trusted security tools.
- Use secure sessions or tokens.
- Check access on the backend.
- Keep payment evidence private.
- Use hard-to-guess order tracking links.
- Limit what a tracking link reveals.
- Record important actions such as payment approval.
- Do not store payment PINs or bank login details.
- Keep secrets out of source code.
- Back up the database and test that backups can be restored.

## Payment handling

At first, customers pay the business directly. KamerBiz records the payment details and the business owner's review decision. KamerBiz does not hold customer funds.

Do not assume that automatic payment checks are available for every personal or merchant payment number. Research provider rules and access before planning this feature.

## Technical principles

- Start with a simple system.
- Avoid separate services unless there is a clear need.
- Keep business rules in the backend.
- Test important order and payment rules.
- Make the interface usable on mobile devices.
- Keep technical choices documented.
