# Manual walkthrough of every built screen

Phase 3 queue item 9 ([03-main-flow.md](../roadmap/03-main-flow.md)): one person uses everything that
is built, as a reader, a contributor and a moderator, in a desktop browser and at 360 px in DevTools'
device mode, before the mid-demo of 9 October. The automated checks (`scripts/check.sh`,
`-P browser`, `-P postgres`) say whether the behaviour is right; this says whether it is usable and
whether anything was missed. Walked by Mikhail; prepared and recorded by the assistant.

Mark each row in both columns: `ok`, `✗ F-n` (a finding below), or `—` with a reason when a width does
not apply. A finding is anything wrong, awkward or missing, however small.

## Before starting

- The stand runs from empty volumes: `docker compose down -v`, then `docker compose up --build`;
  open http://localhost:8080. Seed: 36 articles. No article is pinned (FR-025, editing the pinned
  list, is not built), so the landing page shows no "Pinned" section.
- The moderator's password is the one set in `.env` as `GUIDE_ADMIN_PASSWORD_HASH` for this walk.
- Test files: `/tmp/claude-1000/-home-remi-study-Exchange-Student-Guide-Platform/c169a059-c486-495c-affb-9c93ca66f506/scratchpad/walkthrough-files/`
  — `frro-form-photo.jpg` (a photo of a form), `hostel-rules.pdf`, `png-pretending.pdf` (a PNG named
  `.pdf`), `text-pretending.jpg` (text named `.jpg`), `too-big-photo.jpg` (25 MB, over the 10 MB photo
  limit). No MP4 could be generated on this machine: use a short video from a phone, or mark the
  video rows `—`.
- 360 px: DevTools → device toolbar → a 360-wide phone. Keep the console open throughout (section D).

## A. Reader

| # | Action | Expected | Desktop | 360 px |
|---|---|---|---|---|
| A1 | Open `/` | Hero, search box, "Recently added" cards, tags with their article counts, most visited first (FR-009, FR-031) | | |
| A2 | Open a card | Its article opens | | |
| A3 | Search "FRRO registration" | *Registering with FRRO* first (FR-007; demo step 1) | | |
| A4 | Search a word nothing contains, then an empty query | A "no results" page; the empty query is refused or explained, not an error page | | |
| A5 | Open a tag from the landing page, e.g. `visa` | `/tags/visa` lists only published articles with that tag (FR-008) | | |
| A6 | Go back to `/` | The tag just opened has moved up or kept its place by visits (FR-031) | | |
| A7 | Open *Attendance Requirements* | Markdown renders, including the table | | |
| A8 | Open *The Application Roadmap, at a Glance* and follow a wiki link | The link opens the article it names (FR-002) | | |
| A9 | On an article, look under the text | "What links here" lists the articles that link to it (FR-006) | | |
| A10 | Click a tag on an article | Its tag page opens | | |
| A11 | Press "Save as PDF" | The print dialog shows the article without the header, footer and buttons (FR-030) | | |
| A12 | Open `/articles/no-such-article` | The site's 404 page, in the site frame | | |
| A13 | After C5 approves the photo (come back here): open the article | The photo is under the text with a **Download** link (FR-001, FR-016) | | |
| A14 | Click the photo | Full screen, zoom, close with Esc and by tapping (FR-032) | | |

## B. Contributor

| # | Action | Expected | Desktop | 360 px |
|---|---|---|---|---|
| B1 | Click "Submit an article" in the header | `/submit` with the Markdown editor: toolbar and a preview beside the text (FR-027) | | |
| B2 | Type Markdown with `[[Registering with FRRO]]` and `[[No Such Article]]` | The preview updates as you type; the first link resolves, the second is red (FR-002, FR-004) | | |
| B3 | Type a paragraph in Hindi and one in Tamil | Typed and previewed unchanged | | |
| B4 | Reload the page | The draft comes back (FR-027) | | |
| B5 | Submit with the title "registering with frro" | Refused, with a link to propose an edit to the existing article; what was typed is kept (FR-010) | | |
| B6 | Submit with a blank summary, then with the title `!!!` | Refused with a message each time, the form kept | | |
| B7 | Attach `text-pretending.jpg`, then `png-pretending.pdf` | Refused: the type is read from the content (FEAT-009) | | |
| B8 | Attach `too-big-photo.jpg` | Refused with the limit named (NFR-001) | | |
| B9 | Give a title of your own, attach `frro-form-photo.jpg`, two tags, submit | The confirmation page with a `SUB-…` number, "Check its status", "Back to the guide" | | |
| B10 | Open `/submit` again | The draft is gone (cleared after sending) | | |
| B11 | "Check its status" | Pending (FR-012) | | |
| B12 | Look for the new title on `/`, in search and on its tag page | Not there until approved | | |
| B13 | Type the number in lower case without hyphens into the status page | Found | | |
| B14 | Type a number that was never issued | "Not found", not an error page | | |
| B15 | On *Registering with FRRO*, "Propose an edit" | The form filled in from the article (FR-011) | | |
| B16 | Change one sentence, attach `hostel-rules.pdf`, submit | A confirmation number; the article itself unchanged | | |
| B17 | Propose an edit to another article with the title "Registering with FRRO" | Refused, with a link to that article | | |
| B18 | Last of all: submit until refused (5 per hour) | The sixth answers "too many", not an error page (NFR-005) | | |

