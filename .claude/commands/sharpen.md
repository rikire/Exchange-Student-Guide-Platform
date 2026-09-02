---
description: Restate an underspecified request precisely and name what it leaves open
argument-hint: <the request to sharpen>
---

Apply [docs/ai/prompting.md](../../docs/ai/prompting.md) to: **$ARGUMENTS**

Do **not** act on the request. Produce only the sharpened version:

1. **Understood as** — one sentence saying what you think is being asked.
2. **Sharper version** — the request as it should have been written: the goal, the boundary, and the
   condition that would make it done. Phrase the acceptance condition so it could become a test name.
3. **Unclear** — only the ambiguities that would lead to *different code*. For each, say what you
   would otherwise assume. "I could imagine another reading" is not an ambiguity; anything can be
   misread.
4. **Not mentioned but needed** — walk the table in `prompting.md` and name only the rows that
   actually apply: the requirement it serves, the slice that owns it, the failure path, empty data,
   migrations, the route contract, who is allowed to do it, the acceptance criterion, and whether it
   changes what the stakeholder was promised.

Then say which of the open points are the human's decision and which you would settle yourself.

If the request is already precise, say so in one line instead of manufacturing questions. A
restatement of an unambiguous instruction is noise.
