# Design document

**Due Friday 11 September 2026 — 5 marks.** Source: [design-doc.tex](design-doc.tex) · Built:
[design-doc.pdf](design-doc.pdf) (4 pages, 11 September 2026).

The `.tex` is the source of record and this `.md` is a pointer, not a copy — the same convention
[proposal.md](proposal.md) follows, and for the same reason: a second prose copy would drift from the
submitted PDF within a day, and the submitted PDF is the artefact.

## What it contains, against the rubric

| Rubric row | Marks | Section | Backed by |
|---|---|---|---|
| Architecture note identifies modules and interfaces | 2 | §1 | [architecture/](../architecture/), `ModularityTest`, the C4 level-3 figure |
| Test plan lists at least one test per module | 1 | §2 | the ten-row table in [01-requirements-design.md](../roadmap/01-requirements-design.md) |
| Milestone plan revised in light of scoping feedback | 1 | §3 | [scoping-feedback.md](scoping-feedback.md), [proposal.tex](proposal.tex) §8 |
| Risks and plan B are honest, not boilerplate | 1 | §4 | [roadmap/](../roadmap/) |

§5 is unscored and included anyway: how the decisions were arrived at, which is the part of this
work no other team could have written the same way.

## Two things it says that are easy to miss

**The architecture section describes a boundary the build enforces, and an implementation that has
not started.** Those are separate claims and the document keeps them separate. The eleven slice
packages are declared and empty; phase 2 writes the first working slice.

**§3 covers two milestone revisions with different causes.** Only the first answers the course's
scoping feedback of 28 August. The second came from a capacity review on 10 September and moved six
`should`/`could` items out of phase 3. The rubric row asks about the first; conflating them would
have overstated what the feedback produced.

## Building it

```bash
scripts/diagrams.sh                                   # the figure comes from docs/diagrams/out/
cd docs/course && pdflatex design-doc.tex && pdflatex design-doc.tex
```

Twice, because `hyperref` needs a second pass to resolve references. `.aux`, `.log` and `.out` are
git-ignored; the `.pdf` is committed, because it is what the course receives.
