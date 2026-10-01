# Phase 3 — The main flow

**Status: in progress** (since 27 September, when `contribute` closed). Runs 21 September – 8 October 2026. Ends at the **mid-demo, 9 October**.

## Goal

The demo scenario working on real content: a student searches, reads, follows a wiki link, proposes
an edit with a photo; the moderator approves it and it goes live.

## Steps

Every check below is the same shape: **the acceptance criteria already written under the covering
requirement each become a named test.** Those criteria are in Given/When/Then form in
[functional.md](../requirements/functional.md), so the work of deciding what "done" means was done
in phase 1 and does not need redoing here. Where a step's check says more than that, it is because
the criteria do not reach it.

- [x] `contribute` — submitting a new article and proposing an edit to an existing one
      — check: a submission made through the form is in the queue and reachable from no public page;
      each covering FR's criteria is a test. **Added 25 Sep:** a title whose article address (slug,
      `wikilink`'s `ArticleAddress`) is already taken is rejected like a case-insensitive title
      collision, and the address is stored when the article is published — see
      [data-model.md](../architecture/data-model.md), `slug`.
      **Done 27 Sep:** [FEAT-005](../features/FEAT-005-submitting-an-article-or-an-edit.md), every
      criterion a test in `SubmissionFlowTest`, including the one that a submission is on no public
      page. Cut by the human: the attachment moves to the `media` step (DEBT-008); the edit's `409`
      waits for FR-026 (DEBT-009). Brought with it: CSRF through Spring Security, and `taxonomy`'s
      `Tags`, which closed DEBT-005.
      **Narrowed 10 Sep:** FR-023, FR-024 (direct publish, bypassing the queue) move to phase 4 with
      the admin-panel step below — see the revised milestone plan in
      [01-requirements-design.md](01-requirements-design.md).
- [x] `moderate` — the queue, approval, rejection; the state machine with its invariants
      — check: every transition has a test, and one test asserts that **no path publishes a
      submission that was not approved** — that is the invariant the whole slice exists for.
      **Added 25 Sep:** publishing computes `article.slug`, and approving an edit that changes the
      title recomputes it — which breaks the old address and links to it; see
      [data-model.md](../architecture/data-model.md), `slug`, and [FEAT-002](../features/FEAT-002-article-page.md).
      **Narrowed 10 Sep:** FR-019 (a rejection reason) and FR-026 (removing a published article) move
      to phase 4 — see [01-requirements-design.md](01-requirements-design.md).
      **Done 28 Sep:** [FEAT-006](../features/FEAT-006-moderating-a-submission.md) with the moderator
      login of ADR-0009. Every transition is a test in `ModerationFlowTest`, and
      `no_path_but_approval_changes_the_published_table` is the invariant; it goes red with the
      "already decided" guard removed. Decided by the human: the old address answers `404` after a
      rename (DEBT-010), the login is not yet rate limited (DEBT-011), an address taken while the
      submission waited refuses the approval, and FR-020's revision is written now. Media on the
      review page waits for `media`.
- [x] `wikilink` — the link parser, red links
      — check: a link to a missing article renders as a red link and one to an existing article
      resolves to its route; the parser's corner cases are derived the way
      [testing.md](../ai/testing.md) describes rather than guessed
      **Narrowed 10 Sep:** the "what links here" block (FR-006, backlinks) moves to phase 4 — see
      [01-requirements-design.md](01-requirements-design.md).
      **Done 28 Sep:** the renderer and red links were built in
      [FEAT-001](../features/FEAT-001-wiki-links-in-article-text.md) (25 Sep); the corner cases were
      then derived by the dimensions of testing.md, each now a test, five of them answers
      decided by the human, one of them an escape that stays a link (a code span shows the syntax).
      The link extractor of DEBT-006 waits for FR-006 in phase 4, by the human's decision.
- [x] `taxonomy` — tags, navigation by tag
      — check: browsing a tag returns exactly the published articles carrying it — an unapproved
      submission with that tag must not appear
      **Done 28 Sep:** [FEAT-008](../features/FEAT-008-browsing-by-tag.md), `GET /tags/{tag}`, every
      FR-008 criterion a test in `TagBrowseTest`. A tag's address follows the article address rule
      (decided by the human over five alternatives), and every tag chip is now a link.
