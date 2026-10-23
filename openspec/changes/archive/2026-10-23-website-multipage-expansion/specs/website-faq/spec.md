## ADDED Requirements

### Requirement: FAQ page
The site SHALL provide an FAQ page, present in every supported locale, listing common questions about Pillsner as translated question-and-answer pairs, following the same fallback-to-English behaviour as the rest of the site's content.

#### Scenario: Visitor opens the FAQ page
- **WHEN** a visitor navigates to the FAQ page for a supported locale
- **THEN** the page lists a set of questions with their answers, fully translated in that locale, falling back to English for any question or answer not yet translated

#### Scenario: FAQ entries are independently readable
- **WHEN** the FAQ page renders
- **THEN** each question can be read and expanded without requiring JavaScript
