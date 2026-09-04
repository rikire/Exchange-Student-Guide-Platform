---
description: Run the Definition of Done over the current changes
---

Run the checklist in [docs/ai/definition-of-done.md](../../docs/ai/definition-of-done.md).

Go through the items **one at a time** and give an honest verdict for each: passed, failed, or not
applicable. Run the checks for real, not from memory:

- `./mvnw -q verify`
- `./mvnw test`
- `./mvnw -pl app test -Dtest=ModularityTest`

The traceability and documentation-sync generator lands in phase 2. Until it exists, do items 6
and 7 by reading, the way `/trace-check` describes, and say in the verdict that they were checked
by hand rather than by a tool. Do not run `ai-tools trace` (phase 2): it is not there, and reporting a
checklist item as passed on the strength of a command that failed is the exact failure this list
exists to prevent.

Then **re-read your own diff in full** (`git diff HEAD`) and say separately: what looks doubtful,
what was added "just in case", and what should be deleted.

Do not report "all done" if any item failed. List what is left and ask whether to fix it now.
