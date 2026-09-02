package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MembersTest {

    @TempDir
    Path repoRoot;

    private Repo repo;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(repoRoot.resolve(".git"));
        repo = Repo.find(repoRoot.toString());
    }

    private void writeRegistry(String yaml) throws IOException {
        repo.write(repo.resolve("docs/team/members.yml"), yaml);
    }

    private static final String TWO_MEMBERS =
            """
            members:
              - id: mikhail
                name: Mikhail Novikov
                emails:
                  - rizirey@yandex.ru
                  - mikhail@example.com
              - id: abdirakhim
                name: Abdirakhim Ismailov
                emails: []
            """;

    @Test
    void resolves_a_member_from_a_git_email() throws IOException {
        writeRegistry(TWO_MEMBERS);

        assertEquals(
                "Mikhail Novikov",
                Members.load(repo).byEmail("rizirey@yandex.ru").orElseThrow().name());
    }

    @Test
    void matches_the_email_regardless_of_case() throws IOException {
        writeRegistry(TWO_MEMBERS);

        assertTrue(Members.load(repo).byEmail("RiZiReY@Yandex.RU").isPresent());
    }

    @Test
    void resolves_a_member_from_any_of_their_emails() throws IOException {
        writeRegistry(TWO_MEMBERS);

        assertEquals(
                "mikhail",
                Members.load(repo).byEmail("mikhail@example.com").orElseThrow().id());
    }

    @Test
    void does_not_resolve_an_email_belonging_to_nobody() throws IOException {
        writeRegistry(TWO_MEMBERS);

        assertTrue(Members.load(repo).byEmail("someone@else.org").isEmpty());
    }

    @Test
    void does_not_resolve_a_member_who_has_no_email_recorded_yet() throws IOException {
        writeRegistry(TWO_MEMBERS);

        // Abdirakhim's git email is still missing from the real registry, so this is the case that
        // sends the hook down the "ask the human" path rather than a hypothetical one.
        assertTrue(Members.load(repo).byEmail("").isEmpty());
        assertTrue(Members.load(repo).byId("abdirakhim").isPresent());
    }

    @Test
    void resolves_a_member_by_id_for_the_answer_the_human_gives() throws IOException {
        writeRegistry(TWO_MEMBERS);

        assertEquals(
                "Abdirakhim Ismailov",
                Members.load(repo).byId("ABDIRAKHIM").orElseThrow().name());
    }

    @Test
    void survives_a_registry_that_does_not_exist() throws IOException {
        assertTrue(Members.load(repo).all().isEmpty());
        assertTrue(Members.load(repo).byEmail("anyone@example.com").isEmpty());
    }
}
