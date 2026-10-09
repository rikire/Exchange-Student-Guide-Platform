# Backup and restore

What is worth keeping is the **database** (articles, their earlier versions, submissions, reports)
and the **uploaded files**. The search index is not backed up: the guide builds it again from the
database every time it starts.

## Make a backup

From the folder that holds `docker-compose.yml`, with the guide running:

```bash
scripts/backup.sh
```

It writes `backups/guide-<date>-<time>/` with two files, `database.dump` and `media.tar`. **Copy that
folder off the server** (a shared drive, another machine): a backup on the same disk is lost with it.

To make one every night at 02:00, add to the crontab of a user allowed to run Docker
(`crontab -e`), with the path to the guide's folder:

```
0 2 * * * cd /path/to/guide && scripts/backup.sh >> backups/backup.log 2>&1
```

Old backups are not removed by the script; delete the ones you no longer need.

## Restore

```bash
scripts/restore.sh backups/guide-20261009-020000
```

It asks before doing anything, because **everything on the guide now is replaced** by the backup:
articles, submissions and uploads made after it are gone. Then it puts the database and the files
back and starts the guide, which rebuilds the search index.

## Check that backups work

A backup nobody has restored is a hope. Once after installing, and after any change to the server,
restore the latest backup on a spare machine (or on the same one, right after making it) and open
an article that has a photo. The scripts were checked this way on 9 October 2026: a backup, the
stand emptied (`docker compose down -v`), the restore — 38 articles, an approved edit with its
photo and its submission came back, and search found the edit's text.
