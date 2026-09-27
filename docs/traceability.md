# Traceability matrix

Requirement to feature to route to table to code to test, assembled from the anchors in the
artefacts themselves.

**Generated** by `java -jar tools/target/ai-tools.jar trace`. An edit made here is lost on the next run, and
`trace --check` refuses a file that no longer matches the repository.

Code and tests are the files carrying a `//trace:` anchor for the requirement, migrations the
ones carrying `-- trace:`. Routes and tables come from the feature files that cover it.

## Functional requirements

| Requirement | Status | Features | Code | Tests | Migrations | Routes | Tables | Title |
|---|---|---|---|---|---|---|---|---|
| FR-001 | in-progress | FEAT-002 | Article, ArticleAddress, ArticleController, ArticleNotFoundException, ArticleReadRepository, MediaAsset | ArticleAddressTest, ArticleControllerTest, NotFoundPageTest, PageQueryCountTest, SchemaMigrationTest | V1__create_content_and_moderation_schema, V3__add_media_asset_owner_constraint, V4__add_media_link_and_tag_lookup_indexes, V5__add_article_slug | GET /articles/{title} | article, article_tag, tag | Reading a published article |
| FR-002 | done | FEAT-001, FEAT-002 | ArticleController, TitleResolver, WikiLinkRenderer | ArticleAddressTest, ArticleControllerTest, WikiLinkRendererTest |  | GET /articles/{title} | article, article_tag, tag | Rendering wiki links |
| FR-003 | done | FEAT-005 | SubmissionService | SubmissionFlowTest |  | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, submission, submission_tag, tag | Writing wiki links into a submission |
| FR-004 | done | FEAT-001, FEAT-002 | ArticleController, ArticleLink, WikiLinkRenderer | ArticleControllerTest, SchemaMigrationTest, WikiLinkRendererTest | V1__create_content_and_moderation_schema | GET /articles/{title} | article, article_tag, tag | Red-link rendering |
| FR-005 | planned |  |  |  |  |  |  | Creating an article from a red link |
| FR-006 | planned |  |  |  | V4__add_media_link_and_tag_lookup_indexes |  |  | Backlinks on an article |
| FR-007 | done | FEAT-007 | ArticleSearchMapping, ArticleSearchService, EnglishAnalysis, SearchController, SearchIndexBuilder | SearchFlowTest |  | GET /search | article, article_tag, tag | Full-text search across articles |
| FR-008 | done | FEAT-005, FEAT-008 | Tag, TagBrowseRepository, TagBrowseService, TagController, TagLink, TagNotFoundException, TagRejectedException, TagRepository, Tags | SchemaMigrationTest, TagBrowseTest, TagsTest | V1__create_content_and_moderation_schema, V4__add_media_link_and_tag_lookup_indexes | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, GET /tags/{tag}, POST /articles/{title}/edits, POST /submissions | article, article_tag, submission, submission_tag, tag | Browsing articles by tag |
| FR-009 | done | FEAT-003, FEAT-004 | LandingController, LandingPage, LandingPageService, LandingReadRepository | ArticleArchiveTest, LandingControllerTest, PageQueryCountTest, SchemaMigrationTest | V1__create_content_and_moderation_schema, V6__add_article_published_at_index | GET / | article, article_tag, tag | Landing page |
| FR-010 | in-progress | FEAT-005 | ContributeArticleRepository, Submission, SubmissionController, SubmissionRejectedException, SubmissionRepository, SubmissionService, WebSecurity | SchemaMigrationTest, SubmissionFlowTest, SubmissionNumbersTest, WebSecurityTest | V1__create_content_and_moderation_schema | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, submission, submission_tag, tag | Submitting a new article |
| FR-011 | in-progress | FEAT-005 | ArticleNotPublishedException, ContributeArticleRepository, SubmissionController, SubmissionRejectedException, SubmissionService | SubmissionFlowTest |  | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, submission, submission_tag, tag | Proposing an edit to an existing article |
| FR-012 | planned |  |  |  |  |  |  | Looking up a submission's status |
| FR-013 | planned |  |  |  |  |  |  | Abuse handling without accounts |
| FR-014 | done | FEAT-006 | ModerateSubmissionRepository, ModerationController, ModerationService, ModeratorLoginController, WebSecurity | ModerationFlowTest, ModeratorLoginTest | V2__add_moderation_and_report_query_indexes | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Moderation queue |
| FR-015 | in-progress | FEAT-006 | ModerateSubmissionRepository, ModerationController, ModerationService | ModerationFlowTest | V4__add_media_link_and_tag_lookup_indexes | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Reviewing a submission |
| FR-016 | planned |  |  |  |  |  |  | Downloading a media asset |
| FR-017 | done | FEAT-006 | ModerateArticleRepository, ModerateSubmissionRepository, ModerationController, ModerationService | ModerationFlowTest |  | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Approving a submission |
| FR-018 | done | FEAT-006 | ModerateSubmissionRepository, ModerationController, ModerationService | ModerationFlowTest |  | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Rejecting a submission |
| FR-019 | planned |  |  |  |  |  |  | Providing a rejection reason |
| FR-020 | done | FEAT-006 | ModerateArticleRepository, ModerationService, Revision, RevisionRepository | ModerateArticleRepositoryTest, ModerationFlowTest, SchemaMigrationTest | V1__create_content_and_moderation_schema | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Retaining article revisions |
| FR-021 | planned |  | Report | SchemaMigrationTest | V1__create_content_and_moderation_schema, V2__add_moderation_and_report_query_indexes |  |  | Reporting an article |
| FR-022 | planned |  |  |  |  |  |  | Closing a report |
| FR-023 | planned |  |  |  |  |  |  | Publishing a new article directly |
| FR-024 | planned |  |  |  |  |  |  | Editing an article directly |
| FR-025 | planned |  |  |  |  |  |  | Editing the homepage's pinned articles |
| FR-026 | planned |  |  |  |  |  |  | Removing a published article |
| FR-027 | planned |  |  |  |  |  |  | Writing an article's body |
| FR-028 | planned |  |  |  |  |  |  | Completing a wiki link while writing |

