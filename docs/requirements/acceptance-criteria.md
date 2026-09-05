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

## Why here

The same reason [docs/ai/workflow.md](../ai/workflow.md) asks for a red test before code: a
criterion that cannot be phrased this way cannot be turned into a test, and one that can is already
most of the way there.
