package in.ac.iitm.guide.moderate.internal;

import in.ac.iitm.guide.media.MediaAssets;
import in.ac.iitm.guide.media.MediaItem;
import in.ac.iitm.guide.moderate.persistence.ModerateArticleRepository;
import in.ac.iitm.guide.moderate.persistence.ModerateSubmissionRepository;
import in.ac.iitm.guide.moderate.persistence.RevisionRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Revision;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.TagRejectedException;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only code that writes the published table from a submission (ADR-0003): approval copies a
 * pending submission into {@code article}, rejection only marks it. A decided submission is never
 * decided again.
 */
// trace:FR-014
// trace:FR-015
// trace:FR-017
// trace:FR-018
// trace:FR-019
// trace:FR-020
@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    /** The longest rejection reason stored; the column is unbounded, so the limit is ours (FR-019). */
    public static final int REASON_LIMIT = 2000;

    private final ModerateSubmissionRepository submissions;
    private final ModerateArticleRepository articles;
    private final RevisionRepository revisions;
    private final Tags tags;
    private final MediaAssets media;

    ModerationService(
            ModerateSubmissionRepository submissions,
            ModerateArticleRepository articles,
            RevisionRepository revisions,
            Tags tags,
            MediaAssets media) {
        this.submissions = submissions;
        this.articles = articles;
        this.revisions = revisions;
        this.tags = tags;
        this.media = media;
    }

    /** A line of the queue. */
    public record QueueEntry(String number, SubmissionType type, String title, OffsetDateTime submittedAt) {

        // English whatever the moderator's browser asks for (the human, 28 Sep).
        private static final DateTimeFormatter SUBMITTED =
                DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

        /** @return {@link #submittedAt} as the queue shows it */
        public String submitted() {
            return SUBMITTED.format(submittedAt);
        }
    }

    /** A submission as the moderator reads it; {@code status} says whether it can still be decided. */
    public record Review(
            String number,
            SubmissionType type,
            String title,
            String summary,
            String body,
            List<String> tags,
            List<MediaItem> media,
            SubmissionStatus status) {

        public boolean pending() {
            return status == SubmissionStatus.PENDING;
        }
    }

    /** @return every pending submission, oldest first (FR-014) */
    @Transactional(readOnly = true)
    public List<QueueEntry> queue() {
        return submissions.findByStatusOrderBySubmittedAtAsc(SubmissionStatus.PENDING).stream()
                .map(s -> new QueueEntry(s.getSubmissionNumber(), s.getType(), s.getTitle(), s.getSubmittedAt()))
                .toList();
    }

    /** @throws SubmissionNotFoundException if the number was never issued */
    @Transactional(readOnly = true)
    public Review review(String number) {
        var submission = submissions
                .findWithTagsBySubmissionNumber(number)
                .orElseThrow(() -> new SubmissionNotFoundException(number));
        var tagNames = submission.getTags().stream().map(Tag::getName).sorted().toList();
        return new Review(
                submission.getSubmissionNumber(),
                submission.getType(),
                submission.getTitle(),
                submission.getSummary(),
                submission.getBody(),
                tagNames,
                media.ofSubmission(submission.getId()),
                submission.getStatus());
    }

    /**
     * Publishes a new article, or applies an edit to its article, with the summary and tags the
     * moderator settled on (FR-017). An edit keeps the text it replaces as a revision (FR-020).
     *
     * @throws SubmissionNotFoundException if the number was never issued
     * @throws AlreadyDecidedException if it is no longer pending
     * @throws ApprovalRefusedException if the summary or a tag cannot be published
     * @throws ApprovalConflictException if the title's address was taken while the submission waited
     */
    @Transactional
    public void approve(String number, String summary, List<String> tagNames) {
        var submission = pendingForDecision(number);
        if (summary.isBlank()) {
            throw new ApprovalRefusedException("Give the article a summary.");
        }
        var chosenTags = tagsOf(tagNames);
        var now = OffsetDateTime.now();

        if (submission.getType() == SubmissionType.NEW_ARTICLE) {
            publish(submission, summary.strip(), chosenTags, now);
        } else {
            applyEdit(submission, summary.strip(), chosenTags, now);
        }
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setDecidedAt(now);
        log.info("Approved submission {}", number);
    }

    /**
     * @param reason what the moderator typed, or {@code null}; a blank one is stored as no reason
     * @throws SubmissionNotFoundException if the number was never issued
     * @throws AlreadyDecidedException if it is no longer pending
     * @throws RejectionRefusedException if the reason is longer than {@link #REASON_LIMIT}
     */
    @Transactional
    public void reject(String number, String reason) {
        var submission = pendingForDecision(number);
        var stored = reason == null || reason.isBlank() ? null : reason.strip();
        if (stored != null && stored.length() > REASON_LIMIT) {
            throw new RejectionRefusedException("Keep the reason to " + REASON_LIMIT + " characters or fewer.");
        }
        submission.setRejectionReason(stored);
        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setDecidedAt(OffsetDateTime.now());
        log.info("Rejected submission {}", number);
    }

    private Submission pendingForDecision(String number) {
        var submission = submissions
                .findForDecisionBySubmissionNumber(number)
                .orElseThrow(() -> new SubmissionNotFoundException(number));
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new AlreadyDecidedException(number);
        }
        return submission;
    }

    private Set<Tag> tagsOf(List<String> tagNames) {
        try {
            return tags.named(tagNames);
        } catch (TagRejectedException e) {
            throw new ApprovalRefusedException(e.getMessage() + ".");
        }
    }

    private void publish(Submission submission, String summary, Set<Tag> chosenTags, OffsetDateTime now) {
        var slug = freeSlug(submission.getTitle(), null);
        var article = new Article();
        article.setTitle(submission.getTitle());
        article.setSlug(slug);
        article.setSummary(summary);
        article.setBody(submission.getBody());
        article.setPublishedAt(now);
        article.setUpdatedAt(now);
        article.setTags(chosenTags);
        articles.save(article);
        media.moveToArticle(submission.getId(), article.getId());
    }

    private void applyEdit(Submission submission, String summary, Set<Tag> chosenTags, OffsetDateTime now) {
        var article = articles.findWithTagsByIdAndRemovedAtIsNull(submission.getTargetArticleId())
                .orElseThrow(
                        () -> new ApprovalConflictException("The article this edit is for is no longer published."));

        var revision = new Revision();
        revision.setArticleId(article.getId());
        revision.setTitle(article.getTitle());
        revision.setSummary(article.getSummary());
        revision.setBody(article.getBody());
        revision.setRetainedAt(now);
        revisions.save(revision);

        // TODO(DEBT-010): a changed title moves the article, and its old address answers 404.
        article.setSlug(freeSlug(submission.getTitle(), article.getId()));
        article.setTitle(submission.getTitle());
        article.setSummary(summary);
        article.setBody(submission.getBody());
        article.setUpdatedAt(now);
        article.setTags(chosenTags);
        media.moveToArticle(submission.getId(), article.getId());
    }

    /**
     * The address the title publishes at, refused when another article holds it — checked again here
     * because another submission may have been approved under it since this one was submitted.
     *
     * @param self the article being edited, which may keep its own address; {@code null} for a new one
     */
    // Two approvals under one address at once both pass this check, and the unique slug refuses the
    // second with a 500. Left so deliberately: CON-009.
    private String freeSlug(String title, UUID self) {
        // contribute refuses a title with no address, so a submission always has one.
        var slug = ArticleAddress.slugOf(title).orElseThrow();
        articles.findBySlug(slug).filter(other -> !other.getId().equals(self)).ifPresent(other -> {
            throw new ApprovalConflictException(
                    "An article with this title already exists. Reject this submission, or ask for another title.");
        });
        return slug;
    }
}
