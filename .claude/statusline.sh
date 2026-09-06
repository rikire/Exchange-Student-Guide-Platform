#!/bin/sh
# The status line: the few facts that change a decision mid-session.
#
# Context pressure is the constraint the other advice follows from - a session that has filled its
# window starts forgetting the instructions rather than breaking them - so the percentage is here
# rather than in a command someone has to remember to run. The open debt count is here because a
# register nobody looks at is a register that only grows.
#
# Parsing is done with sed rather than jq: jq is not present on every machine this repository is
# developed on, and a status line is the wrong place to acquire a dependency. A field that cannot
# be read renders as nothing, which is the correct behaviour for a display.
set -u

input=$(cat)

number() {
	printf '%s' "$input" | sed -n "s/.*\"$1\"[[:space:]]*:[[:space:]]*\([0-9][0-9.]*\).*/\1/p" | head -1
}

string() {
	printf '%s' "$input" | sed -n "s/.*\"$1\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" | head -1
}

out=""
add() { [ -n "$1" ] && out="${out:+$out | }$1"; }

add "$(string display_name)"

branch=$(git branch --show-current 2>/dev/null)
add "$branch"

used=$(number used_percentage)
[ -n "$used" ] && add "ctx ${used%.*}%"

root=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -n "$root" ] && [ -f "$root/docs/tech-debt.md" ]; then
	debt=$(sed '/<!--/,/-->/d' "$root/docs/tech-debt.md" | grep -c '^\*\*Status:\*\* open' 2>/dev/null || true)
	[ "${debt:-0}" -gt 0 ] && add "debt ${debt}"
fi

printf '%s\n' "$out"
