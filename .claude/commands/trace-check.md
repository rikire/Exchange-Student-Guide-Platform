---
description: Check traceability and show the gaps in requirement coverage
---

Check that nothing has fallen out of the chain "requirement to code to test".

```
java -jar tools/target/ai-tools.jar trace
```

If the generator is not built yet, do the check by reading:

1. Every requirement with status `done` in `docs/requirements/` has a feature file that covers it,
   an anchor `//trace:FR-XXX` in production code, and one in a test.
2. Every requirement with status `in-progress` has a feature file.
3. Every `CON` has a `**Rationale:**` line.
4. Every `TODO`, `FIXME` or `HACK` in the code references a `DEBT-XXX` that exists in
   `docs/tech-debt.md` and is still `open`.
5. Every feature file's front matter matches reality — the listed files exist and contain the
   anchors claimed.

Show the gaps as a list, worst first, and for each one say whether it is a missing test, a missing
anchor, or a status that is ahead of the work. Do not fix them silently; ask which to fix now.
