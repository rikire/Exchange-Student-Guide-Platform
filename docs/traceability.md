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
| FR-001 | done | FEAT-002, FEAT-009 | Article, ArticleAddress, ArticleController, ArticleNotFoundException, ArticleReadRepository, MediaAsset, MediaAssets, MediaController, MediaItem, SimilarTitles | ArticleAddressTest, ArticleControllerTest, BrowserVideoTest, MediaDeliveryTest, NotFoundPageTest, PageQueryCountTest, SchemaMigrationTest, TemplateUnescapedOutputTest, WikiLinkRendererTest | V1__create_content_and_moderation_schema, V3__add_media_asset_owner_constraint, V4__add_media_link_and_tag_lookup_indexes, V5__add_article_slug | GET /articles/{title}, GET /media/{id}, POST /articles/{title}/edits, POST /submissions | article, article_tag, media_asset, tag | Reading a published article |
| FR-002 | done | FEAT-001, FEAT-002 | ArticleController, TitleResolver, WikiLinkRenderer | ArticleAddressTest, ArticleControllerTest, WikiLinkRendererTest |  | GET /articles/{title} | article, article_tag, tag | Rendering wiki links |
| FR-003 | done | FEAT-005 | SubmissionService | SubmissionFlowTest |  | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, submission, submission_tag, tag | Writing wiki links into a submission |
| FR-004 | done | FEAT-001, FEAT-002 | ArticleController, ArticleLink, WikiLinkRenderer | ArticleControllerTest, SchemaMigrationTest, WikiLinkRendererTest | V1__create_content_and_moderation_schema | GET /articles/{title} | article, article_tag, tag | Red-link rendering |
| FR-005 | done | FEAT-001 | ArticleController | NotFoundPageTest, SubmissionFlowTest, WikiLinkRendererTest |  |  |  | Creating an article from a red link |
| FR-006 | done | FEAT-014 | ArticleArchive, ArticleController, Backlinks, LinkArticleRepository, LinkBackfill, LinkIndexer, LinkRepository, ModerationService, WikiLinkRenderer | BacklinkFlowTest, LinkBackfillTest, WikiLinkRendererTest | V4__add_media_link_and_tag_lookup_indexes | GET /articles/{title} | article, article_link | Backlinks on an article |
| FR-007 | done | FEAT-007 | ArticleSearchMapping, ArticleSearchService, BodyAsText, EnglishAnalysis, SearchController, SearchIndexBuilder | PageQueryCountTest, SearchFlowTest, StartupOrderTest, WikiLinkRendererTest |  | GET /search | article, article_tag, tag | Full-text search across articles |
| FR-008 | done | FEAT-005, FEAT-008 | Tag, TagBrowseRepository, TagBrowseService, TagController, TagLink, TagNotFoundException, TagRejectedException, TagRepository, Tags | ArticleCardTest, BrowserLocalTimeTest, DisplayTimeTest, NotFoundPageTest, PageQueryCountTest, SchemaMigrationTest, TagBrowseTest, TagsTest | V1__create_content_and_moderation_schema, V4__add_media_link_and_tag_lookup_indexes | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, GET /tags, GET /tags/{tag}, POST /articles/{title}/edits, POST /submissions | article, article_tag, submission, submission_tag, tag | Browsing articles by tag |
| FR-009 | done | FEAT-003, FEAT-004 | LandingController, LandingPage, LandingPageService, LandingReadRepository | ArticleArchiveTest, ArticleCardTest, BrowserLayoutTest, LandingControllerTest, PageQueryCountTest, SchemaMigrationTest, SiteHeaderTest | V1__create_content_and_moderation_schema, V6__add_article_published_at_index | GET / | article, article_tag, tag | Landing page |
| FR-010 | done | FEAT-005, FEAT-009 | AcceptedType, ContributeArticleRepository, MediaAssets, MediaFiles, MediaKind, MediaRejectedException, PhotoEncoder, Submission, SubmissionController, SubmissionRejectedException, SubmissionRepository, SubmissionService, Upload, UploadTooLargeAdvice, WebSecurity | EditorPreviewTest, MediaAssetsTest, SchemaMigrationTest, SubmissionFlowTest, SubmissionNumbersTest, UploadTooLargeTest, WebSecurityTest | V1__create_content_and_moderation_schema | GET /articles/{title}/edit, GET /media/{id}, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, media_asset, submission, submission_tag, tag | Submitting a new article |
| FR-011 | done | FEAT-005, FEAT-009 | AcceptedType, ArticleNotPublishedException, ArticleRemovedWhileEditingException, ContributeArticleRepository, MediaAssets, MediaRejectedException, SubmissionController, SubmissionRejectedException, SubmissionService, Upload, UploadTooLargeAdvice | SubmissionFlowTest |  | GET /articles/{title}/edit, GET /media/{id}, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, media_asset, submission, submission_tag, tag | Proposing an edit to an existing article |
| FR-012 | done | FEAT-012 | ContributeArticleRepository, SubmissionController, SubmissionRepository, SubmissionService | BrowserCopyNumberTest, SubmissionStatusTest |  | GET /submissions/status | article, submission | Looking up a submission's status |
| FR-013 | done | FEAT-017 | ContributionLimits | RateLimitTest |  | POST /articles/{title}/edits, POST /contribute/preview, POST /moderate/login, POST /moderate/logout, POST /submissions |  | Abuse handling without accounts |
| FR-014 | done | FEAT-006 | ModerateSubmissionRepository, ModerationController, ModerationService, ModeratorFrame, ModeratorLoginController, WebSecurity | BrowserLocalTimeTest, DisplayTimeTest, ModerationFlowTest, ModeratorLoginTest, ModeratorLoginWithoutHashTest, SessionCookieTest | V2__add_moderation_and_report_query_indexes | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Moderation queue |
| FR-015 | done | FEAT-006, FEAT-009 | MediaAssets, MediaController, MediaItem, ModerateSubmissionRepository, ModerationController, ModerationService | MediaDeliveryTest, ModerationFlowTest | V4__add_media_link_and_tag_lookup_indexes | GET /media/{id}, GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /articles/{title}/edits, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject, POST /submissions | article, article_tag, media_asset, revision, submission, submission_tag, tag | Reviewing a submission |
| FR-016 | done | FEAT-009 | MediaController | MediaDeliveryTest |  | GET /media/{id}, POST /articles/{title}/edits, POST /submissions | media_asset | Downloading a media asset |
| FR-017 | done | FEAT-006 | ModerateArticleRepository, ModerateSubmissionRepository, ModerationController, ModerationService | MediaAssetsTest, ModerationFlowTest |  | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Approving a submission |
| FR-018 | done | FEAT-006 | ModerateSubmissionRepository, ModerationController, ModerationService | ModerationFlowTest |  | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Rejecting a submission |
| FR-019 | done | FEAT-006 | ModerationController, ModerationService | ModerationFlowTest |  | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Providing a rejection reason |
| FR-020 | done | FEAT-006 | ModerateArticleRepository, ModerationService, Revision, RevisionRepository | ModerateArticleRepositoryTest, ModerationFlowTest, SchemaMigrationTest | V1__create_content_and_moderation_schema | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Retaining article revisions |
| FR-021 | done | FEAT-020 | ArticleController, Report, ReportController, ReportRepository, ReportService, ReportSettings, ReportedArticleRepository | ReportFlowTest, SchemaMigrationTest | V1__create_content_and_moderation_schema, V2__add_moderation_and_report_query_indexes | GET /articles/{title}/report, GET /moderate/reports, POST /articles/{title}/reports, POST /moderate/reports/{id}/close | article, report | Reporting an article |
| FR-022 | done | FEAT-020 | ReportController, ReportRepository, ReportService | ReportFlowTest |  | GET /articles/{title}/report, GET /moderate/reports, POST /articles/{title}/reports, POST /moderate/reports/{id}/close | article, report | Closing a report |
| FR-023 | done | FEAT-021 | DirectPublishing, DirectPublishingController, SubmissionRejectedException, Submissions | DirectPublishingTest |  | GET /moderate/articles/{title}/edit, GET /moderate/write, POST /moderate/articles, POST /moderate/articles/{title}/edits | article, media_asset, revision, submission | Publishing a new article directly |
| FR-024 | done | FEAT-021 | ArticleController, DirectPublishing, DirectPublishingController, SubmissionRejectedException, Submissions | DirectPublishingTest |  | GET /moderate/articles/{title}/edit, GET /moderate/write, POST /moderate/articles, POST /moderate/articles/{title}/edits | article, media_asset, revision, submission | Editing an article directly |
| FR-025 | done | FEAT-019 | ArticleAdmin, ArticlePinning, ArticlePinningController | ArticleArchiveTest, LandingControllerTest, PinningTest, SeedRunnerTest | V8__add_article_view_count_and_pin_position | GET /moderate/articles, POST /moderate/articles/{title}/down, POST /moderate/articles/{title}/pin, POST /moderate/articles/{title}/unpin, POST /moderate/articles/{title}/up | article | Editing the homepage's pinned articles |
| FR-026 | done | FEAT-013 | ArticleController, ArticleRemoval, ArticleRemovalController, ModerateArticleRepository | ArticleRemovalTest, SubmissionFlowTest |  | GET /moderate/articles/{title}/remove, POST /moderate/articles/{title}/remove | article | Removing a published article |
| FR-027 | done | FEAT-010 | BodyPreview, ContributeArticleRepository, PreviewController, WebSecurity | BrowserEditorTest, ContentSecurityPolicyTest, EditorPreviewTest |  | POST /contribute/preview | article | Writing an article's body |
| FR-028 | planned |  |  |  |  |  |  | Completing a wiki link while writing |
| FR-029 | done | FEAT-006 | ModerationService | ModerationFlowTest, TextDiffTest |  | GET /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/login, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | article, article_tag, revision, submission, submission_tag, tag | Seeing what an edit changes |
| FR-030 | done | FEAT-002 | ArticleController | ArticleControllerTest, BrowserPrintTest |  | GET /articles/{title} | article, article_tag, tag | Saving an article as a PDF |
| FR-031 | done | FEAT-015 | LandingReadRepository, TagBrowseService, TagController, TagRepository | LandingControllerTest, TagBrowseTest | V7__add_tag_visit_count | GET /, GET /tags/{tag} | article, article_tag, tag | Tags ordered by visits, with their article counts |
| FR-032 | done | FEAT-016 | PhotoViewer | BrowserPhotoViewerTest |  | GET /articles/{title}, GET /moderate/submissions/{number} |  | Viewing photos full screen |
| FR-033 | done | FEAT-018 | ArticleList, ArticleListController, ArticleListRepository | ArticleCardTest, ArticleListTest, PageQueryCountTest |  | GET /articles, GET /articles/{title} | article, article_tag, tag | Browsing every article |
| FR-034 | done | FEAT-018 | ArticleController, ArticleViewRepository | ArticleArchiveTest, ArticleViewCountTest, SeedRunnerTest | V8__add_article_view_count_and_pin_position | GET /articles, GET /articles/{title} | article, article_tag, tag | Counting an article's views |

