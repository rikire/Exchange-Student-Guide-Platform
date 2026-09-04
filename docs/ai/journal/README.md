# Prompt journal

One file per session, named `YYYY-MM-DD-<session>.md`. Each entry records who sent the prompt, the
prompt itself, what came out of it, and **what the human changed by hand afterwards**.

## Who sent the prompt

Resolved from `git config user.email` against [docs/team/members.yml](../../team/members.yml) — the
same registry the weekly contribution log uses, because two registries would disagree inside a week.

When the email matches nobody, the hook cannot ask a person; it can only hand context to the agent.
So it tells the agent to ask, and the answer comes back through
`ai-tools hook author <id>`. Asked once per session, not once per prompt.

That command **fills a blank and never overwrites**. An answer typed into a chat must not be able to
contradict what git says, or the record stops being evidence and becomes a claim.

**What this can and cannot show.** It records whose machine the prompt came from, not who was
holding the keyboard. For two people at one machine that is false precision, and it is said here so
that nobody presents the journal at the viva as more than it is.

The files are written by `ai-tools hook`, wired to the `UserPromptSubmit` and `Stop` hooks in
[.claude/settings.json](../../../.claude/settings.json). Do not write entries by hand; to add your
own words to the current entry, use `/journal-note`.

## The journal is in English; the conversation is not always

The evaluator reads English, so the journal is written in English. But the prompts are often written
in Russian, and a hook cannot translate.

So the agent supplies the rendering before it ends the turn:

```sh
java -jar tools/target/ai-tools.jar hook english \
  --prompt "<the prompt in English>" --outcome "<what was done, in English>"
```

An entry then looks like this:

```markdown
**Prompt**

> Настрой каркас репозитория

**Prompt (English)**

> Set up the repository scaffolding

**Outcome**

> Built the Maven skeleton and the process tooling.
```

**The original prompt is kept.** It is the artefact the course asks us to be able to show; the
translation is an interpretation of it, and keeping both lets a reader of either language check the
other. The outcome is written in English only — it is the agent's own summary, so there is no
original to preserve.

If the prompt was already in English, the translation block is omitted rather than duplicated.

## How an entry reaches the history

The `Stop` hook commits it. That is unusual enough to justify here, because a hook writing to git
history is not free.

An entry is appended **after** the turn's last action. So the turn an entry describes can never be
the turn that commits it — the record is structurally one turn behind, and on 4 September an entry
sat outside the history until someone opened the file and noticed. Nothing owned getting it in, and
"remember to commit the journal" is the kind of rule this repository has already watched fail.

What keeps the cost bounded:

- the pathspec is always `docs/ai/journal`, so a commit someone is building elsewhere is never swept
  in — anything staged stays staged;
- during a merge, a rebase, a cherry-pick or on a detached HEAD it does nothing and says so;
- a failed commit leaves the entry pending, and the next turn that ends tries again.

The commit message is `docs: record the journal entry for <date> <time>`, which passes the same
`commit-msg` gate as everyone else's.

## The times this was written by hand

Twice, both on 4 September, and both to add a rendering that should have been captured in the
moment. Every block added afterwards is labelled **"added 2026-09-04, retrospectively"**, the
originals were not touched, and nothing was removed — including the `Checks` line that says
`English rendering: NOT supplied`. That line is the record of what happened; the block below it is
the repair. Both belong.

**Seven entries, in the morning.** They had been written with no rendering at all: the rule to
supply one lived in `CLAUDE.md` and lost to everything that arrived after it. That is why the
`UserPromptSubmit` hook now asks for a rendering whenever a prompt is not in English.

**One entry, that afternoon** — the entry describing the audit that mechanised six other rules. The
reminder had fired correctly and was ignored anyway, and the `Stop` hook only *recorded* the
omission. A mechanism that observes a rule is not a mechanism that enforces it, so `Stop` now
refuses to end a turn that owes a rendering.

The label is the whole point: a translation inserted afterwards must not be able to pass for one
captured in the moment, or the journal stops being a record of what happened and becomes a record of
what we would like to have happened. The bar for a third time is the same: add, never alter, and say
when it was added. Editing an entry to make the past look tidier is the failure this file exists to
prevent.

## Why the human's edits are recorded

The assistant does not observe the file system between turns. If someone fixes a line by hand after
an answer, the assistant never learns about it — it will keep working from its own picture and may
overwrite the fix. The hook compares SHA-256 snapshots of the working tree taken at the end of one
turn and the start of the next, and reports the difference.

That difference is also the most interesting part of the record for the course: it shows where the
generated code was not good enough and what a person had to correct.

## What this is for

The AI-usage statement of the proposal commits us to being able to explain any line we submit.
Reconstructing that at the end of the semester is not possible. The journal is written as the work
happens so that at the viva there is a record rather than a recollection.

The journal is evidence, not a deliverable to polish. Entries stay as they were written, including
the ones where something went wrong.
