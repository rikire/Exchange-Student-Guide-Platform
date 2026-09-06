# Functional requirements

What the system must do. Each requirement has an `FR-XXX` identifier that is referenced from feature
files, code anchors, migrations, the route contract and commit messages.

Written in phase 1. The proposal is not the source: it was written quickly to convey the idea, and
requirements are derived here from the product as agreed.

## Format

_(Illustrative example below — not a real entry. Real requirements start at `FR-001` in
[Requirements](#requirements).)_

```markdown
### FR-050 — Submitting a new article

**Status:** planned
**Priority:** must

When a reader submits a new article through the form, the system shall enter it into the moderation
queue.

IF a submission has not yet been approved, THEN the system shall not make it visible.

**Acceptance criteria:**

GIVEN a reader fills in the submission form
WHEN they submit it
THEN it enters the moderation queue
  AND it is not visible until approved
```

`**Status:**` is one of `planned`, `in-progress`, `done`, `out-of-scope`. What each status obliges
is in [docs/repository-map.md](../repository-map.md). `**Priority:**` is `must`, `should` or `could`.

An `out-of-scope` requirement also carries `**Rationale:**`.

The normative statement is written in EARS — see
[requirement-statements.md](requirement-statements.md) for the patterns and why. Every `FR` also
carries `**Acceptance criteria:**`, written as Given-When-Then scenarios — see
[acceptance-criteria.md](acceptance-criteria.md). An `NFR` does not use either: it stays a plain,
measurable property statement with a separate Fit Criterion — see
[non-functional.md](non-functional.md).

Use the established actor names only — "reader," "contributor," "moderator" — never "visitor" or any
other unestablished term.

## Requirements

### FR-001 — Reading a published article

**Status:** planned
**Priority:** must

When a reader requests a published article, the system shall show its content, composed as defined
for "Article" in [docs/requirements/glossary.md](glossary.md).

IF a submission has not yet been approved by moderation, THEN the system shall not resolve a route
to it.

**Acceptance criteria:**

```
GIVEN a published article
WHEN a reader requests its route
THEN the response is 200
  AND it shows the article's content

GIVEN a submission not yet approved by moderation
WHEN a reader requests its route
THEN the route does not resolve

GIVEN a submission that has been rejected
WHEN a reader requests its route
THEN the route does not resolve
```

### FR-002 — Rendering wiki links

**Status:** planned
**Priority:** must

When an article body containing `[[Title]]` is rendered, the system shall render it as a link to
the published article whose title matches `Title`, case-insensitively.

IF no published article matches `Title`, THEN the system shall render it as a red link.

**Acceptance criteria:**

```
GIVEN a published article titled "Title"
WHEN [[Title]] (any case) appears in a rendered article body
THEN it renders as a link to that article

GIVEN no published article matches "Title"
WHEN [[Title]] appears in a rendered article body
THEN it renders as a red link
```

### FR-003 — Writing wiki links into a submission

**Status:** planned
**Priority:** must

When a submission's body contains `[[Title]]` markup, the system shall store it unchanged.

**Acceptance criteria:**

```
GIVEN a submission body containing [[Title]]
WHEN the submission is stored
THEN the markup is preserved unchanged
```

### FR-004 — Red-link rendering

**Status:** planned
**Priority:** must

IF a wiki link's title matches no published article, THEN the system shall render it in red,
visually distinguished from an ordinary link.

**Acceptance criteria:**

```
GIVEN a wiki link with no matching published article
WHEN it is rendered
THEN it is styled red

GIVEN a wiki link with a matching published article
WHEN it is rendered
THEN it is not styled as a red link
```

### FR-005 — Creating an article from a red link

**Status:** planned
**Priority:** could

When a reader clicks a red link, the system shall take them to a page inviting them to create the
missing article.

**Acceptance criteria:**

```
GIVEN a red link
WHEN a reader clicks it
THEN they are taken to a page inviting them to create the article
```

### FR-006 — Backlinks on an article

**Status:** planned
**Priority:** could

When a published article's page is displayed, the system shall list the other published articles
that link to it via a wiki link.

IF a link to the article comes from a submission not yet approved by moderation, THEN the system
shall not count it until that submission is approved.

**Acceptance criteria:**

```
GIVEN a published article B linking to published article A
WHEN A's page is displayed
THEN B appears in A's backlink list

GIVEN a submission not yet approved by moderation, linking to article A
WHEN A's page is displayed
THEN that link does not appear in A's backlink list

GIVEN a rejected submission linking to article A
WHEN A's page is displayed
THEN that link does not appear in A's backlink list
```

### FR-007 — Full-text search across articles

**Status:** planned
**Priority:** must

When a reader searches a query, the system shall return the published articles whose title, body,
or tags contain any of the query's words, matched case-insensitively and independent of word form
(for example, singular/plural or verb tense), ranked so that closer and more complete matches
appear first.

IF a submission has not yet been approved by moderation, THEN the system shall exclude it from
search results.

**Acceptance criteria:**

```
GIVEN a published article whose title, body, or tags contain a word from the query
WHEN that query is searched
THEN the article appears in the results

GIVEN a published article matching more of the query's words than another
WHEN that query is searched
THEN it appears higher in the results

GIVEN only a submission not yet approved by moderation matches the query
WHEN that query is searched
THEN no result appears for it

GIVEN only a rejected submission matches the query
WHEN that query is searched
THEN no result appears for it
```

### FR-008 — Browsing articles by tag

**Status:** planned
**Priority:** should

When a reader selects a tag, the system shall return the published articles that carry it.

IF a submission has not yet been approved by moderation, THEN the system shall exclude it from tag
results.

**Acceptance criteria:**

```
GIVEN a published article carrying a tag
WHEN that tag is selected
THEN the article appears in the results

GIVEN a published article not carrying the selected tag
WHEN that tag is selected
THEN the article does not appear in the results

GIVEN only a submission not yet approved by moderation carries the tag
WHEN that tag is selected
THEN no result appears for it

GIVEN only a rejected submission carries the tag
WHEN that tag is selected
THEN no result appears for it
```

### FR-009 — Landing page

**Status:** planned
**Priority:** must

When a reader opens the landing page, the system shall show the most recently added articles, a
list of tags, and a search entry point.

IF any article is pinned, THEN the system shall show the pinned articles before the recently added
ones.

**Acceptance criteria:**

```
GIVEN at least one article is pinned
WHEN a reader opens the landing page
THEN the pinned articles are shown before the recently added articles
  AND a list of tags is shown
  AND a search entry point is shown

GIVEN no article is pinned
WHEN a reader opens the landing page
THEN the most recently added articles are shown
  AND a list of tags is shown
  AND a search entry point is shown
```

### FR-010 — Submitting a new article

**Status:** planned
**Priority:** must

When a contributor submits a new article, optionally attaching a photo, document or video and
suggesting tags, the system shall enter it into the moderation queue and show them a submission
number.

IF the article's title matches an existing article's title, case-insensitively, THEN the system
shall reject the submission, offering the contributor a link to propose an edit to the existing
article or the option to change their own title.

IF the attached media asset exceeds the configured size limit, THEN the system shall reject the
submission and show an error message.

IF the attached media asset is not of an accepted type, THEN the system shall reject the submission
and show an error message.

**Acceptance criteria:**

```
GIVEN a contributor fills in a new article, optionally attaching a photo, document or video and
suggesting tags
WHEN they submit it
THEN it enters the moderation queue
  AND they are shown a submission number

GIVEN a contributor's chosen title matches an existing article's title, case-insensitively
WHEN they attempt to submit
THEN the submission is rejected
  AND they are offered a link to propose an edit to the existing article
  AND they are offered the option to change their title

GIVEN an attached media asset exceeding the configured size limit
WHEN a contributor attempts to submit
THEN the submission is rejected
  AND an error message is shown

GIVEN an attached media asset not of an accepted type
WHEN a contributor attempts to submit
THEN the submission is rejected
  AND an error message is shown
```

### FR-011 — Proposing an edit to an existing article

**Status:** planned
**Priority:** must

When a contributor proposes an edit to a published article — changing its title, its body, or both,
and optionally attaching a photo, document or video and suggesting tags — the system shall enter it
into the moderation queue and show them a submission number.

IF the edit's proposed title matches a different existing article's title, case-insensitively, THEN
the system shall reject the proposal, offering the contributor a link to that existing article or
the option to change their proposed title.

IF the article being edited is no longer published, THEN the system shall reject the edit proposal.

IF the attached media asset exceeds the configured size limit, THEN the system shall reject the
proposal and show an error message.

IF the attached media asset is not of an accepted type, THEN the system shall reject the proposal
and show an error message.

**Acceptance criteria:**

```
GIVEN a contributor proposes an edit to a published article, optionally changing its title and/or
body, attaching a photo, document or video, and suggesting tags
WHEN they submit it
THEN it enters the moderation queue
  AND they are shown a submission number

GIVEN a contributor's proposed new title matches a different existing article's title,
case-insensitively
WHEN they attempt to submit
THEN the proposal is rejected
  AND they are offered a link to that existing article
  AND they are offered the option to change their proposed title

GIVEN the article being edited is no longer published
WHEN a contributor attempts to submit an edit to it
THEN the proposal is rejected

GIVEN an attached media asset exceeding the configured size limit
WHEN a contributor attempts to submit the edit
THEN the proposal is rejected
  AND an error message is shown

GIVEN an attached media asset not of an accepted type
WHEN a contributor attempts to submit the edit
THEN the proposal is rejected
  AND an error message is shown
```

### FR-012 — Looking up a submission's status

**Status:** planned
**Priority:** could

When a contributor enters a submission number, the system shall show its status: pending, approved,
or rejected.

IF the submission was rejected with a reason, THEN the system shall also show that reason.

IF the submission number does not exist, THEN the system shall show that no such submission was
found.

**Acceptance criteria:**

```
GIVEN a valid submission number
WHEN a contributor looks it up
THEN its status (pending, approved, or rejected) is shown

GIVEN a rejected submission that has a stored reason
WHEN a contributor looks it up
THEN the reason is also shown

GIVEN a submission number that does not exist
WHEN a contributor looks it up
THEN the system shows that no such submission was found
```

### FR-013 — Abuse handling without accounts

**Status:** planned
**Priority:** should

IF a contributor submits at a rate exceeding a configured limit, THEN the system shall reject
further submissions from them until the limit resets.

IF a submission does not pass a CAPTCHA challenge, THEN the system shall reject it.

The rate limit itself is set by NFR-005. The CAPTCHA provider is still undecided — a separate
dependency decision, not repeated here.

**Acceptance criteria:**

```
GIVEN a contributor has reached the configured submission rate limit
WHEN they attempt another submission
THEN it is rejected until the limit resets

GIVEN a submission that does not pass the CAPTCHA challenge
WHEN it is submitted
THEN it is rejected
```

### FR-014 — Moderation queue

**Status:** planned
**Priority:** must

When a moderator opens the queue, the system shall list every submission awaiting a decision,
identified by its submission number, ordered oldest first.

IF no submission is awaiting a decision, THEN the system shall show that the queue is empty.

**Acceptance criteria:**

```
GIVEN two submissions awaiting a decision, one older than the other
WHEN a moderator opens the queue
THEN the older one appears first

GIVEN no submission is awaiting a decision
WHEN a moderator opens the queue
THEN the system shows that the queue is empty
```

### FR-015 — Reviewing a submission

**Status:** planned
**Priority:** must

When a moderator opens a submission from the queue, the system shall show its full text and any
attachments.

IF the submission has already been decided, THEN the system shall show that it is no longer
pending.

**Acceptance criteria:**

```
GIVEN a submission awaiting a decision
WHEN a moderator opens it
THEN its full text and any attachments are shown

GIVEN a submission that has already been decided
WHEN a moderator attempts to open it
THEN the system shows that it is no longer pending
```

### FR-016 — Downloading a media attachment

**Status:** planned
**Priority:** should

When a reader requests a media asset attached to a published article, the system shall return it
for download.

IF a media asset is attached to a submission not yet approved by moderation, THEN the system shall
not return it when requested directly.

**Acceptance criteria:**

```
GIVEN a media asset attached to a published article
WHEN a reader requests it
THEN the file is returned for download

GIVEN a media asset attached to a submission not yet approved by moderation
WHEN a reader requests it directly
THEN it is not returned

GIVEN a media asset attached to a rejected submission
WHEN a reader requests it directly
THEN it is not returned
```

### FR-017 — Approving a submission

**Status:** planned
**Priority:** must

When a moderator approves a new-article submission, adjusting its tags if needed, the system shall
publish it as a new article.

When a moderator approves an edit submission, adjusting its tags if needed, the system shall update
the existing article with the proposed changes.

IF the submission has already been decided, THEN the system shall reject the approval attempt.

**Acceptance criteria:**

```
GIVEN a new-article submission awaiting a decision
WHEN a moderator approves it
THEN it is published as a new article

GIVEN an edit submission awaiting a decision
WHEN a moderator approves it
THEN the existing article is updated with the proposed changes

GIVEN a submission that has already been decided
WHEN a moderator attempts to approve it
THEN the approval is rejected
```

### FR-018 — Rejecting a submission

**Status:** planned
**Priority:** must

When a moderator rejects a submission, the system shall mark it as rejected and remove it from the
queue.

IF the submission has already been decided, THEN the system shall reject the rejection attempt.

**Acceptance criteria:**

```
GIVEN a submission awaiting a decision
WHEN a moderator rejects it
THEN it is marked as rejected
  AND it no longer appears in the queue

GIVEN a submission that has already been decided
WHEN a moderator attempts to reject it
THEN the rejection attempt is rejected
```

### FR-019 — Providing a rejection reason

**Status:** planned
**Priority:** could

When a moderator rejects a submission, they may include a reason; the system shall store it
alongside the rejection.

**Acceptance criteria:**

```
GIVEN a moderator rejects a submission
WHEN they include a reason
THEN the reason is stored with the rejection

GIVEN a moderator rejects a submission
WHEN they do not include a reason
THEN the rejection is stored without one
```

### FR-020 — Retaining article revisions

**Status:** planned
**Priority:** should

When an edit submission is approved, the system shall retain the article's content prior to the
change as a revision.

**Acceptance criteria:**

```
GIVEN a published article
WHEN an edit submission to it is approved
THEN the article's content prior to the change is retained as a revision
```

### FR-021 — Reporting an article

**Status:** planned
**Priority:** could

When a reader flags a published article, the system shall add it to the moderator's inbox of
reported articles.

**Acceptance criteria:**

```
GIVEN a published article
WHEN a reader flags it
THEN it is added to the moderator's inbox of reported articles
```

### FR-022 — Closing a report

**Status:** planned
**Priority:** could

When a moderator closes a report, the system shall remove it from the inbox.

**Acceptance criteria:**

```
GIVEN a report in the moderator's inbox
WHEN a moderator closes it
THEN it is removed from the inbox
```

### FR-023 — Publishing a new article directly

**Status:** planned
**Priority:** could

When a moderator writes a new article directly, optionally attaching a photo, document or video,
the system shall publish it immediately, without entering the moderation queue.

IF the article's title matches an existing article's title, case-insensitively, THEN the system
shall reject the publication.

IF the attached media asset exceeds the configured size limit, THEN the system shall reject the
publication and show an error message.

IF the attached media asset is not of an accepted type, THEN the system shall reject the
publication and show an error message.

**Acceptance criteria:**

```
GIVEN a moderator writes a new article directly, optionally attaching a photo, document or video
WHEN they publish it
THEN it is published immediately, without entering the moderation queue

GIVEN a moderator's chosen title matches an existing article's title, case-insensitively
WHEN they attempt to publish
THEN the publication is rejected

GIVEN an attached media asset exceeding the configured size limit
WHEN a moderator attempts to publish
THEN the publication is rejected
  AND an error message is shown

GIVEN an attached media asset not of an accepted type
WHEN a moderator attempts to publish
THEN the publication is rejected
  AND an error message is shown
```

### FR-024 — Editing an article directly

**Status:** planned
**Priority:** could

When a moderator edits an existing article directly — its title, its body, or both, optionally
attaching a photo, document or video — the system shall publish the change immediately, without
entering the moderation queue, retaining the article's content prior to the change as a revision.

IF the edit's proposed title matches a different existing article's title, case-insensitively, THEN
the system shall reject the publication.

IF the attached media asset exceeds the configured size limit, THEN the system shall reject the
publication and show an error message.

IF the attached media asset is not of an accepted type, THEN the system shall reject the
publication and show an error message.

**Acceptance criteria:**

```
GIVEN a moderator edits a published article directly, changing its title and/or body, optionally
attaching a photo, document or video
WHEN they publish the change
THEN it is published immediately, without entering the moderation queue
  AND the article's content prior to the change is retained as a revision

GIVEN an attached media asset exceeding the configured size limit
WHEN a moderator attempts to publish the change
THEN the publication is rejected
  AND an error message is shown

GIVEN an attached media asset not of an accepted type
WHEN a moderator attempts to publish the change
THEN the publication is rejected
  AND an error message is shown

GIVEN a moderator's proposed new title matches a different existing article's title,
case-insensitively
WHEN they attempt to publish
THEN the publication is rejected
```

### FR-025 — Editing the homepage's pinned articles

**Status:** planned
**Priority:** could

When a moderator pins an article, the system shall show it on the landing page ahead of the
recently added articles.

When a moderator unpins an article, the system shall remove it from the landing page's pinned
section.

**Acceptance criteria:**

```
GIVEN a published article
WHEN a moderator pins it
THEN it appears in the landing page's pinned section

GIVEN a pinned article
WHEN a moderator unpins it
THEN it no longer appears in the landing page's pinned section
```
