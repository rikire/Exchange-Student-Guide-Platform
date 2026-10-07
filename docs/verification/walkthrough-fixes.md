# Fixes from the screen walkthrough of 1 October

What [the walkthrough](manual-walkthrough.md) found, grouped into work that can be picked up one
package at a time. Each item names its findings (`F-n` in the walkthrough), what to change, where,
and how to know it is done. The phase 3 roadmap ([03-main-flow.md](../roadmap/03-main-flow.md),
"Fixes from the screen walkthrough") lists the packages and links here.

**Decision** marks an item whose answer is the human's: it changes a requirement, a constraint, an
ADR or a route, or it adds a screen. The recommended answer is given; nothing marked so starts
before the human picks one.

The usual cycle applies to every item that changes behaviour: a red test first, then the change
([workflow.md](../ai/workflow.md)); template and layout changes are held by `BrowserLayoutTest`
under `-P browser`.

## 1. Before the demo — content and bugs

### 1.1 Author notes published in the articles (F-4)

30 of the 36 seed articles end with "## Needs checking (with OGE) before this goes in front of
students" — open questions for the authors, shown to every reader, *Registering with FRRO* included.

- **Change:** take the section out of each article in `app/src/main/resources/data/seed/`. Keep the
  questions, they are real work: move them into one internal list, `docs/content/open-questions.md`,
  one heading per article.
- **Decided 1 Oct by the human:** remove them from the published text; the questions move to
  `docs/content/open-questions.md`.
- **Check:** `grep -l "Needs checking" app/src/main/resources/data/seed/*.md` finds nothing; the stand
  is reset (`docker compose down -v`) so the published copies change too, since the seed skips
  articles that already exist.
- **Done 1 Oct:** removed from 30 articles, the `## Photos` sections of six kept; the questions are
  in [open-questions.md](../content/open-questions.md). `SeedRunnerTest`'s
  `no_seeded_article_publishes_the_authors_open_questions` keeps it out of the seed. The stand reset
  waits for section 4.

### 1.2 The wiki-link hint eaten by Thymeleaf (F-5)

`contribute/SubmissionForm.html` has `Link to another article with [[Title]].`; Thymeleaf reads
`[[...]]` in text as its own inline expression, so the page shows "with Title".

- **Change:** turn off inlining for that element (`th:inline="none"`) or write the brackets as
  `&#91;&#91;Title&#93;&#93;`. Look for any other `[[` in templates while there.
- **Check:** a test that renders `/submit` and finds `[[Title]]` in the page; red before the change.
- **Done 1 Oct:** `th:inline="none"` on the hint, the only `[[` in the templates;
  `EditorPreviewTest`'s `the_form_shows_the_wiki_link_syntax_with_its_brackets`, red first.

### 1.3 Moderation times in UTC (F-23)

`moderate/internal/ModerationService.java` formats `submitted_at` with
`DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")` straight from the stored `OffsetDateTime`, which
is UTC. A submission sent at 17:28 in Chennai shows "11:58".

- **Change:** convert to the office's zone before formatting, from one setting
  (`guide.time-zone: Asia/Kolkata` in `application.yml`), and print the zone ("17:28 IST"). Use the
  same setting for the dates on cards (`taxonomy/internal/TagBrowseService.java`, F-21) and anywhere
  else a time is shown.
- **Check:** a test with a fixed `submitted_at` in UTC expects the IST time and the zone label.
- **Done 1 Oct:** `shared/web/DisplayTime`, chosen by the human over a formatter per slice, serves the
  queue (`1 Oct 2026, 17:28 IST`) and the tag page's dates; `guide.time-zone` in `application.yml`,
  `GUIDE_TIMEZONE` in `docker-compose.yml` and `.env.example`. The status page (2.4) and the article
  page (3.5) use it when they show a date.
- **Changed 1 Oct by the human: each reader sees their own zone**
  ([ADR-0020](../architecture/adr/ADR-0020-times-in-the-readers-own-zone.md)). The queue and the tag
  page wrap each time in `<relative-time>` (GitHub's element, from a WebJar deployed for it on 1 Oct,
  through `shared/web/LocalTime.html`). The queue reads "2 hours ago"; "Updated" is relative within
  30 days and a date after; the full local time is on hover. `DisplayTime`'s IST text stays inside as
  the fallback without scripts. `BrowserLocalTimeTest` checks it in Moscow with a stopped clock.

