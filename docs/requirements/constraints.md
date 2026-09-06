# Constraints

Deliberate simplifications and boundaries, identified as `CON-XXX`. **A `**Rationale:**` line is
always mandatory:** a simplification without a reason is indistinguishable from an omission, and at
the viva the difference is the whole point.

A constraint is not technical debt. Debt is "we know how it should be and this is not it", and it
has a growing cost. A constraint is "we decided not to, and the decision is sound". Debt lives in
[docs/tech-debt.md](../tech-debt.md).

## Format

_(Illustrative example below — not a real entry. Real constraints start at `CON-001` in
[Constraints](#constraints).)_

```markdown
### CON-040 — No user accounts

**Rationale:** Moderation gates quality, and accounts would add registration, password reset and
personal data handling for no gain the stakeholder asked for. Abuse is handled by rate limiting and
a CAPTCHA challenge instead.
```

A constraint is product/requirements-level, not architecture — a decision about what the system
does not do, not how it is built. A framework-level or implementation detail belongs in
[docs/ai/architecture-rules.md](../ai/architecture-rules.md) instead.

## Constraints

### CON-001 — No user accounts

**Rationale:** Moderation gates quality, and accounts would add registration, password reset and
personal data handling for no gain the stakeholder asked for. Abuse is handled by rate limiting and
a CAPTCHA challenge instead.

### CON-002 — No discussion pages

**Rationale:** The moderation queue already gates every change; a separate discussion layer would
add a whole new content type and its own moderation model, for a project with one or two OGE
moderators deciding, not a community reaching consensus among many editors.

### CON-003 — No watchlists

**Rationale:** There are no accounts to attach a personal watchlist to, and nothing persists per
visitor across sessions.

### CON-004 — No diffs

**Rationale:** Version-history groundwork (FR-020) retains each revision's full content, but
rendering a visual diff is UI complexity the timeframe doesn't justify; a moderator reviews the
proposed text in full, not a comparison view.

### CON-005 — Single-language interface

**Rationale:** The interface itself — navigation, labels, buttons — is in one language, English,
matching the documentation-language decision, even though article content mixes scripts (NFR-003).
Full UI translation is out of scope for the team size and timeframe.

### CON-006 — Accepted media types

**Rationale:** Only images, video, documents and audio are accepted as attachments — covering
everything the stakeholder's content genuinely needs (photos of forms, scanned documents,
instructional video or audio) — everything else, including executables and archives, is rejected
outright. A narrow allowlist keeps the upload surface small and reduces the attack surface for a
malicious file.
