## ADDED Requirements

### Requirement: README leads with the product pitch
`README.md` SHALL present the product pitch — what Pillsner does, who it is for, and why someone should trust it with their medication reminders — before any developer setup, toolchain or build instructions.

#### Scenario: Visitor opens the README
- **WHEN** a visitor without prior context opens `README.md`
- **THEN** the title, tagline, project status and a "Why Pillsner" section explaining the product's value appear before any prerequisites, toolchain table or build command

### Requirement: README retains the full feature and privacy picture
`README.md` SHALL continue to describe every user-facing feature area, the app's privacy stance, and the rationale for each requested Android permission, framed as reasons to trust and use the app rather than as implementation notes.

#### Scenario: Visitor wants to know what the app does before installing
- **WHEN** a visitor reads the Features, Privacy and Permissions sections of `README.md`
- **THEN** every feature area, privacy guarantee and permission rationale present in the app is described, with no loss of information compared to the prior README

### Requirement: Developer setup lives in CONTRIBUTING.md
`README.md` SHALL NOT contain prerequisites, toolchain version tables, build/run instructions, the OpenSpec development workflow, or the branching model. That content SHALL live in a root-level `CONTRIBUTING.md`, and `README.md` SHALL link to it from a short contributing section.

#### Scenario: Contributor wants to set up the project
- **WHEN** a contributor wants to build and run Pillsner locally
- **THEN** `README.md` points them to `CONTRIBUTING.md`, which contains the prerequisites, toolchain table, build/run steps, development workflow and branching model

#### Scenario: README is checked for developer setup content
- **WHEN** `README.md` is reviewed for compliance with this requirement
- **THEN** it contains no prerequisites list, toolchain version table, Gradle command instructions, OpenSpec workflow steps, or branching-model description
