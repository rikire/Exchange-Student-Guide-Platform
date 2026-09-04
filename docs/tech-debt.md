# Technical debt

Anything done temporarily, worse than it should be, or as a workaround. The rule from
[docs/ai/workflow.md](ai/workflow.md): **nothing temporary stays unrecorded**, and the entry is made
the moment the debt appears — by the end of the task the context is gone and the entry comes out
useless.

Every marker in the code must reference an entry here: `// TODO(DEBT-007): ...`.

## Debt versus a deliberate constraint

These are different things and get different identifiers.

- **`DEBT-XXX`** — we know how it should be, and this is not it. It has a cost that grows.
- **`CON-XXX`** — we decided not to do it, and that decision is sound. It lives in
  `docs/requirements/constraints.md` and needs a `**Rationale:**`, not a fix.

Recording a scope decision as debt makes the register meaningless; recording real debt as a
constraint hides it.

## Register

### DEBT-001 — A journal translation is bound to a session, not to an entry

**Status:** resolved 2026-09-04
**Created:** 2026-09-03
**Marker:** was in `tools/.../HookCommand.java`, method `english`; removed when this was resolved

**Cause:** `hook english` writes the rendering into the state of the most recently touched session,
which is the right entry only because `hook prompt` normally opens one first. Nothing enforces that
ordering.

**Consequence:** if the rendering is supplied while no entry is open — the hooks not yet loaded, a
session file cleaned up, the agent calling it out of turn — it waits and attaches to the *next*
entry instead. The journal then shows a translation that does not belong to the prompt above it,
which is worse than no translation: it is evidence that quietly lies. Observed once during phase 0,
when `settings.json` had not yet been loaded by the running session.

**How to fix:** give each entry an id when `hook prompt` opens it, have `hook english` take that id
(or refuse when no entry is open), and drop a rendering that does not match the open entry.

**Trigger:** before the first stage where the journal is submitted as evidence — the design document
on 11 September. Until then the risk is only to our own records.

**Resolved 2026-09-04.** Without entry ids, which turned out to be unnecessary. `hook english` now
looks for the session that actually has an entry open — a prompt recorded and no outcome written
yet — and refuses when there is none, or when there is more than one. The second case is not
hypothetical: two of us worked at the same time on 4 September, and picking a session by
modification time could have written one person's rendering into the other's entry.

Found on the same day that seven entries were written with no rendering at all. The register entry
only described the misattribution, so the more common failure — simply forgetting — was invisible to
it. That one is now handled where it happens: a prompt containing Cyrillic makes the
`UserPromptSubmit` hook ask for a rendering in that turn, and the `Stop` hook records
"English rendering: NOT supplied" in the entry, so an omission leaves a trace instead of a gap.

<!--
### DEBT-XXX — Short title

**Status:** open
**Created:** YYYY-MM-DD
**Marker:** app/src/main/java/.../SomeClass.java

**Cause:** why it ended up this way.
**Consequence:** what this costs and who pays it — a slow page, a flaky test, a risk.
**How to fix:** the concrete change, not "redo this properly".
**Trigger:** what makes this urgent — a number of articles, a page load time, a stage of the course.
-->
