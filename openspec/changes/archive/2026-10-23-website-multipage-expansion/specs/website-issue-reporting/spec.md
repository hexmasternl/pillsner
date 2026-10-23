## ADDED Requirements

### Requirement: Report a bug page
The site SHALL provide a "Report a bug" page with a static form (title, description, steps to reproduce, app version, device/Android version) that, without any backend or stored credentials, results in a GitHub issue labelled `bug` on the Pillsner repository.

#### Scenario: Visitor submits the bug report form with JavaScript available
- **WHEN** a visitor fills in the bug report form and submits it
- **THEN** the browser navigates to a GitHub "new issue" page for the Pillsner repository, prefilled with the visitor's title and description (including steps to reproduce, app version and device/Android version) and labelled `bug`

#### Scenario: Visitor has JavaScript disabled
- **WHEN** a visitor without JavaScript opens the bug report page and submits the form
- **THEN** the browser still navigates to a GitHub "new issue" page for the Pillsner repository labelled `bug`, without the title/description prefilled

### Requirement: Request a feature page
The site SHALL provide a "Request a feature" page with a static form (title, description) that, without any backend or stored credentials, results in a GitHub issue labelled `feature` on the Pillsner repository.

#### Scenario: Visitor submits the feature request form with JavaScript available
- **WHEN** a visitor fills in the feature request form and submits it
- **THEN** the browser navigates to a GitHub "new issue" page for the Pillsner repository, prefilled with the visitor's title and description and labelled `feature`

#### Scenario: Visitor has JavaScript disabled
- **WHEN** a visitor without JavaScript opens the feature request page and submits the form
- **THEN** the browser still navigates to a GitHub "new issue" page for the Pillsner repository labelled `feature`, without the title/description prefilled

### Requirement: No backend or stored data for issue reporting
Neither the bug report nor the feature request page SHALL call the GitHub API directly, store any GitHub credential or token, or collect or retain any visitor-submitted data on the site itself; the visitor completes and submits the resulting GitHub issue with their own GitHub account.

#### Scenario: Visitor's input is not retained
- **WHEN** a visitor fills in either form and then closes the tab without submitting
- **THEN** none of their input has been sent anywhere or stored by the site
