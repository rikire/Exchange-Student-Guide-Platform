# Project proposal

**Revision 2 — 9 September 2026. Sent to the course the same day**, attached to the reply in
[scoping-feedback.md](scoping-feedback.md). Source: [proposal.tex](proposal.tex) · Built:
[proposal.pdf](proposal.pdf) (5 pages).

**It is now a submitted document, so it is frozen.** Editing `proposal.tex` would make this
repository disagree with what the course actually received — the same reason revision 1 stays
untouched in the repository root. A further change is revision 3, with its own file and its own
send date, not an edit to this one. That includes §9's first risk, which says the reply had not been
sent: it was true when the PDF went out, and correcting it in place would rewrite a submitted
artefact.

The `.tex` is the source of record and the `.md` is a pointer, not a copy — the same convention the
design document uses. A second prose copy would drift from the submitted PDF within a day, and the
submitted PDF is the artefact.

## Why there is a revision 2

The course returned scoping feedback on 28 August 2026 and made approval of the project conditional
on a reply describing the changes, due 1 September. That reply was not sent. Revision 2 is that
reply. The feedback, the mapping from each part of it to the requirement that answers it, and the
covering email are in [scoping-feedback.md](scoping-feedback.md).

## What changed from revision 1

| Section | Change |
|---|---|
| 1.1 Module ownership | Backend/Frontend split withdrawn; ten vertical slices, ownership claimed on start and measured from git |
| 2 Problem statement | Curated knowledge base → community-maintained wiki with an OGE moderation queue |
| 4 Prior work | MediaWiki and Confluence/Notion added — once the product is a wiki, "why not an existing wiki engine" is the question that matters |
| 5 Tech stack | REST API → server-rendered Thymeleaf; Spring Modulith named; Lombok dropped as unused |
| 7 Verification | Acceptance criteria rewritten for moderation: a submission no longer "appears immediately" |
| 8 Milestone plan | Revision 1's design-doc milestone was not met; stated plainly rather than restated |
| 9 Risks | Replaced with the four real ones, the first being that the project is not formally approved |
| Appendix B | New — the scoping feedback and our reply |

## Revision 1

`Project Proposal.pdf` in the repository root, dated 18 August 2026. It is left where it is: it is
the document that was actually submitted, and moving it would make the submission history harder to
read, not easier. [repository-map.md](../repository-map.md) puts graded documents under
`docs/course/`, which revision 2 follows.