## Non-functional requirements

| Requirement | Status | Features | Code | Tests | Migrations | Routes | Tables | Title |
|---|---|---|---|---|---|---|---|---|
| NFR-001 | planned |  |  |  |  |  |  | Upload size limit |
| NFR-002 | planned |  |  |  |  |  |  | Search latency |
| NFR-003 | planned |  |  |  |  |  |  | Multilingual content survival |
| NFR-004 | in-progress | FEAT-004 | ArchiveArticleRepository, ArchiveFormatException, ArchivedArticle, ArticleArchive, FrontMatter, ImportReport, SeedRunner | ArticleArchiveTest, ExportQueryTest, SeedRunnerTest |  |  | article, article_tag, tag | Exportability |
| NFR-005 | planned |  |  |  |  |  |  | Submission rate limit |
| NFR-006 | done | FEAT-005 | SubmissionNumbers | SubmissionNumbersTest |  | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, submission, submission_tag, tag | Submission number unguessability |
| NFR-007 | planned |  |  | BrowserLayoutTest |  |  |  | Accessibility |
| NFR-008 | planned |  |  | BrowserLayoutTest |  |  |  | Usable at any screen width |

## Constraints

| Requirement | Status | Features | Code | Tests | Migrations | Routes | Tables | Title |
|---|---|---|---|---|---|---|---|---|
| CON-001 |  |  |  |  |  |  |  | No user accounts |
| CON-002 |  |  |  |  |  |  |  | No discussion pages |
| CON-003 |  |  |  |  |  |  |  | No watchlists |
| CON-004 |  |  |  |  |  |  |  | No diffs |
| CON-005 |  |  |  |  |  |  |  | Single-language interface |
| CON-006 |  |  |  |  |  |  |  | Accepted media types |
| CON-008 |  |  |  |  |  |  |  | No machine-facing API, and so no OpenAPI specification |

## Gaps

None.

## Notes

None.
