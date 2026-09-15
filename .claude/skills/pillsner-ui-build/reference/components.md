# Component recipes

Compose recipes for `docs/design-system.md` section 8. Package paths are examples; place each composable in the feature package that owns it (`ui/home`, `ui/medicines`, ...) and shared pieces in `ui/components`. Strings shown as `stringResource(R.string.x)` must exist in `res/values/strings.xml`.

## Tile container colour (6, Card row)

```kotlin
@Composable
fun tileContainerColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh
    else MaterialTheme.colorScheme.surfaceContainerLowest
```

Prefer passing `darkTheme` down from `PillsnerTheme` if the app later gains an override; today `isSystemInDarkTheme()` is exact.

## Dose tile (8.1)

```kotlin
@Composable
fun DoseTile(dose: DoseUiModel, modifier: Modifier = Modifier) {
    val status = intakeStatusColors(dose.status)
    val description = stringResource(
        R.string.dose_tile_description, dose.medicineName, dose.amountSpoken, dose.statusLabel, dose.timeLabel,
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.tileMinHeight)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tileContainerColor()),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(Sizes.stateStripeWidth)
                    .fillMaxHeight()
                    .background(status.container),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        dose.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        dose.timeLabel,
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    )
                }
                Text(dose.amountLabel, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Spacing.xs))
                IntakeStatusChip(dose.status, dose.statusLabel)
            }
        }
    }
}
```

`Row` with a `fillMaxHeight` stripe needs `Modifier.height(IntrinsicSize.Min)` on the row. When intake actions arrive, add a `Button` of `Sizes.primaryActionHeight` full width below the column; the card itself stays non-clickable.

## Intake status chip (8.3)

```kotlin
@Composable
fun IntakeStatusChip(status: IntakeStatus, label: String, modifier: Modifier = Modifier) {
    val colors = intakeStatusColors(status)
    Surface(
        modifier = modifier
            .height(Sizes.statusChipHeight)
            .semantics { stateDescription = label },
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.onContainer,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(colors.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
```

The 18 dp icon inside a 28 dp chip is the one exception to "no raw dp": add `Sizes.iconChip = 18.dp` to `Dimens.kt` rather than inlining it.

## Medicine tile (8.2)

Same `Card` as the dose tile without the stripe. Name in `titleMedium`; one `bodyMedium` `Text` per schedule description in `onSurfaceVariant`; when the list is empty, `stringResource(R.string.medicine_as_needed, amount)`.

Inactive variant:

```kotlin
val inactive = !medicine.isActive
Card(
    colors = CardDefaults.cardColors(
        containerColor = if (inactive) MaterialTheme.colorScheme.surfaceContainerLow else tileContainerColor(),
        contentColor = if (inactive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    ),
    modifier = Modifier.semantics(mergeDescendants = true) {
        if (inactive) stateDescription = inactiveLabel
    },
)
```

Add an `Inactive` chip (`surfaceContainerHighest` / `onSurfaceVariant`) at the trailing end of the name row.

## Buttons (8.4)

```kotlin
// The one positive action on a dose surface
Button(
    onClick = onTaken,
    modifier = Modifier.fillMaxWidth().height(Sizes.primaryActionHeight),
) { Text(stringResource(R.string.reminder_action_took_it)) }

FilledTonalButton(onClick = onSnooze, Modifier.fillMaxWidth().heightIn(min = Sizes.minTouchTarget)) {
    Text(stringResource(R.string.reminder_action_not_yet))
}
OutlinedButton(onClick = onSkip, Modifier.fillMaxWidth().heightIn(min = Sizes.minTouchTarget)) {
    Text(stringResource(R.string.reminder_action_not_going_to))
}

// Destructive, inside AlertDialog only
Button(
    onClick = onDelete,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    ),
) { Text(stringResource(R.string.action_delete)) }
```

Order on any reminder surface is fixed: took it, not yet, not going to.

## FAB (8.5)

```kotlin
FloatingActionButton(
    onClick = onAddMedicine,
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.medicines_add), Modifier.size(36.dp))
}
```

Place it through `Scaffold(floatingActionButton = ...)`. Give the list `contentPadding = PaddingValues(bottom = Sizes.fab + Spacing.lg)`.

