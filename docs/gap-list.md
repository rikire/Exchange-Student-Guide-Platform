# Gap list

Everything still planned or in progress, every open debt entry, and every requirement whose
acceptance criteria outnumber the tests anchored to it. Hiding a known gap is explicitly worse
than declaring it.

**Generated** by `java -jar tools/target/ai-tools.jar gaps`. An edit made here is lost on the next run.

## Requirements not done

"No test, at least" is the criteria minus the tests anchored to the requirement; the
real figure can only be higher.

| Requirement | Status | Priority | Title | Criteria | Tests | No test, at least |
|---|---|---|---|---|---|---|
| FR-001 | in-progress | must | Reading a published article | 3 | 28 | 0 |
| FR-003 | planned | must | Writing wiki links into a submission | 1 | 0 | 1 |
| FR-005 | planned | could | Creating an article from a red link | 1 | 0 | 1 |
| FR-006 | planned | could | Backlinks on an article | 3 | 0 | 3 |
| FR-007 | planned | must | Full-text search across articles | 4 | 0 | 4 |
| FR-008 | planned | should | Browsing articles by tag | 4 | 2 | 2 |
| FR-010 | planned | must | Submitting a new article | 4 | 2 | 2 |
| FR-011 | planned | must | Proposing an edit to an existing article | 5 | 0 | 5 |
| FR-012 | planned | could | Looking up a submission's status | 3 | 0 | 3 |
| FR-013 | planned | should | Abuse handling without accounts | 2 | 0 | 2 |
| FR-014 | planned | must | Moderation queue | 2 | 0 | 2 |
| FR-015 | planned | must | Reviewing a submission | 2 | 0 | 2 |
| FR-016 | planned | should | Downloading a media asset | 3 | 0 | 3 |
| FR-017 | planned | must | Approving a submission | 3 | 0 | 3 |
| FR-018 | planned | must | Rejecting a submission | 2 | 0 | 2 |
| FR-019 | planned | could | Providing a rejection reason | 2 | 0 | 2 |
| FR-020 | planned | should | Retaining article revisions | 1 | 1 | 0 |
| FR-021 | planned | could | Reporting an article | 2 | 1 | 1 |
| FR-022 | planned | could | Closing a report | 1 | 0 | 1 |
| FR-023 | planned | could | Publishing a new article directly | 4 | 0 | 4 |
| FR-024 | planned | could | Editing an article directly | 4 | 0 | 4 |
| FR-025 | planned | could | Editing the homepage's pinned articles | 2 | 0 | 2 |
| FR-026 | planned | should | Removing a published article | 4 | 0 | 4 |
| NFR-001 | planned |  | Upload size limit | 0 | 0 | 0 |
| NFR-002 | planned |  | Search latency | 0 | 0 | 0 |
| NFR-003 | planned |  | Multilingual content survival | 0 | 0 | 0 |
| NFR-004 | in-progress |  | Exportability | 0 | 24 | 0 |
| NFR-005 | planned |  | Submission rate limit | 0 | 0 | 0 |
| NFR-006 | planned |  | Submission number unguessability | 0 | 0 | 0 |

## Done, with criteria that no test is anchored to

None.

## Open technical debt

| Debt | Title | Trigger |
|---|---|---|
| DEBT-006 | The importer writes articles but no `article_link` rows | the first code that writes `article_link` — the extractor in phase 3, or FR-006 in phase |
| DEBT-005 | The tag rule of ADR-0005 is written twice until `taxonomy` exists | the first line of code in the `taxonomy` slice. `contribute` (phase 3) will also write |
| DEBT-004 | Four `app/pom.xml` dependencies are declared with no code or test using them yet | already past — recorded at creation, not deferred. |
| DEBT-002 | The process layer cannot be packaged for a second repository | the first time a second repository needs this, or phase 5 handover — whichever comes |

## Gaps in the traceability chain

None.
