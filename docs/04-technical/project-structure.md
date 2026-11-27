# Project structure and module conventions

## Repository layout

- `apps/web`: React + TypeScript user interface.
- `services/api`: Spring Boot API, organised as a modular monolith.
- `docs`: product, technical, delivery and decision records.
- `.github`: contribution templates, CI and dependency automation.

## Backend conventions

Keep code inside `cm.kamerbiz`. Organise future business capabilities by domain/module (for example `catalog`, `orders`, `vendors`, `agents`). Each module should own its application services and persistence. Avoid reaching into another module's repositories; expose an application-level interface instead. Shared platform concerns belong under `platform` and should remain small.

Use DTOs at API boundaries, Jakarta Bean Validation for request validation, explicit transaction boundaries, Flyway for all schema changes, and `ddl-auto: validate` rather than Hibernate schema generation. Never use entity objects as public API responses.

## Frontend conventions

Keep feature-specific UI and API code grouped by capability as the application grows. Use strict TypeScript, accessible semantic HTML, responsive layouts, and respect `prefers-reduced-motion`. Do not place secrets or privileged authorization decisions in the browser.

## API conventions

Version public routes under `/api/v1`. Use consistent error responses, validate all untrusted input, and enforce authorization in the backend. Health endpoints expose only minimal status information.

## Configuration

Configuration is externalised through environment variables. Safe local placeholders may be documented, but real credentials must never be committed. Production profiles, origins, secrets, storage, and deployment settings must be explicitly configured before deployment.
