## 1. Groundwork

- [x] 1.1 Verify the project scaffold exists (`src/` Gradle project, `app` module, `AppContainer`, `PillsnerTheme`, `SettingsScreen`, `PillsnerApp`) and stop if it does not — this change creates no part of it.
- [x] 1.2 Confirm the debug build, unit tests and lint pass before any change, so a later failure is known to belong to this work.

## 2. The legal text

- [x] 2.1 Draft the English Disclaimer as headed sections covering all five points of the "What the Disclaimer says" requirement, in plain language, short enough to read in one sitting.
- [x] 2.2 Draft the English Terms of Service as headed sections covering licence, "as is" and no warranty, limitation of liability with the unexcludable-liability note, data on the device and backups, revision and re-acceptance, and Dutch governing law with Eduard Keilholz named as publisher and developer.
- [x] 2.3 Add both documents to `values/strings.xml` as one key per heading and one per paragraph, named `legal_disclaimer_s<n>_heading` / `legal_disclaimer_s<n>_p<m>` and `legal_terms_s<n>_…`.
- [x] 2.4 Translate both documents into Dutch in `values-nl/strings.xml`, keeping "Pillsner" and "Eduard Keilholz" untranslated.
- [x] 2.5 Add the remaining English and Dutch strings: the two document titles, the Legal section header and its two row labels, the three acceptance-status lines, the acceptance screen title, its scroll hint, its terms link and its accept label, and the version-and-date line.

## 3. Domain layer

- [x] 3.1 Add `domain/legal/LegalDocument.kt` with `LegalDocumentId`, `TextRef`, `LegalSection` and `LegalDocument` — no Android imports.
- [x] 3.2 Add `domain/legal/LegalAcceptance.kt` holding the two accepted versions and the moment of acceptance.
- [x] 3.3 Add `domain/legal/CurrentLegalDocuments.kt` declaring both documents, their sections in order, their versions (both 1) and their effective dates in one place.
- [x] 3.4 Add `domain/repository/LegalRepository.kt` with `observeAcceptance(): Flow<LegalAcceptance?>` and `suspend fun accept(disclaimerVersion: Int, termsVersion: Int)`.
- [x] 3.5 Add `domain/legal/IsLegalAccepted.kt` returning a `Flow<Boolean>` that is true only when a record exists and both stored versions are at least the current versions.
- [x] 3.6 Unit-test `IsLegalAccepted` for: no record, both current, disclaimer stale, terms stale, both stale, and both newer than the installed build.
- [x] 3.7 Unit-test that both documents are present, have at least one section, carry version 1 and an effective date, and that no document holds prose.

## 4. Data layer

- [x] 4.1 Add `data/settings/DataStoreLegalRepository.kt` storing the two versions and the acceptance instant in the existing general `settingsDataStore`, mirroring `DataStoreLanguageRepository`.
- [x] 4.2 Write acceptance as a single `edit` of all three keys so a version can never be stored without a time.
- [x] 4.3 Read a missing or partial record as no acceptance.
- [x] 4.4 Test the repository: nothing stored reads as null; an acceptance round-trips with the same versions and instant; a record missing the instant reads as null.
- [x] 4.5 Register `LegalRepository` and `IsLegalAccepted` in `di/AppContainer.kt` and register `LegalViewModel` in its `viewModelFactory`.

## 5. Shared document rendering

- [x] 5.1 Add `ui/settings/legal/LegalDocumentBody.kt`: renders a `LegalDocument` as headings in `titleMedium` marked `semantics { heading() }` and paragraphs in `bodyLarge`, with `Spacing.lg` between paragraphs and `Spacing.xl` above each heading. No `maxLines` anywhere.
- [x] 5.2 Give it previews in light and dark and at `fontScale = 2f`.
- [x] 5.3 Run `pillsner-ui-review` on it and fix anything it reports.

## 6. The document screen

- [x] 6.1 Add `LegalDocumentRoute(document: LegalDocumentId)` to `ui/navigation/Routes.kt`.
- [x] 6.2 Add `ui/settings/legal/LegalDocumentScreen.kt`: a `Scaffold` with a `TopAppBar` carrying the document title and a back arrow, the version and effective date in `bodySmall` `onSurfaceVariant` beneath it, and `LegalDocumentBody` in a scrolling column constrained to `Spacing.contentMaxWidth`.
- [x] 6.3 Register `composable<LegalDocumentRoute>` in the `NavHost` in `ui/PillsnerApp.kt`, reading the document from the route.
- [x] 6.4 Add `LegalDocumentTestTags` and previews; run `pillsner-ui-review`.

## 7. The Settings Legal section

