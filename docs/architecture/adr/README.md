# Architecture decision records

One file per decision, on [_TEMPLATE.md](_TEMPLATE.md). An ADR records a decision that is expensive
to reverse, together with the options that were genuinely weighed against it.

## ADR-0002 to ADR-0008 were written after the fact, on 10 September

They were written in one sitting, from the roadmap's list of decisions that needed recording — not
at the moment each decision was taken. That is the wrong order, and it is recorded here rather than
left for a reader to notice from the dates.

Why it matters, in the words of the objection that prompted this note: a record written at the
moment of deciding captures the decision that was actually made. A record reconstructed a week later
captures the decisions **someone remembered to look for** — so a decision nobody put on the list is
not merely undocumented, it is undetectable, because there is nothing left to compare the record
against.

Two things follow.

**The header carries two dates.** `**Decided:**` is the date the decision is traceable to, with the
evidence that dates it; `**Recorded:**` is when the ADR was written. Where those differ, the gap is
visible instead of being flattened into one date that would have claimed more than is true. This is
a deliberate deviation from `_TEMPLATE.md`, which has a single `**Date:**` — the template is right
for an ADR written when the decision is taken, which is what future ones should be.

**The list below is the audit**, not a claim of completeness. It is what a sweep of the roadmap,
`docs/ai/` and the requirements found. A decision that was made and never written anywhere would not
appear here either.

## Decisions that live outside this directory

Found on 10 September while writing ADR-0002 to ADR-0008. Each is a real architectural choice with a
rationale, recorded somewhere that is not an ADR. Whether each becomes one is a judgement call, and
an open one:

| Decision | Where it lives now | Dated |
|---|---|---|
| Spring Modulith's event registry stays switched off | [architecture-rules.md](../../ai/architecture-rules.md) | moved there 7 Sep |
| A tenth slice, `report`, added when three features fit none of the original nine | [01-requirements-design.md](../../roadmap/01-requirements-design.md) | 5 Sep |
| ~~A CAPTCHA challenge rather than a honeypot~~ | **moved** — [ADR-0008](ADR-0008-abuse-handling-without-accounts.md) | decided 6 Sep, moved 10 Sep |
| ~~Upload handling: type from content via Tika, allowlist, generated stored filename, images re-encoded~~ | **moved** — [security architecture](../security.md), under [ADR-0006](ADR-0006-media-storage-and-upload-security.md) | decided 3 Sep, moved 10 Sep |
| Design work: plain per-screen HTML committed, Code to Canvas rather than Figma's MCP server | [docs/design/README.md](../../design/README.md) | 7 Sep, revised 9–10 Sep |

**Resolved 10 September**, after the objection that `docs/ai/security.md` was the wrong home for architecture: everything security-shaped left that file for [docs/architecture/security.md](../security.md), and two decisions large enough for their own record became [ADR-0009](ADR-0009-admin-authentication.md) (the admin area's single password) and [ADR-0010](ADR-0010-bounded-reads.md) (bounded reads). The routing rule in [constraints.md](../../requirements/constraints.md) that had sent architecture into `docs/ai/` was corrected in the same pass. Two rows above are struck through accordingly; the first two remain open.

The design-process row is arguably process rather than architecture and may belong where it is. The first two
are architecture by any reading. `docs/ai/` is instructions for the agent, which is a different
audience from a decision record, so a decision that lives only there is one a reader of the
architecture will not find.

## For the ones still ahead

Phase 1 has not finished deciding. The route contract, the test plan and the seed front-matter shape
are all still open, and every one of them will produce decisions. Those get their ADR **when the
decision is taken**, in the same sitting, with a single `**Date:**` — which is the only version of
this that actually works.

**First one through, 10 September.** Reviewing [ui-routes.md](../ui-routes.md) the day it was written
produced two decisions, and both were recorded the same day rather than added to a list for later:

- [ADR-0011](ADR-0011-submission-number-format.md) — the submission number is an unguessable token,
  not a sequence. Single `**Date:**`, as this section requires.
- [ADR-0006](ADR-0006-media-storage-and-upload-security.md) gained a dated **amendment** rather than
  a second record, because media delivery is the topic it already owns. The amendment also resolves a
  contradiction between FR-016 and `security.md` that neither document could have revealed on its
  own — it only appeared when something tried to route both.

Worth noting against the objection at the top of this file: these two were found by *checking one
document against the others*, not by remembering to look for them. That is the mechanism the
after-the-fact ADRs lacked, and it is the argument for doing the review before the record is cold.
