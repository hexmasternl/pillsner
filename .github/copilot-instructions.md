# Code review guidance

Pillsner is a small, offline, single-user Android app. Review for problems that would
actually reach a user; skip the rest.

## Report
- Bugs that give wrong results: scheduling, dose/intake recording, stock arithmetic, time zones and DST.
- Data loss or corruption, missing Room migrations, broken transactions around a single user action.
- Privacy: network access, analytics, logging medication names or doses.
- Accessibility regressions and violations of docs/design-system.md.

## Do not report
- Failure windows that need the process to die between two local writes milliseconds apart,
  unless data is permanently corrupted rather than a warning being missed.
- Validation that the UI already enforces, unless a second real caller exists.
- Hypothetical concurrency the app cannot produce (it has one user on one device).
- Style, naming or formatting that the IDE formatter and lint settle.
- Missing tests for trivial code.

## Tone
- At most one comment per distinct issue; don't repeat the same point on several lines.
- Mark anything speculative as a question, not a required change.