- [x] 7.1 Add `ui/settings/legal/LegalViewModel.kt` exposing the acceptance state (accepted, accepted-but-revised with its date, or never accepted) and the accepted flag the gate reads, plus an `accept()` event.
- [x] 7.2 Add `ui/settings/legal/LegalSection.kt`: a `headlineSmall` "Legal" heading, a Disclaimer row and a Terms of Service row shaped like the Security section's "Change PIN" row, and the `bodySmall` status line, with the date formatted for the app language.
- [x] 7.3 Add the section as the last `item` in the `SettingsScreen` `LazyColumn` after Security, with `SettingsScreen` gaining the section state and an `onOpenDocument` lambda.
- [x] 7.4 Wire the Settings destination in `PillsnerApp.kt` to `LegalViewModel` and to `navController.navigate(LegalDocumentRoute(...))`.
- [x] 7.5 Add `LegalSectionTestTags` and previews for all three status states; run `pillsner-ui-review`.

## 8. The acceptance screen

- [x] 8.1 Add `AcceptLegal` to `ui/navigation/Routes.kt`.
- [x] 8.2 Add `ui/settings/legal/AcceptLegalScreen.kt`: a `TopAppBar` titled "Before you add a medicine" with a back arrow, the Disclaimer through `LegalDocumentBody` in a scrolling column, a `TextButton` opening the Terms of Service, and a full-width filled accept button at least 56 dp tall pinned below the scrolling content.
- [x] 8.3 Enable the accept button only once the scroll state has reached its end, and treat content that does not scroll as already at its end.
- [x] 8.4 Show the scroll hint beneath the disabled button and remove it when the button becomes enabled; announce the change with a live region.
- [x] 8.5 Keep the scroll position across a visit to the Terms of Service and across rotation.
- [x] 8.6 Give the accept button a semantics state and a disabled reason a screen reader announces.
- [x] 8.7 Register `composable<AcceptLegal>` in the `NavHost`; on accept, record the acceptance then `navigate(MedicationFormGraph())` popping `AcceptLegal` off the back stack.
- [x] 8.8 Add `AcceptLegalTestTags` and previews in light, dark and at `fontScale = 2f`; run `pillsner-ui-review`.

## 9. The gate

- [x] 9.1 Collect the accepted flag from `LegalViewModel` at the `NavHost` level in `PillsnerApp.kt`.
- [x] 9.2 Change the Medicines screen's `onAddMedicine` to navigate to `MedicationFormGraph()` when accepted and to `AcceptLegal` when not, deciding at the moment of the tap.
- [x] 9.3 Leave the tile-tap route into the form in edit mode untouched, and confirm by test that it is never gated.

## 10. Tests

- [x] 10.1 Compose semantics test: the Legal section shows both rows and the right status line for never-accepted, accepted, and accepted-but-revised.
- [x] 10.2 Compose semantics test: the document screen shows the requested document's headings as headings, with its version and effective date.
- [x] 10.3 Compose semantics test: the accept button is disabled with a hint before scrolling and enabled after scrolling to the end, and enabled immediately when the content fits.
- [x] 10.4 Navigation test: the add button opens the acceptance screen when unaccepted and the form directly when accepted.
- [x] 10.5 Navigation test: accepting reaches the form, and back from the untouched form returns to Medicines without passing through the acceptance screen.
- [x] 10.6 Navigation test: backing out of the acceptance screen records nothing and adds no medicine.
- [x] 10.7 Navigation test: after a version bump with an older acceptance stored, tapping an existing medicine tile opens the form in edit mode with no gate.
- [x] 10.8 Test that the Terms of Service opened from the acceptance screen returns to it with its scroll position intact.
- [x] 10.9 Test that the document screen restores the same document after process death.

## 11. Verification and documentation

- [x] 11.1 Run the unit test task from `src/` and report any failure verbatim.
- [x] 11.2 Run lint from `src/` and confirm no missing-translation or extra-translation errors.
- [x] 11.3 Run the instrumented tests for the navigation and screen tests added here.
- [ ] 11.4 Manually check the acceptance screen at maximum font scale, in dark theme, and with TalkBack, confirming the disclaimer scrolls fully and the accept button stays visible and reachable.
- [ ] 11.5 Manually check that a device with no network can read both documents and give acceptance.
- [x] 11.6 Confirm the manifest gained no permission and the build gained no dependency.
- [x] 11.7 Update `README.md` to note that Pillsner shows a disclaimer and terms of service and requires acceptance before the first medicine is added.
- [x] 11.8 Run `pillsner-ui-review` once more across everything added under `ui/settings/legal/` and fix anything outstanding before declaring the change done.
