# Instructions for the AI agent

Rules for working in this repository. The entry point is [CLAUDE.md](../../CLAUDE.md) in the root;
the details live here so that a session does not have to load everything into context.

| Document | About |
|---|---|
| [collaboration.md](collaboration.md) | Who decides what, how the agent disagrees, plans and takes feedback |
| [prompting.md](prompting.md) | Sharpening an underspecified request before acting on it |
| [workflow.md](workflow.md) | A feature from statement to commit, TDD, technical debt |
| [testing.md](testing.md) | What to assert, what to mock, and the corner cases nobody invents |
| [stop-and-ask.md](stop-and-ask.md) | When stopping to ask is mandatory |
| [definition-of-done.md](definition-of-done.md) | Readiness checklist and the double check |
| [architecture-rules.md](architecture-rules.md) | Slices, their boundaries, dependency direction |
| [code-style.md](code-style.md) | Java and Spring conventions, errors, logging, tests |
| [security.md](security.md) | Security requirements for the code and the environment |
| [docs-sync.md](docs-sync.md) | How documentation stays in step with code |
| [roadmap.md](roadmap.md) | How the roadmap is kept and how items are closed |
| [journal/](journal/) | Prompt journal and the human's own edits |
| [PLAN-PROMPT.md](PLAN-PROMPT.md) | The original plan this repository was built from |

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

| Command | What it does |
|---|---|
| `/sharpen <text>` | Restates an underspecified request and names what it leaves open |
| `/feature <description>` | Opens a feature file and links it to requirements |
| `/adr <topic>` | Records an architectural decision |
| `/sync-docs` | Brings documentation back in step with the changes |
| `/trace-check` | Shows gaps in requirement coverage |
| `/dod` | Runs the readiness checklist |
| `/journal-note <text>` | Adds a note to the journal |
| `/weekly-log [--as member] <text>` | Adds a paragraph to this week's contribution log |
| `/stakeholder-note <text>` | Records stakeholder feedback and proposes what it becomes |
| `/course-check [stage]` | Checks the rubric for the current stage |
| `/article <topic>` | Starts a seed article draft in the target format |

Three more — ownership, gap-list and viva-prep — arrive with their generators in phases 2 and 3. They
are listed only once they work: a command that errors is worse than one that is absent, because
it gets tried.

The technical debt register is [docs/tech-debt.md](../tech-debt.md).
