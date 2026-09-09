# Glossary

One vocabulary, used identically in requirements, code, templates and conversation with the
stakeholder. Where the code and this document disagree, one of them is wrong and it is worth finding
out which.

Audited 10 September against every `FR`, `NFR` and `CON` in this directory, the three journeys in
[docs/cjm/](../cjm/), and the eleven screens in [docs/design/screens/](../design/screens/). Seven
terms the requirements already used were missing, and two concepts were running under two names
each — both recorded under [One name per concept](#one-name-per-concept) below.

## Content

| Term | Meaning |
|---|---|
| Article | A published page in the knowledge base: a title (unique among published articles, case-insensitively), a summary, a body of text written in Markdown (which may contain `[[wiki link]]` markup — not raw HTML, see [ADR-0001](../architecture/adr/ADR-0001-article-body-format.md)), a set of tags, and zero or more attached media assets. Content that is not yet approved is a **submission**, not an article |
| Summary | A short description of an article, written by the contributor and adjustable by the moderator. Shown under the title where articles are listed rather than read — tag browse, the landing page. Distinct from the ellipsed body extract search results show around a match, which is derived at query time and stored nowhere |
| Tag | A free-form label on an article; an article may carry several |
| Media asset | An uploaded file attached to an article. Its accepted types are fixed by [CON-006](constraints.md) and its size by [NFR-001](non-functional.md) |
| Pinned article | A published article a moderator has placed at the top of the landing page. Pinning is curation, not a property of the content: FR-009 shows pinned articles before recently added ones, and FR-025 is how the set changes |
| Published at | When an article was first published. Set once, and never changed by a later edit — this is what FR-009 means by "recently added" |
| Updated at | When an article's content last changed through an approved edit. Set at publication alongside `published at`, then moved forward by each approved edit |
| Revision | An article's retained content from before an approved edit changed it. Retained in full, because [CON-004](constraints.md) rules out rendering a diff |

## Contribution and moderation

| Term | Meaning |
|---|---|
| Contributor | Anyone who submits an article or an edit; no account is involved |
| Moderator | Whoever is working in the admin area of the site on OGE's behalf. Not a user account — [CON-001](constraints.md) rules those out, and the area is reached through a single shared password (see [docs/roadmap/03-main-flow.md](../roadmap/03-main-flow.md)). "The moderator" in a requirement therefore names a role someone is acting in, never a stored identity |
| Submission | A proposed new article or a proposed edit, waiting in the moderation queue. Carries the same authored fields as an article — title, summary, body, suggested tags, optional media asset |
| Submission type | Which of the two a submission is: a **new-article submission** or an **edit submission**. An edit submission also names the article it changes |
| Submission number | The identifier a contributor is shown when a submission is accepted into the queue, and the only handle they have on it afterwards — there is no account to look it up under. Also how the queue identifies a submission to the moderator |
| Submission status | One of **pending**, **approved** or **rejected**. Every submission is in exactly one, and FR-012 shows it to whoever holds the submission number |
| Moderation queue | The submissions currently pending, oldest first. Every new article and every edit passes through it, except a moderator's own direct publish (FR-023, FR-024) |
| Rejection reason | The moderator's optional explanation attached to a rejected submission, shown to the contributor alongside the status |
| Report | A flag on a published article, made by a reader, carrying a required message describing the problem; sits in the moderator's inbox until closed |

## Navigation between articles

| Term | Meaning |
|---|---|
| Wiki link | A reference from one article to another, written in the article text as `[[Title]]` |
| Red link | A wiki link pointing at an article that does not exist yet, rendered differently so a reader can tell before following it |
| Backlink | The reverse direction: which articles link to this one |

## Elsewhere

| Term | Meaning |
|---|---|
| Seed | The starter content shipped with the application, in the export format |

## One name per concept

Two concepts were running under two names each. Both are settled here rather than left to whichever
name a given document happened to use:

- **Media asset**, not *attachment*, in requirements and code. `Attachment` stays as the
  reader-facing label on screen — "Attachment (optional)" is what a contributor understands and
  "media asset" is not — so it is one concept with a deliberate interface word, not drift. Where a
  requirement said "attachment" it now says "media asset" (FR-015, FR-016).
- **Submission**, not *proposal*. FR-011 described an edit as a "proposal" while FR-017 and FR-020
  called the same thing an "edit submission"; FR-011 now uses `submission` throughout. The verb
  survives — a contributor still *proposes* an edit — but the thing that reaches the queue has one
  name.
