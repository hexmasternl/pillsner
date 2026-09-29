## ADDED Requirements

### Requirement: Declared permissions are fixed by an allow-list
The app module SHALL hold a plain-text allow-list of the permissions the app declares (after this change: the twelve permissions the app already ships, seven of its own and five merged in by its libraries, plus `CAMERA`), and a build task SHALL compare the merged manifest of every build variant against it after manifest processing. The build SHALL fail, naming each offending permission, when the merged manifest declares a permission not on the list or omits one that is. The task SHALL run as part of `check`, of every `assemble` task and of every `bundle` task, so continuous integration and the release workflow both run it without a separate step.

#### Scenario: Dependency adds a permission
- **WHEN** a dependency's manifest contributes `INTERNET` to the merged manifest
- **THEN** the build fails and the message names `INTERNET` as undeclared

#### Scenario: Manifest and allow-list agree
- **WHEN** the merged manifest declares exactly the permissions on the allow-list
- **THEN** the task passes and the build continues

#### Scenario: Release bundle runs the guard
- **WHEN** the release bundle task runs
- **THEN** the guard task has run for the release variant first

### Requirement: Forbidden dependency groups fail the build
The same task SHALL inspect the variant's runtime classpath and fail the build when it resolves any artifact whose group is `com.google.firebase`, `com.google.mlkit` or `com.google.android.datatransport`, the artifact `androidx.camera:camera-mlkit-vision`, or any artifact whose group is `com.google.android.gms` and whose artifact is not on an explicit allow-list of the wearable artifacts and their existing transitive base. The failure message SHALL name the artifact and the dependency path that pulled it in.

#### Scenario: ML Kit reintroduced
- **WHEN** any module adds a dependency that transitively resolves `com.google.mlkit:text-recognition`
- **THEN** the build fails and the message names that artifact and its path

#### Scenario: CameraX ML Kit bridge added
- **WHEN** any module adds `androidx.camera:camera-mlkit-vision`
- **THEN** the build fails and the message names that artifact

#### Scenario: Wearable artifacts pass
- **WHEN** the classpath holds only the Play services wearable artifacts already listed
- **THEN** the task passes

### Requirement: The OCR artifact checksum is pinned
The allow-list file SHALL also hold the SHA-256 of the OCR library artifact, and the task SHALL fail the build when the resolved artifact's checksum differs. Updating the library version SHALL require updating the pinned checksum in the same change.

#### Scenario: Artifact changed under the same version
- **WHEN** the resolved OCR artifact's checksum differs from the pinned value
- **THEN** the build fails and the message shows both checksums

#### Scenario: Version bump with new checksum
- **WHEN** the catalog version and the pinned checksum are updated together
- **THEN** the task passes

### Requirement: The allow-list is the disclosed contract
The README's permission section SHALL name the allow-list file as the source of truth for the declared permissions, and the pull-request review instructions SHALL ask reviewers to flag any edit to it.

#### Scenario: README points at the allow-list
- **WHEN** the README's permission section is read
- **THEN** it names the allow-list file and states that the build fails when the manifest differs from it
