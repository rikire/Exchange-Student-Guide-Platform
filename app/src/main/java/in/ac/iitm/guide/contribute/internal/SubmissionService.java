package in.ac.iitm.guide.contribute.internal;

import in.ac.iitm.guide.contribute.persistence.ContributeArticleRepository;
import in.ac.iitm.guide.contribute.persistence.SubmissionRepository;
import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaRejectedException;
import in.ac.iitm.guide.media.Upload;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.taxonomy.TagRejectedException;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts a new article or an edit into the moderation queue, pending, and never publishes anything:
 * this slice writes {@code submission} only (ADR-0003), and hands an attachment to {@code media}. The
 * rules about what may be submitted live here, not on the form (docs/ai/architecture-rules.md,
 * "Validation always lives in the domain").
 */
// trace:FR-010
// trace:FR-011
// trace:FR-012
// trace:FR-003
@Service
public class SubmissionService {

    /** {@code title} is {@code VARCHAR(255)}; measured as {@link String#length()}, as {@code Tags} does. */
    private static final int LONGEST_TITLE = 255;

    private final ContributeArticleRepository articles;
    private final SubmissionRepository submissions;
    private final SubmissionNumbers numbers;
    private final Tags tags;
    private final MediaAssets media;
    private final DisplayTime displayTime;

    SubmissionService(
            ContributeArticleRepository articles,
            SubmissionRepository submissions,
            SubmissionNumbers numbers,
            Tags tags,
            MediaAssets media,
            DisplayTime displayTime) {
        this.articles = articles;
        this.submissions = submissions;
        this.numbers = numbers;
        this.tags = tags;
        this.media = media;
        this.displayTime = displayTime;
    }

    /** What a contributor typed. The body is kept exactly as written (FR-003). */
    public record Draft(String title, String summary, String body, List<String> tags) {}

    /**
     * @param attachment the one file FR-010 allows, if the contributor chose one
     * @return the submission number
     * @throws SubmissionRejectedException if the draft cannot be published as it is, its title is
     *     taken, or the attachment is refused
     */
    @Transactional
    public String submitNewArticle(Draft draft, Optional<Upload> attachment) {
        var slug = check(draft);
        articles.findBySlug(slug).ifPresent(existing -> {
            throw new SubmissionRejectedException(
                    "An article with this title already exists. Propose an edit to it, or choose another title.",
                    existing.getRemovedAt() == null ? existing.getSlug() : null);
        });
        return save(draft, SubmissionType.NEW_ARTICLE, null, attachment);
    }

    /**
     * @param address the address of the article being edited, as it appears in the path
     * @param openedFor the article the form was opened for, when the form said
     * @param attachment the one file FR-011 allows, if the contributor chose one
     * @return the submission number
     * @throws ArticleNotPublishedException if no published article is at that address
     * @throws ArticleRemovedWhileEditingException if the article the form was opened for has been
     *     removed since
     * @throws SubmissionRejectedException if the draft cannot be published as it is, its new title
     *     belongs to a different article, or the attachment is refused
     */
    @Transactional
    public String submitEdit(String address, UUID openedFor, Draft draft, Optional<Upload> attachment) {
        var target = ArticleAddress.slugOf(address)
                .flatMap(articles::findWithTagsBySlugAndRemovedAtIsNull)
                .orElseThrow(() -> removedMeanwhile(openedFor)
                        ? new ArticleRemovedWhileEditingException()
                        : new ArticleNotPublishedException(address));
        var slug = check(draft);
        if (!slug.equals(target.getSlug())) {
            articles.findBySlug(slug).ifPresent(other -> {
                throw new SubmissionRejectedException(
                        "A different article already has this title. Choose another title.",
                        other.getRemovedAt() == null ? other.getSlug() : null);
            });
        }
        return save(draft, SubmissionType.EDIT, target.getId(), attachment);
    }

    /**
     * The edit form's content, and which article it was opened for, so a submission can tell an
     * article removed meanwhile from an address that never had one (DEBT-009, FR-026).
     */
    public record Editing(UUID article, Draft draft) {}

    /**
     * @return the published article at that address, as a draft to edit
     * @throws ArticleNotPublishedException if there is none
     */
    @Transactional(readOnly = true)
    public Editing editing(String address) {
        var article = published(address);
        var tagNames = article.getTags().stream().map(Tag::getName).sorted().toList();
        return new Editing(
                article.getId(), new Draft(article.getTitle(), article.getSummary(), article.getBody(), tagNames));
    }