- [x] `search` — indexing and querying, the English analyzer (renamed 28 Sep by the human from
      "both analyzers": the Hindi and Tamil one is NFR-003's)
      — check: FR-007's four criteria are four tests, including both negative ones (an unapproved
      submission and a rejected one must not appear in results)
      **Done 28 Sep:** [FEAT-007](../features/FEAT-007-searching-the-guide.md), every FR-007
      criterion a test in `SearchFlowTest`, the negative ones shown to fail with submissions indexed.
      One analyzer, English stemming; the second, for Hindi and Tamil, is NFR-003's and not built.
      The route contract's `400` for a blank query was kept over the contract's suggested answer.
      Closes DEBT-004.
- [x] `media` — upload, type detection from content, safe delivery
      — check: a file whose extension lies about its content is rejected, and media attached to an
      unapproved submission is unreachable by anyone who has not been given its id
      **Narrowed 10 Sep:** FR-016 (downloading a media attachment) moves to phase 4 — see
      [01-requirements-design.md](01-requirements-design.md).
      **Added 27 Sep:** the attachment on the submission form, cut from `contribute` (FEAT-005) —
      the `attachment` field, the upload handed to `media`, and FR-010's and FR-011's two media
      criteria each (over the size limit, not an accepted type) as four tests. Closes
      [DEBT-008](../tech-debt.md).
      **Built and accepted 28 Sep:** [FEAT-009](../features/FEAT-009-attaching-media-to-a-submission.md).
      Photos (JPEG, PNG, WebP), PDF and MP4, the type read by Tika; photos re-encoded and turned upright;
      NFR-001's limits as settings; `GET /media/{id}` serves an unapproved asset only to a moderator.
      Both checks above are tests, and DEBT-008 is closed. Narrowed by the human the same day: audio
      out of CON-006, DOCX out of the formats; the 50-megapixel ceiling kept, DEBT-015 put off and
      NFR-001 marked done. Accepted after the human uploaded camera photos from a phone and saw them
      upright on the review screen.
- [ ] ~~Admin panel behind the single password~~
      — check: a test enumerates the admin routes **from the route contract** rather than by hand,
      and asserts each one redirects when unauthenticated; a route added later without a test fails it
      **Moved to phase 4, 10 Sep** — it exists only to serve FR-023/FR-024 (direct publish), which
      moved with it. See [01-requirements-design.md](01-requirements-design.md) and
      [04-hardening.md](04-hardening.md).
- [x] Templates brought up to the design screens; the landing page brought up to the IITM reference
      — check: no literal colour or spacing value in the templates, only tokens; the landing page
      compared against the IITM reference side by side and the differences listed
      **Changed 27 Sep:** not generated from Figma. The human chose to draw layouts, phone ones
      included, directly in the HTML screens of `docs/design/screens/`, and templates follow those
      (ADR-0014).
      **Closed 2 Oct by fix 3.4:** the landing page follows `Landing.html`, accepted by the human in
      the browser; the other screens are the design pass items of the walkthrough fixes.
      — owner: **Abdirakhim** (closed by 3.4, the landing page)
- [x] Content: 30 or more articles in the database
      — check: the count comes from the database after import, and searching "FRRO registration"
      returns a relevant article — that is the first step of the demo scenario
      **Done 28 Sep:** ten articles added from OGE's International Student Handbook 2026 (arrival,
      onboarding, transport, Wi-Fi, banks, health, contacts, landmarks, food, safety), each citing
      its pages and listing what needs checking with OGE. The `seed` profile on H2 logs "30 articles
      imported", and "FRRO registration" returns `Registering with FRRO` first. Checked locally, not
      on the compose stand, which needs `POSTGRES_PASSWORD` in `.env`.
      **Checked on the compose stand 29 Sep:** see the demo run below.
- [ ] `ai-tools`: the gap list and rubric generators; the first honest gap list
      — check: `ai-tools gaps` (phase 3) produces a non-empty list containing at least one item
      neither of us would have volunteered
      — owner: **Mikhail**
- [ ] Ownership balance check — if it has drifted, the next tasks come from the lighter side
      — check: both members have authored commits in **every** week of this phase, with the hook's
      journal commits excluded — `sh scripts/contribution.sh` reports the two apart
      — owner: together
- [x] The moderator's pages in the browser check — `BrowserLayoutTest` visits `/moderate/queue` and
      `/moderate/submissions/{number}` without a session, so it measures the login page they redirect
      to, not them (found 28 Sep, after FEAT-006 met the layout of FEAT-003)
      — check: the test logs in first, and the queue and the review page pass NFR-008 and axe at the
      four widths
      **Done 28 Sep:** `BrowserLayoutTest` signs in before `/moderate/**` and fails on any redirect.
      Signed in, it found the queue 541 px wide at 320, and the Review link and the Reject button
      under 44 px; fixed, so NFR-008 is done. The same day `BrowserKeyboardTest` walked the demo
      scenario from the keyboard and found the editor trapping Tab with no focus ring; fixed, and
      NFR-007 is done without the manual screen-reader pass, which the human dropped.
