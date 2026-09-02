---
description: Add a paragraph to this week's contribution log
argument-hint: [--as <member>] <what you did>
---

Record work in this week's contribution log: **$ARGUMENTS**

The course requires a brief weekly log from each student, submitted at every stage
(course requirements, section 10.5).

1. Determine the author:
   - By default, read `git config user.email` and match it against `docs/team/members.yml`.
   - If the arguments start with `--as <member>`, use that member instead and mark the entry as
     recorded on their behalf. That is for pair work, a shared machine, or work done away from the
     repository — a stakeholder meeting, writing articles, preparing the demo.
   - If the email matches nobody in the registry, **stop and ask** rather than guessing.
2. Open `docs/team/weekly-log/YYYY-Www.md` for the current ISO week, creating it from the shape of
   the previous week's file if it does not exist.
3. Add the paragraph under that member's heading. Keep it factual: what was done and what it moved.
   No estimates of effort, no percentages — the ownership figures are measured separately.
4. If the work is already visible in git, do not repeat it: the log is for what git cannot show.

Show me the resulting section.
