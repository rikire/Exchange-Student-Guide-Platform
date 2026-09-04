# Setting up to work in this repository

Written to be handed to an AI assistant. Open this repository with Claude Code (or any assistant
with a terminal) and say: **"work through docs/onboarding.md"**. It will check what is already
there, tell you what is missing, and set the rest up.

You can also follow it by hand; nothing here needs an assistant.

Takes about ten minutes, most of it downloading a JDK.

---

## For the assistant

Four rules for this file:

1. **Check before you install.** Every step below starts with a check. If it passes, say so and move
   on — do not reinstall something that is already working.
2. **Ask once before installing anything.** Collect everything that is missing, show the person the
   exact commands, and wait for a yes. Installing software on someone's laptop is their decision
   ([docs/ai/security.md](ai/security.md)), and a single question is cheaper for them than one per
   package.
3. **Do not invent a package name or a version.** If a command below fails because a name changed,
   search for the current one and say that you did. Guessing produces a plausible command that
   installs nothing.
4. **Finish on the criteria**, not on the steps. Section "Done when" is what actually settles it.

---

## 1. A JDK 21

**Check:** `java -version` prints 21, and `echo $JAVA_HOME` (Windows: `echo %JAVA_HOME%`) points at
it.

The application compiles with `release 21`, so 17 will not build it. Temurin specifically, because
that is what CI uses, and a different vendor locally is a class of "works on my machine".

| Platform | Install |
|---|---|
| macOS | `brew install --cask temurin@21` — if the cask name has changed, `brew search temurin` and use what is there |
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` (needs an elevation prompt). If that is refused, download the `.zip` from adoptium.net, unpack it, and set `JAVA_HOME` to it |
| Linux | SDKMAN (`sdk list java`, then install a `-tem` build of 21), or the tarball from adoptium.net |

**On macOS**, `JAVA_HOME` is normally set in your shell profile:

```sh
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
```

**Maven is not needed and should not be installed.** `./mvnw` fetches the exact version the project
pins, which is what makes the build identical on both our machines and in CI.

## 2. Your git identity

**Check:** `git config user.email` prints the address you actually commit with, and that same address
appears under your entry in [docs/team/members.yml](team/members.yml).

This is not bookkeeping. Authorship in the prompt journal and the contribution log both resolve
through that file, so an address that is missing — or one that differs from what you commit with —
means your work is attributed to nobody.

If the address is not in the registry, say so and propose adding it. That file is protected: the
assistant proposes, a person agrees.

## 3. The git hooks

**Check:** `git config core.hooksPath` prints `.githooks`.

```sh
scripts/hooks.sh
```

That builds the process tooling and points git at the tracked hooks. It has to be run once per
clone — hooks live in the repository but git does not enable them by itself.

**This is the step that gets skipped**, and skipping it is invisible: commits simply stop being
checked. It has already happened once here, which is why section "Done when" tests it rather than
trusting it.

## 4. That everything works

```sh
scripts/check.sh
```

Runs what CI runs: executable bits, the documentation check, the build, the tests and the
formatter. Takes under a minute after the first run.

If it fails, read the message before changing anything — each check prints the command that repairs
it.

## 5. Your editor

Nothing is required. If you use VS Code, the repository ships `.vscode/settings.json` with the
editor settings that match the formatter, so a file you save does not fight Spotless.

The Java extension needs to know about JDK 21; if it reports the wrong version, point
`java.configuration.runtimes` at your installation in your **user** settings, not in the workspace
file — that one is shared, and our JDKs live in different places.

Docker is not needed yet. It arrives in phase 4, for the PostgreSQL profile and the demo stand.

---

## Done when

All four are true. The last one is the point of the list.

1. `scripts/check.sh` finishes with "All checks passed."
2. `git config core.hooksPath` prints `.githooks`.
3. `git config user.email` matches an address in `docs/team/members.yml`.
4. **A deliberately wrong commit message is rejected.** Verify it, do not assume it:

   ```sh
   git commit --allow-empty -m "wip: testing the hook"
   ```

   This must **fail** with a message about the convention. If it succeeds, delete the commit
   (`git reset --hard HEAD~1`) and go back to step 3 — the hooks are not installed, and nothing is
   protecting the history.

   When it correctly fails, nothing was committed and there is nothing to clean up.

---

## Now what

Read these three, in this order. Together about fifteen minutes.

| Document | Why |
|---|---|
| [../CLAUDE.md](../CLAUDE.md) | The six rules the assistant works under. Read it because you will be correcting it when it drifts |
| [ai/collaboration.md](ai/collaboration.md) | Who decides what, and how to disagree with the assistant productively |
| [roadmap/README.md](roadmap/README.md) | Where the project is, what the current phase owes, and by when |

**How we work.** Either of us takes any task — a feature, a chore, a piece of debt — and finishes it
under our own authorship. There are no assigned areas. Ownership is measured from git history rather
than agreed in advance, because the course wants a named owner per module and we would rather that
number were true than tidy.

**What to expect from the assistant.** It will ask closed questions before writing code, refuse to
edit requirements or the schema without your agreement, and record every prompt in
[ai/journal/](ai/journal/). If it does something the instructions forbid, that is a defect in the
instructions and worth fixing there — not a reason to repeat yourself.

**Where the current work is.** The phase files under [roadmap/](roadmap/) list what is open. Pick
something, or ask what is not yet taken.
