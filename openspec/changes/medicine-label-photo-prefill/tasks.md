## 1. Build guard first, so every later step is checked by it

- [x] 1.1 Add `app/manifest-allowlist.txt` listing the seven currently declared permissions (one per line, comments allowed) and an `artifact` line reserved for the OCR AAR checksum. `CAMERA` is added in task 4.1, in the same commit as the manifest line.
- [x] 1.2 Add the `verifyManifestGuard<Variant>` task to `app/build.gradle.kts` (design D8): parse the merged manifest after `process<Variant>Manifest`, compare the `uses-permission` set with the allow-list, walk the runtime classpath for the forbidden groups and `androidx.camera:camera-mlkit-vision` with the wearable allow-list, and check the pinned checksum when an `artifact` line is present. Wire it into `check`, `assemble<Variant>` and `bundle<Variant>`.
- [x] 1.3 Prove the guard works: temporarily add `INTERNET` to the manifest and confirm the debug build fails naming it; remove it again. Record the command and result in this file's verification section.
- [x] 1.4 Add a `Manifest guard` line to `.github/copilot-instructions.md` asking review to flag any edit to `app/manifest-allowlist.txt`.

## 2. Dependencies, repository and trained data

- [x] 2.1 Add JitPack to `settings.gradle.kts` `dependencyResolutionManagement` with `content { includeGroup("cz.adaptech.tesseract4android") }`, with a comment naming this change and the reason (the library is not on Maven Central).
- [x] 2.2 Add `tesseract4android-openmp` (newest stable, 4.9.0 at proposal time; verify) and CameraX `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-compose` (newest stable, 1.6.2 at proposal time; verify) to the version catalog and the app module's dependencies.
- [x] 2.3 Resolve the Tesseract artifact, compute its SHA-256 and pin it on the `artifact` line of `app/manifest-allowlist.txt`; confirm the guard passes with it and fails when one character is changed.
- [x] 2.4 Read the merged debug and release manifests and record in design D7 exactly what CameraX and Tesseract4Android contribute (permissions, features, components). Anything beyond `CAMERA` and the camera feature is a design correction, not an allow-list edit.
- [x] 2.5 Add `tessdata/eng.traineddata` from `tessdata_fast` (record the commit or tag it was taken from and its size in design D1) under `app/src/main/assets/`.
- [x] 2.6 Exclude `files/ocr/` in both `data_extraction_rules.xml` and `full_backup_content.xml`.

## 3. Domain: label interpretation (pure Kotlin)

- [x] 3.1 Add `domain/labelscan/RecognisedLine` and `LabelInterpretation` (with `isEmpty`) models (design D4).
- [x] 3.2 Add `LabelVocabulary`: the six-language unit, frequency, day-part, box-notation, duration, until and noise-word tables with accent- and case-insensitive matching.
- [x] 3.3 Implement dose-token extraction (rule 1) and default-dose selection (rule 2) with unit tests per language.
- [x] 3.4 Implement frequency parsing to schedules (rules 3 and 4), including the default time tables, every-N-hours interval filter, every-other-day, day parts and box notation with equal and differing digits; unit tests for each shape and for every rejected frequency.
- [x] 3.5 Implement name selection (rule 5) with noise-line filtering, token removal and the 60-character cap; unit tests for the Dutch pharmacy label, box front, and noise-first layouts.
- [x] 3.6 Implement date recognition, used-since selection and use-until from until-dates and durations (rules 6 and 7); unit tests for the dispense date, expiry month-year, old date, future date, 7-day, 2-week and explicit until cases, including a course that crosses a month end and a year end.
- [x] 3.7 Add the six end-to-end fixtures (EN, NL, DE, FR, ES, PT) with invented medicine names and assert the whole interpretation for each.
- [x] 3.8 Add `ScanAcceptance`: the pure two-consecutive-good-frames rule from design D3 (name equality case- and whitespace-insensitive), with unit tests for accept, single misread, and partial reads.
- [x] 3.9 Assert the package imports nothing from `android.*`, `androidx.*` or the OCR library (an existing "no Android imports" style test or a new one).

## 4. Manifest, permission and data layer

