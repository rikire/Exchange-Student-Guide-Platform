# Mutation testing

Whether the assertions catch a change to the code, which line coverage cannot say. PIT changes one
thing in a compiled class (a `<` to `<=`, a removed call, a constant), runs the tests that cover it,
and counts the change as killed when one of them fails.

**How to run:** `./mvnw -pl app -P mutation test-compile org.pitest:pitest-maven:mutationCoverage`;
the report is `app/target/pit-reports/index.html`. By hand before a stage, never in CI
([testing.md](../ai/testing.md)). Only classes of pure logic whose tests start no Spring context are
mutated, so each mutant runs in milliseconds: the `wikilink` slice, `TextDiff`, `SubmissionNumbers`
and `DisplayTime`. A class tested only through `@SpringBootTest` would start a context per mutant.

No threshold fails the build. Every surviving mutant is read, and is either a missing assertion,
which gets a test, or equivalent, which is recorded below with why.

## 5 October 2026, before the mid-demo

PIT 1.30.0 with `pitest-junit5-plugin` 1.2.3, over five test classes (79 tests).

| | |
|---|---|
| Mutants | 132 |
| Killed | 130 (98 %) |
| Without coverage | 0 |
| Line coverage of the mutated classes | 262 of 267 (98 %) |

Both survivors are equivalent; no test was added.

- **`WikiLinkRenderer.Occurrence.parse`, `bar < 0` to `bar <= 0`.** At `bar == 0` the title is empty
  and the method has already returned `null`, so the two conditions never differ.
- **`WikiLinkRenderer.renderBody`, the call to `visitChildren` for a `CustomNode` in a heading
  removed.** The only custom node is `WikiLinkNode`, which has no children, and the one extension
  besides it makes tables, which a heading cannot hold. The call does nothing today; it would matter
  if an extension with inline children (strikethrough, say) were added.
