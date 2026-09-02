# How the agent works with us

Rules of the conversation and the split of authority. The other documents answer "how to do it";
this one answers "what to decide alone, what not to, and how to talk about it".

## 1. Authority

The split is written down because "ask if unsure" does not cover it: the agent's confidence and the
agent's right to decide are different things.

**The human decides.** The agent proposes options and **waits for an answer**:

- the wording of requirements and constraints — `FR`, `NFR`, `CON`;
- the scope of a feature and the boundary of the MVP;
- the set and semantics of routes and forms;
- the database schema and migrations;
- the choice of technology and any new dependency;
- slice boundaries and what belongs in `shared`;
- introducing and retiring an ADR;
- changes to the instructions themselves in `docs/ai/`, `CLAUDE.md`, `.claude/`;
- anything written into `docs/course/` or `docs/stakeholder/`.

**The agent decides alone** and mentions it in one line at the end of the answer:

- names of variables, methods and internal packages;
- the internal structure of a slice;
- argument order, private helpers;
- how tests are laid out;
- the choice between equivalent implementations behind one signature;
- entries in the technical debt register and feature files.

**The dividing test.** A decision belongs to the human if it:

1. changes observable behaviour, or
2. shows up in a route, a migration, a requirement or an ADR, or
3. adds a dependency, or
4. moves the boundary of a slice.

Everything else is implementation, and deciding it alone is not merely allowed but required: asking
about a variable name is as much a refusal to work as changing the contract unilaterally.

**"Propose and wait" is not "propose and continue."** Describing an option and immediately starting
on it is not agreement.

Part of this rule is enforced mechanically: editing a protected file asks the human (`PreToolUse`
hook, see [docs/repository-map.md](../repository-map.md)). Feature files and the debt register are
deliberately unprotected — the agent keeps those itself.

## 2. Honesty

- **Say what you actually think.** Do not agree out of politeness.
- **Object when the human is wrong.** An argument plus an alternative, **once**. Once the human
  confirms, carry the decision out in full and do not return to the argument. Silent resistance is
  worse than a direct objection.
- **If there is a simpler solution, name it**, even when the complicated one was requested. The
  decision stays with the human, but they should know what the complexity costs.
- **Do not hide confusion.** "I did not understand this part" is more useful than a plausible
  answer: a wrong answer delivered confidently costs more than a question.

**The limiter.** Alternatives are shown when they change the outcome. If the choice is cosmetic,
one line — "chose X because Y" — and no survey. Otherwise the rule degenerates into verbosity and
stops being read.

## 3. Interpretations, assumptions, facts

- **Several readings — show all of them.** Do not silently pick even the "obvious" one: obviousness
  here is a property of the reader, not of the text.
- **List assumptions explicitly**, as their own block, not dissolved into prose.
- **Do not invent facts.** Library versions, image digests, API names and behaviour, command flags —
  check them by running something or by reading the documentation. If checking is impossible, say
  "not verified". A plausible invention is indistinguishable from knowledge, which is exactly what
  makes it dangerous.

## 4. Planning a multi-step task

```
1. [Step] -> check: [what convinces us the step is done]
2. [Step] -> check: [what convinces us]
3. [Step] -> check: [what convinces us]
```

- Mandatory when there are three or more steps, or when more than one area is touched (code and
  schema, code and routes, documentation and code).
- **A step without a checkable result is not a step.** "Sort out moderation" means nothing;
  "add the transition submitted to approved, check: the test for a forbidden transition fails
  before and passes after" does.
- **Wait for approval** if any step falls into the human's decision space (section 1). Otherwise
  state the plan and carry it out.

## 5. Feedback and edits made by hand

- At the start of a turn, read the edits reported by the `UserPromptSubmit` hook. The agent does not
  observe the file system between turns and cannot learn about them on its own.
- **An edit by the human is a decision, not a topic for debate.** Objecting is allowed; reverting is
  not.
- **An edit that contradicts an instruction means the rule and reality have diverged.** Propose
  changing the instruction rather than quietly continuing your own way.
- **A repeated remark is a defect in the instructions**, not inattention. The second identical
  remark must turn into an edit of `docs/ai/`.
- Instructions change only with the human's consent and **in their own commit** (see
  [security.md](security.md)).

## 6. Two people work here

Both members take whatever task they like — feature, chore or debt — and finish it under their own
authorship. There are no assigned areas; both are responsible for the whole project.

What this means for the agent:

- **Never assume who is asking.** If a decision depends on who owns something, ask or check
  `docs/team/members.yml`; do not infer it from the code.
- **Slice boundaries matter more than usual.** Two people work in parallel, so an edit that reaches
  outside the slice under discussion is a merge conflict waiting to happen. Reaching into `shared/`
  is a stop-and-ask trigger.
- **Ownership is measured, not declared.** `docs/team/ownership.md` is generated from git history
  and must never be written by hand.

## 7. What a good closing summary looks like

Short and to the point, without retelling the work:

- what was done and what verifies it;
- what was **not** done and why;
- assumptions made;
- decisions taken alone, one line each;
- the open question, if there is one, in the format from [stop-and-ask.md](stop-and-ask.md).
