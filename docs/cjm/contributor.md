# Contributor journey

Someone who knows something worth writing down.

Agreed in phase 1, stage 1 — see the **Feature coverage tracker** in
[01-requirements-design.md](../roadmap/01-requirements-design.md). No accounts exist for this role
(`docs/requirements/constraints.md`); the submission number below is the only handle a contributor
has on their own submission afterward.

## Use cases

### UC-010 — Submit a new article

Writes and submits a new article, optionally attaching a photo, a document or a video and
suggesting tags. The submission enters the moderation queue, and the contributor is shown a
submission number.

### UC-011 — Propose an edit

Proposes an edit to an existing article, optionally attaching a photo, a document or a video and
suggesting tags. The proposal enters the moderation queue, and the contributor is shown a
submission number.

### UC-012 — Write a wiki link

Writes a `[[wiki link]]` to another article while composing a submission — including one to an
article that does not exist yet (a red link).

### UC-013 — Track a submission by its number

Looks up a submission's status (pending / approved / rejected) using the submission number shown at
UC-010 or UC-011.

## Not a use case, flagged during coverage

Abuse handling without accounts (rate limiting and a honeypot — already decided in
`docs/requirements/constraints.md`) protects UC-010/UC-011 but is not something the contributor
does, so it does not get a use-case entry of its own here.
