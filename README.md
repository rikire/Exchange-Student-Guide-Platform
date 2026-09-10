# Exchange Student Guide Platform

A community-editable knowledge base for incoming exchange students at IIT Madras. Anyone can write
an article — how to register with FRRO, how hostel check-in works, where to find good biryani — and
every article and edit passes through a moderation queue owned by the Office of Global Engagement.

The problem it solves: the information exists, but it is scattered across the IITM site, WhatsApp
groups and travel blogs, and a student who arrived three days ago cannot find it.

CS5013 "Programming with AI" course project.
Stakeholder: Mr. Thukaram M Damodhar, Lead — International Academic Programs, OGE, IIT Madras.

## Status

**Phase 1, requirements and design, ending 11 September.** 16 of its 17 steps are done; the ten seed
article drafts are the one still open. See [docs/roadmap/](docs/roadmap/).

There is still **no application code beyond the skeleton**, and saying so here is more useful than
letting the volume of `docs/` imply otherwise. What exists is the specification: 26 functional
requirements, 6 non-functional, 7 constraints, 26 use cases, 11 architecture decision records, an
ERD, C4 levels 1 to 3, a route contract covering all 12 `must` features, and 15 screens sketched as
plain HTML. What does not: any slice, any entity, any migration, any template.

The ten slice packages are declared and their boundary is verified by `ModularityTest`, but they hold
no code. Phase 2 builds the first vertical slice end to end.

**Delivered to the course so far:** the proposal (revision 2, 9 September) and the design document
([docs/course/design-doc.pdf](docs/course/design-doc.pdf), 11 September). Next is the mid-demo on
9 October.

## Getting started

**Setting up for the first time: [docs/onboarding.md](docs/onboarding.md).** Open the repository
with an AI assistant and say "work through docs/onboarding.md" — it checks what is missing,
asks before installing anything, and finishes on a criterion rather than on a checklist. Works by
hand too, on macOS, Windows and Linux.

Requires JDK 21. Maven is **not** needed and should not be installed: the wrapper pins 3.9.16,
so the build is identical on both our machines and in CI. A system-wide Maven would add a second
way to build with a possibly different version.

```sh
scripts/hooks.sh                    # build the tooling, install the git hooks — run this first
./mvnw verify                       # build, test, check formatting
./mvnw -pl app spring-boot:run      # http://localhost:8080
scripts/check.sh                    # everything CI runs, before you push
scripts/diagrams.sh                 # re-render the C4 diagrams and the ERD
scripts/contribution.sh             # who wrote what, per ISO week
```

`scripts/hooks.sh` is not optional. Most of what the section below describes runs from
`tools/target/ai-tools.jar`, and a missing jar switches all of it off without failing anything.

## Layout

| Path | What is there |
|---|---|
| `app/` | The Spring Boot application: ten declared slice packages plus `shared`, all empty so far |
| `tools/` | `ai-tools.jar` — the prompt journal, the commit-message gate, the documentation check |
| `docs/requirements/` | What the system must do: `FR`, `NFR`, `CON`, the glossary, acceptance criteria |
| `docs/architecture/` | ADRs, the C4 diagrams, the data model, the route contract, the security rules |
| `docs/design/` | 15 screens as plain HTML, and the canvases they came from |
| `docs/roadmap/` | Six phases, each a checklist whose items carry a check that can fail |
| `docs/course/` | The proposal and the design document, with the rubric each is graded against |
| `docs/ai/` | Instructions for the AI agent, and the journal of every prompt given to it |

Full map: [docs/repository-map.md](docs/repository-map.md).

## How this repository is worked in

Both of us use an AI coding assistant. The repository is set up so that this is legible rather than
invisible. Each line below says what runs today, because a mechanism that is planned and a mechanism
that fires are different things, and only one of them protects anything.

- **Prompts, results and our own hand edits are recorded** in [docs/ai/journal/](docs/ai/journal/),
  written by hooks as the work happens and committed automatically. *Runs today.* A prompt written in
  Russian will not let the turn end until an English rendering is supplied, since the journal is
  evidence for an English-speaking reader.
- **Decisions are recorded when they are taken**, in [docs/architecture/adr/](docs/architecture/adr/),
  each with the options genuinely weighed. *Runs today*, by habit rather than by gate. The register
  says openly that ADR-0002 to ADR-0008 were written after the fact, and why that is weaker.
- **The slice boundary is a test.** `ModularityTest` verifies the ten declared modules, and carries a
  second assertion so that it cannot pass on an application with no modules at all, which is what it
  had been quietly doing. *Runs today.*
- **Commit messages and unrecorded shortcuts are gated.** A message outside the convention is
  refused, and so is a parked-work comment with nothing in [docs/tech-debt.md](docs/tech-debt.md)
  behind it. *Runs today.*
- **Documentation is checked against the repository.** `scripts/check.sh` fails when a document names
  a file that does not exist. *Runs today.* The narrower gates on the route contract, the schema and
  the slice boundary are a `PostToolUse` reminder now and become blocking in phase 2, which
  [docs/ai/docs-sync.md](docs/ai/docs-sync.md) states rather than implies.
- **Requirements trace to code and tests** through anchors in the code. *Phase 2* — the generator
  does not exist yet, and [docs/traceability.md](docs/traceability.md) is a placeholder until it
  does.
- **Contribution is measured, not declared.** `scripts/contribution.sh` reports authorship per ISO
  week with the journal hook's own commits separated out, because `git shortlog` on this repository
  says the opposite of the truth. *Runs today.* The generated ownership table is phase 2.

The rules the assistant works under are in [CLAUDE.md](CLAUDE.md) and [docs/ai/](docs/ai/). That
layer is itself under review: [docs/ai/instruction-backlog.md](docs/ai/instruction-backlog.md) IB-003
asks why 2,375 lines of instructions produced so little compliance, and answers that most of them
have no moment at which they arrive.

## Team

| | |
|---|---|
| Mikhail Novikov | GE26Z858, [@rikire](https://github.com/rikire) |
| Abdirakhim Ismailov | GE26Z860, [@abdra04-gif](https://github.com/abdra04-gif) |

Contact: ge26z860@smail.iitm.ac.in

## Licence

[MIT](LICENSE).
