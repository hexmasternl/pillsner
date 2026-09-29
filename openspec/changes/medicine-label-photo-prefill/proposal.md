## Why

**GitHub Issue:** #85 (https://github.com/hexmasternl/pillsner/issues/85)
**Pull Request:** #86 (https://github.com/hexmasternl/pillsner/pull/86)

Adding a medicine means typing its name, its strength, how often to take it and the dates by hand, which is the slowest and most error-prone moment in Pillsner, especially for people managing several medicines. A photo of the pharmacy label or the box already carries most of that, so reading it on the phone and pre-filling the Add medicine form removes the typing without sending anything off the device.

This feature was built once before (`medicine-add-label-scan`, issue #33) on ML Kit. That build pulled Firebase and Google Play services components into the app through transitive dependencies, silently added `INTERNET` and `ACCESS_NETWORK_STATE` to the shipped manifest, and produced a package that was not accepted as safe to install and crashed at launch on devices where it did install (releases 1.1.0 and 1.1.1). Stripping the permissions (#46) did not fix it, and `main` was reverted to 1.0.2 (#47). The feature is therefore designed again from scratch here, with the failure mode of the first attempt as the primary design constraint: the text recogniser runs entirely inside the app, comes from a library with no Google Play services or Firebase surface, and the build refuses to produce a package whose permissions differ from the ones Pillsner discloses.

## What Changes

- **A "Scan a label" action on the Add medicine form** (add mode only). It opens a live scanning screen inside Pillsner, or lets the user pick an existing photo.
- **Live capture, no shutter needed.** The scanning screen shows the camera preview with a framing guide. Frames are read continuously and recognised on the device; as soon as two consecutive frames agree on a medicine name and at least a dose or a schedule, the scan accepts itself, vibrates briefly, and returns to the form. A shutter button is still there for anyone who prefers to decide the moment themselves, and a torch toggle for dim kitchens. No photo file is ever written: frames stay in memory and are discarded as they are analysed.
- **One new permission, requested honestly.** The live viewfinder needs the `CAMERA` permission. It is declared in the manifest, requested only when the user first opens the scanning screen, and explained in a rationale before the system prompt. Declining leaves the whole app, including "Choose a photo", exactly as usable as before. The camera hardware feature is declared as not required, so Pillsner still installs on devices without a camera. This is the only permission this change adds; the build guard below makes sure no other one comes along.
- **Fully offline text recognition inside the app.** The Tesseract OCR engine (via the Tesseract4Android library, Apache 2.0, pure native code, no Google dependencies) and its trained data ship inside the app package. Nothing is downloaded at install or at first use, and no network permission is declared.
- **Interpretation of the recognised text into form fields**, as a pure domain function with no Android dependency, that pre-fills:
  - the medicine **name**;
  - the **default dose** (the strength on the label, such as "50 mg");
  - the **prescribed dose as a schedule** (such as "1 tablet twice a day" becoming one schedule of 1 tablet at 08:00 and 20:00), using the app's existing schedule shapes;
  - **used since** (the most recent past date on the label, otherwise today);
  - **use until** (an explicit end date, or a course length such as "for 7 days" counted from used since, otherwise left empty).
- **Review before save, always.** Pre-filled values land in the ordinary editable form, an attention banner says the fields came from a scan and must be checked, and the recognised text can be shown so the user can see what was read. Nothing is saved until the user taps Save, with the form's existing validation.
- **Nothing is retained.** Camera frames are never written to disk. A picked photo is read once and never copied. The recognised text lives only in the form's draft and is never logged.
- **A build-time permission guard.** A Gradle verification task, run in CI and before every release build, fails the build if the merged manifest's permission set differs from the disclosed allow-list, if any dependency from the Firebase, ML Kit, Google data-transport or non-wearable Play services groups is on the classpath, or if the OCR library's artifact checksum differs from the pinned one. This is what turns "the change must be very clear about permissions" into something the repository enforces rather than promises.
- **Explicit permission and manifest accounting.** Declared permissions: the existing ones plus `CAMERA` (writing the allow-list showed the existing set is twelve, not seven: five arrive from libraries through manifest merging and have shipped since 1.0.0; see design D7). Manifest additions: the `CAMERA` permission and a `uses-feature` for `android.hardware.camera.any` with `required="false"`. Nothing else: no provider, no queries entry, no service. The README permission table gains a `CAMERA` row.
- Adds the CameraX AndroidX libraries (core, camera2, lifecycle, compose) and one third-party library (Tesseract4Android) from one added build repository (JitPack, restricted to that one group). All are named, justified and disclosed; see the design. The CameraX ML Kit bridge artifact is explicitly forbidden by the guard.
- Supersedes issue #33 and removes the un-applied `medicine-label-ocr-scan` change, which re-proposed the ML Kit approach.

## Capabilities

### New Capabilities

- `medicine-label-scan`: live-scanning a medicine label with the in-app camera or reading a picked photo, recognising its text on the device, interpreting it into a name, default dose, schedule and dates, pre-filling the Add medicine form for review, handling the camera permission, and retaining nothing.
- `build-permission-guard`: the build refuses to produce an app package whose declared permissions, dependency groups or OCR artifact differ from what the repository discloses.

### Modified Capabilities

- `medicine-add`: the Add medicine form, in add mode, offers the "Scan a label" action and shows the review banner after a scan.

## Impact

- **Permissions**: adds `CAMERA` (runtime permission, requested on first use of the scanning screen with a rationale, never at install or app start). `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` and `INTERNET` stay undeclared; `ACCESS_NETWORK_STATE`, which WorkManager has merged in since 1.0.0, is now disclosed rather than added (design D7). One new runtime prompt, only when the user chooses to scan with the camera.
- **Manifest**: adds `<uses-permission android:name="android.permission.CAMERA"/>` and `<uses-feature android:name="android.hardware.camera.any" android:required="false"/>`. No provider, no queries entry.
- **Dependencies**: adds `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle` and `camera-compose` (AndroidX, Google Maven), and `cz.adaptech.tesseract4android:tesseract4android-openmp` from JitPack (settings-level repository with a content filter for that one group only). The guard asserts the dependency tree stays free of `com.google.firebase`, `com.google.mlkit`, `com.google.android.datatransport`, `androidx.camera:camera-mlkit-vision` and any `com.google.android.gms` artifact other than the wearable ones already in use.
- **Package size**: CameraX classes, native OCR libraries (per ABI, delivered by ABI split from the bundle) and one English tessdata_fast model of about 4 MB in assets. The exact per-device increase is measured during implementation and recorded in the design before the pull request opens.
- **Storage**: the trained data is copied once from assets to the app's private files directory on first scan; that directory is excluded from backup and device transfer. No image is ever written.
- **Code**: a new scanning screen and view model, a domain package for label interpretation (pure Kotlin, unit-tested across the six app languages), a data-layer frame recogniser wrapping Tesseract, form view model additions, new string resources in all six languages, a Gradle guard task, and a CI step.
- **Docs**: README permission table and Technology table, `CHANGELOG.md`; the design records what the first attempt did wrong and why this one cannot repeat it.
- **Privacy**: no network access; frames, photos and recognised text never leave the device, are never written and never logged.
