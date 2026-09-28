# Feature backlog

Every feature file with its status, the requirements it covers and the slice it lives in.

**Generated** by `java -jar tools/target/ai-tools.jar trace`. An edit made here is lost on the next run.

| Feature | Title | Status | Covers | Slice | Routes | Tables |
|---|---|---|---|---|---|---|
| FEAT-001 | [Wiki links in article text](FEAT-001-wiki-links-in-article-text.md) | done | FR-002, FR-004 | wikilink |  |  |
| FEAT-002 | [Reading an article](FEAT-002-article-page.md) | in-progress | FR-001, FR-002, FR-004 | articleview | GET /articles/{title} | article, tag, article_tag |
| FEAT-003 | [Landing page](FEAT-003-landing-page.md) | in-progress | FR-009 | home | GET / | article, tag, article_tag |
| FEAT-004 | [Article archive — export, import and seed](FEAT-004-article-archive.md) | in-progress | NFR-004, FR-009 | backup |  | article, tag, article_tag |
| FEAT-005 | [Submitting a new article or an edit](FEAT-005-submitting-an-article-or-an-edit.md) | done | FR-003, FR-008, FR-010, FR-011, NFR-006 | contribute | GET /submit, POST /submissions, GET /articles/{title}/edit, POST /articles/{title}/edits, GET /submissions/{number}/confirmation | submission, submission_tag, tag, article |
| FEAT-006 | [Moderating a submission](FEAT-006-moderating-a-submission.md) | done | FR-014, FR-015, FR-017, FR-018, FR-019, FR-020, FR-029 | moderate | GET /moderate/login, POST /moderate/login, GET /moderate/queue, GET /moderate/submissions/{number}, POST /moderate/submissions/{number}/approve, POST /moderate/submissions/{number}/reject | submission, submission_tag, article, article_tag, tag, revision |
| FEAT-007 | [Searching the guide](FEAT-007-searching-the-guide.md) | done | FR-007 | search | GET /search | article, article_tag, tag |
| FEAT-008 | [Browsing by tag](FEAT-008-browsing-by-tag.md) | done | FR-008 | taxonomy | GET /tags/{tag} | article, article_tag, tag |
| FEAT-009 | [Attaching media to a submission](FEAT-009-attaching-media-to-a-submission.md) | done | FR-010, FR-011, FR-015, FR-001, FR-016, NFR-001 | media | POST /submissions, POST /articles/{title}/edits, GET /media/{id} | media_asset |
| FEAT-010 | [The Markdown editor on the submission form](FEAT-010-markdown-editor.md) | done | FR-027 | contribute | POST /contribute/preview | article |
| FEAT-011 | [A layout for any screen width and for the keyboard](FEAT-011-layout-for-any-screen-and-the-keyboard.md) | done | NFR-007, NFR-008 | shared |  |  |
| FEAT-012 | [Looking up a submission's status](FEAT-012-looking-up-a-submission.md) | done | FR-012 | contribute | GET /submissions/status | submission, article |
