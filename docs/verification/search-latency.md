# Search latency (NFR-002)

NFR-002's fit criterion: search returns results within 2 seconds on a corpus of 100 articles of
roughly 500 words each.

## How it is measured

`scripts/search-latency.sh` measures it on the compose stand. It runs a project of its own
(`guide-load`, port 8081, its own volumes), so the demo stand and its content are untouched, and it
removes everything it started when it exits.

1. The stand starts on an empty database without the seed articles, and Flyway builds the schema.
2. PostgreSQL generates 100 articles of 500 words each. The words come from a vocabulary of 135,
   drawn with a fixed seed (`setseed(0.42)`), so a rerun searches the same corpus. Each article
   gets its own text; this was checked with the same generator, giving 100 distinct bodies of 500
   words.
3. The app restarts on its empty index and rebuilds it from the database (FEAT-007). The script
   waits until a search reports all 100 articles.
4. It records the first search after the rebuild on its own. Then it runs one warm-up round of 20
   queries, and five measured rounds of the same 20: single words, pairs, and one query of nine words.
   Each search is timed by `curl` from the host (`time_total`) and must answer `200`.

## Result, 30 September 2026

| | |
|---|---|
| Where | the compose stand on the development Mac (Docker Desktop, PostgreSQL 17.11, the app's Dockerfile image) |
| Corpus | 100 articles × 500 words |
| First search after the index was built | 0.022 s |
| Measured searches | 100 |
| Minimum / median / 95th percentile / maximum | 0.007 s / 0.009 s / 0.019 s / 0.023 s |
| Limit | 2 s |
| Verdict | **passes** |

Measured with Thymeleaf's template cache off, as the stand runs today
([DEBT-020](../tech-debt.md)); with it on, a page can only be faster.

## What this does not show

- The times are for the server and the local network only. A reader on a phone adds their own
  connection.
- The requests came one at a time. Many readers searching at once were not measured: NFR-002 does
  not ask for it, and nothing before phase 5 does either.
- The "demo stand" is compose on the development Mac. OGE's server, when there is one, has not been
  measured. The script runs there unchanged.
