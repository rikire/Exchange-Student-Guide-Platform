package in.ac.iitm.guide.backup.internal;

import in.ac.iitm.guide.backup.ArchiveFormatException;
import in.ac.iitm.guide.wikilink.ArticleAddress;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Reads one archive file: a YAML block between two {@code ---} lines, then the Markdown body
 * (ADR-0007). The parser is SnakeYAML's; what is decided here is which keys a file may carry.
 */
// trace:NFR-004
public final class FrontMatter {

    /**
     * {@code author} is who wrote a seed draft. The database has nowhere to keep it, so it is read
     * and dropped rather than rejected — the seed files carry it and the archive does not.
     */
    private static final Set<String> KEYS =
            Set.of("title", "summary", "tags", "author", "created", "updated", "pinned");

    private static final Pattern FILE =
            Pattern.compile("\\A---\\R(?<yaml>.*?)\\R---(?:\\R|\\z)(?<rest>.*)\\z", Pattern.DOTALL);

    private FrontMatter() {}

    public static ArchivedArticle parse(String fileName, String text) {
        var file = FILE.matcher(text);
        if (!file.matches()) {
            throw new ArchiveFormatException(
                    fileName + ": no front matter — a file starts with a --- line, the fields, and a closing --- line");
        }
        var fields = fields(fileName, file.group("yaml"));
        for (var key : fields.keySet()) {
            if (!KEYS.contains(key)) {
                throw new ArchiveFormatException(fileName + ": unknown front matter key \"" + key + "\"");
            }
        }
        var title = text(fileName, fields, "title");
        if (ArticleAddress.slugOf(title).isEmpty()) {
            throw new ArchiveFormatException(
                    fileName + ": the title \"" + title + "\" has no letters or digits, so it has no address");
        }
        // One blank line separates the front matter from the body and is not part of the body.
        var body = file.group("rest").replaceFirst("\\A\\R", "");
        if (body.isBlank()) {
            throw new ArchiveFormatException(fileName + ": the body is empty");
        }
        return new ArchivedArticle(
                title,
                text(fileName, fields, "summary"),
                tags(fileName, fields.get("tags")),
                date(fileName, fields, "created"),
                date(fileName, fields, "updated"),
                fields.get("pinned") == null ? null : date(fileName, fields, "pinned"),
                body);
    }

    /**
     * The file text for one article: the fields SnakeYAML writes (so a title with a colon, a quote or
     * a leading {@code @} comes out quoted), a blank line, then the body exactly as it is stored.
     */
    public static String render(ArchivedArticle article) {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("title", article.title());
        fields.put("summary", article.summary());
        fields.put("tags", article.tags());
        fields.put("created", Date.from(article.created().toInstant()));
        fields.put("updated", Date.from(article.updated().toInstant()));
        if (article.pinned() != null) {
            fields.put("pinned", Date.from(article.pinned().toInstant()));
        }
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // Readable without the application (NFR-004): Devanagari as itself, and no folded lines.
        options.setAllowUnicode(true);
        options.setWidth(Integer.MAX_VALUE);
        return "---\n" + new Yaml(options).dump(fields) + "---\n\n" + article.body();
    }

    private static Map<String, Object> fields(String fileName, String yamlText) {
        Object loaded;
        try {
            // A key given twice would otherwise keep the last value and drop the first without a word.
            var options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            loaded = new Yaml(new SafeConstructor(options)).load(yamlText);
        } catch (YAMLException e) {
            throw new ArchiveFormatException(fileName + ": the front matter is not valid YAML — " + e.getMessage());
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new ArchiveFormatException(fileName + ": the front matter must be a list of key: value fields");
        }
        var fields = new LinkedHashMap<String, Object>();
        map.forEach((key, value) -> fields.put(String.valueOf(key), value));
        return fields;
    }

    private static String text(String fileName, Map<String, Object> fields, String key) {
        var value = fields.get(key);
        if (value == null) {
            throw new ArchiveFormatException(fileName + ": \"" + key + "\" is missing");
        }
        if (!(value instanceof String string) || string.isBlank()) {
            throw new ArchiveFormatException(
                    fileName + ": \"" + key + "\" must be text, and quoted if it could be read as another type");
        }
        return string;
    }

    private static List<String> tags(String fileName, Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ArchiveFormatException(fileName + ": \"tags\" must be a list such as [visa, admin]");
        }
        var tags = new ArrayList<String>();
        for (var element : list) {
            if (!(element instanceof String tag)) {
                throw new ArchiveFormatException(fileName + ": the tag " + element
                        + " must be text, and quoted if it could be read as another type");
            }
            tags.add(tag);
        }
        return tags;
    }

    private static OffsetDateTime date(String fileName, Map<String, Object> fields, String key) {
        var value = fields.get(key);
        if (value == null) {
            throw new ArchiveFormatException(fileName + ": \"" + key + "\" is missing");
        }
        if (!(value instanceof Date date)) {
            throw new ArchiveFormatException(
                    fileName + ": \"" + key + "\" must be a date such as 2026-09-21, not " + value);
        }
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }
}
