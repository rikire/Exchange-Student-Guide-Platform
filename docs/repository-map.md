# Repository map

How the repository is laid out: what lives where, who keeps it, and by what rules it changes. This
document answers "where do I put the thing I am writing right now".

Project overview: [README.md](../README.md). Rules for the AI agent: [CLAUDE.md](../CLAUDE.md).

## Directory map

| Path | What is there | Who keeps it |
|---|---|---|
| `app/src/main/java/in/ac/iitm/guide/` | The application: slices and `shared` | human + AI |
| `app/src/main/resources/templates/` | Thymeleaf templates | human + AI |
| `app/src/main/resources/db/migration/` | Flyway migrations | human + AI |
| `app/src/main/resources/data/seed/` | Starter articles, in the export format | human |
| `tools/` | `ai-tools.jar`: traceability, journal, hooks, weekly log | human + AI |
| `docs/requirements/` | Requirements and constraints — `FR`, `NFR`, `CON` | human |
| `docs/architecture/` | C4, data model, route contract, ADRs | human |
| `docs/features/` | Feature files `FEAT-XXX` | human + AI |
| `docs/cjm/` | The paths of the reader, the contributor and the moderator | human |
| `docs/roadmap/` | The roadmap: one file per phase, extended as work goes | human + AI |
| `docs/team/` | Member registry, ownership, weekly contribution logs | mixed, see below |
| `docs/stakeholder/` | The acknowledgement, meeting notes, feedback, acceptance criteria | human |
| `docs/course/` | The rubric and the documents submitted for grading | human + AI |
| `docs/verification/` | Fixtures and how they map to tests | human + AI |
| `docs/design/` | Design reference from the IITM sites, tokens, Figma link | human + AI |
| `docs/handoff/` | Install, run, admin guide, backup, contacts | human + AI |
| `docs/viva/` | Questions and answers per slice | human + AI |
| `docs/tech-debt.md` | The debt register `DEBT-XXX` | human + AI |
| `docs/ai/` | Instructions for the AI agent | human |
| `docs/ai/journal/` | Prompt journal | **hooks** |
| `.claude/` | Claude Code settings: hooks, slash commands | human |
| `.githooks/` | `commit-msg`, `pre-commit`, `pre-push` | human |
| `scripts/` | `check.sh`, `hooks.sh` — what CI runs, and hook installation | human + AI |

## What must not be edited by hand

| File | Written by | How to refresh |
|---|---|---|
| `docs/traceability.md` | `ai-tools trace` _(phase 2)_ | `java -jar tools/target/ai-tools.jar trace` |
| `docs/features/README.md` | `ai-tools trace` _(phase 2)_ | the same command |
| `docs/team/ownership.md` | `ai-tools ownership` _(phase 2)_ | `java -jar tools/target/ai-tools.jar ownership` |
| `docs/gap-list.md` | `ai-tools gaps` _(phase 3)_ | `java -jar tools/target/ai-tools.jar gaps` |
| `docs/diagrams/out/**` | PlantUML | the render script, which arrives in phase 1 with the diagrams |

Each carries a `GENERATED` marker in its header. A hand edit is detected and breaks the build —
deliberately: a generated file edited by hand creates false confidence that the state is current.

## Traceability

Requirements are connected to code by machine-checkable anchors, not by prose.

### The ID space

| Prefix | Meaning | Declared in |
|---|---|---|
| `FR-XXX` | Functional requirement | `docs/requirements/functional.md` |
| `NFR-XXX` | Non-functional requirement | `docs/requirements/non-functional.md` |
| `CON-XXX` | Constraint or deliberate simplification | `docs/requirements/constraints.md` |
| `DEBT-XXX` | Technical debt: done worse than it should be | `docs/tech-debt.md` |
| `FEAT-XXX` | Feature | `docs/features/FEAT-XXX-*.md` |
| `UC-XXX` | A step on a user journey | `docs/cjm/*.md` |
| `ADR-XXXX` | Architectural decision | `docs/architecture/adr/` |

### Declaring a requirement

```markdown
### FR-012 — Submitting a new article

**Status:** in-progress
**Priority:** must

A visitor submits a new article through a form; it enters the moderation queue and is not visible
until approved.
```

The fields `**Status:**`, `**Rationale:**` and `**Verified by:**` are read by the parser — their
shape must not change.

### Placing an anchor

