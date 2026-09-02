# Roadmap

Six phases, one file each. The plan is not a contract: items are added and changed as things become
visible that were not visible at the start of a phase.

The frozen counterpart is [docs/ai/PLAN-PROMPT.md](../ai/PLAN-PROMPT.md) — the original plan,
recorded before the work began and never edited. This one is the living document.

How to mark progress and close a phase: [docs/ai/roadmap.md](../ai/roadmap.md).

| Phase | File | Content | Ends at | Status |
|---|---|---|---|---|
| 0 | [00-init.md](00-init.md) | Build, gates, documentation skeleton, AI instructions | 6 Sep | in progress |
| 1 | [01-requirements-design.md](01-requirements-design.md) | Requirements, CJM, C4, ERD, ADRs, routes, design reference | **Design doc, 11 Sep** | not started |
| 2 | [02-skeleton.md](02-skeleton.md) | Walking skeleton, schema, export/import, rest of the tooling | 20 Sep | not started |
| 3 | [03-main-flow.md](03-main-flow.md) | Contribute, moderate, search, media, tags, content | **Mid-demo, 9 Oct** | not started |
| 4 | [04-hardening.md](04-hardening.md) | Edge cases, security, performance, deployment | 30 Oct | not started |
| 5 | [05-handover.md](05-handover.md) | Handover, stakeholder-run demo, viva preparation | **Final, 6 Nov** | not started |

Article content is written in parallel from phase 0. It does not depend on the code, and an empty
knowledge base at the mid-demo is the most likely way this project fails.

## How to read the checklists

- `[ ]` not started, `[~]` in progress, `[x]` done.
- **An item without a checkable result is not an item.** Every step names what confirms it is done.
- The **Open questions** section is not decorative: if there really are none, write that.
- A phase closes when its **Readiness criterion** is met, not when every box is ticked. The list of
  steps is incomplete by definition; the criterion is the reference point for the next phase.
