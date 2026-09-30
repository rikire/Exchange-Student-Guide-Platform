# ADR-0019 — Rate limits per client address, in memory, owned by the slice that needs them

**Status:** accepted
**Date:** 2026-09-30

## Context

Three places are open to anyone and cost the server something on each request:

- the submission form's two POSTs;
- the editor's preview, which renders up to 100,000 characters;
- the moderator login, whose one shared password can be guessed.

[ADR-0008](ADR-0008-abuse-handling-without-accounts.md) decided that submissions are rate limited by
IP at NFR-005's rate, and left open where the count is kept. [ADR-0009](ADR-0009-admin-authentication.md)
decided that failed logins are limited too. Until now none of the three was limited (DEBT-011,
DEBT-017).

The limits the human confirmed on 30 Sep, per client address:

| Limit | Default | What counts |
|---|---|---|
| Submissions | 5 an hour | A new article and an edit, together. Only the ones accepted count. |
| Preview | 120 a minute | Every request. |
| Failed moderator logins | 10 in 15 minutes | Failures only. |

There is one instance with no accounts (CON-001). A restart is rare.

## Options

### Where the count is kept

**A. In memory.** Nothing to migrate and no write per request. A restart forgets the counts, and a
second instance would count separately.

**B. In the database.** The counts survive a restart. It costs a table, a migration past the schema
freeze, and a write on every preview.

### How the count is made

**C. Our own counter**, a map of timestamps per address. It is small, and it is exactly the kind of
code that is quietly wrong: windows, concurrency, memory growth.

**D. Bucket4j** (`com.bucket4j:bucket4j_jdk17-core` 8.20.0, Apache-2.0, checked on Maven Central on
30 Sep) for the token bucket, held in **Caffeine**, whose version Spring Boot manages. Caffeine
forgets an address once its window has passed and caps how many addresses it holds.

### Where the limiter lives

**E. One limiter in `shared`.** It would be written once. But `shared` holds only what every slice
uses (architecture-rules.md), and exactly two use this.

**F. A servlet filter.** It is written once, but a filter cannot re-render the form, so the typed
text would be lost. That was one of the confirmed requirements.

**G. Each owner keeps its own.** `contribute` owns the submission and preview limits, and
`shared/security`'s login owns the failed-login limit. About fifteen lines of Bucket4j and Caffeine
set-up are written twice.

## Decision

**A, D and G**, confirmed by the human on 30 Sep.

- **A.** The limit is a brake on volume, not a record. Losing the counts on a restart costs at most
  one more window.
- **D.** Refills, concurrency and bounded memory are solved problems.
- **G.** It keeps `shared` as the rules define it, and keeps the typed text on a 429.

**The address is `request.getRemoteAddr()`.** Headers the client writes, such as
`X-Forwarded-For`, are never read, since anyone can set them. A test sends a changing header and
stays counted as one address.

**Every attempt takes a token first.** A refused submission, or a login with the right password,
gives its token back. This way parallel requests cannot all pass one check, and only what should
count does.

**A refusal answers `429` with `Retry-After`.** The submission form comes back with the text typed.
The login form comes back too. The preview answers with a one-paragraph fragment, since the
editor's script puts any answer into the pane.

**Reaching a limit is logged as a `WARN`**, once per window and without the address.

**The limits are settings:** `guide.contribute.submission-limit`, `guide.contribute.preview-limit`
and `guide.admin.failed-login-limit` in `application.yml`.

## Consequences

**Good:**

- Volume from one address is bounded before anything is stored.
- The shared password can be tried 10 times in 15 minutes, not as fast as the server answers.
- OGE can change each limit without a rebuild.
- A test moves a clock instead of waiting an hour.

**Bad:**

- **A campus network behind one NAT address shares one count.** Five accepted submissions an hour
  from the whole of IIT Madras may be too few on a busy day. The limit is a setting for this reason.
- **Behind a reverse proxy, every client has the proxy's address** until the deployment sets
  `server.forward-headers-strategy` and the proxy overwrites the header. The compose stand has no
  proxy.
- **A restart resets the counts, and a second instance would double them.**
- **The ~15 lines of bucket set-up exist twice.**
- The challenge ADR-0008 also decided is still not built. This limits volume, not the first request.

**Reversal:**

- Moving to database counts (B) replaces the two holders and nothing else.
- A third slice needing a limit is the trigger to move the holder into `shared` (E).
