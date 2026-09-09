#!/bin/sh
# Says, at the start of a session, whether the guard layer is actually running.
#
# Every other hook in this repository is the same jar. `target/` is not tracked, so on a fresh
# clone, after `mvnw clean`, or before anyone has run scripts/hooks.sh, that jar does not exist —
# and a hook whose command exits non-zero is a *non-blocking* error. The session then proceeds with
# no prompt journal, no protected-path guard and no documentation gate, and the only sign is a line
# on stderr that nobody is obliged to read. The honesty table in docs/ai/README.md would still be
# claiming all of it was enforced.
#
# So this one is deliberately not the jar: it has to be able to say that the jar is missing. It is
# a reporter and not a gate — it can fail silently in exactly the same way, and if `sh` is not on
# PATH it will. The refusals that must not depend on a build artefact live in `permissions.deny` in
# .claude/settings.json, where the client enforces them and no process of ours has to survive.
set -u

root=${1:-$(cd "$(dirname "$0")/.." && pwd)}
cd "$root" 2>/dev/null || exit 0

jar=tools/target/ai-tools.jar

if [ ! -f "$jar" ]; then
	echo "ENFORCEMENT IS OFF. $jar does not exist, so the prompt journal, the"
	echo "protected-path guard, the bash-bypass refusal and the documentation gate are all inactive."
	echo "Nothing else will say so. Build it before trusting any of them:  ./mvnw -pl tools package"
elif [ -n "$(find tools/src -type f -newer "$jar" -print 2>/dev/null | head -1)" ]; then
	echo "The guard jar is older than tools/src, so the rules running now are an earlier"
	echo "generation than the ones the documentation describes.  Rebuild:  ./mvnw -pl tools package"
fi

# The phase is the first row of the roadmap table that is not done. It is worth the few lines
# because it is the one fact a session cannot infer and CLAUDE.md cannot hold: it changes.
if [ -f docs/roadmap/README.md ]; then
	awk -F'|' '/^\| [0-9] \|/ {
		status = $6; ends = $5; number = $2
		gsub(/^[ \t]+|[ \t]+$/, "", status)
		gsub(/^[ \t]+|[ \t]+$/, "", ends);   gsub(/\*/, "", ends)
		gsub(/^[ \t]+|[ \t]+$/, "", number)
		if (status != "done") {
			print "Roadmap: phase " number " (" status "), ends at " ends "."
			exit
		}
	}' docs/roadmap/README.md
fi

# The deadline rule in docs/ai/roadmap.md says to speak up when a deadline is at risk. It had never
# fired once, because a rule held by memory competes with everything that arrives after it. A date
# cannot be argued with, so the rule becomes a number printed here instead.
if [ -f docs/course/rubric.md ]; then
	today_s=$(date -d "$(date +%Y-%m-%d)" +%s)
	awk -F'|' '/^\| [0-9] \| [A-Za-z]+ [0-9]/ {
		when = $3; what = $4
		gsub(/^[ \t]+|[ \t]+$/, "", when)
		gsub(/^[ \t]+|[ \t]+$/, "", what)
		sub(/^[A-Za-z]+ /, "", when)
		print when "\t" what
	}' docs/course/rubric.md | while IFS="$(printf '\t')" read -r when what; do
		when_s=$(date -d "$when" +%s 2>/dev/null || echo 0)
		[ "$when_s" -eq 0 ] && continue
		days=$(((when_s - today_s) / 86400))
		if [ "$days" -ge 0 ]; then
			echo "Next deadline: $what, $when — $days day(s) away."
			break
		fi
	done
fi

# Open items in the phase that is running. The count is the honest half of the line above: a
# deadline with no work left against it is not the same situation as one with eleven.
phase_file=$(ls docs/roadmap/0*.md 2>/dev/null | while read -r f; do
	grep -q '^\*\*Status: in progress' "$f" && echo "$f" && break
done)
if [ -n "${phase_file:-}" ]; then
	open=$(grep -c '^- \[[ ~]] ' "$phase_file" 2>/dev/null || echo 0)
	echo "Open items in $(basename "$phase_file"): ${open:-0}."
fi

# Commented-out lines are stripped first: the register carries a template whose Status is "open",
# and a counter that reports debt nobody owes is one people stop reading.
if [ -f docs/tech-debt.md ]; then
	open=$(sed '/<!--/,/-->/d' docs/tech-debt.md | grep -c '^\*\*Status:\*\* open' 2>/dev/null || true)
	[ "${open:-0}" -gt 0 ] && echo "Technical debt: $open open entry/entries in docs/tech-debt.md."
fi

exit 0
