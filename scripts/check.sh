#!/bin/sh
# Everything CI runs. Slower than a commit hook, cheaper than a red pipeline.
#
# This script only claims to have run what it actually ran. A check that reports success after a
# step failed is worse than no check, because it is trusted.
set -e

cd "$(dirname "$0")/.."

# We develop on Windows, where git does not track the executable bit (core.filemode=false), so a
# chmod made locally never reaches the index. On Linux and macOS that is not merely inconvenient:
# a git hook without the bit does not fail, it is silently skipped, and the commit-message gate
# stops existing without anyone noticing. Fix with:
#   git update-index --chmod=+x <file>
echo "==> executable bits"
missing=""
for f in mvnw scripts/check.sh scripts/hooks.sh .githooks/commit-msg .githooks/pre-commit .githooks/pre-push; do
	mode=$(git ls-files -s "$f" 2>/dev/null | awk '{print $1}')
	[ "$mode" = "100755" ] || missing="$missing $f($mode)"
done
if [ -n "$missing" ]; then
	echo "These files must be executable in git but are not:$missing" >&2
	echo "Fix: git update-index --chmod=+x$(echo "$missing" | sed 's/([0-9]*)//g')" >&2
	exit 1
fi

# The audit on 4 September found eight divergences on a green build, and five of them were a
# document pointing at something that did not exist. Both checks below are cheap, and the
# traceability gate that would subsume them does not arrive until phase 2.
echo "==> documentation points at things that exist"
report=$(mktemp)

# Relative markdown links, resolved from the linking file's own directory.
find . -name "*.md" -not -path "./.git/*" -not -path "*/target/*" -print | while read -r file; do
	dir=$(dirname "$file")
	grep -oE '\]\(([^)#]+\.md)(#[^)]*)?\)' "$file" 2>/dev/null \
		| sed -E 's/^\]\(//; s/\)$//; s/#.*$//' \
		| while read -r link; do
			case "$link" in http*) continue ;; esac
			[ -e "$dir/$link" ] || echo "broken link: $file -> $link" >>"$report"
		done
done

# A slash command named in the instructions but absent from .claude/commands errors when tried,
# which is worse than one that was never advertised.
for name in $(grep -ohE '`/[a-z-]+`' CLAUDE.md docs/ai/README.md 2>/dev/null | tr -d '`/' | sort -u); do
	[ -f ".claude/commands/$name.md" ] || echo "advertised but missing: /$name" >>"$report"
done

if [ -s "$report" ]; then
	echo "Documentation refers to things that are not there:" >&2
	sed 's/^/  /' "$report" >&2
	rm -f "$report"
	exit 1
fi
rm -f "$report"

echo "==> build, tests and formatting"
./mvnw -B verify

# The traceability and documentation-sync gate is added here in phase 2, once `ai-tools trace`
# exists. See docs/roadmap/02-skeleton.md.

echo
echo "All checks passed."
