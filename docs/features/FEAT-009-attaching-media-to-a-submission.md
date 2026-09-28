---
id: FEAT-009
title: Attaching media to a submission
status: done
covers: [FR-010, FR-011, FR-015, FR-001, FR-016, NFR-001]
slice: media
routes: ["POST /submissions", "POST /articles/{title}/edits", "GET /media/{id}"]
tables: [media_asset]
code:
  - app/src/main/java/in/ac/iitm/guide/media/package-info.java
  - app/src/main/java/in/ac/iitm/guide/media/MediaAssets.java
  - app/src/main/java/in/ac/iitm/guide/media/MediaItem.java
  - app/src/main/java/in/ac/iitm/guide/media/MediaKind.java
  - app/src/main/java/in/ac/iitm/guide/media/MediaRejectedException.java
  - app/src/main/java/in/ac/iitm/guide/media/Upload.java
  - app/src/main/java/in/ac/iitm/guide/media/internal/AcceptedType.java
  - app/src/main/java/in/ac/iitm/guide/media/internal/MediaConfiguration.java
  - app/src/main/java/in/ac/iitm/guide/media/internal/MediaFiles.java
  - app/src/main/java/in/ac/iitm/guide/media/internal/MediaSettings.java
  - app/src/main/java/in/ac/iitm/guide/media/internal/PhotoEncoder.java
  - app/src/main/java/in/ac/iitm/guide/media/persistence/MediaAssetRepository.java
  - app/src/main/java/in/ac/iitm/guide/media/web/MediaController.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/SubmissionController.java
  - app/src/main/java/in/ac/iitm/guide/contribute/web/UploadTooLargeAdvice.java
  - app/src/main/java/in/ac/iitm/guide/contribute/internal/SubmissionService.java
  - app/src/main/java/in/ac/iitm/guide/moderate/internal/ModerationService.java
  - app/src/main/java/in/ac/iitm/guide/articleview/web/ArticleController.java
  - app/src/main/resources/templates/media/Attachments.html
  - app/src/main/resources/templates/contribute/SubmissionForm.html
  - app/src/main/resources/templates/moderate/SubmissionReview.html
  - app/src/main/resources/templates/articleview/Article.html
  - app/src/main/resources/application.yml
tests:
  - app/src/test/java/in/ac/iitm/guide/media/MediaAssetsTest.java
  - app/src/test/java/in/ac/iitm/guide/media/MediaDeliveryTest.java
  - app/src/test/java/in/ac/iitm/guide/contribute/SubmissionFlowTest.java
  - app/src/test/java/in/ac/iitm/guide/contribute/UploadTooLargeTest.java
  - app/src/test/java/in/ac/iitm/guide/moderate/ModerationFlowTest.java
  - app/src/test/java/in/ac/iitm/guide/BrowserLayoutTest.java
---

# FEAT-009 — Attaching media to a submission

## Why

A contributor explains the FRRO form best with a photo of it, and the demo scenario's edit carries
exactly that photo ([scenario-trace.md](../cjm/scenario-trace.md), steps 5 and 7). Until now the
submission form took text only: the attachment was cut from FEAT-005 on 27 Sep and recorded as
[DEBT-008](../tech-debt.md). This feature builds the `media` slice that ADR-0006 and
[security.md](../architecture/security.md) designed, and closes that debt.

## Scenario

1. A contributor proposing an edit (UC-011) or a new article (UC-010) chooses one file on the form.
2. A file that is too large, or not of an accepted type, returns the form with what was typed and
   an error naming the problem; nothing is stored.
3. An accepted file is stored under a name the system chooses, and the submission enters the queue.
4. The moderator opens the submission (UC-015) and sees the photo, the video or the document's name.
5. On approval the file moves to the article, and every reader sees it there (UC-003).
6. Until then, nobody without a moderator session gets the file, whatever address they type.

## Routes

