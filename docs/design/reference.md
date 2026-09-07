# Design reference

Palette, typography and patterns taken from the IITM sites — fetched directly (the OGE logo's
colors read from its actual inline SVG, not guessed from a screenshot), not invented.

## Logo

The OGE logo is an inline SVG on [ge.iitm.ac.in](https://ge.iitm.ac.in/), not a raster file — saved
as [assets/oge-logo.svg](assets/oge-logo.svg) for reuse in the design system and mockups.

## Colors

**OGE brand** (read from the logo SVG's own fill values):

| Color | Hex | Use in the logo |
|---|---|---|
| Maroon | `#781F19` | Outer circle |
| Gold | `#D6A64F` | Inner circle |
| Gold (dark) | `#BDA345` | Circle detail |
| Red (bright) | `#D9261C` | Accent detail |
| Near-black | `#1F1917` | Fine linework |
| White | `#FFFFFF` | Text/detail on the circle |

**IITM institutional** (from the main site, [iitm.ac.in](https://www.iitm.ac.in/) — qualitative,
no inline hex captured):

- Primary: deep navy/blue (header, navigation)
- Accent: orange/saffron
- Neutral: white background, light-gray section dividers and card backgrounds

**Recommendation:** lead with the OGE maroon/gold pair for anything OGE-branded (this product's
header, primary buttons), since that's the stakeholder's own identity — not the wider institute's
navy/saffron, which belongs to IITM generally rather than to OGE specifically.

## Typography

Clean sans-serif throughout both sites, sized for digital readability. The IITM main site exposes
explicit font-size controls (Increase / Standard / Decrease) in its header — an accessibility
pattern worth carrying into this product's own settings, separately from CON-005 (the interface
stays single-language; font-size choice is not a translation).

## Layout patterns observed

- **Card-based sections** with consistent padding and (on IITM) hover effects — the natural pattern
  for an article list, search results, and the landing page's recently-added/pinned sections.
- **Logo top-left, horizontal nav**, consistent on both sites.
- **Event/announcement cards** on the OGE site (title, category tag, date) — a close visual match
  for how a tag and a "recently added" date should read on our landing page.
- **Generous whitespace**, formal-but-approachable tone on both sites.
- IITM's language toggle (Tamil/Hindi/English) is a real precedent for **NFR-003** (mixed-script
  content survival) — evidence the content itself needs to handle these scripts, not a reason to
  add a multilingual UI (already ruled out, CON-005).

## Sources

Fetched directly, 7 Sep 2026: [https://ge.iitm.ac.in/](https://ge.iitm.ac.in/) (raw HTML, for the
logo SVG and colors) and [https://www.iitm.ac.in/](https://www.iitm.ac.in/) (rendered summary, for
the general institutional palette and layout).
