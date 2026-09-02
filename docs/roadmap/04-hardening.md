# Phase 4 — Hardening, security, deployment

**Status: not started.** Runs 10–30 October 2026.

## Goal

Make it survive real use and real inputs, and make it something a third person can stand up.

## Steps

- [ ] Edge cases: empty query, injection attempt, HTML in article text, duplicate titles, a title
      over 100 characters, circular wiki links, a corrupt import archive, a file whose extension
      lies about its content, a file over the limit
- [ ] The full list in `docs/ai/security.md`, plus a security review over the diff
- [ ] Load check: 100 articles of 500 words, search under 2 seconds
- [ ] PostgreSQL profile with Testcontainers
- [ ] `docker-compose.yml` with volumes for media and the search index; one-command start scripts
- [ ] Demo stand: compose plus the real content
- [ ] **Meeting with OGE** — show Mr. Thukaram the working stand, record what he says in
      `docs/stakeholder/` and turn it into requirements or constraints

## Readiness criterion

A third person stands the application up from the written instructions on a clean machine. Edge
cases are covered by tests rather than by having been tried once.

## Open questions

1. How much of the stakeholder's feedback can still be absorbed at this point without putting the
   final deadline at risk. Decide the cut-off before the meeting, not during it.
