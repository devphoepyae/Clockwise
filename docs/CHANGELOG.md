# Clockwise Changelog

All notable changes to Clockwise are recorded here. The current public release is
**v1.0.10**; the 2.x-4.x entries below are retained as the project history. Long-form
behaviour and verification detail lives in [SPECIFICATION.md](SPECIFICATION.md) and
[HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

---

## v1.0.10 - 2026-11-12

**Package:** `com.pj95.clockwise`
**Build:** `versionCode 11`, `versionName 1.0.10`
**Minimum Android:** 8.0 (API 26) - **targetSdk:** 34 - **compileSdk:** 35

The Clockwise rebrand release. Every `WorldClock` / `worldclock` identifier, file,
resource, Gradle setting and documentation reference now uses `Clockwise` /
`clockwise`, while the application id stays `com.pj95.clockwise`.

The last persisted identities moved with it:

- Room database file: `world_clock.db` → `clockwise.db`.
- Premium widget style id: `premium_world_clocks` → `premium_clockwise`.
- Release key: `worldclock-release.jks` → `clockwise.jks`, with a new
  `CN=Clockwise` certificate.

**This is a fresh-install release.** The signing key changed, so an older build must be
uninstalled before installing 1.0.10. The database file name changed too, so saved
cities and alarms start empty.

### Architecture

- Split the pure Kotlin core into `:core:common`, `:core:model` and `:core:domain`;
  `:core:data`, `:core:ui` and the `:feature:*` modules remain.
- Consolidated every release note into this single `docs/CHANGELOG.md` and moved the
  long-form documentation under `docs/`.
- Removed the obsolete `removed-for-v1.0/` archive and the split-APK scratch files.
- Removed the GitHub Pages/Netlify site files (`index.html`, `privacy.html`, `netlify.toml`,
  `sitemap.xml`, `robots.txt`, `tailwind.css`, `screenshots/`); the project now ships only the
  app, its tests and the docs.

### Verification

- JVM suite: 596 tests in 54 suites, 0 failures.
- `lintDebug`: 0 errors, 27 pre-existing warnings.
- Signed release APK: `app/build/outputs/apk/release/app-release.apk`, 4,185,395 bytes,
  APK SHA-256 `e2886ac19404d10f8a140c1fe69fa14c12f5371bc4ab303482a835f81b0801ae`;
  package `com.pj95.clockwise`, `versionCode 11`, `versionName 1.0.10`, label `Clockwise`,
  signer `CN=Clockwise`, certificate SHA-256
  `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9`, no `INTERNET`
  permission.
- Store assets: the icon, feature graphic and four 640×1280 screenshots (clocks, map,
  themes, widget preview) in `submission/`; the release APK and its checksum are attached
  to the GitHub release for `v1.0.10`.
- Device pass (2026-11-13, Redmi Turbo 4 / Android 16): the automated §7.8 rows pass —
  fresh install, `versionCode 11` / `1.0.10`, `clockwise.db` at `user_version = 10` with
  the five seeded cities, no `world_clock.db`, `CN=Clockwise` certificate, no network
  permission, city deep links and shortcuts open, no crash. Widget placement and TalkBack
  remain manual; details in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §7.8.

---

## v4.1.0 - 2026-10-31

**Release date:** 2026-10-31
**Package:** `com.pj95.clockwise` (application id and namespace)
**Build:** `versionCode 7`, `versionName 4.1.0`
**Minimum Android:** 8.0 (API 26) · **targetSdk:** 34 · **compileSdk:** 35

Clockwise 4.1.0 is a focused pass over the alarms introduced in 4.0.0. The alarm list is quieter
and easier to scan, the ring is much harder for the platform to silence, and snooze is now a
per-alarm decision rather than a fixed ten minutes for everything.

It remains free, ad-free and completely offline: the APK declares **no `INTERNET` permission and no
`ACCESS_NETWORK_STATE` permission**, and ships no billing, ads, analytics or crash-reporting library.

> **This release upgrades 4.0.0 in place.** The application id, signing key and user data are
> unchanged. The database migrates from v6 to v8 on first open; every existing alarm keeps unlimited
> snoozing, which is how it behaved in 4.0.0, and the list keeps its familiar time-of-day order.

### A simpler alarm screen

- **The `ALARMS · TIMER` toggle is gone.** The alarm list is the destination; the timer is a detour.
  A single hourglass in the header opens the countdown, and the close button or system Back returns
  to the alarms. Tapping a timer notification still lands directly on the countdown.
- **Compact cards.** An alarm is two lines: the time, then the label and repeat rule together. The
  whole list fits more alarms on one screen without losing the switch, which remains the only
  control that writes without opening the editor.
- **AM/PM sits beside the time.** On the card, in the editor preview and on the ringing screen the
  digits are large and `AM`/`PM` is set small beside them. It is one text node, so TalkBack reads
  `6:30 AM` as a single value rather than two fragments.
- **Adding is where the list is.** A compact `+` in the `YOUR ALARMS` header and the existing
  `ADD ALARM` row both open the editor; the header itself belongs to the timer.

### Reorder and swipe, with Undo

- **Alarms reorder like cities.** Long-press an alarm card and drag it vertically; the list
  rearranges under the finger, a haptic confirms the lift, and the new order is written once when
  the finger lifts. The position is stored per alarm (`sortOrder`), so a relaunch keeps it.
- **Swipe left to delete.** A horizontal swipe removes an alarm or a city card immediately. A
  snackbar then offers **UNDO** for long enough to change your mind.
- **Undo restores the exact row.** A deleted alarm comes back with its original id, position,
  switch state, sound and snooze settings, and is re-armed if it was enabled. A city returns to its
  old place in the list and is active again if it was active before.
- **The gestures are accessible.** Every card exposes **Move up**, **Move down** and a delete
  custom action, so TalkBack users get the same capabilities without a drag or a swipe.
- **Reordering never re-arms anything.** Moving a card changes only its stored position; the
  alarm's trigger is untouched.

### A ring that survives Doze and the lock screen

- **The sound is a foreground service.** `AlarmRingingService` plays a looping `MediaPlayer` on
  `AudioAttributes.USAGE_ALARM`, holds a wake lock and repeats a vibration. It keeps ringing when
  the app is swiped away, the screen is off or the process is trimmed.
- **`AlarmManager.setAlarmClock` remains the trigger.** It is exact, exempt from Doze and visible to
  the platform as a user-facing alarm, which is what draws the status-bar alarm icon.
- **A silent notification channel.** The ring moved out of the notification channel, so the new
  `alarm-ringing` channel carries no sound of its own. The 4.0.0 `alarms` and per-sound channels are
  deleted on upgrade so the shade does not show settings that no longer control anything.
- **No persistent alarm card.** Nothing is posted while an alarm is merely armed; the system alarm
  icon is the only indication. The notification exists only between the trigger and Dismiss/Snooze.
- **The ring opens full screen.** `USE_FULL_SCREEN_INTENT` brings the ringing screen over the lock
  screen and turns the display on. Settings → **Full-screen alarms** reports whether Android 14+
  still allows it, and opens the system screen where it can be restored.
- **Ten-minute safety cap.** The audio stops by itself after ten minutes; the screen stays up so the
  alarm can still be dismissed.

### Snooze that fits the alarm

- **Three per-alarm policies:** **Every time**, **Once** and **Never**. A wake-up alarm can be put
  off as often as you like; a medication reminder can be dismissed but not postponed.
- **The editor offers the strict setting.** A label containing a medication word (`medication`,
  `pills`, `insulin`, `dose`, `important`, …) suggests turning snooze off. It is only a suggestion:
  the app never changes the policy on its own.
- **The rule is enforced everywhere.** The ringing screen hides Snooze, the notification omits the
  action, and the repository refuses the write, so a stale notification tap cannot postpone a
  critical alarm.
- **Once means once per morning.** A natural occurrence resets the counter; a snooze re-fire keeps
  it. Dismissing the alarm also starts the next ring clean.

### Ringing screen

- The user's own label is the hero, with the time and AM/PM beneath it. There is no eyebrow, no
  repeat summary and no generic quote text.
- The background is the wallpaper's dynamic Material You scheme in its **dark** variant, whatever
  theme the app is set to. An alarm at 03:00 should not be a flashbang.
- Snooze is present only when the policy allows it; otherwise the screen says why and offers
  Dismiss alone.

### Upgrade notes

- Database v6 → v7 adds `snoozePolicy`, `snoozesUsed` and `snoozedAtEpochMillis` to `alarms`.
  Existing alarms get `snoozePolicy = ALLOWED`, so no morning routine changes.
- Database v7 → v8 adds `sortOrder` to `alarms` and backfills it by time of day, so the list an
  existing user opens is the list they left. New alarms append to the end of the arrangement.
- New permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `WAKE_LOCK`, all
  required by the ringing service. `VIBRATE` was already declared.
- Still no network permission, no ads, no analytics and no billing library.

### Build

```powershell
.\gradlew.bat clean assembleDebug assembleRelease testDebugUnitTest lintDebug
```

---

## v4.0.0 - 2026-10-28

**Release date:** 2026-10-28
**Package:** `com.pj95.clockwise` (application id and namespace)
**Build:** `versionCode 6`, `versionName 4.0.0`
**Minimum Android:** 8.0 (API 26) · **targetSdk:** 34 · **compileSdk:** 35

Clockwise 4.0.0 is **Phase 1 of "Chronos"**: the app becomes an alarm clock and timer as well as a
clock, and gains the minimum structure a release needs — a privacy policy, help, feedback and
a named place for the future widget gallery. It remains free, ad-free and completely offline: the
APK declares **no `INTERNET` permission and no `ACCESS_NETWORK_STATE` permission**, and ships no
billing, ads, analytics or crash-reporting library.

> **This release upgrades 3.1.0 in place.** The application id, signing key and user data are
> unchanged, so installing 4.0.0 over 3.1.0 keeps the saved cities, the chosen theme, the 12h/24h
> setting, the launcher shortcuts, placed widgets and every alarm — including its ringtone. The
> database migrates from v5 to v6 on first open; the timer table starts empty.

### Countdown timer

- **`ALARMS · TIMER`.** The Alarms tab now holds both, chosen by a segmented control under the
  title. The bottom bar still has three destinations: a timer answers the same question an alarm
  does — what will interrupt me next?
- **One timer at a time.** Three wheels set `00:00:00`–`23:59:59`, with presets for the common
  durations. The chosen duration is clamped so a wheel that lands on zero cannot start a finished
  timer.
- **Stored as an end instant, not a counter.** A running timer keeps counting while the app is
  closed, survives a reboot and cannot drift by a missed tick. A missed redraw delays the display;
  it cannot make the countdown wrong.
- **Pause and resume are lossless.** Pausing freezes the remainder and disarms the trigger;
  resuming counts from that frozen remainder, even across a process death.
- **A quiet countdown notification.** While running, the shade shows a silent, ongoing notification
  using the platform chronometer, counting down. The system ticks it, so there is no per-second
  foreground service and no wake lock.
- **It rings at zero.** A full-screen **Time's up** screen over the lock screen offers **Start
  again** and **Dismiss**. A timer whose end passed while the phone was off rings when the app next
  starts rather than being dropped.
- **A cancelled timer never rings.** The trigger re-reads the stored row before ringing, so a
  timer paused or reset after it was armed only clears the notification.

### Screen saver

- Settings → **Screen saver** opens a full-screen desk clock: pure black, no system bars, the time
  in large tabular type, the full date and a dim `TAP ANYWHERE TO EXIT` hint.
- The type drifts slowly between five positions once a minute, so no glyph holds one pixel long
  enough to ghost, while the travel is small enough to read as still.
- `FLAG_KEEP_SCREEN_ON` holds the display awake only while the screen saver is visible; leaving it
  by a tap or Back releases the flag, so a forgotten screen saver cannot drain the battery in the
  background.
- It is **not an Android Daydream** and never starts by itself. The time follows the app's 12h/24h
  setting; the theme does not, because a screen saver that followed a paper-white theme would not
  save anything.

### Settings as the app's control panel

- The sheet is grouped **`APPEARANCE`** · **`COMPARED WITH YOUR LOCAL TIME`** · **`ALARMS`** ·
  **`EXTRAS`** · **`CHRONOS STUDIO`**.
- The `ALARMS` group reports the four platform grants — **Exact alarms**, **Notifications**,
  **Allow alarms through Do Not Disturb** and **Battery optimisation** — each in words
  (`ALLOWED` / `ALLOW` / `ON` / `OFF` / `EXEMPT` / `CHECK`), with a row that opens the matching
  system screen.
- The states are re-read on every `ON_RESUME`, so a grant made in a system screen repaints the row
  when you come back.
- The app never asks for the battery-optimisation exemption by itself, and never changes your Do
  Not Disturb schedule.

### Privacy, Help, Chronos Studio and feedback

- **Privacy policy**, **Help** and **Chronos Studio** are local Compose screens — no WebView, no
  hosted page, nothing fetched at runtime.
- The policy states plainly that nothing leaves the phone, what is stored, that there are no ads,
  trackers, analytics, crash reporters or billing libraries, and that the user can remove
  everything by clearing app data.
- **Send feedback** opens the user's own mail app with a short diagnostic block — version name and
  code, Android release and API, `MANUFACTURER MODEL`, locale, time zone and the four alarm grant
  states. Nothing is sent until the user presses send, and every line is editable. If no mail app
  handles it, the same text is copied to the clipboard and a message says so.
- **Chronos Studio** names the future widget gallery and says plainly that nothing can be browsed
  or bought yet. No Play Billing dependency ships in Phase 1.

### Compatibility and data

- **Upgrades:** 3.1.0 → 4.0.0 in place. Database v5 → v6 (`MIGRATION_5_6`); the `timer` table starts
  empty, and every alarm keeps its label, repeat days and `soundUri`.
- **Downgrades are not supported.** Opening 3.1.0 after 4.0.0 has migrated the database is not a
  supported path; keep a copy of the APK and the data if you need to go back.
- **2.1.0 and earlier remain a different app.** The 3.0 rebrand changed the application id, so
  those installs sit beside this one and do not carry data across.
- **Permissions are unchanged from 3.1.0.** No new platform permission is requested for the timer
  or the screen saver.

### Not in this release

- No localization; the UI remains English-only.
- No widget gallery and no purchase: Chronos Studio is a placeholder, and no billing code is
  included.
- No store or Uptodown action, and no change to `submission/`.
- The on-device checks for the timer's ring in Doze, the timer across a reboot, the screen saver's
  keep-awake and drift, the four Settings status rows on HyperOS, the mailto path and the timer
  wheels at a large font are listed in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §4l and §7.6
  and have **not been run**; nothing in this note claims they passed.

### Verification

- JVM suite: **542 tests, 0 failures, 50 suites**.
- `lintDebug`: **0 errors**, 34 warnings.
- Screenshots: **52** renders in `app/build/screenshots/`, including the timer in setup and
  running, the screen saver, the privacy policy and the Studio placeholder.
- `assembleDebug`, `assembleRelease` and `assembleDebugAndroidTest` build successfully.
- Permissions: no `INTERNET`, no `ACCESS_NETWORK_STATE`; asserted by `OfflineGuardrailTest`.

Full detail is in [SPECIFICATION.md](SPECIFICATION.md) §0.7, §5.17–§5.19, §8.7 and §12.2.

---

## v3.1.0 - 2026-10-28

**Release date:** 2026-10-28
**Package:** `com.pj95.clockwise` (application id and namespace)
**Build:** `versionCode 5`, `versionName 3.1.0`
**Minimum Android:** 8.0 (API 26) · **targetSdk:** 34 · **compileSdk:** 35

Clockwise 3.1.0 is a **feature release on top of 3.0.0**. It adds alarms, replaces the alarm
keypad with wheel pickers, adds a per-alarm ringtone, retires two themes and puts the local-time
comparison behind a switch. It remains free, ad-free, IAP-free and completely offline: the APK
declares **no `INTERNET` permission and no `ACCESS_NETWORK_STATE` permission**.

> **This release upgrades 3.0.0 in place.** The application id, signing key and user data are
> unchanged, so installing 3.1.0 over 3.0.0 keeps the saved cities, the chosen theme, the
> 12h/24h setting, the launcher shortcuts and any placed widgets. The database migrates from v3
> to v5 on first open; alarms start empty.

### Alarms

- **A third tab.** The bottom bar is now **Alarms · Clocks · Map**. Search moved back to the `+`
  actions on the Clocks and Map screens, where the cities it searches for actually live.
- **A real alarm clock.** An alarm rings through `AlarmManager.setAlarmClock`, so it honours the
  trigger in Doze and shows the system's next-alarm icon. It survives a reboot, a time change and
  a time-zone change.
- **Full-screen ring over the lock screen**, with **Snooze** (ten minutes later, without changing
  the stored alarm) and **Dismiss**.
- **Repeat days** as a bit mask: one-shot, weekdays, weekend, every day, or any combination. A
  one-shot alarm switches itself off after it rings.
- **One alarm, one card**, with the switch as the only control that writes without opening the
  editor.
- If Android refuses exact alarms, the list shows an inline notice with a working action to the
  system screen instead of failing silently.

### A wheel, not a keypad

- The alarm editor no longer accepts typed digits. Hour and minute are **scrollable wheels** that
  snap to the centre line; 12-hour mode adds an `AM` / `PM` control.
- Under the **Material You** theme the editor uses Android's **clock dial** instead, in the app's
  own palette.
- The direct numeric entry is gone under every theme.

### A sound per alarm

- The editor has a `SOUND` section: one row naming the current tone, opening a short list of the
  device's alarm tones.
- Every row can be **previewed** and stopped in place, which a dropdown cannot offer while open.
- The choice is stored with the alarm. `Default alarm sound` is always the first entry, and a tone
  that has since been deleted falls back to the device default rather than ringing silence.
- Each distinct tone gets its own notification channel, so the choice is honoured by the system;
  the default keeps the existing `alarms` channel, so an upgrade does not lose its tuning.

### Simpler themes

- **True Dark and High Contrast are removed.** The app ships **four** themes: Light Monochrome
  (default), Warm Sepia, Cool Slate and Material You.
- Their stored ids are left unused, and an existing install that had either one opens on Light
  Monochrome rather than on a blank or black screen. The retired three-mode "dark" setting reads
  as the default for the same reason.
- The settings sheet now scrolls, so every row is reachable on a short screen or at a large font.

### Compared with your local time

- Settings gains one switch, **`Compared with your local time`**, on by default.
- Turning it off removes the difference line from every city card, the map's selected-city panel
  and the city detail screen. The times and day/night icons stay, and the cards do not change
  height.
- The **meeting planner keeps its comparisons** — it is the tool whose whole purpose is comparing
  times.
- The setting is remembered and survives a restart.

### Compatibility and data

- **Upgrades:** 3.0.0 → 3.1.0 in place. Database v3 → v5 (`MIGRATION_3_4`, `MIGRATION_4_5`); the
  alarms table starts empty and `soundUri` defaults to the device tone.
- **Downgrades are not supported.** Opening 3.0.0 after 3.1.0 has migrated the database is not a
  supported path; keep a copy of the APK and the data if you need to go back.
- **2.1.0 and earlier remain a different app.** The 3.0 rebrand changed the application id, so
  those installs sit beside this one and do not carry data across.

### Not in this release

- No localization; the UI remains English-only.
- No store or Uptodown action, and no change to `submission/`.
- The on-device checks for the wheels, the dial, the real ringtone list, the full-screen ring and
  Doze are listed in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §4k and §7.5 and have **not
  been run**; nothing in this note claims they passed.

### Verification

- JVM suite: **503 tests, 0 failures, 45 suites**.
- `lintDebug`: **0 errors**, 32 warnings.
- Screenshots: **47** renders in `app/build/screenshots/`, including the wheel editor, the dial
  editor, the alarm list and the comparison-off list.
- `assembleDebug`, `assembleRelease` and `assembleDebugAndroidTest` build successfully.
- Permissions: no `INTERNET`, no `ACCESS_NETWORK_STATE`.

Full detail is in [SPECIFICATION.md](SPECIFICATION.md) §0.6, §5.16 and §12.2.

---

## v3.0.0 - 2026-10-27

**Release date:** 2026-10-27
**Package:** `com.pj95.clockwise` (application id and namespace)
**Build:** `versionCode 4`, `versionName 3.0.0`
**Minimum Android:** 8.0 (API 26) · **targetSdk:** 34 · **compileSdk:** 35

Clockwise 3.0.0 is a **rebrand, not a feature release**. The product moves to the PJ95 studio
identity (`com.pj95.clockwise`) and ships a new launcher icon, "Concept 3". Nothing a user can do
inside the app changes: the six themes including Material You, the Room-backed city order, the
launcher shortcuts, the first-launch intro, the solar map, the planner and the Glance widget all
behave exactly as they did in 2.1.0. It remains free, ad-free, IAP-free and completely offline:
the APK declares **no `INTERNET` permission and no `ACCESS_NETWORK_STATE` permission**.

> **This is a new app to Android.** Because `applicationId` changed from `com.worldclock.offline`
> to `com.pj95.clockwise`, 3.0.0 does **not** upgrade over 2.1.0. It installs beside it, and saved
> cities, the chosen theme, the 12h/24h setting and any placed widgets do not carry across. That is
> the direct consequence of the requested identity change, which is why this is `3.0.0` /
> `versionCode 4` rather than a `2.2.0`.

### What's new

#### PJ95 package identity

- `applicationId` and `namespace` are both **`com.pj95.clockwise`**. Until 2.1.0 they were
  `com.worldclock.offline` and `com.worldclock.app`; a component can now be written
  `com.pj95.clockwise/.MainActivity`.
- Every Kotlin package declaration and import, the manifest components, the widget tick action
  (`com.pj95.clockwise.action.WIDGET_TICK`), the deep-link extras
  (`com.pj95.clockwise.extra.CITY_ID`, `.extra.SHORTCUT`, `.extra.SHORTCUT_ID`), the static
  launcher shortcuts, the file-provider authority and the proto `java_package` moved with it.
- The exported Room schema directory is named after the database class, so it moved to
  `app/schemas/com.pj95.clockwise.data.local.ClockwiseDatabase/`. The three JSON snapshots are
  byte-identical: a Room schema does not embed the package, and the identity hash is computed from
  the schema itself, so the v1–v3 migrations still validate against the same exports.
- The release key is unchanged: SHA-256
  `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`.

#### "Concept 3" launcher icon

- A deep royal blue tile (`#0E2A66`) with one subtle diagonal facet (`#143675`), replacing the
  off-white monochrome mark.
- A single mark that reads as four things at once: a **clock** (dial, two hands, centre pin), the
  letter **C** for Chronos (the dial is a 330° arc, open across the top right), a **globe** (the
  same ring is the meridian, opened to suggest the visible hemisphere, not a closed border), and a
  **Latin–Myanmar hybrid** construction — a circle crossed by straight, cut strokes, with the
  minute hand leaving through the opening where a Burmese letter would carry its tail.
- Ivory `#F7F4EC` dial and hour hand; gold `#E2B65A` minute hand, centre pin and one 50° inlay on
  the lower-left of the dial. Gold appears three times and nowhere else, so it stays an accent.
- All geometry stays inside the 66 dp safe zone of the 108 dp adaptive canvas (outermost point
  `r = 30`), so no launcher mask — circle, squircle or rounded square — crops the mark.
- **Layered, not flattened:** `drawable/ic_launcher_background.xml`,
  `drawable/ic_launcher_foreground.xml` and `drawable/ic_launcher_monochrome.xml`, declared by
  `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`. The monochrome layer drops the
  gold inlay deliberately (one tint cannot distinguish it, and merging would leave a seam) and is
  still a clock and still a C for Android 13+ themed icons.
- Legacy `mipmap-{m,h,xh,xxh,xxxh}dpi` PNGs were **rendered from the same vectors**, not drawn
  separately, with the rounded-square mask for `ic_launcher` and the circular mask for
  `ic_launcher_round`. With `minSdk 26` they are never chosen at runtime; they exist for tools and
  launchers that read the base resource.

#### Branding

- The landing page and privacy policy credit **PJ95** and state the new package id and version.
- The **visible name is unchanged**: `app_name` is still `Clockwise`, so the launcher label,
  Recents card, Settings entry and widget-picker label read the same as before. "Chronos" is the
  project and studio codename.

### Unchanged and verified

- **Material You** remains the sixth, opt-in theme, with wallpaper colours on Android 12+ and the
  static Material 3 fallback below it. The five original themes keep their exact colours and
  persisted ids.
- **Room** stays at database version 3 with the explicit v1 → v2 and v2 → v3 migrations. No
  destructive migration is configured and no schema changed.
- Widget behaviour, the screenshot suite, the accessibility semantics and the no-network
  permission set are unchanged.

### Verification

- JVM suite: **438 tests, 0 failures, 0 errors, 40 suites**; **41 screenshots**, including the new
  `app_icon.png` render of the composed icon.
- `lintDebug`: **0 errors, 30 warnings, 0 information**. The warnings are the accepted baseline
  (19 dependency-update notes, 4 unused resources, 3 plurals candidates, 1 vector path, 1 old
  target API, 1 redundant label, 1 `ObsoleteSdkInt` on `mipmap-anydpi-v26`, which is a false
  positive — an `<adaptive-icon>` has to live behind `-v26`).
- Debug APK **14.09 MiB**, release APK **9.72 MiB**. The release grew ~30 KB over 2.1.0, which is
  the five legacy icon PNG pairs and nothing else.
- `aapt2 dump badging`: `package: name='com.pj95.clockwise' versionCode='4' versionName='3.0.0'`;
  `aapt2 dump resources` lists both the five PNG densities and the `(anydpi)` adaptive XML for
  `mipmap/ic_launcher`.
- Merged release manifest: `android:icon="@mipmap/ic_launcher"`,
  `android:roundIcon="@mipmap/ic_launcher_round"`, all components under `com.pj95.clockwise`, no
  `INTERNET`, no `ACCESS_NETWORK_STATE`.
- Real-device checks — the icon under each launcher mask and themed-icon mode, widget placement,
  drag reordering, TalkBack and doze — are tracked in
  [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) and are **not** claimed as passed here. The
  installed reference phone still carries 2.1.0 under the retired id.

### Install notes

- Treat this as a **fresh install**. `adb install -r` works, but there is no data to preserve: the
  package is new. Keep or uninstall the 2.1.0 build deliberately; both can coexist.
- Debug and release APKs are signed with different keys and cannot be installed over each other.
  Uninstalling to switch tracks erases local data.
- The first launch shows the intro, because the proto store is new. The presence-aware rule that
  suppresses the intro for a store written before the field existed is asserted by
  `UserPreferencesTest`, but it cannot be exercised on-device under a new `applicationId`.
- If the old icon is still on the home screen after installing 3.0.0, it is 2.1.0 — two tiles with
  similar names is the expected confusion of a package change, not a failed install.

### Known limitations

- **English only.** Burmese and other localizations remain deliberately deferred.
- **Hardware pass not run.** Every 3.0.0 worksheet result is empty; the icon, widget and
  accessibility checks need a person and a phone.
- **Legacy rasters can go stale.** The `mipmap-*dpi` PNGs are renders of the vectors. Changing the
  icon without re-rendering them would leave an old design for a tool that reads the base resource;
  no supported device loads them.
- **The icon is dark, the app is not.** The five themes are light, warm or monochrome; the tile is
  deep royal blue by design, because it is studio branding rather than a preview of the first
  screen.
- **Debug APK growth.** Debug grew 13.39 → 14.09 MiB, which is not explained by the icon (the
  release grew only ~30 KB); it is dex/multi-dex layout, not shipped functionality.

### Not in this release

- No Burmese or other localization.
- No feature changes: no new screens, no behaviour changes, no new permissions.
- No accounts, cloud sync, ads, purchases or analytics.
- No store submission, Uptodown listing, appeal or resubmission work.
- No change to `submission/`; it stays untracked.

### Links

- [Specification](SPECIFICATION.md)
- [Human verification worksheet](HUMAN_VERIFICATION.md)
- [Release checklist](RELEASE_CHECKLIST.md)
- [Privacy policy](privacy.html)
- [2.1.0 release notes](#v210)

---

## v2.1.0 - 2026-10-27

**Release date:** 2026-10-27
**Package:** `com.worldclock.offline`
**Build:** `versionCode 3`, `versionName 2.1.0`
**Minimum Android:** 8.0 (API 26) · **targetSdk:** 34

Clockwise 2.1.0 is a feature release on top of 2.0.0. It makes the home-screen widget
translucent and fully resizable, adds wallpaper-based Material You colours, lets the city list be
reordered by long-press drag, adds launcher shortcuts for saved cities and quick actions, and
introduces a one-page first-launch intro. It remains free, ad-free, IAP-free and completely
offline: the APK declares **no `INTERNET` permission and no `ACCESS_NETWORK_STATE` permission**.

### What's new

#### Material You, on top of the five existing themes

- A sixth choice, **Material You**, derives its palette from the device wallpaper on Android 12+.
- Below Android 12, or when the platform supplies no dynamic scheme, it falls back to a static
  Material 3 light/dark palette instead of pretending a wallpaper scheme exists.
- The original five themes — Light Monochrome, True Dark, Warm Sepia, Cool Slate and High
  Contrast — keep their exact colours and persisted ids. Light Monochrome remains the default.
- The theme picker now offers six options in the same radio-group list, each with its own
  preview swatch.

#### Drag-and-drop city order

- Long-press a city card and drag it to a new position. The list rearranges live under the finger
  and a haptic confirms the start of the drag.
- The new order is written **once**, when the finger lifts, and only if it actually changed.
- Display order is now owned by **Room** (`saved_cities`, database v3). Proto DataStore remains
  the authority for membership and settings, and is kept in sync so older builds and the widget
  fallback read the same order.
- TalkBack cannot drag, so every card also offers **Move up** and **Move down** custom actions.
  The first card omits Move up, the last omits Move down, and a single-city list offers neither.
- The active card is announced as selected.

#### Launcher shortcuts

- Three fixed quick actions are declared in `res/xml/shortcuts.xml`: **Add city**, **Plan**
  (meeting planner) and **Map** (world map).
- Up to **three** of the user's first saved cities are published as dynamic shortcuts, in saved
  order, using the city's custom label when one is set.
- The dynamic budget subtracts the three static actions from the launcher's maximum, so API 26–29
  launchers (maximum five) publish at most **two** city shortcuts while modern launchers publish
  three.
- City shortcuts use the same deep link as a widget row. A tap opens that city's detail screen and
  reports usage through `ShortcutManagerCompat.reportShortcutUsed`; widget taps are not counted as
  shortcut usage.
- The shortcut set is republished when the saved ids or their labels change. A theme, 12h/24h or
  active-city change does not touch it.

#### First-launch intro

- A fresh install opens on a single Google Tasks-style page: **Every clock, at a glance**, three
  short feature rows (clocks, planning, privacy) and one **Get started** action.
- The intro is gated in `MainActivity` and is dismissed into the normal shell.
- **Show intro again** in the Settings sheet replays it at any time.
- State is a presence-aware proto field: a fresh install seeds an explicit `false`, while a store
  written by 2.0.0 or earlier has no value and is treated as completed. Upgraders therefore do not
  see the intro again.

#### Widget: translucent card, smaller sizes, calmer ticker

- The card is now a Material 3-style translucent shape: the app's card colour at **94% opacity**,
  16 dp rounded corners and a hairline outline. A busy wallpaper can tint it but can never make a
  city time unreadable, and the corner radius renders on every supported API.
- Five responsive layouts replace four: `180×110` (2×2), `220×110`, `250×180`, `250×250` and
  `320×250`. The default footprint stays 4×4 (`minWidth`/`minHeight` 250 dp), and
  `minResizeWidth`/`minResizeHeight` make the 2×2 bucket reachable.
- The measured row budget still reads the size the launcher actually gave the card and the user's
  font scale, counts overflow as `+N more`, and never drops a city silently.
- **Battery-aware ticking:** while the screen is on, the card redraws on the next minute boundary
  with `setAndAllowWhileIdle`. While the screen is off, the alarm backs off to **five minutes**,
  uses inexact `AlarmManager.set`, and is allowed to be batched by Doze. `ACTION_SCREEN_ON`
  redraws immediately and restores the one-minute cadence, so the stale interval is never visible.
- A manual time change, time-zone change or date rollover also redraws the card immediately rather
  than waiting for the next tick.
- The widget still follows the **device's** light/dark setting rather than the app's stored theme;
  this release does not resolve that open question (SPECIFICATION.md §15.12).

#### Accessibility

- The intro, the shortcuts and the new drag handle all carry labels; the drag handle itself is
  decorative because the card's custom actions carry the meaning.
- Reordering is fully operable without a drag, and the active card exposes `selected`.
- Material You keeps the same semantic roles as the other themes, so every contrast and
  touch-target rule from 2.0.0 still applies.
- The Settings theme list remains one radio group, now with six options.

### Verification

- JVM suite: **436 tests, 0 failures, 40 suites**; **41 screenshots**, including new renders for
  the intro, Material You below Android 12 and the Material You settings sheet.
- `lintDebug`: **0 errors, 30 warnings, 0 information** (the `ReportShortcutUsage` warning is
  cleared now that city taps report usage).
- Debug APK **13.39 MiB**, release APK **9.69 MiB**; the exact counts and screenshot list are in
  [SPECIFICATION.md](SPECIFICATION.md) §12.2.
- The release APK is signed with the same certificate as 1.0.0 and 2.0.0:
  SHA-256 `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`.
- The merged release manifest contains no `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- Real-device checks still open — launcher sizing, the translucent card, drag reordering,
  TalkBack announcements, shortcut menus, screen-off battery behaviour and doze — are tracked in
  [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) and SPECIFICATION.md §15.10–§15.12. They are
  **not** claimed as passed by this release note.

### Upgrade notes

- Install over 2.0.0 with the same signing key; saved cities, labels, theme and 12h/24h survive.
- Room database version 3 adds `saved_cities` with an explicit migration. On first launch the
  table is seeded from the existing Proto DataStore order, so an upgrade keeps the list the user
  already had. No destructive migration is configured.
- Existing widgets keep their placement. The new translucent card and the extra 2×2 size appear
  after the launcher refreshes the provider; removing and re-adding the widget is the surest way
  to see the new default.
- The first-launch intro is **not** shown to anyone upgrading from 2.0.0 or earlier; it can be
  replayed from Settings.
- Debug and release APKs are signed with different keys and cannot be installed over each other.
  Uninstalling to switch tracks erases local data.

### Known limitations

- **English only.** Burmese and other localizations are deliberately deferred to a later release.
- **Widget theme.** The card follows the device's light/dark setting, not the app's chosen
  palette — including Material You. This is documented and still open in SPECIFICATION.md §15.12.
- **Widget touch targets.** Rows are 28 dp tall and the refresh target is 24 dp — a deliberate
  trade-off to fit six rows plus chrome. Whether the full-width rows compensate is a
  hardware-verification item.
- **Screen-off cadence.** A card behind a locked screen can be up to five minutes stale, and Doze
  may defer the inexact alarm further. It catches up immediately when the screen comes on.
- **Generated previews** require Android 15+. Older devices use the static preview image.
- **Hardware pass.** This build has not yet been placed on the reference phone. The checklist is
  ready; the results tables remain empty until a human runs them.

### Not in this release

- No Burmese or other localization.
- No accounts, cloud sync, ads, purchases or analytics.
- No store submission, Uptodown listing, appeal or resubmission work.
- No change to `submission/`; it stays untracked.

### Links

- [Specification](SPECIFICATION.md)
- [Human verification worksheet](HUMAN_VERIFICATION.md)
- [Release checklist](RELEASE_CHECKLIST.md)
- [Privacy policy](privacy.html)

---

## v2.0.0 - 2026-10-27

**Release date:** 2026-10-27
**Package:** `com.worldclock.offline`
**Build:** `versionCode 2`, `versionName 2.0.0`
**Minimum Android:** 8.0 (API 26) · **targetSdk:** 34

Clockwise 2.0.0 is the first release since the v1.0 baseline. It adds a modern home-screen
widget, solar-map time travel, a planner converter with a best-overlap recommendation, and an
offline sunrise/sunset engine. It remains free, ad-free, IAP-free and completely offline: the
APK declares **no `INTERNET` permission and no `ACCESS_NETWORK_STATE` permission**.

### What's new

#### Home-screen widget, rebuilt on Glance 1.2.0

- Four responsive card sizes: compact `220×110`, 4×3 `250×180`, 4×4 `250×250` (the default)
  and wide 5×4 `320×250`.
- A measured row budget that reads the size the launcher actually gave the card and the user's
  font scale. A 4×4 card shows all five default cities; a compact card shows one and counts the
  rest as `+N more` instead of dropping them.
- The header opens the app; each city row deep-links to that city's detail screen; `+N more`
  opens the app; the refresh glyph redraws in place.
- Generated picker previews on Android 15+ (`DESIGN_VERSION = 1`), with the static
  `widget_preview.png` as the fallback below API 35.
- A minute ticker that runs only while a widget exists, with the platform's 30-minute update as
  a backstop.

#### Solar map time travel

- Scrub the map through the current day on a 15-minute grid and return to live time with `NOW`.
- The terminator and the subsolar Sun marker move with the chosen minute.
- The selected city's sunrise and sunset line is shown, including explicit polar-day and
  polar-night wording.
- Solar values come from the offline NOAA engine; no network or map provider is used.

#### Planner converter and best overlap

- Read the plan from the device or from any saved city with the `FROM` chips. The wall-clock
  reading stays put while the underlying instant moves, and the rows convert with it.
- `BEST <time>` scans the day for the slot that puts the most cities inside 09:00–17:59,
  breaking ties by distance from local noon and then by the earlier slot.
- No overlap is stated as `NO OVERLAP` rather than hidden or guessed.

#### Offline solar engine

- NOAA first-order solar ephemeris: declination, equation of time, sunrise, sunset and solar
  noon, with apparent-horizon refraction.
- Polar day and polar night are explicit states, not clamped fake events.
- Results are cached in Room; the cache is now bounded to **30 days** behind the device's local
  date and pruned on launch.

#### Hardening and accessibility

- Application-scope coroutine failures are logged instead of taking down a process the user is
  looking at.
- Debug builds run log-only StrictMode; release behaviour is unchanged.
- The Clocks screen is verified at a **1.5× font scale** with a new Roborazzi render, and the
  widget's row budget shrinks rather than clipping when text grows.
- TalkBack labels, heading navigation, merged city cards, 48 dp app controls and a spoken
  context for time differences are unchanged from the baseline and still covered by tests.

### Verification

- JVM suite, lint and both APK builds pass; the exact counts and screenshot list are in
  [SPECIFICATION.md](SPECIFICATION.md) §12.2.
- The release APK is signed with the same certificate as v1.0.0:
  SHA-256 `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`.
- The merged release manifest contains no `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- Real-device checks still open — launcher sizing, generated previews, deep links, 28 dp widget
  targets, doze behaviour and the TalkBack pass — are tracked in
  [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) and SPECIFICATION.md §15.10–§15.12. They are
  not claimed as passed by this release note.

### Upgrade notes

- Install over v1.0.0 with the same signing key; saved cities, labels and preferences are
  preserved by the existing Proto DataStore schema.
- The widget is a new provider, so add it from the launcher's widget picker. There is no v1.0
  widget to upgrade in place because v1.0 shipped without a widget.
- Debug and release APKs are signed with different keys and cannot be installed over each
  other. Uninstalling to switch tracks erases local data.

### Known limitations

- **English only.** Burmese and other localizations are deliberately deferred to a later
  release.
- **Widget theme.** The card follows the device's light/dark setting, not the app's chosen
  palette. This is documented and still open in SPECIFICATION.md §15.12.
- **Widget touch targets.** Rows are 28 dp tall and the refresh target is 24 dp — a deliberate
  trade-off to fit six rows plus chrome. Whether the full-width rows compensate is a
  hardware-verification item.
- **Doze.** The minute alarm is inexact in deep doze and can lag by up to about nine minutes;
  the 30-minute platform update is the backstop.
- **Generated previews** require Android 15+. Older devices use the static preview image.
- **Hardware pass.** This build has not yet been placed on the reference phone. The checklist is
  ready; the results tables are intentionally empty until a human runs them.

### Not in this release

- No Burmese or other localization.
- No accounts, cloud sync, ads, purchases or analytics.
- No store submission, Uptodown listing, appeal or resubmission work.

### Links

- [Specification](SPECIFICATION.md)
- [Human verification worksheet](HUMAN_VERIFICATION.md)
- [Release checklist](RELEASE_CHECKLIST.md)
- [Privacy policy](privacy.html)

---

## v1.0.0 - 2026-10-21

**Package:** `com.worldclock.offline`
**Build:** `versionCode 1`, `versionName 1.0.0`

First public release. A minimalist, offline-first clock for Android with a live
solar-terminator map, a second-aligned ticker and a 12h/24h toggle. Free, ad-free,
IAP-free, with no `INTERNET` permission.

