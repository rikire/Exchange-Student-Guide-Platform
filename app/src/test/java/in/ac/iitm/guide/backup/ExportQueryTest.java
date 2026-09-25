package in.ac.iitm.guide.backup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * What the export asks the database for. A page of a {@code LIMIT/OFFSET} read is only stable if the
 * rows come in a defined order; H2 happens to return the same order twice, PostgreSQL is not obliged
 * to, so a missing sort cannot be seen from the files an export writes on H2 and is asserted on the
 * statement instead.
 */
@SpringBootTest
@Import(ExportQueryTest.LoggedStatements.class)
class ExportQueryTest {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    @TestConfiguration(proxyBeanMethods = false)
    static class LoggedStatements {
        @Bean
        static BeanPostProcessor logStatements() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (!(bean instanceof DataSource dataSource)) {
                        return bean;
                    }
                    return ProxyDataSourceBuilder.create(dataSource)
                            .listener(new QueryExecutionListener() {
                                @Override
                                public void beforeQuery(ExecutionInfo info, List<QueryInfo> queries) {}

                                @Override
                                public void afterQuery(ExecutionInfo info, List<QueryInfo> queries) {
                                    queries.forEach(query -> STATEMENTS.add(query.getQuery()));
                                }
                            })
                            .build();
                }
            };
        }
    }

    @Autowired
    private ArticleArchive archive;

    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    private Path directory;

    @AfterEach
    void clearTheDatabase() {
        jdbc.execute("DELETE FROM article_tag");
        jdbc.execute("DELETE FROM article");
        jdbc.execute("DELETE FROM tag");
    }

    @Test
    // trace:NFR-004
    void the_export_reads_the_articles_in_the_order_of_their_address() {
        archive.importFiles(
                Map.of(
                        "a.md",
                        """
                ---
                title: "Arrival"
                summary: "A summary."
                created: 2026-09-21
                updated: 2026-09-21
                ---

                Text.
                """));
        STATEMENTS.clear();

        archive.exportTo(directory);

        var articleRead = STATEMENTS.stream()
                .map(String::toLowerCase)
                .filter(sql -> sql.contains("removed_at is null"))
                .toList();
        assertThat(articleRead).hasSize(1);
        assertThat(articleRead.get(0)).containsPattern("order by \\S*slug");
    }
}
