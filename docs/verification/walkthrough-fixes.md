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

### 1.2 The wiki-link hint eaten by Thymeleaf (F-5)

`contribute/SubmissionForm.html` has `Link to another article with [[Title]].`; Thymeleaf reads
`[[...]]` in text as its own inline expression, so the page shows "with Title".

- **Change:** turn off inlining for that element (`th:inline="none"`) or write the brackets as
  `&#91;&#91;Title&#93;&#93;`. Look for any other `[[` in templates while there.
- **Check:** a test that renders `/submit` and finds `[[Title]]` in the page; red before the change.

### 1.3 Moderation times in UTC (F-23)

`moderate/internal/ModerationService.java` formats `submitted_at` with
`DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")` straight from the stored `OffsetDateTime`, which
is UTC. A submission sent at 17:28 in Chennai shows "11:58".

- **Change:** convert to the office's zone before formatting, from one setting
  (`guide.time-zone: Asia/Kolkata` in `application.yml`), and print the zone ("17:28 IST"). Use the
  same setting for the dates on cards (`taxonomy/internal/TagBrowseService.java`, F-21) and anywhere
  else a time is shown.
- **Check:** a test with a fixed `submitted_at` in UTC expects the IST time and the zone label.

### 1.4 A real MP4 refused (F-28)

An MP4 with the brand `isom` (what ffmpeg and many Android phones write) is detected by Tika as
`video/quicktime`; `media/internal/AcceptedType.java` accepts only `video/mp4`. Most videos from a
phone will be refused.

- **Decided 1 Oct by the human:** no conversion of every upload; a fixed list of accepted video formats.
  **Open:** the list, and whether `.mov` is worth converting. An iPhone records `.mov` in HEVC by
  default, which Chrome on many Windows and Android devices does not play (not verified per device).
  Converting means ffmpeg in the image, a background job with a "processing" state, HEVC to H.264
  re-encoding that takes CPU, and ffmpeg parsing files from anonymous visitors; a `.mov` already in
  H.264 only needs its container changed (`-c copy`), which is fast. Recommended for now: the MP4
  family (`isom`, `iso2`, `mp41`, `mp42`, `avc1`, `M4V`), stored as `video/mp4`, and a clear message
  for other formats.
- **Change:** in `media`, a second check after Tika: for `video/quicktime`, read the `ftyp` box and
  map the MP4 brands to `AcceptedType.MP4`.
- **Check:** tests with an `isom` file (accepted), an `mp42` file (accepted) and a real `qt  ` file
  (refused, or accepted under the other answer). The test file can be the 10-second clip from
  test-videos.co.uk the walkthrough used.

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

### 1.6 Search sees part of the seed right after a start (F-1)

`search/internal/SearchIndexBuilder` runs first (`@Order(HIGHEST_PRECEDENCE)`) and finds an empty
table; `backup/internal/SeedRunner` then imports the articles while the server already answers.

- **Change:** run the seed before the index builder, and have the builder fill the index after it.
- **Check:** after `down -v` and `up`, the first search for "FRRO registration" puts the FRRO article
  first.

### 1.7 Small wrong texts (F-26, F-14)

- A tag left with no articles answers the 404 page whose text speaks of "no published article at
  this address". **Change:** the 404 page names what was asked for (article, tag) or says "page".
- **Check:** `/tags/no-such-tag` shows a tag-specific message.

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

## 2. Navigation and the moderator's panel

### 2.1 The header (F-6)

Only "Submit an article". Nothing links to `/submissions/status` except the confirmation page, and
there is no way to browse tags.

- **Change:** header links "Browse tags", "Track a submission", "Submit an article", as in the design
  screen; on a phone, a menu button that opens them.
- **Decided 1 Oct by the human:** a new page `GET /tags` listing every tag with its article count (a
  route in `routes.yml`, a template, a test).
- **Check:** every page has the three links; at 390 px they are in the menu and each is 44 px.

### 2.2 All articles, not just the newest twelve (F-7)

The landing page shows the twelve newest; the other 24 are reachable only by search or tag, and
*Registering with FRRO* is not among the twelve.

- **Decided 1 Oct by the human:** an "All articles" page, alphabetical, paginated (a new route), and
  FR-025 built: a moderator's screen to pin and unpin articles, so OGE puts *Registering with FRRO*
  first. FR-025 is a `could` planned for phase 4; building it now moves it, which the roadmap records.
- **Check:** every published article is reachable in two clicks from `/`.

### 2.3 The moderator's own frame (F-24, F-15)

- **Change:** while signed in, the header shows the moderator's links (Queue, the site, Log out)
  instead of "Submit an article"; "Log out" moves into the header.
- **Change:** each action says what it did: after Approve — "Published: *Title*" with a link; after
  Reject — "Rejected *Title*"; after Remove — "Removed *Title*" (a flash message on the queue).
- **Change:** a decided submission links to the published article; the queue marks submissions with
  a file (📎 or "photo", "PDF", "video").
- **Change:** the login page gets a "Back to the guide" link.
- **Check:** tests for each message and link; the queue's attachment mark.

### 2.4 Times, numbers, copying (F-29, F-19)

- **Change:** submission numbers in a larger face where 0 and 8 are unmistakable, with more spacing
  between the groups; a "Copy" button beside the number on the confirmation page (a small script,
  no library: the Clipboard API).
- **Change:** the status page also shows the submission's title and when it was sent.
- **Decided 1 Oct by the human:** show the title and the date; the number is the authorisation
  already (ADR-0011).
- **Check:** tests for the title and date; the copy button is present and labelled.

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
- **Decision, open:** what builds the tag field and the drop zone. Researched 1 Oct (npm registry,
  WebJars on Maven Central, the bundles read for CSP-breaking code; not yet tried in a browser):

  | Library | Latest, WebJar | gzip | Licence | For us |
  |---|---|---|---|---|
  | Tom Select | 2.6.2, WebJar 2.6.2 | 17 KB + 1 KB CSS | Apache-2.0 | a real `<input>` (safe for Hindi and Tamil input); no inline style |
  | Tagify | 4.39.0, WebJar 4.37.1 | 20 KB + 2 KB | MIT | types into `contenteditable` — the input-method risk ADR-0013 avoided |
  | Choices.js | 11.2.4, WebJar 11.2.3 | 20 KB + 2 KB | MIT | one `setAttribute("style")`, which the strict policy may refuse |
  | FilePond | 4.32.12, WebJar 4.32.12 | 33 KB + 3 KB | MIT | uploads by its own request unless `storeAsFile`; previews need `filepond-plugin-image-preview` (last release Dec 2023, WebJar 4.6.11) |
  | Dropzone | 6.3.5, WebJar 6.0.0-beta.2 only | 11 KB | MIT | no current WebJar, so ADR-0013 rules it out |

  Recommended: Tom Select for tags; for the file, our own few lines over the real
  `<input type=file>` unless photo previews are wanted, in which case FilePond with its preview
  plugin. Either library is an addition to ADR-0013.
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
