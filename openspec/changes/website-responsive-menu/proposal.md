## Why

**GitHub Issue:** #63 (https://github.com/hexmasternl/pillsner/issues/63)

Since `website-multipage-expansion` the header lists all nine pages as a flat row of links. That row is wider than the header on most screens: it wraps onto two or three ragged lines on desktop, pushes the language switcher around, and on a phone it turns the sticky header into a block of links that covers a large part of the screen. The site is the first thing a prospective user sees of Pillsner; a menu that looks broken undercuts the "calm, reliable" promise the app makes.

## What Changes

- Group the nine pages into four top-level header entries instead of nine: **Home**, **Features** (Medicines, Schedules, Tracking, On your wrist), **Privacy** and **Help** (FAQ, Report a bug, Request a feature). Grouping is declared once in `hugo.toml` using Hugo's menu `parent` field, so every locale inherits it.
- **Wide screens (≥ 960 px):** a single-row header — brand at the start, the four entries centred as pill-shaped links, language switcher at the end. Features and Help open a small dropdown menu on click or keyboard, styled like the existing language switcher menu. The current page gets the `secondaryContainer` pill indicator; a group whose child is the current page is marked the same way.
- **Narrow screens (< 960 px):** the header collapses to brand, language switcher and a 48 px **Menu** button. The button opens a full-width panel below the sticky header listing every page, grouped under the Features and Help headings, with 48 px tap targets. The panel scrolls on its own if it is taller than the viewport, closes on Escape, on selecting a link and on resizing to wide, and returns focus to the Menu button.
- Works without JavaScript: without JS the narrow-screen menu renders expanded as a plain stacked list, and the dropdowns use native `<details>`/`<summary>` the way the language switcher already does. A small script only adds the toggle, outside-click and Escape behaviour.
- The footer's "Explore" column keeps listing every page flat, now iterating the children of the two groups.
- New translated strings for the Help group label and the Menu button's open/close labels, in all six locales.
- `docs/design-system.md` and `docs/design-system.html` gain a *Website header navigation* component section describing this menu, so the site's navigation is covered by the same source of truth as the app.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `marketing-website`: adds a requirement that the header navigation fits on one row on wide screens, collapses into a menu button on narrow screens, groups pages, indicates the current page and remains usable without JavaScript and with keyboard and screen readers. It builds on the *Cross-page navigation* requirement introduced by `website-multipage-expansion` without changing it.

## Impact

- `src/website/hugo.toml` — menu entries get `parent` groups.
- `src/website/layouts/partials/header.html`, `layouts/partials/footer.html` — new header markup; footer iterates group children.
- `src/website/assets/css/layout.css` — header and navigation styles, responsive breakpoint.
- `src/website/assets/js/` — new `navigation.js`, added to the concatenated bundle in `layouts/_default/baseof.html`.
- `src/website/layouts/partials/icon.html` — `menu` and `close` icons.
- `src/website/i18n/*.toml` — three new keys per locale.
- `docs/design-system.md`, `docs/design-system.html` — new component section.
- No Android app code, no new dependency, no network access, no tracking.
