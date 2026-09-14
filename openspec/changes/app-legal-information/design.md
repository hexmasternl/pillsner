## Context

Settings is a `LazyColumn` of self-contained section composables (`LanguageSection`, `SecuritySection`) in `ui/settings/SettingsScreen.kt`; adding a section means adding one `item`. Secondary screens (`PinSetup`, the medicine form flow) are non-top-level destinations in the single `NavHost` in `ui/PillsnerApp.kt`, and the navigation suite already hides itself on any route that is not one of the three top-level destinations, so a new secondary destination costs nothing extra.

The add-medicine entrance is one lambda: `MedicinesScreen(onAddMedicine = { navController.navigate(MedicationFormGraph()) })` in `PillsnerApp.kt`. That single call site is the whole gate.

Scalar settings live in the general preferences DataStore (`data/settings/SettingsDataStore.kt`), which already holds the language choice and is deliberately separate from the app lock's file. `DataStoreLanguageRepository` is the pattern: a domain repository interface, a DataStore-backed implementation, an absent value meaning "never chosen".

Constraints from `CLAUDE.md`: the domain layer has no Android dependencies; user-facing text lives in string resources with a complete Dutch translation or lint fails; every visual decision comes from a theme token in `docs/design-system.md`; nothing leaves the device; MVVM with state down and events up.

A related change, `app-about-screen`, is in flight and adds an About section to Settings. The two are independent: Legal adds its own section and its own routes and touches no file the other rewrites, beyond adding one `item` to the same `LazyColumn`. Whichever lands second adds its section after the other.

## Goals / Non-Goals

**Goals:**

- The user is told, in plain language and before the first medicine exists, what Pillsner is not: not a medical device, not a source of medical advice, and not something to rely on as the only reminder.
- Responsibility is stated where it belongs: the schedule, the dose and the frequency are the user's and their prescriber's; the app, its publisher and its developers carry none of it.
- Acceptance is explicit, deliberate, recorded, and cannot be reached by accident or by a stray tap.
- The documents stay readable from Settings forever, so the user never has to remember what they agreed to.
- A revision of either document re-asks, without disturbing anything the user already has.
- Nothing new for the privacy promise to account for: no permission, no network, no link out, no dependency.

**Non-Goals:**

- A privacy policy, an open-source licence list, a cookie or tracking notice, an age gate, or anything to sign or export. Each is a separate proposal.
- Fetching or updating the documents over the network. The text ships with the build; a revision ships with a new build.
- Gating anything except adding a medicine. Reading, editing, deactivating and deleting existing medicines, reminders, intake recording, Home, Settings and the watch are untouched.
- Blocking the app at launch. A user who never adds a medicine is never interrupted.
- Legal advice. This change fixes the structure and a drafted text; a lawyer reviewing the wording later changes only the string resources and the document version.

## Decisions

### D1. Two documents, one shape, versioned integers

`domain/legal/LegalDocument.kt`:

```kotlin
enum class LegalDocumentId { DISCLAIMER, TERMS }

data class LegalDocument(
    val id: LegalDocumentId,
    val version: Int,
    val effectiveDate: LocalDate,
    val sections: List<LegalSection>,
)

data class LegalSection(val heading: TextRef, val paragraphs: List<TextRef>)
```

`TextRef` is a plain `value class TextRef(val resourceId: Int)` — an `Int` identifier, not a framework type, so the domain stays Android-free while the UI resolves it with `stringResource`. The domain never holds prose.

Versions are integers, starting at 1 for both documents, declared in one place (`domain/legal/CurrentLegalDocuments.kt`) next to the effective dates. Bumping a version is a one-line edit beside the text it describes, which is the only way it will actually happen when the text changes.

Alternative considered: a single combined document. Rejected: the disclaimer is the part the user must read before adding a medicine and is short enough to be read; the terms are longer, conventional, and belong one tap away. Merging them would either bury the disclaimer or make the gate unreadable.

Alternative considered: semantic version strings. Rejected: nothing needs ordering finer than "newer than what was accepted", and an integer makes that a comparison rather than a parse.

### D2. The text lives in string resources, structured as sections

Each section heading and each paragraph is its own string key (`legal_disclaimer_s1_heading`, `legal_disclaimer_s1_p1`, and so on). The document object lists them in order.

This keeps the repository rule (no inline user-facing text), keeps lint's missing-translation check working over the whole legal text, and lets the screen render headings as real headings, which a screen reader needs to navigate a long document.

Alternative considered: a Markdown or HTML file per language in `res/raw/` or `assets/`. Rejected: it escapes the missing-translation lint check — the one mechanism that guarantees a Dutch user is not shown English terms — and needs a renderer or a parser for prose that has no formatting beyond headings and paragraphs.

### D3. Acceptance: one record, in the general settings DataStore

`domain/legal/LegalAcceptance.kt`:

```kotlin
data class LegalAcceptance(
    val disclaimerVersion: Int,
    val termsVersion: Int,
    val acceptedAt: Instant,
)
```

