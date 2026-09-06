---
name: course-check
description: Check the current stage against the course rubric. Use when asked how many marks are defensible, what is missing before a deadline, or whether the proposal, design document, mid-demo or final submission is ready.
argument-hint: [proposal|design|mid-demo|final]
context: fork
agent: Explore
---

Check readiness for the stage: **$ARGUMENTS** (default: the next deadline that has not passed).

This runs in its own context: it follows every link in the rubric, and those files are evidence for
one answer rather than material for the conversation that asked.

1. Read [docs/course/rubric.md](../../../docs/course/rubric.md) and take the rows for this stage.
2. For each rubric criterion, find the artefact in the repository that would be shown as evidence.
   Follow the link and confirm it actually says what the criterion needs. A file that exists but is
   still a template counts as missing.
3. Report as a table: criterion, marks, evidence, verdict.
4. List separately what is missing and, for each item, the smallest concrete thing that would close
   it and roughly how long that takes.
5. Say plainly how many marks are currently defensible and how many are not.

Do not report a criterion as met on the strength of intent. The rubric is graded on artefacts, and
being wrong about this in September is cheap while being wrong in November is not.
