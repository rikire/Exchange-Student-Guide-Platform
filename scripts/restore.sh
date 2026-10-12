#!/usr/bin/env sh
# Restore the guide from a directory scripts/backup.sh wrote. Everything on the stand now -- articles,
# submissions, uploads -- is replaced by what the backup holds.
#
#   scripts/restore.sh <backup-directory>
#
# Run from the folder that holds docker-compose.yml.
set -eu

dir=${1:?usage: scripts/restore.sh <backup-directory>}
[ -f "$dir/database.dump" ] && [ -f "$dir/media.tar" ] || {
    echo "$dir does not hold database.dump and media.tar" >&2
    exit 1
}

printf 'This replaces everything on this stand with the backup in %s. Type yes to go on: ' "$dir"
read -r answer
[ "$answer" = yes ] || {
    echo "nothing changed"
    exit 1
}

echo "==> stopping the application"
docker compose stop app
docker compose up -d --wait db

echo "==> database"
docker compose exec -T db sh -c 'pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <"$dir/database.dump"

echo "==> uploaded files"
docker compose run --rm --no-deps -T --entrypoint sh app -c \
    'find /var/lib/guide/media -mindepth 1 -delete && tar -C /var/lib/guide/media -xf -' <"$dir/media.tar"

echo "==> starting the application; it rebuilds the search index as it starts"
docker compose up -d app
echo "==> done"
