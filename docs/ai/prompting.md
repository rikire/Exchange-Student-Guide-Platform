# Sharpening the prompt before doing the work

A vague prompt does not produce no work. It produces confident, plausible work aimed at a target
nobody stated — and that is more expensive than a refusal, because it looks finished.

So: **before acting on an underspecified request, ask what it left open — and wait for the answer.**

For this project specifically, there is a second reason. The course asks each of us to explain any
line we submit at the viva. Code produced from a fuzzy instruction is the code neither of us can
defend, because nobody ever decided what it was supposed to do.

## The rule

**If the request would produce code, a document or a schema whose shape depends on something it did
not state — ask, and wait for the answer. Do not pick a default and build on it.**

Picking a sensible default and announcing it is *not* compliance with this rule. It looks
cooperative and it produces the same failure: work aimed at a target nobody chose, now with the
appearance of agreement. The point is that the human decides, before there is code to argue with.

Answer with:

```
Understood as: <one sentence — what I think you are asking for>

Before I write anything:
  1. <closed question>  — suggested: <answer>
  2. <closed question>  — suggested: <answer>
  3. <closed question>  — suggested: <answer>

Also worth deciding now:
  - <the requirement the request implies and did not state>

Say "all as suggested" or answer the ones you want differently.
```

Three things make this cheap enough to keep doing:

- **Closed questions.** "Ignore case?" not "how should case be handled?" — the second one makes the
  human do the drafting.
- **A suggested answer for every question.** They can reply "all as suggested" in three words. What
  matters is that the choice was theirs, not that they typed it out.
- **Only questions that change the artefact.** If both answers produce the same code, it is not a
  question, it is chatter.

While waiting, do anything that is true under **every** answer — that is usually nothing for a small
piece of code, and quite a lot for a feature.

## When not to do this

The exceptions are deliberately few, because the failure this guards against is far more common than
the noise it can cause. Do not ask when:

- the request is a **direct command with a checkable result** — "run the tests", "show the git log",
  "push", "fix the typo on line 12";
- it already carries its acceptance condition — "make the importer reject a ZIP with no manifest,
  with a test";
- the answer is **already written down** in a requirement, an ADR or the roadmap. Then cite it
  instead of asking; asking about a settled decision is worse than silence, because it suggests the
  decision was never recorded.
- it is a follow-up inside a context where the ambiguity was already settled in this session;
- the human is correcting you. A correction is a decision, not a request to be re-scoped.

Note what is *not* on this list: "the task is small". A five-line function has an input contract
whether or not anyone wrote it down, and that contract is exactly the thing that is wrong later.

## The input contract — the questions people forget

For anything that takes input and returns a result, these change the code and are almost never
stated:

| Question | Why it changes the code |
|---|---|
| What may the input contain — letters, digits, punctuation, whitespace, other scripts? | Decides the filter, and whether code points or chars are iterated |
| Is case significant? | One `toLowerCase` that is either right or wrong |
| Which characters are ignored rather than compared? | Whitespace and punctuation are the usual pair, and the usual disagreement |
| What is the behaviour on empty input? | Very often "vacuously true" is right and nobody said so |
| What on `null`, or on input that is not valid at all? | A return value hides the caller's bug; an exception surfaces it. Different contracts |
| How long can the input be? | Decides whether an O(n²) reading is acceptable |
| Where will this live — a throwaway, or repository code? | Repository code needs a requirement, a slice, tests and an anchor |

For work that lands in the repository, [the second checklist](#what-not-mentioned-but-needed-means)
applies on top of this one.

## What counts as "unclear"

Not "I could imagine another meaning" — anything can be misread. It is unclear when the readings
lead to **different code**:

- the goal is named but not the condition that would make it done ("make search better");
- a noun maps to more than one thing in the [glossary](../requirements/glossary.md) — "edit" is a
  submission in the queue, or a moderator's direct change, and those are different flows;
- the boundary is open: does "add moderation" include notifying the contributor by email?
- it is not clear which slice owns it, and the honest answer is a new slice;
- it implies a decision that belongs to the human.

## What "not mentioned but needed" means

The most useful half of the rule. A request usually states the happy path and nothing else. Walk
this list and name what is missing — briefly, only the ones that actually apply:

| Question | Why it bites here |
|---|---|
| Which `FR` does this serve? Does it exist yet? | Work with no requirement cannot be traced, and the gate will say so |
| Which slice owns it? | If the answer is "a new one", that is the human's call |
| What does a visitor see when it fails? | Error paths are where "done" quietly becomes "half done" |
| What happens with empty or absent data? | No results, no tags, a deleted article behind a wiki link |
| Does it need a migration? | The schema is frozen after phase 2; changing it is a joint decision |
| Does the route contract change? | Then `ui-routes.md` changes in the same turn |
| Who is allowed to do it? | Anonymous contributor, or moderator only |
| Does it touch the moderation queue, media or the search index? | Those three couple to almost everything |
| What is the acceptance criterion? | It becomes a test name; if it cannot be phrased, it cannot be tested |
| Does it change what the stakeholder was promised? | Then it is a change to `acceptance.md`, not just to code |

## What happens to the sharpened version

Once agreed, the sharpened prompt **is** the specification for that piece of work. Use its wording
in the feature file's acceptance criteria and in the test names, so the same sentence runs from the
request to the assertion.

It also belongs in the journal. `ai-tools hook english --prompt` takes the rendering that gets
recorded, so record the sharpened version there rather than the original vague one — with the
original still above it, since [the journal keeps both](journal/README.md).

That record is worth marks on its own: it shows scope being negotiated rather than assumed, which is
exactly what the course says is the hard part of software.

## How this rule is kept alive

It is delivered by the `UserPromptSubmit` hook with **every** request, not left to be remembered
from the start of the session. That is not belt and braces: it was added after a session read this
rule at startup, then answered a request for a palindrome function with two implementations and no
questions at all. Instructions read once compete with everything that arrives later, and lose.

The reminder is emitted unconditionally rather than guessed at from the wording. A heuristic over
imperative verbs in two languages would miss the cases that matter, and a reminder that fires only
sometimes teaches the reader to ignore it.

No hook can judge whether a request was actually vague, so this reinforces the rule rather than
gating it. The audit trail is the journal: prompts and outcomes are recorded, so whether vague
requests were met with questions can be reviewed after the fact.

## On demand

`/sharpen <text>` applies this to a prompt without acting on it — useful for thinking a request
through before committing to it.
