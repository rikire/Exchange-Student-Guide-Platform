---
paths:
  - "**/src/test/**/*.java"
---

# Tests

Full document: [docs/ai/testing.md](../../docs/ai/testing.md) — including how to *derive* the corner
cases, which is deliberately a method rather than a list.

- **The test comes first and must fail first.** A test that is green before the implementation
  checks nothing.
- One behaviour per test. Three assertions about three different things fail without saying which
  broke.
- Test names read as sentences, underscores only:
  `rejects_an_upload_whose_content_does_not_match_its_extension`.
- Arrange / act / assert separated by blank lines, with no comments marking the sections.
- **No `Thread.sleep`** — an edit adding one under `src/test/` is refused. If timing matters, make
  the clock injectable.
- **No `@Disabled` without a debt entry** — also refused.
- `@SpringBootTest` is the last resort, not the default: it is slow, and a slow suite stops being run.
- Fixtures live beside the test that uses them. A shared fixture is a shared file, which is what two
  people working in parallel least want.
- A test carries `//trace:FR-XXX` for the requirement it verifies.
