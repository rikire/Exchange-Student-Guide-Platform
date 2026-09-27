package in.ac.iitm.guide.contribute.internal;

import in.ac.iitm.guide.contribute.persistence.ContributeArticleRepository;
import in.ac.iitm.guide.contribute.persistence.SubmissionRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.TagRejectedException;
import in.ac.iitm.guide.taxonomy.Tags;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts a new article or an edit into the moderation queue, pending, and never publishes anything:
 * this slice writes {@code submission} only (ADR-0003). The rules about what may be submitted live
 * here, not on the form (docs/ai/architecture-rules.md, "Validation always lives in the domain").
 */
// trace:FR-010
// trace:FR-011
// trace:FR-003
@Service
public class SubmissionService {

    /** {@code title} is {@code VARCHAR(255)}; measured as {@link String#length()}, as {@code Tags} does. */
    private static final int LONGEST_TITLE = 255;

    private final ContributeArticleRepository articles;
    private final SubmissionRepository submissions;
    private final SubmissionNumbers numbers;
    private final Tags tags;

    SubmissionService(
            ContributeArticleRepository articles,
            SubmissionRepository submissions,
            SubmissionNumbers numbers,
            Tags tags) {
        this.articles = articles;
        this.submissions = submissions;
        this.numbers = numbers;
        this.tags = tags;
    }

    /** What a contributor typed. The body is kept exactly as written (FR-003). */
    public record Draft(String title, String summary, String body, List<String> tags) {}

    /**
     * @return the submission number
     * @throws SubmissionRejectedException if the draft cannot be published as it is, or its title is
     *     taken
     */
    @Transactional
    public String submitNewArticle(Draft draft) {
        var slug = check(draft);
        articles.findBySlug(slug).ifPresent(existing -> {
            throw new SubmissionRejectedException(
                    "An article with this title already exists. Propose an edit to it, or choose another title.",
                    existing.getRemovedAt() == null ? existing.getSlug() : null);
        });
        return save(draft, SubmissionType.NEW_ARTICLE, null);
    }

    /**
     * @param address the address of the article being edited, as it appears in the path
     * @return the submission number
     * @throws ArticleNotPublishedException if no published article is at that address
     * @throws SubmissionRejectedException if the draft cannot be published as it is, or its new title
     *     belongs to a different article
     */
    @Transactional
    public String submitEdit(String address, Draft draft) {
        var target = published(address);
        var slug = check(draft);
        if (!slug.equals(target.getSlug())) {
            articles.findBySlug(slug).ifPresent(other -> {
                throw new SubmissionRejectedException(
                        "A different article already has this title. Choose another title.",
                        other.getRemovedAt() == null ? other.getSlug() : null);
            });
        }
        return save(draft, SubmissionType.EDIT, target.getId());
    }

    /**
     * @return the published article at that address as a draft to edit
     * @throws ArticleNotPublishedException if there is none
     */
    @Transactional(readOnly = true)
    public Draft draftOf(String address) {
        var article = published(address);
        var tagNames = article.getTags().stream().map(Tag::getName).sorted().toList();
        return new Draft(article.getTitle(), article.getSummary(), article.getBody(), tagNames);
    }

    /** @return the stored form of a typed number, when it was ever issued */
    @Transactional(readOnly = true)
    public Optional<String> issued(String typedNumber) {
        return SubmissionNumbers.canonical(typedNumber).filter(submissions::existsBySubmissionNumber);
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
        if (draft.body().isBlank()) {
            throw new SubmissionRejectedException("The article has no text.");
        }
        return ArticleAddress.slugOf(title)
                .orElseThrow(() -> new SubmissionRejectedException("The title needs at least one letter or digit."));
    }

    private String save(Draft draft, SubmissionType type, UUID target) {
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
        return submissions.save(submission).getSubmissionNumber();
    }
}
