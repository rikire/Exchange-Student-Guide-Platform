# Non-functional requirements

Properties of the system rather than things it does. Identified as `NFR-XXX`.

A non-functional requirement is only useful when it is measurable. "Fast" is not a requirement;
"search returns within 2 seconds over 100 articles of 500 words" is, because it can fail.

## Format

_(Illustrative example below — not a real entry, hence a number outside the real range. Real
requirements start at `NFR-001` in [Requirements](#requirements).)_

```markdown
### NFR-050 — Search latency

**Status:** planned

Search returns results within a bound that keeps browsing usable on a modest corpus.

**Fit criterion:** Search returns results within 2 seconds on a corpus of 100 articles of roughly
500 words each. Verified by the load fixture in docs/verification/fixtures.md.
```

Following the [Volere](https://www.volere.org/) template: a plain statement of the property, plus a
separate **Fit Criterion** — the specific, numeric check that proves it. "If a fit criterion cannot
be found, the requirement is either ambiguous or poorly understood." A `Fit criterion` still pending
its numbers says so explicitly rather than inventing a plausible-looking one.

Unlike an `FR`, an `NFR` is never written as an EARS "the system shall" sentence — that phrasing
describes a behaviour (a functional concern), not a property. A rejection/validation behaviour that
an `NFR`'s limit implies (for example, rejecting an oversized upload) belongs in the `FR` that
triggers it, as an unwanted-behaviour clause, not here.

## Requirements

### NFR-001 — Upload size limit

**Status:** planned

An uploaded media asset's size does not exceed a configurable maximum that differs by file type,
and the whole media volume does not exceed a configurable maximum of its own. Both are application
settings, not hardcoded constants.

**Fit criterion:** by default — 10 MB per image, 20 MB per document, 200 MB per video, and 20 GB for
the whole media volume. Verified by a test asserting each configured limit is enforced.

### NFR-002 — Search latency

**Status:** planned

Search returns results within a bound that keeps browsing usable on a modest corpus.

**Fit criterion:** Search returns results within 2 seconds on a corpus of 100 articles of roughly
500 words each.

### NFR-003 — Multilingual content survival

**Status:** planned

Article content mixing English with Hindi, Tamil and other non-Latin scripts is stored and searched
without corruption or loss.

**Fit criterion:** Storing and reading back an article whose body mixes English, Hindi and Tamil
text returns the exact original text unchanged, and a search query written in Hindi or Tamil script
matches an article containing that script.

### NFR-004 — Exportability

**Status:** planned

The knowledge base can be exported in full to a human-readable format, independent of the
application, so OGE is never locked into this system.

**Fit criterion:** An export produces a set of plain files — the same Markdown-with-front-matter
format used for seeding — containing every published article and its metadata, openable and
readable without running the application.

### NFR-005 — Submission rate limit

**Status:** planned

A contributor's submissions are rate-limited by IP, at a configurable rate that tolerates a shared
campus network without blocking distinct contributors behind the same address.

**Fit criterion:** by default, 5 submissions per IP per hour, configurable as an application
setting.

### NFR-006 — Submission number unguessability

**Status:** planned

A submission number cannot be arrived at by guessing, incrementing, or working backwards from
another one. It is the only thing standing between a stranger and a contributor's unapproved
submission, because [CON-001](constraints.md) leaves no account to check the holder against —
[ADR-0011](../architecture/adr/ADR-0011-submission-number-format.md).

**Fit criterion:** at least 60 bits of entropy per number, drawn from a cryptographically secure
source. Verified by a test asserting that numbers generated for consecutive submissions share no
ordering — sorting a batch by issue time does not sort it by value — and that the generator is seeded
from `SecureRandom` rather than a counter, a timestamp, or a content hash.
