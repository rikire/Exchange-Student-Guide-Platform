PlantUML sources: C4 levels 1-3 and the ERD. Written in phase 1.

Rendered output goes to [`../out/`](../out/). It is **generated and must not be edited by hand** —
change the `.puml` source and re-run `scripts/diagrams.sh`.

It **is** committed, unlike most generated output. The `.gitignore` carries an explicit exception
for it, because `docs/architecture/overview.md` and `data-model.md` embed these files as images and
the forge previews them: a rendered diagram that is not in the repository shows as a broken image in
both. Editing an `.svg` by hand would therefore be invisible until the next render silently
discarded it, which is why the rule above matters more here than for output nobody reads.

`scripts/diagrams.sh` fetches a pinned, checksummed PlantUML into a gitignored cache and runs it as
a separate process. Nothing is added to a pom — PlantUML's artifact is GPL and this repository is
MIT.

**`c4-component-actual` has no source here.** It is the diagram the code draws of itself:
`ModularityTest` asks Spring Modulith's `Documenter` to write it under `app/target/`, and
`scripts/diagrams.sh` renders it beside `c4-component`, which is the designed one. Run `./mvnw test`
before the script, which stops with a message when the file is missing. The two are meant to be
compared: a dashed box or arrow in `c4-component` is designed and not built yet.
