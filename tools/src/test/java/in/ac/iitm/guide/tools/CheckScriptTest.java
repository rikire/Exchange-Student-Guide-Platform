package in.ac.iitm.guide.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercise orchestration without compiling the unrelated application. */
class CheckScriptTest {
    @TempDir
    Path root;

    private void setup() throws Exception {
        Path script = Repo.find(null).root().resolve("scripts/check.sh");
        Files.createDirectories(root.resolve("scripts"));
        Files.copy(script, root.resolve("scripts/check.sh"));
        for (String name : List.of(
                "mvnw",
                "scripts/hooks.sh",
                "scripts/session-start.sh",
                "scripts/contribution.sh",
                "scripts/diagrams.sh",
                ".githooks/commit-msg",
                ".githooks/pre-commit",
                ".githooks/pre-push")) {
            Path path = root.resolve(name);
            Files.createDirectories(path.getParent());
            Files.writeString(path, "#!/bin/sh\nexit 0\n");
            assertTrue(path.toFile().setExecutable(true));
        }
        Files.writeString(root.resolve("mvnw"), "#!/bin/sh\necho \"maven:$*\" >> calls\nexit ${TEST_MAVEN_EXIT:-0}\n");
        assertTrue(root.resolve("scripts/check.sh").toFile().setExecutable(true));
        Files.createDirectories(root.resolve("bin"));
        Files.writeString(root.resolve("bin/java"), "#!/bin/sh\necho \"java:$*\" >> calls\nexit 0\n");
        assertTrue(root.resolve("bin/java").toFile().setExecutable(true));
        command("git", "init", "-q");
        command("git", "add", ".");
        command("git", "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid", "commit", "-qm", "base");
    }

    private void command(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), out);
    }

    private int run(boolean base, boolean fail) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("sh", "scripts/check.sh").directory(root.toFile());
        builder.redirectErrorStream(true).redirectOutput(root.resolve("run.log").toFile());
        builder.environment().put("PATH", root.resolve("bin") + ":" + System.getenv("PATH"));
        builder.environment().remove("DOCS_SYNC_BASE");
        if (base) builder.environment().put("DOCS_SYNC_BASE", "HEAD");
        if (fail) builder.environment().put("TEST_MAVEN_EXIT", "7");
        return builder.start().waitFor();
    }

    @Test
    void verifies_each_module_once() throws Exception {
        setup();
        assertEquals(0, run(true, false));
        List<String> calls = Files.readAllLines(root.resolve("calls"));
        assertEquals(
                1,
                calls.stream()
                        .filter(s -> s.startsWith("maven:") && s.contains("-pl tools"))
                        .count());
        assertEquals(
                1,
                calls.stream()
                        .filter(s -> s.startsWith("maven:") && s.contains("-pl app"))
                        .count());
        assertTrue(calls.stream().anyMatch(s -> s.contains("trace --docs-sync HEAD")));
    }

    @Test
    void a_failed_tool_build_stops_before_application_and_success_message() throws Exception {
        setup();
        assertEquals(7, run(true, true));
        assertEquals(1, Files.readAllLines(root.resolve("calls")).size());
        assertFalse(Files.readString(root.resolve("run.log")).contains("All checks passed"));
    }

    @Test
    void a_missing_base_is_not_a_successful_empty_comparison() throws Exception {
        setup();
        assertNotEquals(0, run(false, false));
        assertFalse(Files.readString(root.resolve("run.log")).contains("All checks passed"));
    }

    @Test
    void a_new_branch_without_upstream_compares_with_the_remote_default_branch() throws Exception {
        setup();
        command("git", "update-ref", "refs/remotes/origin/main", "HEAD");
        assertEquals(0, run(false, false), Files.readString(root.resolve("run.log")));
        assertTrue(Files.readAllLines(root.resolve("calls")).stream()
                .anyMatch(s -> s.matches("java:.*trace --docs-sync [0-9a-f]{40}")));
    }
}
