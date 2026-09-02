# The original plan

The course asks students to be able to explain how their code was produced. This is the plan the
repository was built from, recorded on **2 September 2026 before any code existed** and not edited
since. How it actually played out is visible in [the journal](journal/) and in
[docs/roadmap/](../roadmap/), which is the living document.

Where this plan turned out to be wrong, that is left standing rather than corrected. A plan quietly
rewritten to match the outcome teaches nothing.

## The starting brief

Set up the repository so that the process is legible: prompts and their results are recorded,
documentation cannot silently diverge from the code, requirements trace to code and tests, and the
artefacts the CS5013 rubric asks for exist as part of the work rather than as a write-up at the end.

The process scaffolding is adapted from an earlier repository of one of the team members
(a Go backend service), with the parts that do not transfer replaced rather than ported.

## Decisions taken before starting

| Question | Decision | Reason |
|---|---|---|
| Product shape | Community-editable knowledge base, not a fixed set of topic pages | Knowledge about hostel check-in and where to eat lives with students, not with OGE |
| Language | Java 17 | Course policy is Java; 17 is what is installed |
| Framework | Spring Boot 3.5.16, Thymeleaf, Bootstrap 5 | Mature line with settled documentation; Boot 4 exists but pulls a stack with far less written about it, and both of us must explain every line at the viva |
| Architecture | Vertical slices, Spring Modulith for the boundary check | Two people take tasks freely and work in parallel; the structure has to keep them out of the same files |
| Search | Hibernate Search 7.2.6 + Lucene | Aligns with Hibernate ORM 6.6, which is what Boot 3.5 ships |
| Persistence | H2 in development, PostgreSQL in production, Flyway migrations | Migrations are also where schema traceability anchors live |
| Deployment | Docker Compose handed over, with volumes for media and the Lucene index | The stakeholder does not need us to host it; a compose file is the deliverable |
| Documentation language | English throughout | The evaluator, the stakeholder and the end users all read English |
| Tooling | A Maven module producing `ai-tools.jar` | One language across the repository, and the tooling itself is defensible at the viva |
| Ownership | Measured from git history, not assigned in advance | We take tasks freely; the rubric still needs a named owner per module |
| Proposal | Not binding | It was written quickly to explain the idea; requirements are written properly here |

## How the four scaffolding requirements are met

**Prompt journal** — `ai-tools hook` on `UserPromptSubmit` and `Stop`. The part that matters:
edits the human makes by hand are detected by comparing SHA-256 snapshots of the working tree
between turns. That is the one thing the assistant cannot know on its own, because it does not
observe the file system between turns.

**Documentation that stays in step** — three levels: the rule in
[docs-sync.md](docs-sync.md), a `PostToolUse` hint at the moment of the edit, and a blocking gate at
the end of the turn. Only the route contract, the schema and the slice boundary block; a gate that
fires on every refactor gets bypassed and then protects nothing.

**Instructions for the AI** — nine documents in `docs/ai/`, with
[stop-and-ask.md](stop-and-ask.md) and [definition-of-done.md](definition-of-done.md) separated out
because they are consulted at different moments than the rest.

**Traceability** — one ID space and anchors in code, tests, migrations, the route contract and
commit messages. The [matrix](../traceability.md) is generated, not written: a handwritten matrix
diverges from the repository within days.

## The phases

| Phase | Content | Ends at |
|---|---|---|
| 0 | Scaffolding: build, gates, documentation skeleton, AI instructions | 6 Sep 2026 |
| 1 | Requirements, CJM, C4, ERD, ADRs, route contract, design reference | Design doc, 11 Sep 2026 |
| 2 | Walking skeleton, schema, export/import, the rest of the tooling | 20 Sep 2026 |
| 3 | The main flow: contribute, moderate, search, media, tags | Mid-demo, 9 Oct 2026 |
| 4 | Edge cases, security, performance, deployment, stakeholder meeting | 30 Oct 2026 |
| 5 | Handover, stakeholder-run demo, viva preparation | Final, 6 Nov 2026 |

Design is deliberately separated from implementation: decisions that are expensive to reverse are
made before code is written under them.

Article content is written in parallel from phase 0, because it does not depend on the code and
because an empty knowledge base at the mid-demo is the most likely way this project fails.

## Open questions at the start of phase 1

- The exact composition of the landing page beyond "pinned articles and files, plus search".
- Media quotas: the limit per file, per submission, and for the volume as a whole.
- Which of us writes which articles.
- Whether approved edits keep a full version history in the UI, or only accumulate the data for it.

## What was already known to be risky

- **The stakeholder is out of the loop until phase 4.** The next meeting happens when there is
  something to show, so requirements and acceptance criteria are written by us in the meantime and
  reconciled later.
- **Content is not code.** Thirty articles is real work that no amount of tooling shortens.
- **Uploads are the largest security surface** in a project that otherwise has almost none.