## C. Moderator

| # | Action | Expected | Desktop | 360 px |
|---|---|---|---|---|
| C1 | Open `/moderate/queue` without logging in | The login page | | |
| C2 | Log in with a wrong password, then the right one | Refused, then the queue | | |
| C3 | Look at the queue | B9's and B16's submissions, each by its number (FR-014) | | |
| C4 | Open B16's edit | The changed sentence shown as a diff, and the PDF (FR-029, FR-015) | | |
| C5 | Open B9's new article | Its text, the photo and the suggested tags (FR-015) | | |
| C5a | In C5, copy the photo's address (`/media/…`); open it in a private window, where you are not logged in | 404 while the submission waits: an unapproved file is reachable only by the moderator (FR-015) | | |
| C6 | Approve B9 after changing the summary and a tag | Back to the queue; the article is live, in search and on its tag page with the new summary and tags (FR-017) | | |
| C7 | Open B9's number again and press Approve or Reject | Refused as already decided (409) | | |
| C8 | Reject B16 with a reason | B11's status page for B16's number shows "rejected" and the reason (FR-018, FR-019) | | |
| C9 | Approve B2's link target: submit a new article titled "No Such Article", approve it | The red link of B2 now resolves on articles that use it | | |
| C10 | Remove an article that others link to (e.g. *Registering with FRRO*, after A-rows are done) | A confirmation page first; afterwards its address is 404, it is gone from `/`, search and its tags, and links to it are red (FR-026) | | |
| C11 | Log out | `/moderate/queue` asks for the login again | | |

## D. Across every screen

| # | Check | Expected | Desktop | 360 px |
|---|---|---|---|---|
| D1 | Every page visited above | No sideways scrolling, nothing cut off, buttons easy to press (NFR-008) | — | |
| D2 | The demo scenario (section E) from the keyboard alone | The focus is visible at every stop; nothing needs the mouse (NFR-007) | | — |
| D3 | The console, throughout | No Content-Security-Policy refusals, no failed requests | | |
| D4 | Back and reload after each form you sent | No "resubmit the form?" dialog that sends it twice; nothing breaks | | |
| D5 | Overall | Anything confusing, ugly, slow or missing: write it down, however small | | |

## E. The demo scenario, start to finish

The mid-demo rubric's sentence ([rubric.md](../course/rubric.md)): a student opens the landing page,
searches for "FRRO registration", reads the article, follows a wiki link to a related one, proposes
an edit with a photo of the form — the OGE moderator approves it in the panel and the change is live.
Do it once at desktop width on an article not removed in C10, noting where it drags.

| Step | Done | Notes |
|---|---|---|
| E1 Landing → search "FRRO registration" | | |
| E2 Read the article → follow a wiki link | | |
| E3 Propose an edit with `frro-form-photo.jpg` | | |
| E4 Log in → approve in the panel | | |
| E5 The change and the photo are live | | |

## Results of the walk, 1 October

