---
id: FEAT-015
title: Tags ordered by visits, with their article counts
status: done
covers: [FR-031]
slice: taxonomy
routes: ["GET /", "GET /tags/{tag}"]
tables: [tag, article_tag, article]
code:
  - app/src/main/resources/db/migration/V7__add_tag_visit_count.sql
  - app/src/main/java/in/ac/iitm/guide/shared/persistence/Tag.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/persistence/TagRepository.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/internal/TagBrowseService.java
  - app/src/main/java/in/ac/iitm/guide/taxonomy/web/TagController.java
  - app/src/main/java/in/ac/iitm/guide/home/persistence/LandingReadRepository.java
  - app/src/main/java/in/ac/iitm/guide/home/internal/LandingPage.java
  - app/src/main/java/in/ac/iitm/guide/home/internal/LandingPageService.java
  - app/src/main/resources/templates/home/Landing.html
  - app/src/main/resources/static/css/site.css
tests:
  - app/src/test/java/in/ac/iitm/guide/home/LandingControllerTest.java
  - app/src/test/java/in/ac/iitm/guide/taxonomy/TagBrowseTest.java
---

# FEAT-015 — Tags ordered by visits, with their article counts

> Created under the contract the human confirmed on 30 Sep. The feature was first asked for on 23 Sep,
> but its requirement never reached `functional.md`, and the number it was drafted under, FR-027, went
> to the editor.

## Why

The landing page lists up to 50 tags alphabetically, with nothing telling a reader which tags lead
somewhere substantial or which ones other students actually use. With a count beside each tag, a
reader can see that "visa" holds six articles and "laundry" one. Ordering by visits puts the tags
students open most within reach first.

## Scenario

1. A reader opens the landing page. Under "Tags" each tag shows its name and, quieter, the number of
   live articles carrying it; the hidden text beside the number makes it read as "visa 6 articles".
2. The reader opens "visa". The tag page answers as before, and the visit is counted.
3. The next reader to open the landing page finds "visa" at or nearer the front. Among tags visited
   equally often, the order is alphabetical.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /` | the tag list, ordered and counted | `home/Landing.html` |
| `GET /tags/{tag}` | a `200` counts one visit to every tag behind the address; a `404` counts none | `taxonomy/TagBrowse.html` |

## Schema impact

`tag.visit_count BIGINT NOT NULL DEFAULT 0` (V7, [ADR-0017](../architecture/adr/ADR-0017-counting-tag-visits.md)).
It is written by one atomic `UPDATE`, which the tag page runs after it knows it will answer `200`.
The landing page reads the tags, their article counts and their order in one grouped query over
`article_tag`, so its query count is unchanged (`PageQueryCountTest`, ADR-0010). The article count is
not stored.

## Decisions this feature fixed

All four were confirmed by the human on 30 Sep, as suggested:

- **Every opening that answers counts**, a refresh included. With no accounts (CON-001) there is no
  one to deduplicate against.
- **The moderator's visits count** like anyone's.
- **The counter is not exported** (ADR-0007): it describes use, not content.
- **The count is shown on the landing page's tag list only**, not on the chips of article cards,
  which already sit beside the article they describe.

Tags whose names share one address (`visa/frro` and `visa frro`, FEAT-008) each count the visit to
their shared page, and each keeps its own chip and article count.

## Acceptance criteria

- [x] A tag carried by two published articles and one removed article is shown with the number 2 —
      `LandingControllerTest.each_tag_shows_how_many_published_articles_carry_it`
- [x] Tags visited equally often come first in alphabetical order, then the less visited ones —
      `LandingControllerTest.tags_are_listed_most_visited_first_and_by_name_when_visits_are_equal`
- [x] Opening a tag's page adds one visit each time, whatever the letter case of the address —
      `TagBrowseTest.opening_a_tags_page_counts_a_visit_each_time`
- [x] A tag page that answers `404` counts nothing —
      `TagBrowseTest.a_tag_page_that_answers_404_counts_no_visit`
- [x] Every tag behind one address counts the visit —
      `TagBrowseTest.every_tag_behind_one_address_counts_the_visit`

Evidence, 30 Sep: all five were red first. Four failed on the missing `visit_count` column, and the
count test failed on the absent number. All five are green after V7 and the code.

**Accepted by the human on 30 Sep** on the compose stand (PostgreSQL 17, V7 applied to the existing
volume): the counts beside the tags, a tag moving to the front after its page was opened, equal
visits in alphabetical order, and the list at a narrow width. On that acceptance FR-031 is `done`.

## Deliberately out of scope

- Any window on the count ("most visited this month"): ADR-0017's option B, if it is ever asked for.
- Protection against a script inflating a tag: waits for NFR-005's rate limit.
- Showing the number of visits: FR-031 orders by it and shows only the article count.

## Open questions

None.
