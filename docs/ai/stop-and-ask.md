# When to stop and ask

Stopping is not a failure to work. A wrong answer delivered confidently costs more than a question,
and on this project it costs twice: once in the code, and once at the viva when neither of us can
explain why it is that way.

## First: is this a stopping problem at all?

Most of the time an unclear request is not a reason to stop — it is a reason to **sharpen it and
carry on** ([prompting.md](prompting.md)): restate it, name the assumptions, do everything that
holds under any answer. Stopping is for the shorter list below, where no assumption is safe.

Confusing the two is expensive in both directions. Stopping on every ambiguity makes the agent
useless; assuming past a decision that belongs to the human makes its output untrustworthy.

## Mandatory stop triggers

Stop and ask when any of these is true:

1. **A written requirement admits more than one reading** — an `FR`, an acceptance criterion, an
   ADR. Not "pick the obvious one": obviousness is a property of the reader, and these are the
   human's artefacts, so a divergent reading has to be settled rather than assumed.
   (An ambiguous *request in conversation* is different: sharpen it and carry on —
   [prompting.md](prompting.md).)
2. **The task needs a decision from the human's space** ([collaboration.md](collaboration.md)):
   a requirement's wording, a route, the schema, a new dependency, a slice boundary, an ADR.
3. **Something must move into `shared/`.** It is the one place two people collide, so it grows only
   by decision.
4. **A red test cannot be written first** because the environment cannot be stood up. Say so; do not
   silently reverse the order.
5. **The change would break an acceptance criterion** recorded in `docs/stakeholder/acceptance.md`.
6. **A fact cannot be verified** — a version, an API's behaviour, a flag. Say "not verified" rather
   than producing something plausible.
7. **The instruction and reality disagree.** Propose changing the instruction instead of quietly
   working around it.
8. **The same remark has come up twice.** That is a defect in `docs/ai/`, and the fix is an edit
   there, not more care.

## Not a reason to stop

- The name of a variable, method or internal package.
- The layout of a test.
- The choice between equivalent implementations behind one signature.
- Anything already decided in an ADR or a requirement.

Asking about these is as much a refusal to work as changing the route contract unilaterally.

## Do the independent part first

A question does not freeze the whole task. Finish everything that is true under **any** answer, then
stop with a specific question and a summary of what is already done. Delivering nothing while
waiting is only right when proceeding under any assumption would be unsafe or would make the work
useless if the guess is wrong.

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

Rules for the block:

- **One question at a time.** Three questions at once get one answer and two silent assumptions.
- **Always give a recommendation.** "Whatever you prefer" pushes the work back without the context
  the agent already has.
- **Name the consequence, not the mechanism.** "Search stops matching Tamil words" beats "the
  analyzer changes".
- **Two or three options, not a survey.** If there is really only one sensible path, say so and ask
  for confirmation instead of inventing alternatives.