- [ ] Weekly-log paragraphs in each member's own words for W37, W38 and W39 — the W37 and W38 files
      were generated on 28 Sep with the git figures only, and W39 still lacks Abdirakhim's paragraph.
      Each is marked as written after the week it covers
      — check: no file in `docs/team/weekly-log/` up to W39 still says "Not written yet"
      — owner: each member writes their own paragraph

## Queue raised on 27 September

Raised by the human after FEAT-005, taken **one at a time in this order** — the next is discussed
only when the one before is settled. Items 2, 3 and 5–8 change requirements or architecture, so each starts
as a discussion and becomes a requirement, an ADR or a step above only with the human's decision.
Where the steps above and this queue compete for the same days, the human orders them.

1. [x] **PostgreSQL profile** — `-P postgres` runs the tests on PostgreSQL in Testcontainers and
   fails when Docker is not running; closes DEBT-007 and lets FEAT-005 be marked done
   — check: the profile passes, and fails with `flyway-database-postgresql` removed
   **Done 27 Sep:** 164 tests pass on PostgreSQL 17.11; red with the Flyway module removed. The
   failure without Docker was not run, since the demo stand runs on the same Docker.
2. [x] **Markdown editor** — first the requirement (what a contributor must be able to do, on a
   phone, in Devanagari and Tamil, without JavaScript), then the library, from a sourced comparison
   — check: a requirement with acceptance criteria agreed, and a library chosen with its version,
   licence and WebJar verified
   **Decided 27 Sep:** FR-027 (must), FR-028 (`[[` completion, should), NFR-007 (WCAG 2.2 AA) and
   [ADR-0013](../architecture/adr/ADR-0013-markdown-editor-and-front-end-assets.md) — EasyMDE from its
   WebJar, server-rendered preview, a strict Content-Security-Policy, WebJars only. Building it is a
   separate contract, with a browser check under the policy and a Hindi and Tamil check on a phone.
3. [x] **Responsive layout** — a requirement for every screen from phone to desktop, and how to build
   it (own tokens and CSS, or a framework)
   — check: the requirement agreed; the approach recorded
   **Decided 27 Sep:** NFR-008 (320–1920 px, no sideways scrolling, 16-px text and 44-px targets on a
   phone) and [ADR-0014](../architecture/adr/ADR-0014-responsive-layout-and-browser-checks.md): our
   own CSS on the tokens, mobile first; a `browser` Maven profile with Playwright and axe-core that
   checks NFR-008 and NFR-007's AA at four widths on every built route; phone layouts drawn in the HTML
   screens of `docs/design/screens/`, not in Figma. Left for this item: building it, under its own
   contract.
   **Built 27 Sep:** mobile-first CSS on the tokens, Noto Sans from its WebJar, phone layouts in four
   HTML screens and `docs/design/screens/Phones.html` beside the desktop ones. `-P browser` runs
   `BrowserLayoutTest` over every built GET route and the not-found page: first red (every page
   scrolled sideways at 320 px, targets of 22–40 px, one axe failure), now green at all four widths.
   **Agreed next, 27 Sep:** `moderate` (with item 8's diff if CON-004 is revised), then `search`, then
   the content, then item 4; the questions for OGE (item 10) are worth asking now.
   **Update 28 Sep:** `moderate` was built by Abdirakhim (FEAT-006, `379f554`) while the queue was
   being worked, so the next step is `search`, then the content, then item 4. Item 8 now means adding
   the diff to FEAT-006's review screen. `BrowserLayoutTest` visits the moderation pages (nine pages
   at four widths, green on 28 Sep), but without a session, so it measures the login page they
   redirect to — corrected the same day; see the step on the moderator's pages above. Waiting for the human: whether CON-004 ("No diffs") is
   revised for item 8, and whether ADR-0014's wording, which still names `ui-routes.md` as the source
   of the pages, is brought in line with `routes.yml`.

