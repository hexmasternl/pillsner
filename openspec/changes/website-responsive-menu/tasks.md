## 1. Design system

- [x] 1.1 Add section 8.14 *Website header navigation* to `docs/design-system.md` (grouping, breakpoint, tokens for bar, links, current-page indicator, dropdown, Menu button and mobile panel, motion, accessibility, no-JS behaviour) and a matching Do/Don't row
- [x] 1.2 Add a light and dark specimen of the wide-screen header with an open dropdown and of the narrow-screen header with the open menu panel to `docs/design-system.html`

## 2. Menu structure and strings

- [ ] 2.1 In `src/website/hugo.toml` add the `features` and `help` parent entries, set `parent` on the seven child entries, and add a comment stating the four-top-level-entry limit
- [ ] 2.2 Add `nav_help`, `nav_menu_open` and `nav_menu_close` to `i18n/en.toml`, then `nl`, `fr`, `es`, `pt`, `de`; confirm `nav_features` exists in every locale
- [ ] 2.3 Add `menu` and `close` icons to `layouts/partials/icon.html`

## 3. Header and footer templates

- [ ] 3.1 Rewrite `layouts/partials/header.html`: brand, a single `<nav>` rendering top-level links and `<details>` groups (with `aria-current="page"` on the current link and a current marker on the group that `HasMenuCurrent`), the Menu `<button>` with `aria-controls`/`aria-expanded` and i18n labels in `data-` attributes, and the language switcher
- [ ] 3.2 Add the inline `<head>` script to `layouts/_default/baseof.html` that sets the `js` class on `<html>`, with a comment about CSP hashing
- [ ] 3.3 Update `layouts/partials/footer.html` so the Explore list stays flat, iterating children of parent entries

## 4. Styles and behaviour

- [ ] 4.1 Replace the `.site-nav` rules in `assets/css/layout.css` with the narrow-first panel layout and the ≥ 960 px single-row layout, using tokens only, 48 px targets, and reduced-motion handling
- [ ] 4.2 Style the group dropdowns in `assets/css/components.css`, sharing the language switcher menu's surface, shape, shadow and entrance
- [ ] 4.3 Add `assets/js/navigation.js` (toggle, Escape and link-click close with focus return, outside-click and sibling-close for groups, close on crossing 960 px) and add it to the JS bundle in `baseof.html`

## 5. Documentation

- [ ] 5.1 Update `src/website/README.md` where it describes the header, menu configuration and scripts

## 6. Verification

- [ ] 6.1 Run `hugo --minify` from `src/website/` and confirm a clean build
- [ ] 6.2 Check the header at 360 px, 768 px, 960 px and 1280 px in English, French and German: one row on wide, Menu button on narrow, no truncated label
- [ ] 6.3 Check keyboard-only use (Tab, Enter/Space, Escape, focus return) and a screen reader announcement of the Menu button and groups
- [ ] 6.4 Check with JavaScript disabled: all pages reachable on narrow and wide viewports
- [ ] 6.5 Check light and dark schemes and `prefers-reduced-motion: reduce`
- [ ] 6.6 Confirm no tracking, network request or new storage was introduced
