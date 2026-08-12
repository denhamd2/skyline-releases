# Sky design language — research notes & app mapping

Research into how Sky designs its **Sky Go** (mobile) and **Sky Q / Sky Glass**
(TV) interfaces, with close attention to **motion / micro-animation**, plus the
official design principles that are publicly documented. The last section maps
each principle to how our Sky-Go-inspired app implements it.

> Caveat on sources: Sky's full design system (**Sky UI**) and web **Toolkit**
> live behind Sky login/VPN, so several primary pages return 403. Where a page
> couldn't be fetched directly, the summary is drawn from its search-indexed
> description and from public design-press coverage and reviews. Everything is
> cited at the bottom.

---

## 1. There is a real, official Sky design system

Sky Digital maintains **Sky UI** — a component + Figma design system with
documentation and usage guidelines. It's organised the way most mature systems
are:

- **Foundations (tokens):** the core values that drive everything — **colour,
  typography, spacing, gradients, easings**.
- **Components:** the smallest UI building blocks (buttons, text, cards…).
- **Sections:** preset combinations of components for common page types, to
  speed up builds.

Separately, Sky open-sourced an earlier CSS/SCSS system, the **Sky Toolkit**
(`sky-uk/toolkit` on GitHub), which is where the concrete, public tokens live.
Sky's in-house agency **Sky Creative** owns the brand expression and pushes a
deliberately "premium" look. Sky ran a significant **rebrand in Nov 2025**
(e.g. sub-brand gradients for TV/Mobile/Broadband were retired).

---

## 2. Typography

- Two brand families: **Sky Text** (body/UI) and **Sky Headline** (headings).
  Sky Sports uses its own **Sky Sports Sans** (F37 foundry, 5 weights). These
  are **proprietary** — they can't be redistributed in a third-party app.
- The Toolkit defines a **mobile-first, responsive type scale** as *classes*
  rather than against HTML tags, deliberately separating **semantic** meaning
  from **stylistic** size:
  - Headings: `heading-alpha`, `heading-bravo`, `heading-charlie`, … (descending
    size).
  - Text: `text-lead` (intro), `text-body` (default), `text-smallprint`
    (captions/legal).
- Takeaway: a small, disciplined scale with a clear lead/body/caption hierarchy;
  headings in a heavier display face, body in a highly legible text face.

## 3. Colour & gradients

- A deep, near-black/navy canvas with **Sky blue** as the action colour, red for
  live, and restrained use of brand-blue accents.
- **Gradients are a foundation** with explicit rules: **never on typography or
  icons**; only on larger illustration/hero surfaces. Sub-brand gradients were
  removed in the 2025 rebrand — i.e. gradients are used sparingly and
  purposefully, not as decoration everywhere.

## 4. Layout, spacing, shape

- 8-pt-style spacing scale, generous gutters, and **rounded corners** throughout
  (cards, tiles, pill buttons) — nothing sharp. Content is organised into
  horizontally-scrolling **rails** of cards under section headers, with a large
  cinematic **hero** at the top of the landing screen.

---

## 5. Motion & micro-animation (the important part)

Sky UI has dedicated **Motion** and **Easings** foundations. The documented
principles:

- **Easing = natural acceleration/deceleration.** Real objects don't move at
  constant speed; UI motion shouldn't either. Linear is avoided in favour of
  eased curves.
- **Duration is proportional to the change.** Short durations for small changes
  (a toggle, a focus highlight); longer durations for large moves (a full screen
  transition). Fast enough never to feel like waiting.
- **Enter vs exit asymmetry:**
  - Elements **entering** use **ease-out** — they arrive quickly and *settle*.
  - Elements **exiting** use **ease-in** — they start slow and *leave fast*.
- **Choreography (staggering):** when several elements animate together, they are
  choreographed rather than moving all at once. The signature Sky pattern is a
  **"Fade In & Slide Up"**, **staggered** diagonally — starting top-left and
  finishing bottom-right — to guide the eye through the transition and avoid
  overwhelming the viewer.
- **Motion is functional, not decorative:** it communicates state change, spatial
  relationship, and the result of an action (a thing sliding out means "gone").

### On the TV (Sky Q / Sky Glass) specifically
Reviews consistently single out the **motion quality** of the 10-foot UI:

- **Tile-based** layout (Netflix-like): a **menu on the left, live preview on the
  right**; big high-resolution imagery.
- **Slick transition animations with rarely any lag**, "smooth visual flourishes
  when changing between screens," crisp text — Sky Glass is a redesign that
  *iterates on* Sky Q and is praised for speed + cohesiveness.
- **Focus-driven motion** is the core interaction: the focused tile scales up
  slightly and lifts, artwork/metadata animate in, and the row eases to keep the
  focused item in view. (This is the standard 10-foot pattern Sky follows; a
  10-foot UI uses oversized text/'controls for a ~3 m viewing distance.)

---

## 6. How our app applies these principles

| Sky principle | Our implementation |
|---|---|
| Sky Text / Sky Headline (proprietary) | **Manrope** (SIL Open Font License) as the closest legal substitute; two-weight "sky go" lockup |
| Small semantic type scale (lead/body/smallprint) | Material 3 typography mapped to `TextPrimary` / `TextSecondary` / `TextMuted` tiers (`ui/theme/Theme.kt`) |
| Near-black/navy canvas, Sky-blue action, red live | `SkyPalette` — Canvas `#05070A`, Accent `#0B69F5`, LiveRed `#E11D3F`; TV uses a subtle navy→black **gradient** background |
| Gradients sparing, never on type/icons | Gradients only on heroes, fallback backdrops, the TV nav underline, and card washes — never behind text/icons |
| Rounded, 8-pt, rails + hero | `SkyRadius`/`SkySpacing` tokens; horizontally-scrolling rails; cinematic hero on Home |
| Enter = ease-out, exit = ease-in | Compose default enter/exit + crossfade on image loads; press-scale on cards/buttons |
| Staggered "fade in & slide up" | **Opportunity to improve** — we do per-item crossfade but not the diagonal staggered reveal; worth adding to rails/screen transitions |
| 10-foot focus motion (scale + lift) | We currently use a **white outline only** on focus (the scale was removed because it caused row jump). A *layout-stable* subtle scale/elevation could be reintroduced to better match Sky |
| Slick screen transitions on TV | **Opportunity** — tab switches are instant; adding a short fade/slide would match Sky Q/Glass polish |

### Suggested next motion touches (if we want to get closer)
1. **Staggered rail reveal** — when a screen loads, fade+slide-up its rails
   diagonally (≈40–60 ms stagger) rather than showing them all at once.
2. **Layout-stable focus pop on TV** — reintroduce a subtle scale (≈1.04) via a
   graphics-layer transform that doesn't reflow siblings, plus a soft elevation
   shadow, alongside the existing white outline.
3. **Screen-transition crossfade** on both phone nav and TV tab changes (~200 ms
   ease-out in, ease-in out).
4. **Focus/press feedback** everywhere: 0.97 press-scale, ease-out ~120 ms.

---

## Sources
- Sky UI Design System — Motion: http://sky-ui.cf.sky.com/guides/motion/
- Sky UI Design System — Easings: http://sky-ui.cf.sky.com/foundations/easings
- Sky UI Design System — Gradients: https://sky-ui.cf.sky.com/foundations/gradients
- Sky UI Design System — Typography: http://sky-ui.cf.sky.com/foundations/typography/
- Sky Toolkit (open source) — typography SCSS: https://github.com/sky-uk/toolkit/blob/develop/packages/sky-toolkit-ui/components/_typography.scss
- Sky Creative (brand design): https://creative.sky/list/brand-design/
- Sky Sports Sans / F37 rebrand: https://www.designweek.co.uk/type-led-sky-sports-redesign-aims-to-put-the-love-back-into-the-brand/
- Sky Glass review (interface/motion): https://www.stuff.tv/review/sky-glass-review/
- Sky Glass review (design): https://www.forbes.com/sites/davidphelan/2021/11/18/sky-glass-review-great-design--staggering-content-but-not-quite-perfect/
- Sky Q vs Sky Glass UI: https://www.sky.com/hello-sky/sky-glass-vs-sky-q
- 10-foot user interface (background): https://en.wikipedia.org/wiki/10-foot_user_interface
- UX in Motion Manifesto (functional animation background): https://medium.com/ux-in-motion/creating-usability-with-motion-the-ux-in-motion-manifesto-a87a4584ddc
