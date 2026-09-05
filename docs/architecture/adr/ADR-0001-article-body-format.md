# ADR-0001 — Article body format: Markdown, not raw HTML

**Status:** accepted
**Date:** 2026-09-05

## Context

The article body needs a markup format, compatible with the already-chosen inline `[[wiki link]]`
syntax. Content is submitted anonymously — no accounts — and passes through moderation before
publish, but moderation review checks meaning, not markup safety. The plan already names uploads as
"the largest security surface... [in a project] that otherwise has almost none"; a body-format
choice that opens a second major injection surface would contradict that.

## Options

### A. Plain text only

No formatting at all. Safe by construction. Too limited for procedural content — no lists, no
headings, no emphasis.

### B. Markdown

Converts safely to HTML by construction — a compliant converter does not pass through raw
`<script>` or event-handler attributes. Supports the formatting procedural articles actually need
(lists, headings, bold/italic), and sits naturally alongside `[[wiki link]]`, which is the same kind
of lightweight inline markup.

### C. Raw HTML

Most flexible, but requires a hand-built allowlist sanitizer (strip everything but a small safe tag
set) plus a CSP header as a second layer, and ongoing tests against sanitizer bypasses, to be safe
against anonymous, unauthenticated submitters.

## Decision

B — Markdown. The deciding factor: content comes from unauthenticated, anonymous contributors, and
Markdown is safe without needing to hand-maintain and continuously test an HTML allowlist.

## Consequences

**Good:** no sanitizer library to build, test and keep current; safe conversion to HTML by
construction; contributors get real formatting; consistent with the already-adopted `[[wiki link]]`
syntax.

**Bad:** contributors must learn Markdown syntax; some formatting raw HTML could express directly
(custom inline styling, complex tables) is harder or impossible.

**Reversal:** revisiting raw HTML later means building the allowlist sanitizer and CSP hardening
discussed alongside this decision. Stored Markdown converts to HTML at read time regardless, so
existing content is not blocked from a later change. Worth reconsidering if a real authoring need
appears that Markdown genuinely cannot express.