| Method and path | Purpose | Template |
|---|---|---|
| `POST /submissions` | The new-article form, now `multipart/form-data` with `attachment` | `contribute/SubmissionForm.html` |
| `POST /articles/{title}/edits` | The edit form, the same way | `contribute/SubmissionForm.html` |
| `GET /media/{id}` | The bytes of one asset | — |

Codes as in [ui-routes.md](../architecture/ui-routes.md).

## Schema impact

None. `media_asset` exists since V1, with the owner check of V3 and the two indexes of V4. The bytes
live under a media root on the filesystem (ADR-0006), `guide.media.root`.

## Decisions this feature fixed

Contract confirmed by the human on 28 Sep (prompt journal, 28 Sep), after the formats were looked up
on Wikipedia and the libraries on Maven Central:

- **Accepted formats**, from content, never from the name or the declared type:
  - photo: JPEG, PNG, WebP — each re-encoded, and turned upright by its EXIF orientation first;
  - document: PDF;
  - video: MP4.

  **DOCX dropped on 28 Sep by the human**, after the check it needed failed: `tika-core` reports a
  Word document, a macro-enabled one (DOCM) and a spreadsheet (XLSX) alike as
  `application/x-tika-ooxml`, and Tika's module that separates them brings Apache POI, BouncyCastle
  and log4j-core with it. A Word file saved as PDF serves the same reader; DOCX returns with a
  check of its `[Content_Types].xml` when OGE asks for it.

  HEIC, the iPhone's default since iOS 11, is refused with a message to save the photo as JPEG: no
  Java library found reads it. MP4 is the one video container every major browser plays. Audio was
  removed from CON-006 the same day.
- **Libraries**, each checked on Maven Central on 28 Sep: `org.apache.tika:tika-core` 3.3.2
  (Apache-2.0) reads the type from the magic bytes; `com.drewnoakes:metadata-extractor` 2.21.0
  (Apache-2.0) reads the EXIF orientation; `com.twelvemonkeys.imageio:imageio-webp` 3.15.2 (BSD)
  lets the JDK's ImageIO read WebP. JPEG and PNG are read and written by the JDK itself. Tika 3.3.2
  was chosen over 4.0.0 (18 Aug 2026) as the mature line with the same detection API.
- **Limits of NFR-001 are settings** (`guide.media.*`): 10 MB a photo, 20 MB a document, 200 MB a
  video, and 20 GB for the volume, counted as the sum of `size_bytes`.
- **A refused attachment answers `422`** with the form and what was typed. A request larger than the
  largest limit is refused by the container before the form is read, and answers `413` with the empty
  form and the error.
- **`GET /media/{id}`**: `200` for an asset on a published article; `200` for one on a pending or
  rejected submission only with a moderator session, `404` without; `404` for an unknown id.
  `X-Content-Type-Options: nosniff` always; `Range` is answered by Spring MVC's `Resource` handling;
  everything that is not a photo is sent as `Content-Disposition: attachment`.
- **Approval moves the asset** from the submission to the article, through `media`'s published type.
- **Shown** as a picture, a video player, or the document's name and size linking to its route, on
  the review screen and on the article.

## Acceptance criteria

- [x] A new article submitted with a photo is pending in the queue and the photo is stored with it.
- [x] An edit submitted with a photo is pending in the queue and the photo is stored with it.
- [x] A new article with an attachment over its limit is refused with a message, nothing stored.
- [x] An edit with an attachment over its limit is refused with a message, nothing stored.
- [x] A new article with an attachment not of an accepted type is refused with a message.
- [x] An edit with an attachment not of an accepted type is refused with a message.
- [x] A file whose name says `.jpg` and whose content is HTML is refused.
- [x] SVG, HTML, an executable, a ZIP and a Word document are refused; a HEIC photo is refused
      with the hint.
