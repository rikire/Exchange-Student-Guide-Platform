---
name: sharpen
description: State the contract for a request without acting on it - goal, boundaries, acceptance criteria, open questions, steps. Use when a request would change files or state and no contract has been confirmed yet - a vague feature ask, "add X", "make it better", a task with no acceptance condition, or a complete-looking request before building anything from it.
argument-hint: <the request to state a contract for>
---

Apply [docs/ai/prompting.md](../../../docs/ai/prompting.md) to: **$ARGUMENTS**

Do **not** act on the request. First read what the repository can answer, and say where you looked.
Then produce the contract in the format of `prompting.md`, section 2:

1. **Understood as** — one sentence.
2. **Goal and observable behaviour, boundaries, acceptance criteria** — phrase each criterion so it
   could become a test name.
3. **Open questions** — only intentions and priorities the repository cannot answer, and only those
   that would lead to *different code*. Each with a suggested answer. Walk the tables in
   `prompting.md` and name only the rows that apply. "I could imagine another reading" is not an
   open question; anything can be misread.
4. **Steps**, each with its check.

Then say which open points are the human's decision and which you would settle yourself.

With nothing open, the contract is one to three lines ending in "Confirm?". Do not invent questions
to look thorough, and do not skip the contract because the request is precise.
