**GitHub Issue:** #34 (https://github.com/hexmasternl/pillsner/issues/34)

## Why

The Usage history screen already computes adherence, the taken/skipped/missed/unanswered breakdown and the outcome and timing-accuracy charts, but that data is trapped on the device's screen. A person bringing their adherence to a doctor or pharmacist appointment today has no way to hand it over except reading numbers off their phone. Turning what is already shown into a document the user can print, save or share keeps Pillsner's on-device promise intact — no account, no server, the file only moves if and where the user explicitly sends it through the Android share sheet.

## What Changes

- Add a **"Share with your doctor"** action to the Usage history screen's top app bar, available for the period currently selected on screen.
- Generate a PDF report on-device containing: the medicine's name and its schedule in plain language, the adherence percentage and the taken/skipped/missed/unanswered breakdown for the selected period, matching the figures already shown on screen exactly (a formatted export of existing numbers, not a new calculation).
- Offer an optional **"Include individual doses"** toggle in the share sheet-launching dialog; when on, the PDF gains a dose-by-dose appendix (date/time due, time answered, outcome) for the same period. Off by default, since it is more sensitive detail than the summary.
- Also offer a **CSV** export of the same dose-level data as a secondary, raw option in the same dialog, for a recipient who wants to import the numbers elsewhere rather than read a formatted document.
- Hand the generated file to the standard Android share sheet (`ACTION_SEND` with a `FileProvider` content URI) so the user picks where it goes — print, save, or share to a specific app. Pillsner itself never transmits the file.
- The generated file is written to app-private cache storage and is safe to regenerate on every share; it is not treated as data the user manages or that survives an app data reset.

## Capabilities

### New Capabilities
- `adherence-summary-export`: on-device generation of a PDF and CSV adherence report for a medicine's usage history over a selected period, and handing it to the Android share sheet.

### Modified Capabilities
- `medicine-usage-history`: the Usage history screen's top app bar gains a "Share with your doctor" action that opens the export flow described above, scoped to the medicine and period currently on screen.

## Impact

- New PDF generation code path using Android's first-party `android.graphics.pdf.PdfDocument` — no third-party PDF library, since the report is single-page, text-and-simple-shapes content well within what `PdfDocument` draws directly onto a `Canvas`.
- New `FileProvider` entry in the manifest and its `res/xml/file_paths.xml`, scoped to a dedicated cache subdirectory, so a shared file can be exposed to another app without granting broader file access.
- Reuses the existing adherence, breakdown and per-dose data already loaded by the Usage history view model; no new repository queries beyond what that screen already reads for its period.
- No new permissions: sharing a file via `FileProvider` and `ACTION_SEND` needs none, and the app lock already gates reaching the Usage history screen in the first place, so the export action inherits that protection without any change to `app-lock`.
