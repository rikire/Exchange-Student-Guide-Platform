# Setting up to work in this repository

Written to be handed to an AI assistant. Open this repository with Claude Code (or any assistant with
a terminal) and say: **"work through docs/onboarding.md"**. It will check what is already there, tell
you what is missing, and set the rest up. You can also follow it by hand; nothing here needs an
assistant.

If you are an assistant and the session report says `ENFORCEMENT IS OFF`, or `java` is not found, this
is the document to work through.

---

## For the assistant

1. **Check before you install.** Every step below starts with a check. If it passes, say so and move
   on; do not reinstall something that works.
2. **Ask once before installing anything.** Collect everything that is missing, show the person the
   exact commands, and wait for a yes. Installing software on someone's laptop is their decision
   ([docs/ai/security.md](ai/security.md)), and one question is cheaper than one per package. Prefer an
   install that needs no `sudo`: an assistant cannot answer a password prompt.
3. **Do not invent a package name, a URL or a version.** If a command below fails because a name
   changed, look up the current one and say that you did.
4. **Finish on the criteria**, not on the steps. "Done when" settles it.

---

## 1. A JDK 21

**Check:** `java -version` prints 21, and `echo $JAVA_HOME` (Windows: `echo %JAVA_HOME%`) points at it.

The application compiles with `release 21`, so 17 will not build it. Temurin specifically, because CI
uses it and a different vendor locally is a class of "works on my machine".

| Platform | Install |
|---|---|
| macOS | `brew install --cask temurin@21` — if the cask name has changed, `brew search temurin` and use what is there |
| Windows | `winget install EclipseAdoptium.Temurin.21.JDK` (needs an elevation prompt). If that is refused, download the `.zip` from adoptium.net, unpack it, and set `JAVA_HOME` to it |
| Linux | The tarball below, or SDKMAN (`sdk list java`, then a `-tem` build of 21). The distribution's `openjdk-21-jdk` works but is not Temurin and needs `sudo` |

**Linux without `sudo`** (tested on x64 Ubuntu; for another architecture change `architecture=` to a
value the API lists):

```sh
curl -fsS "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse" -o temurin21.json
# .[0].binary.package.link is the download and .checksum its SHA-256 (jq, or read the JSON)
curl -fsSL "<link>" -o jdk21.tar.gz && echo "<checksum>  jdk21.tar.gz" | sha256sum -c
mkdir -p ~/.local/opt ~/.local/bin && tar -xzf jdk21.tar.gz -C ~/.local/opt
ln -s ~/.local/opt/jdk-21*/bin/java ~/.local/bin/java     # ~/.local/bin must be on PATH
```

The checksum must match before you unpack. The download itself is on `github.com/adoptium`, which the
API links to.

**On macOS**, `JAVA_HOME` is normally set in your shell profile:

```sh
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
```

**Maven is not needed and should not be installed.** `./mvnw` fetches the version the project pins, so
the build is the same on both our machines and in CI.

## 2. Your git identity

**Check:** `git config user.email` prints the address you actually commit with, and that same address
appears under your entry in [docs/team/members.yml](team/members.yml).

Authorship in the prompt journal and the contribution log both resolve through that file; an address
that is missing, or differs from what you commit with, attributes your work to nobody.

If the address is not in the registry, say so and propose adding it. That file is protected: the
assistant proposes, a person agrees.

## 3. The git hooks

**Check:** `git config core.hooksPath` prints `.githooks`.

```sh
scripts/hooks.sh
```

It builds the process tooling and points git at the tracked hooks. Run it once per clone: hooks live
in the repository but git does not enable them by itself. **This is the step that gets skipped**, and
skipping it is invisible: commits simply stop being checked, which is why "Done when" tests it.

The build prints `Author identity unknown … fatal`. That looks like a failure and is not: it is the
output of a test that makes a commit fail on purpose.

## 4. That everything works

```sh
scripts/check.sh
```

It runs what CI runs: executable bits, the documentation check, the build, the tests and the
formatter, and takes under a minute once the dependencies are downloaded. If it fails, read the
message before changing anything; each check prints the command that repairs it.

## 5. Your editor

Nothing is required. VS Code picks up `.vscode/settings.json`, whose settings match the formatter, so a
saved file does not fight Spotless. If the Java extension reports the wrong JDK, point
`java.configuration.runtimes` at your installation in your **user** settings, not in the shared
workspace file: our JDKs live in different places.

Docker is not needed yet. It arrives in phase 4, for the PostgreSQL profile and the demo stand.

---

## Done when

All five are true. The last two are the point of the list, because they are the ones that fail
silently.

1. `scripts/check.sh` finishes with "All checks passed."
2. `git config core.hooksPath` prints `.githooks`.
3. `git config user.email` matches an address in `docs/team/members.yml`.
4. **A deliberately wrong commit message is rejected.** Verify it, do not assume it:

   ```sh
   printf 'wip: testing the hook\n' > /tmp/msg && .githooks/commit-msg /tmp/msg; echo "exit=$?"
   ```

   It must print a message about the convention and `exit=1`. Nothing is committed either way. If it
   exits 0, the hooks are not installed: go back to step 3.
5. **The Claude Code hooks fire.** In a Claude Code session opened in this repository, send any
   message. A file for today's session must appear in `docs/ai/journal/`. Hooks run the jar when the
   event happens, so a jar built after the session started is picked up without a restart. If no file
   appears, the jar is missing or `java` is not on the `PATH` of the process that started Claude Code.

---

## Now what

Read these three, in this order:

| Document | Why |
|---|---|
| [../CLAUDE.md](../CLAUDE.md) | The six rules the assistant works under. Read it because you will be correcting it when it drifts |
| [ai/collaboration.md](ai/collaboration.md) | Who decides what, and how to disagree with the assistant productively |
| [roadmap/README.md](roadmap/README.md) | Where the project is, what the current phase owes, and by when |

**How we work.** Either of us takes any task — a feature, a chore, a piece of debt — and finishes it
under our own authorship. There are no assigned areas. Ownership is measured from git history, because
the course wants a named owner per module and we would rather that number were true than tidy.

**What to expect from the assistant.** It reads the repository and states a contract before it writes
anything, refuses to edit requirements or the schema without your agreement, and records every prompt
in [ai/journal/](ai/journal/). If it does something the instructions forbid, that is a defect in the
instructions and worth fixing there, not a reason to repeat yourself.

**Where the current work is.** The phase files under [roadmap/](roadmap/) list what is open. Pick
something, or ask what is not yet taken.
