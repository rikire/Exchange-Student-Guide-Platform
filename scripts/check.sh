#!/bin/sh
# Everything CI runs. Slower than a commit hook, cheaper than a red pipeline.
#
# This script only claims to have run what it actually ran. A check that reports success after a
# step failed is worse than no check, because it is trusted.
set -e

cd "$(dirname "$0")/.."

echo "==> build, tests and formatting"
./mvnw -B verify

# The traceability and documentation-sync gate is added here in phase 2, once `ai-tools trace`
# exists. See docs/roadmap/02-skeleton.md.

echo
echo "All checks passed."
