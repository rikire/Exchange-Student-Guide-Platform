---
description: Bring the documentation back in step with the current changes
---

Bring the documentation in step with what has changed, following
[docs/ai/docs-sync.md](../../docs/ai/docs-sync.md).

1. Look at what actually changed: `git diff HEAD --name-only` and `git status`.
2. For each changed area, apply the mapping table in `docs-sync.md` and say which document is now
   obliged to change.
3. Update those documents **in substance**: describe the new behaviour, not the fact that an edit
   happened. "Added the media_assets table" is not an update; explaining what it holds, why the
   checksum sits next to the generated filename, and which queries the indexes serve, is.
4. Update the feature file's `code`, `tests` and `status` fields. Move a requirement to `done` only
   when both the code and the test exist.
5. If it is unclear how a change alters the described behaviour, do not rewrite at random — stop and
   ask.

Report which documents you changed and which ones you decided did not need changing, with the reason.
