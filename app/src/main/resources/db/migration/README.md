Flyway migrations. Each carries `-- trace: FR-XXX` in its header and is checked in both
directions. The schema freezes at the end of phase 2; after that it changes by agreement.
See docs/architecture/data-model.md.
