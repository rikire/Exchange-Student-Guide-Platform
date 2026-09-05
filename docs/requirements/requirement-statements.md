# Requirement statements: EARS

Every `FR`'s normative statement is written in EARS (Easy Approach to Requirements Syntax) — a small
set of sentence patterns that constrain a requirement to one clause order and one topic, so a
paragraph cannot quietly state the happy path, an exception, and a cross-reference all at once.

Developed by Alistair Mavin et al. at Rolls-Royce, first published at IEEE RE'09 (2009).

**This applies to `FR` only.** An `NFR` is a property of the system, not a response to a trigger —
it stays a plain, measurable statement (see the existing `NFR-003 — Search latency` example in
[non-functional.md](non-functional.md)), never an EARS "shall" sentence. Phrasing an NFR as "the
system shall reject X" smuggles a functional behaviour (validating and rejecting input) into a
non-functional document; that behaviour belongs in whichever `FR` triggers it, as its own
unwanted-behaviour clause.

## Patterns used here

- **Event-driven:** `When <trigger>, the system shall <response>.` — the normal case.
- **Unwanted behaviour:** `IF <undesired condition>, THEN the system shall <response>.` — an
  exception or rejection path. Almost every `FR` here pairs one event-driven sentence with one
  unwanted-behaviour sentence, because "what happens on success" and "what happens on the exception"
  are two different requirements, not one.
- **Ubiquitous:** a plain `The system shall <response>.`, for an `FR` constraint that holds all the
  time rather than in reaction to a trigger.

A requirement's `**Status:**`, `**Priority:**` fields, and its
[Given-When-Then acceptance criteria](acceptance-criteria.md), are unaffected by this — EARS
constrains the normative sentence only. EARS is the contract; the GWT scenarios beneath it are the
test cases derived from that contract.

## Why here

Kills the specific failure a free-text paragraph invites: a requirement that reads fine but turns
out, on close reading, to state more than one thing, or to leave the exception path implicit. If a
sentence does not fit one of the patterns above, it is not one requirement yet.
