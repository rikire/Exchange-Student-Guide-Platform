# Instructions for the AI agent

Rules for working in this repository. The entry point is [CLAUDE.md](../../CLAUDE.md) in the root;
the details live here so that a session does not have to load everything into context.

| Document | About |
|---|---|
| [collaboration.md](collaboration.md) | Who decides what, how the agent disagrees, plans and takes feedback |
| [prompting.md](prompting.md) | Sharpening an underspecified request before acting on it |
| [workflow.md](workflow.md) | A feature from statement to commit, TDD, technical debt |
| [testing.md](testing.md) | What to assert, what to mock, and how to derive the corner cases |
| [stop-and-ask.md](stop-and-ask.md) | When stopping to ask is mandatory |
| [definition-of-done.md](definition-of-done.md) | Readiness checklist and the double check |
| [architecture-rules.md](architecture-rules.md) | Slices, their boundaries, dependency direction |
| [code-style.md](code-style.md) | Java and Spring conventions, errors, logging, tests |
| [security.md](security.md) | Security requirements for the code and the environment |
| [docs-sync.md](docs-sync.md) | How documentation stays in step with code |
| [roadmap.md](roadmap.md) | How the roadmap is kept and how items are closed |
| [journal/](journal/) | Prompt journal and the human's own edits |
| [instruction-backlog.md](instruction-backlog.md) | Changes to these instructions that the human has asked for but not yet adopted — a queue, not a rule |
| [audit-2026-09-06.md](audit-2026-09-06.md) | This layer audited against the platform: what was found, fixed, and deliberately not done |
| [audit-planning-2026-09-07.md](audit-planning-2026-09-07.md) | Whether the planning process is working, what it has produced, and five proposals awaiting a decision |
| [PLAN-PROMPT.md](PLAN-PROMPT.md) | The original plan this repository was built from |

**Five of these documents also exist as path-scoped rules** in `.claude/rules/`, carrying only their
load-bearing lines: `java-style`, `testing` and `architecture` on Java files, `security` on the
slices that touch uploads and templates, `schema` on migrations and the data model. Claude Code loads a rule when a
matching file is opened, so the subset is in context at the moment it applies rather than available
to be looked up afterwards. That is the whole point: rule 4 and the style rules were held by good
faith, and good faith fails by forgetting rather than by deciding.

**The documents here stay canonical.** A rule file is a pointer with an excerpt, never a second
source of truth, and one that contradicts its document is a defect no check can catch — `docs-check`
verifies that the link resolves, not that the excerpt is still faithful. When you change one of
these documents, read the matching rule file in the same turn.

## The six rules that outrank the rest

1. **Requirements and architecture are decided by the human.** Propose options and wait.
2. **A vague prompt is not an instruction.** Ask closed questions, suggest an answer to each, and
   wait. A default chosen quietly is still a target nobody picked.
3. **Unsure — stop and ask.** Say what you think; show alternatives, including simpler ones.
4. **Test before code.** A test that is green before the implementation checks nothing.
5. **Changed behaviour — update the documentation in the same turn.** Not "later".
6. **Nothing is lost:** a requirement traces to code and to a test; anything temporary is recorded
   in the debt register.

**What is actually enforced, as opposed to asked for:**

| Rule | How it holds |
|---|---|
| 1. The human decides | A hook asks before an edit lands in a protected file |
| 2. Sharpen a vague prompt | Delivered with every prompt by a hook — reinforced, not gated: no mechanism can judge whether a request was vague |
| 3. Stop and ask when unsure | Good faith. Nothing can measure confidence |
| 4. Test before code | Good faith. After the fact, a test written first is indistinguishable from one written second |
| 5. Documentation in the same turn | Partly: the turn cannot end while a document describes something the repository does not contain |
| 6. Nothing is lost | Partly: an edit adding a marker with no debt reference is refused. The traceability half arrives in phase 2 |
| The journal is in English | The turn cannot end while it owes a rendering, and the entry is committed when it is written |
| Do not reinvent what a library does | Partly: creating a file whose name suggests a wheel asks first. Whether the answer is honest is not mechanisable |
| The human takes part in domain, schema and security decisions | Partly: creating a file under `shared/`, in the schema or security packages, asks first |
| A switched-off or sleeping test | An edit adding `@Disabled` without a debt entry, or `Thread.sleep` under `src/test/`, is refused; a `@Test` that asserts nothing is questioned |
| The record survives a shortened conversation | A `PreCompact` hook writes into the entry that the context was compacted. It does not preserve what was discarded — it marks the gap as a gap |
| The work is not graded only by whoever did it | Partly: `/dod` hands the diff to the `dod-reviewer` subagent, which sees the change and the criteria without the reasoning that produced them. It is still the same model, and it is still started by the run it is auditing |
| A plan item says what would confirm it | A roadmap step with no `— check:` fails the documentation check. Naming a phase does not excuse it, unlike every other rule there — in a phase file all the work is still ahead, so that escape would switch the rule off |

