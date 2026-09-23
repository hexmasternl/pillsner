## 1. Dependency and manifest

- [ ] 1.1 Add `com.google.mlkit:text-recognition` (the bundled/on-device model artifact, not `-cloud` or a GMS-dependent variant) to the version catalog and the `app` module's dependencies.
- [ ] 1.2 Declare `android.permission.CAMERA` in the manifest and a `<uses-feature android:name="android.hardware.camera.any" android:required="false"/>` entry so install is unaffected on camera-less devices.
- [ ] 1.3 Add `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />` and the same for `ACCESS_NETWORK_STATE`, alongside the manifest's existing "no network permission" comment. `com.google.mlkit:text-recognition`'s transitive Firebase installations/datatransport dependencies declare both regardless of model variant, and this exact leak previously shipped in 1.1.0/1.1.1 and forced a revert (`088d4f5` / `fa6a11e`) — proactively stripping them here, the same way that fix did, is a known-necessary step, not a defensive guess.
- [ ] 1.4 After adding the dependency, inspect the merged manifest (`app/build/outputs/logs/manifest-merger-*-report.txt` or Android Studio's Merged Manifest tab) and confirm `INTERNET` and `ACCESS_NETWORK_STATE` are absent from the final build, catching any other transitive permission the artifact might also contribute.
- [ ] 1.5 Add the new dependency to the README's disclosed-dependencies section.

## 2. Domain: label text parsing

- [ ] 2.1 Add a `LabelScanResult` domain model (candidate name, optional candidate dose amount + unit).
- [ ] 2.2 Implement `ParseMedicineLabelText(rawText: String): LabelScanResult` as a pure function in the domain layer, with no Android or ML Kit dependency: find the dose token first, by matching a `<number><space?><unit>` regex against the existing fixed unit list (mg, g, mcg, ml, tablet, capsule, drop, puff, unit) anywhere on the first non-empty line, then derive the name candidate from that same line with the matched dose token (and the punctuation/whitespace it leaves behind) stripped out.
- [ ] 2.3 Unit test `ParseMedicineLabelText` against representative label text: name + dose on one line (e.g. "Metoprolol 40 mg" → name "Metoprolol", dose "40"/"mg"), name only, no usable text, dose in an unrecognised unit, multi-line noisy label text.

## 3. Data/UI: capture, recognise, discard

- [ ] 3.1 Add a "Scan label" action to the Add medicine form UI, offering both camera capture and photo-picker entry points.
- [ ] 3.2 Wire the camera capture path through a `FileProvider` temp file created under `context.cacheDir/label-scan/` only (never external or persistent storage), and a runtime `CAMERA` permission request; on permission denial or no camera hardware, fall back to offering only the photo picker with an explanatory message.
- [ ] 3.3 Wire the photo-picker path via `ActivityResultContracts.PickVisualMedia` (no camera permission required).
- [ ] 3.4 Run ML Kit's on-device `TextRecognizer` against the resulting image, pass the recognised text to `ParseMedicineLabelText`, and prefill the form's mutable name/dose state with the result.
- [ ] 3.5 Delete any temp file created for the camera-capture path, and release the picked `Uri` reference, in a `finally` block so a photo is never retained regardless of recognition outcome.
- [ ] 3.6 Sweep `context.cacheDir/label-scan/` for leftover files on app start and at the beginning of every new scan, deleting anything found — the `finally` cleanup in 3.5 does not run if the process is killed or crashes mid-recognition, so this sweep is what actually bounds how long a temp file can survive.
- [ ] 3.7 Show a brief in-progress state while recognition runs, and a message when nothing usable was recognised.

## 4. Accessibility and review

- [ ] 4.1 Ensure "Scan label" has a spoken label and its in-progress/failure states are announced, per the form's existing accessibility contract.
- [ ] 4.2 Verify prefilled fields go through the exact same validation and error paths as manually typed values (no new validation branch).
- [ ] 4.3 Verify the form remains fully usable via TalkBack and at the largest font scale with "Scan label" present, per `pillsner-ui-review`.

## 5. Verification

- [ ] 5.1 Run domain unit tests (`ParseMedicineLabelText`) and the module's unit test task.
- [ ] 5.2 Run lint.
- [ ] 5.3 Manually verify on a device/emulator: scan with a real label photo, scan with no usable text, decline camera permission, and use the photo picker on a camera-less emulator profile.
- [ ] 5.4 Instrumented test: a file left in `context.cacheDir/label-scan/` (simulating a crash mid-recognition) is removed by the startup/next-scan sweep, and no such file is ever created outside that cache subdirectory.
- [ ] 5.5 Build a release variant and confirm the merged manifest (per 1.4) still excludes `INTERNET` and `ACCESS_NETWORK_STATE`.
