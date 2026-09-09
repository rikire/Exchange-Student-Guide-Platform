# Scoping feedback on the proposal, and our reply

The feedback the course returned on the proposal, and what we did about it. This is the evidence for
the **"Milestone plan revised in light of scoping feedback"** mark in the design rubric
([rubric.md](rubric.md)) — that criterion presupposes feedback, so the feedback itself has to be in
the repository, not only its consequences.

Recorded 9 September 2026, twelve days after it arrived. It had never been written down; it was
found only because the design document's revision section could not be written without it.

## What was received

**From:** Yuvraj Talukdar (CS23D009) `<cs23d009@cse.iitm.ac.in>`
**To:** `ge26z858@smail.iitm.ac.in`, `ge26z860@smail.iitm.ac.in`
**Cc:** Omkar Dhawal `<omkar@cse.iitm.ac.in>`, `cs25s011@smail.iitm.ac.in`
**Date:** Friday, 28 August 2026, 06:50 IST
**Subject:** CS5013 Project

The body is reproduced verbatim below. The full transport headers (DKIM, ARC, SPF) are in the
mailbox and are not reproduced here — they authenticate the message, they are not its content.

> Hello
>
> The idea is ok, but building a portal which contains only a few bits of information is not
> sufficient. We suggest building a wikipedia style portal, where information can be added / edited
> / removed based on the consent of the community. Provide us more details on the changes you will
> make to the proposal. Will will take the final decision on the approval of the project after that.
>
> Deadline- 1st Sep 2026

## Status: replied on 9 September, eight days after the deadline

**The 1 September deadline passed with no reply.** The reply went on **9 September 2026**, with
revision 2 of the proposal attached. The lateness stays in this record: it is what happened, and the
design document's risk section rests on it.

**Approval is still not confirmed.** The course made the decision conditional on receiving the
reply; the reply has now been received, and no answer has come back yet. Those are two different
states and only the first has changed.

## What the feedback asked for, and where each part now stands

| Asked for                                      | Where it is answered                                                                                                                                                                                                                                                                                                                             | Priority |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| Information can be **added** by the community  | [FR-010](../requirements/functional.md#fr-010--submitting-a-new-article) — anyone submits a new article, no account                                                                                                                                                                                                                              | must     |
| Information can be **edited** by the community | [FR-011](../requirements/functional.md#fr-011--proposing-an-edit-to-an-existing-article) — anyone proposes an edit to an existing article                                                                                                                                                                                                        | must     |
| Information can be **removed**                 | [FR-026](../requirements/functional.md#fr-026--removing-a-published-article) — added 9 September, in direct response to this email                                                                                                                                                                                                               | should   |
| **Consent of the community**                   | [FR-014](../requirements/functional.md#fr-014--moderation-queue), [FR-015](../requirements/functional.md#fr-015--reviewing-a-submission), [FR-017](../requirements/functional.md#fr-017--approving-a-submission), [FR-018](../requirements/functional.md#fr-018--rejecting-a-submission) — every submission passes an OGE-owned moderation queue | must     |
| More than "a few bits of information"          | 26 `FR`, 5 `NFR`, 7 `CON`, 26 use cases across three roles, ten vertical slices                                                                                                                                                                                                                                                                  | —        |

**Removal was the gap.** Until 9 September none of the 25 requirements covered removing a published
article: the word appeared only for taking a rejected submission off the queue, closing a report and
unpinning. The feedback names three verbs and we had written two. `FR-026` closes it.

**The wiki form was not a consequence of this email.** It was our own direction before the feedback
arrived, and the proposal of 18 August already had the stakeholder adding and editing articles
(§7, acceptance criteria). The feedback and our intent agreed; it did not redirect us. What it did
change is `FR-026`, and it makes the moderation queue a course requirement rather than only our
design choice.

## Our reply — sent 9 September 2026

Sent by: Mikhail Novikov. Attachment: revision 2 of the proposal
([proposal.md](proposal.md) → `proposal.pdf`). Sent verbatim as drafted below, so this text is the
text the course received, not a draft of it.

> Subject: Re: CS5013 Project — Team A (GE26Z858, GE26Z860): changes to the proposal
>
> Thank you for the feedback of 28 August, and our apologies for the late reply — we owed you this
> by 1 September and did not send it.
>
> We agree with the direction, and it matches the one we had already taken. The platform is
> specified as a community-editable, wikipedia-style knowledge base rather than a fixed set of
> topic pages:
>
> - Anyone can create an article or propose an edit to an existing one, **without an account**.
> - Every submission — a new article or an edit — enters a moderation queue owned by the Office of
>   Global Engagement, and becomes visible only once a moderator approves it. A moderator can also
>   correct or remove a published article.
> - Articles cross-reference each other with `[[wiki links]]`, including red links to articles that
>   do not exist yet, and every article shows what links to it.
> - Free-form tags, full-text search across the whole base, attached images, documents and video,
>   and export/import of the entire knowledge base.
>
> Your third verb — *removed* — was the one thing our requirements did not cover, and we have added
> it as a requirement rather than noting it as an intention.
>
> This is written up as 26 functional requirements, 6 non-functional ones, 7 recorded constraints
> and 25 use cases across three roles (reader, contributor, moderator), with the code organised as
> ten vertical slices. The updated proposal is attached; the sections that changed are 1.1, 2, 4, 5,
> 7, 8 and 9.
>
> We would be grateful for confirmation that the project is approved on this basis.
>
> Best regards,
> Mikhail Novikov (GE26Z858)
> Abdirakhim Ismailov (GE26Z860)

## Correction: three of the four figures in the sent reply are wrong

Found 9 September, after the reply had gone. The reply and the proposal both say "26 functional
requirements, 6 non-functional ones, 7 recorded constraints and 25 use cases". Counted properly, as
the repository stood when the reply went out:

| Stated | Actual | Why it was wrong |
|---|---|---|
| 26 `FR` | 26 | correct |
| 6 `NFR` | **5** | `NFR-050` is the illustrative example in the format section, not a requirement |
| 7 `CON` | **6** | `CON-040` is the illustrative example, likewise. CON-008 was added later the same day, so there are now genuinely 7 — the sent figure was wrong when it was written, and is not made right by that |
| 25 use cases | **26** | `UC-026` was added the same day, before the reply went, and the figure was not recounted |

The cause is worth naming because it recurred. The same trap in `functional.md` — `FR-050` in the
format section — was caught and excluded; the identical pattern in the other two files was not
checked, and the count was taken from a `grep -c` that included the example. One correct exclusion
was mistaken for a correct method.

Nothing about the scope is misstated: three counts of specification volume are out by one each, two
low and one high. It is recorded rather than quietly fixed because the figures went to the course,
and `proposal.tex` cannot be edited to match — it is the document they received.

## When an answer arrives

Paste it underneath. If it changes the scope it is scoping feedback in its own right and belongs in
this file, not only in somebody's memory of the thread. If none arrives before the design document
is submitted, that document says the approval is outstanding rather than assuming it.
