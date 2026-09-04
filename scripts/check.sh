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

# The tooling is built before the application so that the documentation check below can run even
# when the application does not compile: a broken build should not also blind the other checks.
echo "==> process tooling"
./mvnw -q -B -pl tools package

# The audit on 4 September found eight divergences on a green build. Seven were a document
# describing something the repository did not contain, which no test could ever have failed on.
echo "==> documentation describes what exists"
java -jar tools/target/ai-tools.jar docs-check

echo "==> build, tests and formatting"
./mvnw -B verify

# The traceability gate is added here in phase 2, once `ai-tools trace` exists.
# See docs/roadmap/02-skeleton.md.

echo
echo "All checks passed."
