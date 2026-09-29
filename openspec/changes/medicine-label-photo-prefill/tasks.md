## 1. Build guard first, so every later step is checked by it

- [ ] 1.1 Add `app/manifest-allowlist.txt` listing the seven currently declared permissions (one per line, comments allowed) and an `artifact` line reserved for the OCR AAR checksum.
- [ ] 1.2 Add the `verifyManifestGuard<Variant>` task to `app/build.gradle.kts` (design D8): parse the merged manifest after `process<Variant>Manifest`, compare the `uses-permission` set with the allow-list, walk the runtime classpath for the forbidden groups with the wearable allow-list, and check the pinned checksum when an `artifact` line is present. Wire it into `check`, `assemble<Variant>` and `bundle<Variant>`.
- [ ] 1.3 Prove the guard works: temporarily add `INTERNET` to the manifest and confirm the debug build fails naming it; remove it again. Record the command and result in this file's verification section.
- [ ] 1.4 Add a `Manifest guard` line to `.github/copilot-instructions.md` asking review to flag any edit to `app/manifest-allowlist.txt`.

## 2. Dependency, repository and trained data

- [ ] 2.1 Add JitPack to `settings.gradle.kts` `dependencyResolutionManagement` with `content { includeGroup("cz.adaptech.tesseract4android") }`, with a comment naming this change and the reason (the library is not on Maven Central).
- [ ] 2.2 Add `tesseract4android-openmp` to the version catalog at the newest stable release (4.9.0 at proposal time; verify) and to the app module's dependencies.
- [ ] 2.3 Resolve the artifact, compute its SHA-256 and pin it on the `artifact` line of `app/manifest-allowlist.txt`; confirm the guard passes with it and fails when one character is changed.
- [ ] 2.4 Confirm, by reading the merged debug and release manifests, that the library contributes no permission, provider, service, receiver or activity. Record the finding in design D7 if anything differs from what it states.
- [ ] 2.5 Add `tessdata/eng.traineddata` from `tessdata_fast` (record the commit or tag it was taken from and its size in design D1) under `app/src/main/assets/`.
- [ ] 2.6 Exclude `files/ocr/` in both `data_extraction_rules.xml` and `full_backup_content.xml`.

## 3. Domain: label interpretation (pure Kotlin)

- [ ] 3.1 Add `domain/labelscan/RecognisedLine` and `LabelInterpretation` models (design D4).
- [ ] 3.2 Add `LabelVocabulary`: the six-language unit, frequency, day-part, box-notation, duration, until and noise-word tables with accent- and case-insensitive matching.
- [ ] 3.3 Implement dose-token extraction (rule 1) and default-dose selection (rule 2) with unit tests per language.
- [ ] 3.4 Implement frequency parsing to schedules (rules 3 and 4), including the default time tables, every-N-hours interval filter, every-other-day, day parts and box notation with equal and differing digits; unit tests for each shape and for every rejected frequency.
- [ ] 3.5 Implement name selection (rule 5) with noise-line filtering, token removal and the 60-character cap; unit tests for the Dutch pharmacy label, box front, and noise-first layouts.
- [ ] 3.6 Implement date recognition, used-since selection and use-until from until-dates and durations (rules 6 and 7); unit tests for the dispense date, expiry month-year, old date, future date, 7-day, 2-week and explicit until cases, including a course that crosses a month end and a year end.
- [ ] 3.7 Add the six end-to-end fixtures (EN, NL, DE, FR, ES, PT) with invented medicine names and assert the whole interpretation for each.
- [ ] 3.8 Assert the package imports nothing from `android.*`, `androidx.*` or the OCR library (an existing "no Android imports" style test or a new one).

## 4. Data: capture files and the recogniser

- [ ] 4.1 Add `res/xml/label_scan_paths.xml` with the single `cache-path label-scan/` entry and the `FileProvider` manifest entry (`exported="false"`, `grantUriPermissions="true"`, authority `${applicationId}.labelscan`), plus the `<queries>` intent for `android.media.action.IMAGE_CAPTURE`.
- [ ] 4.2 Add `LabelScanFiles`: creates a capture file under `cacheDir/label-scan/`, returns its provider URI, deletes one file, and sweeps the directory. Call the sweep from `PillsnerApplication.onCreate`.
- [ ] 4.3 Add `TessdataInstaller`: copies `assets/tessdata/eng.traineddata` to `filesDir/ocr/tessdata/` when missing or when the version marker differs from the app's version code.
- [ ] 4.4 Add `LabelTextRecogniser` (design D3): EXIF orientation, bounded decode, greyscale, `TessBaseAPI` init with `PSM_AUTO`, text-line iteration to `RecognisedLine`s, cancellation via `stop()`, cleanup in `finally`, debug-level timing logs with no payload.
- [ ] 4.5 Wire `TessdataInstaller`, `LabelScanFiles` and `LabelTextRecogniser` into `AppContainer` and expose a `ScanLabel` use case (install data if needed, recognise, interpret, delete capture file) to the form view model factory.
- [ ] 4.6 Instrumented test: a stale file under `cacheDir/label-scan/` is removed by the sweep; a capture file is deleted after a failed recognition; no file is created outside that subdirectory.

