## ADDED Requirements

### Requirement: Re-applying the language on a system locale change does not stall the UI
When the phone's locale changes while the app is running, re-applying the app's already-resolved language SHALL NOT perform a blocking storage read on the main thread, since the stored language cannot change without an app restart. Re-applying the language SHALL use the value already resolved for the running process.

#### Scenario: Phone language changes while the app is in the foreground
- **WHEN** the phone's system language changes while the app is running
- **THEN** the app reapplies its already-resolved language to the new configuration without a blocking read from the language setting store
