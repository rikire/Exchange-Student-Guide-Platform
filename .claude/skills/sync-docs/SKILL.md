---
name: sync-docs
description: Update documentation for a confirmed change using the shared review scope. Preserve human approval for requirements and architecture.
---

Use the task's base, inventory, exclusions and criteria from
[verification](../../../docs/ai/verification.md); do not infer scope from only uncommitted files.
Apply [the documentation mapping](../../../docs/ai/docs-sync.md) to that change.

Update factual descriptions and feature implementation progress autonomously inside the approved
contract. Requirements, their statuses, route contracts, schema, ADRs and architectural decisions
require explicit approval of the proposed changes before writing; existing approval for those exact
changes is sufficient. Present missing decisions together and continue independent permitted work.
Do not derive acceptance from the mere existence of code and tests. Human acceptance is separate
from implementation verification. Never edit generated files by hand.

Report updated documents, proposed protected updates awaiting approval, and justified non-applicable
items. If documentation remains required but unapproved, report incomplete verification, not done.
