# Contributing to KamerBiz

Welcome! This guide is the entry point for anyone contributing to KamerBiz.

## Before you start
1. Read the [README](README.md), [product overview](docs/02-product/product-overview.md), [MVP scope](docs/02-product/mvp-scope.md), and [technical overview](docs/04-technical/technical-overview.md).
2. Read [development workflow](docs/05-delivery/development-workflow.md) and [security baseline](docs/04-technical/security-baseline.md).
3. Check open GitHub Issues and the project board. Do not start untracked work.

## Work item first
Every change must have a GitHub Issue before implementation. The issue should explain the problem, expected outcome, acceptance criteria, and relevant risks. Discuss unclear requirements before moving it to Ready.

## Branch and commit conventions
- Branch from the latest `master`: `feat/<issue>-short-description`, `fix/<issue>-short-description`, `docs/<issue>-short-description`, or `chore/<issue>-short-description`.
- Keep changes focused and reasonably small.
- Use Conventional Commits, e.g. `feat(web): add vendor sign-in page`.
- Never commit secrets, credentials, private customer data, or production exports.

## Pull requests
1. Link the issue with `Closes #<number>` where appropriate.
2. Use the pull request template and mark the PR ready only when review can begin.
3. Ensure required CI checks pass and request review.
4. Address feedback and keep the PR up to date.
5. Merge only after approval and required checks. Prefer squash merging.
6. An issue is Done only after its PR is merged and the acceptance criteria are met.

## Quality expectations
Add or update tests for changed behavior. Validate input at trust boundaries, enforce authorization on the server, and avoid logging secrets or sensitive personal/payment information. Update docs and ADRs when behavior or architecture changes.