## 5. Form: entry point, progress, apply and banner

- [ ] 5.1 Extend `MedicationFormUiState` with `canScanLabel` (add mode), `showScanOptions`, `cameraAvailable`, `isScanning`, `pendingInterpretation` (for the replace dialog), `showScanBanner` and `scanRawText`; persist the banner flag, the raw text and the pending capture URI through `SavedStateHandle`.
- [ ] 5.2 Add view model handlers: `onScanLabelClicked`, `onScanOptionsDismissed`, `startCapture(): Uri`, `onPhotoCaptured(success)`, `onPhotoPicked(uri?)`, `cancelScan`, `onReplaceConfirmed`, `onReplaceDeclined`, `onScanBannerDismissed`, `onShowScanText`, and `applyInterpretation` (design D5's field rules, prescriber untouched, `DraftSaver.save` after applying).
- [ ] 5.3 Build the UI with `pillsner-ui-build`: the "Scan a label" secondary button with camera icon above the name field (add mode only), the option sheet, the modal progress state with Cancel, the replace dialog, the attention banner with "Show text" and dismiss, the raw-text sheet, and the two snackbars. Register the `TakePicture` and `PickVisualMedia` launchers in `MedicationFormNavigation`.
- [ ] 5.4 Add the `ic_camera` vector drawable if none exists, following the design system's icon rules.
- [ ] 5.5 Add every new string to `values/strings.xml` and to `values-nl`, `values-de`, `values-fr`, `values-es` and `values-pt`.
- [ ] 5.6 Unit tests for the view model: apply on an untouched draft, replace dialog on an edited draft, keep leaves the draft, nothing-readable leaves the draft and emits the message, failure emits the other message, cancel resets `isScanning`, banner flag survives a saved-state round trip, prescriber never changes.
- [ ] 5.7 Compose test: the button is present in add mode and absent in edit mode; the banner and its actions are present after an applied interpretation; the form scrolls fully at the largest font scale with the banner shown.
- [ ] 5.8 Run `pillsner-ui-review` on everything under `ui/` touched by this change and fix every finding.

## 6. Documentation

- [ ] 6.1 README: add the sentence under the permission table (design D7), name `app/manifest-allowlist.txt` as the source of truth, and add the OCR library to the Technology table.
- [ ] 6.2 `CHANGELOG.md`: describe the feature, the zero-permission approach, the size increase and the build guard under the next release.
- [ ] 6.3 Fill design D7's native-library row and the Open Questions with the measured facts from section 8.

## 7. Removal of the superseded change

- [ ] 7.1 Confirm `openspec/changes/medicine-label-ocr-scan/` is gone from this branch (removed with the proposal commit) and that nothing else references it.

## 8. Verification

- [ ] 8.1 Run the unit test task for `:app` and report the result verbatim.
- [ ] 8.2 Run lint and report the result verbatim.
- [ ] 8.3 Run the instrumented tests from 4.6 on an emulator and report the result verbatim.
- [ ] 8.4 Manual, on a device: photograph at least one real label or box per app language (EN, NL, DE, FR, ES, PT, using the phone's camera app) and one picked photo. For each, record in design D1 which of name, default dose, schedule, used since and use until were right, wrong or absent. If a language is unusable, add its `tessdata_fast` file and re-measure, and record the size cost. If accuracy is unacceptable overall, stop and reopen design D1 rather than shipping.
- [ ] 8.5 Manual: airplane mode from a fresh install, scan works; cancel during reading; no camera app (emulator without camera) shows picker only; process death while the camera app is open still delivers the result.
- [ ] 8.6 Build a release bundle, confirm the guard ran and passed for the release variant, and record the per-ABI download size increase (bundletool `get-size total`) in design D7 and the CHANGELOG.
- [ ] 8.7 Confirm with `adb logcat` on a release build that a scan writes no recognised text, name or amount to the log.
