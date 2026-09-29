# ADR-0014 — Responsive layout and browser checks

**Status:** accepted
**Date:** 2026-09-27

## Context

NFR-008 asks every screen to work from 320 to 1920 CSS pixels, with 44-pixel targets on a phone, and
NFR-007 asks for WCAG 2.2 AA with an automated check in the build. On 27 Sep the stylesheet had no
media query; the page gutters (40 px a side) and the card grid's 260-px minimum add up to more than
320 px, so a phone scrolls sideways — worked out from the CSS, not yet measured in a browser. The
design screens in `docs/design/screens/` are desktop only.

Two things bind the choice. Bootstrap was taken out of the stack on 26 Sep because no template used
it, and ADR-0013 allows front-end libraries only as WebJars. Neither requirement can be verified
without a browser: sideways scrolling, target sizes and contrast only exist once a page is laid out.

## Options

### Layout

**A. Our own CSS on the existing tokens.** Mobile first: a single column that widens, grids built on
`min(…, 100%)` so no track is wider than the screen, `clamp()` for gutters and type, one or two
breakpoints. No dependency; the IITM look stays ours; `TemplateTokensTest` keeps holding templates to
classes and tokens. Costs the CSS for each screen, which is few: five templates today, fifteen screens
sketched.

**B. Bootstrap 5.3.8 (WebJar).** A ready grid and utilities, but templates fill with its classes, the
IITM design has to be painted over its defaults, and its menu needs its JavaScript. It was removed
once already for having no use.

**C. Pico CSS 2.1.1 (WebJar).** Styles plain HTML and is responsive as shipped, but brings its own
look, which competes with our tokens instead of carrying them.

### Verification

**A. Browser tests in a `browser` Maven profile**, the way `postgres` works: Playwright for Java
(`com.microsoft.playwright:playwright` 1.63.0) opens each page of the running application at the four
widths and checks the fit criterion of NFR-008; axe-core (`com.deque.html.axe-core:playwright` 4.13.0)
checks WCAG 2.2 AA on the same pages, which is the automated check NFR-007 promises. Both are on Maven
Central at those versions (checked 27 Sep). Playwright downloads a browser on its first run.

**B. Screenshots by hand** at three widths before each stage. Costs nothing and is forgotten; NFR-007's
automated check would then not exist.

### Design screens

**A. Edit the HTML screens in `docs/design/screens/` directly**, adding the phone layout to each.
**B. Figma or Claude Design first**, then code. Rejected by the human: the round trip through another
tool costs more than it returns for this many screens, and Figma was already dropped as a phase 2
step on 22 Sep.

## Decision

Layout **A**, verification **A**, design screens **A** — the human's choice on 27 Sep. For the
layout, the deciding factor is that the design already lives in our tokens and a framework would have
to be overridden to show it; for verification, that both requirements are about rendered pages and
only a browser renders them.

**What follows, rather than being a separate decision:**

- The `browser` profile is not part of the default build or the pre-push hook, like `postgres`: it
  needs a browser download and a running application. It fails when the browser cannot start,
  rather than skipping.
- The pages it visits are every GET route marked built in `routes.yml`, the route contract whose
  tables `ui-routes.md` shows (since 27 Sep), so a new route is checked once it exists, not once
  someone remembers.
- A design screen shows the phone layout next to the desktop one; a template follows its screen.

## Consequences

**Good:** no front-end framework to learn, override or upgrade. The phone layout and accessibility
are checked in a real browser, with a failure naming the page and the width, and the same run gives
NFR-007 its automated check.

**Bad:** every new screen needs its own phone layout, written by us. Two new test dependencies and a
browser download on each machine that runs the profile; the profile is only as good as the list of
routes it visits. axe finds a subset of WCAG failures, so the manual pass NFR-007 names is still
needed.

**Reversal:** the CSS is one file; adopting a framework later means rewriting it and the template
classes, which grows with every screen. Dropping the browser profile removes two test dependencies
and leaves NFR-007 and NFR-008 with manual checks only.
