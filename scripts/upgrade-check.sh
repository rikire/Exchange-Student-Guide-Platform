#!/usr/bin/env bash
# Upgrade check: does the image built from this working tree start on the volumes an older version
# filled, and keep working there? The demo rehearsal of 5 Oct found two ways it did not: volumes the
# root image wrote (DEBT-026), and a search index from before a mapping change (ADR-0004, amended).
# No test inside the JVM can see either, since both live in what an older image left on disk.
#
#   scripts/upgrade-check.sh [base-ref]     # base-ref defaults to origin/main
#
# It runs a compose project of its own (guide-upgrade, port 18080, its own volumes, a throwaway
# database and moderator password), so the stand is not touched, and removes all of it at the end.
# Steps: the base image starts, an edit with a photo is sent and approved; the new image starts on
# the same volumes; then the page, search for the base's edit, the base's photo, and a second edit
# with a photo, approved and found, are checked. Exits 1 at the first check that fails.
set -euo pipefail

BASE_REF=${1:-origin/main}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
PROJECT=guide-upgrade
PORT=18080
URL=http://localhost:$PORT
WORK=$(mktemp -d)
JAR_CP_FILE=$WORK/cp.txt

say() { printf '==> %s\n' "$*"; }
fail() {
    printf 'FAIL: %s\n' "$*" >&2
    compose new logs app --tail 40 >&2 || true
    exit 1
}

compose() {
    local tag=$1
    shift
    docker compose -p "$PROJECT" --env-file "$WORK/env" -f "$ROOT/docker-compose.yml" -f "$WORK/$tag.yml" "$@"
}

cleanup() {
    compose new down -v --remove-orphans >/dev/null 2>&1 || true
    git -C "$ROOT" worktree remove --force "$WORK/base" >/dev/null 2>&1 || true
    docker image rm "$PROJECT:base" "$PROJECT:new" >/dev/null 2>&1 || true
    rm -rf "$WORK"
}
trap cleanup EXIT

# --- throwaway configuration --------------------------------------------------------------------

say "classpath for the password hash"
(cd "$ROOT" && ./mvnw -q -pl app dependency:build-classpath -Dmdep.outputFile="$JAR_CP_FILE" >/dev/null)
PASSWORD=$(head -c 18 /dev/urandom | base64 | tr -d '/+=')
cat >"$WORK/Hash.java" <<'EOF'
public class Hash {
    public static void main(String[] args) {
        System.out.print(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(args[0]));
    }
}
EOF
HASH=$(java -cp "$(cat "$JAR_CP_FILE")" "$WORK/Hash.java" "$PASSWORD")
cat >"$WORK/env" <<EOF
POSTGRES_PASSWORD=$(head -c 18 /dev/urandom | base64 | tr -d '/+=')
GUIDE_ADMIN_PASSWORD_HASH='$HASH'
GUIDE_CONTRIBUTE_SUBMISSIONLIMIT_REQUESTS=100
EOF
for tag in base new; do
    cat >"$WORK/$tag.yml" <<EOF
services:
  app:
    image: $PROJECT:$tag
    build: !reset null
    ports: !override
      - "$PORT:8080"
EOF
done

# --- images -------------------------------------------------------------------------------------

say "base image from $BASE_REF"
git -C "$ROOT" worktree add --detach "$WORK/base" "$BASE_REF" >/dev/null
docker build -q -t "$PROJECT:base" "$WORK/base" >/dev/null
say "new image from the working tree"
docker build -q -t "$PROJECT:new" "$ROOT" >/dev/null

# --- HTTP helpers -------------------------------------------------------------------------------

wait_for_start() {
    for _ in $(seq 1 90); do
        [ "$(curl -s -o /dev/null -w '%{http_code}' "$URL/")" = 200 ] && return 0
        sleep 2
    done
    return 1
}

token() { grep -o 'name="_csrf" value="[^"]*"' "$1" | head -1 | sed 's/.*value="//;s/"$//'; }

