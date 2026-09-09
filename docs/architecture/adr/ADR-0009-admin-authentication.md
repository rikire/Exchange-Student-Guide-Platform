# ADR-0009 — One shared password for the admin area, not accounts

**Status:** accepted
**Decided:** 3 September — the rule is in `docs/ai/security.md` from that file's first commit
**Recorded:** 10 September, after the fact — see [README.md](README.md)

## Context

[CON-001](../../requirements/constraints.md) removes user accounts from the product. That settles
the contributor side cleanly — anyone submits, nobody registers — but it leaves a hole it does not
address: the moderator still has to be *someone the system will let in*. FR-014 through FR-019,
FR-023 through FR-026 are all actions that must not be available to a passing visitor.

So the moderator needs authentication while the product has decided against accounts. Those are not
actually in conflict, but only because "no user accounts" is a statement about the **product's
users** — readers and contributors — and the admin area is not part of the product's user model. The
glossary now says this explicitly under `Moderator`; before this ADR it was implicit, which is how
the same phrase came to describe both a person and a login.

The scale is one institute office: one or two OGE staff, sharing an inbox and a queue.

## Options

### A. No authentication — an obscure URL

The admin routes exist at a path nobody advertises.

Not a control. The route contract is a public document in this repository, the URLs appear in
templates, and a single leaked link is permanent. It would also make FR-018's rejection reason and
FR-026's removal available to anyone who found the path — destructive actions with no gate at all.

Listed because it is what "we have no accounts" degrades into if the question is never asked.

### B. One shared password, from an environment variable, stored as a BCrypt hash

Everyone in the office uses the same password. No user table, no registration, no password reset
flow, no personal data — so nothing is added back that CON-001 removed.

Costs attribution: the system cannot tell which moderator approved something, because there is
nothing to tell them apart. Rotation is manual and out-of-band — when someone leaves the office, the
password changes and everyone is told.

### C. Real accounts for moderators only

Per-person login, so an audit trail is possible and revoking one person does not disturb the others.

It reintroduces exactly what CON-001's rationale rejected — registration, password reset, personal
data handling — for a set of users that numbers one or two. It also creates a second class of
identity in a product whose whole simplicity comes from having none, and every screen showing "who
did this" becomes a feature nobody asked for.

## Decision

**B** — one shared password, taken from an environment variable and stored as a BCrypt hash, with
failed attempts rate limited and logged at `WARN`.

The deciding factor is proportion. C's only real advantage over B is per-person attribution, and no
requirement asks for it: no `FR` shows who moderated anything, and CON-004 already rules out the
diff view that would be the natural place to display it. Paying for registration and password reset
to get an audit trail nothing reads is the wrong trade at this size.

**What follows from B, rather than being a separate decision:** the password is never in
`application.yml` and never in git; the session cookie is `HttpOnly`, `SameSite=Lax`, and `Secure`
behind TLS; every destructive admin action is logged with what it affected, since the log is the
only trace of a decision the identity model cannot attribute.

## Consequences

**Good:** the admin area costs one environment variable and a filter chain; nothing that CON-001
removed comes back; a moderator who joins the office needs no provisioning step.

**Bad:** no attribution — "who approved this" is unanswerable by design, and the action log is the
nearest substitute. A shared password is only as good as the office's handling of it, and rotation
is a human process with no reminder attached. If OGE ever grows past a handful of moderators, or
asks who approved something, this is the decision that has to change.

**Reversal:** moving to option C means adding a user table, a login flow and a password reset path,
and amending CON-001 to say that it constrains the product's users rather than everyone. That is a
requirements change and a stakeholder conversation, not a refactor — the signal that it is due is
OGE asking for per-person attribution or for one moderator's access to be revoked without disturbing
the rest.
