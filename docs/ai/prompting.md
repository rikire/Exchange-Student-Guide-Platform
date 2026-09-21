# From request to confirmed contract

A vague request produces confident work aimed at a target nobody stated. A complete-looking one does
the same when the agent fills the unstated remainder with its own defaults. So nothing that changes
files or state starts until a contract has been confirmed. The course also asks each of us to explain
any submitted line at the viva, and nobody can defend behaviour nobody decided.

## 1. Read before asking

Sort what the request leaves unknown into three kinds. Only the second is a question for the human.

| Kind | Example | Do |
|---|---|---|
| A fact the repository or a source can answer | where a check lives, what a requirement says, whether a version exists | Look it up and say where. A question about it is a gap in preparation. |
| An intention, a priority or a product decision | what counts as a duplicate, fail or warn, who may do it | Put it in the contract as an open question with a suggested answer. |
| A technical choice inside the contract | private names, helper layout, equivalent APIs | Decide it and say so in one line. |

A decision already written in a requirement, an ADR or the roadmap is cited, never asked again.

## 2. The contract

Required for any request that changes files or state. A question and a read-only command need none.

```
Understood as: <one sentence>
Goal and observable behaviour: <what a person can see afterwards>
Boundaries: <what is out of scope>
Acceptance criteria: <each phrased so that it can become a test name>
Open questions:
  1. <closed question> — suggested: <answer>
Steps:
  1. <step> — check: <what convinces us it is done>
Confirm, or answer the open questions.
```

With nothing open the contract is one to three lines ending in the confirmation question. "The task
is small" shortens the contract and does not remove it: a five-line function has an input contract
whether or not anyone wrote it down.

Choosing a default and announcing it is not compliance. It leaves a target nobody picked, now with
the look of agreement.

A contract says what; a plan says how. A detailed plan built on a misread task is still a misread
task, so the contract comes first and the plan follows it. The agent's sharper wording never replaces
the human's request: show both.

## 3. Confirmation

- **Explicit.** "Confirmed", "all as suggested", or answers to the open questions. Silence and
  "propose and continue" are not confirmation.
- **Recorded once**, with who confirmed it, in the feature file, an audit or the journal entry, and
  not asked again.
- **Reopened** when the result, the boundaries, the criteria or the approach change. Stop the
  affected work and revise; the rest may continue.
- A correction by the human is a decision, not a request to re-scope. A follow-up inside a confirmed
  contract is not a new request.

The confirmed sentences become the acceptance criteria in the feature file and the names of the
tests, so one sentence runs from the request to the assertion. Record the confirmed contract as the
English rendering with `ai-tools hook english --prompt`; the original prompt stays above it.

## 4. The input contract

For anything that takes input and returns a result, these change the code and are rarely stated:

| Question | Why it changes the code |
|---|---|
| What may the input contain — letters, digits, punctuation, whitespace, other scripts? | Decides the filter, and whether code points or chars are iterated |
| Is case significant? | One `toLowerCase` that is either right or wrong |
| Which characters are ignored rather than compared? | Whitespace and punctuation are the usual pair, and the usual disagreement |
| What is the behaviour on empty input? | "Vacuously true" is often right and nobody said so |
| What on `null`, or on input that is not valid at all? | A return value hides the caller's bug; an exception surfaces it |
| How long can the input be? | Decides whether an O(n²) reading is acceptable |
| Where will this live — a throwaway, or repository code? | Repository code needs a requirement, a slice, tests and an anchor |

## 5. What the request did not mention

Open questions worth naming are the ones whose readings lead to **different code**: a goal without a
condition that makes it done; a noun that maps to more than one glossary term (an "edit" is a
submission in the queue or a moderator's direct change); an open boundary; an unclear owning slice;
a decision that belongs to the human. Walk this list and name only the rows that apply:

| Question | Why it bites here |
|---|---|
| Which `FR` does this serve? Does it exist yet? | Work with no requirement cannot be traced |
| Which slice owns it? | "A new one" is the human's call |
| What does a visitor see when it fails? | Error paths are where "done" becomes "half done" |
| What happens with empty or absent data? | No results, no tags, a deleted article behind a wiki link |
| Does it need a migration? | The schema is frozen after phase 2 |
| Does the route contract change? | Then `ui-routes.md` changes in the same turn |
| Who is allowed to do it? | Anonymous contributor, or moderator only |
| Does it touch the moderation queue, media or the search index? | Those three couple to almost everything |
| What is the acceptance criterion? | It becomes a test name; if it cannot be phrased, it cannot be tested |
| Does it change what the stakeholder was promised? | Then it is a change to `acceptance.md` |

## 6. How the rule is delivered

The `UserPromptSubmit` hook injects the reminder with every request, because a rule read once at
session start loses to everything that arrives later. It was added after a session answered a
palindrome request with two implementations and no questions. The reminder is unconditional: a
heuristic over verbs in two languages would miss the cases that matter.

No hook can judge whether a request was complete, so the reminder reinforces the rule and does not
gate it. The journal is the audit trail. `/sharpen <text>` writes the contract without acting.
