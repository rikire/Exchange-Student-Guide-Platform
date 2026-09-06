---
paths:
  - "**/*.java"
---

# Java, the parts a formatter cannot settle

Full document: [docs/ai/code-style.md](../../docs/ai/code-style.md). This is the load-bearing
subset, here because it has to be in context while the code is being written rather than available
to be looked up afterwards.

- Spotless with `palantir-java-format` owns all layout. Do not argue with it or configure around it.
- **No Lombok.** Every line has to be explainable at the viva without "the annotation generates it".
- **No `Util`/`Helper` classes, no `Impl` suffix.** A method with no home belongs to the type it
  operates on; with one implementation the class is simply `SearchService`.
- Comment **why**, never **what**. Javadoc is mandatory on types sitting directly in a slice
  package — those are the contract other slices depend on.
- Never swallow an exception. An empty `catch` is a defect; an expected one carries a comment
  saying why. Messages name the value that caused the failure, never a secret.
- SLF4J, never `System.out`, parameterised form only. Never log article content in full.
- English in code, comments, commit messages, log messages and exception messages.
- Any `TODO`, `FIXME` or `HACK` carries a `DEBT-XXX` that exists in
  [docs/tech-debt.md](../../docs/tech-debt.md). An edit adding one without it is refused.
