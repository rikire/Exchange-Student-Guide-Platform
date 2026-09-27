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
- [ ] `media` — upload, type detection from content, safe delivery
      — check: a file whose extension lies about its content is rejected, and media attached to an
      unapproved submission is unreachable by anyone who has not been given its id
      **Narrowed 10 Sep:** FR-016 (downloading a media attachment) moves to phase 4 — see
      [01-requirements-design.md](01-requirements-design.md).
      **Added 27 Sep:** the attachment on the submission form, cut from `contribute` (FEAT-005) —
      the `attachment` field, the upload handed to `media`, and FR-010's and FR-011's two media
      criteria each (over the size limit, not an accepted type) as four tests. Closes
      [DEBT-008](../tech-debt.md).
- [ ] ~~Admin panel behind the single password~~
      — check: a test enumerates the admin routes **from the route contract** rather than by hand,
      and asserts each one redirects when unauthenticated; a route added later without a test fails it
      **Moved to phase 4, 10 Sep** — it exists only to serve FR-023/FR-024 (direct publish), which
      moved with it. See [01-requirements-design.md](01-requirements-design.md) and
      [04-hardening.md](04-hardening.md).
- [ ] Templates brought up to the design screens; the landing page brought up to the IITM reference
      — check: no literal colour or spacing value in the templates, only tokens; the landing page
      compared against the IITM reference side by side and the differences listed
      **Changed 27 Sep:** not generated from Figma. The human chose to draw layouts, phone ones
      included, directly in the HTML screens of `docs/design/screens/`, and templates follow those
      (ADR-0014).
- [x] Content: 30 or more articles in the database
      — check: the count comes from the database after import, and searching "FRRO registration"
      returns a relevant article — that is the first step of the demo scenario
      **Done 28 Sep:** ten articles added from OGE's International Student Handbook 2026 (arrival,
      onboarding, transport, Wi-Fi, banks, health, contacts, landmarks, food, safety), each citing
      its pages and listing what needs checking with OGE. The `seed` profile on H2 logs "30 articles
      imported", and "FRRO registration" returns `Registering with FRRO` first. Checked locally, not
      on the compose stand, which needs `POSTGRES_PASSWORD` in `.env`.
- [ ] `ai-tools`: the gap list and rubric generators; the first honest gap list
      — check: `ai-tools gaps` (phase 3) produces a non-empty list containing at least one item
      neither of us would have volunteered
- [ ] Ownership balance check — if it has drifted, the next tasks come from the lighter side
      — check: both members have authored commits in **every** week of this phase, with the hook's
      journal commits excluded — `sh scripts/contribution.sh` reports the two apart
- [ ] The moderator's pages in the browser check — `BrowserLayoutTest` visits `/moderate/queue` and
      `/moderate/submissions/{number}` without a session, so it measures the login page they redirect
      to, not them (found 28 Sep, after FEAT-006 met the layout of FEAT-003)
      — check: the test logs in first, and the queue and the review page pass NFR-008 and axe at the
      four widths
- [ ] Weekly-log paragraphs in each member's own words for W37, W38 and W39 — the W37 and W38 files
      were generated on 28 Sep with the git figures only, and W39 still lacks Abdirakhim's paragraph.
      Each is marked as written after the week it covers
      — check: no file in `docs/team/weekly-log/` up to W39 still says "Not written yet"

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
   the diff to FEAT-006's review screen. `BrowserLayoutTest` already checks the moderation pages
   (nine pages at four widths, green on 28 Sep). Waiting for the human: whether CON-004 ("No diffs") is
   revised for item 8, and whether ADR-0014's wording, which still names `ui-routes.md` as the source
   of the pages, is brought in line with `routes.yml`.

4. [ ] **Build the Markdown editor** — FR-027 and FR-028 on the `contribute` form, as ADR-0013
   decides: EasyMDE from its WebJar, a preview endpoint that renders with `WikiLinkRenderer` and caps
   the text's size, CDN downloads off, our own toolbar icons, the draft kept by EasyMDE's `autosave`
   and cleared after a successful submission, the strict Content-Security-Policy sent by every page
   from `shared/security`. The preview endpoint's missing rate limit is a debt entry until NFR-005.
   Comes after the responsive layout, so the editor goes into a layout that already fits a phone.
   — check: a test per FR-027 criterion; the editor works in a browser under the policy; typing
   Hindi and Tamil with Gboard on Android and the iOS keyboard does not duplicate, drop or reorder
   characters (if it does and no setting fixes it, TinyMDE is tried against the same checks); FR-028
   either works or is recorded as not done
5. [ ] **Frontend interactivity** — whether a lightweight library (htmx, Alpine.js or none) is
   added, and for what
   — check: the choice recorded as an ADR with the alternatives weighed
6. [ ] **All screens, with features not yet built** — how screens of unbuilt slices are shown
   without fake data passing for real (for example fixtures only under a separate profile, each one a
   debt entry)
   — check: the option chosen; every screen in `docs/design/screens/` reachable
7. [ ] **Email one-time code for submitting** — against spam and bots, a way to ban an address
   later, and approval status sent by email. Collects an email address but creates no account;
   touches CON-001's reason (personal data), ADR-0008 (which challenge), a mail server, and what OGE
   agrees to store
   — check: a requirement and an ADR agreed, and the stakeholder's answer recorded
8. [ ] **Diff view for the moderator** — reviewing an edit shows exactly what changed, where.
   Reverses CON-004 ("No diffs"); belongs to the review screen of `moderate` (FR-015)
   — check: CON-004 revised by the human and the requirement written
9. [ ] **Walk through every screen and feature** with the human, and turn what comes out into
   requirements and steps
   — check: the list of changes recorded
10. [ ] **Questions for OGE**, asked together rather than one meeting each: whether OGE is bound by
    GIGW and so by a WCAG level (NFR-007); what OGE agrees to store about contributors and which
    mail server may send from its name (item 7)
    — check: the answers recorded in `docs/stakeholder/` and turned into requirements or constraints

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

## Readiness criterion

The mid-demo scenario runs end to end on real data; both of us appear in the git history in every
week of the phase; the gap list is written honestly rather than trimmed before the demo.

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
