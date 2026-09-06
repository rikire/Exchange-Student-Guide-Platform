---
name: dod
description: Run the Definition of Done over the current changes. Use before calling work finished, before a commit that closes a feature, or whenever someone asks whether something is done, ready, or safe to merge.
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(./mvnw:*)
---

## What has actually changed

Working tree:

!`git status --short || true`

Against HEAD:

!`git diff HEAD --stat || true`

## The checklist

Run the checklist in [docs/ai/definition-of-done.md](../../../docs/ai/definition-of-done.md).

The diff above is the subject. It is injected rather than described because a checklist run from
memory of what was changed is a checklist run against the wrong thing.

Go through the items **one at a time** and give an honest verdict for each: passed, failed, or not
applicable. Run the checks for real, not from memory:

- `./mvnw -q verify`
- `./mvnw test`
- `./mvnw -pl app test -Dtest=ModularityTest`

The traceability and documentation-sync generator lands in phase 2. Until it exists, do items 6
and 7 by reading, the way `/trace-check` describes, and say in the verdict that they were checked
by hand rather than by a tool. Do not run `ai-tools trace` (phase 2): it is not there, and reporting a
checklist item as passed on the strength of a command that failed is the exact failure this list
exists to prevent.

## The second reading

Then **re-read your own diff in full** (`git diff HEAD`) and say separately: what looks doubtful,
what was added "just in case", and what should be deleted.

Then hand the same diff to the `dod-reviewer` subagent and report what it found. You wrote this
code, so your reading of it is the one reading that cannot be independent; the subagent sees the
diff and the criteria without the reasoning that produced them. Where it disagrees with your own
verdict, say so rather than picking the more comfortable of the two.

Do not report "all done" if any item failed. List what is left and ask whether to fix it now.
