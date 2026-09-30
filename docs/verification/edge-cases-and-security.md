# Edge cases and the security list

Evidence for two steps of [phase 4](../roadmap/04-hardening.md), gathered on 30 September 2026:
the nine edge cases, each a named test shown to fail with its guard removed, and every rule of
[security.md](../architecture/security.md) traced to a test, a requirement not yet built or a debt
entry. The security review of the same day closes the section at the end.

## The nine edge cases

"Red without the guard" means: the guard in the named file was changed as shown, only that test
was run and failed with the message given, and the file was then restored. The script applied one
change at a time and restored the file from its saved copy; `git status` showed `app/src/main`
clean afterwards.

| # | Case | Test | Guard removed | Red without it |
|---|---|---|---|---|
| 1 | Empty query | `SearchFlowTest.a_blank_query_answers_400` | `SearchController`: `query == null \|\| query.isBlank()` → `query == null` | `Status expected:<400> but was:<200>` |
| 2 | Injection, Lucene | `SearchFlowTest.query_syntax_typed_into_the_search_box_is_searched_as_words_not_obeyed` (new) | `ArticleSearchService`: `f.match()` → `f.simpleQueryString()`, which obeys `*` | `acc*` found "Bank account" |
| 2 | Injection, SQL | `SubmissionFlowTest.sql_typed_into_a_submission_is_stored_as_written_and_runs_nothing` (new) | `SubmissionService.save`: a native query built by concatenating the title, added before the save | H2 `42000`, syntax error in `... title = 'Bank'); DROP TABLE ...`: the request failed |
| 3 | HTML in article text | `ArticleControllerTest.raw_html_in_a_body_reaches_the_page_as_text` | `WikiLinkRenderer`: `escapeHtml(true)` → `false` | the `<script>` reached the page |
| 4 | Duplicate titles | `SubmissionFlowTest.a_title_matching_an_existing_articles_case_insensitively_is_refused_with_a_link_to_propose_an_edit` | `SubmissionService.submitNewArticle`: the `findBySlug` collision check replaced by `Optional.empty()` | `expected: 422` |
| 5 | Title over the limit | `SubmissionFlowTest.a_title_of_255_characters_is_accepted_and_one_of_256_is_refused` | `SubmissionService.check`: `title.length() > LONGEST_TITLE` → `false` | H2 `22001`, value too long for `VARCHAR(255)`: `500`, not the form |
| 6 | Circular wiki links | `BacklinkFlowTest.two_articles_linking_to_each_other_both_render_and_each_lists_the_other` (new) | **none to remove** | — |
| 7 | A corrupt import archive | `ArticleArchiveTest.a_file_that_is_not_utf8_text_stops_the_import_rather_than_arriving_with_replaced_characters` (new) | `ArticleArchive.importFrom`: `Files.readString(path, UTF_8)` → `new String(Files.readAllBytes(path), UTF_8)` | `Expecting code to raise a throwable`: the article was imported |
| 8 | Extension that lies about the content | `MediaAssetsTest.a_file_not_of_an_accepted_type_is_refused_and_nothing_is_stored`, cases "html named as a document" and "html named as a video" (new) | `MediaAssets.typeOf`: `tika.detect(in)` → `tika.detect(upload.originalName())` | both new cases stored the file |
| 9 | A file over the limit | `MediaAssetsTest.a_document_exactly_at_the_document_limit_is_stored_and_one_byte_over_is_refused` | `MediaAssets.attach`: `upload.size() > limit.toBytes()` → `false` | `Expecting code to raise a throwable` |

Notes on four rows:

- **Row 5.** The roadmap named a 100-character limit. FR-010's form and the schema allow 255, and
  that limit was kept on 30 Sep by the human's decision; the roadmap now says 255.
- **Row 6.** No code follows a wiki link further than one step: the renderer asks only whether each
  linked title is published, and the backlink list reads one table. There is no recursion to guard,
  so no guard can be removed. The test pins the behaviour, and a recursive renderer would fail it by
  overflowing the stack.
