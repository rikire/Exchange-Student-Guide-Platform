---
name: feature
description: Prepare a feature specification and link it to requirements after a confirmed contract. Propose missing requirements before writing them.
argument-hint: <short description of the feature>
---

Open a feature for **$ARGUMENTS** under the confirmed contract. If none exists, follow
[the contract procedure](../../../docs/ai/prompting.md) first. Approval is about decisions, not
permission to invoke this skill; use it autonomously inside an already approved task.

1. Read the relevant requirements, existing features and [repository map](../../../docs/repository-map.md).
2. Identify covered FR/NFR identifiers. If a requirement is missing or must change, present the
   proposed wording and acceptance criteria, then wait for explicit human approval before writing it.
   Calling this skill, approving implementation, or agreeing to research is not that approval.
3. Reuse agreed architecture. Propose any new slice, route semantics, schema or architectural decision
   and wait before recording it. Do not ask again about decisions already explicitly approved.
4. Allocate the next free FEAT identifier and use [the template](../../../docs/features/_TEMPLATE.md).
   Record the approved behavior, boundaries and acceptance criteria. Front matter describes current
   files, not intended future files. Cite the contract/decision confirmation in the feature body.
5. Show the result. If no new decision is needed, continue the approved implementation without an
   extra confirmation round. If invoked only to prepare a specification, stop after that deliverable.

Requirement status changes and human acceptance follow [verification](../../../docs/ai/verification.md).
