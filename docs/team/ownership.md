# Slice ownership

Who has actually done more in each slice, measured from git history rather than assigned in advance.

**Generated** by `java -jar tools/target/ai-tools.jar ownership`. An edit made here is lost on the next run.

A slice's commits are the authored commits that touch its code, its tests or its templates, and
the migrations count for `shared`. "More work" is the larger number of commits; lines are shown
beside them and do not decide it. The journal commits the `Stop` hook writes are never counted as
authorship: they are reported in their own column below and never added to the authored one.

## Per slice

| Slice | mikhail commits | mikhail lines | abdirakhim commits | abdirakhim lines | More work |
|---|---|---|---|---|---|
| articleview | 1 | 12 | 1 | 453 | even |
| backup | 1 | 12 | 1 | 1127 | even |
| contribute | 1 | 12 | 0 | 0 | mikhail |
| home | 1 | 12 | 1 | 474 | even |
| media | 1 | 12 | 0 | 0 | mikhail |
| moderate | 1 | 12 | 0 | 0 | mikhail |
| report | 1 | 12 | 0 | 0 | mikhail |
| search | 1 | 12 | 0 | 0 | mikhail |
| shared | 2 | 26 | 6 | 1190 | abdirakhim |
| taxonomy | 1 | 12 | 0 | 0 | mikhail |
| wikilink | 1 | 12 | 1 | 684 | even |

## All authored work, and the hook's

| Member | Authored commits | Journal commits (Stop hook) |
|---|---|---|
| mikhail | 114 | 67 |
| abdirakhim | 40 | 271 |
