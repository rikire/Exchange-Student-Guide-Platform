---
name: ownership
description: Refresh and read the slice ownership table, which is measured from git history. Use when asked who has done more in a slice, whether the work between the two members is balanced, or before choosing the next task.
---

Refresh the ownership table and report what it says.

```
java -jar tools/target/ai-tools.jar ownership
```

It rewrites `docs/team/ownership.md` from git history. Read the file and report:

1. For each slice, who has more authored commits, and the slices with none from one of the two.
2. The overall authored figures, **and the journal commits the `Stop` hook wrote, in their own
   column**. Never add the two: the hook commits under whoever's machine the session ran on, so
   counting them measures prompting and not authoring.
3. If the balance has drifted, name the slices where the lighter side could take the next task. That
   is a suggestion for the humans, not an assignment.

The file is generated. Do not edit it by hand; a hand edit is lost on the next run.
