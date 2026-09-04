package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The prompt journal: what was asked, what came out of it, and what the human changed afterwards.
 *
 * <p>The course asks students to be able to explain how the code was produced. Reconstructing that
 * at the end of the semester is not possible, so the record is written as the work happens, by the
 * hooks rather than by hand.
 */
public final class Journal {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final Repo repo;
    private final String sessionId;
    private final Path stateFile;
    private final ObjectNode state;

    private Journal(Repo repo, String sessionId, Path stateFile, ObjectNode state) {
        this.repo = repo;
        this.sessionId = sessionId;
        this.stateFile = stateFile;
        this.state = state;
    }

    public static Journal open(Repo repo, String sessionId) throws IOException {
        String id = (sessionId == null || sessionId.isBlank()) ? "no-session" : sessionId;
        Path file = repo.resolve(".claude/.journal-state/" + id + ".json");
        ObjectNode state = Files.isRegularFile(file)
                ? (ObjectNode) MAPPER.readTree(Files.readString(file))
                : MAPPER.createObjectNode();
        return new Journal(repo, id, file, state);
    }

    /** Opens an entry for a new prompt and returns the files the human edited since the last turn. */
    public List<String> startEntry(String prompt) throws IOException {
        Map<String, String> previous = readSnapshot();
        Map<String, String> current = Snapshot.take(repo);
        List<String> humanEdits = previous.isEmpty() ? List.of() : Snapshot.diff(previous, current);

        state.put("promptAt", ZonedDateTime.now(ZONE).toString());
        state.put("prompt", prompt == null ? "" : prompt);
        // Both belong to one turn. A new prompt is a new turn, and so a fresh chance for the gate to
        // refuse once - otherwise a single refusal would exempt every turn after it in the session.
        state.remove("needsEnglish");
        state.remove("renderingRefused");
        writeSnapshot(current);
        save();
        return humanEdits;
    }

    /**
     * Records that this turn owes the journal an English rendering.
     *
     * <p>Decided by the hook that reads the prompt, so that the condition lives in one place: the
     * gate at the end of the turn checks exactly what the reminder at the start of it asked for.
     */
    public void setNeedsEnglish() {
        state.put("needsEnglish", true);
    }

    /** True when the prompt was not in English, so a rendering is owed before the turn ends. */
    public boolean needsEnglish() {
        return state.path("needsEnglish").asBoolean(false);
    }

    /**
     * True once the gate has already refused this turn over a missing rendering.
     *
     * <p>It refuses once. A gate that can refuse for ever leaves no way to end a turn in which the
     * rendering genuinely cannot be produced, and a session nobody can end is worse than an entry
     * whose {@code Checks} line admits the omission.
     */
    public boolean renderingRefused() {
        return state.path("renderingRefused").asBoolean(false);
    }

    public void setRenderingRefused() {
        state.put("renderingRefused", true);
    }

    /**
     * Records who sent the prompts in this session.
     *
     * <p>Resolved from the git identity where possible. When it cannot be, the agent asks the human
     * and calls this — which is why it fills a blank but never overwrites: an answer typed into the
     * chat must not be able to contradict what git says, or the record stops being evidence.
     *
     * @return false when an author is already known and was therefore kept
     */
    public boolean setAuthorIfUnknown(String memberId, String displayName) {
        if (!state.path("authorName").asText("").isBlank()) {
            return false;
        }
        state.put("authorId", memberId);
        state.put("authorName", displayName);
        return true;
    }

