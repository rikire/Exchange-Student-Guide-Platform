---
description: Open a new feature — its file, its identifiers, its link to requirements
argument-hint: <short description of the feature>
---

Open a new feature: **$ARGUMENTS**

The order is mandatory; do not skip steps.

1. Read [docs/ai/workflow.md](../../docs/ai/workflow.md) and [docs/repository-map.md](../../docs/repository-map.md).
2. Find the next free `FEAT-XXX` by looking at the files in `docs/features/`.
3. Work out which requirements in `docs/requirements/functional.md` the feature covers.
   - If no suitable requirement exists, **write the requirement first**, not the feature.
   - If a requirement's wording is ambiguous, stop and ask, following
     [docs/ai/stop-and-ask.md](../../docs/ai/stop-and-ask.md). Do not fill the gap yourself.
4. Decide which slice this belongs to, and say why. If it does not fit any existing slice, that is a
   decision for the human — stop and ask rather than inventing a slice.
5. Create `docs/features/FEAT-XXX-<kebab-slug>.md` from
   [docs/features/_TEMPLATE.md](../../docs/features/_TEMPLATE.md). Fill in the front matter completely.
6. In the file, describe: the goal, the scenario, the routes involved, the schema impact, the
   acceptance criteria, and what is deliberately **out** of scope.
7. Show me the file and ask what needs clarifying before any code is written.

Do not write code at this step.