- [ ] 4.1 Add `<uses-permission android:name="android.permission.CAMERA"/>` with a comment naming this change, and `<uses-feature android:name="android.hardware.camera.any" android:required="false" tools:replace="android:required"/>`, to the manifest; add `CAMERA` to `app/manifest-allowlist.txt` in the same commit; confirm the guard passes.
- [ ] 4.2 Add `CameraPermission` (following `ui/home/NotificationPermissionRequest.kt`): granted check, rationale-needed check, permanently-denied detection, request launcher, and an intent to the app's settings page.
- [ ] 4.3 Add `TessdataInstaller`: copies `assets/tessdata/eng.traineddata` to `filesDir/ocr/tessdata/` when missing or when the version marker differs from the app's version code.
- [ ] 4.4 Add `LabelTextRecogniser` (design D3): owns one `TessBaseAPI` for the life of a scan session (`open`/`close`), recognises a greyscale byte frame (`setImage` with one byte per pixel, `PSM_AUTO`, text-line iteration to `RecognisedLine`s), supports `stop()`, and logs only timings without payload.
- [ ] 4.5 Add `FrameCropper`: Y-plane extraction from an `ImageProxy`, crop to the guide rectangle mapped into frame coordinates, rotation by `rotationDegrees`, as plain byte-array loops with unit tests on synthetic frames.
- [ ] 4.6 Add `PickedPhotoDecoder`: EXIF orientation, bounded decode (long side ≤ 2,000 px) from a content URI stream, conversion to greyscale bytes; never copies the source.
- [ ] 4.7 Wire `TessdataInstaller`, `LabelTextRecogniser`, `FrameCropper` and `PickedPhotoDecoder` into `AppContainer` and expose them to the view model factory.

## 5. Scanning screen

- [ ] 5.1 Add the `LabelScan` route to the medication form graph and `LabelScanViewModel` (camera state: torch available/on, seconds elapsed, hint level, latest interpretation, accepted interpretation) that binds `Preview` and `ImageAnalysis` (YUV, keep-only-latest, 1280 × 960 preferred, single-thread executor), runs cropper → recogniser → interpreter per frame, applies `ScanAcceptance`, and never binds `ImageCapture`.
- [ ] 5.2 Build `LabelScanScreen` with `pillsner-ui-build`: `CameraXViewfinder`, framing guide drawn with theme tokens, instruction text with the 8-second and 20-second hint changes, shutter, torch toggle, cancel, haptic and "Label read" announcement on acceptance, and camera release on dispose and on background.
- [ ] 5.3 Hand the accepted or shutter interpretation to the shared `MedicationFormViewModel` (`onInterpretationReceived`) and pop back to the form.
- [ ] 5.4 Compose test: shutter before any read returns an empty interpretation; cancel pops without calling the form; controls have content descriptions.

## 6. Form: entry point, permission, apply and banner

