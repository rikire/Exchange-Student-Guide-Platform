package in.ac.iitm.guide.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import jakarta.persistence.EntityManager;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FEAT-009's storage rules through the published type, on H2 with the real migrations and a media
 * root of its own. The limits are NFR-001's settings made small, so a test file can cross them.
 */
@SpringBootTest
class MediaAssetsTest {

    private static final Path ROOT = MediaTestFiles.ROOT;

    @Autowired
    private MediaAssets media;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void clearTheDatabaseAndTheRoot() throws IOException {
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission");
        jdbc.execute("DELETE FROM article_link");
        jdbc.execute("DELETE FROM article");
        MediaTestFiles.empty(ROOT);
    }

    static Stream<Arguments> photos() {
        return Stream.of(
                Arguments.of("form.jpg", MediaTestFiles.jpeg(30, 20), "image/jpeg"),
                Arguments.of("form.png", MediaTestFiles.png(30, 20), "image/png"),
                Arguments.of("form.webp", MediaTestFiles.resource("media/photo.webp"), "image/png"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("photos")
    // trace:FR-010
    void an_accepted_photo_is_stored_as_a_photo_that_decodes(String name, byte[] bytes, String storedType)
            throws IOException {
        var submission = pending();

        var item = media.attach(submission, upload(name, bytes));

        assertThat(item.kind()).isEqualTo(MediaKind.PHOTO);
        assertThat(item.originalName()).isEqualTo(name);
        assertThat(row(item.id()).get("CONTENT_TYPE")).isEqualTo(storedType);
        assertThat(ImageIO.read(stored(item.id()).toFile()))
                .as("the stored file is a picture ImageIO can read")
                .isNotNull();
    }

    @Test
    // trace:FR-010
    void a_stored_photo_is_re_encoded_so_bytes_hidden_after_its_image_data_are_not_kept() throws IOException {
        // security.md: a valid header followed by a second file is a polyglot, and re-encoding is
        // what destroys the second file.
        var polyglot = concat(MediaTestFiles.jpeg(30, 20), "<html><script>alert(1)</script></html>".getBytes());

        var item = media.attach(pending(), upload("form.jpg", polyglot));

        var kept = new String(Files.readAllBytes(stored(item.id())), StandardCharsets.ISO_8859_1);
        assertThat(kept).doesNotContain("<script>");
    }

    @Test
    // trace:FR-010
    void a_photo_with_an_exif_orientation_is_stored_upright() throws IOException {
        // The fixture is 40 x 20 as stored, with a blue band down its left edge and EXIF orientation 6,
        // "turn 90 degrees clockwise to view": upright it is 20 x 40 with the band along the top.
        var item = media.attach(pending(), upload("sideways.jpg", MediaTestFiles.resource("media/orientation-6.jpg")));

        var image = ImageIO.read(stored(item.id()).toFile());
        assertThat(image.getWidth()).isEqualTo(20);
        assertThat(image.getHeight()).isEqualTo(40);
        assertThat(new Color(image.getRGB(10, 2)).getBlue())
                .as("the band that was on the left is now along the top")
                .isGreaterThan(150);
    }

    @Test
    // trace:FR-010
    void a_pdf_is_stored_byte_for_byte_as_a_document() throws IOException {
        var pdf = MediaTestFiles.pdf(2_000);

        var item = media.attach(pending(), upload("checklist.pdf", pdf));

        assertThat(item.kind()).isEqualTo(MediaKind.DOCUMENT);
        assertThat(Files.readAllBytes(stored(item.id()))).isEqualTo(pdf);
        assertThat(row(item.id()).get("CONTENT_TYPE")).isEqualTo("application/pdf");
    }

    @Test
    // trace:FR-010
    void an_mp4_is_stored_as_a_video() {
        var item = media.attach(pending(), upload("walk.mp4", MediaTestFiles.mp4(2_000)));

        assertThat(item.kind()).isEqualTo(MediaKind.VIDEO);
        assertThat(row(item.id()).get("CONTENT_TYPE")).isEqualTo("video/mp4");
    }

    static Stream<Arguments> refused() {
        return Stream.of(
                Arguments.of(
                        "html named as a photo", "form.jpg", "<!DOCTYPE html><html><body>hi</body></html>".getBytes()),
                // Found 30 Sep with Tika's detection switched to the name: the photo above is still
                // refused by re-encoding, while a document and a video are stored as they came.
                Arguments.of(
                        "html named as a document",
                        "form.pdf",
                        "<!DOCTYPE html><html><body>hi</body></html>".getBytes()),
                Arguments.of(
                        "html named as a video", "clip.mp4", "<!DOCTYPE html><html><body>hi</body></html>".getBytes()),
                Arguments.of(
                        "svg",
                        "logo.svg",
                        "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes()),
                Arguments.of("html", "page.html", "<!DOCTYPE html><html></html>".getBytes()),
                Arguments.of("executable", "setup.exe", concat("MZ".getBytes(), new byte[200])),
                Arguments.of("zip", "photos.zip", MediaTestFiles.zip("hello.txt")),
                Arguments.of(
                        "word document", "form.docx", MediaTestFiles.zip("[Content_Types].xml", "word/document.xml")),
                Arguments.of("gif", "anim.gif", concat("GIF89a".getBytes(), new byte[100])),
                Arguments.of("plain text", "notes.txt", "hello".getBytes()),
                Arguments.of("empty file", "empty.jpg", new byte[0]));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refused")
    // trace:FR-010
    void a_file_not_of_an_accepted_type_is_refused_and_nothing_is_stored(String what, String name, byte[] bytes)
            throws IOException {
        var submission = pending();

        assertThatThrownBy(() -> media.attach(submission, upload(name, bytes)))
                .isInstanceOf(MediaRejectedException.class)
                .hasMessageContaining("not an accepted type");

        assertNothingStored();
    }

    @Test
    // trace:FR-010
    void a_heic_photo_is_refused_with_the_hint_to_save_it_as_jpeg() {
        var heic =
                concat(ByteBuffer.allocate(4).putInt(24).array(), "ftypheic\0\0\0\0mif1heic".getBytes(), new byte[40]);

        assertThatThrownBy(() -> media.attach(pending(), upload("IMG_0001.HEIC", heic)))
                .isInstanceOf(MediaRejectedException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    // trace:FR-010
    void an_original_name_that_climbs_directories_does_not_reach_the_stored_path() throws IOException {
        var item = media.attach(pending(), upload("../../outside.jpg", MediaTestFiles.jpeg(10, 10)));

        var path = stored(item.id());
        assertThat(path.toAbsolutePath().normalize())
                .startsWith(ROOT.toAbsolutePath().normalize());
        assertThat(path.getFileName().toString()).doesNotContain("outside").doesNotContain("..");
        assertThat(item.originalName()).isEqualTo("../../outside.jpg");
    }

    @Test
    // trace:NFR-001
    void a_photo_over_the_photo_limit_is_refused() throws IOException {
        var large = MediaTestFiles.noisyPng(200, 200);
        assertThat(large.length).as("the fixture crosses the test's 64 KB").isGreaterThan(64 * 1024);

        assertThatThrownBy(() -> media.attach(pending(), upload("big.png", large)))
                .isInstanceOf(MediaRejectedException.class)
                .hasMessageContaining("64 KB");
        assertNothingStored();
    }

    @Test
    // trace:NFR-001
    void a_document_exactly_at_the_document_limit_is_stored_and_one_byte_over_is_refused() {
        media.attach(pending(), upload("at.pdf", MediaTestFiles.pdf(16 * 1024)));

        assertThatThrownBy(() -> media.attach(pending(), upload("over.pdf", MediaTestFiles.pdf(16 * 1024 + 1))))
                .isInstanceOf(MediaRejectedException.class)
                .hasMessageContaining("16 KB");
    }

    @Test
    // trace:NFR-001
    void a_video_over_the_video_limit_is_refused() {
        assertThatThrownBy(() -> media.attach(pending(), upload("long.mp4", MediaTestFiles.mp4(32 * 1024 + 1))))
                .isInstanceOf(MediaRejectedException.class)
                .hasMessageContaining("32 KB");
    }

    @Test
    // trace:NFR-001
    void a_file_that_would_take_the_volume_over_its_limit_is_refused() throws IOException {
        // The volume is counted as the sum of size_bytes (FEAT-009), so a row stands in for the files.
        var submission = pending();
        jdbc.update(
                "INSERT INTO media_asset (id, submission_id, stored_name, original_name, content_type, size_bytes,"
                        + " uploaded_at) VALUES (?, ?, 'x', 'x', 'application/pdf', ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(),
                submission,
                256 * 1024 - 1_000);

        assertThatThrownBy(() -> media.attach(submission, upload("more.pdf", MediaTestFiles.pdf(2_000))))
                .isInstanceOf(MediaRejectedException.class)
                .hasMessageContaining("full");
        assertThat(MediaTestFiles.files(ROOT)).isEmpty();
    }

    @Test
    // trace:FR-017
    void approval_moves_an_asset_from_its_submission_to_the_article() {
        var submission = pending();
        var item = media.attach(submission, upload("form.jpg", MediaTestFiles.jpeg(10, 10)));
        var article = article();

        media.moveToArticle(submission, article);

        assertThat(media.ofSubmission(submission)).isEmpty();
        assertThat(media.ofArticle(article)).extracting(MediaItem::id).containsExactly(item.id());
        assertThat(row(item.id())).containsEntry("SUBMISSION_ID", null).containsEntry("ARTICLE_ID", article);
    }

    private static Upload upload(String name, byte[] bytes) {
        return new Upload(name, bytes.length, new ByteArrayResource(bytes));
    }

    private static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (var part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private Map<String, Object> row(UUID id) {
        return jdbc.queryForMap("SELECT * FROM media_asset WHERE id = ?", id);
    }

    private Path stored(UUID id) {
        return ROOT.resolve((String) row(id).get("STORED_NAME"));
    }

    private void assertNothingStored() throws IOException {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class))
                .isZero();
        assertThat(MediaTestFiles.files(ROOT)).isEmpty();
    }

    private UUID pending() {
        var submission = new Submission();
        submission.setSubmissionNumber("SUB-TEST-0000-%04d".formatted(++numbers));
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Getting a SIM card " + numbers);
        submission.setSummary("Where to buy one.");
        submission.setBody("Take your passport.");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(status -> entityManager.persist(submission));
        return submission.getId();
    }

    private int numbers;

    private UUID article() {
        var now = OffsetDateTime.now();
        var article = new Article();
        article.setTitle("Registering with FRRO");
        article.setSlug("registering-with-frro");
        article.setSummary("A summary.");
        article.setBody("Text.");
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(new HashSet<>());
        transaction.executeWithoutResult(status -> entityManager.persist(article));
        return article.getId();
    }
}