- **Row 7.** The other ways an archive can be corrupt, such as no front matter, invalid YAML, a
  missing key, a wrong type or a key given twice, are the thirteen cases of
  `a_file_that_cannot_become_an_article_is_refused_naming_the_file_and_the_fault`. One bad file
  leaving the database unchanged is `one_bad_file_leaves_the_database_as_it_was`. The new test
  refuses a Latin-1 file with `UncheckedIOException("cannot read the archive directory …")`, which
  does not name the file; the other bad files are named.
- **Row 8.** Before 30 Sep this test stayed green with Tika removed. Its only lying file was
  HTML named `.jpg`, and photo re-encoding refused that file on its own. A document and a video are
  stored byte for byte, so the new cases are the ones only Tika stops.

## The security list

Every rule of [security.md](../architecture/security.md), in its order. A rule that is not built
points to its requirement or debt entry, as the human decided on 30 Sep, rather than being recorded
as a constraint.

### Uploads

| Rule | Held by |
|---|---|
| Type decided by content | `MediaAssetsTest.a_file_not_of_an_accepted_type_is_refused_and_nothing_is_stored` (row 8 above) |
| Polyglots: photos re-encoded | `MediaAssetsTest.a_stored_photo_is_re_encoded_so_bytes_hidden_after_its_image_data_are_not_kept` |
| Allowlist: audio, executables, archives, SVG, HTML refused | the same parameterized test: SVG, HTML, EXE, ZIP, DOCX, GIF, MP3 (added 30 Sep), text, empty |
| Stored name generated by the system | `MediaAssetsTest.an_original_name_that_climbs_directories_does_not_reach_the_stored_path` |
| Size limits before bytes are written | `MediaAssetsTest`: photo, document (at and one over), video and volume limits; `UploadTooLargeTest` at the container. A file far over the container limit gets a closed connection: [DEBT-015](../tech-debt.md) |
| Delivery through a controller, path kept inside the root | `MediaDeliveryTest.an_unknown_id_and_one_that_is_not_an_id_answer_404`. The inside-the-root check in `MediaFiles` has no test of its own: a stored name is always a UUID and an allowlisted extension, so no request reaches it with anything else |
| `nosniff` always, `attachment` for everything but a photo | `MediaDeliveryTest.an_asset_on_a_published_article_is_returned_to_anyone_with_nosniff`, `a_document_is_returned_as_an_attachment_and_a_photo_is_not` |
| Video streamed with `Range` | `MediaDeliveryTest.a_range_request_for_a_video_answers_206_with_that_range` |
| Unpublished media only to a moderator session | `MediaDeliveryTest`: pending and rejected answer `404` without a session, pending is returned to the moderator, a removed article's asset answers `404` |

### Article content

| Rule | Held by |
|---|---|
| Raw HTML escaped, URLs sanitised | `ArticleControllerTest.raw_html_in_a_body_reaches_the_page_as_text` (row 3), `WikiLinkRendererTest.a_javascript_url_in_an_ordinary_link_is_not_rendered_live`, `EditorPreviewTest.the_preview_is_the_article_page_rendering_so_raw_html_is_escaped` |
| No HTML sanitizer | nothing to test; the review of 30 Sep found none |
| `th:utext` only for the converter's output, with a comment saying so | `TemplateUnescapedOutputTest` (new). `moderate/SubmissionReview.html` had no comment on 30 Sep, and it was added |
| External links get `rel="noopener noreferrer"` | `WikiLinkRendererTest.an_external_link_in_a_body_opens_without_handing_the_page_to_the_target` |
| No URL supplied by a visitor is fetched | nothing to test; the review found no outbound HTTP client in `app/src/main` |

### Forms and abuse

| Rule | Held by |
|---|---|
| CSRF on every state-changing form | `403` without the token: `SubmissionFlowTest`, `ModerationFlowTest`, `EditorPreviewTest`; every POST in the flow tests carries the token taken from its form |
| CAPTCHA on submission | not built: FR-013 (`planned`) |
| Rate limit on submission and login | not built: NFR-005, [DEBT-011](../tech-debt.md) (login), [DEBT-017](../tech-debt.md) (preview) |
| Request body limit at the container | `UploadTooLargeTest.a_request_larger_than_the_container_accepts_answers_413_with_the_form_and_the_error`; [DEBT-015](../tech-debt.md) |

