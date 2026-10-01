---
id: FEAT-011
title: A layout for any screen width and for the keyboard
status: done
covers: [NFR-007, NFR-008]
slice: shared
routes: []
tables: []
code:
  - app/src/main/resources/templates/shared/web/Layout.html
  - app/src/main/resources/static/css/tokens.css
  - app/src/main/resources/static/css/site.css
  - app/src/main/resources/static/css/editor.css
  - app/src/main/resources/static/js/editor.js
tests:
  - app/src/test/java/in/ac/iitm/guide/BrowserLayoutTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserKeyboardTest.java
  - app/src/test/java/in/ac/iitm/guide/TemplateTokensTest.java
  - app/src/test/java/in/ac/iitm/guide/ButtonStylesTest.java
---

# FEAT-011 — A layout for any screen width and for the keyboard

The cross-cutting half of every page, written down on 28 Sep when NFR-007 and NFR-008 were closed.
The layout was built on 27 Sep as queue item 3 of [03-main-flow.md](../roadmap/03-main-flow.md),
under [ADR-0014](../architecture/adr/ADR-0014-responsive-layout-and-browser-checks.md); it had no
feature file until then.

## Why

Most readers and contributors are students on phones (NFR-008), and a student who cannot use a mouse
or see well has to be able to read, search and contribute (NFR-007).

## Scenario

Every page, from 320 to 1920 CSS pixels: no sideways scrolling of the page (a wide table scrolls
inside itself), 16-px text and 44-px targets on a phone and a tablet. From the keyboard alone the
demo scenario — search, read, follow a wiki link, propose an edit, sign in, approve — can be done
with Tab, typing and Enter, and the focus is visible at every stop.

## Routes

None of its own: it is the frame and the styles of every built page.

## Schema impact

None.

## Acceptance criteria

- [x] Every built GET page of `routes.yml`, and the not-found page, fits 320, 768, 1280 and 1920 px
  and passes axe's WCAG 2.2 AA rules — `BrowserLayoutTest`, the moderator's pages signed in
- [x] The demo scenario can be done from the keyboard alone with the focus always visible —
  `BrowserKeyboardTest.the_demo_scenario_can_be_done_from_the_keyboard_alone_with_the_focus_always_visible`
- [x] Templates carry tokens, never a literal colour or length — `TemplateTokensTest`
- [x] At 1280 and 1920 px the main content is at least 60 % of the window (walkthrough fix 3.1) —
  `BrowserLayoutTest`, on every page
- [x] Every action is one of the four buttons of
  [Buttons.html](../design/screens/Buttons.html) (fix 3.2) — `ButtonStylesTest`, 18 actions
- [x] The focus ring is the site's maroon, not the browser's blue (fix 3.2, F-21) —
  `BrowserLayoutTest.the_focus_ring_is_the_sites_maroon_not_the_browsers_blue`

## What closing it found (28 Sep)

- `BrowserLayoutTest` measured the login page in place of the moderator's pages. Signed in, the queue
  was 541 px wide at 320 px, and the Review link and the Reject button were under 44 px.
- `BrowserKeyboardTest` found the editor trapping Tab and drawing no focus ring (FEAT-010).

## Width and buttons (walkthrough fixes 3.1 and 3.2, 2 Oct)

Drawn first as [Buttons.html](../design/screens/Buttons.html) and accepted by the human on 2 Oct,
with three decisions: the frame grows to 1440 px (`--page-width`, was 1100, 57 % of a 1920 px
window); Cancel and Back are quiet links with an arrow, as walkthrough-fixes.md says, not the grey
ghost button of the older screens; Approve stays maroon rather than the screens' olive.

- Four buttons in `site.css`: primary (filled maroon), secondary (maroon outline), quiet (text, with
  an arrow when it goes back: `button-back`), danger (red outline). 44 px high, a 4-px corner
  (`--radius-control`), 12 px apart in a row; on a phone a form's buttons stand in a column. They
  replace `button-reject`, `print-button`, the outlined `button-quiet` and the bare action links.
- The search button stays gold: it is part of the search box.
- A 3-px maroon ring at `:focus-visible` on everything the keyboard reaches.
- Running text keeps its 46-rem measure; what fills the wider frame is the sidebars and the wide
  editor of fixes 3.4–3.6. Until those land an article has space beside it.
- Red first: `ButtonStylesTest` 14 of 18; `BrowserLayoutTest` 17 pages at 1920 px ("1100 px, under
  60 %") and the focus ring.
- The 404 page's inline "Back to the guide" is left for fix 3.8, which rebuilds that page.

**Found on the human's phone, 2 Oct:** below 1024 px the header wrapped, Menu fell to the left edge and its list, opening leftwards, went off the screen. The menu now keeps to the right edge (`.site-nav { margin-left: auto }`); `BrowserLayoutTest.on_a_phone_the_menu_sits_at_the_right_and_opens_inside_the_screen`, at 320 and 390 px, was red first. The landing's search placeholder is "Search the guide", which fits a phone.

## Deliberately out of scope

- A manual pass with a screen reader, which NFR-007's fit criterion asked for until the human dropped
  it on 28 Sep. What a screen reader announces is checked only as far as axe checks names and roles.
- Browsers other than Chromium: the `browser` profile runs Chromium only (ADR-0014).

## Open questions

None.
