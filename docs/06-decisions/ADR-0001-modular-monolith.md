# ADR-0001: Start with a modular monolith

- **Status:** Accepted
- **Date:** 2026-11-27
- **Decision owners:** KamerBiz project
- **Related issue/PR:** #1

## Context
KamerBiz is an early-stage product with a small delivery team. The first release needs clear domain boundaries without the operational overhead of distributed services.

## Decision
Build the backend as one Spring Boot application organised into explicit modules by business capability. Keep the web frontend as a separate application. Use PostgreSQL as the primary relational database. Do not introduce microservices, Kafka, or Kubernetes for the MVP without a demonstrated need.

## Alternatives considered
- Microservices from the beginning — independent deployment, but adds infrastructure, network failure modes, and operational cost before scale requires it.
- Single tightly coupled application — simple deployment, but risks poor boundaries; modular package/module boundaries are required.

## Consequences
### Positive
- Easier local development, testing, deployment, and onboarding.
- Domain boundaries can be extracted later if real scaling or team needs justify it.

### Negative / trade-offs
- Modules share a deployment lifecycle and database; boundaries must be protected through code structure and tests.

## Security and privacy considerations
Centralise authentication and authorization policies, enforce tenant isolation at every module boundary, and avoid direct cross-module access to another module's persistence internals.

## Follow-up
Establish module conventions and architecture tests as the backend grows.
