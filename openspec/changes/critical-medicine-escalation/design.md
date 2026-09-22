## Context

`reminder-scheduling` today escalates every unanswered reminder identically: re-ask every 15 minutes, four times at most, at the standard notification importance. `medication-schedule-model` has no notion of a medicine mattering more than another. The domain layer is Android-framework-free; escalation cadence and notification channel/importance live in the data layer's alarm scheduling and notification-posting code.

## Goals / Non-Goals

**Goals:**
- Let the user flag a medicine as critical, per medicine, off by default.
- Escalate a critical dose's unanswered reminder faster and harder than the standard cadence, without changing what gets recorded (still taken/skipped/missed) or how it lapses.
- Make the flag visible wherever a medicine or dose already surfaces, so whoever answers understands why it behaves differently.
- Mirror the flag to the paired watch, consistent with everything else already mirrored.

**Non-Goals:**
- No medical judgement about which medicines are critical — the user decides, unvalidated.
- No new delivery channel (no calling a contact, no alerting another device). Escalation stays within the existing per-device notification and exact-alarm mechanism.
- No override of the outer lapse bound (next dose due, or 24 hours) — only the cadence leading up to it changes.
- No change to Snooze, Skip or Confirm semantics.

## Decisions

**Escalation parameters: 5-minute re-ask, up to 8 times, distinct sound.** The standard cadence is 15 minutes × 4 (one hour of asking). Critical escalation uses 5 minutes × 8 (40 minutes of asking, twice as many prompts), reusing the exact same repeat-alarm mechanism `reminder-scheduling`'s "Per-dose alarms and one housekeeping alarm" requirement already describes — only the interval and repeat count differ per dose, computed from the medicine's `critical` flag at the moment each repeat alarm is armed. This needs no new alarm tier or scheduling primitive.

Alternative considered: making the interval and repeat count user-configurable per medicine. Rejected for v1 — it adds a settings surface for a niche case before there's evidence the fixed cadence is wrong for anyone; the flag itself is already the point of user control.

**Distinct sound/vibration pattern, not a full do-not-disturb override.** Critical reminders use a separate, more insistent notification channel (Android notification channels are immutable once created, so this is a second channel, `critical_reminder`, alongside the existing one) with a distinct sound and vibration pattern. `AlarmManager.setAlarmClock`, already used for every reminder per `reminder-scheduling`, already carries the platform's highest level of Doze/standby exemption and typically breaks through Do Not Disturb as an alarm-clock-tier alert; no additional bypass API is used, keeping "the app never requests the battery-optimisation exemption" and "no system dialog the user did not ask for" intact.

Alternative considered: `NotificationManager.canBypassDnd` / full-screen intents. Rejected — full-screen intents are a much larger behavioural and permission surface for a first version, and the alarm-clock tier already gives critical reminders the platform's strongest delivery guarantee.

**Badge presentation.** A small icon plus the string "Critical" (localised), placed next to the medicine name on the tile and the dose detail screen, matching the existing "Inactive" pattern in `medicine-overview` (distinction never by colour alone). No new theme token beyond an existing semantic icon already in the design system's icon set; if none fits, the design agent picks one during implementation per `docs/design-system.md`.

**Persistence.** `critical` is a plain `Boolean` column on the `Medication` entity, default `false`, following the same migration pattern as any other schema change: a new Room migration plus its migration test.

**Wear sync.** `wearable-sync`'s shared contract gains a `critical: Boolean` field per dose entry (mirroring the medicine it belongs to), defaulting to `false` when absent so an older payload without the field still deserialises under the existing "unknown field tolerated" / additive-only versioning rule. The watch shows the same badge; it makes no scheduling decision, so no cadence logic is needed on the watch.

## Risks / Trade-offs

- **Escalation fatigue** → Mitigation: the flag is opt-in and off by default; the user chooses which medicines warrant it.
- **A second notification channel adds a small amount of setup code** → Mitigation: created once at app start alongside the existing channel, following the same existing pattern.
- **Distinguishing critical from non-critical reminders in the watchdog/resilience path** → Mitigation: none needed — `reminder-delivery-resilience`'s watchdog and silently-missed detection operate on whatever alarm is currently armed, and are agnostic to which cadence produced it; no change required there.

## Migration Plan

1. Add the `critical` column via a Room migration (default `false`) with its migration test.
2. Add the toggle to the add/details forms and the badge to the tile and dose detail screen.
3. Add the second notification channel and the critical cadence to the alarm-arming and re-ask logic.
4. Extend the wear sync contract with the `critical` field and the watch badge.
5. No data backfill needed — every existing medicine defaults to non-critical, unchanged behaviour.

## Open Questions

None outstanding — escalation parameters, sound/vibration approach and badge presentation are decided above.