### 1.4 A real MP4 refused (F-28)

An MP4 with the brand `isom` (what ffmpeg and many Android phones write) is detected by Tika as
`video/quicktime`; `media/internal/AcceptedType.java` accepts only `video/mp4`. Most videos from a
phone will be refused.

- **Decided 1 Oct by the human:** the common video formats are accepted as uploaded, with no
  conversion and no editing on the server; rare and specialised formats are refused.
  - The list: MP4 and M4V (every MP4 brand, `isom`, `mp42` and the rest), MOV, WebM, MKV, AVI, 3GP,
    MPEG, OGG, WMV. Each is an `AcceptedType` keyed by the type Tika reads from the bytes, never by
    the extension or the type the browser sends; anything else is refused with the list in the
    message.
  - The server does not parse a video: no ffmpeg, no metadata reading. Its exposure stays at Tika's
    signature check.
  - Limits (NFR-001, raised 1 Oct, see 1.9): 500 MB per video, 100 GB for the media volume; NFR-005's 5
    submissions an hour; nothing reaches a public page before a moderator approves it.
  - A video keeps its metadata, including where it was filmed, which a phone writes into a `.mov`.
    Photos lose it in re-encoding; videos would not. Decided: warn the author under the file field,
    and add a line to the moderator's review page; no stripping.
- **Change, accepting:** the new `AcceptedType` rows, each stored as the type it was detected as,
  with its own extension. The form's `accept` attribute and the "accepted" text list the same set.
- **Change, playing on the site:** every video gets a `<video>` player; the browser's own decoders
  decide what plays, and a JavaScript player would not add any. MP4 with H.264, WebM and OGG play
  in every current browser; a `.mov` plays where the browser can decode its codec (H.264 in Chrome
  and Safari, HEVC from an iPhone in Safari and only on some Chrome systems — not verified per
  device); MKV, AVI, 3GP, MPEG and WMV will mostly not. A small script of our own (CSP allows
  `'self'`) listens for the player's `error` event and replaces it with "This video can't play in
  your browser — download it", the Download link (FR-016) staying under it either way. The formats
  known not to play inline (AVI, MPEG, WMV) skip the player and show the download card directly.
- **Change, delivery:** the stored type and `nosniff` on every response, as now;
  `Content-Disposition: attachment` for the types shown only as a download card.
- **Requirements touched at implementation, each with the human's yes:** CON-006 (the format list),
  FR-010 and FR-011 (what is accepted), `docs/ai/security.md` (the allowlist and the metadata note).
- **Check:**
  - one test per format with a real sample file, accepted and stored under its detected type; a
    renamed text file refused. Whether Tika's core detector tells WebM from MKV and recognises 3GP,
    AVI and WMV is not verified: the samples settle it, and a format it cannot detect is dropped
    from the list or brought back to the human;
  - a browser test: an MP4 plays inline, an unplayable file shows the download card;
  - the human uploads an iPhone `.mov` and an Android `.mp4` on the stand.
- **Accepting done 1 Oct (1.4a).** tika-core 3.3.2 probed with hand-made headers:
  - `isom`, `avc1` and `dash` MP4s, and a MOV with or without `ftyp`, are all `video/quicktime`, so the
    `ftyp` brand decides between MP4 and MOV;
  - WebM and MKV are both `application/x-matroska` and are stored as `video/webm`;
  - Ogg is `application/ogg` and WMV `video/x-ms-asf` with or without a picture, so both were dropped
    by the human.

  `MediaAssetsTest` has a test per format, red first. Playing, the download card and the metadata
  warning are 1.4b.
