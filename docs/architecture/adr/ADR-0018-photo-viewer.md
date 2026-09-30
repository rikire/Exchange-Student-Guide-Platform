# ADR-0018 — PhotoSwipe as the photo viewer

**Status:** accepted
**Date:** 2026-09-30

## Context

FR-032 asks for photos that open full screen, zoom, and page on to the others, on a phone as on a
desktop. Two rules from ADR-0013 bind any answer: front-end code comes from a WebJar on Maven Central,
with no Node in the build, and the Content-Security-Policy allows scripts and styles only from the
site's own files, never inline. The photos have no stored size: those in an article's text are
Markdown images, and an attached photo's row holds no width or height.

## Options

### A. PhotoSwipe 5 from its WebJar

MIT, `org.webjars.npm:photoswipe:5.4.3`, checked on Maven Central on 30 Sep; npm is at 5.4.4, one
patch ahead, and its last release was in May 2024. It is a pair of ES modules and one stylesheet, with
no dependencies. It handles pinch, wheel and double-tap zoom, panning a zoomed photo, swiping and the
arrow keys, and returns the focus to where it was on closing. Its source builds no inline style in
HTML: it sets styles through the DOM, which the policy allows. It needs each photo's size before it
opens it.

### B. GLightbox or another lightbox

Other lightboxes exist, GLightbox the best known. None was evaluated: neither its zoom, nor whether
it has a current WebJar, nor whether it builds inline styles the policy would refuse. The human asked
for PhotoSwipe by name, and it passed every check it was put to, so no second candidate was needed to
decide. This option is recorded as not examined, not as rejected on its merits.

### C. Our own viewer on a `<dialog>`

No dependency. Pinch zoom, panning with momentum and swiping between photos are exactly what a
library exists for, and writing them is replacing a dependency with our own code
(docs/ai/workflow.md §7a) with no reason that holds.

## Decision

**A**, asked for by name by the human and confirmed on 30 Sep. It meets both of ADR-0013's rules as
checked above, and it does everything FR-032 asks, zoom and panning included.

- **Sizes are read in the browser** from the loaded photo (`naturalWidth`, `naturalHeight`), so no
  column is added. An attached photo is lazy-loaded, so `photos.js` loads the page's photos before the
  viewer opens.
- **Zoom** goes past a photo's own size to twice it, and no further; beyond that it only blurs.
- **Without scripts** the photos stay as they were. With them, each photo is a button reachable with
  Tab, named "Open photo: …" (NFR-007), and opened by Enter or a click.
- **Where:** the article page and the moderator's review page, through the fragment
  `shared/web/PhotoViewer.html`.

## Consequences

**Good:**

- Every photo on an article, in the text or attached, opens full screen. Nothing on the server
  changes.
- `BrowserPhotoViewerTest` holds the behaviour and the policy in a real browser.

**Bad:**

- The version is written in three places: the root pom, `PhotoViewer.html` and `photos.js`. They
  have to change together.
- The library has had no release since May 2024. If a browser change breaks it, we either fix it
  ourselves or move to B.
- Opening the viewer loads every photo on the page first. On an article with many large photos, the
  first opening is slower.

**Reversal:** removing the fragment from two templates, `photos.js` and the dependency returns the
pages to plain photos. The trigger would be a breakage the library does not fix, or a policy
violation it starts to cause.
