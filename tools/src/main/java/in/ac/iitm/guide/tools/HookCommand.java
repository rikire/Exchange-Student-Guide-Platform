package in.ac.iitm.guide.tools;

import java.util.ArrayList;
import java.util.List;

/** Implements the Claude Code hook subcommands. */
final class HookCommand {

    private HookCommand() {}

    static void run(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ai-tools hook <" + Commands.describe(Commands.HOOK) + ">");
            System.exit(64);
        }
        if (!Commands.HOOK.contains(args[0])) {
            System.err.println(
                    "ai-tools: unknown hook " + args[0] + "; expected one of " + Commands.describe(Commands.HOOK));
            System.exit(64);
        }
        switch (args[0]) {
            case "prompt" -> prompt();
            case "guard" -> guard();
            case "stop" -> stop();
            case "note" -> note(String.join(" ", List.of(args).subList(1, args.length)));
            case "english" -> english(java.util.Arrays.copyOfRange(args, 1, args.length));
            case "author" -> author(java.util.Arrays.copyOfRange(args, 1, args.length));
            default -> throw new IllegalStateException("dispatch missing for " + args[0]);
        }
    }

    /**
     * Stores an English rendering of the prompt and the outcome for the entry in progress.
     *
     * <p>The journal is evidence for an English-speaking evaluator, but the conversation is not
     * always in English and a hook cannot translate. The agent therefore supplies the rendering
     * before it ends the turn, and the {@code stop} hook writes it next to the original.
     */
    private static void english(String[] args) throws Exception {
        String prompt = valueOf(args, "--prompt");
        String outcome = valueOf(args, "--outcome");
        if (prompt == null && outcome == null) {
            System.err.println("usage: ai-tools hook english [--prompt <text>] [--outcome <text>]");
            System.exit(64);
        }
        Repo repo = Repo.find(null);

        // Bound to an entry that is actually open, not to whichever session file was touched last:
        // modification time is updated for reasons unrelated to the turn, and with two of us working
        // at once it could put one person's rendering into the other's entry.
        //
        // Among open entries the newest prompt wins. A session that ended without its Stop hook
        // leaves an entry open for ever, so "exactly one is open" is a state that stops occurring
        // after a few days -- refusing on more than one would make the command permanently unusable,
        // and a command that always refuses gets worked around.
        String session = sessionWithNewestOpenEntry(repo);
        if (session == null) {
            System.err.println("ai-tools: no journal entry is open, so this rendering has nothing to belong to.");
            System.err.println("  Supply it during the turn, between the prompt and the end of your answer.");
            System.exit(1);
            return;
        }

        Journal journal = Journal.open(repo, session);
        journal.setEnglish(prompt, outcome);
        journal.save();
    }

    /**
     * The session whose open entry has the most recent prompt, or null when none is open.
     *
     * <p>Ordered by the recorded prompt time rather than by file modification time: the hook rewrites
     * a state file for reasons that have nothing to do with a turn beginning.
     */
    private static String sessionWithNewestOpenEntry(Repo repo) throws Exception {
        java.nio.file.Path dir = repo.resolve(".claude/.journal-state");
        if (!java.nio.file.Files.isDirectory(dir)) {
            return null;
        }
        String newest = null;
        String newestAt = "";
        try (var files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".json")) {
                    continue;
                }
                String session = name.substring(0, name.length() - ".json".length());
                Journal journal = Journal.open(repo, session);
                if (!journal.hasOpenEntry()) {
                    continue;
                }
                // ISO-8601 with a fixed offset sorts correctly as text.
                if (journal.promptedAt().compareTo(newestAt) > 0) {
                    newestAt = journal.promptedAt();
                    newest = session;
                }
            }
        }
        return newest;
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

    private static final java.util.regex.Pattern CYRILLIC = java.util.regex.Pattern.compile("\\p{IsCyrillic}");

    /**
     * Asked for only when it is actually needed.
     *
     * <p>Seven entries were written in one day with no rendering at all, because the instruction
     * to supply one lived in CLAUDE.md and lost to everything that arrived after it. This fires on
     * the prompts that need it and stays silent on the rest, so it does not become noise.
     */
    private static final String TRANSLATION_REMINDER =
            """
            This prompt is not in English, and the journal is evidence for an English-speaking \
            evaluator. Before you finish this turn, record the rendering:

              java -jar tools/target/ai-tools.jar hook english \
                --prompt "<this prompt in English>" --outcome "<what you did, in English>"

            The original is kept beside it. Skip --prompt only if the prompt was already English.\
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

        if (event.prompt() != null && CYRILLIC.matcher(event.prompt()).find()) {
            message.append("\n\n").append(TRANSLATION_REMINDER);
        }

        String unresolved = resolveAuthor(repo, journal);
        if (unresolved != null) {
            message.append("\n\n").append(unresolved);
        }
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

    /**
     * Works out who is sending the prompts, and records it on the session.
     *
     * <p>A hook cannot put a question to a person: it can only hand context to the agent. So when
     * the git identity matches nobody in the registry, this returns the text that tells the agent to
     * ask, and the answer comes back through {@code hook author}.
     *
     * @return null once the author is known, otherwise the instruction to ask the human
     */
    private static String resolveAuthor(Repo repo, Journal journal) throws Exception {
        if (!journal.author().isBlank()) {
            return null;
        }
        Members members = Members.load(repo);
        String email = Members.gitEmail(repo).orElse("");

        var matched = members.byEmail(email);
        if (matched.isPresent()) {
            journal.setAuthorIfUnknown(matched.get().id(), matched.get().name());
            journal.save();
            return null;
        }

        String known = members.all().isEmpty()
                ? "(the registry is empty)"
                : members.all().stream()
                        .map(m -> m.id() + " = " + m.name())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
        return "The author of this prompt could not be determined: the git identity "
                + (email.isBlank() ? "is not configured" : "is " + email)
                + ", which matches nobody in docs/team/members.yml. Known members: " + known + ".\n\n"
                + "Ask which of them is sending these prompts, before doing the work, and record the answer:\n"
                + "  java -jar tools/target/ai-tools.jar hook author <id>\n\n"
                + "It is asked once per session, not once per prompt. Then propose adding this email to "
                + "docs/team/members.yml so the question stops being necessary — that file is the human's "
                + "to change, so propose it and wait.";
    }

    /**
     * Records the author the human named when git could not identify them.
     *
     * <p>Fills a blank only. Overwriting a resolved author would turn this into a way to attribute
     * work to whoever is convenient, and the journal would stop being evidence of anything.
     */
    private static void author(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ai-tools hook author <member-id>");
            System.exit(64);
        }
        Repo repo = Repo.find(null);
        Members members = Members.load(repo);
        var member = members.byId(args[0]);
        if (member.isEmpty()) {
            System.err.println("ai-tools: no member with id " + args[0] + " in docs/team/members.yml");
            System.exit(64);
            return;
        }

        Journal journal = Journal.open(repo, latestSession(repo));
        if (!journal.setAuthorIfUnknown(member.get().id(), member.get().name())) {
            System.err.println("ai-tools: the author of this session is already " + journal.author()
                    + "; this command fills a blank, it does not reassign work.");
            System.exit(1);
            return;
        }
        journal.save();
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
        checks.add(journal.hasEnglishRendering() ? "English rendering: supplied" : "English rendering: NOT supplied");
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
