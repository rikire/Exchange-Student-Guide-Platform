---
description: Record stakeholder feedback and propose what it changes
argument-hint: <what the stakeholder said>
---

Record stakeholder feedback: **$ARGUMENTS**

1. Append it to `docs/stakeholder/feedback.md` with the date and how it arrived (meeting, email,
   message). Record what was **said**, not your interpretation of it — those are separate lines.
2. Then work out what it becomes, and propose it:
   - a new or changed `FR` in `docs/requirements/functional.md`;
   - a changed acceptance criterion in `docs/stakeholder/acceptance.md`;
   - a `CON` if we are deciding not to do it;
   - nothing, if it is context rather than a request — say so plainly.
3. `docs/stakeholder/` and `docs/requirements/` are in the human's decision space: show the proposed
   edits and **wait** for agreement.

Never quietly reshape an acceptance criterion to match what we happened to build. If what we built
diverges from what was agreed, say that in as many words — the honest version is worth marks and the
tidied-up version is not.
