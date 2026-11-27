# Development workflow

## Delivery model
KamerBiz uses a lightweight Scrum-inspired process with a visible Kanban flow. We use short, time-boxed iterations (normally two weeks once the team is operating as a team), a prioritised product backlog, sprint planning, a daily async check-in when needed, review/demo, and a retrospective. Do not create ceremony for its own sake when only one contributor is active.

## Work item lifecycle
**Backlog → Ready → In Progress → Review → Done**

- **Backlog:** captured, not yet refined or committed.
- **Ready:** clear, prioritised, small enough to complete, and has acceptance criteria.
- **In Progress:** implementation has started; branch and issue are linked.
- **Review:** pull request is open, CI is running/passing, and review is requested.
- **Done:** PR is merged to `master`, acceptance criteria are satisfied, and any required docs are updated.

Blocked work stays visible with a comment explaining the blocker; do not silently leave it In Progress.

## Ticket rules
- Every planned change, bug, technical task, security improvement, or documentation task gets an Issue.
- No implementation begins without an Issue, except urgent incident response; create the issue retrospectively as soon as safe.
- Issues must include context, outcome, acceptance criteria, and testing notes.
- Split large initiatives into an Epic/parent issue and smaller deliverable issues. Avoid one issue for an entire release.
- Keep one primary owner per issue and limit work in progress.

## Definition of Ready
- User/problem and desired outcome are understandable.
- Scope and non-goals are stated.
- Acceptance criteria are testable.
- Dependencies and security/privacy implications are identified.
- Work is small enough for one iteration; otherwise split it.

## Definition of Done
- Acceptance criteria met.
- Automated tests added/updated and relevant checks pass.
- Code reviewed and approved by someone other than the author when a second contributor is available.
- Security/privacy implications considered; no secrets or sensitive data committed.
- Documentation and ADRs updated where necessary.
- PR merged; issue closed automatically or manually after merge.

## Pull request policy
Use a branch per issue and a PR into `master`. Link the issue, explain the change, list validation performed, describe security impact, and include UI screenshots when relevant. Do not merge with failing required checks. Prefer squash merge to keep history readable.

## Planning and prioritisation
Prioritise in this order: security/correctness foundations, core vendor and customer journeys, operational readiness, then enhancements. Estimate relatively (S/M/L) only when useful. Do not optimise velocity metrics or commit to sprint scope before capacity and dependencies are understood.

## Board
Use GitHub Projects with the lifecycle columns above. Add the KamerBiz repository as a project resource and ensure new issues are visible on the board. The board is the delivery view; GitHub Issues remain the source of truth for work details.
