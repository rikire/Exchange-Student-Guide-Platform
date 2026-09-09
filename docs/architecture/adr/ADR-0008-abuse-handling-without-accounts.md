# ADR-0008 — Rate limit by IP, plus a challenge, and no honeypot

**Status:** accepted
**Decided:** 6 September — the honeypot research is recorded in `docs/ai/security.md` under that date
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

[CON-001](../../requirements/constraints.md) removes user accounts, and with them every abuse
control that depends on knowing who is acting. FR-013 has to hold anyway: anyone at all can post
text and a 200 MB video to a public form.

The thing that makes this urgent is an ordering property of the design. Moderation runs **after**
the submission is stored — the row is written and the file is on disk before any human sees it. So
moderation gates what gets *published*; it does nothing about what gets *stored*. A script posting
continuously fills the queue, fills the media volume against NFR-001's 20 GB, and buries the real
submissions the moderator is there to read. Every one of those is a denial of service that never
publishes a single word.

NFR-005 already sets the number: 5 submissions per IP per hour, configurable.

## Options

### A. Moderation alone

The queue is already a human gate, so arguably nothing else is needed.

It answers the wrong question, for the reason above: the cost is paid at storage, not at
publication. It also puts the whole burden on one or two OGE staff, whose attention is the scarcest
resource in the system — and the project's premise is to widen that bottleneck, not to feed it.

### B. A honeypot field

A hidden form field that a human never fills and a naive bot does. No dependency, no friction for
real contributors, no data leaves the site.

This was the cheap option and it was genuinely preferred until it was checked. Research during
phase 1 (6 September, recorded in [security.md](../../ai/security.md)) found that modern
form-filling bots detect and skip honeypot fields as a matter of course, so it no longer reliably
stops the thing it was chosen to stop. Keeping it would have been security theatre — a control whose
presence in a document is its only effect.

### C. Rate limiting by IP, plus a challenge on submission

Two controls addressing two different attackers. The rate limit bounds volume from any single
source, including a well-behaved script and an enthusiastic human. The challenge raises the cost of
the first request rather than the hundredth, which is what actually deters drive-by automation.

Costs: friction for genuine contributors, a third-party dependency if the challenge is hosted, and —
the real one — IP is a poor identity on a campus.

## Decision

**C** — rate limiting by IP at NFR-005's configured rate, plus a challenge on the submission form.
The honeypot is explicitly not implemented, so that its absence reads as a decision rather than an
omission.

The deciding factor between B and C is evidence: B's whole value was that it was cheap *and*
effective, and the second half did not survive being checked.

## What is deliberately still open

**Which challenge.** Two families, and the trade is not primarily technical:

- **Hosted** (the familiar widgets) — strong, maintained by someone else, and it sends every
  visitor's request to a third party. For a university office publishing a guide for international
  students, that is a privacy question for OGE to answer, not one for us to settle in an
  architecture record.
- **Self-hosted** (a proof-of-work or locally generated challenge) — no third party, no visitor data
  leaving the deployment, weaker against a determined attacker, and more of it is our code to
  maintain.

Either way it is a dependency, so it arrives with evidence rather than a name recalled here: that
the artefact and version exist, that it is maintained, and what its licence is. Named in this ADR
would be a version nobody verified.

**Where the rate limit is counted.** In-process is enough for a single-instance deployment and
loses its state on restart; a database-backed counter survives restarts and costs a write on every
submission. Both satisfy NFR-005; the choice belongs with the implementation, which is why the
requirement fixes the *rate* and not the mechanism.

## Consequences

**Good:** the volume a single source can store is bounded before moderation is involved, so the
moderator's attention is protected rather than spent; NFR-005's rate is an application setting, so
OGE can loosen it without a rebuild; refusing the honeypot keeps one fewer control that would have
been trusted without working.

**Bad:** the challenge is friction on the path the whole product depends on — a student with
something worth writing down meets an obstacle before they can write it, and some will not finish.
Worse, **IP is a bad identity here**: NFR-005's own fit criterion admits it, because a hostel or the
campus network can put hundreds of distinct people behind one address, so a rate low enough to stop
a script can block a floor of genuine contributors. The rate is configurable precisely because the
right number cannot be known before the system meets the real network, and finding it will take
observation after deployment rather than reasoning now.

**Reversal:** both controls are additive and sit in `shared/security`; removing either is a
configuration change rather than a redesign. If the challenge turns out to cost more contributions
than the abuse it prevents — which is measurable, by comparing submission rates before and after —
then dropping it and leaning on the rate limit alone is the amendment, and it should be made on that
evidence rather than on complaint volume.
