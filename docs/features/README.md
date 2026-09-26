# Feature backlog

Every feature file with its status, the requirements it covers and the slice it lives in.

**Generated** by `java -jar tools/target/ai-tools.jar trace`. An edit made here is lost on the next run.

| Feature | Title | Status | Covers | Slice | Routes | Tables |
|---|---|---|---|---|---|---|
| FEAT-001 | [Wiki links in article text](FEAT-001-wiki-links-in-article-text.md) | done | FR-002, FR-004 | wikilink |  |  |
| FEAT-002 | [Reading an article](FEAT-002-article-page.md) | in-progress | FR-001, FR-002, FR-004 | articleview | GET /articles/{title} | article, tag, article_tag |
| FEAT-003 | [Landing page](FEAT-003-landing-page.md) | in-progress | FR-009 | home | GET / | article, tag, article_tag |
| FEAT-004 | [Article archive — export, import and seed](FEAT-004-article-archive.md) | in-progress | NFR-004, FR-009 | backup |  | article, tag, article_tag |
