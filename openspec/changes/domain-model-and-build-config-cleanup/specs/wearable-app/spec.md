## ADDED Requirements

### Requirement: Minimum Wear OS platform version

The wear module SHALL target and build against a minimum SDK of API 30 (Wear OS 3), matching the floor already declared in the project's version catalog and its README Toolchain table. The wear build configuration MUST reference that catalog value rather than the phone module's lower minimum SDK.

#### Scenario: Wear module build configuration

- **WHEN** the wear module is built
- **THEN** its effective minimum SDK is API 30, read from the version catalog's Wear OS entry
