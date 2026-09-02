# Git hooks

Tracked in the repository rather than living in `.git/hooks`, so both of us get the same checks.

Install them once after cloning:

```sh
scripts/hooks.sh
```

That builds `ai-tools.jar` and runs `git config core.hooksPath .githooks`.

| Hook | What it checks |
|---|---|
| `commit-msg` | The message convention, so git history shows which commit delivered which requirement |
| `pre-commit` | AI instructions are not mixed with code in one commit; Java formatting |
| `pre-push` | The full `scripts/check.sh` |

Bypassing a hook with `--no-verify` is not allowed. A check that is bypassed once is bypassed always.
