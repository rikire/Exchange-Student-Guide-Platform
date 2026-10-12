# Installing and running the guide

For whoever looks after the server (IITM's IT, or someone at OGE comfortable with a terminal). The
moderators' own guide is [moderator-guide.md](moderator-guide.md).

The guide is one application and one PostgreSQL database, both run by Docker Compose from this
repository. Uploaded files and the search index live in Docker volumes beside the database.

## What the server needs

- Linux with **Docker Engine** and the **Docker Compose plugin** (`docker compose version` answers).
- Disk for the uploads: the guide refuses new files once they total 100 GB (`GUIDE_MEDIA_VOLUMELIMIT`).
- Outbound internet once, for the first build (it downloads Java libraries).

## 1. Get the code

```bash
git clone https://github.com/rikire/Exchange-Student-Guide-Platform.git guide
cd guide
```

## 2. Configure it

```bash
cp .env.example .env
```

Then edit `.env`:

- `POSTGRES_PASSWORD` — any long random string, for example the output of `openssl rand -base64 24`.
  Nobody types it; only the application uses it.
- `GUIDE_ADMIN_PASSWORD_HASH` — the moderators' shared password, stored only as its hash.
  **Use a long random password**, not a word: the guide allows ten wrong guesses per visitor every
  15 minutes, which stops a slow guesser and does not stop a dictionary of common passwords.
  ```bash
  openssl rand -base64 18                     # a password; give it to the moderators
  docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" 'the password' | tr -d ':\n'
  ```
  Paste the hash **in single quotes** (`GUIDE_ADMIN_PASSWORD_HASH='$2y$10$…'`): it contains `$`.
  Left empty, nobody can sign in as a moderator.

The other settings in `.env.example` (upload limits, how many submissions one visitor may send an
hour, the time zone) have working defaults; each is explained there.

## 3. Start it

```bash
docker compose up -d --build
docker compose logs -f app        # until "Started GuideApplication"; Ctrl+C leaves it running
```

The first start creates the database, imports the 38 starter articles and builds the search index.
Open `http://<server>:8080/`. Moderators sign in at `http://<server>:8080/moderate/login`.

Docker restarts nothing by itself after a reboot of the server unless told to: add
`restart: unless-stopped` to both services in `docker-compose.yml`, or start it again with
`docker compose up -d`.

## 4. Behind IITM's HTTPS proxy

The guide speaks plain HTTP on port 8080; HTTPS is the proxy's. Three things go together:

1. **The proxy** forwards to `http://<server>:8080`, sets `X-Forwarded-For` and
   `X-Forwarded-Proto`, and **accepts request bodies of at least 510 MB**: a video may be 500 MB
   (`GUIDE_MEDIA_VIDEOLIMIT`), and many proxies refuse anything over 1 MB by default (nginx:
   `client_max_body_size 510m;`).
2. **`.env`** tells the guide which address is the proxy, so it believes the proxy about each
   visitor's address and about https, and nobody else:
   ```
   GUIDE_FORWARD_HEADERS=native
   GUIDE_TRUSTED_PROXY=10\.1\.2\.3        # the proxy's own address, dots escaped
   GUIDE_COOKIE_SECURE=true
   ```
   Without the first two, every visitor looks like the proxy: ten wrong passwords from anyone would
   lock the moderators out for 15 minutes, and five submissions an hour would be shared by every
   student. Without the third, the moderators' session cookie could also travel over plain HTTP.
3. **Port 8080 reachable from the proxy only** (a firewall rule, or `"127.0.0.1:8080:8080"` under
   `ports:` when the proxy runs on the same server), so nobody reaches the guide around the proxy.

Then `docker compose up -d` again. Signing in should land on `https://…/moderate/queue`.

## 5. Updating to a newer version

```bash
scripts/backup.sh                          # first, always: see backup.md
git pull
docker compose up -d --build
```

The database is migrated and the search index rebuilt as the application starts. Uploads and the
database are kept.

A stand first started **before 2 October 2026** (only ours: the image then ran as root) stops with
`HSEARCH600001: … does not point to a writable directory`. Run once, then start again:

```bash
docker compose run --rm --no-deps --user root --entrypoint chown app -R guide:guide /var/lib/guide
```

`scripts/upgrade-check.sh <older-version>` tries an update on volumes of its own before it is done
for real (it needs Java 21 and Docker on the machine that runs it).

## When something is wrong

| You see | Look at | Usually |
|---|---|---|
| The site does not answer | `docker compose ps`, `docker compose logs app` | the app stopped; `docker compose up -d` |
| `HSEARCH600001 … not … writable` in the log | section 5 | volumes from an older image |
| Moderators cannot stay signed in | `.env` | `GUIDE_COOKIE_SECURE=true` while the site is served over plain HTTP |
| Big uploads fail with a connection error | the proxy's body size limit | section 4, point 1 |
| Everyone gets "Too many …" at once | `.env` | the proxy is not trusted: section 4, point 2 |
