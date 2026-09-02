# Sharpening the prompt before doing the work

A vague prompt does not produce no work. It produces confident, plausible work aimed at a target
nobody stated — and that is more expensive than a refusal, because it looks finished.

So: **before acting on an underspecified request, restate it sharply, name what is unclear, and name
the requirements the request implies but does not mention.** Then act.

For this project specifically, there is a second reason. The course asks each of us to explain any
line we submit at the viva. Code produced from a fuzzy instruction is the code neither of us can
defend, because nobody ever decided what it was supposed to do.

## The rule

When a request is underspecified in a way that changes the work, answer first with:

```
Understood as: <one sentence — what I think you are asking for>

Sharper version:
  <the prompt as it should have been written: goal, boundary, acceptance condition>

Unclear:
  1. <question> — I would otherwise assume <X>
  2. <question> — I would otherwise assume <Y>

Not mentioned but needed:
  - <the requirement the request implies and did not state>
```

Then, without waiting:

- **Do everything that is true under any answer.** A question does not freeze the whole task.
- **Wait** only for what falls in the human's decision space ([collaboration.md](collaboration.md)),
  or where proceeding under either reading would waste the work.
- Otherwise **carry on under the stated assumptions**. They are written down; that is what makes
  them correctable.

## When not to do this

The rule earns its place by firing rarely. Do not restate when:

- the request is direct and checkable — "run the tests", "show the git log", "fix the typo on
  line 12";
- it already carries its acceptance condition — "make the importer reject a ZIP with no manifest,
  with a test";
- it is a follow-up inside a context where the ambiguity was already settled;
- the human is correcting you. A correction is a decision, not a request to be re-scoped.

A restatement attached to an unambiguous instruction is noise, and a rule that produces noise gets
skipped when it matters.

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

## On demand

`/sharpen <text>` applies this to a prompt without acting on it — useful for thinking a request
through before committing to it.
