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
| FR-001 | in-progress | must | Reading a published article | 3 | 36 | 0 |
| FR-005 | planned | could | Creating an article from a red link | 1 | 0 | 1 |
| FR-006 | planned | could | Backlinks on an article | 3 | 0 | 3 |
| FR-011 | in-progress | must | Proposing an edit to an existing article | 5 | 8 | 0 |
| FR-013 | planned | should | Abuse handling without accounts | 2 | 0 | 2 |
| FR-016 | planned | should | Downloading a media asset | 3 | 2 | 1 |
| FR-021 | planned | could | Reporting an article | 2 | 1 | 1 |
| FR-022 | planned | could | Closing a report | 1 | 0 | 1 |
| FR-023 | planned | could | Publishing a new article directly | 4 | 0 | 4 |
| FR-024 | planned | could | Editing an article directly | 4 | 0 | 4 |
| FR-025 | planned | could | Editing the homepage's pinned articles | 2 | 0 | 2 |
| FR-026 | planned | should | Removing a published article | 4 | 0 | 4 |
| FR-028 | planned | should | Completing a wiki link while writing | 2 | 0 | 2 |
| NFR-002 | planned |  | Search latency | 0 | 0 | 0 |
| NFR-003 | planned |  | Multilingual content survival | 0 | 0 | 0 |
| NFR-004 | in-progress |  | Exportability | 0 | 24 | 0 |
| NFR-005 | planned |  | Submission rate limit | 0 | 0 | 0 |

## Done, with criteria that no test is anchored to

None.

## Open technical debt

| Debt | Title | Trigger |
|---|---|---|
| DEBT-017 | The editor's preview is not rate limited | NFR-005 in phase 4 ([04-hardening.md](roadmap/04-hardening.md)), or the first sign of |
| DEBT-016 | The files of rejected submissions stay in the media root | the volume passing half its limit, or phase 4's hardening |
| DEBT-015 | A file far over the container's limit gets a closed connection, not the page | the first report of an upload ending in a connection error, or phase 4's hardening. |
| DEBT-014 | The stand's session cookie is not marked `Secure` | the demo stand step of phase 4 ([04-hardening.md](roadmap/04-hardening.md)), and in any |
| DEBT-013 | The CSRF token is not replaced when the moderator logs in | the security review of phase 4 ([04-hardening.md](roadmap/04-hardening.md)). |
| DEBT-011 | Failed moderator logins are logged but not rate limited | NFR-005, phase 4 ([04-hardening.md](roadmap/04-hardening.md)) — and in any case before |
| DEBT-010 | An article's old address answers `404` after an edit changes its title | the first renamed article anyone complains about, or the phase 4 edge cases, whichever |
| DEBT-009 | An edit whose article stopped being published answers `404`, not `409` | FR-026 (removing a published article), phase 4. |
| DEBT-006 | The importer writes articles but no `article_link` rows | the first code that writes `article_link` — FR-006 in phase 4. The human decided on |
| DEBT-002 | The process layer cannot be packaged for a second repository | the first time a second repository needs this, or phase 5 handover — whichever comes |

## Gaps in the traceability chain

None.