Walked by the assistant in the human's Chrome (Claude in Chrome), desktop at 1568 and 1920 px and at
390 px, while the human walked the same screens. Passed as expected: A1–A5, A7–A10, A12; B1–B7,
B9–B17 (two `[walk]` submissions and the refusals); C1–C8, C10 (on the `[walk]` article, not on
*Registering with FRRO*, which the demo needs), C11; D1, D3, D4 on the pages above, D2 in part (the
focus is visible, the default blue). A13–A14 were checked afterwards on a third `[walk]` article (`SUB-A7A3-HMG7-T3FJ`, approved, viewed full screen with zoom, closed with Esc, then removed). Afterwards, on the human's request: B8 over HTTP (a 25 MB photo answers 422, "larger than 10 MB, the limit
for a photo"); B18 with the limit lowered to 2 for one restart (the third submission answers 429,
"Too many submissions from your network… Your text is still below", then the limit was set back);
the video row with a downloaded MP4 (refused — F-28); section E in one pass (F-30), on *Your First
Days on Campus*, which now carries the `[walk]` sentence and photo until the stand is reset with
`docker compose down -v`. A11 was checked by the human: "Save as PDF" works. Long input, asked about by the human, was checked afterwards over HTTP and in Chrome (F-31 to F-33): a 255-character title with spaces, one without, an emoji/RTL/Tamil title (renders correctly), a 2,140-character summary, 41 tags and a 154 KB body. **Not checked:** C9 (resolving a red link by publishing its title:
the resolution itself is shown by the editor's preview and by the article pages). The human's own walk adds to the
findings below.

## Findings

| ID | Row | What happened | Severity | Goes to |
|---|---|---|---|---|
| F-1 | A3, found while preparing | Right after a start from empty volumes, "FRRO registration" answered *Application Document Checklist* first: the search index is built from an empty table **before** the seed imports, and the application already answers while the import runs, so for its first seconds search sees only part of the seed. A minute later the right article is first. | awkward | [fixes 1.6](walkthrough-fixes.md) |
| F-2 | A1 | The landing page does not match its design screen (`docs/design/screens/Landing.html`): the header has only "Submit an article", not "Browse tags" and "Track a submission"; the hero sits as a box inside the column, left-aligned with a separate Search button, not full width and centred; the tags are a section under twelve cards, not a "Browse by tag" sidebar; cards are three across with the tags last and no date, not two across with a tag above the title and "Updated … ago". (No "Pinned" section is expected: FR-025 is not built.) Phase 3's open step "Templates brought up to the design screens" covers it. | wrong | [fixes 3.4](walkthrough-fixes.md) |
| F-3 | B18 | The submission limit could not be raised for a test on the stand: it is a setting (`guide.contribute.submission-limit`), but `docker-compose.yml` did not pass it to the container. Fixed during the walk: compose now passes `GUIDE_CONTRIBUTE_SUBMISSIONLIMIT_REQUESTS` (default 5) and `.env.example` documents it; this walk runs with 1000. | wrong | fixed in the walk, not committed |
| F-4 | A7 | 30 of the 36 seed articles end with a published section "Needs checking (with OGE) before this goes in front of students" listing open questions — an author's working note shown to every reader, including on *Registering with FRRO*, the demo's first article. | blocks the demo | [fixes 1.1](walkthrough-fixes.md) |
| F-5 | B2 | The hint under the editor reads "Link to another article with Title.": the template says `[[Title]]`, and Thymeleaf takes `[[...]]` in text for its own inline expression, so the brackets are eaten. The one place that teaches the wiki-link syntax does not show it. | wrong | [fixes 1.2](walkthrough-fixes.md) |
| F-6 | A1, header | The header offers only "Submit an article". The tracking page `/submissions/status` exists but nothing links to it except the confirmation page, so a contributor who closed that page cannot find it; there is no link to browse tags either. The design screen has "Browse tags" and "Track a submission" there. On a phone the one link wraps onto its own line. | wrong | [fixes 2.1](walkthrough-fixes.md) |
| F-7 | A1 | The landing page shows the twelve newest articles and nothing leads to the other 24 except search and tags: no "all articles" list, no pages. *Registering with FRRO*, the first thing every student needs, is not among the twelve; with no pinned articles (FR-025 unbuilt) the landing page has no way to put it first. | awkward | [fixes 2.2](walkthrough-fixes.md) |
| F-8 | A1 | The tag list at the bottom is in visit order, which with no visits yet reads as random (visa, academics, accommodation, admin…); the chips and counts are 12 px in maroon on cream, hard to scan. The design puts it in a sidebar as "Browse by tag". | awkward | [fixes 3.3](walkthrough-fixes.md) |
| F-9 | A3, A4 | Search results are the landing page's cards: no snippet around the match and no highlighting, so it is not clear why *Banks, ATMs and the Post Office* or *Connecting to the Campus Wi-Fi* answer "FRRO registration". An article matches if it holds any one word of the query (a `match` predicate with the default OR; first guessed to be Lucene syntax, which the code refutes): `<script>alert(1)</script>` finds 16 articles, `hostel" OR 1=1 --` all 36 (escaped on the page, so not a security hole, but noise). The "no results" page says "browse the tags on the home page" without a link. | awkward | [fixes 1.5](walkthrough-fixes.md) |
| F-10 | A7 | On an article, "Propose an edit" and "Save as PDF" are only at the very end of a long page, and look different (a link and a button). There is no "last updated" date (it matters for deadlines like FRRO's 14 days), the summary is not shown under the title, the right half of a desktop screen is empty (the design has a sidebar with "What links here" and a report button), external links such as the e-FRRO portal are not marked, and a long article has no contents list. | awkward | [fixes 3.5](walkthrough-fixes.md) |
| F-11 | B4 | The editor keeps a draft of the body only: after a reload the title and summary are empty again. | awkward | [fixes 3.6](walkthrough-fixes.md) |
| F-12 | B9 | The form's lower half, raised by the human during the walk: the five fixed tag boxes are clumsy — tags should be dynamic (type, Enter or comma makes a chip, remove with ×, suggestions from existing tags), not five unlabelled inputs wrapping four and one; the file input is the browser's grey "Choose File" button and looks out of place — the design screen has a drop zone ("Drop a photo, document, or video, or browse files") with the limits; the toolbar has no `[[wiki link]]` button and its icons have no visible labels. | wrong | [fixes 3.6](walkthrough-fixes.md) |
| F-13 | B5 | A refused form shows its message in a box wider than the form (about 1195 px against 962), and the field at fault is not marked: the message is at the top, not under the title, and the input is not flagged as invalid for a screen reader. | awkward | [fixes 3.6](walkthrough-fixes.md) |
| F-14 | A12 | The 404 page is a dead end: no search box, no suggestion of articles with a similar title, and no "write this article" (FR-005, a `could`, is not built). | idea | [fixes 3.8](walkthrough-fixes.md) |
| F-15 | C1 | The login page shows the public header's "Submit an article" and no way back to the guide; the tracking page and the login page are each a single field in an empty screen, with no line explaining what the number is or where it was given. | idea | [fixes 2.3, 3.8](walkthrough-fixes.md) |
| F-16 | A1 at 390 px | On a phone the search placeholder is cut ("…bank accou"). | awkward | [fixes 3.6](walkthrough-fixes.md) |
| F-17 | B1, A7, every page | Raised by the human twice: most of the screen is unused and the main part is narrow. Measured on a 1920 px window: an article's text is 736 px (38 % of the width, 742 px empty on the right), the submission form 736 px (38 %) with editor panes of 368 px each, the landing page's grid 1020 px (53 %). The same narrow column is used for the status, login and confirmation pages. | wrong | [fixes 3.1](walkthrough-fixes.md) |
| F-18 | B9 | After a refusal the attached file is gone (a browser cannot keep it) and the message does not say to attach it again; a contributor who fixes the title and resubmits silently loses the photo. | awkward | [fixes 3.6](walkthrough-fixes.md) |
| F-19 | B9 | The confirmation page shows the number, the only key to the submission, with no "copy" button; the status page shows only "Pending", not which submission (no title, no date), so someone with several cannot tell them apart. | idea | [fixes 2.4](walkthrough-fixes.md) |
| F-20 | B15 | The edit form's heading is "Propose an edit" without the article's name; "Cancel" goes to the landing page, not back to the article; on a long article the editor grows to the article's full height, so the toolbar scrolls out of sight (it should stay pinned); the refusal "Open that article" does not name the article. | awkward | [fixes 3.6](walkthrough-fixes.md) |
| F-21 | A5 | Cards differ between pages: a tag page shows "Updated 21 Sep 2026" on each card, the landing page does not. The tag page's heading is a small "Browsing by tag" over a chip, not a title. The keyboard focus ring is the browser's default blue, not the site's colours. | awkward | [fixes 3.2, 3.3](walkthrough-fixes.md) |
| F-22 | all screens | The human's summary of the walk: the design needs improving on almost every screen, not in one place. F-2, F-6, F-8, F-10, F-12, F-13, F-15, F-17, F-20 and F-21 are the instances found; they are better taken as one design pass over every screen, against the design screens, than as separate fixes. | wrong | [fixes 3](walkthrough-fixes.md) |
| F-23 | C3 | Times in the queue are in UTC: a submission sent at 14:58 on the tester's clock (UTC+3) shows "11:58" — 17:28 in Chennai. For an office in India they should be IST, and say so. | wrong | [fixes 1.3](walkthrough-fixes.md) |
| F-24 | C3, C6, C7, C10 | The moderator has no navigation of their own: the header still shows the public "Submit an article", "Log out" floats alone above the content, and there is no link from the panel to the site. No action confirms what it did: after Approve, Reject or Remove the queue simply reappears; a decided submission says "it was approved" with no link to the published article. The queue does not show whether a submission has a file attached. | awkward | [fixes 2.3](walkthrough-fixes.md) |
| F-25 | C4 | The edit's diff is drawn on the raw Markdown, with `**` and the source's hard line breaks, not on the rendered text; the summary to adjust before publishing is a single-line input that cuts a long summary off; the tags are the same five fixed boxes as the form (F-12); "Download" sits under the file's card instead of in it. The diff itself — word-level, added text in green, unchanged paragraphs folded — works well. | awkward | [fixes 3.7](walkthrough-fixes.md) |
| F-26 | C10 | The removal confirmation puts "Remove" and "Cancel" against each other with no gap, "Cancel" a small link; a tag left with no articles answers the 404 page whose text speaks of "no published article at this address". | awkward | [fixes 1.7, 3.2](walkthrough-fixes.md) |
| F-27 | A9, C10, B9 | Raised by the human: "Propose an edit" and actions like it are hard to read as actions. Not a contrast problem (9.5:1 against the page, 44 px tall): the article's main action is an underlined link in body weight and the same maroon as links in the text, beside "Save as PDF", which is an outlined bold button; "Remove this article", "Back to the guide", "Check its status" and "Cancel" are plain links again. No primary/secondary distinction and no icons, so nothing reads as a button except the one that matters least. | wrong | [fixes 3.2](walkthrough-fixes.md) |
| F-28 | B9 (video) | A real MP4 is refused as "not an accepted type". A 10-second clip from test-videos.co.uk (`ftypisom`, brand `isom` — what ffmpeg and many Android phones write) is detected by Tika as `video/quicktime`, and `AcceptedType` (`media/internal/AcceptedType.java`) accepts only `video/mp4`. Checked by running Tika with the application's own classpath on the file. Every `isom` MP4 will be refused the same way. | wrong | [fixes 1.4](walkthrough-fixes.md) |
| F-29 | C3 | Submission numbers are set in a small monospace font where 0 and 8 are easy to confuse at a glance: the assistant read `SUB-KX2M-A840-TGY2` as `A848` from the queue and reached a 404. Crockford base32 already avoids I, L, O and U; a larger, clearer face (or grouping with more spacing) would finish the job for a number people copy by hand. | awkward | [fixes 2.4](walkthrough-fixes.md) |
| F-30 | E | The demo scenario runs end to end (landing → search → *Registering with FRRO* → wiki link → *Your First Days on Campus* → edit with a photo → login → approve → the new text and photo are live). Where it drags: the actions are at the end of a long article (F-10), the editor of a long article is as tall as the article (F-20), and nothing confirms the approval (F-24). A wiki link that wraps onto two lines is fine to click by hand. | idea | — (the scenario passes) |
| F-31 | B9 with long input | **One submission breaks the landing page for everyone.** A 255-character title with no spaces (accepted: the limit is length only) overflows its card, runs over the next cards and makes the page scroll sideways: 2525 px wide in a 1920 px window, 2102 px in a 500 px one. The same title overflows the review page and stretches the queue table so that "Submitted" and "Review →" are off screen. `BrowserLayoutTest` cannot see it: its fixtures have ordinary titles. | blocks the demo | [fixes 1.8](walkthrough-fixes.md) |
| F-32 | B9 with long input | A summary has no length limit: 2,140 characters were accepted and the card shows all of it, a card as tall as the screen. Long titles (255 characters with spaces) take five lines in 32-px type on the review page. | wrong | [fixes 1.8](walkthrough-fixes.md) |
| F-33 | B9 with long input | The number of tags is not limited: one submission sent 41 tags (the form shows five fields, but the server takes any number), and the review page shows 41 inputs in a column, pushing "Approve & publish" far down. Each would become a row in `tag` on approval. A 154 KB body was accepted; no body limit was found either. | wrong | [fixes 1.8](walkthrough-fixes.md) |

Severity: **blocks the demo**, **wrong** (a requirement not met), **awkward** (met, but poor to use),
**idea** (not asked for). "Goes to" is decided with the human after the walk: a queue item in
[03-main-flow.md](../roadmap/03-main-flow.md), a `DEBT-XXX` in [tech-debt.md](../tech-debt.md), a
requirement (only with the human's yes), or dropped with a reason.
