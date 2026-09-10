# CS5013 rubric — 25 marks and the evidence for each

Every criterion from the course requirements, with the artefact in this repository that would be
shown as evidence. Checked with `/course-check`.

A row is only green when the linked artefact **says what the criterion needs**. A file that exists
but is still a template counts as missing. Being wrong about this in September is cheap; being
wrong in November is not.

Legend: `[ ]` no evidence · `[~]` partial · `[x]` defensible

## Timeline

| Phase | Date | Deliverable | Marks |
|---|---|---|---|
| 2 | Fri 21 Aug 2026 | Proposal | 5 — submitted |
| 3 | Fri 11 Sep 2026 | Design document | 5 |
| 4 | Fri 9 Oct 2026 | Mid-demo | 5 |
| 5 | Fri 6 Nov 2026 | Final demo and viva | 10 |

## Proposal — 5 marks (submitted 18 Aug 2026)

| Criterion | Marks | Evidence | State |
|---|---|---|---|
| Realism of scope for one semester | 1 | `Project Proposal.pdf` | [x] |
| Quality of the prior-work section | 1 | `Project Proposal.pdf`, section 4 | [x] |
| Stakeholder acknowledgement present and credible | 1 | [../stakeholder/acknowledgement.md](../stakeholder/acknowledgement.md) | [ ] |
| Verification plan is concrete | 1 | `Project Proposal.pdf`, section 7 | [x] |
| Milestone plan is coherent | 1 | `Project Proposal.pdf`, section 8 | [x] |

> The proposal was written quickly to convey the idea and does not bind the work. Requirements are
> written properly in `docs/requirements/`. Where the delivered scope diverges from the proposal,
> the design document says so explicitly rather than quietly.

## Design document — 5 marks (due 11 Sep 2026)

| Criterion | Marks | Evidence | State |
|---|---|---|---|
| Architecture note identifies modules and interfaces | 2 | [design-doc.pdf](design-doc.pdf) §1; [../architecture/overview.md](../architecture/overview.md), [../ai/architecture-rules.md](../ai/architecture-rules.md), `ModularityTest` | [~] |
| Test plan lists at least one test per module | 1 | [design-doc.pdf](design-doc.pdf) §2 — ten slices, one named test each | [x] |
| Milestone plan revised in light of scoping feedback | 1 | [design-doc.pdf](design-doc.pdf) §3; [scoping-feedback.md](scoping-feedback.md) — the feedback itself; [proposal.tex](proposal.tex) §8 — the revision | [x] |
| Risks and plan B are honest, not boilerplate | 1 | [design-doc.pdf](design-doc.pdf) §4 — five risks, each with the signal that would fire | [x] |

**What earns the two architecture marks here:** slices are real packages with a boundary a test
enforces, not boxes in a diagram. Spring Modulith generates the module diagram and canvas from the
code, so what the document shows is what the code is.

**Why that row is `[~]` and not `[x]`, as of 11 September.** Half of it now holds and half does not,
and splitting the difference is more useful than claiming either. What holds: the eleven modules are
declared as packages, `ModularityTest` verifies them, and a second assertion added on 10 September
stops that test passing on an application with no modules at all — which is what it had been doing
since the skeleton was created. What does not: the packages are empty, so no *interface between* two
slices exists yet to be described or enforced. The C4 level-3 figure is generated from
`docs/diagrams/src/`, not from the code by Modulith's `Documenter`, because Modulith would currently
draw eleven boxes and no arrows. The row goes to `[x]` when the first two slices talk to each other
through a published type, in phase 2 or 3, and the diagram can be regenerated from the code.

## Mid-demo — 5 marks (due 9 Oct 2026)

| Criterion | Marks | Evidence | State |
|---|---|---|---|
| Core workflow runs end to end on at least one real input | 2 | [mid-demo.md](mid-demo.md), the demo script | [ ] |
| Git history shows both members contributing across weeks | 1 | [../team/weekly-log/](../team/weekly-log/), `git log` | [ ] |
| Gap list is honest | 1 | [../gap-list.md](../gap-list.md) | [ ] |
| You can explain any part of your code on the spot | 1 | [../viva/](../viva/), [../ai/journal/](../ai/journal/) | [ ] |

**The demo scenario, in one sentence:** a student opens the landing page, searches for "FRRO
registration", reads the article, follows a wiki link to a related one, proposes an edit with a
photo of the form — the OGE moderator approves it in the panel and the change is live.

**On the gap list:** hiding a known gap is explicitly worse than declaring it. The list is generated
from requirement and feature status, so it cannot be quietly trimmed before the demo.

## Final demo and viva — 10 marks (due 6 Nov 2026)

| Criterion | Marks | Evidence | State |
|---|---|---|---|
| Stakeholder-executed demo on their real input | 3 | [final.md](final.md), the recording, [../stakeholder/](../stakeholder/) | [ ] |
| Verification evidence — tests passing, edge cases addressed | 2 | [../verification/](../verification/), CI runs | [ ] |
| Individual viva on your own modules | 3 | [../team/ownership.md](../team/ownership.md), [../viva/](../viva/) | [ ] |
| Individual viva on your partner's modules | 1 | [../viva/](../viva/) | [ ] |
| Handoff artefacts are usable | 1 | [../handoff/](../handoff/) | [ ] |

**The question the final demo actually asks** is not "is it hosted" but "can the stakeholder use it
without you sitting next to them". So the handoff package has to stand on its own: a compose file, a
one-command start, an admin guide written for someone non-technical, and a way to back up and
restore the content.

**On the viva:** since we take tasks freely rather than owning fixed areas, both of us prepare on
every slice. `ownership.md` tells us which ones we are most likely to be asked about first.

## Anti-freeloading mechanisms (course requirements, section 10)

| Mechanism | Where it lives |
|---|---|
| Named module ownership | [../team/ownership.md](../team/ownership.md) — generated from git |
| Git evidence across the semester | The history itself |
| Individual viva | [../viva/](../viva/) |
| Peer contribution split | Submitted privately at the final stage |
| Weekly contribution log | [../team/weekly-log/](../team/weekly-log/) |

## AI-usage statement

The proposal commits us to being able to explain any line we submit. The evidence is
[../ai/journal/](../ai/journal/): prompts, outcomes, and the edits we made by hand afterwards,
recorded by hooks as the work happened rather than reconstructed at the end.