photo() {
    cat >"$WORK/Photo.java" <<'EOF'
public class Photo {
    public static void main(String[] args) throws Exception {
        var image = new java.awt.image.BufferedImage(64, 48, java.awt.image.BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(image, "jpg", new java.io.File(args[0]));
    }
}
EOF
    java -Djava.awt.headless=true "$WORK/Photo.java" "$1"
}

# Sends an edit of $1 adding the sentence $2 with a photo; prints the submission number.
send_edit() {
    local slug=$1 sentence=$2 jar=$WORK/visitor
    rm -f "$jar"
    curl -s -c "$jar" -b "$jar" "$URL/articles/$slug/edit" -o "$WORK/form.html"
    local article title summary body
    article=$(grep -o 'name="article" value="[^"]*"' "$WORK/form.html" | sed 's/.*value="//;s/"$//')
    title=$(grep -o 'name="title"[^>]*value="[^"]*"' "$WORK/form.html" | sed 's/.*value="//;s/"$//')
    summary=$(grep -o 'name="summary"[^>]*value="[^"]*"' "$WORK/form.html" | sed 's/.*value="//;s/"$//')
    body="$sentence"
    photo "$WORK/photo.jpg"
    curl -s -c "$jar" -b "$jar" -o /dev/null -w '%{redirect_url}' \
        --form-string "_csrf=$(token "$WORK/form.html")" --form-string "article=$article" \
        --form-string "title=$title" --form-string "summary=$summary" --form-string "body=$body" \
        --form-string "tags=upgrade" \
        -F "attachment=@$WORK/photo.jpg;type=image/jpeg" \
        "$URL/articles/$slug/edits" | grep -o 'SUB-[A-Z0-9-]*'
}

approve() {
    local number=$1 jar=$WORK/moderator
    rm -f "$jar"
    curl -s -c "$jar" -b "$jar" "$URL/moderate/login" -o "$WORK/login.html"
    curl -s -c "$jar" -b "$jar" -o /dev/null --data-urlencode "_csrf=$(token "$WORK/login.html")" \
        --data-urlencode "password=$PASSWORD" "$URL/moderate/login"
    curl -s -c "$jar" -b "$jar" "$URL/moderate/submissions/$number" -o "$WORK/review.html"
    # The summary to approve with: a textarea's text since 8 Oct, an input's value before.
    local summary
    summary=$(grep -o '<textarea[^>]*name="summary"[^>]*>[^<]*' "$WORK/review.html" | head -1 | sed 's/.*>//')
    if [ -z "$summary" ]; then
        summary=$(grep -o '<input[^>]*name="summary"[^>]*>' "$WORK/review.html" | head -1 \
            | grep -o 'value="[^"]*"' | sed 's/^value="//;s/"$//')
    fi
    [ -n "$summary" ] || fail "no summary found on the review page of $number"
    curl -s -c "$jar" -b "$jar" -o /dev/null -w '%{http_code}' --data-urlencode "_csrf=$(token "$WORK/review.html")" \
        --data-urlencode "summary=$summary" --data-urlencode "tags=upgrade" \
        "$URL/moderate/submissions/$number/approve"
}

found() { curl -s "$URL/search?q=$1" | grep -q "href=\"/articles/$2\""; }

# --- the base version fills the volumes ---------------------------------------------------------

say "base version starts on new volumes"
compose base up -d >/dev/null 2>&1
wait_for_start || fail "the base version did not start"
before=$(send_edit applying-for-your-student-visa "Upgradecheckbefore marks the edit the base version approved.")
[ -n "$before" ] || fail "the base version refused the edit"
[ "$(approve "$before")" = 302 ] || fail "the base version refused to approve $before"
found upgradecheckbefore applying-for-your-student-visa || fail "the base version does not find its own edit"
photo_path=$(curl -s "$URL/articles/applying-for-your-student-visa" | grep -o 'src="/media/[^"]*"' | head -1 | sed 's/src="//;s/"$//')
compose base stop app >/dev/null 2>&1

# --- the new version on the same volumes --------------------------------------------------------

say "new version starts on the base's volumes"
compose new up -d app >/dev/null 2>&1
wait_for_start || fail "the new version did not start on the base's volumes"
found upgradecheckbefore applying-for-your-student-visa || fail "search lost the base's edit"
[ "$(curl -s -o /dev/null -w '%{http_code}' "$URL$photo_path")" = 200 ] || fail "the base's photo is gone"
after=$(send_edit registering-with-frro "Upgradecheckafter marks the edit the new version approved.")
[ -n "$after" ] || fail "the new version refused an edit with a photo"
[ "$(approve "$after")" = 302 ] || fail "the new version refused to approve $after"
found upgradecheckafter registering-with-frro || fail "the new version does not find the edit it approved"

say "upgrade from $BASE_REF: passed"
