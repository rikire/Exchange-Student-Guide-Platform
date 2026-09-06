# Glossary

One vocabulary, used identically in requirements, code, templates and conversation with the
stakeholder. Where the code and this document disagree, one of them is wrong and it is worth finding
out which.

Written in phase 1. The terms below are the ones already in use and will be defined properly then.

| Term | Meaning |
|---|---|
| Article | A published page in the knowledge base: a title (unique among published articles, case-insensitively), a body of text written in Markdown (which may contain `[[wiki link]]` markup — not raw HTML, see [ADR-0001](../architecture/adr/ADR-0001-article-body-format.md)), a set of tags, and zero or more attached media assets |
| Submission | A proposed new article or a proposed edit, waiting in the moderation queue |
| Moderator | An OGE staff member who approves or rejects submissions |
| Contributor | Anyone who submits an article or an edit; no account is involved |
| Tag | A free-form label on an article; an article may carry several |
| Wiki link | A reference from one article to another, written in the article text |
| Red link | A wiki link pointing at an article that does not exist yet |
| Backlink | The reverse direction: which articles link to this one |
| Revision | An article's retained content from before an approved edit changed it |
| Media asset | An uploaded file attached to an article |
| Seed | The starter content shipped with the application, in the export format |
