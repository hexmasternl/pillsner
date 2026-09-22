## 1. Dependency and manifest

- [ ] 1.1 Add `com.google.mlkit:text-recognition` to the version catalog and the `app` module's dependencies.
- [ ] 1.2 Declare `android.permission.CAMERA` in the manifest and a `<uses-feature android:name="android.hardware.camera.any" android:required="false"/>` entry so install is unaffected on camera-less devices.
- [ ] 1.3 Add the new dependency to the README's disclosed-dependencies section.

## 2. Domain: label text parsing

- [ ] 2.1 Add a `LabelScanResult` domain model (candidate name, optional candidate dose amount + unit).
- [ ] 2.2 Implement `ParseMedicineLabelText(rawText: String): LabelScanResult` as a pure function in the domain layer, with no Android or ML Kit dependency, matching dose tokens against the existing fixed unit list (mg, g, mcg, ml, tablet, capsule, drop, puff, unit).
- [ ] 2.3 Unit test `ParseMedicineLabelText` against representative label text: name + dose on one line, name only, no usable text, dose in an unrecognised unit, multi-line noisy label text.

## 3. Data/UI: capture, recognise, discard

- [ ] 3.1 Add a "Scan label" action to the Add medicine form UI, offering both camera capture and photo-picker entry points.
- [ ] 3.2 Wire the camera capture path through a `FileProvider` temp file and runtime `CAMERA` permission request; on permission denial or no camera hardware, fall back to offering only the photo picker with an explanatory message.
- [ ] 3.3 Wire the photo-picker path via `ActivityResultContracts.PickVisualMedia` (no camera permission required).
- [ ] 3.4 Run ML Kit's on-device `TextRecognizer` against the resulting image, pass the recognised text to `ParseMedicineLabelText`, and prefill the form's mutable name/dose state with the result.
- [ ] 3.5 Delete any temp file created for the camera-capture path, and release the picked `Uri` reference, in a `finally` block so a photo is never retained regardless of recognition outcome.
- [ ] 3.6 Show a brief in-progress state while recognition runs, and a message when nothing usable was recognised.

## 4. Accessibility and review

- [ ] 4.1 Ensure "Scan label" has a spoken label and its in-progress/failure states are announced, per the form's existing accessibility contract.
- [ ] 4.2 Verify prefilled fields go through the exact same validation and error paths as manually typed values (no new validation branch).
- [ ] 4.3 Verify the form remains fully usable via TalkBack and at the largest font scale with "Scan label" present, per `pillsner-ui-review`.

## 5. Verification

- [ ] 5.1 Run domain unit tests (`ParseMedicineLabelText`) and the module's unit test task.
- [ ] 5.2 Run lint.
- [ ] 5.3 Manually verify on a device/emulator: scan with a real label photo, scan with no usable text, decline camera permission, and use the photo picker on a camera-less emulator profile.
