---
name: trace-check
description: Check traceability and show the gaps in requirement coverage. Use when asked whether requirements are covered by code and tests, before closing a feature, or when preparing evidence that nothing has been lost.
context: fork
agent: Explore
---

Check the shared scope from [verification](../../../docs/ai/verification.md), including its base,
criteria and pre-existing changes. Structural traceability is not proof of behavioral coverage.

This runs in its own context because it reads widely and returns a short answer. The files it opens
are of no use to the conversation afterwards, and leaving them there crowds out the work itself.

```
java -jar tools/target/ai-tools.jar trace --check
```

It exits non-zero and lists every gap. If the jar is not built, report the executable check BLOCKED. Reading can diagnose these gaps but cannot turn that check into PASS:

1. Every requirement with status `done` in `docs/requirements/` has a feature file that covers it,
   an anchor `//trace:FR-XXX` in production code, and one in a test.
2. Every requirement with status `in-progress` has a feature file.
3. Every `CON` has a `**Rationale:**` line.
4. Every `TODO`, `FIXME` or `HACK` in the code references a `DEBT-XXX` that exists in
   `docs/tech-debt.md` and is still `open`.
5. Every feature file's front matter matches reality — the listed files exist and contain the
   anchors claimed.

Show the gaps as a list, worst first, and for each one say whether it is a missing test, a missing
anchor, or a status that is ahead of the work. Fix confirmed gaps only when this is part of an approved implementation contract; otherwise report them. Requirement or architecture changes need explicit human approval. Do not re-request permission for in-scope fixes.
