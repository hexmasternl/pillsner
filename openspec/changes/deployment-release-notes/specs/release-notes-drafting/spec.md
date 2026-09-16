## ADDED Requirements

### Requirement: Drafting runs only on the development-to-main release pull request
The system SHALL run a dedicated GitHub Actions workflow whenever a pull request targeting `main` is opened, reopened, or synchronized, but SHALL only draft release notes when that pull request's head branch is `development`. The workflow SHALL NOT act on any pull request targeting `main` whose head branch is not `development`.

#### Scenario: Release pull request opened
- **WHEN** a pull request from `development` into `main` is opened
- **THEN** the release-notes-drafting workflow runs

#### Scenario: Release pull request updated before merge
- **WHEN** a pull request from `development` into `main` that is already open receives new commits (synchronize event)
- **THEN** the release-notes-drafting workflow runs again

#### Scenario: Unrelated pull request targeting main
- **WHEN** a pull request whose head branch is not `development` is opened against `main`
- **THEN** the release-notes-drafting workflow does not draft release notes

### Requirement: Shipped changes are found from the OpenSpec archive, with a fallback for proposal-less fixes
The system SHALL identify the OpenSpec changes included in a release pull request by comparing the set of archived change proposals (`openspec/changes/archive/**/proposal.md`) present at the pull request's head commit against the set present at its base commit. For each newly present proposal, the system SHALL read its `## Why` and `## What Changes` sections and its `**GitHub Issue:**` line, when present, as that change's summary. The system SHALL also identify merged pull requests in the same commit range that are not represented by any such archived change, and SHALL include their titles as additional, undated items.

#### Scenario: Release pull request contains one or more archived changes
- **WHEN** one or more proposal files under `openspec/changes/archive/` are present at the release pull request's head commit but absent at its base commit
- **THEN** each such change's `## Why`/`## What Changes` summary and linked issue (if any) are included as source material for the release notes

#### Scenario: Release pull request contains a fix that skipped the proposal step
- **WHEN** a merged pull request's commits are in the release pull request's commit range but no archived change's proposal corresponds to it
- **THEN** that pull request's title is included as an additional item, without an issue reference

#### Scenario: No new archived changes in range
- **WHEN** the commit range contains no newly archived change proposals and no other merged pull requests
- **THEN** the workflow SHALL NOT call the AI drafting step and SHALL post a comment stating there is nothing new to summarize, rather than posting empty or fabricated release notes

### Requirement: Release notes are drafted in English and Dutch using GitHub Models
The system SHALL send the collected change summaries to GitHub Models, authenticated with the workflow's default `GITHUB_TOKEN` (via the `models: read` permission), and SHALL request two brief, non-technical, user-facing summaries suitable for a Play Store "what's new" listing: one in English and one in Dutch. The system SHALL NOT require or use any repository secret dedicated to this drafting step.

#### Scenario: Successful draft generation
- **WHEN** the collected change summaries are sent to GitHub Models
- **THEN** the response contains a brief English summary and a brief Dutch summary, each written as user-facing "what's new" style text

#### Scenario: No dedicated secret configured
- **WHEN** the workflow runs in a repository with no additional secret provisioned for this step
- **THEN** drafting still succeeds using only the workflow's own `GITHUB_TOKEN`

### Requirement: Drafts are posted as a single, updatable pull request comment
The system SHALL post the English and Dutch drafts together as one comment on the release pull request, marked with a hidden identifier so a later run can find and update it. On a later run against the same pull request, the system SHALL edit that existing comment in place rather than posting a new one. The system SHALL NOT commit any file, including `distribution/whatsnew/whatsnew-en-US` or `distribution/whatsnew/whatsnew-nl-NL`, and SHALL NOT push to any branch.

#### Scenario: First draft on a release pull request
- **WHEN** the release-notes-drafting workflow completes successfully on a release pull request with no prior drafting comment
- **THEN** a new comment containing both the English and Dutch drafts, and the hidden marker, is posted on that pull request

#### Scenario: Redraft after the pull request changes
- **WHEN** the workflow runs again on a release pull request that already has a comment carrying the marker
- **THEN** that existing comment is edited to contain the newly drafted text, and no second comment is created

#### Scenario: Nothing is committed
- **WHEN** the workflow runs, regardless of outcome
- **THEN** no commit is made to `development`, `main`, or any other branch, and `distribution/whatsnew/whatsnew-en-US` and `distribution/whatsnew/whatsnew-nl-NL` are left unchanged

### Requirement: Drafting failures do not block continuous integration or the release build
The system SHALL run the release-notes-drafting workflow independently of `ci.yml` and `release.yml`, with no job dependency between them. A failure in the drafting workflow (including a GitHub Models error or quota limit) SHALL NOT prevent the release pull request from being merged and SHALL NOT prevent `release.yml` from running on that merge.

#### Scenario: GitHub Models call fails
- **WHEN** the call to GitHub Models fails or times out
- **THEN** the release-notes-drafting workflow fails, and the release pull request remains mergeable and unaffected by that failure

#### Scenario: Merge proceeds independently
- **WHEN** the release pull request is merged into `main`
- **THEN** `release.yml` runs as it does today, regardless of whether the release-notes-drafting workflow succeeded, failed, or was still running