    /**
     * What FR-012 shows to whoever holds the number: the status, the moderator's reason, and where an
     * approved submission can be read — never the submission's own text.
     *
     * @param articlePath the article's page while it is live at its address, else {@code null}
     */
    public record Status(
            String number,
            String title,
            OffsetDateTime submittedAt,
            String submitted,
            SubmissionStatus status,
            String rejectionReason,
            String articlePath) {}

    /** @return the status of the submission a typed number names, when it was ever issued */
    @Transactional(readOnly = true)
    public Optional<Status> statusOf(String typedNumber) {
        return SubmissionNumbers.canonical(typedNumber)
                .flatMap(submissions::findBySubmissionNumber)
                .map(submission -> new Status(
                        submission.getSubmissionNumber(),
                        submission.getTitle(),
                        submission.getSubmittedAt(),
                        displayTime.dateTime(submission.getSubmittedAt()),
                        submission.getStatus(),
                        submission.getRejectionReason(),
                        submission.getStatus() == SubmissionStatus.APPROVED
                                ? liveAddress(submission)
                                        .map(ArticleAddress::pathOf)
                                        .orElse(null)
                                : null));
    }

    /**
     * An edit names its article by id, so the link follows a later rename. A new article is found by
     * the address of its title, since {@code article} keeps no link back to the submission (ADR-0003):
     * once a later edit renames it, or it is removed, there is no link to show.
     */
    private Optional<String> liveAddress(Submission submission) {
        if (submission.getType() == SubmissionType.EDIT) {
            return articles.findLiveSlugById(submission.getTargetArticleId());
        }
        return ArticleAddress.slugOf(submission.getTitle())
                .filter(slug -> articles.findLiveSlugs(Set.of(slug)).contains(slug));
    }

    /** @return the stored form of a typed number, when it was ever issued */
    @Transactional(readOnly = true)
    public Optional<String> issued(String typedNumber) {
        return SubmissionNumbers.canonical(typedNumber).filter(submissions::existsBySubmissionNumber);
    }

    private boolean removedMeanwhile(UUID openedFor) {
        return openedFor != null
                && articles.findById(openedFor)
                        .map(article -> article.getRemovedAt() != null)
                        .orElse(false);
    }

    private Article published(String address) {
        return ArticleAddress.slugOf(address)
                .flatMap(articles::findWithTagsBySlugAndRemovedAtIsNull)
                .orElseThrow(() -> new ArticleNotPublishedException(address));
    }

    /** @return the address the title would be published at */
    private static String check(Draft draft) {
        var title = draft.title().strip();
        if (title.isEmpty()) {
            throw new SubmissionRejectedException("Give the article a title.");
        }
        if (title.length() > LONGEST_TITLE) {
            throw new SubmissionRejectedException("The title is longer than " + LONGEST_TITLE + " characters.");
        }
        if (draft.summary().isBlank()) {
            throw new SubmissionRejectedException("Give the article a summary.");
        }
        if (draft.summary().strip().length() > Article.LONGEST_SUMMARY) {
            throw new SubmissionRejectedException(
                    "The summary is longer than " + Article.LONGEST_SUMMARY + " characters.");
        }
        if (draft.body().isBlank()) {
            throw new SubmissionRejectedException("The article has no text.");
        }
        if (draft.body().length() > BodyPreview.LONGEST_BODY) {
            throw new SubmissionRejectedException(
                    "The text is longer than " + BodyPreview.LONGEST_BODY + " characters.");
        }
        return ArticleAddress.slugOf(title)
                .orElseThrow(() -> new SubmissionRejectedException("The title needs at least one letter or digit."));
    }

    private String save(Draft draft, SubmissionType type, UUID target, Optional<Upload> attachment) {
        var submission = new Submission();
        submission.setSubmissionNumber(numbers.next());
        submission.setType(type);
        submission.setTargetArticleId(target);
        submission.setTitle(draft.title().strip());
        submission.setSummary(draft.summary().strip());
        submission.setBody(draft.body());
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(OffsetDateTime.now());
        try {
            submission.setTags(tags.named(draft.tags()));
        } catch (TagRejectedException e) {
            throw new SubmissionRejectedException(e.getMessage() + ".");
        }
        var saved = submissions.save(submission);
        try {
            // The refusal rolls the submission back with it: a contributor fixes the file and sends
            // the whole form again.
            attachment.ifPresent(upload -> media.attach(saved.getId(), upload));
        } catch (MediaRejectedException e) {
            throw new SubmissionRejectedException(e.getMessage());
        }
        return saved.getSubmissionNumber();
    }
}
