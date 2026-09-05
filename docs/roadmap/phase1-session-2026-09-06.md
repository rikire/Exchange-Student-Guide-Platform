# Phase 1, FR-writing — session notes, 6 September 2026

Temporary handoff note — written so the session can resume tomorrow without re-deriving context.
Supersedes [phase1-session-2026-09-05.md](phase1-session-2026-09-05.md) (delete both once the
`FR id` column in the Feature coverage tracker has no empty cells, or fold what's still open
directly into [01-requirements-design.md](01-requirements-design.md)).

## What was done today

Restarted FR-writing from scratch (the human's explicit call, having disliked the first pass's loose
prose) rather than patch FR-001–008. Along the way:

- **Format settled**: EARS for the `FR` normative statement, GWT for its acceptance criteria — see
  [requirement-statements.md](../requirements/requirement-statements.md) and
  [acceptance-criteria.md](../requirements/acceptance-criteria.md). `NFR` stays a plain property
  statement with a separate Volere-style **Fit Criterion**, never an EARS "shall" — see the updated
  `## Format` in [non-functional.md](../requirements/non-functional.md).
- **Rule adopted**: prefer a glossary term over an `FR`-to-`FR` ID cross-reference (e.g. FR-002 says
  "renders as a red link," not "see FR-004") — an `FR` should be independently readable.
- **Rule adopted**: every "happy path" `FR` also gets checked for an unapproved-*and*-rejected
  submission scenario, not just "not yet approved" — the two are grammatically the same phrase but
  need separate GWT coverage.
- **13 FRs written** (FR-001 through FR-015, one gap — see below): reading, wiki-link
  render/write/red-link, a new feature discovered mid-review (red-link → article-creation prompt,
  FR-005, `could`), backlinks, search (now explicitly OR-matching with ranking, tags included),
  tag browsing, landing page (now also lists recently-added articles and a tag list — an old open
  question, resolved), submitting a new article, proposing an edit (both now handle title-collision
  against existing articles, another old open question — resolved), submission status lookup, abuse
  handling (**honeypot replaced with CAPTCHA** after research showed honeypots are increasingly
  bypassed by modern bots — updated in `docs/ai/security.md` and `docs/cjm/contributor.md` too),
  moderation queue (oldest-first order, decided today), reviewing a submission.
- Red-link rendering's priority raised from `could` to `must` (same priority as the wiki-link
  rendering it's part of).

## Known gap — pick this up before continuing the order below

**"Download a media attachment" (UC-004, `media`, should) has no `FR id` in the tracker.** It was
feature #8 in the agreed dependency order (between tag browsing and the landing page) and got missed
during the restart. Derive and write it before moving on, so the tracker's empty-cell check stays
meaningful.

## In progress — drafted, not yet confirmed or written

**FR-016 — Approving a submission** (UC-016, `moderate`, must). Draft on the table when the session
paused, split into new-article-submission vs. edit-submission approval (different outcomes: publish
a new article vs. update the existing one), plus a race-condition guard for a submission already
decided:

```
When a moderator approves a new-article submission, adjusting its tags if needed, the system shall
publish it as a new article.

When a moderator approves an edit submission, adjusting its tags if needed, the system shall update
the existing article with the proposed changes.

IF the submission has already been decided, THEN the system shall reject the approval attempt.
```

Present this again for confirmation before writing it — the human had not yet said yes when the
session paused.

## Remaining, in order, after FR-016

Download a media attachment (the gap above) should slot in before FR-009 conceptually, but can be
done now without disrupting anything already written — FR numbers are assigned as we go, not fixed
in advance.

1. Approve a submission (UC-016) — in progress, see above
2. Reject a submission (UC-017)
3. Version-history groundwork (UC-018)
4. Handle a reported article (UC-019)
5. Write and publish a new article directly, bypassing the queue (UC-020)
6. Edit and publish an article directly, bypassing the queue (UC-021)
7. Edit the homepage, including what's pinned (UC-022)
8. Report an article (UC-009) — the reader-facing half; `report` slice

## Where everything lives

| What | File |
|---|---|
| FR/NFR text | `docs/requirements/functional.md`, `non-functional.md` |
| EARS / GWT format docs | `docs/requirements/requirement-statements.md`, `acceptance-criteria.md` |
| Feature list, priority, slice, FR id | `docs/roadmap/01-requirements-design.md` — Feature coverage tracker |
| Abuse-handling decision (CAPTCHA, not honeypot) | `docs/ai/security.md` |
