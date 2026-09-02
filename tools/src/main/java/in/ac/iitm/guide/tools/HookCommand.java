package in.ac.iitm.guide.tools;

import java.util.ArrayList;
import java.util.List;

/** Implements the Claude Code hook subcommands. */
final class HookCommand {

    private HookCommand() {}

    static void run(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ai-tools hook <prompt|guard|stop|note>");
            System.exit(64);
        }
        switch (args[0]) {
            case "prompt" -> prompt();
            case "guard" -> guard();
            case "stop" -> stop();
            case "note" -> note(String.join(" ", List.of(args).subList(1, args.length)));
            case "english" -> english(java.util.Arrays.copyOfRange(args, 1, args.length));
            default -> System.err.println("ai-tools: unknown hook " + args[0]);
        }
    }

    /**
     * Stores an English rendering of the prompt and the outcome for the entry in progress.
     *
     * <p>The journal is evidence for an English-speaking evaluator, but the conversation is not
     * always in English and a hook cannot translate. The agent therefore supplies the rendering
     * before it ends the turn, and the {@code stop} hook writes it next to the original.
     */
    // TODO(DEBT-001): this binds the rendering to the latest session rather than to the entry that
    // is actually open, so a rendering supplied with no entry in flight attaches to the next one.
    private static void english(String[] args) throws Exception {
        String prompt = valueOf(args, "--prompt");
        String outcome = valueOf(args, "--outcome");
        if (prompt == null && outcome == null) {
            System.err.println("usage: ai-tools hook english [--prompt <text>] [--outcome <text>]");
            System.exit(64);
        }
        Repo repo = Repo.find(null);
        Journal journal = Journal.open(repo, latestSession(repo));
        journal.setEnglish(prompt, outcome);
        journal.save();
    }

    /** Reads {@code --name value} from the argument list; returns null when the flag is absent. */
    private static String valueOf(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * The rule that CLAUDE.md cannot enforce on its own.
     *
     * <p>Instructions are read once when a session starts and then compete with everything else in
     * the context window. That is fine for rules with a gate behind them, and not fine for this one,
     * which applies at exactly one moment: when a request arrives. So it is delivered with every
     * request instead of being trusted to survive.
     *
     * <p>It is emitted unconditionally rather than guessed at from the wording of the prompt.
     * A heuristic over imperative verbs in two languages would miss the cases that matter, and a
     * reminder that fires only sometimes teaches the reader to ignore it.
     */
    private static final String SHARPEN_REMINDER =
            """
            Before writing code, a document or a schema: if this request leaves open anything that \
            changes what you would build - the input contract, the boundary, the acceptance \
            condition - ask closed questions with a suggested answer for each, and WAIT.

            Choosing a sensible default and announcing it is not compliance: the target is still one \
            nobody picked. "The task is small" is not an exception; a five-line function has an input \
            contract whether or not anyone wrote it down.

            Do not ask when the request is a direct command with a checkable result, when it already \
            states its acceptance condition, or when the answer is already written down in a \
            requirement, an ADR or the roadmap - cite it instead. Details: docs/ai/prompting.md.\
            """;

    /**
     * Opens a journal entry, carries the sharpening rule into the turn, and reports the files the
     * human touched by hand.
     *
     * <p>The report of hand edits exists because the assistant does not observe the file system
     * between turns: without it such an edit is silently lost, and may be overwritten.
     */
    private static void prompt() throws Exception {
        HookEvent event = HookEvent.readFromStdin();
        Repo repo = Repo.find(event.cwd());
        Journal journal = Journal.open(repo, event.sessionId());

        List<String> humanEdits = journal.startEntry(event.prompt());

        StringBuilder message = new StringBuilder(SHARPEN_REMINDER);
        if (!humanEdits.isEmpty()) {
            message.append("\n\nFiles the human changed by hand since your last answer:\n");
            humanEdits.forEach(line -> message.append("- ").append(line).append('\n'));
            message.append(
                            "\nRead them before continuing: this is a correction of course, not something to argue with.\n")
                    .append("If an edit contradicts an instruction, propose changing the instruction ")
                    .append("(docs/ai/collaboration.md).");
        }
        HookEvent.emitContext("UserPromptSubmit", message.toString());
    }

    /** Asks the human before an edit lands in a file that belongs to their decision space. */
    private static void guard() throws Exception {
        HookEvent event = HookEvent.readFromStdin();
        Repo repo = Repo.find(event.cwd());
        String relative = repo.relativize(event.filePath());
        String reason = ProtectedPaths.reasonFor(relative);
        if (reason == null) {
            return;
        }
        HookEvent.emitDecision(
                "PreToolUse",
                "ask",
                relative + " belongs to the human's decision space: " + reason + ".\n\n"
                        + "Editing it is allowed only after explicit agreement "
                        + "(docs/ai/collaboration.md, section \"Who decides what\").\n"
                        + "If that agreement has not been given yet, describe the change you propose and wait.");
    }

    /** Closes the journal entry for the turn. */
    private static void stop() throws Exception {
        HookEvent event = HookEvent.readFromStdin();
        Repo repo = Repo.find(event.cwd());
        Journal journal = Journal.open(repo, event.sessionId());

        List<String> checks = new ArrayList<>();
        checks.add("traceability gate: not enabled yet (phase 2)");
        journal.finishEntry(event.lastAssistantMessage(), checks, List.of());
    }

    private static void note(String text) throws Exception {
        if (text.isBlank()) {
            System.err.println("usage: ai-tools hook note <text>");
            System.exit(64);
        }
        Repo repo = Repo.find(null);
        Journal.open(repo, latestSession(repo)).addNote(text);
    }

    /** Picks the most recently touched session so that a note lands in the session in progress. */
    private static String latestSession(Repo repo) throws Exception {
        java.nio.file.Path dir = repo.resolve(".claude/.journal-state");
        if (!java.nio.file.Files.isDirectory(dir)) {
            return "no-session";
        }
        try (var files = java.nio.file.Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .max(java.util.Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .orElse("no-session");
        }
    }
}
