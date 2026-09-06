---
paths:
  - "**/db/migration/*.sql"
  - "docs/architecture/data-model.md"
---

# The schema is the human's decision

Full document: [docs/ai/collaboration.md](../../docs/ai/collaboration.md), decision table.

- **Entity fields, cardinality, the fetch strategy and the values of a status enum are decided by
  the human, not proposed and adopted.** Editing this file asks first, and the question wants a
  decision rather than permission.
- A migration changes the schema of a running system: it is never edited after it has been applied,
  only followed by another.
- Every migration carries `-- trace: FR-XXX` in its header naming the requirement it serves.
- The migration and [docs/architecture/data-model.md](../../docs/architecture/data-model.md) change
  in the same turn. A schema the documentation does not describe is the drift this repository keeps
  auditing itself for.
