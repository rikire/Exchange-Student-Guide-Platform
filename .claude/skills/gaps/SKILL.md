---
name: gaps
description: Refresh and read the gap list — requirements not done, open technical debt, criteria with no test, and gaps in the traceability chain. Use when asked what is still missing, what is left before a stage, or when preparing the gap list for the mid-demo or the final.
---

Refresh the gap list and report what it says.

```
java -jar tools/target/ai-tools.jar gaps
```

It rewrites `docs/gap-list.md` from the requirements, the code anchors and `docs/tech-debt.md`. Read
the file and report, worst first:

1. Requirements not done, `must` before `should` before `could`, with the criteria that have no test.
   "No test, at least" is a lower bound: criteria carry no identifiers, so the real figure can only
   be higher.
2. Done requirements with fewer anchored tests than criteria.
3. Open technical debt, with each entry's trigger.
4. Any gap in the traceability chain.

Hiding a known gap is worse than declaring it: do not leave an item out because it is awkward. The
file is generated; do not edit it by hand.
