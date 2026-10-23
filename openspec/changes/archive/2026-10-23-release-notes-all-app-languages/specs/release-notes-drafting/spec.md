# Spec Delta

## MODIFIED Requirements

### Requirement: Release notes are drafted in English and Dutch using GitHub Copilot
The system SHALL send the collected change summaries to GitHub Copilot, via the GitHub Copilot CLI authenticated with a dedicated repository secret (`GIHUB_COPILOT_API_KEY`), and SHALL request one brief, non-technical, user-facing summary suitable for a Play Store "what's new" listing in each of the app's supported languages: English, Dutch, German, French, Spanish and Portuguese. Each draft SHALL be written in its own language only.

`GIHUB_COPILOT_API_KEY` is a known misspelling of `GITHUB_COPILOT_API_KEY` (missing a T), carried here deliberately because it matches the actual repository secret's name and the workflow's current reference to it. Renaming either the secret or the workflow alone, without the other, breaks this requirement; a future change proposal should rename both together rather than this spec silently drifting from what the workflow actually reads.

#### Scenario: Successful draft generation
- **WHEN** the collected change summaries are sent to GitHub Copilot
- **THEN** the response contains a brief summary in each of English, Dutch, German, French, Spanish and Portuguese, each written as user-facing "what's new" style text in that language only

#### Scenario: Dedicated secret missing or invalid
- **WHEN** the workflow runs in a repository where `GIHUB_COPILOT_API_KEY` is not configured or has expired
- **THEN** the drafting step fails, and the failure is isolated to this workflow (per the "Drafting failures do not block CI or the release build" requirement below)

### Requirement: Drafts are posted as a single, updatable pull request comment
The system SHALL post the drafts for every app language together as one comment on the release pull request, each under a heading naming its language and the whatsnew file it belongs in, marked with a hidden identifier so a later run can find and update it. On a later run against the same pull request, the system SHALL edit that existing comment in place rather than posting a new one. The system SHALL NOT commit any file, including any file under `distribution/whatsnew/`, and SHALL NOT push to any branch.

#### Scenario: First draft on a release pull request
- **WHEN** the release-notes-drafting workflow completes successfully on a release pull request with no prior drafting comment
- **THEN** a new comment containing the English, Dutch, German, French, Spanish and Portuguese drafts, and the hidden marker, is posted on that pull request

#### Scenario: Redraft after the pull request changes
- **WHEN** the workflow runs again on a release pull request that already has a comment carrying the marker
- **THEN** that existing comment is edited to contain the newly drafted text, and no second comment is created

#### Scenario: Nothing is committed
- **WHEN** the workflow runs, regardless of outcome
- **THEN** no commit is made to `development`, `main`, or any other branch, and every file under `distribution/whatsnew/` is left unchanged

## ADDED Requirements

### Requirement: Every app language has Play release notes
The repository SHALL hold one Play "what's new" file under `distribution/whatsnew/` for each language the app supports: `whatsnew-en-US`, `whatsnew-nl-NL`, `whatsnew-de-DE`, `whatsnew-fr-FR`, `whatsnew-es-ES` and `whatsnew-pt-PT`. Each file SHALL be non-empty and at most 500 characters, Google Play's limit. Play listing languages that the app does not support SHALL NOT get a file.

#### Scenario: All six files present and within the limit
- **WHEN** the release-notes check runs on the repository
- **THEN** it passes only if each of the six files exists, is non-empty and holds at most 500 characters

#### Scenario: A language file is missing
- **WHEN** one of the six whatsnew files is absent
- **THEN** the check fails and names the missing language

#### Scenario: A file exceeds Play's limit
- **WHEN** a whatsnew file holds more than 500 characters
- **THEN** the check fails and names the file and its length
