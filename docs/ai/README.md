# Instructions for the AI agent

Rules for working in this repository. The entry point is [CLAUDE.md](../../CLAUDE.md) in the root;
the details live here so that a session does not have to load everything into context.

| Document | About |
|---|---|
| [collaboration.md](collaboration.md) | Who decides what, how the agent disagrees, plans and takes feedback |
| [prompting.md](prompting.md) | Sharpening an underspecified request before acting on it |
| [workflow.md](workflow.md) | A feature from statement to commit, TDD, technical debt |
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

Rules 1, 2 and 6 are backed by hooks and by the matrix generator. Rule 2 is reinforced rather than
gated: no hook can judge whether a request was vague, but the `UserPromptSubmit` hook delivers the
rule with every request, because an instruction read once at the start of a session loses to
everything that arrives afterwards. Rules 3 and 4 rest on good faith, and saying so plainly is
better than pretending otherwise.

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
| `/ownership` | Recomputes actual slice ownership and shows the balance |
| `/stakeholder-note <text>` | Records stakeholder feedback and proposes what it becomes |
| `/gap-list` | Regenerates the honest list of what is not done |
| `/course-check [stage]` | Checks the rubric for the current stage |
| `/viva-prep <slice>` | Prepares questions and answers about a slice |
| `/article <topic>` | Starts a seed article draft in the target format |

The technical debt register is [docs/tech-debt.md](../tech-debt.md).
