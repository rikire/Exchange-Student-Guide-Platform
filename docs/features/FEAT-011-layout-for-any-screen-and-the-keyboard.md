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

## What closing it found (28 Sep)

- `BrowserLayoutTest` measured the login page in place of the moderator's pages. Signed in, the queue
  was 541 px wide at 320 px, and the Review link and the Reject button were under 44 px.
- `BrowserKeyboardTest` found the editor trapping Tab and drawing no focus ring (FEAT-010).

## Deliberately out of scope

- A manual pass with a screen reader, which NFR-007's fit criterion asked for until the human dropped
  it on 28 Sep. What a screen reader announces is checked only as far as axe checks names and roles.
- Browsers other than Chromium: the `browser` profile runs Chromium only (ADR-0014).

## Open questions

None.
