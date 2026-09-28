---
id: FEAT-010
title: The Markdown editor on the submission form
status: done
covers: [FR-027]
slice: contribute
routes: ["POST /contribute/preview"]
tables: [article]
code:
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/BodyPreview.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/PreviewController.java
  - app/src/main/java/in/ac/iitm/guide/contribute/persistence/ContributeArticleRepository.java
  - app/src/main/java/in/ac/iitm/guide/shared/security/WebSecurity.java
  - app/src/main/resources/templates/contribute/SubmissionForm.html
  - app/src/main/resources/templates/contribute/SubmissionConfirmation.html
  - app/src/main/resources/static/js/editor.js
  - app/src/main/resources/static/js/draft-sent.js
  - app/src/main/resources/static/css/editor.css
  - app/src/main/resources/static/img/editor-icons.svg
tests:
  - app/src/test/java/in/ac/iitm/guide/contribute/EditorPreviewTest.java
  - app/src/test/java/in/ac/iitm/guide/shared/security/ContentSecurityPolicyTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserEditorTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserLayoutTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserKeyboardTest.java
---

# FEAT-010 — The Markdown editor on the submission form

Queue item 4 of [03-main-flow.md](../roadmap/03-main-flow.md), built as
[ADR-0013](../architecture/adr/ADR-0013-markdown-editor-and-front-end-assets.md) decides. Contract
confirmed by the human on 28 Sep: the preview at `POST /contribute/preview`, a preview limit of
100,000 characters, FR-028 as a separate contract, and the phone check put off to later.

## Why

A contributor writing an article in a bare textarea cannot see what the Markdown and the `[[Title]]`
links will turn into until OGE publishes it, and loses the text if the tab closes. The editor shows
the published rendering while they write and keeps an unsent draft in their browser.

## Scenario

1. A contributor opens `GET /submit` or an article's edit form (UC-010, UC-011). The body is EasyMDE
   with a toolbar of seven controls and a preview: under the text on a phone and a tablet, beside it
   from 64rem up.
2. A control used with text selected puts the Markdown around the selection.
3. When typing pauses for 300 ms, the page posts the body to `POST /contribute/preview` with the
   form's CSRF token, and the preview shows the answer: the article page's own rendering, wiki links
   resolved against published articles and missing ones red.
4. The body is saved to `localStorage` a second after each change, one draft per form (a new
   article, or an edit of one address). Reopening the form in the same browser restores it.
5. The submission succeeds and the confirmation page clears that draft. A refused submission keeps
   it; so does a form over the container's limit, which comes back empty (`413`) and gets the text
   back from the draft.

Without JavaScript the form is the plain textarea it was, and submits the same way.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `POST /contribute/preview` | The body rendered as the article page renders it; `413` over 100,000 characters | — (an HTML fragment) |

## Schema impact

None. The preview reads `article.slug` and `article.removed_at` with one query per body, as the
article page does.

## Acceptance criteria

FR-027's criteria, each with its test:

- [x] A formatting control with text selected inserts the Markdown around the selection —
  `BrowserEditorTest.a_formatting_control_used_with_text_selected_puts_the_markdown_around_the_selection`
- [x] When typing pauses, the preview beside the text shows the server's rendering with wiki links
  resolved — `EditorPreviewTest.the_preview_shows_the_servers_rendering_with_wiki_links_resolved`,
  `BrowserEditorTest.the_preview_beside_the_text_shows_the_servers_rendering_with_wiki_links_resolved`
- [x] Text in any script, typed with its input method, is previewed and stored unchanged — the server
  half is `EditorPreviewTest.text_in_any_script_is_shown_by_the_preview_and_stored_unchanged_by_the_submission`,
  and typing through the browser's text input is
  `BrowserEditorTest.text_in_any_script_typed_into_the_editor_is_previewed_and_submitted_unchanged`.
  The phone check with Gboard and the iOS keyboard, which ADR-0013 required before FR-027 is done,
  was dropped by the human on 28 Sep: the desktop result is enough for them.
- [x] A draft is restored after the page is closed and reopened, and cleared once the submission
  succeeds —
  `BrowserEditorTest.an_unsent_draft_is_restored_when_the_form_is_reopened_and_cleared_once_the_submission_succeeds`
- [x] The editor is usable from a phone to a desktop without scrolling sideways — `BrowserLayoutTest`
  at 320, 768, 1280 and 1920 px on `/submit` and the edit form, with axe's WCAG 2.2 AA rules

And ADR-0013's conditions:

- [x] Every response carries the strict policy —
  `ContentSecurityPolicyTest.every_response_allows_scripts_and_styles_only_from_the_site_itself`; the
  editor runs under it with no violation or script error, which every `BrowserEditorTest` test
  asserts, shown red on 28 Sep with an inline script put on the form.
- [x] The preview is bounded and needs the token —
  `EditorPreviewTest.a_body_at_the_preview_limit_is_rendered_and_one_character_over_it_is_refused`,
  `EditorPreviewTest.a_preview_without_a_csrf_token_is_refused`
- [x] Raw HTML in the preview is escaped as on the article page —
  `EditorPreviewTest.the_preview_is_the_article_page_rendering_so_raw_html_is_escaped`

## What building it found

- **CodeMirror's `contenteditable` input dropped Indic text.** Forced on in desktop Chromium, it lost
  every Hindi and Tamil character and the emoji typed through Playwright's text input, and kept only
  the ASCII (`"    . e "`). The default `textarea` input kept all of it. CodeMirror uses
  `contenteditable` on a phone whatever the desktop does, so the phone check is the one that decides;
  if it fails there too, forcing `textarea` on phones is the configuration to try before TinyMDE.
- **EasyMDE clears the draft when the form is submitted**, before the server answers. FR-027 keeps
  it until success, so that listener is suppressed (`autosave.binded`) and the confirmation page
  clears the draft (`draft-sent.js`).
- **EasyMDE bound Tab to indenting**, so the keyboard could not leave the body (WCAG 2.1.2), and the
  focused editor drew no ring. Found by `BrowserKeyboardTest` on 28 Sep: Tab and Shift-Tab now move
  the focus, a list is nested with spaces, and the editor draws the ring.
- EasyMDE's syntax colours for links, quotes and HTML tags failed axe's contrast rule; `editor.css`
  replaces them with the palette's.
- `BrowserLayoutTest` counted CodeMirror's hidden input, a textarea inside a 3-by-0 box, as a 13-px
  target; an element clipped to nothing is now exempt from its text and target checks.
- The WebJar declares CodeMirror, marked and a spell checker as dependencies by version range, which
  the bundled `dist/easymde.min.js` already contains; `app/pom.xml` excludes them.

## Deliberately out of scope

- FR-028 (`[[` completion) — its own contract, by the human's decision of 28 Sep.
- The preview's rate limit — NFR-005; [DEBT-017](../tech-debt.md) until then.
- A body length limit on the submission itself: the 100,000 characters bound the preview only.

## Open questions

None. FR-027 was marked done by the human on 28 Sep without the phone check; the `contenteditable`
finding above is the risk that decision carries.