`domain/repository/LegalRepository.kt` exposes `observeAcceptance(): Flow<LegalAcceptance?>` and `suspend fun accept(disclaimerVersion: Int, termsVersion: Int)`. `data/settings/DataStoreLegalRepository.kt` stores three preference keys in the existing `settingsDataStore`, mirroring `DataStoreLanguageRepository`. Null means never accepted — the same reading a fresh install gets.

It goes in the general settings file, not the app lock's: it is not security material, and a user restoring a backup onto a new phone has genuinely already accepted these terms, so carrying the record along is correct rather than a leak.

Acceptance is a single write of all three values, so a record can never say a version was accepted at no particular time.

Alternative considered: a Room table of acceptances, one row per acceptance event, as an audit trail. Rejected: nothing reads history, it drags a schema migration and a DAO into a three-value setting, and an on-device log nobody can produce in evidence is not an audit trail.

### D4. IsLegalAccepted: one use case decides

```kotlin
class IsLegalAccepted(
    private val repository: LegalRepository,
    private val documents: LegalDocuments,
) {
    operator fun invoke(): Flow<Boolean>
}
```

True when a record exists and both stored versions are greater than or equal to the current versions. Greater-than-or-equal rather than equal, so a downgraded build does not re-ask a user who accepted a newer text.

A single, Android-free predicate, unit-tested against every combination: no record, both current, one stale, both stale, both newer. The UI asks this one question and nothing else reasons about versions.

### D5. The gate is the add-medicine entrance, and only that

In `PillsnerApp.kt` the add lambda becomes a decision:

```kotlin
onAddMedicine = {
    if (legalAccepted) navController.navigate(MedicationFormGraph())
    else navController.navigate(AcceptLegal)
}
```

`legalAccepted` comes from a `LegalViewModel` collected at the `NavHost` level, so the decision is made against the current value at the moment of the tap rather than a value captured earlier.

Accepting on the acceptance screen records the acceptance and then navigates to `MedicationFormGraph()`, popping the acceptance screen off the back stack, so back from the form goes to Medicines and never back into the gate. Backing out of the gate returns to Medicines with nothing written.

Alternative considered: gating inside `MedicationFormViewModel`, so every route into the form is covered. Rejected: the form is one flow with two entrances, and the other entrance is editing a medicine that already exists — which must never be blocked, or a user could be locked out of their own data by a revised text. Gating at the add entrance says exactly what the requirement says.

Alternative considered: gating at app launch, before anything. Rejected: it turns a first launch into a legal interstitial, and the requirement is acceptance before a medicine is added, not before the app is seen.

### D6. The acceptance screen

A non-top-level destination, `@Serializable data object AcceptLegal`, with a `TopAppBar` titled "Before you add a medicine" and a back arrow (design system 8.7).

Content, in a scrolling `Column` constrained to `Spacing.contentMaxWidth`:

1. The **disclaimer in full**, rendered by the same `LegalDocumentBody` composable the document screen uses — headings in `titleMedium` with `semantics { heading() }`, paragraphs in `bodyLarge`, `Spacing.lg` between paragraphs and `Spacing.xl` above each heading.
2. A `TextButton` reading "Read the Terms of Service", which opens the terms in the document screen. Coming back leaves the acceptance screen exactly as it was.
3. A single filled `Button`, full width, minimum 56 dp, labelled "I understand and accept", pinned below the scrolling content so it is always reachable (design system 8.4, 8.11).

There is no decline button: the back arrow and system back are the decline, and they return to Medicines. A second button that means "do not add a medicine" would be a button whose only function is to undo the tap that opened the screen.

The accept button is **not** enabled until the disclaimer has been scrolled to its end. A user who accepts without the text ever having been on screen has not been told anything, which is the whole point of the gate. When the text fits without scrolling, it is already at its end and the button is enabled at once. The disabled button carries a supporting line explaining why, so the state is never a mystery.

Alternative considered: a checkbox reading "I have read and understood" above the button. Rejected: it adds a tap that means the same thing as the button beneath it, and scroll-to-end is the stronger evidence that the text was actually presented.

Alternative considered: an `AlertDialog`. Rejected: design system 8.12 puts a `bodyLarge` body and at most two actions in a dialog; a document that must be scrolled to its end is a screen.

### D7. The document screen

`@Serializable data class LegalDocumentRoute(val document: LegalDocumentId)` — the route carries which document, so process death restores the right one with nothing to rebuild.

A `Scaffold` with a `TopAppBar` carrying the document's title and a back arrow, and `LegalDocumentBody` in a scrolling column, with the effective date and version shown once beneath the title in `bodySmall` `onSurfaceVariant`. Reached from the Settings Legal section and from the acceptance screen's terms link; back returns to wherever it came from.

No view model: the content is constant for the process and comes from the route plus `CurrentLegalDocuments`. This is the same narrow departure from MVVM that a static screen justifies; the acceptance screen, which has state, has one.

### D8. The Settings Legal section

`ui/settings/legal/LegalSection.kt`: a `headlineSmall` header reading "Legal" marked as a heading, then two `ListItem` rows — "Disclaimer" and "Terms of Service" — each with an `ic_chevron_right` trailing icon, `clickable(role = Role.Button)` and `heightIn(min = Sizes.minTouchTarget)`, matching the row shape the Security section's "Change PIN" already uses so Settings stays visually one list.

