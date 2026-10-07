# ADR-0015 — Showing an edit as a diff

**Status:** accepted
**Date:** 2026-09-29

## Context

The moderator read an edit in full and had to find the changed sentence themselves, because CON-004
ruled out a diff view. On 29 Sep the human asked for the change to be visible at once, and CON-004
was narrowed to "no stored diffs" (FR-029). Three things bind the answer:

- **The review page must fit a 320 px phone** without sideways scrolling (NFR-008), and pass WCAG
  2.2 AA (NFR-007), which rules out marking changes by colour alone.
- **The edit's text is visitor-written.** Everything on the page is escaped, and `th:utext` is kept
  for the Markdown converter's output only (ADR-0001, `docs/ai/security.md`).
- **Comparing texts is a solved problem** (longest common subsequence, Myers' algorithm); the project
  does not write its own version of one (`docs/ai/workflow.md` §7a).

## Options

Four presentations were put to the human on 29 Sep, as Wikipedia's shows them:

### A. Two columns — published left, proposed right

MediaWiki's default. Changed paragraphs side by side, the changed words marked inside them. The most
familiar on a wide screen; on a phone the two columns do not fit and have to stack.

### B. One column, inline

Removed text struck through and added text underlined in one flow. Fits any width with no special
case, but reads less like "before and after".

### C. A visual diff of the rendered page

The easiest for a reader who does not know Markdown. Needs an HTML diff; no maintained Java library
for one was known, and a hand-written one is exactly what §7a rules out.

### D. A "before / after" card per changed paragraph

Simple to read on a phone; long when an edit touches many paragraphs.

For the comparison itself: **java-diff-utils 4.17** (Apache-2.0, no runtime dependencies, the release
in its Maven Central metadata on 29 Sep), or writing the algorithm ourselves, which §7a excludes.

## Decision

**A, chosen by the human**, with removed text red and added text green, and — the agent's objection,
accepted — the two columns stacked into one below 48 rem, so a phone never scrolls sideways.

Details fixed with it:

- **The Markdown source is compared, not the rendered page**, paragraph by paragraph (a blank line
  separates them), and word by word inside a changed paragraph. Line endings and extra blank lines
  are not a change.
- **Unchanged paragraphs more than one away from a change are folded** into a count.
- **The comparison is with the article as it is now**, which is what approval replaces.
- **Title, summary and tags** are shown as the old value and the new one when they differ.
- **Red and green come with strike-through and underline** (`<del>` and `<ins>`), so the marks do not
  rely on colour (WCAG 1.4.1).
- **java-diff-utils finds the differences; `TextDiff` only arranges them.** The library's
  `DiffRowGenerator` is not used: it returns strings with the markup already inserted, which the page
  would have to print with `th:utext`. `TextDiff` returns plain text segments marked or not, and the
  template escapes every one.

## Consequences

**Good:** the moderator sees what changed without rereading the article, on any screen. Storage is
untouched: revisions stay full copies (ADR-0003), and the diff is computed per page view.

**Bad:** one more dependency to keep current. The moderator reads Markdown syntax (`**`, `[[ ]]`) in
the comparison; the rendered text is one click away under "Show the full proposed text". An edit whose
article has since been removed has nothing to compare with, and the page shows the full text instead
— reachable only once FR-026 removes articles. The comparison
costs a diff per view, which is proportionate for articles of a few thousand words.

**Reversal:** `TextDiff` is the only class that imports the library; replacing it, or moving to
option B, touches that class, the review template and the CSS. Reconsider if moderators say the
Markdown syntax gets in the way — option C is then the next step, and needs a library for it first.

## Amendment, 8 October — the reading text is compared too (fix 3.7, F-25)

The walkthrough of 1 October found what the reversal note foresaw: the moderator reads `**` and the
source's hard line breaks in the comparison (F-25). Decided 8 Oct by the human:

- **Two views of the same comparison, the reading text first.** Each paragraph is turned into plain
  text by commonmark's own `TextContentRenderer` (already a dependency), so `**`, link targets and
  hard line breaks are gone and a `[[Title]]` reads as its title, and `TextDiff` compares those
  texts exactly as it compares the source. The Markdown view stays one switch away, without a page
  reload, so a summary being typed is not lost.
- **Not option C.** Nothing is diffed as HTML, no library is added, and every segment is still
  escaped by the template. A change of formatting alone (a word made bold) leaves the reading text
  the same; that view then says so and points to the Markdown one, where the change is visible.

- **Tags as what changed.** Decided the same day by the human after trying it: the tags row shows
  only the tags the edit removes (struck through) and adds (underlined), not the whole old list and
  the whole new one, where one added tag was lost among four kept ones. Title and summary stay old
  and new.

**Consequence:** the comparison is computed twice per view. Paragraphs stay the source's paragraphs,
so a row of one view is the same row in the other.