4. [x] **Build the Markdown editor** — FR-027 and FR-028 on the `contribute` form, as ADR-0013
   decides: EasyMDE from its WebJar, a preview endpoint that renders with `WikiLinkRenderer` and caps
   the text's size, CDN downloads off, our own toolbar icons, the draft kept by EasyMDE's `autosave`
   and cleared after a successful submission, the strict Content-Security-Policy sent by every page
   from `shared/security`. The preview endpoint's missing rate limit is a debt entry until NFR-005.
   Comes after the responsive layout, so the editor goes into a layout that already fits a phone.
   — check: a test per FR-027 criterion; the editor works in a browser under the policy; typing
   Hindi and Tamil with Gboard on Android and the iOS keyboard does not duplicate, drop or reorder
   characters (if it does and no setting fixes it, TinyMDE is tried against the same checks); FR-028
   either works or is recorded as not done
   **Done 28 Sep:** [FEAT-010](../features/FEAT-010-markdown-editor.md). The preview
   at `POST /contribute/preview`, limited to 100,000 characters (DEBT-017 for the rate); the policy on
   every response; each FR-027 criterion a test, `BrowserEditorTest` under the policy with no
   violation. Decided by the human: FR-028 is a separate contract, and the phone check is dropped —
   FR-027 is done on the desktop result. Found: CodeMirror's `contenteditable` input, the one a phone
   uses, dropped Hindi and Tamil typed in desktop Chromium; that is the risk left unchecked.
5. [ ] **Frontend interactivity** — whether a lightweight library (htmx, Alpine.js or none) is
   added, and for what
   — check: the choice recorded as an ADR with the alternatives weighed
   — owner: together — a discussion; the human decides
6. [ ] **All screens, with features not yet built** — how screens of unbuilt slices are shown
   without fake data passing for real (for example fixtures only under a separate profile, each one a
   debt entry)
   — check: the option chosen; every screen in `docs/design/screens/` reachable
   — owner: together — a discussion; the human decides
7. [ ] **Email one-time code for submitting** — against spam and bots, a way to ban an address
   later, and approval status sent by email. Collects an email address but creates no account;
   touches CON-001's reason (personal data), ADR-0008 (which challenge), a mail server, and what OGE
   agrees to store
   — check: a requirement and an ADR agreed; what is stored and the mail server are ours to decide,
   OGE is not asked (item 10, closed 29 Sep)
   — owner: together — a discussion; the human decides
8. [x] **Diff view for the moderator** — reviewing an edit shows exactly what changed, where.
   Reverses CON-004 ("No diffs"); belongs to the review screen of `moderate` (FR-015)
   — check: CON-004 revised by the human and the requirement written
   **Decided and built 29 Sep:** CON-004 narrowed to "no stored diffs", FR-029 (`should`) written,
   [ADR-0015](../architecture/adr/ADR-0015-showing-an-edit-as-a-diff.md) — two columns chosen by the
   human over three alternatives, stacked on a phone, java-diff-utils 4.17. Built in
   [FEAT-006](../features/FEAT-006-moderating-a-submission.md).
9. [x] **Walk through every screen and feature** with the human, and turn what comes out into
   requirements and steps
   — check: the list of changes recorded
   **Done 1 Oct:** walked in Chrome by the assistant while the human walked the same screens —
   [manual-walkthrough.md](../verification/manual-walkthrough.md), 30 findings; the fixes are the
   section "Fixes from the screen walkthrough of 1 October" below.
10. [x] **Questions for OGE**, asked together rather than one meeting each: whether OGE is bound by
    GIGW and so by a WCAG level (NFR-007); what OGE agrees to store about contributors and which
    mail server may send from its name (item 7)
    — check: the answers recorded in `docs/stakeholder/` and turned into requirements or constraints
    **Closed 29 Sep without asking:** the human expects OGE to have no view on these and decided to
    take the better option ourselves. NFR-007 keeps WCAG 2.2 AA as our own choice; item 7 decides
    what is stored and which mail server sends when it is taken up.

11. [x] **A route specification that code and tests can read** — raised by the human on 27 Sep.
    `ui-routes.md` is the route contract, and CON-008 rules out OpenAPI because every response is
    HTML, but it is a Markdown table: nothing checks that it matches the controllers, and a test that
    needs the list of routes would have to parse prose. Options to weigh: a test that compares the
    controllers' mappings with the table in both directions; or a machine-readable route file from
    which the table is generated
    — check: the option chosen by the human, and a route added to code without the contract (or the
    other way round) fails the build
    **Done 27 Sep:** the human chose the machine-readable file. `docs/architecture/routes.yml` holds
    every route and deferred feature; `ai-tools routes` writes the two tables of `ui-routes.md` from
    it, byte for byte the tables it replaced, and `routes --check` in `check.sh` fails when they are
    stale; `RouteContractTest` compares the `built` routes with Spring's mappings in both directions,
    shown red each way.

## Fixes from the screen walkthrough of 1 October

