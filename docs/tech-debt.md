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

### DEBT-003 — `shared` is a closed Modulith module and has to be an open one

**Status:** open
**Created:** 2026-09-10
**Marker:** `app/src/main/java/in/ac/iitm/guide/shared/package-info.java` — the javadoc, not a `TODO`:
there is no code yet to hang one on

**Cause:** the ten slices and `shared` were declared as Modulith modules on 10 September so the
design document could show a boundary the build enforces rather than a diagram someone drew. Modulith
detects a module from a `package-info.java` alone, with no annotation — which is why no dependency
was added. But without `@ApplicationModule(type = Type.OPEN)`, `shared` is **closed**, and a closed
module keeps its nested packages internal to itself.

**Consequence:** none today, because every one of these packages is empty. It bites in phase 2, at
the first real code: architecture-rules.md has each slice declaring its own Spring Data repository
over the JPA entities in `shared.persistence`, and a slice importing those types from a closed
`shared` is exactly what `ModularityTest` refuses. The failure will look like a boundary violation in
the slice being written, when the actual cause is a missing declaration on `shared` — which is the
kind of misdirection that costs an afternoon.

**How to fix:** add `spring-modulith-api` at compile scope (it is already managed by the Modulith BOM
in `app/pom.xml` and already present transitively at test scope, so this is a scope change rather
than a new artefact — but it is still a dependency decision and needs asking), then annotate
`shared` with `@ApplicationModule(type = ApplicationModule.Type.OPEN)`. The alternative is
`@NamedInterface` on `shared.persistence` and each slice naming it in `allowedDependencies`, which is
stricter and more work; that choice wants a paragraph in architecture-rules.md, not a silent pick.

**Trigger:** the first entity in `shared.persistence`, or the first slice repository — whichever
comes first in phase 2. Not before: annotating an empty package to prevent a failure that cannot
happen yet would mean adding a dependency for nothing.

### DEBT-002 — The process layer cannot be packaged for a second repository

**Status:** open
**Created:** 2026-09-06
**Marker:** none in code — this is an absence, recorded here so it is not rediscovered

**Cause:** the audit on 6 September set out to package `.claude/` and `tools/` as a Claude Code
plugin, which is the documented answer to "a second repository needs the same setup" — and this
scaffolding came from a template repository, so the second one already exists. Two things block it,
both structural rather than fiddly. The hooks run `tools/target/ai-tools.jar`, a Maven artefact of
*this* repository that `target/` keeps out of git, so an installed plugin has no jar to run and
fails the way described in `docs/ai/README.md`. And every skill links into `docs/ai/`, which would
not exist in the repository it was installed into, so the commands would load and then point at
nothing.

**Consequence:** the work does not travel. Each new repository re-derives the same rules by hand,
and they drift apart immediately — which is how this repository came to differ from the template it
started from. Nothing is broken today; the cost is paid the first time somebody wants this setup
somewhere else.

**How to fix:** decide what the portable unit actually is. Either the plugin bundles the documents
it links to and ships a built jar as a release asset, or the jar's rules move into the plugin as
scripts with no build step and the skills stop linking outward. The first keeps one source of truth
and adds a release process; the second is portable immediately and forks the rules. That is a design
decision, not an implementation detail, and it wants an ADR.

**Trigger:** the first time a second repository needs this, or phase 5 handover — whichever comes
first. Not before: a plugin that installs and then misbehaves is worse than none, for the same
reason a slash command that errors is worse than one that is absent.

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
