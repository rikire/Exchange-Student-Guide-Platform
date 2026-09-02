---
description: Start a seed article draft in the target format
argument-hint: <topic>
---

Start a draft article about: **$ARGUMENTS**

Articles are written directly in the format the importer reads, so that seeding, backup, transfer
and test fixtures all use one mechanism.

1. Create `app/src/main/resources/data/seed/<kebab-slug>.md` with YAML front matter: title, tags,
   author, dates. Match the shape of the files already there.
2. Write for someone who landed in Chennai three days ago and knows nothing: concrete steps, real
   places, actual costs, what goes wrong and what to do about it. No marketing tone.
3. Link related articles with `[[Title]]`. A link to an article that does not exist yet is fine —
   it renders as a red link and marks something worth writing.
4. Anything you cannot verify — an office's opening hours, a fee, a phone number — mark clearly
   rather than inventing it. A confidently wrong opening time sends someone across campus for
   nothing.
5. Tell me which facts need checking with OGE before this goes in front of students.
