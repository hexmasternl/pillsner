## 1. Dependency and permission setup

- [ ] 1.1 Add `com.google.mlkit:text-recognition` (bundled Latin model) to `gradle/libs.versions.toml` and `app/build.gradle.kts`; confirm no unbundled/Play-services text-recognition dependency is present
- [ ] 1.2 Add the `CAMERA` permission to the app manifest, requested only at scan time (no install-time prompt)
- [ ] 1.3 Add a `CAMERA` row to the README permissions table describing it as optional, for the label-scan shortcut only

## 2. Domain: recognized-text parsing

- [ ] 2.1 Define `LabelScanResult` (nullable name, nullable amount+unit) in the domain layer
- [ ] 2.2 Implement `ParseLabelText`: a pure, Android-free function mapping raw recognized text + current app language to a `LabelScanResult`
- [ ] 2.3 Build the per-language unit-word vocabulary (mg, g, mcg, ml, tablet, capsule, drop, puff, unit and their Dutch/German/French/Spanish/Portuguese equivalents) alongside the existing `DoseUnit` enum
- [ ] 2.4 Unit tests for `ParseLabelText`: clear name only, name + valid dose, no usable text, ambiguous/garbled text, one case per supported language

## 3. Recognition service

- [ ] 3.1 Implement `LabelTextRecognizer` wrapping the ML Kit `TextRecognizer` client, taking an in-memory image and returning raw recognized text or nothing
- [ ] 3.2 Ensure the image is never written to app-controlled storage and is released after recognition completes (success or failure)
- [ ] 3.3 Wire `LabelTextRecognizer` and `ParseLabelText` into `AppContainer.kt` the same way existing services are wired

## 4. UI: capture and review flow

- [ ] 4.1 Add the "Scan label" action to `MedicationFormScreen.kt`, visible only in add mode
- [ ] 4.2 Add the camera-permission request flow for the action, following the pattern in `ui/home/NotificationPermissionRequest.kt`
- [ ] 4.3 Wire camera capture (`ActivityResultContracts.TakePicture` or equivalent) with fallback to the system photo picker (`ActivityResultContracts.PickVisualMedia`) on denial, no camera, or explicit "choose photo" preference
- [ ] 4.4 Handle cancellation from either path with no change to form state
- [ ] 4.5 Show a brief in-progress state while recognition runs, and a plain "couldn't read that photo" message when nothing usable is found
- [ ] 4.6 Run `pillsner-ui-review` on the new UI before marking this group done

## 5. ViewModel and form state wiring

- [ ] 5.1 Add `applyScanResult(LabelScanResult)` (or equivalent) to `MedicationFormViewModel`, writing into the existing `name`, `doseText` and `doseUnit` draft fields
- [ ] 5.2 Ensure prefilled dose/unit values flow through the exact same validation as typed input (no bypass of `MedicationFormUiState`'s existing error rules)
- [ ] 5.3 Ensure `hasEdits`/discard-draft behavior treats a scan-driven prefill the same as a manual edit (leaving the form after a scan asks to discard, same as after typing)
- [ ] 5.4 Ensure the draft (including any scan-derived prefill already applied to the fields) survives rotation and process death, consistent with the existing `medicine-add` requirement — no new state to persist beyond the fields it already writes to

## 6. Strings and localization

- [ ] 6.1 Add all new user-facing strings (action label, permission rationale if any, in-progress and not-recognized messages) to `values/strings.xml` and every `values-*` locale
- [ ] 6.2 Verify the unit-word vocabulary (task 2.3) covers realistic label wording for each of the six languages, not just direct translations of the English word list

## 7. Tests and verification

- [ ] 7.1 Compose semantics/UI test covering: action visible in add mode only, permission denial falls back to picker, cancel leaves form untouched
- [ ] 7.2 Manual test case: scan a real label photo per supported language and confirm sensible prefill or a graceful "not recognized" outcome
- [ ] 7.3 Confirm via code review that no path writes the captured image to disk or to the saved medicine/draft
- [ ] 7.4 Run unit tests, lint, and `pillsner-ui-review` before considering the change complete
