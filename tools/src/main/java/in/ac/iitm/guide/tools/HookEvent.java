package in.ac.iitm.guide.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
        String content) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static HookEvent readFromStdin() throws IOException {
        byte[] raw = System.in.readAllBytes();
        if (raw.length == 0) {
            return new HookEvent(null, null, null, null, null, null, null, null, null);
        }
        JsonNode node = MAPPER.readTree(new String(raw, StandardCharsets.UTF_8));
        JsonNode input = node.path("tool_input");
        return new HookEvent(
                text(node, "session_id"),
                text(node, "cwd"),
                text(node, "hook_event_name"),
                text(node, "prompt"),
                text(node, "last_assistant_message"),
                text(node, "tool_name"),
                input.path("file_path").asText(null),
                input.path("command").asText(null),
                // Write carries `content`; Edit carries the replacement text. Either is the text
                // about to land, which is what makes a check possible before it does rather than
                // a complaint afterwards.
                input.path("content").asText(input.path("new_string").asText(null)));
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
