# Prompt journal

One file per session, named `YYYY-MM-DD-<session>.md`. Each entry records the prompt, what came out
of it, and **what the human changed by hand afterwards**.

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
