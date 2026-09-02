# Phase 1 — Requirements and design

**Status: not started.** Runs 5–11 September 2026. Ends at the **design document, due 11 September**.

## Goal

Decide what the system does and how it is shaped, before code is written under those decisions. The
design document is the deliverable; the repository documents are the source it is assembled from.

## Steps

- [ ] `FR` / `NFR` / `CON` in `docs/requirements/` for the product as agreed
      — check: every `FR` has a status and every `CON` a rationale
- [ ] Glossary — one vocabulary for article, submission, revision, tag, media asset
- [ ] CJM for three roles: reader, contributor, OGE moderator
      — check: the mid-demo scenario traces through the reader and contributor journeys end to end
- [ ] C4 levels 1–3 and the ERD in PlantUML — check: the diagram script renders them
- [ ] ADRs: slices and Modulith; moderation and the version-history groundwork; search and
      multilingual content; taxonomy; media storage and upload security; export format;
      abuse handling without accounts
      — check: each has at least two genuinely considered options
- [ ] `docs/architecture/ui-routes.md` — the route contract
- [ ] **Decided here, not earlier:** what the landing page contains beyond pinned items and search;
      media quotas per file, per submission and for the volume; the allowed file types
- [ ] Design reference from the IITM sites, into `docs/design/reference.md`
- [ ] Draft screens with `/design`: landing, article, search results, submission form, queue
- [ ] Test plan: at least one test per slice — check: the plan names the test, not just the module
- [ ] Revised milestone plan, risks and plan B tied to seams that exist in the code
- [ ] Assemble `docs/course/design-doc.md` and produce the PDF (2–4 pages)
- [ ] Content: 10 or more articles drafted

## Readiness criterion

The design document is submitted, and `/course-check design` finds evidence for all five marks with
no row resting on a template.

## Open questions

1. How much of the version-history groundwork to commit to in the ADR. Storage is nearly free and
   must start in phase 2 or the history cannot be reconstructed later; the UI is a nice-to-have.
2. Whether the landing page's pinned items are curated by the moderator or derived from activity.
   This affects the schema, so it cannot wait past this phase.