## Non-functional requirements

| Requirement | Status | Features | Code | Tests | Migrations | Routes | Tables | Title |
|---|---|---|---|---|---|---|---|---|
| NFR-001 | done | FEAT-009 | MediaAssets, MediaKind, MediaRejectedException, MediaSettings, RejectedMediaSweep | MediaAssetsTest, MediaConfigurationTest, RejectedMediaSweepTest |  | GET /media/{id}, POST /articles/{title}/edits, POST /submissions | media_asset | Upload size limit |
| NFR-002 | done | FEAT-007 |  |  |  | GET /search | article, article_tag, tag | Search latency |
| NFR-003 | done | FEAT-007 | EnglishAnalysis | SearchFlowTest |  | GET /search | article, article_tag, tag | Multilingual content survival |
| NFR-004 | done | FEAT-004 | ArchiveArticleRepository, ArchiveFormatException, ArchivedArticle, ArticleArchive, FrontMatter, ImportReport, SeedRunner | ArticleArchiveTest, ExportQueryTest, SeedRunnerTest, StartupOrderTest |  |  | article, article_tag, tag | Exportability |
| NFR-005 | done | FEAT-017 | ContributionLimitSettings, ContributionLimits, ModeratorLoginController, PreviewController, SubmissionController | RateLimitTest |  | POST /articles/{title}/edits, POST /contribute/preview, POST /moderate/login, POST /moderate/logout, POST /submissions |  | Submission rate limit |
| NFR-006 | done | FEAT-005 | SubmissionNumber, SubmissionNumbers | SubmissionNumbersTest |  | GET /articles/{title}/edit, GET /submissions/{number}/confirmation, GET /submit, POST /articles/{title}/edits, POST /submissions | article, submission, submission_tag, tag | Submission number unguessability |
| NFR-007 | done | FEAT-011 | Layout | BrowserKeyboardTest, BrowserLayoutTest, ButtonStylesTest |  |  |  | Accessibility |
| NFR-008 | done | FEAT-011 | Layout | BrowserLayoutTest |  |  |  | Usable at any screen width |

## Constraints

| Requirement | Status | Features | Code | Tests | Migrations | Routes | Tables | Title |
|---|---|---|---|---|---|---|---|---|
| CON-001 |  |  |  |  |  |  |  | No user accounts |
| CON-002 |  |  |  |  |  |  |  | No discussion pages |
| CON-003 |  |  |  |  |  |  |  | No watchlists |
| CON-004 |  |  |  |  |  |  |  | No stored diffs |
| CON-005 |  |  |  |  |  |  |  | Single-language interface |
| CON-006 |  |  |  |  |  |  |  | Accepted media types |
| CON-008 |  |  |  |  |  |  |  | No machine-facing API, and so no OpenAPI specification |
| CON-009 |  |  |  |  |  |  |  | Two approvals under one address at the same moment are not answered gracefully |
| CON-010 |  |  |  |  |  |  |  | No CAPTCHA on the forms |

## Gaps

None.

## Notes

None.
