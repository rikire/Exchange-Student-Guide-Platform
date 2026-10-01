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

**Status:** done — marked by the human on 28 Sep on FEAT-009's evidence: the four limits are
`guide.media.*` settings, and `MediaAssetsTest` enforces each at its configured value

An uploaded media asset's size does not exceed a configurable maximum that differs by file type,
and the whole media volume does not exceed a configurable maximum of its own. Both are application
settings, not hardcoded constants.

**Fit criterion:** by default — 10 MB per image, 20 MB per document, 500 MB per video, and 100 GB
for the whole media volume. Verified by a test asserting each configured limit is enforced, and by
`MediaConfigurationTest` and `UploadTooLargeTest` that the container's ceiling follows them.

Raised from 200 MB and 20 GB by the human on 1 Oct after the screen walkthrough: a minute of phone
video in 1080p is roughly 100–130 MB (typical figure, not measured), and OGE sets its own values on
its server.

### NFR-002 — Search latency

**Status:** done — measured on 30 Sep by `scripts/search-latency.sh` on the compose stand, as agreed
with the human: 100 searches on 100 articles of 500 words, the slowest in 0.023 s

Search returns results within a bound that keeps browsing usable on a modest corpus.

**Fit criterion:** Search returns results within 2 seconds on a corpus of 100 articles of roughly
500 words each.

**Verified by:** `scripts/search-latency.sh`, results in
[search-latency.md](../verification/search-latency.md).

### NFR-003 — Multilingual content survival

**Status:** planned

Article content mixing English with Hindi, Tamil and other non-Latin scripts is stored and searched
without corruption or loss.

**Fit criterion:** Storing and reading back an article whose body mixes English, Hindi and Tamil
text returns the exact original text unchanged, and a search query written in Hindi or Tamil script
matches an article containing that script.

### NFR-004 — Exportability

**Status:** in-progress

The knowledge base can be exported in full to a human-readable format, independent of the
application, so OGE is never locked into this system.

**Fit criterion:** An export produces a set of plain files — the same Markdown-with-front-matter
format used for seeding — containing every published article and its metadata, openable and
readable without running the application.

### NFR-005 — Submission rate limit

**Status:** done — accepted by the human on 1 Oct on the compose stand, built with
[FEAT-017](../features/FEAT-017-rate-limits.md)

A contributor's submissions are rate-limited by IP, at a configurable rate that tolerates a shared
campus network without blocking distinct contributors behind the same address.

**Fit criterion:** by default, 5 submissions per IP per hour, configurable as an application
setting.

**Verified by:** `RateLimitTest`: the sixth accepted submission in an hour from one address answers
`429`, and the limit is `guide.contribute.submission-limit` in `application.yml`
([ADR-0019](../architecture/adr/ADR-0019-rate-limits-per-client-address.md)).

### NFR-006 — Submission number unguessability

**Status:** done

A submission number cannot be arrived at by guessing, incrementing, or working backwards from
another one. It is the only thing standing between a stranger and a contributor's unapproved
submission, because [CON-001](constraints.md) leaves no account to check the holder against —
[ADR-0011](../architecture/adr/ADR-0011-submission-number-format.md).

**Fit criterion:** at least 60 bits of entropy per number, drawn from a cryptographically secure
source. Verified by a test asserting that numbers generated for consecutive submissions share no
ordering — sorting a batch by issue time does not sort it by value — and that the generator is seeded
from `SecureRandom` rather than a counter, a timestamp, or a content hash.

### NFR-007 — Accessibility

**Status:** done

Every screen can be used from the keyboard alone and with a screen reader, with enough contrast and
text that scales, so that a student with a visual or motor impairment can read, search and
contribute. Decided with the human on 27 Sep. Whether OGE, as part of a government institute, is
bound to a national standard (GIGW) that names a level was not asked: on 29 Sep the human decided
not to take such questions to OGE, so WCAG 2.2 AA stands as our own choice rather than a constraint.

**Fit criterion:** WCAG 2.2 level AA on every screen. Verified by an automated accessibility check
of each template in the build (axe, `BrowserLayoutTest`), and by an automated walk through the demo
scenario from the keyboard alone, the focus visible at every stop (`BrowserKeyboardTest`). The
manual pass with a screen reader was dropped by the human on 28 Sep; what a screen reader announces
is checked only as far as axe checks names and roles.

### NFR-008 — Usable at any screen width

**Status:** done

Every screen works from a small phone to a wide desktop, because most contributors and readers are
students on phones. Decided with the human on 27 Sep. WCAG 2.2 AA (NFR-007) already asks for reflow
at 320 CSS pixels and 24-pixel targets; this goes further on targets, since a phone is the main
device rather than an edge case.

**Fit criterion:** at viewport widths of 320, 768, 1280 and 1920 CSS pixels, no page scrolls
sideways; body text is at least 16 CSS pixels on a phone, so iOS does not zoom a form on focus; every
button, form control and navigation link is at least 44 by 44 CSS pixels at 320 and 768, links
inside running text excepted. Verified in a browser by the build's `browser` profile
([ADR-0014](../architecture/adr/ADR-0014-responsive-layout-and-browser-checks.md)).
