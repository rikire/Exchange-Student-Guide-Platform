# Phase 0 — Initialisation

**Status: done.** Closed 4 September 2026.

## Goal

Prepare the repository and the process so that the work that follows is traceable and protected from
documentation drifting away from the code — before the first line of feature code exists.

## Steps

- [x] Maven wrapper, since neither Maven nor make is installed locally
- [x] Multi-module build (`app` + `tools`); the Spring Boot application starts empty
- [x] `.editorconfig`, `.gitattributes`, `.dockerignore`
- [x] `tools/` module producing `ai-tools.jar`: `hook prompt|guard|stop|note`, `commit-msg`
- [x] `CLAUDE.md` and `docs/ai/` — the agent's instructions
- [x] `docs/repository-map.md` — ID space, anchors, commit convention
- [x] Documentation skeleton: requirements, cjm, architecture, features, adr, roadmap, team,
      stakeholder, course, design, handoff, verification, viva
- [x] `docs/course/rubric.md` — all 25 marks mapped to evidence
- [x] Claude Code hooks and slash commands
- [x] Git hooks and `scripts/`
- [x] CI, pull request and issue templates
- [x] `.env.example`, `README.md`, `docker-compose.yml` placeholder
- [x] `docs/team/members.yml` completed — Abdirakhim's git email (`abdraoper@gmail.com`) is recorded
- [x] ~~Article format fixed and the first three to five drafts written~~ — moved to phase 1, see
      below

## Readiness criterion

`scripts/check.sh` passes on the empty scaffold; `scripts/hooks.sh` installs the git hooks; the
prompt journal writes non-empty entries — verified by hand, not assumed.

## Changes to the original plan

[docs/ai/PLAN-PROMPT.md](../ai/PLAN-PROMPT.md) is frozen, so deviations are recorded here instead.

- **Java 17 to Java 21** (3 Sep). The plan settled on 17 only because it was the JDK that happened to
  be installed. Temurin 21 is now installed and is the default, which also removes a smaller problem:
  the local JDK was GraalVM while CI uses Temurin, and that mismatch is a class of "works on my
  machine" waiting to happen. Nothing in the code needs a 21-only feature; this is alignment, not
  capability.
  **Consequence for the team:** Abdirakhim needs Temurin 21 as well before his build will work.
- **Maven is deliberately not installed.** The wrapper pins 3.9.16, which is what makes the build
  identical on both machines and in CI. A system-wide Maven would be a second way to build, at a
  possibly different version.
- **The prompt journal is written in English** (3 Sep). Our conversation is in Russian and the
  evaluator reads English, so `ai-tools hook english` takes the rendering from the agent during the
  turn. The original prompt is kept next to the translation: it is the artefact, and the translation
  is an interpretation of it.
- **A sixth rule was added: sharpen a vague prompt before acting on it** (3 Sep). New document
  [docs/ai/prompting.md](../ai/prompting.md) and the `/sharpen` command. It is deliberately narrow —
  restating an unambiguous instruction is noise, and a rule that produces noise gets skipped when it
  matters. `stop-and-ask.md` was amended at the same time so the two do not contradict each other:
  an ambiguous *request* is sharpened and carried on with, while an ambiguous *written requirement*
  still stops.
- **`.vscode/settings.json` is committed** (3 Sep). User-level VS Code settings on one machine
  pinned JDK 17 into the integrated terminal's environment, which would make every terminal build
  fail against `release 21`. The workspace file cancels that override and puts `JAVA_HOME/bin`
  first on PATH. It deliberately contains no machine-specific paths, so it works for both of us;
  `.gitignore` now excludes `.vscode/*` but keeps `settings.json`.
- **"Article format fixed and the first three to five drafts written" moved to phase 1** (4 Sep).
  The seed front-matter shape (title, tags, author, dates) is not process/tooling — it is a first
  cut at what an article *is*, which overlaps directly with phase 1's own glossary and ERD work
  ([01-requirements-design.md](01-requirements-design.md)). Deciding it alone here, ahead of that
  joint work, risked a shape phase 1 would immediately revise. Phase 1 already plans "10 or more
  articles drafted" as its own content step; the moved item is folded into that rather than kept as
  a separate quota under a schema likely to be superseded within days.

