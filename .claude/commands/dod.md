---
description: Run the Definition of Done over the current changes
---

Run the checklist in [docs/ai/definition-of-done.md](../../docs/ai/definition-of-done.md).

Go through the items **one at a time** and give an honest verdict for each: passed, failed, or not
applicable. Run the checks for real, not from memory:

- `./mvnw -q verify`
- `./mvnw test`
- `./mvnw -pl app test -Dtest=ModularityTest`
- `java -jar tools/target/ai-tools.jar trace --docs-sync HEAD`

Then **re-read your own diff in full** (`git diff HEAD`) and say separately: what looks doubtful,
what was added "just in case", and what should be deleted.

Do not report "all done" if any item failed. List what is left and ask whether to fix it now.