| Artefact | Markup |
|---|---|
| Java code and tests | `//trace:FR-012` above the method or class |
| Deferred work | `// TODO(DEBT-007): ...` — `FIXME` and `HACK` are equivalent |
| Migration | `-- trace: FR-012` in the file header |
| Route contract | `trace: FR-012` in the route's row |
| Documentation | `<!-- trace: UC-003 -->` |
| Commit | `feat(FEAT-003): add article submission form [FR-012]` |

One anchor may reference several requirements: `//trace:FR-012, FR-013`.

### The feature file

The front matter is read by the generator, so the fields must reflect reality rather than intent:

```yaml
---
id: FEAT-003
title: Article submission
status: in-progress          # planned | in-progress | done | out-of-scope
covers: [FR-012, FR-013]
slice: contribute
routes: ["GET /contribute", "POST /contribute"]
tables: [articles, submissions]
code: [app/src/main/java/in/ac/iitm/guide/contribute/ContributeController.java]
tests: [app/src/test/java/in/ac/iitm/guide/contribute/ContributeControllerTest.java]
---
```

### Status rules

What is checked depends on the status. Without this, an empty scaffold would be red everywhere and
the validator would be switched off on day one.

| Status | What must exist |
|---|---|
| `planned` | Nothing. The requirement is declared, work has not started |
| `in-progress` | A feature file with `covers: [ID]` |
| `done` | A feature file, an anchor in code, and an anchor in a test |
| `out-of-scope` | A `**Rationale:**` line giving the reason |

`DEBT` entries have their own lifecycle — `open` and `resolved` — and their own mandatory fields:
**Cause**, **Consequence**, **How to fix**, **Trigger**. A marker left in the code while the status
is `resolved` is an error: either it was forgotten, or the status was set in advance.

For an `NFR` marked `done`, a `**Verified by:**` line may stand in for code and tests — not every
non-functional requirement is checked by a test.

For a `CON` the `**Rationale:**` line is always mandatory: a simplification without a reason is
indistinguishable from an omission.

## Commit convention

```
<type>(<scope>): <subject> [<requirements>]

feat(FEAT-003): add article submission form [FR-012]
fix(FEAT-003): reject a submission whose title is already taken [FR-014]
docs: describe the moderation state machine
chore(FEAT-000): bootstrap repository scaffolding
refactor(DEBT-007): replace the in-memory rate limiter
```

- Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`, `build`, `ci`, `style`, `revert`.
- For `feat` and `fix` a `FEAT-XXX` scope and a `[FR-XXX]` reference are **mandatory**: those are the
  commits that change behaviour, and the git history has to show which commit delivered which
  requirement. For the other types it would be ceremony.
- A commit paying off debt uses the scope `DEBT-XXX`.
- The header is at most 100 characters and does not end with a period.
- Checked by the `commit-msg` hook; run it by hand with
  `java -jar tools/target/ai-tools.jar commit-msg <file>`.

## Automation around the repository

| Mechanism | When it fires | What it does |
|---|---|---|
| `UserPromptSubmit` hook | A prompt is submitted | Delivers the sharpening rule, resolves the author, opens a journal entry, reports the human's own edits |
| `PreToolUse` hook (edits) | An edit is about to be written | Asks when the file is the human's; refuses a marker with no debt reference; refuses a disabled or sleeping test; asks about a test with no assertion, and about a **new** file under `shared/`, in the schema or security packages, or named like a wheel |
| `PreToolUse` hook (shell) | A command is about to run | Refuses the flags that skip checks; asks when a build is piped somewhere that hides its exit code |
| `PostToolUse` hook | _Not wired yet (phase 2)_ | Will say which document an edit obliges you to update |
| `Stop` hook | The assistant ends a turn | Refuses while the turn owes the journal an English rendering, or while the documentation check is red — once per cause either way, so a session cannot deadlock; then closes the journal entry and commits it |
| `commit-msg` | `git commit` | Checks the message convention |
| `pre-commit` | `git commit` | Instructions not mixed with code, Spotless, fast tests |
| `pre-push` | `git push` | The full `scripts/check.sh` |
| CI | Pull request | All of the above plus the full test suite |

Claude Code hooks are configured in [.claude/settings.json](../.claude/settings.json); git hooks are
installed with `scripts/hooks.sh`.

**The `Stop` hook makes a commit, and it is the only automation that writes to history.** A journal
entry is appended after the turn's last action, so the turn it describes can never be the turn that
commits it — which is how an entry sat outside the history until a person opened the file. The
commit always names the pathspec `docs/ai/journal`, so work staged elsewhere is left exactly where
it was, and it does nothing during a merge, a rebase or on a detached HEAD.
