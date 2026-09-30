---
id: FEAT-016
title: Viewing photos full screen
status: done
covers: [FR-032]
slice: articleview
routes: ["GET /articles/{title}", "GET /moderate/submissions/{number}"]
tables: []
code:
  - app/src/main/resources/static/js/photos.js
  - app/src/main/resources/templates/shared/web/PhotoViewer.html
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/templates/moderate/SubmissionReview.html
  - app/src/main/resources/static/css/site.css
tests:
  - app/src/test/java/in/ac/iitm/guide/BrowserPhotoViewerTest.java
---

# FEAT-016 — Viewing photos full screen

> Created under the contract the human confirmed on 30 Sep, which named PhotoSwipe.

## Why

A photo in an article is shown at the width of the text column, which on a phone is a few centimetres.
A reader who wants the details of a temple carving, or a moderator checking the fields on a photo of a
form, needs to enlarge it and move around in it. With several photos on a page, the reader also wants
to page through them without scrolling back.

## Scenario

1. A reader taps a photo in an article. It opens full screen on a dark background.
2. They pinch, double-tap or use the zoom control to enlarge it, up to twice the photo's own size, and
   drag to move around.
3. They swipe or press the arrows to go to the next photo on the page, the attached ones included, in
   the order they appear. A counter shows "2 / 3".
4. Escape, the close control or a tap on the background closes the viewer, and the focus is back on
   the photo they opened.

From the keyboard, Tab reaches each photo, which is announced as "Open photo: …", and Enter opens it.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /articles/{title}` | photos in the text and attached open in the viewer | `articleview/Article.html` |
| `GET /moderate/submissions/{number}` | a submission's photos open in the viewer | `moderate/SubmissionReview.html` |

## Schema impact

None. A photo's size is read in the browser from the loaded image ([ADR-0018](../architecture/adr/ADR-0018-photo-viewer.md)).

## Decisions this feature fixed

All confirmed by the human on 30 Sep, as suggested:

- **PhotoSwipe 5.4.3 from its WebJar.** npm is at 5.4.4; ADR-0013 allows WebJars only.
- **Sizes read in the browser**, not stored.
- **Zoom up to twice a photo's own size.**
- **Without scripts nothing changes. With them, a photo is a button** reachable from the keyboard
  (NFR-007).

## Acceptance criteria

- [x] A photo in the text opens full screen when clicked —
      `BrowserPhotoViewerTest.a_photo_in_the_text_opens_full_screen_when_clicked`
- [x] The viewer pages through every photo on the page, attached ones included —
      `BrowserPhotoViewerTest.the_viewer_pages_through_every_photo_on_the_page_attached_ones_included`
- [x] The zoom control enlarges the photo past the screen's fit —
      `BrowserPhotoViewerTest.the_zoom_control_enlarges_the_photo_past_the_screens_fit`
- [x] Enter opens a photo reached by keyboard; Escape closes it and returns the focus —
      `BrowserPhotoViewerTest.a_photo_opens_from_the_keyboard_and_escape_closes_it_and_returns_the_focus`
- [x] A photo is offered to a screen reader as a button that opens the viewer —
      `BrowserPhotoViewerTest.a_photo_offers_itself_to_a_screen_reader_as_opening_the_viewer`
- [x] A photo on the moderator's review page opens in the viewer —
      `BrowserPhotoViewerTest.a_photo_on_the_moderators_review_page_opens_in_the_viewer`

Every test also fails on a Content-Security-Policy violation or a script error.

Evidence, 30 Sep: all six were red first. Five failed because nothing opened. The screen-reader test
failed because the photo had no role. All six are green with the viewer, under the policy.

**Accepted by the human on 30 Sep** on the compose stand: opening, zooming, paging through the photos and
closing them. On that acceptance FR-032 is `done`.

## Deliberately out of scope

- Captions in the viewer: the alt text is used for the photo's name, and nothing is shown over the
  photo.
- Photos on other pages, such as the landing page's cards, which carry none.
- Videos and documents, which keep their own player and download link.

## Open questions

None.
