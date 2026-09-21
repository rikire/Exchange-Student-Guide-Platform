# Instructions for the AI agent

The entry point is [CLAUDE.md](../../CLAUDE.md); the detail lives here so that a session does not
load everything into context.

| Document | About |
|---|---|
| [collaboration.md](collaboration.md) | Who decides what, how the agent disagrees, plans and takes feedback |
| [prompting.md](prompting.md) | From request to confirmed contract |
| [workflow.md](workflow.md) | A feature from statement to commit, TDD, technical debt |
| [testing.md](testing.md) | What to assert, what to mock, how to derive the corner cases |
| [stop-and-ask.md](stop-and-ask.md) | Observable stop triggers and recovery |
| [definition-of-done.md](definition-of-done.md) | Readiness checklist and the double check |
| [architecture-rules.md](architecture-rules.md) | Slices, their boundaries, dependency direction |
| [code-style.md](code-style.md) | Java and Spring conventions, errors, logging, tests |
| [security.md](security.md) | Security requirements for the code and the environment |
| [docs-sync.md](docs-sync.md) | Keeping documentation in step with code, and how to write it |
| [roadmap.md](roadmap.md) | How the roadmap is kept and how items are closed |
| [journal/](journal/) | Prompt journal and the human's own edits |
| [instruction-backlog.md](instruction-backlog.md) | Requested changes to these instructions: a queue, not a rule |
| [audit-2026-09-21.md](audit-2026-09-21.md) | The AI process audited on a clean machine: findings, rule map, stage contracts, eval results |
| [audit-2026-09-06.md](audit-2026-09-06.md) | This layer audited against the platform |
| [audit-planning-2026-09-07.md](audit-planning-2026-09-07.md) | Planning, tracking and work distribution audited; five proposals, all decided |
| [PLAN-PROMPT.md](PLAN-PROMPT.md) | The original plan this repository was built from |

## How each document reaches the agent

Each document has a delivery mechanism; those held by memory say so instead of counting as
enforcement.

| Mechanism | When it delivers | Documents |
|---|---|---|
| **Gate** — the hook asks or refuses | at the action | `collaboration` (protected paths), `stop-and-ask`, `workflow` (the test-first order) |
| **Reminder** — `additionalContext` | at the action | `prompting` (every prompt), `docs-sync` (a tracked area changed), `roadmap` (the deadline, each session start) |
| **Path rule** — `.claude/rules/` | when a matching file is **read** | `code-style`, `testing`, `architecture-rules`, `collaboration` (its decision table, on the schema), `docs-sync` (how to write documentation, on any document) |
| **Subagent** — its own context | when invoked | `definition-of-done` (`dod-reviewer`), `testing` (`test-reviewer`) |
| **Memory** — nothing delivers it | never | what is left of `collaboration` and `workflow` |

A gate stops the action; a reminder can be read and ignored; prose is weaker than either.

- Path rules were checked by observation on 9 September: reading a `.java` file loads `java-style`
  and `architecture`, and the `paths:` frontmatter with a quoted YAML list works. A new rule file was
  not observed loading in the session that created it (`docs`, 21 September); whether a new session
  loads it is not yet checked.
- `security` fires on files under `shared/`, `media/`, `moderate/` and `contribute/` and on
  templates. Those packages hold only `package-info.java` so far.
- A rule file is a pointer with an excerpt, never a second source of truth. `docs-check` verifies that
  its link resolves, not that the excerpt is still faithful, so when you change one of these
  documents, read the matching rule file in the same turn.

## What is enforced, as opposed to asked for

Rules 1 to 6 are stated in [CLAUDE.md](../../CLAUDE.md).

