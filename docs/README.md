# Documentation

Map of the project documentation. How the repository is laid out:
[repository-map.md](repository-map.md).

## Requirements

| Document | About |
|---|---|
| [requirements/functional.md](requirements/functional.md) | What the system must do — `FR-XXX` |
| [requirements/non-functional.md](requirements/non-functional.md) | Properties of the system — `NFR-XXX` |
| [requirements/constraints.md](requirements/constraints.md) | Deliberate simplifications — `CON-XXX` |
| [requirements/glossary.md](requirements/glossary.md) | One vocabulary for the domain |
| [tech-debt.md](tech-debt.md) | Technical debt — `DEBT-XXX` |

## User journeys

| Document | About |
|---|---|
| [cjm/reader.md](cjm/reader.md) | The student who needs an answer and has three days of context |
| [cjm/contributor.md](cjm/contributor.md) | Someone who knows something worth writing down |
| [cjm/moderator.md](cjm/moderator.md) | The OGE staff member who approves what goes live |

## Architecture

| Document | About |
|---|---|
| [architecture/overview.md](architecture/overview.md) | C4 levels 1–3 and the slice map |
| [architecture/data-model.md](architecture/data-model.md) | Tables, relations, indexes and why |
| [architecture/ui-routes.md](architecture/ui-routes.md) | The route contract — the source of truth |
| [architecture/adr/](architecture/adr/) | Architectural decisions and their reasoning |

## Work and state

| Document | About |
|---|---|
| [features/README.md](features/README.md) | Feature backlog with statuses _(generated)_ |
| [traceability.md](traceability.md) | Requirement to code to test _(generated)_ |
| [gap-list.md](gap-list.md) | What is honestly not done _(generated)_ |
| [roadmap/](roadmap/) | The plan, one file per phase |
| [verification/](verification/) | Fixtures and how they map to tests |

## The course

| Document | About |
|---|---|
| [course/rubric.md](course/rubric.md) | All 25 marks and the evidence for each |
| [team/](team/) | Ownership, weekly contribution logs, member registry |
| [stakeholder/](stakeholder/) | What OGE actually said, and the acceptance criteria |
| [viva/](viva/) | Questions and answers per slice |
| [handoff/](handoff/) | Install, run, administer, back up |

## Working with the AI

| Document | About |
|---|---|
| [ai/README.md](ai/README.md) | Index of the agent's instructions |
| [ai/collaboration.md](ai/collaboration.md) | Who decides what, and how the agent talks |
| [ai/journal/](ai/journal/) | Prompts, results, and our own hand edits |
| [ai/PLAN-PROMPT.md](ai/PLAN-PROMPT.md) | The original plan, frozen |

## The principle

Documentation stays in step with the code by mechanism, not by discipline: some files are generated,
and for the rest a divergence breaks the build. Rules: [ai/docs-sync.md](ai/docs-sync.md).
