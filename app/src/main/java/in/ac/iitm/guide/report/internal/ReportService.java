package in.ac.iitm.guide.report.internal;

import in.ac.iitm.guide.report.persistence.ReportRepository;
import in.ac.iitm.guide.report.persistence.ReportedArticleRepository;
import in.ac.iitm.guide.shared.persistence.Article;
import in.ac.iitm.guide.shared.persistence.Report;
import in.ac.iitm.guide.shared.web.AddressLimit;
import in.ac.iitm.guide.shared.web.DisplayTime;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-021: a reader flags a published article with a message; FR-022: the moderator closes the flag.
 * The message is required and bounded; one client address may send {@code guide.report.limit}.
 */
// trace:FR-021
// trace:FR-022
@Service
@EnableConfigurationProperties(ReportSettings.class)
public class ReportService {

    /** The longest message; a reader's note, not an article. */
    public static final int MESSAGE_LIMIT = 2000;

    /** Open reports the inbox shows at once (ADR-0010). */
    static final int INBOX_LIMIT = 100;

    private final ReportRepository reports;
    private final ReportedArticleRepository articles;
    private final DisplayTime displayTime;
    private final Clock clock;
    private final AddressLimit limit;

    ReportService(
            ReportRepository reports,
            ReportedArticleRepository articles,
            DisplayTime displayTime,
            ReportSettings settings,
            ObjectProvider<Clock> clock) {
        this.reports = reports;
        this.articles = articles;
        this.displayTime = displayTime;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
        this.limit =
                new AddressLimit(settings.limit().requests(), settings.limit().per(), this.clock);
    }

    /** The article at this address, if it is published: only those can be reported. */
    @Transactional(readOnly = true)
    public Optional<Article> reportable(String address) {
        return ArticleAddress.slugOf(address).flatMap(articles::findBySlugAndRemovedAtIsNull);
    }

    /**
     * Counts one report from {@code client}.
     *
     * @return empty when it may be stored, otherwise how long until it may
     */
    public Optional<Duration> take(String client) {
        var probe = limit.take(client);
        return probe.isConsumed() ? Optional.empty() : Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    /** @param message already checked: not blank and within {@link #MESSAGE_LIMIT} */
    @Transactional
    public void report(Article article, String message) {
        var report = new Report();
        report.setArticleId(article.getId());
        report.setMessage(message.strip());
        report.setReportedAt(OffsetDateTime.now(clock));
        reports.save(report);
    }

    /** One open report as the inbox shows it. */
    public record Entry(
            UUID id,
            String title,
            String path,
            String editPath,
            OffsetDateTime reportedAt,
            String reported,
            String message) {}

    @Transactional(readOnly = true)
    public List<Entry> inbox() {
        var open = reports.findOpenOnLiveArticles(PageRequest.of(0, INBOX_LIMIT));
        var named = articles.findByIdIn(open.stream().map(Report::getArticleId).toList()).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
        return open.stream()
                .map(report -> {
                    var article = named.get(report.getArticleId());
                    return new Entry(
                            report.getId(),
                            article.getTitle(),
                            ArticleAddress.pathOf(article.getSlug()),
                            "/moderate/articles/" + article.getSlug() + "/edit",
                            report.getReportedAt(),
                            displayTime.dateTime(report.getReportedAt()),
                            report.getMessage());
                })
                .toList();
    }

    /** @return false when there is no such report; closing a closed one changes nothing */
    @Transactional
    public boolean close(UUID id) {
        var report = reports.findById(id);
        report.filter(open -> open.getClosedAt() == null)
                .ifPresent(open -> open.setClosedAt(OffsetDateTime.now(clock)));
        return report.isPresent();
    }
}