## Problems found after the first push

- **The executable bit never reached git** (3 Sep). We develop on Windows, where
  `core.filemode` is false, so a local `chmod +x` is not recorded in the index. CI failed on
  `./mvnw: Permission denied`, which was the harmless half of the problem: `.githooks/*` lost the
  bit too, and on Linux or macOS a hook without it is not an error — it is silently skipped, so the
  commit-message gate would simply have stopped existing for whoever cloned the repository.
  Fixed with `git update-index --chmod=+x`, and `scripts/check.sh` now refuses to pass while any of
  those six files is not `100755`. CI calls that script through `sh` so the check still runs when
  the bit is the thing that is broken.

## Audit of 4 September, and what it found

The phase was walked against its own criteria before being closed. `scripts/check.sh` was green and
CI had passed four runs, and eight divergences were still there — every one of them documentation
claiming something the code did not do.

- `CLAUDE.md` advertised three slash commands that did not exist.
- `/dod` told the agent to run `ai-tools trace` (phase 2), which does not exist and has no fallback, so the
  readiness checklist itself failed halfway.
- The `PostToolUse` hook was listed as active automation. It is not wired.
- Four links in `docs/course/rubric.md` — the document an evaluator reads — pointed at missing files.
- `scripts/diagrams.sh` was named as the way to refresh the diagrams; it arrives in phase 1 with them.
- `docs/team/members.yml` carried a bare `TODO` with no debt reference, which `CLAUDE.md` forbids.
- Four methods in `tools/` were written for phase 2 and never called.
- Six directories the map describes are absent from a fresh clone: git does not track empty ones.

All eight are fixed. The dead code was deleted rather than parked behind a debt entry: the rule in
[definition-of-done.md](../ai/definition-of-done.md) already says code written for later is deleted,
and proposing an exception to it was the wrong instinct.

**The lesson is about the shape of the failure, not the eight items.** Every one lived in the gap
between a document and the thing it described, and every one was invisible to a green build. The
mechanisms that would have caught them — a link checker and the traceability gate — are exactly the
ones deferred to phase 2, so phase 0 documented a discipline it could not yet enforce on itself.
`scripts/check.sh` now runs a link check for this reason.

## Verified

All of the following was run, not assumed. Dates are when the check actually passed.

- `scripts/check.sh` — 19 tests green across both modules, Spotless enforced (2 Sep).
- Spring Boot 3.5.16 and Spring Modulith 1.4.13 resolve and run together. This was the version risk
  flagged in the plan, and it is now closed (2 Sep).
- `ai-tools commit-msg` accepts a conforming message and exits 1 on a non-conforming one (2 Sep).
- The `guard` hook asks on `docs/requirements/functional.md` and stays silent on an ordinary source
  file (2 Sep).
- The journal writes a non-empty entry containing the prompt, the outcome and the checks (2 Sep).
- **Detection of edits made by hand works**: a modified file and a deleted file between two turns
  were both reported. This is the one thing the assistant cannot learn on its own, so it was worth
  testing rather than trusting (2 Sep).
- `scripts/hooks.sh` builds the jar and sets `core.hooksPath` to `.githooks` (2 Sep).
- Clean build on Temurin 21 in a fresh shell with no environment overrides — 25 tests green
  (3 Sep).
- The journal keeps the original prompt and adds the English rendering beside it, and a translation
  does not leak into the next entry (3 Sep, `JournalTest`).
- `scripts/check.sh` reconfirmed green — including `docs-check` and the link check — after closing
  the phase's last two items: `docs/team/members.yml` verified to already carry Abdirakhim's email,
  and the article-format item moved to phase 1 (4 Sep).

## Open questions

1. Whether to keep the `PostToolUse` documentation reminder out of phase 0. It is currently not
   wired: the mapping table it would enforce points at documents that do not exist yet, so it would
   fire on everything. Planned for phase 2 with the rest of the gate.