**Every row above except one runs from `tools/target/ai-tools.jar`, and that jar is a build
artefact.** `target/` is not tracked, so on a fresh clone, after `mvnw clean`, or before anyone has
run `scripts/hooks.sh`, it does not exist — and a hook whose command exits non-zero is a
*non-blocking* error. The session then proceeds with no journal, no protected-path guard, no
bash-bypass refusal and no documentation gate, and this table would still be claiming all of it
held. That was true of this table for the whole of phase 0, and it was found by auditing the table
against the platform rather than against itself.

Two things changed on 6 September. A `SessionStart` hook now says so at the top of the session; it
is plain shell precisely so that it can run when the jar cannot, and it is a reporter, not a gate —
it can fail the same silent way if `sh` is not on PATH. And the two refusals that must not depend
on a build succeeding moved to `permissions.deny` in
[.claude/settings.json](../../.claude/settings.json), where the client enforces them and no process
of ours has to survive. That layer is coarser than the jar's rules — it matches on the command text
rather than on parsed segments — so both are kept: the jar refuses precisely, and the settings layer
refuses at all.

The exception matters more than the fix. **A mechanism that depends on a build is a mechanism with
an off switch nobody has to touch on purpose.**

Rule 6 was described here as backed by the matrix generator before that generator existed. It was
not, and the claim was corrected on 4 September — a document overstating its own enforcement is the
exact defect this repository keeps auditing itself for.

The journal row is the newest, and it was added for the same reason. The rendering rule had a
reminder and a record: the hook asked at the start of the turn, and wrote `NOT supplied` into the
entry at the end of it. Both fired correctly, and the rendering still did not appear — **watching a
rule break is not enforcing it.** The refusal is the difference.

**How much an "asks first" row is worth depends on the permission mode.** A hook can answer with
`deny` or with `ask`, and in permissive modes an `ask` is answered automatically — so every rule in
this table held by a question is weaker there than it looks, while the refusals still hold. Nothing
in the repository can change that; it is said here so that nobody presents the table at the viva as
more than it is.

## Slash commands

They live in `.claude/skills/<name>/SKILL.md`. They were `.claude/commands/*.md` until 6 September,
when they moved: skills are what Claude Code reads now, and commands are kept only for backward
compatibility. The move bought three things the older shape could not do — a command can inject the
output of a real command into its own prompt instead of asking the assistant to run one and report
back; a command that only reads can run in its own context instead of filling this one; and a
command with side effects can be hidden from the assistant so that only a person can start it.

The last column says which. **Person only** means the assistant cannot invoke it — those seven all
write into the record or into the human's decision space, and a command that files an ADR because it
inferred that one was wanted is worse than no command.

| Command | What it does | Who can start it |
|---|---|---|
| `/sharpen <text>` | Restates an underspecified request and names what it leaves open | either |
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

Three more — ownership, gap-list and viva-prep — arrive with their generators in phases 2 and 3. They
are listed only once they work: a command that errors is worse than one that is absent, because
it gets tried.

## Do the instructions actually change anything?

There are 100 tests over `tools/`, and they prove the *tooling* works: that a marker without a debt
reference is refused, that a journal entry is written, that a document naming a missing file fails
the build. **Not one of them shows that a single line in these documents changes what the assistant
does.** For a repository whose subject is working with an AI, that is the largest untested surface
in it, and it is the same defect the honesty table exists to catch — a rule believed to work because
it is written down.

`sharpen`, `dod` and `feature` now carry `evals/evals.json` beside them: a realistic prompt and a
list of verifiable statements the answer has to satisfy. They are graded from the transcript, and
the interesting number is the difference between the pass rate with the skill and without it — a
skill that changes nothing is a skill whose instructions were already obvious.

**They have been written and not yet run.** No pass rate is claimed anywhere, and none should be
quoted until the runs exist. Recording that here rather than leaving the file to imply otherwise is
the point of writing it down at all.

The technical debt register is [docs/tech-debt.md](../tech-debt.md).