- [x] A stored photo is re-encoded: bytes hidden after its image data are not kept.
- [x] A photo with an EXIF orientation is stored upright.
- [x] An original name such as `../../x.jpg` does not reach the stored path.
- [x] Each of NFR-001's four limits is enforced at its configured value.
- [x] A request over the container's limit answers `413` with the form and an error.
- [x] An asset on a pending submission answers `404` without a moderator session.
- [x] An asset on a rejected submission answers `404` without a moderator session.
- [x] An asset on a pending submission is returned to a moderator.
- [x] An asset on a published article is returned to anyone, with `nosniff`.
- [x] A document is returned as an attachment; a photo is not.
- [x] A `Range` request for a video answers `206` with that range.
- [x] An unknown id answers `404`.
- [x] Approving a submission moves its asset to the article, and the article shows it.
- [x] The review screen shows the submission's asset.

Evidence, 28 Sep: each criterion is a test in the files above, red first. Tests that passed before
their code existed were shown to catch their fault instead: with the EXIF step removed the
orientation test went red, with the photo stored as uploaded the polyglot test did, with every asset
served to everyone the pending, rejected and removed-article delivery tests did, and without the
image width rule `BrowserLayoutTest` measured the article at 1633 px wide at 320. Run: `./mvnw verify`,
`-P postgres` and `-P browser` (the article measured with a 1600 px photo and a long-named PDF).

Found while building, each settled rather than guessed:

- **The container's multipart limits** are 1 MB a file by default, which would refuse a 10 MB photo;
  they are now 200 MB a file and 210 MB a request (`spring.servlet.multipart`).
- **The CSRF token is read from a multipart body** by Spring Security through a real Tomcat, shown by
  `UploadTooLargeTest` rather than assumed.
- **A file more than 2 MB over the container's limit** ends in a closed connection, not the `413`
  page: Tomcat's `max-swallow-size`. Checked with a 5 MB file, recorded as DEBT-015; left as it
  is for now by the human on 28 Sep.
- **A decoded photo is at most 50 megapixels** (`PhotoEncoder.LARGEST_PIXELS`), since decoding costs
  four bytes a pixel whatever the file's size; a 48-megapixel phone photo fits. Over it, the photo is
  refused as not an accepted type. Confirmed by the human on 28 Sep.
- **An approved edit adds its asset to the article's**, beside those already there.
- **Checked by the human from a phone, 28 Sep** (seed profile, H2): an edit proposed from the
  phone's browser with a picture, the queue, the review screen showing it. The stored file was a
  1056 × 1158 PNG with no EXIF block left, so the phone had sent a PNG, not a camera photo; the
  EXIF orientation still waits for a portrait photo from the camera. The submission was not
  approved, by the human's choice; approval and the article are covered by `ModerationFlowTest`.
- **Checked again with the camera, 28 Sep**: three 12-megapixel photos taken on the phone and
  uploaded from its browser. The two taken upright were stored as 3024 × 4032, standing, and the
  one taken sideways as 4032 × 3024; none kept an EXIF block or a GPS position. The human saw each
  the right way up on the review screen.

**Accepted by the human on 28 Sep** after those two checks from a phone ("everything works"). The
requirements' statuses are left for the human to change.

**29 Sep, FR-016 (phase 4, taken early at the human's choice):** every asset on an article page, and
on the moderator's review, has a Download link to `GET /media/{id}` with the `download` attribute and
the original name, so a photo or a video, which the server sends inline, is saved rather than opened.
The server is unchanged: a document already came as an attachment, and the access rules that FR-016's
second and third criteria ask for were built and tested with this feature on 28 Sep, now anchored to
FR-016. The link test was red before the template changed; `BrowserLayoutTest` measures the link at
four widths, 44 px on a phone. Accepted by the human on 29 Sep after downloading an attached file from
an approved edit in a browser; on that acceptance FR-016 is `done`.

## Deliberately out of scope

- Assets in the export archive (ADR-0007) — the backup slice's work.
- Removing the files of rejected submissions: they are unreachable and only take space; recorded as
  debt, by the human's decision on 28 Sep.
- More than one attachment per submission: FR-010 and FR-011 say one.
- Audio: removed from CON-006 on 28 Sep.
- DOCX: dropped on 28 Sep, see the decisions above.

## Open questions

None.