### Capabilities held by contributors

| Rule | Held by |
|---|---|
| Submission number: 60 bits from a secure source, not a sequence | `SubmissionNumbersTest.all_sixty_bits_of_the_source_reach_the_number`, `numbers_sorted_by_issue_are_not_sorted_by_value` (NFR-006) |

### Reads

| Rule | Held by |
|---|---|
| Queries do not grow with rows; fetch strategy chosen | `PageQueryCountTest`: landing and article pages only. Search and tag pages: [DEBT-021](../tech-debt.md) |
| Every public list bounded | `LandingControllerTest.each_section_is_bounded_so_a_large_guide_cannot_make_an_unbounded_page`, `TagBrowseTest.the_first_fifty_are_listed_with_the_total`. Search: [DEBT-021](../tech-debt.md) |

### The admin area

| Rule | Held by |
|---|---|
| Password only as a BCrypt hash; no hash, no login | `ModeratorLoginWithoutHashTest.no_password_logs_in_when_no_hash_is_set` (new) |
| `/moderate/**` needs the moderator session | `ModeratorLoginTest.every_moderate_route_but_the_login_redirects_to_the_login_without_a_session` |
| Session id changes on login | `ModeratorLoginTest.the_right_password_reaches_the_queue_under_a_new_session_id`. CSRF token not replaced: [DEBT-013](../tech-debt.md) |
| Failed login logged at `WARN` without the password | `ModeratorLoginTest.a_wrong_password_answers_401_on_the_form_and_is_logged_as_a_warning`. No rate limit: [DEBT-011](../tech-debt.md) |
| Session cookie `HttpOnly`, `SameSite=Lax` | `SessionCookieTest.the_session_cookie_a_login_sets_is_http_only_and_same_site_lax` (new, on a real server). `Secure`: [DEBT-014](../tech-debt.md) |
| Destructive actions logged | `ModerationFlowTest.approving_and_rejecting_are_logged_with_the_submission_number`, `ArticleRemovalTest.a_removal_is_logged_with_the_articles_address` (new). Bulk import has no admin route; `SeedRunner` logs the count |

### Data and secrets

| Rule | Held by |
|---|---|
| `.env` ignored by git | `.gitignore`, lines `.env` and `.env.*`; a configuration fact, not a test |
| No secret in a log | `WebSecurityTest.no_user_account_exists_so_no_password_is_generated_and_logged`, and the failed-login test above |
| Export holds published content only | `ArticleArchiveTest.an_export_holds_one_file_per_published_article_named_by_its_address` (a removed article is left out); submissions are in another table, which the export does not read |

## Security review, 30 September

`/security-review` covered `app/src/main` (Java, templates, scripts, configuration and migrations),
the `Dockerfile`, `docker-compose.yml` and `.env.example`, not only a diff, because no review had
been run before. The known debt entries were excluded from its brief.

**Result: no vulnerability at confidence 8 of 10 or above.** The sinks it checked and found safe:

- rendering, and the three `th:utext`;
- `editor.js`'s `innerHTML`, fed by the CSRF-protected preview;
- links built from slugs and UUIDs;
- redirects, all to constant targets or to a server-made submission number;
- JPQL, only with bound parameters, and the analysed Lucene `match`;
- media paths;
- SnakeYAML with `SafeConstructor`;
- Tika on the bytes alone;
- the `/moderate/**` gate and the media role check;
- BCrypt, failing closed on a blank hash;
- `SecureRandom` submission numbers;
- error pages;
- the Content-Security-Policy.

Its three hardening notes are entered in the register:

| Note | Entry |
|---|---|
| The application container runs as root | [DEBT-018](../tech-debt.md) |
| The moderator cannot log out | [DEBT-019](../tech-debt.md) |
| Templates are not cached on the stand | [DEBT-020](../tech-debt.md) |

The walk above also found one missing comment on a `th:utext`, which was fixed. It found two gaps
in the tests: [DEBT-021](../tech-debt.md), and the lying files of row 8, which now have tests.
