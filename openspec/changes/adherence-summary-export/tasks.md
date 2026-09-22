## 1. Domain: report model and builder

- [ ] 1.1 Add an `AdherenceReport` domain data class (medicine name, schedule text, period label, scheduled/taken/skipped/missed/unanswered counts, adherence percentage, optional per-dose rows) with no Android dependency
- [ ] 1.2 Add an `AdherenceReportBuilder` that maps the Usage history screen's existing state (figures, breakdown, per-dose data) plus an "include doses" flag into an `AdherenceReport`
- [ ] 1.3 Unit test `AdherenceReportBuilder`: summary-only vs. with-doses mapping, figures matching the source state exactly, empty-doses list when detail is off

## 2. PDF export

- [ ] 2.1 Add `PdfExportRenderer` that draws an `AdherenceReport` onto one or more `PdfDocument.Page` canvases: header (medicine, schedule, period, generated-on date), summary figures, breakdown, and a paginated per-dose table when included
- [ ] 2.2 Write the rendered `PdfDocument` to `context.cacheDir/exports/pillsner-report-<timestamp>.pdf`
- [ ] 2.3 Unit/instrumented test: PDF generation succeeds for a summary-only report, a report with a small dose list, and a report large enough to require pagination

## 3. CSV export

- [ ] 3.1 Add `CsvExportWriter` that writes the same `AdherenceReport` as a header row plus summary figures, and per-dose rows when included, to `context.cacheDir/exports/pillsner-report-<timestamp>.csv`
- [ ] 3.2 Unit test `CsvExportWriter` output for summary-only and with-doses reports, including correct escaping of any comma or quote in a medicine name or schedule text

## 4. Sharing

- [ ] 4.1 Add a `FileProvider` entry to the manifest and `res/xml/file_paths.xml` scoped to the `exports/` cache subdirectory only
- [ ] 4.2 Build and launch an `ACTION_SEND` intent with `FLAG_GRANT_READ_URI_PERMISSION` for the generated file's `content://` URI, through a chooser with a string-resource title
- [ ] 4.3 Instrumented test: sharing exposes only the generated file's URI and no other app-private path is reachable through the granted permission

## 5. UI: export entry point and dialog

- [ ] 5.1 Add a "Share with your doctor" action to the Usage history screen's top app bar, visible only when figures (not the empty state) are shown, using a string resource label
- [ ] 5.2 Add the export choice dialog: format selection (PDF preselected, CSV secondary) and an "Include individual doses" switch defaulting off
- [ ] 5.3 Wire the dialog's confirm action to build the `AdherenceReport`, generate the chosen file format, and launch the share intent
- [ ] 5.4 Ensure the action and dialog meet accessibility requirements: spoken labels, large-font layout, one-handed reachability, consistent with the rest of the Usage history screen

## 6. Verification

- [ ] 6.1 Run `pillsner-ui-review` against the new top app bar action and dialog
- [ ] 6.2 Run unit tests, lint, and instrumented tests for the sharing/file-provider path
- [ ] 6.3 Manually verify: export with and without dose detail, PDF and CSV, on a device with no app installed that can open a PDF (share sheet still offers "Save to Files" / print) and on one that can
