#!/bin/sh
# NFR-002's load check: search answers within 2 seconds on 100 articles of about 500 words.
#
# Runs on the compose stand, in a project of its own (guide-load, port 8081, its own volumes), so the
# demo stand and its content are untouched; everything it starts is removed on exit. The articles are
# generated inside PostgreSQL with a fixed seed, so a rerun searches the same corpus. The index is
# rebuilt from the database when the app starts on an empty one (FEAT-007).
#
# Usage: scripts/search-latency.sh        (ARTICLES, WORDS, RUNS and PORT may be set to change it)
# Exit status: 0 when the slowest measured search is under the limit, 1 otherwise.
set -eu
cd "$(dirname "$0")/.."

PROJECT=guide-load
PORT=${PORT:-8081}
ARTICLES=${ARTICLES:-100}
WORDS=${WORDS:-500}
RUNS=${RUNS:-5}
LIMIT=2.0
BASE="http://localhost:$PORT"

override=$(mktemp)
times=$(mktemp)
compose() { docker compose -p "$PROJECT" -f docker-compose.yml -f "$override" "$@"; }
cleanup() {
	compose down -v >/dev/null 2>&1 || true
	rm -f "$override" "$times"
}
trap cleanup EXIT

# The load stand serves no seed articles and listens beside the demo stand, not on its port.
cat >"$override" <<EOF
services:
  app:
    environment:
      SPRING_PROFILES_ACTIVE: ""
    ports: !override
      - "$PORT:8080"
EOF

wait_for_the_app() {
	tries=0
	until curl -fs -o /dev/null "$BASE/"; do
		tries=$((tries + 1))
		if [ "$tries" -gt 90 ]; then
			echo "The load stand did not answer on $BASE." >&2
			exit 1
		fi
		sleep 2
	done
}

echo "==> starting the load stand ($PROJECT, $BASE)"
compose up --build -d >/dev/null 2>&1
wait_for_the_app
compose stop app >/dev/null 2>&1

echo "==> writing $ARTICLES generated articles of $WORDS words"
compose exec -T db sh -c 'psql -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<SQL
SELECT setseed(0.42) AS seed \gset
WITH vocabulary AS (
  SELECT ARRAY['visa', 'hostel', 'mess', 'campus', 'bus', 'library', 'registration', 'fees', 'payment',
    'wifi', 'bank', 'account', 'hospital', 'exam', 'timetable', 'course', 'credits', 'semester', 'frro',
    'office', 'airport', 'transfer', 'beach', 'festival', 'shopping', 'laundry', 'student', 'exchange',
    'department', 'professor', 'lecture', 'lab', 'assignment', 'grade', 'attendance', 'portal', 'document',
    'passport', 'insurance', 'doctor', 'pharmacy', 'canteen', 'coffee', 'tea', 'dinner', 'breakfast',
    'lunch', 'room', 'key', 'card', 'identity', 'form', 'deadline', 'week', 'month', 'morning', 'evening',
    'train', 'metro', 'auto', 'cab', 'road', 'gate', 'building', 'hall', 'temple', 'market', 'mall',
    'city', 'chennai', 'india', 'tamil', 'language', 'class', 'friend', 'club', 'sport', 'cricket',
    'football', 'gym', 'pool', 'music', 'dance', 'culture', 'food', 'rice', 'curry', 'water', 'rain',
    'monsoon', 'summer', 'weather', 'phone', 'sim', 'internet', 'email', 'contact', 'help', 'safety',
    'guide', 'map', 'walk', 'cycle', 'forest', 'deer', 'lake', 'river', 'sea', 'sunset', 'weekend',
    'holiday', 'travel', 'ticket', 'booking', 'queue', 'counter', 'receipt', 'cash', 'upi', 'price',
    'the', 'a', 'and', 'of', 'to', 'in', 'is', 'for', 'on', 'with', 'you', 'your', 'at', 'from', 'by']
    AS words
)
INSERT INTO article (id, title, slug, summary, body, published_at, updated_at)
SELECT gen_random_uuid(),
       'Load check article ' || lpad(n::text, 3, '0'),
       'load-check-article-' || lpad(n::text, 3, '0'),
       'Generated for the NFR-002 load check.',
       -- n inside the word series ties each body to its row, so every article gets its own text.
       (SELECT string_agg(v.words[1 + floor(random() * array_length(v.words, 1))::int], ' ')
          FROM generate_series(1, $WORDS + 0 * n), vocabulary AS v),
       now(), now()
  FROM generate_series(1, $ARTICLES) AS n;
SQL

echo "==> restarting the app so it builds the index from the database"
compose start app >/dev/null 2>&1
wait_for_the_app
tries=0
until curl -fs "$BASE/search?q=load" | grep -q ">$ARTICLES results for"; do
	tries=$((tries + 1))
	if [ "$tries" -gt 60 ]; then
		echo "The index did not reach $ARTICLES articles." >&2
		exit 1
	fi
	sleep 2
done

measure() {
	curl -s -o /dev/null -w '%{http_code} %{time_total}\n' --get --data-urlencode "q=$1" "$BASE/search"
}

queries='visa
hostel
mess food
campus bus
library hours
registration
fees payment
wifi
bank account
hospital
exam timetable
course credits
semester
frro office
airport transfer
beach sunset
festival
shopping mall
laundry
hostel mess registration fees campus bus timetable'

first=$(measure "visa" | cut -d' ' -f2)
echo "==> the first search after the index was built: ${first} s"

echo "==> one warm-up round, then $RUNS measured rounds of 20 queries"
printf '%s\n' "$queries" | while IFS= read -r q; do measure "$q" >/dev/null; done
round=0
while [ "$round" -lt "$RUNS" ]; do
	printf '%s\n' "$queries" | while IFS= read -r q; do
		result=$(measure "$q")
		if [ "${result%% *}" != 200 ]; then
			echo "\"$q\" answered ${result%% *}" >&2
			exit 1
		fi
		echo "${result#* }" >>"$times"
	done
	round=$((round + 1))
done

sort -n "$times" | awk -v limit="$LIMIT" -v articles="$ARTICLES" -v words="$WORDS" '
	{ t[NR] = $1 }
	END {
		p95 = t[int(NR * 0.95 + 0.999)]
		printf "%d searches on %d articles of %d words: min %.3f s, median %.3f s, p95 %.3f s, max %.3f s\n",
			NR, articles, words, t[1], t[int((NR + 1) / 2)], p95, t[NR]
		if (t[NR] < limit) { printf "PASS: the slowest search is under %.1f s\n", limit; exit 0 }
		printf "FAIL: the slowest search took %.3f s, the limit is %.1f s\n", t[NR], limit; exit 1
	}'
