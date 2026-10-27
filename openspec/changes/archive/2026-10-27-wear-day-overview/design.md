## Context

GitHub issue [#71](https://github.com/hexmasternl/pillsner/issues/71). The watch app is one screen. `UpcomingWindowFilter` keeps the doses the phone sent that fall within six hours, `WatchViewModel` turns them into a flat list, and `UpcomingDosesScreen` renders it with a header, the cards and the connectivity footer. The phone publishes every pending dose it holds — a rolling two-day window — so the data for a two-day agenda is already on the wrist; only the filter and the layout stand between it and the user.

What the phone sends per dose today is the dose id, the medicine name, the amount pre-formatted in the phone app's language, and the scheduled instant. A details screen needs three things that are not in there: the medicine's default dose, its schedule, and its stock.

## Goals / Non-Goals

**Goals:**
- Answer "what am I taking today, and tomorrow?" on the wrist, in one scrolling list.
- Let a dose lead to the medicine behind it without the user picking up the phone.
- Keep the watch strictly read-only and keep an older watch build working against a newer phone.

**Non-Goals:**
- Any write action on the watch. Confirming, snoozing and skipping stay on the phone's bridged reminder notification.
- A third day, a full history, or adherence figures on the watch.
- A navigation library on the watch for one read-only step.

## Decisions

- **D1 — Today and tomorrow, grouped by time, decided on the watch.** `DayAgenda` replaces `UpcomingWindowFilter`: it keeps the doses whose local date is today or tomorrow *in the watch's own zone*, groups them by the minute they are due and orders both days and times ascending. It stays free of Android imports, so the rule that decides what the user sees is a plain unit test. Doing it on the watch, against the watch's clock, is what keeps the agenda right — including across midnight — while the phone is out of reach.

  Doses due in the same minute share one time heading, because "08:00" written three times over three medicines is the same information three times.

  Overdue doses need no special case: they are the earliest times of today and sort to the top by themselves. The phone never sends a dose that has lapsed into missed, so a dose whose time has gone is one the user still has to take.

- **D2 — The agenda is a flat list of rows with stable keys.** `WatchUiState` holds sections of time groups, which is the shape the screen reasons about; the screen flattens it into day, time and dose rows, each with its own key, for the lazy column. `WatchUiState.entries` stays available as the agenda read in order, which is also how the details screen finds the dose it was asked for.

- **D3 — The medicine details are written out by the phone, not the watch.** `SyncedDose` gains an optional `details` holding the default dose, one line per schedule and a stock line. The phone is the only side that holds the units, the schedule vocabulary and their translations, exactly as it already is for the amount; a watch that formatted a schedule itself would need the whole schedule model and every plural resource, and the two sides would drift.

  `WearMedicineDetails` on the phone reads the medicine and its stock batches and formats the three fields with the same formatters the phone screens use, so the watch says what the phone says, word for word. `DoseSyncPublisher` looks each medicine up once per publish, however many doses it has.

  Stock is the batches' remaining amounts totalled in the medicine's own default dose unit, which is how the rest of the app totals stock. A medicine with no batches at all gets no stock line rather than a zero one: it does not track stock, which is not the same as having run out.

- **D4 — Additive payload, version unchanged.** `details` is optional with a default. A newer phone's payload still decodes on an older watch, which ignores the field it does not know; a newer watch shows a dose from an older phone with the details section simply absent, saying so rather than showing an empty screen. Raising the payload version would blank the list on every watch that had not updated yet, for a field that is not essential to the list at all.

  The same nullability carries the real case where there is nothing to say: a dose outlives the medicine it came from, so a dose whose medication is gone is published without details rather than dropped.

- **D5 — One remembered dose id instead of a navigation graph.** The details screen is one read-only step off the agenda. `WearApp` holds the selected dose id and renders the details of that dose, with `BackHandler` returning to the agenda — which is also what the watch's swipe-to-dismiss gesture triggers. Adding `wear-compose-navigation` for a single destination would be a dependency and a graph for one boolean.

  The selected dose is resolved against the live state on every emission, so a dose answered on the phone while its details are open takes the user back to the agenda instead of leaving a dose on screen that no longer exists.

- **D6 — Read-only is enforced by what is not there.** The details screen renders label-and-value text only. There is no text field, no button, no swipe action, no long press. The one thing a tap does anywhere on the watch is open these details.

## Risks / Trade-offs

- **A longer list on a small screen.** Two days of doses can be twenty rows. Mitigated by the day and time headings, which are what make it scannable, and by the list scrolling with rotary as well as touch. The user asked for exactly this trade: more to scroll, in exchange for a real overview.
- **More text in the payload.** The details repeat per dose on the wire, a few hundred bytes for a busy user, and the Data Layer message stays well within its limits. Structuring the payload by medicine instead would save that at the cost of a lookup on the watch and a harder-to-read wire format.
- **A details screen that can go stale.** It shows what the last payload said. That is the same promise the list already makes, and the connectivity footer on the agenda is where the app is honest about it.
