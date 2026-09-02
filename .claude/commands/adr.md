---
description: Record an architectural decision as an ADR
argument-hint: <topic of the decision>
---

Record an architectural decision: **$ARGUMENTS**

1. Check that this really is an ADR. The test: the decision would be expensive to reverse, or it has
   a non-obvious alternative. The moderation state machine is an ADR; a package name is not.
   If it is not an ADR, say so and stop.
2. Find the next free number in `docs/architecture/adr/`.
3. Create the file from [docs/architecture/adr/_TEMPLATE.md](../../docs/architecture/adr/_TEMPLATE.md).
4. Fill in, honestly:
   - **Context** — what forced the decision, with the constraint that actually binds.
   - **Options** — at least two, each with its cost. An ADR with one option is a rationalisation.
   - **Decision** and **Consequences**, including the bad ones.
5. `docs/architecture/adr/` is in the human's decision space: show me the draft and **wait** for
   agreement before writing the file.