- **Showing done 1 Oct (1.4b).**
  - A player for every video but AVI and MPEG, which get only the card.
  - `videos.js` turns a player the browser cannot decode into the card.
  - The metadata warning on the form and the review page.
  - Delivery needed no change: every video was already sent with `Content-Disposition: attachment`,
    which a `<video>` ignores.
  - Tests: `MediaDeliveryTest`, `EditorPreviewTest` and `BrowserVideoTest`, the playable sample being
    `webm.webm` from github.com/mathiasbynens/small (no copyright).
  - Checked by the human on the stand: an iPhone `.mov` on 2 Oct and an Android `.mp4` on 8 Oct,
    each uploaded, approved by the moderator and played on the published article. Fix 1.4 is closed.

### 1.5 Search results are noise (F-9)

`search/internal/ArticleSearchService.java` uses a `match` predicate over title, body and tags with
the default operator, OR: an article matches if it holds any one word of the query. "FRRO
registration" also brings the Wi-Fi and bank articles; `<script>alert(1)</script>` finds 16 articles,
`hostel" OR 1=1 --` all 36. (The walkthrough first guessed that Lucene syntax was being parsed; the
code shows it is not.)

- **Change:** require every word (`simpleQueryString` with `defaultOperator(AND)`, or `match` with
  `minimumShouldMatch` near 100 %), and fall back to OR only when AND finds nothing, saying so on the
  page ("No article has all the words; showing articles with any of them").
- **Change:** show a snippet with the matched words highlighted (Hibernate Search's highlighter on
  `body`) under each result's title.
- **Check:** tests: "FRRO registration" returns only articles holding both words; the junk queries
  return none; a snippet holds `<mark>`.

**Done 2 Oct**, with the human's decisions of that day: FR-007 now says every word; no fallback to
fewer words, which would bring the junk queries back, and the empty page says no article holds all
of them, with a link to the tags; the passage is plain text, without Markdown; the result follows its
design screen, `SearchResults.html`. Recorded in FEAT-007, with the one limit found: the stemmer keeps
"registration" and "registering" apart, so a query in one form misses an article written only in the
other.

**The junk-query check reworded by the human, 2 Oct:** on the seed, `hostel" OR 1=1 --` finds 6
articles, each of which really holds "hostel", "or" and "1" ("1. Reach your hostel or guest house"),
where it found all 36 before. The check is now: the markup query finds nothing, and a query with
operators finds only articles holding every one of its words, never the whole guide.

### 1.6 Search sees part of the seed right after a start (F-1)

`search/internal/SearchIndexBuilder` runs first (`@Order(HIGHEST_PRECEDENCE)`) and finds an empty
table; `backup/internal/SeedRunner` then imports the articles while the server already answers.

- **Change:** run the seed before the index builder, and have the builder fill the index after it.
- **Check:** after `down -v` and `up`, the first search for "FRRO registration" puts the FRRO article
  first.

**Found on 2 Oct, before the change:** on a fresh stand the server answered at 34.6 s and the seed
finished at 35.9 s. For that second, search answered *0 results*, then the FRRO article first; the
wrong article first did not reproduce. The seed's own commit indexes its articles, so reordering the
two runners alone changes nothing a reader sees: both still ran after the server started. **Done
instead:** `SeedRunner` and `SearchIndexBuilder` are `SmartLifecycle` beans in phases before Spring
Boot's web server, seed first. `StartupOrderTest` reads the database and the index at the moment the
server starts (red first: no articles yet). On a fresh stand: seed 30.39 s, index 30.46 s, server
30.61 s; 420 searches from the first answer on, none empty, FRRO first in each.

### 1.7 Small wrong texts (F-26, F-14)

- A tag left with no articles answers the 404 page whose text speaks of "no published article at
  this address". **Change:** the 404 page names what was asked for (article, tag) or says "page".
- **Check:** `/tags/no-such-tag` shows a tag-specific message.

**Done 2 Oct:** `error/404.html` picks its text from the path in Spring Boot's error model: an
article, a tag ("No published article carries this tag", with a link to the tags), or any other
page. `NotFoundPageTest` checks the three through a real server; the tag and the other page were red
first.

### 1.8 Long input breaks the layout, and some input has no limit (F-31, F-32, F-33)

A 255-character title without spaces makes the landing page 2525 px wide in a 1920 px window and
pushes the queue's "Review" link off screen; a summary and the list of tags have no limits at all.

