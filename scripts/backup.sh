#!/usr/bin/env sh
# Back up the guide: the database (articles, submissions, revisions, reports) and the uploaded files.
# The search index is not backed up; it is built again from the database at every start (ADR-0004).
#
#   scripts/backup.sh [directory]     # default: backups/guide-<date>-<time>
#
# Run from the folder that holds docker-compose.yml, with the stand running. The result is two files,
# database.dump and media.tar; copy the directory somewhere that is not this machine.
set -eu

out=${1:-backups/guide-$(date +%Y%m%d-%H%M%S)}
mkdir -p "$out"

echo "==> database"
docker compose exec -T db sh -c 'pg_dump --format=custom -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >"$out/database.dump"

echo "==> uploaded files"
docker compose run --rm --no-deps -T --entrypoint tar app -C /var/lib/guide/media -cf - . >"$out/media.tar"

echo "==> done: $out ($(du -sh "$out" | cut -f1))"
