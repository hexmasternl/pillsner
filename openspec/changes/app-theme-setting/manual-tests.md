# Manual tests: app-theme-setting

Three of this change's promises are about what the screen looks like the instant it appears, which
no automated test on this project can see: an emulator screenshot would prove the colours but not
that they were right on the *first* frame, and the system bar icons are drawn outside the app's own
window entirely.

Set up once: install the debug build on a phone and set the phone itself to dark theme.

## 1. Choosing a theme takes effect at once, and survives a cold start (task 8.4)

1. Open Settings → Theme and choose **Dark** while the phone is set to light.
   **Expect:** the Settings screen repaints dark immediately, with no restart notice — unlike the
   language setting, which does show one.
2. Kill the app from recents and start it cold.
   **Expect:** the very first frame is dark. No white flash, which is the whole reason the theme is
   read with a blocking call at startup.
3. Choose **System default**, then toggle the phone's own dark mode.
   **Expect:** the app follows the phone, without being restarted.

## 2. Light on a dark phone keeps the system bars readable (task 8.5)

1. Set the phone to dark theme and choose **Light** in Pillsner.
   **Expect:** the status bar and navigation bar icons are dark against the app's light background,
   and readable. The app decides this, not the phone, so getting it wrong leaves invisible icons.

## 3. The unlock screen uses the chosen theme (task 8.6)

1. Turn on the app lock with a PIN, choose **Dark**, and set the phone to light.
2. Leave the app and come back so it relocks, then cold start it.
   **Expect:** the unlock screen itself is dark. It is composed above the navigation host, so it is
   the one screen that could plausibly miss the theme.

## Status

Not yet run. These need a physical device and a human eye on the first frame; the automated suite
covers the stored choice, the repaint and the state that drives them.
