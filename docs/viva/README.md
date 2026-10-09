# Viva preparation

A map of the code for whoever has to change it on the spot: where each thing lives, how a request
travels through it, and where a typical change goes. The mid-evaluation of October gives each of us a
live change to make; the final viva asks about our own slices and the partner's
([../course/rubric.md](../course/rubric.md)). Who wrote most of each slice is in
[../team/ownership.md](../team/ownership.md); both of us prepare on all of them.

## How any request travels

One Spring Boot application (`app/`), cut into **slices**, each a package under
`app/src/main/java/in/ac/iitm/guide/<slice>/`. Inside every slice the same four places:

| Package | Holds | Who may use it |
|---|---|---|
| `<slice>/` (the root) | the slice's **published** types: what other slices may call | any slice |
| `<slice>/web/` | `@Controller` classes: a route in, a template name out | nobody else |
| `<slice>/internal/` | services, rules, settings | nobody else |
| `<slice>/persistence/` | Spring Data repositories, queries | nobody else |

`ModularityTest` (Spring Modulith) and `ArchitectureRulesTest` (ArchUnit) fail the build when one slice
reaches into another's `web/`, `internal/` or `persistence/`. Entities shared by several slices are in
`shared/persistence/` (`Article`, `Submission`, `Tag`, `MediaAsset`, `Revision`, `Report`).

So a request is always: **route → `web/` controller → `internal/` service → `persistence/`
repository → back to the controller → Thymeleaf template** in
`app/src/main/resources/templates/<slice>/`. Every page is wrapped by
`templates/shared/web/Layout.html` (header, footer, styles). The schema is in
`app/src/main/resources/db/migration/` (Flyway, `V1`…`V8`); settings in
`app/src/main/resources/application.yml` under `guide:`. Every route is listed in
[../architecture/ui-routes.md](../architecture/ui-routes.md), generated from `routes.yml`.

## The slices

| Slice | What it does | Routes | Start reading at | Tests |
|---|---|---|---|---|
| `home` | the landing page: pinned and recent articles, tags | `GET /` | `web/LandingController`, `internal/LandingPageService` | `LandingControllerTest` |
| `articleview` | an article page, the list of all articles, view counts | `GET /articles`, `GET /articles/{title}` | `web/ArticleController` | `ArticleControllerTest`, `ArticleListTest`, `NotFoundPageTest` |
| `wikilink` | `[[Title]]` in Markdown → links, addresses from titles | none (a library the others call) | `WikiLinkRenderer`, `ArticleAddress` | `WikiLinkRendererTest`, `ArticleAddressTest`, `WikiLinkPropertiesTest` |
| `backlink` | "What links here" under an article | none (fed by the `ArticleTextChanged` event) | `internal/LinkIndexer`, `Backlinks` | `BacklinkFlowTest` |
| `search` | full-text search, Lucene through Hibernate Search | `GET /search` | `web/SearchController`, `internal/ArticleSearchService`, `internal/ArticleSearchMapping` | `SearchFlowTest` |
| `taxonomy` | tags: storing them once, browsing by tag | `GET /tags`, `GET /tags/{tag}` | `Tags` (the only writer of tags), `web/TagController` | `TagsTest`, `TagBrowseTest` |
| `contribute` | the anonymous submission form, the editor's preview, the status page | `GET /submit`, `POST /submissions`, `GET /articles/{title}/edit`, `POST /articles/{title}/edits`, `/submissions/...` | `web/SubmissionController`, `internal/SubmissionService` | `SubmissionFlowTest`, `FieldErrorTest`, `TagFieldTest`, `EditFormTest` |
| `media` | uploads: type from content, size limits, photos re-encoded, delivery | `GET /media/{id}` | `MediaAssets`, `internal/AcceptedType`, `internal/PhotoEncoder`, `web/MediaController` | `MediaAssetsTest`, `MediaDeliveryTest` |
| `moderate` | the queue, review with a diff, approve or reject, direct publishing, pinning, removal | `/moderate/...` | `web/ModerationController`, `internal/ModerationService`, `internal/TextDiff` | `ModerationFlowTest`, `DirectPublishingTest`, `TextDiffTest` |
| `report` | a reader reports an article; the moderator's inbox | `/articles/{title}/report`, `/moderate/reports` | `web/ReportController`, `internal/ReportService` | `ReportFlowTest` |
| `backup` | export and import of all articles; the seed at start | none (a command and the `seed` profile) | `ArticleArchive`, `internal/SeedRunner`, `internal/FrontMatter` | `ArticleArchiveTest`, `SeedRunnerTest` |
| `about` | the OGE team and developers pages | `GET /oge-team`, `GET /developers` | `web/AboutController` | `AboutPagesTest` |
| `shared` | entities, security (login, CSP, rate limits), time display | `/moderate/login`, `/moderate/logout` | `security/WebSecurity`, `security/ModeratorLoginController`, `web/DisplayTime` | `WebSecurityTest`, `ContentSecurityPolicyTest`, `ModeratorLoginTest` |

