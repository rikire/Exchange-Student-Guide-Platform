package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The team registry, and the mapping from a git identity to a person.
 *
 * <p>Both the prompt journal and the weekly contribution log resolve authorship through this one
 * file. Two registries would disagree within a week, and the course asks who did what.
 */
public final class Members {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Path REGISTRY = Path.of("docs/team/members.yml");

    /** One member of the team. */
    public record Member(String id, String name, List<String> emails) {

        boolean owns(String email) {
            return emails.stream().anyMatch(known -> known.equalsIgnoreCase(email));
        }
    }

    private final List<Member> members;

    private Members(List<Member> members) {
        this.members = members;
    }

    public static Members load(Repo repo) throws IOException {
        Path file = repo.resolve(REGISTRY.toString());
        if (!Files.isRegularFile(file)) {
            return new Members(List.of());
        }
        JsonNode root = YAML.readTree(Files.readString(file));

        List<Member> parsed = new ArrayList<>();
        for (JsonNode node : root.path("members")) {
            List<String> emails = new ArrayList<>();
            node.path("emails").forEach(email -> emails.add(email.asText()));
            parsed.add(new Member(node.path("id").asText(""), node.path("name").asText(""), List.copyOf(emails)));
        }
        return new Members(List.copyOf(parsed));
    }

    public List<Member> all() {
        return members;
    }

    public Optional<Member> byEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return members.stream().filter(member -> member.owns(email.strip())).findFirst();
    }

    public Optional<Member> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String wanted = id.strip().toLowerCase(Locale.ROOT);
        return members.stream()
                .filter(member -> member.id().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    /**
     * Author addresses in the history that belong to nobody in the registry.
     *
     * <p>Registering an address is not the same as committing with it: one of us registered one he
     * does not use, and a person had to notice. Attribution silently resolves to nobody in that
     * case, which is the failure this makes loud.
     */
    public List<String> unregisteredAuthors(Repo repo) {
        List<String> unknown = new ArrayList<>();
        for (String email : Git.run(repo, "log", "--format=%ae").lines()) {
            if (!email.isBlank() && byEmail(email).isEmpty() && !unknown.contains(email)) {
                unknown.add(email);
            }
        }
        return unknown;
    }

    /**
     * The git identity configured where this repository is checked out, or empty if there is none.
     *
     * <p>A failed git call is deliberately quiet here. Authorship is worth reporting as unknown; it
     * is never worth breaking someone's session over a git call that did not work.
     */
    public static Optional<String> gitEmail(Repo repo) {
        Git.Result result = Git.run(repo, "config", "user.email");
        return !result.ok() || result.first().isBlank() ? Optional.empty() : Optional.of(result.first());
    }
}