Beneath them, one `bodySmall` line of status: when a current acceptance exists, "Accepted on <date>"; when the documents have been revised since, "Accepted on <date> — these documents have changed since"; when never accepted, "Not yet accepted". The date is formatted for the app language. The line is informational only — Settings offers no way to accept and no way to withdraw acceptance, because acceptance belongs to the moment a medicine is added and withdrawal would mean deleting data the user did not ask to lose.

Placed after Security, so the section order reads Language, Security, Legal.

### D9. Strings and translation

New keys in `values/strings.xml` and `values-nl/strings.xml`: both documents' headings and paragraphs, the two screen titles, the section header and rows, the acceptance screen's title, its scroll hint and its button, and the three status lines.

The Dutch translation is a real translation of the legal text, not a copy. The publisher's name (Eduard Keilholz) and the product name are not translated.

The drafted text says, in the **Disclaimer**: Pillsner is a reminder aid and not a medical device; it gives no medical advice; the user and their prescriber alone decide what is taken, how much, how often and when; a reminder may arrive late or not at all for reasons outside the app's control, so Pillsner must never be the only thing the user relies on; neither the app, its publisher nor its developers are responsible for a dose missed, taken late, doubled or taken wrongly, nor for any harm arising from use of the app; and questions about medication go to a doctor or pharmacist, emergencies to the emergency services.

In the **Terms of Service**: a personal, non-exclusive, revocable licence to use the app; provision "as is" without warranty of any kind; liability limited as far as Dutch law permits, with the note that nothing excludes liability that cannot lawfully be excluded; all data held on the device only, with backups the user's own responsibility and no access by the publisher; the terms may be revised and a revision is re-presented before the next medicine is added; and the terms are governed by the law of the Netherlands, with Eduard Keilholz named as publisher and developer.

### D10. Package layout

```
domain/legal/LegalDocument.kt          (LegalDocumentId, LegalDocument, LegalSection, TextRef)
domain/legal/LegalAcceptance.kt
domain/legal/CurrentLegalDocuments.kt  (LegalDocuments: the two documents, their versions and dates)
domain/legal/IsLegalAccepted.kt
domain/repository/LegalRepository.kt
data/settings/DataStoreLegalRepository.kt
ui/settings/legal/LegalSection.kt
ui/settings/legal/LegalDocumentBody.kt
ui/settings/legal/LegalDocumentScreen.kt
ui/settings/legal/AcceptLegalScreen.kt
ui/settings/legal/LegalViewModel.kt
```

Legal sits under `ui/settings/` because Settings is where the documents are read, mirroring `ui/settings/language/`, even though the acceptance screen is reached from Medicines.

### D11. Accessibility

- Every section heading in a document is a semantic heading, so a screen reader user can jump between them instead of hearing the whole text linearly.
- The accept button's disabled state exposes why it is disabled through its supporting text, and that text is announced when it appears.
- Reaching the end of the disclaimer, and the button becoming enabled, is announced rather than being a silent visual change.
- At the largest font scale the document scrolls fully, nothing sets `maxLines`, and the accept button stays pinned and fully visible.
- Test tags: `LegalSectionTestTags.DISCLAIMER_ROW`, `TERMS_ROW`, `STATUS`; `AcceptLegalTestTags.ACCEPT`, `TERMS_LINK`, `SCROLL_HINT`; `LegalDocumentTestTags.TITLE`, `BODY`.

## Risks / Trade-offs

- **A drafted legal text is not a lawyer's text** → The structure is what this change fixes: versioned documents, an acceptance record and a gate. Replacing the wording later is editing string resources and bumping one integer, with no code change. The proposal and this design both say the text is drafted, not reviewed.
- **Scroll-to-end before accepting can frustrate a user who just wants to add a medicine** → It happens once, the disclaimer is deliberately short, and a document short enough to fit the screen enables the button immediately. The alternative — a user who accepts text they were never shown — defeats the reason the gate exists.
- **Gating only the add entrance means a user who added medicines under version 1 keeps using them under version 2 without accepting version 2** → Accepted, and deliberate. The alternative is blocking a person from their own medication reminders over a revised text, which would be a worse outcome than the one the gate protects against. The next medicine they add re-asks.
- **Two sections landing in the same `LazyColumn` from two in-flight changes (`app-about-screen`)** → Each adds one `item` with a distinct key. Whichever lands second appends after the other; the conflict, if any, is one line.
- **A restored device backup carries the acceptance record** → Correct behaviour: the same person accepted the same versions. If a future change excludes the general settings file from backup, acceptance is re-asked, which is safe in the other direction too.

## Migration Plan

No schema, no data migration. Every existing install reads a null acceptance record, which is exactly what a fresh install reads: the next medicine added asks for acceptance, and everything already in the app keeps working untouched. Rolling back is deleting the new files and reverting three call sites; the orphaned preference keys are ignored.

## Open Questions

None. The publisher (Eduard Keilholz) and the governing law (the Netherlands) are confirmed; the wording awaits legal review, which changes string resources and the version integers only.
