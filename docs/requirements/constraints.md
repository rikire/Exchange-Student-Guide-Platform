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
does not do, not how it is built. Something that is *not* a constraint goes to one of three places,
and the distinction is who the reader is:

| It is | It goes to |
|---|---|
| A decision about how the system is built, with alternatives that were weighed | an ADR in [docs/architecture/adr/](../architecture/adr/) |
| A standing structural convention the codebase follows | [docs/ai/architecture-rules.md](../ai/architecture-rules.md) |
| An instruction about how the assistant works | elsewhere in [docs/ai/](../ai/) |

**Corrected 10 September.** This rule previously sent every "framework-level or implementation
detail" to `docs/ai/architecture-rules.md` — which pointed architecture *into* the assistant's
instruction directory, and is how the security architecture came to live in `docs/ai/security.md`
and drift out of step with the ADR and the constraint that later superseded it. An architectural
decision has a wider audience than the assistant, and needs a home that says so.

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

### CON-004 — No stored diffs

**Rationale:** Version-history groundwork (FR-020) retains each revision's full content, not a diff
against the next one: a full copy is read whole, needs no patch applied to rebuild it, and costs
little at this size.

**Narrowed 29 Sep by the human:** until then this constraint also ruled out *showing* a diff, so the
moderator read an edit in full. Rereading a whole article to find one changed sentence is what the
moderator's review page is for, so [FR-029](functional.md#fr-029--seeing-what-an-edit-changes) now
shows it, computed when the page is rendered and never stored.

### CON-005 — Single-language interface

**Rationale:** The interface itself — navigation, labels, buttons — is in one language, English,
matching the documentation-language decision, even though article content mixes scripts (NFR-003).
Full UI translation is out of scope for the team size and timeframe.

### CON-006 — Accepted media types

**Rationale:** Only images, video and documents are accepted as media assets — covering
everything the stakeholder's content genuinely needs (photos of forms, scanned documents,
instructional video) — everything else, including audio, executables and archives, is rejected
outright. A narrow allowlist keeps the upload surface small and reduces the attack surface for a
malicious file.

**Narrowed 28 September by the human: audio removed.** It entered on 7 September from the answer
"maybe audio", and nothing since has needed it: no requirement, use case or stakeholder request, and
FR-010, FR-011 and NFR-001 already named only photos, documents and video — NFR-001 had no audio
size limit. The one use found for it, the pronunciation of Tamil or Hindi phrases, is served by a
link in the article's text. Each accepted type is more to detect, test and serve safely, so audio
returns only when OGE asks for it.

**Video widened 1 October by the human** (walkthrough F-28: a phone's MP4 was refused). Accepted
are the common formats as uploaded, with no conversion: MP4, MOV, M4V, 3GP, WebM, MKV, AVI, MPEG.
Ogg and WMV are not: the type detector reads them the same whether they carry a picture or only
sound, and audio stays out.

### CON-008 — No machine-facing API, and so no OpenAPI specification

**Rationale:** Every response the application produces is HTML for a person in a browser. There is
no separate client to serve — revision 2 of the proposal withdrew revision 1's REST API for exactly
that reason — and no requirement asks for machine access. The one integration-shaped requirement,
NFR-004, exports the knowledge base as plain Markdown files rather than over HTTP.

A generated specification would describe `text/html` responses carrying no schema: a document that
looks like a contract while specifying nothing checkable, which is worse than not having one,
because it invites trust it cannot repay. What is actually needed is in
[docs/architecture/ui-routes.md](../architecture/ui-routes.md) — path, slice, template, form fields,
response codes — and that contract additionally names the requirement each route serves, which an
OpenAPI document has nowhere to put.

Revisit if a real machine consumer appears, such as a mobile client or an OGE system pulling
articles. That is an ADR and a dependency decision, not something to add quietly.

### CON-009 — Two approvals under one address at the same moment are not answered gracefully

**Rationale:** When two moderators approve two different submissions whose titles share an address
at the same moment, the second one gets an error page (`500`) instead of the contracted `409`. OGE
moderates behind one shared password (ADR-0009), so two people approving same-titled submissions in
the same instant is not a realistic event. Nothing is harmed when it happens: the unique `slug`
refuses the second write, that approval rolls back, and its submission stays pending for a second
try, which then gets the ordinary `409`. Handling it would take a test that forces the race and code
that translates the database's refusal. The human judged that not worth it on 28 Sep, so it was
closed as DEBT-012.

### CON-010 — No CAPTCHA on the forms

**Rationale:** FR-013 asked for a CAPTCHA with its provider undecided. Every hosted one (reCAPTCHA,
hCaptcha, Turnstile) runs a script from another domain in the page, which the Content-Security-Policy
(`script-src 'self'`, ADR-0013) refuses, and sends each contributor's browser data to that company,
which the guide has avoided everywhere else. The abuse it stops is already bounded: five
submissions an hour from one address (NFR-005), every submission seen by OGE before it is public,
and the moderator's rejection. Decided by the human on 2 Oct; FR-013 keeps its rate-limit half,
which is done. Revisit if OGE reports spam the limit does not hold.
