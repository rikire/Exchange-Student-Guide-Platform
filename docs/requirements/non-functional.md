# Non-functional requirements

Properties of the system rather than things it does. Identified as `NFR-XXX`.

A non-functional requirement is only useful when it is measurable. "Fast" is not a requirement;
"search returns within 2 seconds over 100 articles of 500 words" is, because it can fail.

## Format

```markdown
### NFR-003 — Search latency

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

None yet. Being re-derived alongside the `FR`s that need them, not in a separate pass — see
[docs/roadmap/01-requirements-design.md](../roadmap/01-requirements-design.md).

Already known to belong here: search latency; the content being English with Hindi, Tamil and other
scripts mixed in, which the storage and the analyzers must both survive; upload size limits; and the
knowledge base being exportable to a human-readable format so that OGE is not locked in.
