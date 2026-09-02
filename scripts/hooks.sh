#!/bin/sh
# Builds ai-tools.jar and points git at the tracked hooks.
# Run once after cloning: scripts/hooks.sh
set -e

cd "$(dirname "$0")/.."

echo "Building ai-tools.jar ..."
./mvnw -q -B -pl tools package

echo "Pointing git at .githooks ..."
git config core.hooksPath .githooks
chmod +x .githooks/* 2>/dev/null || true

echo
echo "Done. The Claude Code hooks and the git hooks are both live."
echo "The jar is at tools/target/ai-tools.jar and is rebuilt by this script."
