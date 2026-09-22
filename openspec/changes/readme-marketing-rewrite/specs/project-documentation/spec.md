## ADDED Requirements

### Requirement: README leads with the product pitch
`README.md` SHALL present the product pitch — what Pillsner does, who it is for, and why it is trustworthy — before any developer-facing content (prerequisites, toolchain, build instructions, development workflow, branching model), and SHALL link to the Google Play Store listing for application id `nl.hexmaster.pillsner` near the top of the file.

#### Scenario: Visitor opens the README
- **WHEN** a visitor opens `README.md` on GitHub
- **THEN** the title, tagline, product pitch, "Why Pillsner" and the feature list appear before any prerequisites, toolchain table, build instructions, development workflow or branching model content
- **AND** a link to `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner` appears within that leading section

#### Scenario: Visitor wants to see the app
- **WHEN** a visitor reads the leading section of `README.md`
- **THEN** at least one screenshot from `docs/screens/` is displayed, illustrating a core part of the product (seeing what's due, confirming a dose, or managing medicines)

### Requirement: Developer content lives in CONTRIBUTING.md
Prerequisites, the toolchain table, building/running instructions, the development workflow and the branching model SHALL live in `CONTRIBUTING.md` at the repository root, and `README.md` SHALL replace that content with a short section linking to `CONTRIBUTING.md`.

#### Scenario: Contributor looks for build instructions
- **WHEN** a contributor opens `CONTRIBUTING.md`
- **THEN** they find the prerequisites, the toolchain table, build/run instructions, the development workflow and the branching model

#### Scenario: README references CONTRIBUTING.md
- **WHEN** a visitor reads the "Contributing" section of `README.md`
- **THEN** it links to `CONTRIBUTING.md` rather than repeating the developer-oriented content inline

### Requirement: Product screenshots are descriptively named and uniformly sized
Screenshots kept under `docs/screens/` SHALL be named for what they depict rather than their raw capture timestamp, and any screenshot embedded in `README.md` SHALL render at the same display size as every other screenshot embedded there, regardless of the source images' native resolution.

#### Scenario: Screenshot file is renamed
- **WHEN** a screenshot in `docs/screens/` is referenced from documentation
- **THEN** its filename describes the screen it shows (e.g. `home-upcoming-dose.png`, `dose-confirmation.png`) rather than a `Screenshot_<timestamp>.png` capture name

#### Scenario: Multiple screenshots appear together in the README
- **WHEN** the README displays more than one screenshot in the same section
- **THEN** each one renders at the same fixed display width, so they appear the same size regardless of their native pixel dimensions
