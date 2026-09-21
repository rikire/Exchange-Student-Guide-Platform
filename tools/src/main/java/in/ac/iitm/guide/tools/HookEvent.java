package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One Claude Code hook event, read as JSON from standard input.
 *
 * <p>Only the fields this tooling actually uses are modelled; unknown fields are ignored so that a
 * change on the Claude Code side does not break the hook.
 */
public record HookEvent(
        String sessionId,
        String cwd,
        String eventName,
        String prompt,
        String lastAssistantMessage,
        String toolName,
        String filePath,
        String command,
        String content,
        String trigger,
        String agentId,
        String agentType) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static HookEvent readFromStdin() throws IOException {
        byte[] raw = System.in.readAllBytes();
        if (raw.length == 0) {
            return new HookEvent(null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return parse(new String(raw, StandardCharsets.UTF_8));
    }

    /**
     * The tags the harness puts first on a message it relays from a background task, a subagent or
     * another session. Observed in this repository's journal and in a live hand-back; a wrapper not
     * listed here is treated as a person's prompt until it is added.
     */
    private static final List<String> MACHINE_TAGS =
            List.of("<task-notification>", "<agent-message", "<cross-session-message");

    /**
     * True when the prompt is relayed by the harness rather than typed by a person.
     *
     * <p>Recognised by the tag it starts with. A person's own message that merely mentions a tag does
     * not start with it, so it still gets an entry and the reminder.
     */
    public boolean isMachineMessage() {
        if (prompt == null) {
            return false;
        }
        String start = prompt.stripLeading();
        return MACHINE_TAGS.stream().anyMatch(start::startsWith);
    }

    static HookEvent parse(String json) throws IOException {
        JsonNode node = MAPPER.readTree(json);
        JsonNode input = node.path("tool_input");
        return new HookEvent(
                text(node, "session_id"),
                text(node, "cwd"),
                text(node, "hook_event_name"),
                text(node, "prompt"),
                text(node, "last_assistant_message"),
                text(node, "tool_name"),
                // NotebookEdit has no file_path: its target is notebook_path. Reading only the one
                // name let a notebook under docs/ai/ through with no path to judge.
                input.path("file_path").asText(input.path("notebook_path").asText(null)),
                input.path("command").asText(null),
                // Write carries `content`; Edit carries the replacement text; NotebookEdit carries
                // new_source. Any of them is the text about to land, which is what makes a check
                // possible before it does rather than a complaint afterwards.
                input.path("content")
                        .asText(input.path("new_string")
                                .asText(input.path("new_source").asText(null))),
                // PreCompact says whether a person asked for the compaction or the context filled
                // up. Which one it was is the difference between a decision and an accident, and
                // the journal should not have to guess.
                text(node, "trigger"),
                text(node, "agent_id"),
                text(node, "agent_type"));
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText(null);
    }

    /**
     * Prints a permission decision.
     *
     * <p>The decision travels as JSON on stdout with exit code 0 rather than through exit code 2:
     * the launcher does not reliably propagate a non-zero code, so the code-based mechanism would
     * silently do nothing.
     */
    public static void emitDecision(String eventName, String decision, String reason) throws IOException {
        Map<String, Object> specific = new LinkedHashMap<>();
        specific.put("hookEventName", eventName);
        specific.put("permissionDecision", decision);
        specific.put("permissionDecisionReason", reason);
        emit(specific);
    }

    /** Prints extra context for the assistant to read before it continues. */
    public static void emitContext(String eventName, String text) throws IOException {
        Map<String, Object> specific = new LinkedHashMap<>();
        specific.put("hookEventName", eventName);
        specific.put("additionalContext", text);
        emit(specific);
    }

    private static void emit(Map<String, Object> specific) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hookSpecificOutput", specific);
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.println(MAPPER.writeValueAsString(payload));
    }
}
