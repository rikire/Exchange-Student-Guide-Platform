# Moderator journey

The OGE staff member who approves what goes live.

Agreed in phase 1, stage 1 — see the **Feature coverage tracker** in
[01-requirements-design.md](../roadmap/01-requirements-design.md).

## Use cases

### UC-014 — View the queue

Sees every submission waiting for a decision, identified by its submission number.

### UC-015 — Read a submission

Reads a submission in full — text and any attachments (photo, document or video) — before deciding.

### UC-016 — Approve a submission

Approves a submission, adjusting or finalizing the tags the contributor suggested if needed. The
article publishes.

### UC-017 — Reject a submission

Rejects a submission.

### UC-018 — See retained article history

Sees that each approved revision of an article is retained (no diff view — already decided in
`docs/requirements/constraints.md`).

### UC-019 — Handle a reported article

Sees a report in the inbox (`reader.md`, UC-009) and acts on it however they see fit — usually
editing the article directly (UC-021) if it needs correcting — then closes the report (UC-025)
whenever they consider it done. Revised 7 Sep: no separate accept/reject decision on the report
itself; editing and closing are independent, existing actions.

### UC-020 — Write and publish an article directly

Writes a new article the same way a contributor does (UC-010: optional media, tags), but publishes
it immediately, bypassing the moderation queue.

### UC-021 — Edit and publish an article directly

Edits an existing article the same way a contributor proposes an edit (UC-011: optional media,
tags), but publishes the change immediately, bypassing the moderation queue.

### UC-022 — Edit the homepage

Edits the homepage, including which items are pinned there.

### UC-024 — Explain a rejection

When rejecting a submission, optionally gives a reason, so the contributor can see why. Added
7 Sep, alongside FR-018 — see the Feature coverage tracker.

### UC-025 — Close a report

Closes a report from the inbox at any time, whether or not the article was changed. Added 7 Sep,
alongside FR-021/FR-022 — see the Feature coverage tracker.

### UC-026 — Remove a published article

Takes a published article off the site: its route stops resolving, it leaves search results, tag
listings and the landing page, and links pointing at it turn red. Added 9 Sep, from the course's
scoping feedback of 28 August, which names removal alongside adding and editing — see
[scoping-feedback.md](../course/scoping-feedback.md) and FR-026.
