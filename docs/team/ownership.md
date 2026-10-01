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
| articleview | 3 | 28 | 8 | 530 | abdirakhim |
| backlink | 0 | 0 | 3 | 553 | abdirakhim |
| backup | 3 | 92 | 4 | 1155 | abdirakhim |
| contribute | 4 | 1032 | 10 | 1446 | abdirakhim |
| home | 1 | 12 | 5 | 579 | abdirakhim |
| media | 2 | 89 | 10 | 1470 | abdirakhim |
| moderate | 1 | 12 | 18 | 2363 | abdirakhim |
| report | 1 | 12 | 0 | 0 | mikhail |
| search | 1 | 12 | 5 | 715 | abdirakhim |
| shared | 5 | 130 | 18 | 1916 | abdirakhim |
| taxonomy | 2 | 230 | 3 | 612 | abdirakhim |
| wikilink | 1 | 12 | 4 | 861 | abdirakhim |

## All authored work, and the hook's

| Member | Authored commits | Journal commits (Stop hook) |
|---|---|---|
| mikhail | 141 | 117 |
| abdirakhim | 162 | 587 |
