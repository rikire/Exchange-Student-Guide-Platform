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

        assertTrue(event.isTaskNotification());
    }

    @Test
    void leading_whitespace_does_not_hide_a_task_notification() throws IOException {
        assertTrue(HookEvent.parse("{\"prompt\":\"\\n  <task-notification>x\"}").isTaskNotification());
    }

    @Test
    void an_ordinary_prompt_is_not_a_task_notification() throws IOException {
        assertFalse(
                HookEvent.parse("{\"prompt\":\"Add search to the articles\"}").isTaskNotification());
    }

    @Test
    void a_prompt_that_only_mentions_the_tag_is_not_a_task_notification() throws IOException {
        // A person asking about the tag must still get a journal entry and the contract reminder.
        HookEvent event = HookEvent.parse("{\"prompt\":\"why does <task-notification> show up in my journal?\"}");

        assertFalse(event.isTaskNotification());
    }

    @Test
    void an_event_without_a_prompt_is_not_a_task_notification() throws IOException {
        assertFalse(HookEvent.parse("{}").isTaskNotification());
    }

    @Test
    void reads_the_subagent_identity_when_the_event_carries_one() throws IOException {
        HookEvent event = HookEvent.parse("{\"agent_id\":\"a1\",\"agent_type\":\"researcher\"}");

        assertEquals("a1", event.agentId());
        assertEquals("researcher", event.agentType());
    }
}