    /**
     * A stable short fingerprint of a set of problems, used to tell "the same cause again" from a
     * new one.
     */
    public static String fingerprint(String text) {
        return Snapshot.sha256(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .substring(0, 16);
    }

    /** The cause the gate last refused on, so that it does not refuse on it twice. */
    public String lastGateCause() {
        return state.path("lastGateCause").asText("");
    }

    public void setLastGateCause(String cause) {
        if (cause == null || cause.isBlank()) {
            state.remove("lastGateCause");
        } else {
            state.put("lastGateCause", cause);
        }
    }

    /** When the open entry's prompt arrived, ISO-8601, or empty when no entry is open. */
    public String promptedAt() {
        return state.path("promptAt").asText("");
    }

    /** True while a prompt has been recorded and its outcome has not been written yet. */
    public boolean hasOpenEntry() {
        return !state.path("prompt").asText("").isBlank();
    }

    /** The author of this session's prompts, or empty when it is still unresolved. */
    public String author() {
        return state.path("authorName").asText("");
    }

    /**
     * Records an English rendering of the prompt and of the outcome for the current entry.
     *
     * <p>The journal is read by an English-speaking evaluator, but the conversation is not always in
     * English. The hooks cannot translate, so the agent supplies the rendering during the turn and
     * {@link #finishEntry} writes it alongside the original.
     */
    public void setEnglish(String prompt, String outcome) {
        if (prompt != null && !prompt.isBlank()) {
            state.put("promptEn", prompt);
        }
        if (outcome != null && !outcome.isBlank()) {
            state.put("outcomeEn", outcome);
        }
    }

    /** True when a rendering was supplied for the entry in progress. */
    public boolean hasEnglishRendering() {
        return !state.path("promptEn").asText("").isBlank()
                || !state.path("outcomeEn").asText("").isBlank();
    }

    /** Closes the entry: appends prompt, outcome and the human's edits to today's journal file. */
    public void finishEntry(String assistantMessage, List<String> checks, List<String> humanEdits) throws IOException {
        String prompt = state.path("prompt").asText("");
        String promptEn = state.path("promptEn").asText("");
        String outcomeEn = state.path("outcomeEn").asText("");
        if (prompt.isBlank() && (assistantMessage == null || assistantMessage.isBlank())) {
            return;
        }

        StringBuilder entry = new StringBuilder();
        entry.append("\n## ").append(ZonedDateTime.now(ZONE).format(TIME)).append("\n\n");

        // Per entry rather than once per file: a session is usually one person, but two people at
        // one machine would break a file-level header silently, and this costs one line.
        String author = author();
        entry.append("**Author:** ")
                .append(
                        author.isBlank()
                                ? "unresolved — the git identity matched nobody in docs/team/members.yml"
                                : author)
                .append("\n\n");

        // The original prompt is the artefact the course asks us to show; the translation is an
        // interpretation of it. Keeping both lets a reader of either language check the other.
        entry.append("**Prompt**\n\n").append(quote(prompt)).append("\n\n");
        if (!promptEn.isBlank() && !promptEn.strip().equals(prompt.strip())) {
            entry.append("**Prompt (English)**\n\n").append(quote(promptEn)).append("\n\n");
        }

        String outcome = outcomeEn.isBlank() ? assistantMessage : outcomeEn;
        entry.append("**Outcome**\n\n").append(quote(outcome)).append("\n\n");

        if (!humanEdits.isEmpty()) {
            entry.append("**Edited by hand afterwards**\n\n");
            humanEdits.forEach(line -> entry.append("- ").append(line).append('\n'));
            entry.append('\n');
        }
        if (!checks.isEmpty()) {
            entry.append("**Checks**\n\n");
            checks.forEach(line -> entry.append("- ").append(line).append('\n'));
            entry.append('\n');
        }

        appendToTodaysFile(entry.toString());
        state.remove("prompt");
        state.remove("promptEn");
        state.remove("outcomeEn");
        state.remove("needsEnglish");
        state.remove("renderingRefused");
        writeSnapshot(Snapshot.take(repo));
        save();
    }

    /** Adds a free-form note written by a person into today's journal file. */
    public void addNote(String note) throws IOException {
        appendToTodaysFile("\n## " + ZonedDateTime.now(ZONE).format(TIME) + " — note\n\n" + note + "\n");
    }

    public void save() throws IOException {
        repo.write(stateFile, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(state));
    }

    private void appendToTodaysFile(String text) throws IOException {
        String shortId = sessionId.length() > 8 ? sessionId.substring(0, 8) : sessionId;
        Path file = repo.resolve("docs/ai/journal/" + LocalDate.now(ZONE) + "-" + shortId + ".md");
        if (!Files.isRegularFile(file)) {
            String header = "# Session " + shortId + " — " + LocalDate.now(ZONE) + "\n\n"
                    + "Written by the `hook` commands of `ai-tools`, not by hand.\n";
            repo.write(file, header);
        }
        Files.writeString(file, text, java.nio.file.StandardOpenOption.APPEND);
    }

    private Map<String, String> readSnapshot() {
        Map<String, String> result = new TreeMap<>();
        state.path("snapshot")
                .fields()
                .forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
        return result;
    }

    private void writeSnapshot(Map<String, String> snapshot) {
        ObjectNode node = MAPPER.createObjectNode();
        snapshot.forEach(node::put);
        state.set("snapshot", node);
    }

    private static String quote(String text) {
        if (text == null || text.isBlank()) {
            return "> _(empty)_";
        }
        List<String> lines = new ArrayList<>();
        for (String line : text.strip().split("\r?\n")) {
            lines.add("> " + line);
        }
        return String.join("\n", lines);
    }
}
