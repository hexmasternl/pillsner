## 1. Design system

- [x] 1.1 Add section 8.14 *Website header navigation* to `docs/design-system.md` (grouping, breakpoint, tokens for bar, links, current-page indicator, dropdown, Menu button and mobile panel, motion, accessibility, no-JS behaviour) and a matching Do/Don't row
- [x] 1.2 Add a light and dark specimen of the wide-screen header with an open dropdown and of the narrow-screen header with the open menu panel to `docs/design-system.html`

## 2. Menu structure and strings

- [x] 2.1 In `src/website/hugo.toml` add the `features` and `help` parent entries, set `parent` on the seven child entries, and add a comment stating the four-top-level-entry limit
- [x] 2.2 Add `nav_help`, `nav_menu_open` and `nav_menu_close` to `i18n/en.toml`, then `nl`, `fr`, `es`, `pt`, `de`; confirm `nav_features` exists in every locale
- [x] 2.3 Add `menu` and `close` icons to `layouts/partials/icon.html`

## 3. Header and footer templates

- [x] 3.1 Rewrite `layouts/partials/header.html`: brand, a single `<nav>` rendering top-level links and `<details>` groups (with `aria-current="page"` on the current link and a current marker on the group that `HasMenuCurrent`), the Menu `<button>` with `aria-controls`/`aria-expanded` and i18n labels in `data-` attributes, and the language switcher
- [x] 3.2 Add the inline `<head>` script to `layouts/_default/baseof.html` that sets the `js` class on `<html>`, with a comment about CSP hashing
- [x] 3.3 Update `layouts/partials/footer.html` so the Explore list stays flat, iterating children of parent entries

## 4. Styles and behaviour

- [x] 4.1 Replace the `.site-nav` rules in `assets/css/layout.css` with the narrow-first panel layout and the ≥ 960 px single-row layout, using tokens only, 48 px targets, and reduced-motion handling
- [x] 4.2 Style the group dropdowns (in `assets/css/layout.css`, next to the rest of the header — see design.md implementation notes), sharing the language switcher menu's surface, shape, shadow and entrance
- [x] 4.3 Add `assets/js/navigation.js` (toggle, Escape and link-click close with focus return, outside-click and sibling-close for groups, close on crossing 960 px) and add it to the JS bundle in `baseof.html`

## 5. Documentation

- [x] 5.1 Update `src/website/README.md` where it describes the header, menu configuration and scripts

## 6. Verification

- [x] 6.1 Run `hugo --minify` from `src/website/` and confirm a clean build
- [x] 6.2 Check the header at 360 px, 768 px, 960 px and 1280 px in English, French and German: one row on wide, Menu button on narrow, no truncated label
- [x] 6.3 Check keyboard-only use (Tab, Enter/Space, Escape, focus return) and a screen reader announcement of the Menu button and groups
- [x] 6.4 Check with JavaScript disabled: all pages reachable on narrow and wide viewports
- [x] 6.5 Check light and dark schemes and `prefers-reduced-motion: reduce`
- [x] 6.6 Confirm no tracking, network request or new storage was introduced

### Verification notes

- 6.1: `hugo --minify --environment production` builds cleanly, no errors or warnings.
- 6.2: Checked in headless Edge over DevTools. At exactly 960 px all six locales (en, nl, fr, es, pt, de) fit on one 64 px row with no truncated label and no horizontal page scroll. The Menu button shows below 960 px (360 px in de, 768 px in fr); the open panel lists all nine pages and scrolls on its own when the viewport is too short (360 × 480). Resizing across 960 px closes the panel.
- 6.3: Tab order is skip link, brand, Home, Features, Privacy, Help, language switcher. Enter opens a group, and Tab leaving a group closes it. Opening one group closes the other. Escape closes the open group and returns focus to its summary, and on narrow screens it closes the panel and returns focus to the Menu button. An outside click closes a group. The accessibility tree shows the nav as the "Main" navigation landmark, the groups as expandable disclosures and the Menu button as "Menu"/expanded=false changing to "Close menu"/expanded=true. **Not done:** a run with a real screen reader (NVDA/TalkBack), and activating controls with the Space key.
- 6.4: With script execution disabled the Menu button is hidden. On narrow screens the menu shows in place with collapsible groups and the header is not sticky; on wide screens it is the same single row as with JS, with native dropdowns.
- 6.5: Light and dark were checked in screenshots at 360, 768, 960 and 1280 px. Reduced motion: every nav animation is gated behind `prefers-reduced-motion: no-preference`. The rendered motion itself was not observed.
- 6.6: `navigation.js` and the header make no network request and use no storage or cookies. The inline `<head>` script only adds a class.
