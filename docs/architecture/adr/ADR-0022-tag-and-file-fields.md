# ADR-0022 — Tag and file fields from libraries, and the policy they need

**Status:** accepted (by the human, 2 Oct)
**Date:** 2026-10-02

## Context

The walkthrough of 1 Oct found the submission form's tag field and file field the hardest parts of
the form to use (F-12, F-13): tags are typed into a bare text field, and the file is the browser's
grey button with no name, size or way to remove it. Fix 3.6 asks for a tag field that turns words
into chips and suggests existing tags, and a drop zone that shows the chosen file.

The human decided on 1 Oct: libraries, not our own scripts — Tom Select for tags, FilePond with
`filepond-plugin-image-preview` for the file, in `storeAsFile` mode so the file is sent with the
form and the server does not change. This ADR records that decision as an addition to
[ADR-0013](ADR-0013-markdown-editor-and-front-end-assets.md), and one cost found while preparing it.

Constraints already decided (ADR-0013):
- assets only from WebJars on Maven Central, at a verified version, with no Node in the build;
- a strict Content-Security-Policy on every page: scripts and styles only from the site itself,
  nothing inline;
- any script has to survive text typed in Hindi and Tamil through an input method.

**The cost found on 2 Oct.** Reading the bundles against the policy in `WebSecurity`: FilePond
itself sets styles only through the CSSOM (`element.style.cssText`), which the policy allows, and
`storeAsFile` puts the file into a plain `<input type=file>` through `DataTransfer`. The image
preview plugin does two things the current policy blocks:
1. it loads the chosen image from a `blob:` URL (`URL.createObjectURL` into `new Image().src`) —
   blocked by `img-src 'self'`;
2. it scales the image in a Web Worker built from a `blob:` URL (`createWorker(BitmapWorker)`) —
   with no `worker-src`, `script-src 'self'` applies and `new Worker` throws. The plugin's fallback
   covers a worker that returns no bitmap, not one that cannot be created.

Found by reading the code; not yet seen in a browser.

Versions checked on Maven Central and npm on 2 Oct:

| Library | WebJar | npm latest | Licence | Dependencies the WebJar declares |
|---|---|---|---|---|
| Tom Select | 2.6.2 | 2.6.2 | Apache-2.0 | `orchidjs__sifter`, `orchidjs__unicode-variants`, by version range |
| FilePond | 4.32.12 | 4.32.12 | MIT | none |
| filepond-plugin-image-preview | 4.6.11 | 4.6.12 | MIT | none |

## Options

### A. FilePond with the image preview, the policy widened by `blob:` for images and workers

`img-src 'self' blob:; worker-src blob:`. A `blob:` URL can be made only by a script already
running on our page, and `script-src 'self'` is unchanged, so a script from anywhere else still does
not run. What is lost: if a script of ours were ever made to do something hostile, it could also
show an image or start a worker from data it built — which it could already do through the CSSOM
and `fetch`. The contributor sees a thumbnail of the photo before sending, which catches the wrong
photo early.

### B. FilePond without the preview plugin, the policy unchanged

The drop zone shows the file's name, size and a way to remove it — everything fix 3.6's text asks
for. No thumbnail. Goes against the human's choice of 1 Oct, which included previews.

### C. Our own drop zone and preview

Ruled out by the human on 1 Oct: libraries, not our own scripts (ADR-0013's reasoning applies).

### Tags: Tom Select, not Tagify or Choices.js

Compared on 1 Oct ([walkthrough-fixes.md](../../verification/walkthrough-fixes.md), 3.6): Tagify
types into `contenteditable`, the input-method risk ADR-0013 avoided; Choices.js calls
`setAttribute("style")`, which the policy blocks. Tom Select types into a real `<input>` and sets no
inline style. Not a separate decision, recorded here for the reason.

## Decision

**A**, chosen by the human on 2 Oct. Tom Select 2.6.2, FilePond 4.32.12 and
filepond-plugin-image-preview 4.6.11 from their WebJars. Tom Select's two declared dependencies are
excluded: `tom-select.complete.min.js` is a self-contained UMD bundle with Sifter and the
unicode-variants code inside it, and their version ranges would make the build resolve whatever is
newest on the day — the same exclusion as EasyMDE's in `app/pom.xml`.

The deciding factor: the thumbnail is what the human asked for, and the policy loses almost nothing,
because `script-src` stays `'self'`.

## Consequences

**Good:** the tag field and the drop zone come from maintained libraries; the form still posts the
same fields, so `SubmissionFlowTest` and the server are untouched. Every version is in
`app/pom.xml`.

**Bad:** about 17 + 33 kB gzip more script on the form page, plus the plugin. The policy is no longer
`'self'` only, and `ContentSecurityPolicyTest` has to pin the new string so that nothing widens it
further unnoticed. The preview plugin has had no release since December 2023 and its WebJar is one
patch behind npm. Tom Select and FilePond each add their own CSS, which has to be brought to the
site's tokens.

**Reversal:** each library is one script, one stylesheet and a configuration on one form; dropping
the preview plugin restores the old policy (option B). Reconsider if the input-method check fails
for Tom Select, if the plugin breaks with a FilePond release, or if a browser check shows the policy
still blocks the preview.
