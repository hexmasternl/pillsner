## Why

Adding a medicine means typing its name, its strength, how often to take it and the dates by hand, which is the slowest and most error-prone moment in Pillsner, especially for people managing several medicines. A photo of the pharmacy label or the box already carries most of that, so reading it on the phone and pre-filling the Add medicine form removes the typing without sending anything off the device.

This feature was built once before (`medicine-add-label-scan`, issue #33) on ML Kit. That build pulled Firebase and Google Play services components into the app through transitive dependencies, silently added `INTERNET` and `ACCESS_NETWORK_STATE` to the shipped manifest, and produced a package that was not accepted as safe to install and crashed at launch on devices where it did install (releases 1.1.0 and 1.1.1). Stripping the permissions (#46) did not fix it, and `main` was reverted to 1.0.2 (#47). The feature is therefore designed again from scratch here, with the failure mode of the first attempt as the primary design constraint: the text recogniser runs entirely inside the app, comes from a library with no Google Play services or Firebase surface, and the build refuses to produce a package whose permissions differ from the ones Pillsner discloses.

## What Changes

- **A "Scan a label" action on the Add medicine form** (add mode only). It offers to take a photo with the phone's own camera app or to pick an existing photo. Pillsner never opens the camera itself, so it declares and requests **no camera permission** and no photo or storage permission.
- **Fully offline text recognition inside the app.** The Tesseract OCR engine (via the Tesseract4Android library, Apache 2.0, pure native code, no Google dependencies) and its trained data ship inside the app package. Nothing is downloaded at install or at first use, and no network permission is declared.
- **Interpretation of the recognised text into form fields**, as a pure domain function with no Android dependency, that pre-fills:
  - the medicine **name**;
  - the **default dose** (the strength on the label, such as "50 mg");
  - the **prescribed dose as a schedule** (such as "1 tablet twice a day" becoming one schedule of 1 tablet at 08:00 and 20:00), using the app's existing schedule shapes;
  - **used since** (the most recent past date on the label, otherwise today);
  - **use until** (an explicit end date, or a course length such as "for 7 days" counted from used since, otherwise left empty).
- **Review before save, always.** Pre-filled values land in the ordinary editable form, an attention banner says the fields came from a photo and must be checked, and the recognised text can be shown so the user can see what was read. Nothing is saved until the user taps Save, with the form's existing validation.
- **The photo is never kept.** A camera capture goes to a temporary file in the app's cache that is deleted as soon as recognition ends; a picked photo is read once and never copied. Leftovers from a crash are swept on the next start and the next scan.
- **A build-time permission guard.** A Gradle verification task, run in CI and before every release build, fails the build if the merged manifest's permission set differs from the disclosed list, if any dependency from the Firebase, ML Kit, Google data-transport or non-wearable Play services groups is on the classpath, or if the OCR library's artifact checksum differs from the pinned one. This is what turns "the change must be very clear about permissions" into something the repository enforces rather than promises.
- **Explicit permission and manifest accounting.** The set of declared permissions is unchanged by this change. The manifest gains exactly two non-permission entries: a `<queries>` intent so the app can honestly tell whether a camera app is installed, and a `FileProvider` (not exported) so the camera app can write one photo into Pillsner's cache. The README permission section is updated to say so.
- Adds one third-party library (Tesseract4Android) and one build repository (JitPack, restricted to that one group). Both are named, justified and disclosed; see the design.
- Supersedes issue #33 and removes the un-applied `medicine-label-ocr-scan` change, which re-proposed the ML Kit approach.

## Capabilities

### New Capabilities

- `medicine-label-scan`: taking or picking a photo of a medicine label, recognising its text on the device, interpreting it into a name, default dose, schedule and dates, pre-filling the Add medicine form for review, and discarding the photo.
- `build-permission-guard`: the build refuses to produce an app package whose declared permissions, dependency groups or OCR artifact differ from what the repository discloses.

### Modified Capabilities

- `medicine-add`: the Add medicine form, in add mode, offers the "Scan a label" action and shows the review banner after a scan.

## Impact

- **Permissions**: no permission added, removed or changed. `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` and `INTERNET` all stay undeclared. No new runtime permission prompt from Pillsner.
- **Manifest**: adds a `<queries>` entry for `android.media.action.IMAGE_CAPTURE` and one `androidx.core.content.FileProvider` with `exported="false"` and a path list limited to the cache subdirectory used for scans.
- **Dependencies**: adds `cz.adaptech.tesseract4android:tesseract4android-openmp` from JitPack (settings-level repository with a content filter for that one group only). No other new artifact. The guard task asserts that the dependency tree stays free of `com.google.firebase`, `com.google.mlkit`, `com.google.android.datatransport` and any `com.google.android.gms` artifact other than the wearable ones already in use.
- **Package size**: native OCR libraries (per ABI, delivered by ABI split from the bundle) plus one English tessdata_fast model of about 4 MB in assets. The exact per-device increase is measured during implementation and recorded in the design before the pull request opens.
- **Storage**: the trained data is copied once from assets to the app's private files directory on first scan; that directory is excluded from backup and device transfer. The scan cache subdirectory is temporary.
- **Code**: new domain package for label interpretation (pure Kotlin, unit-tested across the six app languages), a data-layer recogniser wrapping Tesseract, capture plumbing in the form screen, form view model additions, new string resources in all six languages, a Gradle guard task, and a CI step.
- **Docs**: README permission section and `CHANGELOG.md`; the design records what the first attempt did wrong and why this one cannot repeat it.
- **Privacy**: no network access; the photo and the recognised text never leave the device and are never logged.
