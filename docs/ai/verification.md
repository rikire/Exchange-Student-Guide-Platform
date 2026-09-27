# Review scope and verification evidence

## One subject for every procedure

At contract approval, record the base commit, acceptance criteria, authorized paths and pre-existing
changes in the task state. Use the PR merge base when reviewing a branch; for a new local task use
its starting HEAD and record the pre-existing diff separately. Resolve a named base once; do not
silently replace a missing base with HEAD or an empty diff. If the original base was not recorded,
inspect history and state the justified base and its limits before reviewing.

Run `java -jar tools/target/ai-tools.jar review-scope <base>` and retain its JSON outside the repository
or in an ignored task-state directory. It includes committed, staged, unstaged and untracked paths,
and deletions. The fingerprint includes HEAD, base, index, and tracked/unignored file contents and
executable flags; it excludes ignored build outputs. It does not certify the environment or behavior.
Submodules and unsupported filesystem entries fail explicitly. Do not expose secret contents in reports.

Pass the same base, inventory, criteria, contract confirmation and pre-existing-change exclusions to
`dod`, `sync-docs`, `trace-check`, `test-reviewer` and `dod-reviewer`. Read `git diff <base> HEAD --`,
`git diff --cached HEAD --`, `git diff --`, and the listed new files. Inspect the final contents too;
a staged or unstaged undo does not remove the earlier layer from review. A missing file is a deletion, not a read error
to suppress. No reviewer substitutes `git diff HEAD` or an independently chosen base.
The inventory is repository-wide: distinguish task changes from existing work, but report interactions
that affect verification. Never claim someone else's changes as your own. Mixed edits to the same file
need a baseline diff; a filename exclusion alone is insufficient.

## Evidence and invalidation

For each applicable criterion record: command or review action, result, output/log location,
base and fingerprint, environment/profile, and any limitation. Results are PASS, FAIL, BLOCKED,
NOT RUN, or NOT APPLICABLE (with a reason). A manual inspection is labeled MANUAL and is not a
replacement for an unavailable required executable check. Record RED and GREEN separately for TDD;
a file's presence or a mental counterexample is not execution evidence.

Capture the fingerprint before and after verification. If they differ, identify what changed;
the earlier result cannot establish readiness of the new state. After fixes, rerun affected checks.
Reuse a passing result only for the same fingerprint and relevant environment/profile. An environment
change, failed check, or unexplained change requires revalidation. This is an evidence protocol, not
a persistent automatic cache. Tests that mutate tracked files make the result stale until reviewed.

For product changes, the aggregate entry point is `scripts/check.sh`, with `DOCS_SYNC_BASE` set to
the recorded base. It runs tooling and application verification once each. Do not surround it with
another full `verify`, `test`, or `ModularityTest` run on unchanged inputs. Focused TDD runs remain
appropriate during implementation. Persistence changes additionally require the PostgreSQL profile
specified by the Definition of Done. For process-only changes, run `./mvnw -pl tools verify`, the
relevant behavioral fixtures, and `docs-check`; mark unrelated product checks not applicable with
scope justification. An aggregate failure is never a pass because a narrower test succeeded.

## Completion and acceptance

Report **implementation verified** only when every applicable criterion has current passing evidence,
required reviews are resolved, and no required check is BLOCKED or NOT RUN. Otherwise report partial
completion and the exact gaps. Report **accepted by the human** only after the human explicitly
accepts the result. Approval to implement is not acceptance of the finished result. Requirement
statuses and architectural records remain human-owned; propose their updates with evidence and wait
unless that exact update was already explicitly approved. A feature can record implementation progress
without claiming human acceptance or changing a requirement's status.

Fix confirmed defects inside the contract without another permission request; rerun affected checks
and obtain a new review of substantive corrections. Ask only when a fix changes requirements,
architecture, scope, acceptance criteria or another protected decision. A review cannot be declared
complete when the reviewer was unavailable; report the limitation. Do not commit or publish merely
because verification passed.
