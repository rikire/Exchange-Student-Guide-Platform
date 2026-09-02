# CS5013 Project — guide and rules

**Programming with AI**  
**Instructor:** V. Krishna Nandivada  
**Weight:** 25% of the course grade

---

## Contents

- [CS5013 Project — guide and rules](#cs5013-project--guide-and-rules)
  - [Contents](#contents)
  - [1. Overview](#1-overview)
  - [2. What kinds of projects qualify](#2-what-kinds-of-projects-qualify)
  - [3. How to size your project](#3-how-to-size-your-project)
    - [Signals it is *too small*](#signals-it-is-too-small)
    - [Signals it is *too large*](#signals-it-is-too-large)
    - [A useful anchor](#a-useful-anchor)
  - [4. How to deliver the tool](#4-how-to-deliver-the-tool)
  - [5. Finding your IITM customer](#5-finding-your-iitm-customer)
  - [6. What makes a project unsuitable](#6-what-makes-a-project-unsuitable)
  - [7. Group and individual rules](#7-group-and-individual-rules)
  - [8. Evaluation stages](#8-evaluation-stages)
  - [9. Rubric](#9-rubric)
    - [Proposal (5 marks)](#proposal-5-marks)
    - [Design (5 marks)](#design-5-marks)
    - [Mid‑demo (5 marks)](#middemo-5-marks)
    - [Final demo + viva (10 marks)](#final-demo--viva-10-marks)
  - [10. Contribution and freeloader prevention](#10-contribution-and-freeloader-prevention)
  - [11. Timeline](#11-timeline)
  - [12. Proposal submission template](#12-proposal-submission-template)

---

## 1. Overview

The project is **25% of your grade**. It is the part of the course where you stop practising on toy problems and build something a real person will actually use.

> **Ground rule:** every project must be of concrete use to a specific IITM faculty member, research group, centre, lab, hostel, office, or department.

No generic "todo app", no yet-another-Kaggle-notebook, no clone of an existing SaaS product with no user attached. The reason is not bureaucratic: verification, scope negotiation, and "did this actually work" are the hard parts of software, and you only learn them when there is a real user on the other side who can tell you the answer is wrong.

---

## 2. What kinds of projects qualify

Anything that a specific person at IITM would install, run, or click on. A non-exhaustive menu:

- **Admin / office automation** — a script or small web tool that turns a recurring manual task (attendance consolidation, form collation, roster reconciliation, seat allocation, duty rostering) into a one-click operation for a specific office.
- **Teaching aids** — a grader, a plagiarism-neighbour checker, a quiz generator, a lecture-transcript search tool, an auto-marker for a specific course's assignments — built for a specific instructor.
- **Research-lab tooling** — a data-ingestion pipeline, an experiment tracker, a small dashboard, a preprocessing utility, a lab-specific labelling interface — built for a specific PI or group.
- **Student-service utilities** — a hostel-mess feedback aggregator, a library-availability notifier, a shuttle-tracker, a lost-and-found board — built for a specific hostel, office, or student body that has agreed to use it.
- **Data pipelines / small internal APIs** — a scraper-plus-cleaner that produces a dataset a research group actually wants, or a REST endpoint that a lab's existing tool can call.

If your idea does not fit any of the above, that is fine — the categories are examples, not a menu. What matters is the shape: one named IITM stakeholder, one concrete problem, one deliverable they will use.

---

## 3. How to size your project

The scope should fit one semester of part-time work by a team of one or two students, alongside your other courses. Here is how to check whether your idea is roughly the right size.

### Signals it is *too small*
- The stakeholder could do it themselves in an afternoon.
- There is no verification step because the output is trivially correct.
- You could finish it in one week and would spend the rest of the semester polishing the README.

### Signals it is *too large*
- You cannot describe the mid-demo scenario in one sentence.
- It needs three or more external systems to be integrated before anything works.
- The stakeholder's own description of what they want is longer than one paragraph.
- You need data, access, or approvals you do not have on day one and cannot obtain in the first two weeks.

### A useful anchor
A well‑sized project has a core workflow with roughly **three to five steps** — something you can list on a single slide — and, once deployed, gives the stakeholder back a repeating chunk of time or removes a recurring source of manual error. If you cannot state the "before" and "after" for the stakeholder's week in one line each, the scope is not yet clear enough.

> **Scope with the stakeholder, not alone.** They know their own workflow better than you do. If they say "honestly, just this one part would already help me a lot," listen to them — that is your mid-demo scope. You can always add more later if time allows; you cannot easily shrink an over-promised proposal.

---

## 4. How to deliver the tool

Hosted is one option, not a requirement. What matters is that the stakeholder can actually run it after you hand it over.

- **A script or CLI tool** — a repo, a one‑line install, and a README with 3 example invocations. Fine for office staff comfortable on a terminal, or for a PhD student who will run it themselves.
- **A desktop / local app** — a small Tkinter, Electron, or Java‑Swing GUI that runs on the stakeholder's own machine. Good when the data is sensitive and cannot leave their laptop.
- **A spreadsheet plug‑in / macro / Google‑Apps‑Script** — if the workflow already lives in Excel or Google Sheets, meeting them where they are is often the highest‑impact choice.
- **A hosted web app** — the classic option. Only worth it if the stakeholder actually needs multi‑user or remote access. IITM has internal hosting (department servers, CC), or you can use a free tier (Render, Fly, Vercel) with the stakeholder's consent.
- **A scheduled job** — a cron on their machine or a GitHub Action that runs weekly and emails a report. Underrated for "recurring manual task" projects.
- **An integration** — a plugin, extension, or webhook into a tool they already use (Moodle, Google Calendar, Slack, a lab notebook).

The rubric‑relevant question is not "is it hosted" but **"can the stakeholder use it without you sitting next to them?"** That is what the final demo will check.

---

## 5. Finding your IITM customer

You are looking for one person who will say **"yes, if you build this I will use it."** Not "sounds cool" — actually use it.

**Where to look:**

- Your own department's office staff — they run recurring manual processes.
- A faculty member whose course you have taken — ask if any part of their grading or lab admin could be automated.
- A research lab you know someone in — ask the PhD students what one hour of their week they wish a script would take back.
- A centre (CFI, IC&SR, YRC, Shaastra, hostel affairs, library) — they run operational workflows and are usually open to student projects.

**Approach script:**

> "I am taking CS5013 this semester and the course project asks us to build something a specific IITM stakeholder will actually use. Do you have a recurring task, a small tool you wish existed, or a manual workflow that we could try to automate for you? If we build it we will need about 20 minutes of your time twice — once to confirm the scope, and once at the end to check it works for you."

Get their agreement **in writing** (an email is fine) and attach it to your proposal. This is the stakeholder acknowledgement that the rubric asks for.

---

## 6. What makes a project unsuitable

- ❌ No real user. "I will build X and maybe someone will find it useful" — no.
- ❌ Purely academic re‑implementation of a paper with no downstream user.
- ❌ Needs data you cannot legally get — student records, exam scripts, medical data, salary information. If you cannot access it in week 2 you will not be able to demo in week 12.
- ❌ Scope you cannot finish. A full LMS clone, a research‑grade ML model, a production‑grade mobile app — not in one semester with 25% weight.
- ❌ Something the stakeholder already has. Ask before you propose.
- ❌ Something an existing tool already does well enough. Before you propose, spend 30 minutes searching for existing solutions — commercial, open‑source, or a workflow your stakeholder already has. If one of them fits, use it and pick a different project. The proposal's "prior work" section will ask you to name what you found and why it does not suffice.
- ❌ Something that is only interesting if the AI‑generated code works. The AI assistant is a tool; the project must have value even if the generation loop had failed and you had to write everything yourself.

---

## 7. Group and individual rules

- You may work **solo** or in a **team of two**. No teams of three.
- A team of two is expected to deliver more than a solo project. Scope is the negotiated part; effort per person is not.
- In a team of two, both members must own roughly equivalent modules. "One person does everything and the other tests" is not acceptable.
- Team composition is locked at proposal submission. Splits after that require instructor approval and usually result in scope reduction.
- Both members must be visible in the git history throughout the semester. Long gaps for one member will be flagged at every stage.

---

## 8. Evaluation stages

The **25 marks** are split across four stages. You cannot skip a stage and make it up later.

| Stage | Marks | What is submitted / demonstrated |
|-------|-------|----------------------------------|
| **Proposal** | 5 | Written proposal (see template below) + stakeholder acknowledgement email attached. |
| **Design** | 5 | Architecture note, module split, test plan, milestone plan. Short (2–4 pages). |
| **Mid‑demo** | 5 | Working core workflow, live in front of the evaluator. Git history evidence from both members. Honest gap‑list of what is not done. |
| **Final demo + viva** | 10 | Stakeholder‑executed demo (the stakeholder runs it in front of the evaluator, or a video shows them doing so). Verification results. Individual viva — each student answers questions on their own modules and on their partner's. |

---

## 9. Rubric

The rubric grades your **process** — not your stakeholder's satisfaction. You will not be penalised if the office decides not to adopt your tool for reasons outside your control. But having a real user makes the process itself concrete: scope is negotiated with someone who can push back, "does it work" has a specific answer, and edge cases are the ones that actually occur.

### Proposal (5 marks)

| Criteria | Marks |
|----------|-------|
| Realism of scope for one semester | 1 |
| Quality of prior‑work section (specifics, not prose) | 1 |
| Stakeholder acknowledgement present and credible | 1 |
| Verification plan is concrete (fixtures, tests, acceptance criteria) | 1 |
| Milestone plan is coherent | 1 |

### Design (5 marks)

| Criteria | Marks |
|----------|-------|
| Architecture note clearly identifies modules and interfaces | 2 |
| Test plan lists at least one test per module | 1 |
| Milestone plan revised in light of scoping feedback | 1 |
| Risks and plan‑B are honest, not boilerplate | 1 |

### Mid‑demo (5 marks)

| Criteria | Marks |
|----------|-------|
| Core workflow runs end‑to‑end on at least one real input | 2 |
| Git history shows both members contributing across weeks | 1 |
| Gap‑list is honest — hiding known gaps is worse than declaring them | 1 |
| You can explain any part of your code on the spot | 1 |

### Final demo + viva (10 marks)

| Criteria | Marks |
|----------|-------|
| Stakeholder‑executed demo works on their real input | 3 |
| Verification evidence (tests passing, edge cases addressed) | 2 |
| Individual viva — each student on their own modules | 3 |
| Individual viva — each student on the partner's modules | 1 |
| Handoff artefacts (README, install steps, contact) are usable | 1 |

---

## 10. Contribution and freeloader prevention

Five mechanisms, layered. None of them alone is decisive; together they make freeloading very hard to hide.

1. **Named module ownership.** Every module in the repo has a single named owner. In a team of two, ownership must be roughly balanced.
2. **Git evidence.** Both members must have commits throughout the semester. A commit dump in the last week from one member is a red flag.
3. **Individual viva at final.** Each student answers questions on their own modules and on their partner's. If one member cannot explain what the other built, the mark is adjusted.
4. **Peer contribution split.** At final submission, each student submits a private percentage split (e.g., "60/40 in my favour"). If the two students' splits disagree by more than 20 percentage points, the instructor investigates.
5. **Weekly contribution log.** Each student maintains a brief weekly log (one paragraph) of what they did that week. Submitted with each stage.

---

## 11. Timeline

Five phases, spread across the semester. Every deadline is a **Friday**. Submissions are due by **23:59 IST** on the listed date.

| Phase | Date | Milestone |
|-------|------|-----------|
| **Phase 1** | Fri, 31 Jul 2026 | This document released. Start looking for a stakeholder. Team formation begins. |
| **Phase 2** | Fri, 21 Aug 2026 | **Proposal due** (5 marks). Team locked. Stakeholder acknowledgement email attached. |
| *Aug 22 – Sep 10* | — | Proposal feedback returned. Scoping revisions if flagged. Start scaffolding. |
| **Phase 3** | Fri, 11 Sep 2026 | **Design doc due** (5 marks). Architecture, module split, test plan, revised milestone plan. |
| *Sep 12 – Oct 8* | — | Main build. Weekly contribution log continues. |
| **Phase 4** | Fri, 9 Oct 2026 | **Mid‑demo** (5 marks). Working core workflow. Both members present. Stakeholder invited (optional at this stage). |
| *Oct 10 – Nov 5* | — | Finish, harden, stakeholder dry‑run, hand off. |
| **Phase 5** | Fri, 6 Nov 2026 | **Final submission + viva** (10 marks). Stakeholder‑executed demo (present or video). Individual viva. |

---

## 12. Proposal submission template

Submit as a single **PDF**, 3–5 pages. Attach the stakeholder acknowledgement email as an appendix.

> LaTeX template: `proposal-template.tex` · `proposal-template.pdf`  
> Download the `.tex`, fill in the bracketed placeholders, and compile with `pdflatex`. Uses only the `article` class plus `geometry`, `hyperref`, `enumitem`, and `parskip` — no other dependencies.

The template has **nine sections** and one appendix:

1. **Team information**  
   Team ID (or "solo"), names, roll numbers, department, primary contact email, GitHub handles.

2. **Problem statement**  
   3–4 sentences: what problem, whose problem, what the current pain looks like. No marketing language.

3. **Users and stakeholder**  
   Named stakeholder (person, role, department / centre / lab). One paragraph on how you met them and what they said. Attach their acknowledgement email in the appendix.

4. **Prior work and why it does not suffice**  
   List at least three existing solutions your stakeholder could plausibly use instead — commercial products, open‑source tools, in‑house scripts, or manual workflows. For each, give the name and link, what it does that overlaps with your proposal, and the specific reason it is not adopted (cost, missing feature, wrong data model, licensing, offline‑only, cannot integrate, etc.). "There is nothing like this" is almost never true and will be marked down.

5. **Tech stack**  
   Language is **Java** (course policy). List your chosen framework or build system (plain `javac`, Maven, Gradle, Spring Boot, Java‑Swing, etc.), key libraries, and where the tool will run (stakeholder's laptop, department server, cron job, etc.).

6. **AI‑usage statement**  
   A short standard paragraph acknowledging that you will use AI coding assistants during the project and will be able to explain any part of the code you submit. The LaTeX template contains suggested wording you can copy verbatim.

7. **Verification plan**  
   What tests you will run, on what fixtures, and what user‑visible acceptance criteria the stakeholder will use to say "yes, this works." Be specific: *"processes the July 2026 attendance sheet without manual correction"* beats *"produces correct output."*

8. **Milestone plan**  
   A three‑line sketch: what will exist by the design doc, what will exist at mid‑demo, what will exist at final. You are not expected to plan week‑by‑week at proposal time — you will refine this in the design doc.

9. **Risks and plan B**  
   Three things that could derail this. For each, a one‑sentence fallback that keeps the project alive at a smaller scope.

**Appendix A.** Paste the stakeholder acknowledgement email verbatim, with sender name, role, and date.

---

*End of document*