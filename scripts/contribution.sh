#!/bin/sh
# Who actually wrote what, per ISO week, with the hook's commits kept separate.
#
# `git shortlog` is the number a reader reaches for first, and on this repository it says the
# opposite of the truth. The `Stop` hook commits every journal entry itself, under the identity of
# whoever's machine the session ran on, so a member who prompts a lot and authors little outranks
# one who authors a lot and prompts little. On 6 September the raw counts were 57 to 38 one way
# while authored work was roughly 8 to 35 the other. Every anti-freeloading mechanism the course
# uses reads this history.
#
# So this reports the two numbers apart and never adds them up. It is not `ai-tools ownership`,
# which arrives in phase 2 and measures per slice; the obligation to produce a weekly log arrives
# before that, and this is what fills its "from git" half until then.
#
# Usage: sh scripts/contribution.sh [<since>]     e.g. sh scripts/contribution.sh 2026-09-01
set -u

cd "$(dirname "$0")/.." || exit 1
since=${1:-}
range=${since:+--since=$since}

registry=docs/team/members.yml

# Resolves an email to a member id through the registry, so a new machine with a new address shows
# up as "unregistered" rather than silently becoming a third person.
member_of() {
	awk -v target="$1" '
		/^  - id:/    { id = $3 }
		/^      - /   { email = $2; if (tolower(email) == tolower(target)) { print id; exit } }
	' "$registry"
}

# One line per commit: email, ISO week, and whether the Stop hook wrote it.
commits=$(git log $range --no-merges --date=format:'%G-W%V' --format='%ae|%ad|%s' \
	| awk -F'|' '{
		hook = ($3 ~ /^docs: record the journal entry for /) ? "hook" : "authored"
		print $1 "|" $2 "|" hook
	}')

[ -z "$commits" ] && { echo "No commits in range."; exit 0; }

weeks=$(printf '%s\n' "$commits" | cut -d'|' -f2 | sort -u)
emails=$(printf '%s\n' "$commits" | cut -d'|' -f1 | sort -u)

printf '%-12s %-14s %10s %10s\n' "WEEK" "MEMBER" "AUTHORED" "HOOK"
printf '%-12s %-14s %10s %10s\n' "----" "------" "--------" "----"

for week in $weeks; do
	for email in $emails; do
		id=$(member_of "$email")
		[ -z "$id" ] && id="UNREGISTERED"
		a=$(printf '%s\n' "$commits" | grep -c "^$email|$week|authored$" || true)
		h=$(printf '%s\n' "$commits" | grep -c "^$email|$week|hook$" || true)
		[ "$a" -eq 0 ] && [ "$h" -eq 0 ] && continue
		printf '%-12s %-14s %10s %10s\n' "$week" "$id" "$a" "$h"
	done
done

echo
echo "Authored totals, and the directories each member touched:"

# Aggregated per member, not per address: one person with two git identities is one contributor,
# and reporting them as two is the same class of error as counting the hook as a person.
ids=$(for email in $emails; do id=$(member_of "$email"); echo "${id:-UNREGISTERED}"; done | sort -u)

for id in $ids; do
	total=0
	shas=""
	for email in $emails; do
		this=$(member_of "$email")
		[ -z "$this" ] && this="UNREGISTERED"
		[ "$this" = "$id" ] || continue
		n=$(printf '%s\n' "$commits" | grep -c "^$email|.*|authored$" || true)
		total=$((total + n))
		# Filtered by subject here, so the file list below never has to see a subject line.
		shas="$shas $(git log $range --no-merges --author="$email" --format='%H|%s' \
			| grep -v '|docs: record the journal entry for ' | cut -d'|' -f1)"
	done
	[ "$total" -eq 0 ] && continue
	dirs=$(for sha in $shas; do git show --pretty=format: --name-only "$sha"; done \
		| grep '/' | grep -v '^docs/ai/journal/' \
		| cut -d/ -f1-2 | sort | uniq -c | sort -rn | head -6 \
		| awk '{printf "%s(%s) ", $2, $1}')
	printf '  %-14s %3s commits   %s\n' "$id" "$total" "$dirs"
done

echo
echo "Hook commits are the Stop hook writing journal entries, not authored work."
echo "They are reported so nobody adds them in by accident, and never totalled with the other column."
