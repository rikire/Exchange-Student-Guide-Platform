---
id: FEAT-022
title: The OGE team and developers pages
status: done
covers: [FR-035]
slice: about
routes: ["GET /oge-team", "GET /developers"]
tables: []
code:
  - app/src/main/java/in/ac/iitm/guide/about/package-info.java
  - app/src/main/java/in/ac/iitm/guide/about/web/AboutController.java
  - app/src/main/resources/templates/about/OgeTeam.html
  - app/src/main/resources/templates/about/Developers.html
  - app/src/main/resources/templates/shared/web/Layout.html
  - app/src/main/resources/static/css/site.css
tests:
  - app/src/test/java/in/ac/iitm/guide/about/AboutPagesTest.java
  - app/src/test/java/in/ac/iitm/guide/SiteHeaderTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserLayoutTest.java
  - app/src/test/java/in/ac/iitm/guide/ModularityTest.java
---

# FEAT-022 — The OGE team and developers pages

## Why

A newcomer who needs a person, not an article, had nowhere on the guide to find who at OGE receives
exchange students, how to reach them, or where the office is. And the guide said nothing about who
built it or for whom. The human asked for both on 9 Oct and drew them as the mockups
[OgeTeam.html](../design/screens/OgeTeam.html) and [Developers.html](../design/screens/Developers.html).

## Scenario

1. A reader follows "OGE team" in the header of any page (on a phone, from the menu).
2. `/oge-team` shows the Dean first, then the two Inbound contacts with email, phone and room, and a
   "Visit OGE" sidebar: address, rooms, front office, a Google Maps search and the full team on
   ge.iitm.ac.in.
3. The reader follows "Developers": `/developers` shows "Built for" OGE and "By students of"
   Innopolis University, the three developers, why the guide was built, and thanks.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `GET /oge-team` | Who at OGE to contact, and how to visit | `about/OgeTeam.html` |
| `GET /developers` | Who built the guide, and for whom | `about/Developers.html` |

## Schema impact

None. The content is static in the two templates; a change is a commit, not a moderator's edit
(the human, 9 Oct, contract question 4).

## Acceptance criteria

- [x] the OGE team page answers 200 to anyone
- [x] the developers page answers 200 to anyone
- [x] the OGE team page puts the Dean before the Inbound contacts
- [x] the developers page names all three developers
- [x] the header links to the OGE team and developers pages
- [x] on a wide screen the header shows six links
- [x] every image on the about pages is served from the site

Both pages are also measured by `BrowserLayoutTest` at 320, 768, 1280 and 1920 px with axe, as every
built route is: on 9 Oct a long address overflowed the card at 320 px, so on a narrow phone the photo
now stands above the text and each label above its value.

## Deliberately out of scope

- Editing either page from the moderator's area: it would need a table, and the schema is frozen
  after phase 2.
- The rest of the OGE directory: only the Dean and the Inbound team serve an incoming student; the
  page links to ge.iitm.ac.in/team for everyone else.

## Open questions

None. Decided by the human on 9 Oct: a new slice `about`, static templates, FR-035's wording, and
the missing facts kept as visible blanks (DEBT-025).
