# Non-functional requirements

Properties of the system rather than things it does. Identified as `NFR-XXX`.

A non-functional requirement is only useful when it is measurable. "Fast" is not a requirement;
"search returns within 2 seconds over 100 articles of 500 words" is, because it can fail.

## Format

```markdown
### NFR-003 — Search latency

**Status:** planned
**Verified by:** the load fixture in docs/verification/fixtures.md

Search returns results within 2 seconds on a corpus of 100 articles of roughly 500 words each.
```

For an `NFR` marked `done`, the `**Verified by:**` line may stand in for a code and test anchor —
not every non-functional requirement is checked by a test, and pretending otherwise would push
people to write meaningless ones.

## Requirements

None yet. Written in phase 1.

Already known to belong here: search latency; the content being English with Hindi, Tamil and other
scripts mixed in, which the storage and the analyzers must both survive; upload size limits; and the
knowledge base being exportable to a human-readable format so that OGE is not locked in.
