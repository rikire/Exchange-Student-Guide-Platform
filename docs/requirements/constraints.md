# Constraints

Deliberate simplifications and boundaries, identified as `CON-XXX`. **A `**Rationale:**` line is
always mandatory:** a simplification without a reason is indistinguishable from an omission, and at
the viva the difference is the whole point.

A constraint is not technical debt. Debt is "we know how it should be and this is not it", and it
has a growing cost. A constraint is "we decided not to, and the decision is sound". Debt lives in
[docs/tech-debt.md](../tech-debt.md).

## Format

```markdown
### CON-004 — No user accounts

**Rationale:** Moderation gates quality, and accounts would add registration, password reset and
personal data handling for no gain the stakeholder asked for. Abuse is handled by rate limiting and
a honeypot instead.
```

## Constraints

None yet. Written in phase 1.

Already known to belong here: no user accounts; no discussion pages; no watchlists; no diffs; a
single-language interface; and the Spring Modulith event registry left switched off.
