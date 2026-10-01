package in.ac.iitm.guide.taxonomy;

import in.ac.iitm.guide.shared.persistence.Tag;
import in.ac.iitm.guide.taxonomy.persistence.TagRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one way a tag reaches the {@code tag} table (ADR-0005): every slice that writes tags asks here
 * rather than inserting, so {@code SIM Card} and {@code sim card} can never become two rows.
 */
// trace:FR-008
@Service
public class Tags {

    /**
     * {@code tag.name} is {@code VARCHAR(64)}: PostgreSQL counts characters there and H2 counts
     * UTF-16 units, so the limit is on {@link String#length()}, which fits both. Devanagari and Tamil
     * are one unit a character; only characters outside the Basic Multilingual Plane count twice.
     */
    private static final int LONGEST = 64;

    /**
     * Distinct tags on one article or submission, counted after normalising (the human, 1 Oct,
     * walkthrough F-33); every path that writes tags comes through here.
     */
    static final int MOST = 10;

    private final TagRepository tags;

    Tags(TagRepository tags) {
        this.tags = tags;
    }

    /**
     * @param names tags as someone typed them
     * @return the stored tag for each distinct name, created when missing; empty for no names
     * @throws TagRejectedException if a name is empty after trimming or longer than 64 characters, or
     *     there are more than {@value #MOST} distinct names
     */
    @Transactional
    public Set<Tag> named(Collection<String> names) {
        var normalised = new LinkedHashSet<String>();
        for (var name : names) {
            normalised.add(normalise(name));
        }
        if (normalised.size() > MOST) {
            throw new TagRejectedException("More than " + MOST + " tags");
        }
        if (normalised.isEmpty()) {
            return Set.of();
        }

        var existing =
                tags.findByNameIn(normalised).stream().collect(Collectors.toMap(Tag::getName, Function.identity()));
        var result = new LinkedHashSet<Tag>();
        for (var name : normalised) {
            result.add(existing.computeIfAbsent(name, this::create));
        }
        return result;
    }

    private static String normalise(String name) {
        var tag = name.strip().toLowerCase(Locale.ROOT);
        if (tag.isEmpty()) {
            throw new TagRejectedException("A tag is empty");
        }
        if (tag.length() > LONGEST) {
            throw new TagRejectedException("The tag \"" + tag + "\" is longer than " + LONGEST + " characters");
        }
        return tag;
    }

    private Tag create(String name) {
        var tag = new Tag();
        tag.setName(name);
        return tags.save(tag);
    }
}
