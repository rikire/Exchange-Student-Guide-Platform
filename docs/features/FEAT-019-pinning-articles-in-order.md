---
id: FEAT-019
title: Pinning articles to the landing page, in order
status: done
covers: [FR-025]
slice: moderate
routes: ["GET /moderate/articles", "POST /moderate/articles/{title}/pin", "POST /moderate/articles/{title}/unpin", "POST /moderate/articles/{title}/up", "POST /moderate/articles/{title}/down"]
tables: [article]
code:
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ArticlePinning.java
  - app/src/main/java/in/ac/iitm/guide/moderate/web/ArticlePinningController.java
  - app/src/main/java/in/ac/iitm/guide/moderate/persistence/ModerateArticleRepository.java
  - app/src/main/java/in/ac/iitm/guide/home/persistence/LandingReadRepository.java
  - app/src/main/resources/templates/moderate/ArticleAdmin.html
  - app/src/main/resources/data/seed/registering-with-frro.md
tests:
  - app/src/test/java/in/ac/iitm/guide/moderate/PinningTest.java
  - app/src/test/java/in/ac/iitm/guide/backup/SeedRunnerTest.java
---

# FEAT-019 — Pinning articles to the landing page, in order

## Why

The walkthrough of 1 Oct (F-7) found *Registering with FRRO*, the first thing a new student needs,
missing from the landing page. The landing page already had a pinned section, but nothing let OGE
pin an article. FR-025 was a phase-4 `could`; the human took it into phase 3 on 1 Oct and asked for
the pinned order to be set by hand.

## Scenario

1. The signed-in moderator opens `/moderate/articles`, the HomeAdmin design screen. The pinned
   articles are listed in their order, and every published article is listed by title, 50 to a page.
2. "Pin" puts an article last among the pinned. "Move up" and "Move down" swap it with its neighbour,
   and "Unpin" takes it out. "Remove" leads to FR-026's confirmation.
3. The landing page's pinned section follows that order.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /moderate/articles` | The screen | `moderate/ArticleAdmin.html` |
| `POST /moderate/articles/{title}/pin`, `/unpin`, `/up`, `/down` | The four actions | — (redirects) |

## Schema impact

`article.pin_position` (V8, [ADR-0021](../architecture/adr/ADR-0021-article-views-and-pin-order.md)),
added with FEAT-018. `pinned_at` still records when an article was pinned.

## Decisions this feature fixed

- **A new pin goes last; moving swaps places with the neighbour in the order**, so a gap left by an
  unpinned or removed article never matters and nothing is renumbered.
- **Pinning a pinned article, or moving the first up or the last down, changes nothing** and is not
  an error.
- **No link to the screen outside the moderator's header** (fix 2.3), by the human's choice over a
  temporary link on the queue (1 Oct). Until 2.3 the address is typed.
- **The seed pins FRRO first** (`pinned`, `pin: 1`), so a fresh stand shows it at the top.
- **Buttons are `button-quiet`**, the style already used by Log out. The design pass (fix 3.2) gives
  them their final look.

## Acceptance criteria

- [x] A pinned article appears in the landing page's pinned section; an unpinned one leaves it.
- [x] With A, B, C pinned, moving C up shows A, C, B; a newly pinned article goes after the others.
- [x] Moving the first up or the last down changes nothing.
- [x] The screen lists the published articles with Pin or Unpin, and Remove.
- [x] Without the login the screen redirects; a POST without the token is `403`; an unknown address is
      `404`.
- [x] `BrowserLayoutTest` passes the screen at all four widths.

**Fix 3.2, 2 Oct:** the page's actions wear the site's buttons (FEAT-011, [Buttons.html](../design/screens/Buttons.html)): "Pin", "Unpin", "Move up" and "Move down" secondary.

**F-22 check, 8 Oct** ([walkthrough-fixes 3.9](../verification/walkthrough-fixes.md), D3 and D4): "Remove" was left a plain link and is now `button-danger`, as on the article page; from 1024 px both tables take the frame's width, with every cell on its row's middle line.

## Deliberately out of scope

- Dragging to reorder, which would need a script.
- A limit on how many articles are pinned: the landing page shows the first twelve.

## Open questions

None.
