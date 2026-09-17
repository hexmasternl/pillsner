## 1. Dependency and permission setup

- [ ] 1.1 Add `com.google.mlkit:text-recognition` (bundled Latin model) to `gradle/libs.versions.toml` and `app/build.gradle.kts`; confirm no unbundled/Play-services text-recognition dependency is present
- [ ] 1.2 Add the `CAMERA` permission to the app manifest, requested only at scan time (no install-time prompt), plus a `FileProvider` entry (and its `file_paths.xml` resource) scoped to a cache subdirectory for the temporary capture file
- [ ] 1.3 Add a `CAMERA` row to the README permissions table describing it as optional, for the label-scan shortcut only

## 2. Domain: recognized-text parsing

- [ ] 2.1 Define `LabelScanResult` (nullable name, nullable amount+unit) in the domain layer
- [ ] 2.2 Implement `ParseLabelText`: a pure, Android-free function mapping raw recognized text + current app language to a `LabelScanResult`
- [ ] 2.3 Build the per-language unit-word vocabulary (mg, g, mcg, ml, tablet, capsule, drop, puff, unit and their Dutch/German/French/Spanish/Portuguese equivalents) alongside the existing `DoseUnit` enum
- [ ] 2.4 Unit tests for `ParseLabelText`: clear name only, name + valid dose, no usable text, ambiguous/garbled text, one case per supported language

## 3. Recognition service

- [ ] 3.1 Implement `LabelTextRecognizer` wrapping the ML Kit `TextRecognizer` client, taking an in-memory image and returning raw recognized text or nothing
- [ ] 3.2 Ensure the temporary capture file (from the camera intent) is deleted immediately after recognition completes, on every outcome including failure; a photo picked from the gallery is read directly with no file written at all
- [ ] 3.3 Wire `LabelTextRecognizer` and `ParseLabelText` into `AppContainer.kt` the same way existing services are wired

## 4. Navigation: carrying a scan result into a fresh Add medicine form

- [ ] 4.1 Add `scannedName`, `scannedDoseAmount` and `scannedDoseUnit` as optional fields on the `MedicationFormGraph` route, alongside its existing `medicationId`
- [ ] 4.2 Update `MedicationFormViewModel`'s add-mode initial state construction to seed `name`, `doseText` and `doseUnit` from those fields when present, leaving edit mode (`medicationId != null`) entirely unaffected
- [ ] 4.3 Confirm the seeded values behave as ordinary draft state: they survive rotation/process death via the existing route-argument mechanism, go through the same validation as typed input, and are subject to the existing discard-draft confirmation the same as a manual edit

## 5. UI: scan button and capture flow on the Medicines screen

- [ ] 5.1 Add the "Scan medicine label" icon button to `MedicinesScreen.kt`, grouped with the existing add FAB, with its own content description and test tag
- [ ] 5.2 Add the camera-permission request flow for the button, following the pattern in `ui/home/NotificationPermissionRequest.kt`
- [ ] 5.3 Wire camera capture (`ActivityResultContracts.TakePicture` via the new `FileProvider` Uri) with fallback to the system photo picker (`ActivityResultContracts.PickVisualMedia`) on denial, no camera, or explicit "choose photo" preference
- [ ] 5.4 Handle cancellation from either path with no navigation and no message left on the Medicines screen
- [ ] 5.5 Show a brief in-progress state while recognition runs, then navigate to `MedicationFormGraph` with the recognized values (or none) and, when nothing was recognized, a message stating the photo could not be read
- [ ] 5.6 Run `pillsner-ui-review` on the new UI before marking this group done

## 6. Strings and localization

- [ ] 6.1 Add all new user-facing strings (button content description, in-progress and not-recognized messages) to `values/strings.xml` and every `values-*` locale
- [ ] 6.2 Verify the unit-word vocabulary (task 2.3) covers realistic label wording for each of the six languages, not just direct translations of the English word list

## 7. Tests and verification

- [ ] 7.1 Compose semantics/UI test covering: scan button visible and reachable on the Medicines screen, permission denial falls back to the picker, cancelling leaves the Medicines screen untouched
- [ ] 7.2 UI test: a fake `LabelTextRecognizer`/`ParseLabelText` result opens the Add medicine form prefilled with the expected name/dose/unit, in add mode, and every field stays editable
- [ ] 7.3 UI test: nothing recognized opens the Add medicine form with its normal empty defaults and the not-recognized message
- [ ] 7.4 Manual test case: scan a real label photo per supported language and confirm sensible prefill or a graceful "not recognized" outcome
- [ ] 7.5 Confirm via code review that no path writes the captured image anywhere beyond the deleted temporary file, and that no image ever reaches the saved medicine/draft
- [ ] 7.6 Run unit tests, lint, and `pillsner-ui-review` before considering the change complete
