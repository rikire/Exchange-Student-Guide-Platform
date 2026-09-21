package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class HookCommandTest {

    @Test
    void the_reminder_asks_for_a_confirmed_contract_before_anything_changes() {
        assertTrue(HookCommand.CONTRACT_REMINDER.contains("WAIT for an explicit confirmation"));
    }

    @Test
    void the_reminder_no_longer_excuses_a_request_that_looks_complete() {
        // Two instructions that disagree are worse than either: the agent follows the shorter one,
        // and before this the reminder said not to ask what CLAUDE.md said to always confirm.
        assertFalse(HookCommand.CONTRACT_REMINDER.contains("Do not ask when"));
        assertFalse(HookCommand.CONTRACT_REMINDER.contains("direct command"));
    }

    @Test
    void the_reminder_leaves_questions_and_read_only_commands_without_a_contract() {
        assertTrue(HookCommand.CONTRACT_REMINDER.contains("read-only command"));
    }

    @Test
    void the_reminder_still_forbids_stating_a_duration() {
        assertTrue(HookCommand.CONTRACT_REMINDER.contains("Do not state how long anything will take"));
    }

    @Test
    void a_subagent_note_names_the_agent_and_its_id() throws IOException {
        HookEvent event = HookEvent.parse("{\"agent_id\":\"a1\",\"agent_type\":\"researcher\"}");

        assertEquals("Subagent finished: researcher (a1).", HookCommand.subagentNote(event));
    }

    @Test
    void a_subagent_note_says_so_when_the_event_names_nobody() throws IOException {
        assertEquals(
                "Subagent finished (the event did not say which).", HookCommand.subagentNote(HookEvent.parse("{}")));
    }
}
