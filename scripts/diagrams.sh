#!/bin/sh
# Renders docs/diagrams/src/*.puml into docs/diagrams/out/.
#
# PlantUML is fetched rather than depended on. Its main artifact is GPL and this repository is MIT,
# so the jar is downloaded into a gitignored cache and run as a separate process — invoked, not
# linked. Nothing is added to a pom.
#
# The version and its checksum are pinned. A jar that does not match the checksum is not run: an
# unverified binary that renders our architecture diagrams is a supply-chain problem wearing a
# convenience.
set -e

cd "$(dirname "$0")/.."

VERSION="1.2026.8"
SHA256="5e1ecfa8ecd32c90b03bbf3b1eb6f020943f98ab0fcf4032be31a0002ee2c462"
URL="https://github.com/plantuml/plantuml/releases/download/v${VERSION}/plantuml-${VERSION}.jar"
JAR=".cache/plantuml-${VERSION}.jar"

SRC="docs/diagrams/src"
OUT="docs/diagrams/out"

# Verified on 10 September 2026 by downloading it: the jar reports 1.2026.8, and its bundled C4
# standard library resolves offline, so rendering needs no network once the jar is cached.

checksum_of() {
	if command -v sha256sum >/dev/null 2>&1; then
		sha256sum "$1" | cut -d' ' -f1
	elif command -v shasum >/dev/null 2>&1; then
		shasum -a 256 "$1" | cut -d' ' -f1
	else
		echo "no sha256sum or shasum on PATH; cannot verify $1" >&2
		exit 1
	fi
}

if [ ! -f "$JAR" ]; then
	echo "==> fetching PlantUML $VERSION"
	mkdir -p .cache
	curl -sSL --fail -o "$JAR.part" "$URL"
	mv "$JAR.part" "$JAR"
fi

echo "==> verifying $JAR"
actual=$(checksum_of "$JAR")
if [ "$actual" != "$SHA256" ]; then
	echo "Checksum mismatch for $JAR" >&2
	echo "  expected $SHA256" >&2
	echo "  actual   $actual" >&2
	echo "Delete it and re-run to fetch again. If it still differs, do not run it." >&2
	exit 1
fi

if [ ! -d "$SRC" ] || [ -z "$(find "$SRC" -name '*.puml' -print -quit)" ]; then
	echo "No .puml sources in $SRC" >&2
	exit 1
fi

# -failfast2 turns a diagram-level error into a non-zero exit. Without it PlantUML writes an image
# of the error message and reports success, which is the failure mode this script exists to avoid:
# a green run and a picture of a stack trace committed as architecture.
#
# Two formats. SVG is what the architecture documents embed, because a forge previews it and it stays
# sharp. PNG exists for LaTeX: pdflatex cannot embed SVG, and converting one needs inkscape or
# rsvg-convert, neither of which is installed here. PlantUML already has the diagram in memory, so
# asking it for both costs one flag instead of a second tool in the chain. Every diagram renders both
# ways rather than only the one the design document happens to embed today; a per-diagram exception
# is the kind of rule that stops being true without anyone noticing.
echo "==> rendering"
mkdir -p "$OUT"
java -jar "$JAR" -failfast2 -tsvg -o "$(cd "$OUT" && pwd)" "$SRC"/*.puml
java -jar "$JAR" -failfast2 -tpng -o "$(cd "$OUT" && pwd)" "$SRC"/*.puml

# PlantUML writes a .cmapx beside any PNG whose diagram carries a link or a tooltip: an HTML
# client-side image map. Nothing here serves HTML, and LaTeX cannot use one, so it would be a
# generated file committed for no reader. Removed rather than left to accumulate.
rm -f "$OUT"/*.cmapx

# This script used to prepend a GENERATED marker to each SVG. Dropped on 10 September along with the
# convention itself: nothing ever read the marker, and the one thing that touched it was the loop
# here checking it had not already added one. The reasoning is in docs/ai/docs-sync.md. Anyone
# wondering whether to edit docs/diagrams/out by hand has a better answer than a comment in the file:
# rerun this script and watch the edit disappear.

echo
echo "Rendered into $OUT:"
ls -1 "$OUT"
