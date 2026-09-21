# When to stop

Questions before the work belong to the contract ([prompting.md](prompting.md)). This document covers
stops during the work. A wrong answer delivered confidently costs twice here: in the code, and at the
viva when nobody can explain why it is that way.

"I am not sure" is a valid reason to stop and not a sufficient safety net, because a model can be
confidently wrong. The triggers below are events anyone can observe.

## Observable triggers

Stop dependent work when any of these is true:

1. A check refutes a hypothesis you acted on — a test, a command output, a source, a human's report.
2. A second attempt at the same thing is about to start and nothing new has been learned since the
   first.
3. The change touches something outside the contract's boundaries, or has grown past its steps.
4. A result contradicts a requirement, an ADR, an acceptance criterion (including
   `docs/stakeholder/acceptance.md`) or the contract.
5. Behaviour needed to continue is unknown and cannot be looked up. Say "not verified" rather than
   produce something plausible.
6. You cannot link the change to a requirement or to a step of the contract.
7. You can no longer say which decisions are agreed and which were only proposed.

## Stops that belong to the human's space

- A written requirement admits more than one reading. Obviousness is a property of the reader.
- The task needs a decision from the human's space ([collaboration.md](collaboration.md)): the wording
  of a requirement, a route, the schema, a dependency, a slice boundary, an ADR.
- Something must move into `shared/`, the one place two people collide.
- A red test cannot be written first because the environment cannot be stood up. Say so; do not
  reverse the order silently.
- The instruction and reality disagree. Propose changing the instruction instead of working around it.
- The same remark has come up twice. That is a defect in `docs/ai/`, and the fix is an edit there.

## Recovery

Acknowledging a mistake is not enough: what was built on it stays wrong.

1. Stop the actions that depend on the refuted or contradicted premise. Independent research that
   is still useful may continue.
2. Write down what is confirmed, what is refuted and what is unknown.
3. Trace what rested on the premise: decisions, code, tests, documents, and statements already made
   to the human.
4. Look up the available fact, or ask the human for a decision.
5. Revise the plan, and the contract if its result, boundaries or criteria changed.
6. Fix the consequences and repeat the checks they affect. A test built on the refuted assumption is
   not a check.

A new attempt needs new information or a new hypothesis that can be checked.

## Not a reason to stop

The name of a variable, method or internal package; the layout of a test; the choice between
equivalent implementations behind one signature; anything already decided in an ADR or a requirement.
Asking about these is as much a refusal to work as changing the route contract unilaterally.

## Do the independent part first

A question does not freeze the task. Finish everything that is true under any answer, then stop with
a specific question and a summary of what is done. Deliver nothing while waiting only when proceeding
under any assumption would be unsafe or wasted.

## The format of the question

```
Done so far:
  - <what is finished and what verifies it>

Blocked on: <the single decision needed>

Options:
  A. <option> — <consequence>
  B. <option> — <consequence>

Recommendation: <A or B, and why in one line>
```

- **One question at a time.** Three at once get one answer and two silent assumptions.
- **Always a recommendation.** "Whatever you prefer" hands the work back without the context the agent
  already has.
- **Name the consequence, not the mechanism.** "Search stops matching Tamil words" beats "the analyzer
  changes".
- **Two or three options, not a survey.** If there is one sensible path, say so and ask for
  confirmation.