- **Change, layout:** every place that shows a title or a summary wraps long words
  (`overflow-wrap: anywhere` on titles, card text and table cells in `static/css/site.css`); the
  queue's title column truncates with an ellipsis and keeps "Review" visible; cards clamp the summary
  to three lines.
- **Change, limits (domain, not only the form):** a summary limit, a limit on the number of tags per
  submission, and a body limit, each refused with a message like the title's.
- **Decided 1 Oct by the human:** summary 300 characters, 10 tags, body 100,000 characters.
- **Change, test:** `BrowserLayoutTest` gains a fixture article with a 255-character unbroken title
  and a long summary, so this cannot come back unseen.
- **Check:** with that fixture, no page scrolls sideways at any width; tests for each new limit.
- **Limits done 1 Oct (Mikhail's half):** `Article.LONGEST_SUMMARY` (300) checked by the submission
  and the moderator's approval, the body's 100,000 (`BodyPreview.LONGEST_BODY`) by the submission,
  10 distinct tags by `Tags.named` for every path that writes tags; `maxlength` on the summary and
  body fields. A boundary pair for each in `SubmissionFlowTest`, `ModerationFlowTest` and `TagsTest`,
  red first.
- **Layout done 2 Oct (Abdirakhim's half):** `BrowserLayoutTest` gained an article and a pending
  submission with a 255-character unbroken title and a summary holding a long unbroken word, and
  measures its article page too; 18 page-and-width cases scrolled sideways first (landing, all
  articles, the tag page, the article page). `site.css` now breaks a too-long word anywhere in titles,
  cards and the article body, gives grid cards `min-width: 0`, clamps a card's summary to three lines,
  shortens a long title in the queue with an ellipsis (whole on hover and on the review page), and
  pins the queue's Review column to the table's right edge, where a phone scrolls the table inside
  itself; `the_queue_keeps_every_review_link_in_view_beside_a_long_title` was red at 320 px first.
  On the way: the search page's "Best match" badge from fix 1.5 was under 16 px on a phone (fixed
  with the other secondary text), and its classes collided with the forms' `.result-card`, so they
  are `search-hit*` now.

### 1.9 Larger uploads, and what they leave behind

Raised to the human on 1 Oct: 200 MB per video and 20 GB in all are too little.

- **Done 1 Oct:** NFR-001's defaults are 500 MB per video and 100 GB for the volume; each limit is
  set on the stand from `.env` (`GUIDE_MEDIA_*`); the container's multipart ceiling is derived from
  the limits in `media/internal/MediaConfiguration.java` instead of being a second number in
  `application.yml`. Checked by `MediaConfigurationTest` and `UploadTooLargeTest`.
- **Change, still to do — DEBT-016 (named DEBT-014 here until 1 Oct, which is the cookie's
  `Secure` flag), moved into phase 3 by the human:** a sweep that deletes the files
  and rows of assets whose submission was rejected longer ago than a set time (a `guide.media`
  setting). At 500 MB a video and 5 submissions an hour, one address can leave 2.5 GB an hour of
  rejected files.
- **Check:** a test that a rejected submission's asset older than the setting is swept, a newer one
  is kept, and a published article's asset is never swept; DEBT-016 closed.
- **Sweep done 1 Oct.** `MediaAssets.sweepRejected` removes the rows in the transaction and the
  files after it commits; `RejectedMediaSweep` runs it a minute after start-up and every 24 hours
  (the human: not at a set hour, which a stand switched off at night never reaches).
  `guide.media.rejected-kept-for` is 7 days, `GUIDE_MEDIA_REJECTEDKEPTFOR` on the stand.
  `RejectedMediaSweepTest` runs at 2 days and covers rejected, recent, pending and published assets
  and the freed volume. DEBT-016 is closed.

### 1.10 What else should be a setting

Reviewed 1 Oct and agreed with the human: a value becomes a setting when the office running the
stand may need its own; product rules and security bounds stay constants.

- **Settings:** the display time zone (1.3, `guide.time-zone`); NFR-001's limits (1.9, done); the
  age after which rejected files are swept (1.9). Already settings: NFR-005's limits, the moderator
  password, the index and media directories.
- **Constants, deliberately:** the landing page's card and tag counts and the search, backlink and
  tag-page sizes (`LandingPageService`, `ArticleSearchService`, `Backlinks`, `TagBrowseService`);
  the content limits — title 255 (a schema column), tag 64, rejection reason 2000, and 1.8's
  summary, tag-count and body limits; the security bounds — a photo's 50 megapixels, a submission
  number's 60 bits, the content-security policy, the 100,000 addresses a rate limiter remembers.
  Making one of these a setting is a way to weaken it without a code review.
- **Check:** each new setting has a default in `application.yml`, a line in `.env.example` if the
  stand sets it, and a test at a non-default value.
- **1 Oct:** the time zone done (`DisplayTimeTest` at `UTC`); the sweep age waits for 1.9.
- **1 Oct:** the sweep age done (`rejected-kept-for`, tested at 2 days); both settings are in.

## 2. Navigation and the moderator's panel

### 2.1 The header (F-6)

Only "Submit an article". Nothing links to `/submissions/status` except the confirmation page, and
there is no way to browse tags.

- **Change:** header links "Browse tags", "Track a submission", "Submit an article", as in the design
  screen; on a phone, a menu button that opens them.
- **Decided 1 Oct by the human:** a new page `GET /tags` listing every tag with its article count (a
  route in `routes.yml`, a template, a test).
- **Check:** every page has the three links; at 390 px they are in the menu and each is 44 px.

**Done 2 Oct:** four links, "All articles" added on Mikhail's note of 1 Oct, by the human's choice
over the screen's three; `GET /tags` by name with counts, up to 500; a `<details>` menu below
1024 px. Recorded in FEAT-008.

### 2.2 All articles, not just the newest twelve (F-7)

The landing page shows the twelve newest; the other 24 are reachable only by search or tag, and
*Registering with FRRO* is not among the twelve.

- **Decided 1 Oct by the human:** an "All articles" page, alphabetical, paginated (a new route), and
  FR-025 built: a moderator's screen to pin and unpin articles, so OGE puts *Registering with FRRO*
  first. FR-025 is a `could` planned for phase 4; building it now moves it, which the roadmap records.
- **Check:** every published article is reachable in two clicks from `/`.
- **Widened 1 Oct by the human:**
  - FR-033: the list ordered by title, update or views, and narrowed to a tag.
  - FR-034: each article's views.
  - FR-025: the pinned order set by hand.
  - V8, under ADR-0021.
- **List and views done 1 Oct:** [FEAT-018](../features/FEAT-018-every-article-and-its-views.md),
  `GET /articles`, linked from the landing page, which meets the check above. The seed carries
  made-up `views`. Still to do: the moderator's screen to pin, unpin and order (FR-025), and FRRO
  pinned in the seed.
- **Pinning done 1 Oct:** [FEAT-019](../features/FEAT-019-pinning-articles-in-order.md),
  `/moderate/articles`, after the HomeAdmin screen. The seed pins FRRO first.

### 2.3 The moderator's own frame (F-24, F-15)

- **Change:** while signed in, the header shows the moderator's links (Queue, the site, Log out)
  instead of "Submit an article"; "Log out" moves into the header.
- **Change:** each action says what it did: after Approve — "Published: *Title*" with a link; after
  Reject — "Rejected *Title*"; after Remove — "Removed *Title*" (a flash message on the queue).
- **Change:** a decided submission links to the published article; the queue marks submissions with
  a file (📎 or "photo", "PDF", "video").
- **Change:** the login page gets a "Back to the guide" link.
- **Added 1 Oct:** the moderator's header also links to "Homepage & articles", `/moderate/articles`
  (FR-025, FEAT-019). The human chose this over a temporary link, so until this item the screen is
  reached by its address.
- **Check:** tests for each message and link; the queue's attachment mark.

**Done 2 Oct:** while signed in, every page's header shows Queue, Homepage & articles, The guide and
Log out (`ModeratorFrame` in `shared/security` tells the shared header), and Log out left the page
bodies. After Approve the queue says "Published:" with a link to the article, after Reject
"Rejected", after Remove "Removed", each with the title, once (a flash attribute). A decided
submission's review links to the article while it is live: an edit's article by its id, a new
article at its title's address, since the submission keeps no link to it. The queue marks a file as
"photo", "PDF" or "video", read for the whole queue in one query. The login page has "Back to the
guide". Eight tests red first, in `ModerationFlowTest`, `ArticleRemovalTest` and `ModeratorLoginTest`.

### 2.4 Times, numbers, copying (F-29, F-19)

- **Change:** submission numbers in a larger face where 0 and 8 are unmistakable, with more spacing
  between the groups; a "Copy" button beside the number on the confirmation page (a small script,
  no library: the Clipboard API).
- **Change:** the status page also shows the submission's title and when it was sent.
- **Decided 1 Oct by the human:** show the title and the date; the number is the authorisation
  already (ADR-0011).
- **Check:** tests for the title and date; the copy button is present and labelled.
- **Done 1 Oct:** [FEAT-012](../features/FEAT-012-looking-up-a-submission.md).
  - FR-012 gained the title-and-date criterion.
  - The number is in its groups on all four pages, in the site's face (no new font).
  - Copy uses the Clipboard API, falling back to selecting the number where the API is missing (the
    plain-HTTP stand).
  - Tests: `SubmissionStatusTest`, `ModerationFlowTest`, `BrowserCopyNumberTest`.
  - Whether 0 and 8 now read apart is the human's look in a browser.

## 3. A design pass over every screen (F-22)

Raised by the human: almost every screen needs its design improved. Do it as one pass, against the
design screens, not as separate patches. Order: first the shared pieces (3.1–3.3), then each screen.

**Process:** redraw each screen's phone and desktop layout in `docs/design/screens/` first
(ADR-0014: the template follows its screen), have the human accept the screen, then change the
template. `Phones.html` shows both widths side by side.

### 3.1 Use the width (F-17)

On a 1920 px window an article and the form are 736 px (38 %), the editor panes 368 px each.

- **Change:** a wider page frame (`--page-width` in `static/css/tokens.css`) and layouts that use it:
  an article with a sidebar (3.4), the form with a wide editor (3.5), the queue and the review page at
  full frame width. Running text keeps a readable measure (about 70 characters) inside the wider
  frame; what grows is what sits beside it.
- **Check:** `BrowserLayoutTest` gains a rule: at 1280 and 1920 px the main content uses at least
  60 % of the window.

### 3.2 One set of buttons (F-27)

"Propose an edit" is an underlined link in body weight, "Save as PDF" an outlined bold button,
"Remove", "Cancel" and "Back to the guide" links again. Contrast is fine (9.5:1); the problem is that
nothing reads as an action.

- **Change:** three button styles in `site.css` — primary (filled maroon), secondary (outlined),
  quiet (text with an icon) — and use them everywhere: "Propose an edit" and "Submit for review"
  primary; "Save as PDF", "Download", "Check its status" secondary; "Cancel", "Back to the guide"
  quiet; "Remove" and "Reject" in a danger style. Buttons in a row get a gap (F-26).
- **Change:** a focus ring in the site's colours instead of the browser's blue (F-21).
- **Check:** a design screen with every button, accepted by the human; axe still clean.

### 3.3 One card (F-21, F-8)

Cards differ: a tag page shows "Updated 21 Sep 2026", the landing page does not.

- **Change:** one card fragment in `shared/web` used by the landing page, search, and tag pages:
  title, summary, tags, "Updated …".
- **Change:** the tag list as the design screen's "Browse by tag" sidebar; tags larger than 12 px;
  ordered by visits only once there are visits, alphabetically before.

### 3.4 The landing page (F-2)

Five differences from `docs/design/screens/Landing.html`: header links (2.1), a full-width centred
hero with the search box and its icon, a sidebar of tags (3.3), two cards across with a tag above
the title and a date, and a "Pinned" section (2.2).

- **Check:** the landing page compared with its screen side by side, differences listed and accepted
  by the human — the open phase 3 step "Templates brought up to the design screens".

### 3.5 The article page (F-10)

- **Change:** "Propose an edit" and "Save as PDF" at the top beside the title, not only at the end;
  the summary under the title; "Updated …" (in IST, 1.3); a sidebar on desktop with "What links
  here" and the article's tags; a contents list for an article with three or more headings; external
  links marked (an icon and `rel="noopener"` opening in a new tab).
- **Check:** a test for each element; `BrowserLayoutTest` at 390 px.

### 3.6 The submission and edit forms (F-12, F-13, F-18, F-20, F-11, F-16)

- **Change, tags:** one tag field that turns each word into a chip on Enter or comma, removes with ×,
  and suggests existing tags as you type (from the tag list the page already has); it still submits
  `tags` fields, so the server does not change.
- **Change, file:** a drop zone in the site's style ("Drop a photo, document or video here, or choose
  a file") with the limits, showing the chosen file's name and size and a way to remove it, instead
  of the browser's grey button.
- **Change, editor:** full frame width (3.1); a toolbar button for `[[wiki link]]`; labels on the
  toolbar icons (tooltips and `aria-label`); the toolbar stays pinned while a long body scrolls.
- **Change, errors:** the message beside the field at fault, the field marked `aria-invalid`, the
  error box no wider than the form; after a refusal, a line that the file must be chosen again.
- **Change, draft:** the draft keeps the title and summary as well as the body.
- **Change, edit form:** "Propose an edit to *Title*"; "Cancel" back to the article; the collision
  message names the other article.
- **Change, phone:** a search placeholder that fits ("Search the guide").
- **Decided 1 Oct by the human:** libraries, not our own scripts. Tags: **Tom Select**. File:
  **FilePond** with `filepond-plugin-image-preview`, in `storeAsFile` mode so the file goes with the
  form and the server does not change. Both as WebJars, an addition to ADR-0013 by a new ADR.
  Researched 1 Oct (npm registry, WebJars on Maven Central, the bundles read for CSP-breaking code;
  not yet tried in a browser):

  | Library | Latest, WebJar | gzip | Licence | For us |
  |---|---|---|---|---|
  | Tom Select | 2.6.2, WebJar 2.6.2 | 17 KB + 1 KB CSS | Apache-2.0 | a real `<input>` (safe for Hindi and Tamil input); no inline style |
  | Tagify | 4.39.0, WebJar 4.37.1 | 20 KB + 2 KB | MIT | types into `contenteditable` — the input-method risk ADR-0013 avoided |
  | Choices.js | 11.2.4, WebJar 11.2.3 | 20 KB + 2 KB | MIT | one `setAttribute("style")`, which the strict policy may refuse |
  | FilePond | 4.32.12, WebJar 4.32.12 | 33 KB + 3 KB | MIT | uploads by its own request unless `storeAsFile`; previews need `filepond-plugin-image-preview` (last release Dec 2023, WebJar 4.6.11) |
  | Dropzone | 6.3.5, WebJar 6.0.0-beta.2 only | 11 KB | MIT | no current WebJar, so ADR-0013 rules it out |

- **Check:** `SubmissionFlowTest` still passes unchanged (the server contract is the same); tests for
  the draft; `BrowserLayoutTest`; the human tries the form on desktop and phone.

### 3.7 The moderator's review page (F-25)

- **Change:** the diff of the rendered text as well as the Markdown (a toggle), the summary as a
  textarea, the tag field from 3.6, "Download" inside the file's card.
- **Check:** tests for the toggle and the textarea.

### 3.8 The small pages (F-14, F-15, F-19)

- The 404 page: a search box and "Did you mean" titles close to the address.
- The tracking and login pages: one line on what the number is and where it was given; a link back
  to the guide.

## 4. The stand before the demo

- Commit the walkthrough's change to `docker-compose.yml` and `.env.example` (F-3, done during the
  walk).
- Remove `GUIDE_CONTRIBUTE_SUBMISSIONLIMIT_REQUESTS=1000` from `.env`.
- Change the moderator's password: the one used in the walk is in the prompt journal.
- Reset the stand (`docker compose down -v`, `up --build`): *Your First Days on Campus* carries the
  walk's `[walk]` sentence and photo, which cannot be removed from the application.
