# ADR-0011 — The submission number is an unguessable token, not a sequence

**Status:** accepted
**Date:** 2026-09-10

## Context

[CON-001](../../requirements/constraints.md) removes user accounts, so a contributor has nothing to
look a submission up under except the number they were given. The glossary already says so, in
stronger terms than anyone had noticed: the submission number is "the only handle they have on it
afterwards" ([glossary.md](../../requirements/glossary.md), *Submission number*), and FR-012 shows the
status "to whoever holds the submission number".

That is the definition of a bearer capability. Holding the number is the entire authorisation.

Nothing had been decided about what the number *is*. The ERD types it `submission_number : text
<<unique>>` and stops there, and the screens sketched in stage 3 filled the gap with `SUB-04815`,
`SUB-04817`, `SUB-04819`, `SUB-04821`, `SUB-04822` — a counter, drawn because a counter is what
"number" suggests, not because anyone chose one.

**What forced the decision** was writing the route contract. `GET /submissions/{number}/confirmation`
answers `200` or `404` to anyone, which against a counter is an enumeration of every submission the
platform has ever received, including the unapproved and the rejected ones. That contradicts the
same document's own `404` rule — that a target which must not be revealed to exist returns `404` —
and it silently delivers FR-012, a `could`-priority feature the same document defers to phase 4.

The number is therefore a security property, and it was about to be settled by a placeholder in a
mockup.

## Options

### A. The number is a random high-entropy token

`SUB-K7M2-QX9P-4TVB` — a generated token in Crockford base32 (which drops `I`, `L`, `O` and `U`, the
characters people mistranscribe), 12 characters, 60 bits.

No new column: `submission_number` already exists and is already `text <<unique>>`, so this is a
statement about how the value is produced, not a schema change. The confirmation route stays public
and stays `200`/`404` exactly as drafted, because guessing is infeasible rather than merely
discouraged. FR-012's lookup, when phase 4 builds it, is safe without needing a gate of its own.

It is the rule [security.md](../security.md) already applies one section away, to media: "reachable
only by an unguessable id".

Costs legibility. Sixteen characters is more to type than five digits, more to read down a phone
line, and more to get right when a contributor emails OGE about a submission.

### B. Keep the sequence, and make the confirmation page unaddressable

`SUB-04822` survives; the confirmation is rendered from a POST-redirect session flash at
`GET /submit/confirmation`, so no public route is keyed on the number at all.

Cheapest change, and the number stays readable. But it protects the confirmation page by removing
it, not the number by making it safe — and the number is what the glossary says the contributor
keeps. FR-012 then arrives in phase 4 with the identical problem unsolved and no obvious answer,
because by then the numbers are already issued and already sequential. It also makes the
confirmation screen's own copy — "You can look up its status any time by entering this number" —
false for as long as FR-012 is deferred, which is a screen promising something the system has
decided not to do.

### C. A readable number for the moderator, a separate token for the contributor

`submission_number` stays `SUB-04822` and identifies the submission in the queue (FR-014); a second
`access_token` column is what the contributor is given and what appears in confirmation and lookup
URLs.

Gets both properties. Costs a column, and worse, splits one concept into two identifiers that mean
different things to different audiences — after which every contributor-facing screen and every
error message has to remember which one it is allowed to show. The glossary would need two entries
where it currently has one, and the first time someone shows the wrong one it is a silent leak.

## Decision

**A** — the submission number is a generated 60-bit token.

The deciding factor is that the glossary already treats the number as a bearer capability. A is the
only option that makes the documented behaviour true; B and C both leave a document describing a
property the value does not have. Between B and C, B defers the same decision to phase 4 under worse
conditions and C pays a column plus a permanent disclosure hazard to keep five digits readable —
neither is worth it when the requirement was already written down.

**What follows from A, rather than being a separate decision:**

- The token is generated from a cryptographically secure source, never from a counter, a timestamp,
  a hash of the content, or `Random`.
- The `SUB-` prefix and the grouping into fours are presentation only. They exist so a contributor
  can tell what the string is when they find it in a note; they carry no entropy and nothing parses
  them.
- Lookup is case-insensitive and ignores the hyphens, because a contributor retyping the value from
  a screenshot will get one of those wrong.
- The token is what the moderation queue shows (FR-014, "identified by its submission number").
  There is no second, prettier identifier — that is option C, and it was rejected.

The measurable half of this is [NFR-006](../../requirements/non-functional.md), so that "unguessable"
is a property a test can fail rather than an adjective in a document.

## Consequences

**Good:** `GET /submissions/{number}/confirmation` needs no gate, which matters because
[CON-001](../../requirements/constraints.md) leaves nothing to gate it *with* — there is no account
to check the number against. FR-012 becomes a phase-4 feature with no security question left in it.
The confirmation screen's "keep it somewhere safe" stops being advice about a value that was not
worth protecting.

**Bad:** the number is genuinely worse to handle. A contributor who mistypes one character gets a
`404` with no way to recover it, and there is no "resend my number" path, because there is no
address to send it to — CON-001 again. Anyone who loses the token has lost the submission; it will
still be moderated and still be published, but they cannot watch it happen. A sequence would have
let OGE find a submission for someone who half-remembered their number, and this rules that out.
Five sketched screens carried the old format and all of them had to change.

**Reversal:** cheap in the schema and expensive in the field. `submission_number` is `text` either
way, so switching to option C later is one added column and a migration — but every token already
issued stays valid and unguessable, so the two formats coexist indefinitely. The trigger to
reconsider is OGE reporting that contributors cannot communicate their numbers, which is the cost
named above showing up in practice rather than in this file.