Found by the [walkthrough](../verification/manual-walkthrough.md) (queue item 9): 33 findings, each
linked from its row to the package that fixes it. What to change, where and how to check it is in
[walkthrough-fixes.md](../verification/walkthrough-fixes.md); a **decision** is the human's and is
settled before that item starts. Order: section 1 and 2.1 before the mid-demo of 9 October; the
design pass (3) is the largest piece and starts with redrawn screens.

- [x] [Take the author notes out of 30 seed articles (F-4)](../verification/walkthrough-fixes.md#11-author-notes-published-in-the-articles-f-4) — decided 1 Oct: removed from the text, kept in `docs/content/open-questions.md`
      — check: `grep "Needs checking"` over the seed finds nothing, and the reset stand shows none
      — owner: **Mikhail**
      **Done 1 Oct:** the sections of 30 articles moved verbatim to
      [open-questions.md](../content/open-questions.md); `SeedRunnerTest` asserts no seeded article
      publishes one, red before the change. The reset stand is section 4's.
- [x] [Show `[[Title]]` in the editor's hint (F-5)](../verification/walkthrough-fixes.md#12-the-wiki-link-hint-eaten-by-thymeleaf-f-5)
      — check: a test finds `[[Title]]` on `/submit`, red first
      — owner: **Mikhail**
      **Done 1 Oct:** `th:inline="none"` on the hint; `EditorPreviewTest` red first. No other `[[` in
      the templates.
- [x] [Show times in IST from one setting (F-23)](../verification/walkthrough-fixes.md#13-moderation-times-in-utc-f-23)
      — check: a test with a UTC time expects the IST time and label
      — owner: **Mikhail**
      **Done 1 Oct:** `shared/web/DisplayTime` (`shared` grown by the human's choice over a copy per
      slice) formats the queue's time and the tag page's date from `guide.time-zone`; red first in
      `ModerationFlowTest`, `TagBrowseTest` and `DisplayTimeTest`.
      **Changed the same day by the human:** each reader sees their own zone, IST as the fallback
      ([ADR-0020](../architecture/adr/ADR-0020-times-in-the-readers-own-zone.md), accepted),
      `BrowserLocalTimeTest`.
- [ ] [Accept the MP4 files phones write (F-28)](../verification/walkthrough-fixes.md#14-a-real-mp4-refused-f-28) — decided 1 Oct: the common formats (MP4, MOV, WebM, MKV, AVI, 3GP, MPEG, OGG, WMV) as uploaded, no conversion; play on the site where the browser can, a download card otherwise; warn about location metadata
      — check: tests with `isom`, `mp42` and `qt` files
      **Accepting done 1 Oct:** eight formats; Ogg and WMV dropped by the human (Tika cannot tell them
      from audio). **Showing done 1 Oct:** a player, a card when the browser cannot decode it, AVI
      and MPEG as a card only, the metadata warning. Left: the human's iPhone `.mov` and Android
      `.mp4` on the stand, which closes this item.
      — owner: **Mikhail**
- [x] [Make search require every word, with snippets (F-9)](../verification/walkthrough-fixes.md#15-search-results-are-noise-f-9)
      — check: "FRRO registration" returns only articles with both words; the markup query returns
      none, and a query with operators only articles holding all its words (reworded 2 Oct)
      Built 2 Oct: FR-007 changed to every word, a marked passage of plain text, the result as its
      design screen draws it (FEAT-007). Accepted by the human on 2 Oct on the stand.
      — owner: **Abdirakhim**
- [x] [Seed before indexing at start-up (F-1)](../verification/walkthrough-fixes.md#16-search-sees-part-of-the-seed-right-after-a-start-f-1)
      — check: after `down -v` and `up`, the first FRRO search is right
      Built 2 Oct: the seed and the index build run before the web server starts
      (`StartupOrderTest`, red first); a fresh stand gave 420 searches, none empty. Accepted by the
      human on 2 Oct.
      — owner: **Abdirakhim**
- [x] [A 404 text that fits what was asked for (F-26)](../verification/walkthrough-fixes.md#17-small-wrong-texts-f-26-f-14)
      — check: `/tags/no-such-tag` says it is a tag
      Built 2 Oct: the 404 page names an article, a tag or a page (`NotFoundPageTest`). Accepted by
      the human on 2 Oct.
      — owner: **Abdirakhim**
- [x] [Long input must not break the layout; limits for summary, tags and body (F-31, F-32, F-33)](../verification/walkthrough-fixes.md#18-long-input-breaks-the-layout-and-some-input-has-no-limit-f-31-f-32-f-33) — decided 1 Oct: summary 300, 10 tags, body 100,000
      — check: a fixture with a 255-character unbroken title scrolls no page sideways; a test per limit
      Layout built 2 Oct: the long-title fixture in `BrowserLayoutTest`, red first. Accepted by the
      human on 2 Oct.
      — owner: **Abdirakhim** the layout and the `BrowserLayoutTest` fixture; **Mikhail** the three limits
      **Limits done 1 Oct** (summary, body, tag count, in the domain and the form's `maxlength`);
      the layout half is still to do.
- [x] [Larger uploads: 500 MB a video, 100 GB in all, from `.env`; sweep rejected files (DEBT-016)](../verification/walkthrough-fixes.md#19-larger-uploads-and-what-they-leave-behind) — decided 1 Oct; the limits done 1 Oct, the sweep to do
      — check: `MediaConfigurationTest`, `UploadTooLargeTest`; a sweep test that never touches a published asset
      — owner: **Mikhail**
      **Done 1 Oct:** the sweep, a minute after start-up and every 24 hours, 7 days by default
      (`RejectedMediaSweepTest`); DEBT-016 closed. The debt was named DEBT-014 here until then.
- [x] [What becomes a setting and what stays a constant](../verification/walkthrough-fixes.md#110-what-else-should-be-a-setting) — decided 1 Oct: the time zone and the sweep age; the rest stays
      — check: each new setting has a default, an `.env.example` line where the stand sets it, a test at another value
      — owner: **Mikhail**
      **Done 1 Oct:** `guide.time-zone` and `guide.media.rejected-kept-for`, each with a default, an
      `.env.example` line and a test at another value.
- [x] [Header links: Browse tags, Track a submission, Submit (F-6)](../verification/walkthrough-fixes.md#21-the-header-f-6) — decided 1 Oct: a new `GET /tags` page
      — check: every page has the three links; a menu at 390 px
      Built 2 Oct: four header links, a menu on phones, `GET /tags` (FEAT-008). Accepted by the
      human on 2 Oct.
      — owner: **Abdirakhim**
- [x] [Every article reachable, FRRO first (F-7)](../verification/walkthrough-fixes.md#22-all-articles-not-just-the-newest-twelve-f-7) — decided 1 Oct: an "All articles" page and FR-025 (pinning) built now
      — check: every article in two clicks from `/`
      FR-025 (`could`) is taken from phase 4 into this step; recorded in
      [04-hardening.md](04-hardening.md).
      **Widened 1 Oct by the human:** FR-025 gains a manual order, and two requirements join it —
      FR-033 (every article, 50 a page, by title, recently updated or most viewed, narrowed to a tag)
      and FR-034 (an article's views, counted except the moderator's). Schema in
      [ADR-0021](../architecture/adr/ADR-0021-article-views-and-pin-order.md), migration V8.
      — check: each criterion of FR-025, FR-033 and FR-034 a test, red first
      **FR-033 and FR-034 done 1 Oct:** [FEAT-018](../features/FEAT-018-every-article-and-its-views.md).
      **FR-025 done 1 Oct:** [FEAT-019](../features/FEAT-019-pinning-articles-in-order.md), FRRO pinned
      first in the seed. The screen's link waits for the moderator's header (2.3).
      — owner: **Mikhail**
- [x] [The moderator's own header, and a message after each action (F-24, F-15)](../verification/walkthrough-fixes.md#23-the-moderators-own-frame-f-24-f-15)
      — check: tests for "Published", "Rejected", "Removed", the link to the published article and the file mark
      Built 2 Oct: the moderator's header, a message after each decision, the article link, the
      file mark and the login's way back (FEAT-006). Accepted by the human on 2 Oct.
      — owner: **Abdirakhim**
- [x] [Clearer submission numbers, a Copy button, title and date on the status page (F-29, F-19)](../verification/walkthrough-fixes.md#24-times-numbers-copying-f-29-f-19) — decided 1 Oct: title and date shown
      — check: tests for the title, the date and the button
      — owner: **Mikhail**
      **Done 1 Oct:** FEAT-012; FR-012 gained the title-and-date criterion. The human looks at the
      numbers in a browser to judge 0 against 8.
- [ ] [Design pass over every screen, against the design screens (F-22)](../verification/walkthrough-fixes.md#3-a-design-pass-over-every-screen-f-22) — screens redrawn and accepted by the human before templates change
      — check: each screen compared with its design screen, differences accepted
      — owner: **Abdirakhim**, except 3.6 and 3.7
- [x] [Use the width of the window (F-17)](../verification/walkthrough-fixes.md#31-use-the-width-f-17)
      — check: `BrowserLayoutTest`: main content at least 60 % of a 1280 and 1920 px window
      — owner: **Abdirakhim**
      Built 2 Oct: `--page-width` 1440 px, and the article page as its design screen draws it (text
      and a 260-px sidebar, the layout half of 3.5). Accepted by the human on 2 Oct.
- [x] [One set of buttons, a focus ring in the site's colours (F-27, F-21, F-26)](../verification/walkthrough-fixes.md#32-one-set-of-buttons-f-27)
      — check: a design screen with every button, accepted; axe clean
      — owner: **Abdirakhim**
      Built 2 Oct: [Buttons.html](../design/screens/Buttons.html) accepted, then four buttons and a
      maroon focus ring (FEAT-011, `ButtonStylesTest`); axe clean. Accepted by the human on 2 Oct.
      Mikhail's 3.6 can start.
- [x] [One card everywhere; tags as a sidebar (F-21, F-8)](../verification/walkthrough-fixes.md#33-one-card-f-21-f-8)
      — check: the same card on landing, search and tag pages
      — owner: **Abdirakhim**
      Built 2 Oct: `shared/web/ArticleCard.html` on the landing, tag and all-articles pages
      (`ArticleCardTest`); search keeps its own list with the passage, by the human's decision.
      Accepted by the human on 2 Oct.
- [x] [The landing page brought to its design screen (F-2)](../verification/walkthrough-fixes.md#34-the-landing-page-f-2)
      — check: side-by-side comparison accepted — closes the open step "Templates brought up to the design screens"
      — owner: **Abdirakhim**
      Built 2 Oct: full-width hero, two cards across, "Browse by tag" sidebar (FEAT-003). Accepted
      by the human on 2 Oct.
- [ ] [The article page: actions at the top, summary, date, sidebar, contents, external links (F-10)](../verification/walkthrough-fixes.md#35-the-article-page-f-10)
      — check: a test per element; 390 px
      — owner: **Abdirakhim**
- [ ] [The forms: dynamic tags, a drop zone, a wide editor with a `[[link]]` button, errors at the field, a fuller draft (F-11, F-12, F-13, F-16, F-18, F-20)](../verification/walkthrough-fixes.md#36-the-submission-and-edit-forms-f-12-f-13-f-18-f-20-f-11-f-16) — decided 1 Oct: Tom Select for tags, FilePond with image previews for the file, by a new ADR
      — check: `SubmissionFlowTest` unchanged and green; the human tries the form on desktop and phone
      — owner: **Mikhail**
- [ ] [The review page: a rendered diff, a summary textarea, the tag field (F-25)](../verification/walkthrough-fixes.md#37-the-moderators-review-page-f-25)
      — check: tests for the toggle and the textarea
      — owner: **Mikhail**
- [ ] [The 404, tracking and login pages made helpful (F-14, F-15, F-19)](../verification/walkthrough-fixes.md#38-the-small-pages-f-14-f-15-f-19)
      — check: each page has its explanation and a way back
      — owner: **Abdirakhim**
- [ ] [The stand before the demo: commit F-3, drop the raised limit, change the password, reset](../verification/walkthrough-fixes.md#4-the-stand-before-the-demo)
      — check: `.env` has no raised limit; `down -v` and `up` give 36 clean articles
      — owner: together, last — after every section-1 item is merged

## Parallel streams, 1 October

Agreed by the human on 1 Oct: the open items above split into two streams that touch different
files, so both members can work at once. Each item carries its owner on an `— owner:` line. The
split leans toward Mikhail in `app/src`, where `scripts/contribution.sh` and the ownership table
show him the lighter side.
[ownership.md](../team/ownership.md) was stale when this was decided and was regenerated with it.

| | Mikhail — media, time, content, the contributor's forms | Abdirakhim — search, navigation, the shared layout |
|---|---|---|
| Before the mid-demo | 1.1 author notes, 1.2 `[[Title]]`, 1.3 + 1.10 IST and settings, 1.4 video formats, 1.8 the three limits, 1.9 the sweep | 1.5 search by every word, 1.6 seed before index, 1.7 the 404 text, 1.8 the layout, 2.1 the header and `GET /tags` |
| After the mid-demo | 2.2 all articles and FR-025, 2.4 numbers and the status page, 3.6 the forms (new ADR), 3.7 the review page; the `ai-tools gaps` step | 2.3 the moderator's frame, 3.1–3.3 width, buttons, card, 3.4 the landing page, 3.5 the article page, 3.8 the small pages |
| Each, alone | the weekly-log paragraphs up to W39 | the weekly-log paragraphs up to W39 |
| Together | the stand (section 4), last; queue items 5–7 as discussions | |

Where the streams meet — one owner per file, and the second waits for the first to be merged:

- `static/css/site.css` and `tokens.css`: Abdirakhim only. Mikhail starts 3.6 after 3.1 and 3.2.
- The header fragment: Abdirakhim only (2.1, then 2.3).
- `contribute/SubmissionForm.html`: Mikhail only (1.2, 1.4's `accept`, 1.8's limits, 3.6).
- The search results template: Abdirakhim, 1.5 before 3.3's card.
- Dates on cards and the article page: Mikhail's 1.3 before Abdirakhim's 3.3 and 3.5.
- The landing page's pinned section: Mikhail's 2.2 before Abdirakhim's 3.4.
- `routes.yml`: both add a route (`/tags`, the all-articles page); the tables are regenerated by
  `ai-tools routes` after a merge, never merged by hand.

**Where Mikhail's stream touched the other, 1 Oct** — for Abdirakhim before 2.1, 2.3 and 3.1–3.3:
- `site.css` gained `.media-list [hidden]` (1.4b) and the number rules `.submission-number`,
  `.queue-number`, `.number-group`, `.number-dash`, `.number-row` (2.4); keep them through the design
  pass.
- The moderator's header (2.3) has to link to `/moderate/articles` (FR-025, FEAT-019). Nothing else
  links there, by the human's choice.
- `/articles` (FR-033) is a new page for the header (2.1) and the card (3.3); its cards and the tag
  page's use `<relative-time>` from `shared/web/LocalTime.html` (ADR-0020).
- Mikhail's open items: the human's checks on the stand (iPhone `.mov`, Android `.mp4`, 0 against 8
  in the numbers), then 3.6, which waits for 3.1 and 3.2, and 3.7.

## Readiness criterion

The mid-demo scenario runs end to end on real data; both of us appear in the git history in every
week of the phase; the gap list is written honestly rather than trimmed before the demo.

**Demo run on the compose stand, 29 Sep — the first part met.** `docker compose up --build` from
empty volumes: PostgreSQL 17.11, six migrations, "Seed: 30 articles imported". Walked over HTTP,
then accepted by the human in the browser: "FRRO registration" returns `Registering with FRRO`
first; a wiki link resolves and a missing title renders as `wikilink-missing`; an edit with a JPEG
waits in the queue, is on no public page or search result, and its media answers `404` to anyone
but the moderator; the moderator logs in, sees the photo, approves, and the article, the photo and
search show the new text; the photo survives both a restart and a recreated container. Found on the
way: `guide-media` was declared but not mounted, so an uploaded photo was lost with the container —
now mounted at `GUIDE_MEDIA_ROOT`, and `.env.example` no longer lists variables the stand ignores.
Left open: none of the 30 articles contains a red link, so the demo can show one only in the
editor's preview.

## Open questions

Raised on 7 September while giving every step a checkable result.

1. ~~**The moderation state machine's states and transitions are not written down.** The check above
   assumes there is a set of them to test. The `ADR`, `ERD entity` and `Route` columns of the feature
   coverage tracker are still empty for every row, so nothing yet says what the states are. This
   blocks `moderate`, and `moderate` is the middle of the demo scenario.~~ Resolved 10 Sep:
   [ADR-0003](../architecture/adr/ADR-0003-moderation-and-revision-storage.md) (accepted) settles
   the submission's states (`pending`/`approved`/`rejected`) and the table shape they live in.
   `ERD entity` and `Route` for the `moderate` rows are separate, still-open steps (the ERD and the
   route contract themselves are unchecked above), not blocked by this question any more.
2. ~~**Is this phase's scope survivable?** Six of these eleven steps are whole slices, and the phase
   runs eighteen days from an application that is currently 51 lines of Java. The audit of
   7 September puts the capacity arithmetic and a proposed MoSCoW cut in front of the human; cutting
   scope is not the agent's call, and neither is deciding that no cut is needed.~~ Resolved 10 Sep:
   six `should`/`could` items (FR-023, FR-024 and the admin panel; FR-019; FR-006; FR-026; FR-016)
   move to phase 4, leaving each slice's `must`-priority acceptance criteria as the phase-3 exit bar.
   Full breakdown, the risk signal and plan B are in the "Revised milestone plan" entry of
   [01-requirements-design.md](01-requirements-design.md).
