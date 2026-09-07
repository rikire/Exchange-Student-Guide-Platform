# Acceptance criteria: Given-When-Then

Every `FR`'s acceptance criteria are written as Given-When-Then (GWT) scenarios — a semi-structured
way of stating a test case that reads as plain English and maps directly onto a test's name and
body.

- **Given** states the precondition: the state the system is in before the action.
- **When** states the action taken.
- **Then** states the observable outcome that action produces.

A clause with more than one condition chains them with **AND**, one per line.

Introduced by Dan North in 2006 as part of behaviour-driven development.

## Example

```
GIVEN a published article titled "FRRO"
WHEN a reader requests its page
THEN the response is 200
  AND the page shows the article's content
```

An `FR` with more than one scenario lists them one after another, each its own Given/When/Then
block.

## Unapproved and rejected are two scenarios, not one

Every happy-path `FR` that touches submissions gets criteria for **both** a submission that has not
yet been approved **and** one that was rejected. In English they collapse into the same phrase —
"not published" — which is exactly why they get written as one scenario and one test, leaving the
second path unverified. They are different states reached by different routes, and a bug can live in
either without touching the other.

Adopted 6 September while re-deriving `FR-001`–`FR-015`; `FR-007`'s four criteria are the worked
example.

## Why here

The same reason [docs/ai/workflow.md](../ai/workflow.md) asks for a red test before code: a
criterion that cannot be phrased this way cannot be turned into a test, and one that can is already
most of the way there.
