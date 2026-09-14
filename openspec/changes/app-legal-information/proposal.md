## Why

Pillsner reminds people about medication, and the one thing it can never promise is that the reminder arrives. A phone can be off, out of battery, in a mode that silences notifications, or simply left in another room. Today the app says nothing about that, and nothing about who decides the dose, the frequency and the schedule — which is always the user and their prescriber, never the app. A medication app that ships without that stated plainly is misleading about what it is, and leaves its publisher and developers exposed.

The user must be told, once, before they entrust their first medicine to it, that Pillsner is an aid to memory and not a medical device, and must actively accept that. After that the documents stay readable from Settings, so the agreement is never something the user has to remember rather than look up.

## What Changes

- Add two legal documents to the app, held in the domain layer and rendered from string resources: a **Disclaimer** and **Terms of Service**. Each carries a version number and an effective date.
  - The **Disclaimer** states that Pillsner is a reminder aid, not a medical device, and gives no medical advice; that the user alone is responsible for what they take, the dose, the frequency and the schedule; that a reminder may be late or may not arrive at all and Pillsner must not be the only thing the user relies on; and that neither the app, its publisher (Eduard Keilholz) nor its developers can be held responsible for a dose missed, taken late, doubled or taken wrongly, nor for any harm arising from using the app. It points the user to their doctor or pharmacist, and to the emergency services in an emergency.
  - The **Terms of Service** state the licence to use the app, that it is provided "as is" without warranty, the limitation of liability to the extent Dutch law permits, that all data stays on the device and backups are the user's own affair, that the terms may be revised, and that they are governed by the law of the Netherlands.
- Add a **Legal** section to Settings, below Security, with a row for each document, plus a line showing which version was accepted and when.
- Add a **legal document screen**: a secondary destination with a top app bar and a back arrow that renders one document as headed, scrollable prose.
- **Gate adding a medicine on acceptance.** Tapping the add button on the Medicines screen when the current documents have not been accepted opens an acceptance screen showing the disclaimer in full, with the terms one tap away, and a single explicit accept action. Accepting records the accepted versions and continues straight into the add-medicine form. Declining or backing out returns to Medicines and adds nothing.
- Record acceptance on the device — the accepted disclaimer version, the accepted terms version, and the moment of acceptance. When either document is revised to a higher version, the gate appears again before the next medicine is added.
- Editing, deactivating and deleting existing medicines, reminders, intake recording and every other screen are **not** gated. Acceptance guards taking on something new, never reaching what the user already has.
- All new text ships as English and Dutch string resources. No new permission, no network access, no link out of the app, no third-party dependency.

## Capabilities

### New Capabilities
- `app-legal`: what the Disclaimer and the Terms of Service say, how they are versioned, how they are read from Settings, how acceptance is asked for and recorded, and how acceptance gates adding a medicine.

### Modified Capabilities
- `app-navigation`: Settings gains a Legal section; the navigation host gains a legal document destination and an acceptance destination, both non-top-level with the bottom navigation bar hidden.
- `medicine-add`: the add-medicine form is reachable only once the current legal documents have been accepted; the add button opens the acceptance screen first when they have not been.

## Impact

- New `domain/legal/`: `LegalDocument`, `LegalDocumentId`, `LegalAcceptance`, the current document versions, and an `IsLegalAccepted` use case — no Android dependencies, unit-tested.
- New `domain/repository/LegalRepository.kt` and `data/settings/DataStoreLegalRepository.kt` storing acceptance in the existing general settings DataStore, beside the language choice.
- New `ui/settings/legal/`: the Settings Legal section, the document screen, the acceptance screen and their view model.
- Changed: `ui/settings/SettingsScreen.kt` (one more section), `ui/navigation/Routes.kt` (two more routes), `ui/PillsnerApp.kt` (two more destinations and the gated add action), `di/AppContainer.kt` (one repository, one view model).
- New strings in `values/strings.xml` and `values-nl/strings.xml`: both documents, the section, the screens and the acceptance copy.
- `README.md`: note that the app shows a disclaimer and terms and requires acceptance before the first medicine.
- Tests: unit tests for acceptance and versioning, repository tests for the stored value, Compose semantics tests for the section, the document screen and the acceptance screen, and a navigation test that the add button leads to the gate when unaccepted and to the form when accepted.