| Rule | How it holds |
|---|---|
| 1. The human decides | A hook asks before an edit lands in a protected file |
| 2. Confirmed contract before implementation | Reminder with every prompt, by a hook — reinforced, not gated: no mechanism can judge whether a request was complete |
| 3. Stop on a trigger, and when unsure | Good faith. Nothing detects a refuted hypothesis or a repeated attempt |
| 4. Test before code | Creating a production class with no matching test asks first and names the expected file. The order cannot be proved afterwards, so it is asked at creation |
| 5. Documentation in the same turn | Partly: a `PostToolUse` hook names the document a change has put out of date, once per tracked area per session, and the turn cannot end while a document describes something the repository does not contain |
| 6. Nothing is lost | Partly: an edit adding a marker with no debt reference is refused. The traceability half arrives in phase 2 |
| The journal is in English | The turn cannot end while it owes a rendering; the entry is committed when it is written |
| Do not reinvent what a library does | Partly: creating a file whose name suggests a wheel asks first. Whether the answer is honest is not mechanisable |
| The human takes part in domain, schema and security decisions | Partly: creating a file under `shared/`, in the schema or security packages, asks first |
| A switched-off or sleeping test | An edit adding `@Disabled` without a debt entry, or `Thread.sleep` under `src/test/`, is refused; a `@Test` that asserts nothing is questioned |
| The record survives a shortened conversation | A `PreCompact` hook writes that the context was compacted. It marks the gap; it does not preserve what was discarded |
| The work is not graded only by whoever did it | Partly: `/dod` hands the diff to the `dod-reviewer` subagent, which sees the change and the criteria without the reasoning behind them. Same model, started by the run it audits |
| A stated number of requirements is true | A document whose count disagrees with the files fails the documentation check; `ai-tools count` prints the real figures and skips the sample entry under `## Format`. Dated records and blockquotes are left alone |
| A plan item says what would confirm it | A roadmap step with no `— check:` fails the documentation check. Naming a phase does not excuse it: in a phase file all the work is still ahead, so the escape would switch the rule off |

**Every row except the two `permissions.deny` refusals runs from `tools/target/ai-tools.jar`, a build
artefact.** On a fresh clone, after `mvnw clean`, or before `scripts/hooks.sh`, the jar does not
exist, and a hook whose command exits non-zero is a non-blocking error: the session proceeds with no
journal, guard, bypass refusal or documentation gate. Two mitigations:

- A `SessionStart` hook, plain shell so that it runs without the jar, says at the top of the session
  that the jar is missing or older than `tools/src`. It is a reporter, not a gate, and needs `sh` on
  PATH.
- The two refusals that must not depend on a build (`--no-verify`, `-DskipTests`) are
  `permissions.deny` in [.claude/settings.json](../../.claude/settings.json), where the client
  enforces them. They match command text, so they are coarser than the jar's rules; both layers stay.

A mechanism that depends on a build is a mechanism with an off switch nobody has to touch on purpose.
Watching a rule break is not enforcing it: the English-rendering rule had a reminder and a record and
still failed until the `Stop` hook refused.

A hook `ask` is answered automatically in permissive permission modes, so every row held by a
question is weaker there than it looks; refusals still hold. This is reported, not tested here, and
nothing in the repository can change it.

## Slash commands

They live in `.claude/skills/<name>/SKILL.md`. **Person only** means the assistant cannot invoke the
command: those seven write into the record or into the human's decision space, and a command that
files an ADR because it inferred one was wanted is worse than no command.

| Command | What it does | Who can start it |
|---|---|---|
| `/sharpen <text>` | States the contract for a request without acting on it | either |
| `/feature <description>` | Opens a feature file and links it to requirements | person only |
| `/adr <topic>` | Records an architectural decision | person only |
| `/sync-docs` | Brings documentation back in step with the changes | person only |
| `/trace-check` | Shows gaps in requirement coverage | either, own context |
| `/dod` | Runs the readiness checklist | either |
| `/journal-note <text>` | Adds a note to the journal | person only |
| `/weekly-log [--as member] <text>` | Adds a paragraph to this week's contribution log | person only |
| `/stakeholder-note <text>` | Records stakeholder feedback and proposes what it becomes | person only |
| `/course-check [stage]` | Checks the rubric for the current stage | either, own context |
| `/article <topic>` | Starts a seed article draft in the target format | person only |

Ownership, gap-list and viva-prep arrive with their generators in phases 2 and 3 and are listed once
they work: a command that errors is worse than one that is absent, because it gets tried.

## Do the instructions actually change anything?

The tests over `tools/` prove the tooling works; none shows that a line in these documents changes
what the assistant does. The evals in `sharpen`, `dod` and `feature` (`evals/evals.json`: a realistic
prompt and verifiable statements) exist for that.

Only the four contract-and-stop scenarios of `sharpen` have been run, before and after a rules change
and graded blind; [audit-2026-09-21.md](audit-2026-09-21.md) has the table and its limits. The earlier
scenarios of `sharpen` and those of `dod` and `feature` are written and unrun: no pass rate exists for
them and none should be quoted.

The technical debt register is [docs/tech-debt.md](../tech-debt.md).
