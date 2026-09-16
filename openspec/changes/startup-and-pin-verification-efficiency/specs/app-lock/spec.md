## ADDED Requirements

### Requirement: PIN verification does not block the UI thread
Checking a PIN against its stored verifier SHALL run off the main thread. The unlock screen, the identity check and the change-PIN flow SHALL remain responsive (input keeps working, no dropped frames) while a PIN check, PIN creation, or Keystore key resolution is in progress.

#### Scenario: Unlock screen stays responsive during verification
- **WHEN** the user submits a PIN on the unlock screen
- **THEN** the PIN check runs off the main thread and the screen continues to accept input and render without a stall

#### Scenario: Identity check stays responsive during verification
- **WHEN** the user submits a PIN during an identity check before changing the PIN or disabling the lock
- **THEN** the PIN check runs off the main thread and the identity check screen remains responsive
