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
| about | 0 | 0 | 1 | 284 | abdirakhim |
| articleview | 5 | 661 | 19 | 890 | abdirakhim |
| backlink | 0 | 0 | 3 | 553 | abdirakhim |
| backup | 6 | 268 | 7 | 1283 | abdirakhim |
| contribute | 14 | 2153 | 19 | 1761 | abdirakhim |
| home | 3 | 41 | 10 | 701 | abdirakhim |
| media | 5 | 592 | 13 | 1537 | abdirakhim |
| moderate | 7 | 625 | 27 | 3666 | abdirakhim |
| report | 1 | 12 | 3 | 647 | abdirakhim |
| search | 3 | 57 | 13 | 1236 | abdirakhim |
| shared | 12 | 363 | 29 | 2207 | abdirakhim |
| taxonomy | 6 | 339 | 6 | 760 | even |
| wikilink | 2 | 108 | 7 | 1091 | abdirakhim |

## All authored work, and the hook's

| Member | Authored commits | Journal commits (Stop hook) |
|---|---|---|
| mikhail | 179 | 172 |
| abdirakhim | 255 | 779 |