## Bottom navigation (8.6)

```kotlin
NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
    destinations.forEach { d ->
        val selected = d == current
        NavigationBarItem(
            selected = selected,
            onClick = { onNavigate(d) },
            icon = { Icon(if (selected) d.filledIcon else d.outlinedIcon, contentDescription = null) },
            label = { Text(stringResource(d.label)) },
            alwaysShowLabel = true,
        )
    }
}
```

Material's default indicator is `secondaryContainer`, which is the design. At `WindowWidthSizeClass.Medium` and above swap for `NavigationRail`.

## Screen title (8.7)

```kotlin
Text(
    stringResource(R.string.home_title),
    style = MaterialTheme.typography.displayLarge,
    modifier = Modifier.semantics { heading() },
)
```

Once per screen, at the top of the content column, followed by `Spacing.xl`. Secondary screens use `TopAppBar(title = { Text(..., style = MaterialTheme.typography.titleLarge) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } })`.

## Attention banner (8.9)

```kotlin
@Composable
fun ReminderBanner(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(Icons.Filled.NotificationsOff, contentDescription = null)
            Column {
                Text(message, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
```

Only shown when reminders cannot be delivered. Sits above the dose list.

## Empty state (8.10)

```kotlin
@Composable
fun EmptyState(icon: ImageVector, title: String, hint: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.xxxl, horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, Modifier.size(Sizes.iconEmptyState), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}
```

## Forms (8.11)

```kotlin
OutlinedTextField(
    value = state.name,
    onValueChange = onNameChange,
    label = { Text(stringResource(R.string.medicine_field_name)) },
    isError = state.nameError != null,
    supportingText = state.nameError?.let { { Text(stringResource(it)) } },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
)
```

- Quantity: numeric `OutlinedTextField` beside an `ExposedDropdownMenuBox` for the unit, in a `Row` with `Spacing.md`.
- Dates: read-only field that opens `DatePickerDialog`. Times: `TimePickerDialog`. Never free text.
- Weekdays: `FilterChip`s in a `FlowRow` with `Spacing.sm`; Material's selected colour is `secondaryContainer`, which is the design.
- Save: `Button` of `Sizes.primaryActionHeight`, full width, pinned at the bottom with `Modifier.imePadding()`.

## Dialogs and sheets (8.12)

```kotlin
AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.medicine_delete_title, name), style = MaterialTheme.typography.headlineMedium) },
    text = { Text(stringResource(R.string.medicine_delete_body), style = MaterialTheme.typography.bodyLarge) },
    confirmButton = { /* destructive Button from 8.4 */ },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    shape = MaterialTheme.shapes.large,
)

ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = MaterialTheme.shapes.extraLarge, // top corners
) { ... }
```

## Reminder notification (8.8)

Not Compose. In `ReminderNotifier`:

```kotlin
NotificationCompat.Builder(context, CHANNEL_REMINDERS)
    .setSmallIcon(R.drawable.ic_notification_capsule)     // monochrome
    .setColor(ContextCompat.getColor(context, R.color.pillsner_green)) // #1B7F5C, the one colour resource
    .setContentTitle(dose.medicineName)
    .setContentText(context.getString(R.string.reminder_text, dose.amountLabel, dose.medicineName, dose.timeLabel))
    .setStyle(NotificationCompat.BigTextStyle().bigText(...))
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setCategory(NotificationCompat.CATEGORY_REMINDER)
    .setOngoing(true)
    .setPublicVersion(publicNotification) // "Time for your medicine", no name or amount
    .addAction(0, context.getString(R.string.reminder_action_took_it), tookItIntent)
    .addAction(0, context.getString(R.string.reminder_action_not_yet), notYetIntent)
    .addAction(0, context.getString(R.string.reminder_action_not_going_to), notGoingToIntent)
    .setDeleteIntent(notYetIntent) // swipe counts as Not yet
    .build()
```

The single `res/values/colors.xml` entry `pillsner_green` exists only because the notification API needs a resource; it must equal `PillsnerPalette.LightPrimary`.

## Previews

```kotlin
@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseTilePreview() {
    PillsnerTheme { Surface { DoseTile(previewDose(IntakeStatus.Due)) } }
}
```

Preview data may use inline strings; production code may not.
