# AGENTS.md

This repository's instructions for an AI coding agent live in
[CLAUDE.md](CLAUDE.md), and the detail behind them in [docs/ai/](docs/ai/).

`AGENTS.md` is the cross-vendor filename; Claude Code reads `CLAUDE.md`. Rather than keep two copies
that drift, this file points at the one that is maintained. An agent that reads `AGENTS.md` and not
`CLAUDE.md` should read `CLAUDE.md` now.

**What such an agent should know before it does anything here:** most of the enforcement in this
repository is Claude Code hooks in `.claude/`, and none of it will run for you. The rules are still
the rules — they are just not being checked. In particular:

- Requirements, ADRs, the database schema, the route contract, the stakeholder record and the course
  documents are the human's decision. Propose and wait; do not edit them.
- A test comes before the code it tests, and must fail first.
- Changed behaviour and its documentation move in the same turn.
- Never bypass a check.
- Every deferred-work marker names a `DEBT-XXX` entry that exists in
  [docs/tech-debt.md](docs/tech-debt.md). A marker with no entry behind it is refused.
- Do not accumulate fallbacks: default a value once, at the boundary where it enters, and never add
  a branch for a case you cannot name.
- Instructions for the AI change in their own commit, never alongside code.
