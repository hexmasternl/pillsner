## Why

**GitHub Issue:** #94 (https://github.com/hexmasternl/pillsner/issues/94)
**Pull Request:** #95 (https://github.com/hexmasternl/pillsner/pull/95)

The marketing website at `src/website/` last changed its content on 2026-09-23. Several features shipped after that without the site being updated, because the `website-feature-sync` skill and the matching `tasks` rule in `openspec/config.yaml` didn't exist yet (issue #94). Two of the gaps go beyond missing information. The FAQ and the privacy page now make claims that the app no longer matches, about how data moves to a new phone and about which permissions the app asks for. Visitors rely on exactly those pages.

## What Changes

Copy-only updates to the existing pages, in English and translated into nl, fr, es, pt and de:

- **Backup and restore** (`data-backup-restore`): rewrite the FAQ answer "What happens when I get a new phone?", which still says Pillsner can't move your data itself, so it describes Export data and Import data in Settings: a passphrase-encrypted file the user saves and opens themselves, which replaces everything on import, with no passphrase recovery. Add a matching paragraph to the privacy page. Its "no backup service" claim stays true, but it needs to say that the export file goes only where the user puts it, is encrypted, and leaves out the app lock.
- **Label scanning** (`medicine-label-photo-prefill`): describe scanning a medicine label with the camera, or choosing a photo, to pre-fill the Add medicine form for review on the Medicines page. Disclose the camera permission on the privacy page: it is asked for only when the user chooses to scan, the text is read on the phone, nothing is kept, and nothing is sent. Mention it on the Home Medicines card if the card's one-line summary changes.
- **Theme setting** (`app-theme-setting`): mention that the app follows the system's light or dark setting, or can be set to either.
- **Audit sweep**: during implementation, run `website-feature-sync` in audit mode against every change archived since 2026-09-23. Fix any further gap it finds in the same change, and record what it checked in `tasks.md`.

There are no layout, template, CSS or navigation changes, and no new pages. Nothing goes on the site for a change that has not been archived, so the in-flight changes stay off it.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
(none. This is a content update. `marketing-website` and `website-faq` describe the site's structure and behaviour, which do not change. The change sets `skip_specs: true`.)

## Impact

- **Website content**: `src/website/content/{en,nl,fr,es,pt,de}/` — `faq.md`, `privacy.md`, `medicines.md`, possibly `_index.md`, and any page the audit flags. Possibly new screenshots under the site's image folder, made with made-up medicine names only.
- **App code**: none. The in-app privacy policy under `app-legal` is out of scope. If the audit finds it is also out of date about the camera permission or export, that gets a separate change.
- **Deployment**: the update reaches the live site with the next `development` → `main` release, like any other site change.
