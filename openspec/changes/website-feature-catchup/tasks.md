# Tasks

## 1. Audit

- [ ] 1.1 Run `website-feature-sync` in audit mode against every change archived since 2026-09-23. Record each one in this task as "belongs / doesn't belong (reason)", and for the ones that belong, list the page and what's missing. Verify: every archived change dated 2026-09-23 or later appears in the list.
- [ ] 1.2 Check the claims that most often go stale (audit step 3): the privacy page against `src/app/manifest-allowlist.txt`, the FAQ's permission and data answers, the languages against `hugo.toml` and the app's `values-*` folders, and the watch page against the `wearable-app` spec. Verify: each check is recorded here with its outcome.

## 2. Website: correct claims the app no longer meets

- [ ] 2.1 Rewrite the English FAQ answer "What happens when I get a new phone?" so it describes Export data and Import data in Settings. Cover: a passphrase-encrypted file the user saves and opens, an import that replaces everything after confirmation, no passphrase recovery, the app lock not included, and Android's own backup kept as the secondary route. Verify: each sentence maps to a requirement in `openspec/specs/data-backup-restore/spec.md`, and the button names match `values/strings.xml`.
- [ ] 2.2 Update English `privacy.md`: add the export file to "On your device, and nowhere else" (only where the user saves it, encrypted, no network), and disclose the camera permission (asked for only when choosing to scan, read on the phone, nothing kept or sent). Verify: every permission in `manifest-allowlist.txt` that a user can notice is either described on the page or confirmed in 1.2 as not needing a mention.
- [ ] 2.3 Translate 2.1 and 2.2 into nl, fr, es, pt and de, using the app's own locale strings for feature and button names. Verify: `hugo --minify --panicOnWarning` passes and every locale's `faq.md` and `privacy.md` have the same structure as English.

## 3. Website: describe features that shipped without copy

- [ ] 3.1 Add label scanning to English `medicines.md` under Adding a medicine: scan with the camera or choose a photo, fields pre-filled for review, nothing saved. Update the Home Medicines card only if its one-line summary no longer holds. Verify: the copy matches `openspec/specs/medicine-label-scan/spec.md`, and any screenshot uses made-up names only.
- [ ] 3.2 Mention the theme setting (system, light or dark) on the page the audit assigns, the FAQ by default. Verify: the copy matches `openspec/specs/app-theme/spec.md`.
- [ ] 3.3 Add copy for any further gap found in 1.1 that fits an existing page. Anything needing a new page or layout work is noted here and in the PR summary instead. Verify: every "belongs" item from 1.1 is either covered or explicitly deferred.
- [ ] 3.4 Translate 3.1–3.3 into nl, fr, es, pt and de. Verify: `hugo --minify --panicOnWarning` passes and every locale has the same structure as English.

## 4. Integration

- [ ] 4.1 Run `verify-change` and record the result as the Verification section of this file. Verify: the Hugo build passes and no file outside `src/website/content/` and the site's image folder changed (`git diff --stat development`).
