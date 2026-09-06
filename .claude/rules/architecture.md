---
paths:
  - "app/src/main/java/**"
---

# Slices and their boundaries

Full document: [docs/ai/architecture-rules.md](../../docs/ai/architecture-rules.md).

- A slice is a feature, named in the singular: `search`, `moderate`, `media`.
- **Public** is what sits *directly* in a slice package. Everything in a nested package —
  `internal/`, `web/` — is invisible to other slices.
- **No shared repositories.** A slice owns its persistence; another slice asks it, rather than
  reaching into its tables.
- **Validation always lives in the domain.** A form, a controller or a database constraint may
  repeat it, but none of them is where it is decided.
- **Moving something into `shared` is a stop-and-ask trigger** — it is the one place where two
  people's work collides. Creating a new file there asks first, and the question is not a formality.
- Production code carries `//trace:FR-XXX` for the requirement it implements.

`ModularityTest` is the mechanism that makes the boundary real, and it currently verifies an empty
set of modules because no slice package exists yet. Until the first one does, every rule above is
held by good faith rather than by the build.
