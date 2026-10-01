# ADR-0020 — Times in the reader's own zone

**Status:** accepted (by the human, 1 Oct)
**Date:** 2026-10-01

## Context

The walkthrough of 1 Oct (F-23) found the moderation queue showing stored UTC. Fix 1.3 converted
every shown time to one office zone, `guide.time-zone` (Asia/Kolkata), labelled `IST`
(`shared/web/DisplayTime`). The human then asked for each person to see times in their own
computer's zone. Readers are exchange students, many still at home or just arrived with a laptop in
their home zone. Moderators sit in Chennai.

A server cannot know the browser's zone: no request header carries it. So the server can only send
one zone, and the browser has to convert it.

Constraints already decided:
- assets come only from WebJars on Maven Central, with no Node in the build (ADR-0013);
- the Content-Security-Policy allows scripts only from the site itself;
- the guide is in English whatever the browser asks for (the human, 28 Sep).

## Options

### A. The office's zone only (fix 1.3 as built)

One zone, labelled, with no script. A reader abroad converts in their head. An article updated late
on 30 Sep UTC reads "1 Oct" to everyone.

### B. `@github/relative-time-element` 5.3.1 (MIT), WebJar `org.webjars.npm:github__relative-time-element:5.3.1`

GitHub's web component over `<time>`: `<relative-time datetime="…">fallback</relative-time>`.
- In the browser it rewrites the text with `Intl.DateTimeFormat` and `Intl.RelativeTimeFormat` in
  the computer's zone, and sets a `title` with the full local date and time.
- Within 30 days it says "2 hours ago"; after that it says "on 1 Oct 2026" (its `threshold`
  default, `P30D`).
- It reads the language from the nearest `lang`, so `lang="en-GB"` keeps English and the day-month
  order.
- About 9 KB gzipped, no dependencies, an ES module that registers itself.
- No inline style: the bundle's `style` keys are `Intl` options, read 1 Oct.
- No WebJar existed. The human chose to deploy one, and it was deployed from webjars.org on 1 Oct.
  This is the version from npm, not a fork.
- Without JavaScript, the fallback text stays: fix 1.3's labelled IST.

### C. Our own script on `Intl`

About 30 lines doing the same. Weighed only because B had no WebJar. Once the WebJar existed, it
would be our own version of what a maintained library does.

### D. The file copied into `static/`

The same library without a WebJar. This is against ADR-0013's rule, for no gain once the WebJar
exists.

## Decision

**B**, chosen by the human on 1 Oct over A, C and D.

Where it is used:
- **The moderator's queue** shows how long ago a submission was sent, the full local time on hover.
- **An article's "Updated"** (the tag page now, the landing page and article page when they show it)
  is relative within 30 days and a date after that, the full local time on hover.

The server-rendered `DisplayTime` text stays inside each element as the fallback for no JavaScript,
printing and a saved PDF.

**The one thing that decided it:** it is the established pattern (GitHub, GitLab), and it keeps the
page a cacheable server rendering with a correct fallback.

## Consequences

**Good:**
- Each reader sees a time that is right where they are.
- "3 days ago" has no midnight-boundary problem.
- New pages get it by wrapping their time in the element.

**Bad:**
- A time is no longer the same text for everyone.
- A test of what a reader sees needs a real browser (`-P browser`, with Playwright's `timezoneId`
  and clock). MockMvc sees only the fallback.
- The version is pinned in three places: the root pom, the fragment that loads it, and this ADR.
- We now depend on a WebJar we deployed ourselves. A new version needs a new deploy from
  webjars.org.

**Reversal:** remove the fragment and the dependency, and the pages show the fallback, which is
option A. Reconsider if OGE wants one official time on the page, such as a deadline that must read
the same for everyone.
