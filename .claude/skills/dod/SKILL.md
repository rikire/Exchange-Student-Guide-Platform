---
name: dod
description: Assess readiness against acceptance criteria and current evidence, including required review. Use before claiming implementation verified or when asked whether a change is ready.
---

Use [verification](../../../docs/ai/verification.md) to establish the shared base, inventory,
pre-existing-change exclusions and fingerprint. Missing scope is a verification error, not an empty
change. Do not suppress Git errors. An empty task change is reported as such, not as verified work.

Apply the relevant items in [Definition of Done](../../../docs/ai/definition-of-done.md).
Choose the process-only or product verification suite from verification.md. Reuse existing results
only under its evidence rules; do not run overlapping full suites merely to tick another item.
For each criterion give PASS, FAIL, BLOCKED, NOT RUN, or justified NOT APPLICABLE, with evidence.
Use the recorded base for documentation checks. Missing tools may be diagnosed by reading but a
required executable check remains BLOCKED until it runs successfully.

Read the shared change and identify correctness risks, unnecessary additions and missing evidence.
Give `dod-reviewer` the same scope, criteria and logs in a fresh context. Report confirmed findings,
assumptions and NEEDS_DECISION separately. If delegation is unavailable, disclose the missing review.
Fix confirmed defects within the contract, rerun affected checks and review substantive fixes without
asking permission again. Escalate only changed requirements, architecture or other protected decisions.

Conclude **implementation verified** only when the evidence protocol permits it. Otherwise list the
remaining gaps. Do not claim **accepted by the human**, change requirement status, commit or publish
without the corresponding explicit authorization.
