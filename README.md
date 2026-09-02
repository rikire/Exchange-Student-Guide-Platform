# Exchange Student Guide Platform

A community-editable knowledge base for incoming exchange students at IIT Madras. Anyone can write
an article — how to register with FRRO, how hostel check-in works, where to find good biryani — and
every article and edit passes through a moderation queue owned by the Office of Global Engagement.

The problem it solves: the information exists, but it is scattered across the IITM site, WhatsApp
groups and travel blogs, and a student who arrived three days ago cannot find it.

CS5013 "Programming with AI" course project.
Stakeholder: Mr. Thukaram M Damodhar, Lead — International Academic Programs, OGE, IIT Madras.

## Status

Phase 0 — the repository scaffolding. No application features yet. See
[docs/roadmap/](docs/roadmap/).

## Getting started

Requires JDK 21. Maven is **not** needed and should not be installed: the wrapper pins 3.9.16,
so the build is identical on both our machines and in CI. A system-wide Maven would add a second
way to build with a possibly different version.

```sh
scripts/hooks.sh                    # build the tooling, install the git hooks — run this first
./mvnw verify                       # build, test, check formatting
./mvnw -pl app spring-boot:run      # http://localhost:8080
```

## Layout

| Path | What is there |
|---|---|
| `app/` | The Spring Boot application: vertical slices plus a small shared core |
| `tools/` | `ai-tools.jar` — traceability, prompt journal, contribution log |
| `docs/` | Requirements, architecture, roadmap, course deliverables |
| `docs/ai/` | Instructions for the AI agent working in this repository |

Full map: [docs/repository-map.md](docs/repository-map.md).

## How this repository is worked in

Both of us use an AI coding assistant. The repository is set up so that this is legible rather than
invisible:

- **Prompts, results and our own hand edits are recorded** in [docs/ai/journal/](docs/ai/journal/),
  written by hooks as the work happens.
- **Requirements trace to code and tests** through anchors, and the coverage matrix is generated
  rather than maintained by hand.
- **Documentation cannot silently drift** from the code: the route contract, the schema and the
  slice boundaries are gated.
- **Contribution is measured**, not declared: [docs/team/](docs/team/).

The rules the assistant works under are in [CLAUDE.md](CLAUDE.md) and [docs/ai/](docs/ai/).

## Team

| | |
|---|---|
| Mikhail Novikov | GE26Z858, [@rikire](https://github.com/rikire) |
| Abdirakhim Ismailov | GE26Z860, [@abdra04-gif](https://github.com/abdra04-gif) |

Contact: ge26z860@smail.iitm.ac.in

## Licence

[MIT](LICENSE).
