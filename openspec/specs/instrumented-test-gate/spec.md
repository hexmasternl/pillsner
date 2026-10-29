# instrumented-test-gate Specification

## Purpose
Makes sure every pull request runs the complete instrumented test suite of the Android app and the wearable app on an emulator, so local test runs can stay selective without letting a regression reach `development` unnoticed.

## Requirements

### Requirement: Pull requests run the full instrumented suite
Continuous integration SHALL run every instrumented test of the phone app and of the wearable app on an emulator for each pull request into `development` or `main`, and again for each new commit pushed to such a pull request. It SHALL NOT select a subset of the tests.

#### Scenario: Pull request into development
- **WHEN** a pull request into `development` is opened or receives a new commit
- **THEN** CI runs all instrumented tests of both modules on an emulator

#### Scenario: Change touches only one module
- **WHEN** a pull request changes files in the phone app only
- **THEN** CI still runs the instrumented tests of both modules

#### Scenario: Website-only change
- **WHEN** a pull request changes files under `src/website/` only
- **THEN** CI does not run the instrumented tests

### Requirement: A failing instrumented test fails the check
The instrumented-test check SHALL report failure when any instrumented test fails or errors, or when the emulator fails to start, and SHALL report success only when every instrumented test passed.

#### Scenario: One test fails
- **WHEN** one instrumented test fails and all others pass
- **THEN** the pull request's instrumented-test check reports failure

#### Scenario: Emulator does not start
- **WHEN** the emulator fails to boot
- **THEN** the check reports failure rather than success with zero tests run

### Requirement: Failure reports are kept
When the instrumented-test check fails, CI SHALL make the instrumented test reports of both modules available as a downloadable artifact of the run, so a failure can be diagnosed without re-running it locally.

#### Scenario: Reports after a failure
- **WHEN** the instrumented-test check fails
- **THEN** the run offers the HTML and XML test reports of both modules for download
