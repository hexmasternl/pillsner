## MODIFIED Requirements

### Requirement: The legal text is translated
Every string of both legal documents SHALL have an English, Dutch, German, French, Spanish and Portuguese version, and a missing or extra translation for any supported language MUST fail the project's lint task. The publisher's name and the product name MUST NOT be translated.

#### Scenario: Dutch user reads Dutch terms
- **WHEN** the app language is Dutch and the user opens the Terms of Service
- **THEN** the whole document is in Dutch

#### Scenario: New language user reads translated terms
- **WHEN** the app language is Spanish and the user opens the Terms of Service
- **THEN** the whole document is in Spanish

#### Scenario: Missing translation fails lint
- **WHEN** a legal string is added without a translation for every supported language and lint runs
- **THEN** lint fails with a missing-translation error

#### Scenario: Names are not translated
- **WHEN** the app language is Dutch
- **THEN** the publisher is still named "Eduard Keilholz" and the product is still named "Pillsner"
