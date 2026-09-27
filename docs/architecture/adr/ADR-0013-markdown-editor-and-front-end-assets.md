# ADR-0013 — Markdown editor and front-end assets

**Status:** accepted
**Date:** 2026-09-27

## Context

FR-027 asks for a comfortable editor for an article's body: formatting controls, a preview beside
the text that updates as the contributor types, and a draft kept in the browser. The human ruled out
writing an editor of our own — too much code and too many bugs for what a library already does —
and set three conditions on 27 Sep:

- **Assets come from WebJars on Maven Central.** The build is Maven only, with no Node; the version
  of every script is then in `app/pom.xml` like any other dependency.
- **A strict Content-Security-Policy**: scripts and styles only from our own files, nothing inline.
  Article bodies are the one place where visitor-written text reaches a page; ADR-0001 escapes it,
  and the policy is the second layer if escaping ever fails.
- **Any script must survive the editor.** Articles are in English, Hindi and Tamil, typed through
  the operating system's input method, often on a phone.

One fact about the candidates shaped everything else: **every editor renders Markdown with its own
parser** (marked, remark, Lezer or its own). None of them shows what commonmark-java will publish,
and none knows `[[Title]]`. A preview that differs from the published page is worse than none.

The comparison was made on 27 Sep from the npm registry, the GitHub API, bundlephobia and Maven
Central; the figures below are from that day, and the EasyMDE WebJar was downloaded and its bundle
read.

## Options

### A. EasyMDE 2.21.0 (MIT), WebJar `org.webjars.npm:easymde:2.21.0`

A toolbar, side-by-side preview, full screen and a built-in `autosave` to `localStorage`, over
CodeMirror 5. The only editor whose WebJar is at its current npm version. `previewRender` can be
replaced by an asynchronous call, so the preview can be the server's rendering. About 107 kB gzip
plus its CSS. By default it downloads Font Awesome and a spell-check dictionary from a CDN; both are
options that can be switched off. The bundle has no `eval` and inserts no `<style>` element, but
calls `setAttribute("style")` once, which a strict policy blocks — what that breaks is not yet known.
CodeMirror 5 has open mobile input issues (duplicated input on Firefox for Android, iOS
autocapitalisation).

### B. TinyMDE (tiny-markdown-editor) 0.2.34 (MIT)

About 18 kB, active, and its `customInlineGrammar` can highlight `[[...]]`. It edits through
`contenteditable`, where Chinese IME input was broken until 2024 and undo until 2025; Devanagari and
Tamil are untested. Its WebJar is at 0.2.25, nine releases behind npm. It has no preview, so the
server preview would be a pane of our own.

### C. A plain textarea with a server-rendered preview

No input-method risk and nothing new to test, but no toolbar, no highlighting and no built-in
draft: every piece of comfort becomes our code, which is what the human ruled out.

### Rejected without a trial

- **Toast UI Editor** — repository archived, 643 open issues.
- **Milkdown Crepe** — about 460 kB, a WYSIWYG view rendered by remark, so it diverges from the
  published page by construction; WebJar behind npm.
- **OverType** — aligns an overlay over a monospace textarea, which Devanagari and Tamil conjuncts do
  not keep; injects a `<style>` element; no WebJar.
- **ink-mde** — no release since September 2024; about 210 kB; no WebJar.
- **A CDN** for any of them — every visitor's request would go to a third party, the site would
  break when it does, and it conflicts with the strict policy.

## Decision

**A — EasyMDE from its WebJar**, with its preview rendered by our server and its CDN downloads
switched off. **B is the fallback** if A fails the checks below. The deciding factor is that A is the
only candidate available as a current WebJar that already has the preview, the draft and the
toolbar FR-027 asks for, so the only code of ours is the preview endpoint and the configuration.

**What follows from A, rather than being a separate decision:**

- The preview calls an endpoint that renders with the same `WikiLinkRenderer` as the article page.
  It is public and anonymous, so it is bounded — a size limit on the text now, the rate limit when
  NFR-005 is built — and a debt entry records the gap until then.
- Toolbar icons are our own; `autoDownloadFontAwesome` and `spellChecker` are off.
- The policy header is sent by every page from `shared/security`, not only the form's.
- Two checks decide between A and B before FR-027 is called done: the editor works under the strict
  policy in a browser, and typing Hindi and Tamil with Gboard on Android and the iOS keyboard does not
  duplicate, drop or reorder characters. If A fails either and a configuration cannot fix it, B is
  tried against the same two checks.
- `[[` completion (FR-028, should) needs a CodeMirror 5 hint add-on attached to EasyMDE's own
  instance; whether that works is not verified and does not block FR-027.
- Any other front-end library follows the same rule: a WebJar at a verified version, or it is not
  added.

## Consequences

**Good:** the contributor gets a toolbar, a preview that is the published page, and a saved draft,
from a maintained library. The version of every script is in `app/pom.xml`. The strict policy
protects every page, not only the editor.

**Bad:** about 107 kB of script on the form page. CodeMirror 5's mobile issues are the library's, and
we can only work around them. The strict policy forbids inline scripts and styles everywhere, which
rules out some future libraries and every `onclick=` or `style=` in a template. The preview endpoint
is a new anonymous way to make the server work, bounded only by size until NFR-005.

**Reversal:** the editor is one script, one stylesheet and a configuration on one form; replacing it
with B or with a plain textarea changes the form and keeps the preview endpoint. Reconsider if the
device checks fail, if EasyMDE stops being released, or if a WebJar of a better editor appears.