- [ ] 6.1 Extend `MedicationFormUiState` with `canScanLabel` (add mode), `showScanOptions`, `cameraAvailable`, `showCameraRationale`, `isScanning` (picked photo), `pendingInterpretation` (for the replace dialog), `showScanBanner` and `scanRawText`; persist the banner flag and the raw text through `SavedStateHandle`.
- [ ] 6.2 Add view model handlers: `onScanLabelClicked`, `onScanOptionsDismissed`, `onScanWithCameraChosen`, `onRationaleContinue`, `onRationaleDismissed`, `onCameraPermissionResult(granted, permanentlyDenied)`, `onPhotoPicked(uri?)`, `cancelScan`, `onInterpretationReceived`, `onReplaceConfirmed`, `onReplaceDeclined`, `onScanBannerDismissed`, `onShowScanText`, and `applyInterpretation` (design D5's field rules, prescriber untouched, `DraftSaver.save` after applying).
- [ ] 6.3 Build the UI with `pillsner-ui-build`: the "Scan a label" secondary button with camera icon above the name field (add mode only), the option sheet, the camera rationale dialog, the permission-denied snackbar with the settings action, the modal "Reading the photo…" state with Cancel, the replace dialog, the attention banner with "Show text" and dismiss, the raw-text sheet, and the result snackbars. Register the permission and `PickVisualMedia` launchers in `MedicationFormNavigation`.
- [ ] 6.4 Add the `ic_camera`, `ic_flash_on` and `ic_flash_off` vector drawables if none exist, following the design system's icon rules.
- [ ] 6.5 Add every new string to `values/strings.xml` and to `values-nl`, `values-de`, `values-fr`, `values-es` and `values-pt`.
- [ ] 6.6 Unit tests for the form view model: apply on an untouched draft, replace dialog on an edited draft, keep leaves the draft, empty interpretation leaves the draft and emits the message, decode failure emits the other message, cancel resets `isScanning`, banner flag survives a saved-state round trip, prescriber never changes, denied permission emits the unavailable message.
- [ ] 6.7 Compose test: the button is present in add mode and absent in edit mode; the rationale shows before the system prompt; the banner and its actions are present after an applied interpretation; the form scrolls fully at the largest font scale with the banner shown.
- [ ] 6.8 Run `pillsner-ui-review` on everything under `ui/` touched by this change and fix every finding.

## 7. Documentation

- [ ] 7.1 README: add the `CAMERA` row to the permission table (design D7 wording), name `app/manifest-allowlist.txt` as the source of truth, and add CameraX and the OCR library to the Technology table.
- [ ] 7.2 `CHANGELOG.md`: describe the feature, the new camera permission and when it is asked, the size increase and the build guard under the next release.
- [ ] 7.3 Fill design D7's library rows and the Open Questions with the measured facts from section 9.
- [ ] 7.4 Confirm `openspec/changes/medicine-label-ocr-scan/` is gone from this branch (removed with the proposal commit) and that nothing else references it.

## 8. Play listing

- [ ] 8.1 Review the Play Console Data safety answers against design D7 (camera frames processed in memory, not collected, not shared) and note in this file whether any answer needs changing before the release that ships this change.

## 9. Verification

- [ ] 9.1 Run the unit test task for `:app` and report the result verbatim.
- [ ] 9.2 Run lint and report the result verbatim.
- [ ] 9.3 Run the Compose and instrumented tests from 5.4 and 6.7 on an emulator and report the result verbatim.
- [ ] 9.4 Manual, on a device: live-scan at least one real label or box per app language (EN, NL, DE, FR, ES, PT) and one picked photo. For each, record in design D1 which of name, default dose, schedule, used since and use until were right, wrong or absent, and how many seconds acceptance took. If a language is unusable, add its `tessdata_fast` file and re-measure, and record the size cost. If accuracy is unacceptable overall, stop and reopen design D1 rather than shipping.
- [ ] 9.5 Measure per-frame recognition time on the slowest supported test device and record it in design D3's open question; if it exceeds about one second, lower the analysis resolution and re-measure.
- [ ] 9.6 Manual: airplane mode from a fresh install, scan works; first-use rationale then system prompt; deny, permanently deny and settings action; camera-less emulator shows "Choose a photo" only; background during scan releases the camera and relocks when the app lock is on; rotation on the scanning screen.
- [ ] 9.7 Build a release bundle, confirm the guard ran and passed for the release variant, and record the per-ABI download size increase (bundletool `get-size total`) in design D7 and the CHANGELOG.
- [ ] 9.8 Confirm with `adb logcat` on a release build that a scan writes no recognised text, name or amount to the log, and with the device file explorer that no image file appeared under the app's cache or files directories.

### Verification record

**1.3 Guard proof (29 September 2026).** With `<uses-permission android:name="android.permission.INTERNET" />` added to `app/src/main/AndroidManifest.xml`, `./gradlew :app:assembleDebug` failed at `:app:verifyManifestGuardDebug` with:

```
Manifest guard failed for processDebugMainManifest:
  - Permission android.permission.INTERNET is in the merged manifest but not in the allow-list
```

The line was removed again (`git diff` on the manifest is empty). `./gradlew :app:bundleRelease :app:check --dry-run` lists `:app:verifyManifestGuardRelease` after `:app:processReleaseManifest` and before `:app:bundleRelease`, and `:app:verifyManifestGuardDebug` under `:app:check`.

**Design correction found while writing the allow-list.** The merged manifest already carried five permissions beyond the seven in Pillsner's own manifest, all contributed by libraries: `USE_BIOMETRIC` and `USE_FINGERPRINT` (androidx.biometric, app lock), `WAKE_LOCK` and `ACCESS_NETWORK_STATE` (androidx.work, the alarm watchdog) and `nl.hexmaster.pillsner.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (androidx.core). They shipped in every release so far. The allow-list discloses all twelve with their contributors; design D7 and the README are corrected accordingly. `INTERNET` is not among them.