Browser tests (`Browser*Test`, run with `-P browser`) and page-wide checks (`PageQueryCountTest`,
`RouteContractTest`, `TemplateTokensTest`) are at the root of `app/src/test/java/in/ac/iitm/guide/`.

## Three flows, file by file

**A reader searches.** `GET /search?q=…` → `search/web/SearchController` →
`internal/ArticleSearchService` builds the Lucene query (every word must match) over the fields
mapped in `internal/ArticleSearchMapping`; the body is indexed as plain text by `internal/BodyAsText`,
and `internal/Passage` cuts the marked passage → `templates/search/SearchResults.html`. The index is
rebuilt from the `article` table at every start (`internal/SearchIndexBuilder`, ADR-0004) and updated
on every commit through JPA.

**A student proposes an edit with a photo.** `GET /articles/{title}/edit` →
`contribute/web/SubmissionController.editForm` fills `FormPage` from the article →
`templates/contribute/SubmissionForm.html` (the editor is `static/js/editor.js`, tags
`static/js/tags.js`, the file `static/js/files.js`). `POST /articles/{title}/edits` → the controller
checks the rate limit (`internal/ContributionLimits`) → `internal/SubmissionService.submitEdit`
checks the draft (`check`: title, summary, body, address) and the title collision, saves a
`Submission` (`shared/persistence`), asks `taxonomy`'s `Tags.named` for the tags and `media`'s
`MediaAssets.attach` for the file → a submission number (`internal/SubmissionNumbers`) → redirect to
the confirmation page. A refusal is a `SubmissionRejectedException` naming its `Field`, shown beside
that field.

**The moderator approves.** `POST /moderate/submissions/{number}/approve` →
`moderate/web/ModerationController` → `internal/ModerationService.approve`: checks the summary and
the tags; for an edit it saves the old text as a `Revision`, then updates the `Article` (title, slug,
summary, body, tags), moves the media from the submission to the article (`MediaAssets.moveToArticle`)
and publishes `ArticleTextChanged`, on which `backlink` rewrites the links; the search index updates
when the transaction commits. The review page's diff is `internal/TextDiff`, on the text as read and
on the Markdown.

## Where a typical change goes

| Asked to… | Change | And check |
|---|---|---|
| change a limit (photo size, tags per article, submissions an hour) | `application.yml` under `guide:`; tag count is `Tags.MOST`, text lengths `Article.LONGEST_SUMMARY`, `BodyPreview.LONGEST_BODY` | the test of that limit, e.g. `SubmissionFlowTest.ten_tags_are_accepted_and_eleven_are_refused` |
| change wording on a page | the template under `templates/<slice>/`; the header and footer are `shared/web/Layout.html` | the controller's test asserts some texts |
| add a rule to the submission form | `contribute/internal/SubmissionService.check`, throwing `SubmissionRejectedException` with its `Field` | a case in `FieldErrorTest` or `SubmissionFlowTest`, red first |
| add a field to what is shown on an article | `articleview/web/ArticleController` puts it in the model, `templates/articleview/Article.html` shows it | `ArticleControllerTest` |
| add a column | a new migration `V9__….sql` (the schema is frozen: a new migration names its ADR), the entity in `shared/persistence/` | `SchemaMigrationTest`, `-P postgres` |
| add a route | the controller, then `docs/architecture/routes.yml` and `java -jar tools/target/ai-tools.jar routes` | `RouteContractTest`; the pre-push check refuses an undescribed controller change |
| change a colour or spacing | a token in `static/css/tokens.css`, never a literal in a template | `TemplateTokensTest` |
| change a search rule | `search/internal/ArticleSearchService` | `SearchFlowTest` |

## Commands

```bash
./mvnw -pl app spring-boot:run -Dspring-boot.run.profiles=dev,seed   # the app on :8080 with the seed
./mvnw -pl app test -Dtest=SubmissionFlowTest                        # one test class
./mvnw -pl app test -P browser -Dtest=BrowserEditorTest              # one browser test class
./mvnw verify                                                        # everything, as the hooks run it
```
