---
description: Add a note to the prompt journal
argument-hint: <text of the note>
---

Add a note to the current journal entry:

```
java -jar tools/target/ai-tools.jar hook note "$ARGUMENTS"
```

The prompt and the outcome are recorded automatically by the hooks. Use this for what the hooks
cannot see: why an approach was abandoned, what was tried and did not work, a decision taken away
from the keyboard.
