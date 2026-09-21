package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class HookEventTest {

    @Test
    void a_task_notification_is_not_something_a_person_typed() throws IOException {
        HookEvent event = HookEvent.parse("{\"prompt\":\"<task-notification>\\n<task-id>abc</task-id>\"}");

        assertTrue(event.isMachineMessage());
    }

    @Test
    void a_message_relayed_from_another_agent_is_not_something_a_person_typed() throws IOException {
        HookEvent event =
                HookEvent.parse("{\"prompt\":\"<agent-message from=\\\"a1\\\">\\n[Subagent hand-back] text\"}");

        assertTrue(event.isMachineMessage());
    }

    @Test
    void a_message_relayed_from_another_session_is_not_something_a_person_typed() throws IOException {
        assertTrue(HookEvent.parse("{\"prompt\":\"<cross-session-message from=\\\"w\\\">hi\"}")
                .isMachineMessage());
    }

    @Test
    void leading_whitespace_does_not_hide_a_task_notification() throws IOException {
        assertTrue(HookEvent.parse("{\"prompt\":\"\\n  <task-notification>x\"}").isMachineMessage());
    }

    @Test
    void an_ordinary_prompt_is_not_a_task_notification() throws IOException {
        assertFalse(
                HookEvent.parse("{\"prompt\":\"Add search to the articles\"}").isMachineMessage());
    }

    @Test
    void a_prompt_that_only_mentions_the_tag_is_not_a_task_notification() throws IOException {
        // A person asking about the tag must still get a journal entry and the contract reminder.
        HookEvent event = HookEvent.parse("{\"prompt\":\"why does <task-notification> show up in my journal?\"}");

        assertFalse(event.isMachineMessage());
    }

    @Test
    void an_event_without_a_prompt_is_not_a_task_notification() throws IOException {
        assertFalse(HookEvent.parse("{}").isMachineMessage());
    }

    @Test
    void reads_the_subagent_identity_when_the_event_carries_one() throws IOException {
        HookEvent event = HookEvent.parse("{\"agent_id\":\"a1\",\"agent_type\":\"researcher\"}");

        assertEquals("a1", event.agentId());
        assertEquals("researcher", event.agentType());
    }

    @Test
    void a_notebook_edit_names_its_target_in_notebook_path() throws IOException {
        // The field names are those of the NotebookEdit tool definition (notebook_path, new_source),
        // not a guess: the guard read file_path only, and this tool has no such field.
        HookEvent event = HookEvent.parse(
                "{\"tool_name\":\"NotebookEdit\",\"tool_input\":"
                        + "{\"notebook_path\":\"/repo/docs/ai/x.ipynb\",\"new_source\":\"print(1)\",\"edit_mode\":\"replace\"}}");

        assertEquals("/repo/docs/ai/x.ipynb", event.filePath());
    }

    @Test
    void a_notebook_edit_carries_the_text_about_to_land_in_new_source() throws IOException {
        HookEvent event = HookEvent.parse("{\"tool_name\":\"NotebookEdit\",\"tool_input\":"
                + "{\"notebook_path\":\"/repo/x.ipynb\",\"new_source\":\"print(1)\"}}");

        assertEquals("print(1)", event.content());
    }

    @Test
    void an_ordinary_edit_still_reads_file_path_and_new_string() throws IOException {
        HookEvent event = HookEvent.parse("{\"tool_name\":\"Edit\",\"tool_input\":"
                + "{\"file_path\":\"/repo/A.java\",\"old_string\":\"a\",\"new_string\":\"b\"}}");

        assertEquals("/repo/A.java", event.filePath());
        assertEquals("b", event.content());
    }
}
