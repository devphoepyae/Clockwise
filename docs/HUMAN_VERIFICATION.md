# Human verification - Clockwise v1.0.10 (rebrand and module cleanup)

This is the worksheet for the checks a JVM cannot make: **placing the Glance widget on a real
home screen**, **dragging an alarm or city card with a thumb**, **swiping a card away and undoing
it**, **scrolling the alarm and timer wheels**, **hearing a ringtone preview**, **using the
launcher's long-press menu**, **hearing TalkBack**, **judging the launcher icon and Material You on
real wallpaper**, **watching an alarm or timer ring over the lock screen**, **leaving the screen
saver awake for an hour**, and **watching the screen-aware tick through doze**. The JVM suite, lint
and both APK builds are green and are reported in [SPECIFICATION.md](SPECIFICATION.md) §12.2 and
§13; this file records only what still needs a person and a phone.

| | |
| --- | --- |
| **App under test** | `1.0.10` (`versionCode 11`), package `com.pj95.clockwise` |
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` — for the functional pass |
| **Release APK** | `app/build/outputs/apk/release/app-release.apk` — signed, for the performance/memory pass |
| **Device** | Redmi Turbo 4 (`24129RT7CC`), HyperOS, Android 16 |
| **Device zone** | `Asia/Yangon` — **UTC+6:30** |
| **Launcher** | HyperOS launcher; record its grid and any per-app scaling |
| **Widget design version** | `DESIGN_VERSION = 1` (`WidgetPreviewPublisher`) |
| **Database version** | Room **10**; 1.0.10 is a fresh install and seeds v10 directly |

> **Debug and release cannot be installed over each other.** They are signed with different
> keys, so `adb install -r` across tracks fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
> Do the functional/widget/TalkBack/alarm/timer pass on **debug**, then either stop there or
> uninstall (which erases saved cities, alarms, timers and widgets) before installing **release**
> for the performance numbers. Never mix the two in one results table.

> **1.0.10 is a fresh install.** The signing key changed to `clockwise.jks` (`CN=Clockwise`), so an
> older 4.1.0 install must be uninstalled first; `adb install -r` will fail with
> `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. The database file is now `clockwise.db`, so the app starts
> with no saved cities and no alarms. The database is created at `pragma user_version = 10`
> directly. Installing over the old `com.worldclock.offline` 2.1.0 remains impossible — two
> different apps (§0.5).

> **Status: 3.0.0 pass complete; 3.1.0 through 4.1.0 passes not run; 1.0.10 not run.** The 3.0.0
> pass over ADB against a Redmi Turbo 4 (`IVQSS8HYWCCIM7EQ`, HyperOS, Android 16, `Asia/Yangon`) is
> recorded in §7.1–§7.4 and is kept as history. The 3.1.0 rows are in §7.5, the 4.0.0 Phase 1 rows
> are in §7.6, the 4.1.0 alarm rows are in §7.7 and the 1.0.10 rebrand rows are in §7.8; all four
> are **NOT RUN**. Nothing in this file claims those passed.

> **Run the fresh-install checks before restoring data.** Every install of 1.0.10 is fresh unless
> it is the in-place upgrade from 4.1.0, so §4f is now the first thing to do rather than a special
> case. The intro gate depends on whether the store was written before the field existed; because
> the data directory is new, that check only proves the fresh path, never the upgrade path — the
> upgrade path from 2.1.0 cannot be tested under a new `applicationId` and is asserted by
> `UserPreferencesTest` instead. The 4.1.0 → 1.0.10 upgrade is testable and is covered in §7.8.

---

## 0. Prepare the phone

1. **Close floating windows.** A picture-in-picture video and the Wi-Fi/Bluetooth panel covered
   the right-hand side of the app during the earlier pass — exactly where the times are.
2. **Record the starting state.** Do not wipe the app just to make the numbers tidy. The
   sizing matrix has a column for the **five default cities** and one for **eight saved
   cities**; use whichever state the phone is in, and write it down. Under 4.1.0 the first
   launch is the intro and the list is seeded with the five defaults, so the eight-city column
   has to be built by hand — add Auckland, Cairo and Tokyo with the `Village` label.
3. **Pin the device font size** to the default for the main pass. The large-font checks are a
   separate section (§5b), because they deliberately change row heights and row counts.
4. **Set the device theme to Light** for the first screenshot, then repeat the theme check in
   Dark. The widget follows the **device**, not the app's stored theme.
5. **Have a colourful wallpaper ready.** Material You reads the wallpaper; a flat grey wallpaper
   cannot show whether the feature works. Switch wallpaper once during §4g and watch the palette
   and the launcher shortcuts repaint.
6. **Set the alarm volume to about halfway** before §4j, so a preview is audible without being
   startling, and note whether the phone is on silent. A silent phone still rings with
   `CATEGORY_ALARM`, but the judgement of the tone is what the row is for.
7. Keep the phone **unplugged** for the doze check; charging keeps it out of deep idle.

The five default cities are, in order: **Tokyo, Sydney, Los Angeles, New York, London**. The
previously recorded phone had eight cities — including `Tokyo` with the custom label
`Tokyo (Village)`, Los Angeles, London and Auckland — so use that as the eight-city column.

### 0.1 Shell setup

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.pj95.clockwise"
$activity = "com.pj95.clockwise/com.pj95.clockwise.MainActivity"
$provider = "com.pj95.clockwise/com.pj95.clockwise.widget.ClockwiseWidgetReceiver"
$shots = "$env:TEMP\opencode\clockwise-device"
New-Item -ItemType Directory -Force $shots | Out-Null

& $adb devices -l
```

Since 3.0.0 the package name and the Kotlin namespace are the **same** string,
`com.pj95.clockwise`, so a component can be written `com.pj95.clockwise/.MainActivity`. The
fully-qualified pairs above are kept because they also work, and because the 2.1.0 build that may
still be on the phone is installed under the retired `com.worldclock.offline` id — its components
will not answer to `com.pj95.clockwise`.

### 0.2 What this ROM's shell can and cannot do

Recorded so the next run does not spend an hour rediscovering it. Each line is the exact failure,
not a guess.

| Capability | State | Exact error |
| --- | --- | --- |
| `adb shell input tap/keyevent/swipe`, `monkey` | **Blocked** | `java.lang.SecurityException: Injecting input events requires the caller … to have the INJECT_EVENTS permission` |
| `settings put secure/system …` | **Blocked** | `SecurityException: com.android.shell was not granted this permission: android.permission.WRITE_SECURE_SETTINGS` / `WRITE_SETTINGS` |
| `wm set-user-rotation` | **Missing** | `Unknown command: set-user-rotation` |
| `dumpsys deviceidle force-idle` | **Blocked** | `Unable to go deep idle; stopped at INACTIVE` (repeats on every attempt) |
| `sqlite3` on device | **Missing** | `run-as: exec failed for sqlite3: Permission denied` |
| `cmd appwidget` (bind a widget) | **Missing** | no such shell command; there is no way to place a widget without the launcher UI |
| `uiautomator dump` | **Fails** | returns an empty window hierarchy on this ROM |
| Gradle `connectedDebugAndroidTest` install step | **Blocked** | `INSTALL_FAILED_USER_RESTRICTED`; `adb install -r` + `am instrument` works |
| `pm clear` | **Blocked** | `CLEAR_APP_USER_DATA` denial; the test rule deletes the DataStore instead |
| `cmd power wakeup` | **Works** | useful, because the screen sleeps mid-run and `screencap` then returns a stale frame |

To unblock the interaction rows, enable **Developer options → USB debugging (Security settings)**
(and MIUI's *Install via USB*), then rerun. That one toggle covers input injection, Settings writes
and the Gradle connected task; widget placement and TalkBack still need a person.

A stale-frame trap worth knowing: if `screencap` is called while the display is off, it returns
the **last composited frame**. Two screenshots 5 s apart with identical MD5 and a static
`dumpsys gfxinfo` frame count mean the phone is asleep, not that the app is hung. Always
`cmd power wakeup` first.

---

## 1. Machine preconditions (no tapping needed)

Run these before placing anything. They prove the build on the phone is the build the tests
ran against.

| # | Check | Command | Expected |
| --- | --- | --- | --- |
| P1 | Device visible | `& $adb devices -l` | one `device` line, no `unauthorized` |
| P2 | Install debug | `& $adb uninstall com.pj95.clockwise; & $adb install app\build\outputs\apk\debug\app-debug.apk` | `Success` - 1.0.10 is a **fresh install** (new signing key); uninstall any 4.1.0 build first (a 2.1.0 install is a different package) |
| P3 | Version on device | `& $adb shell dumpsys package $pkg \| Select-String "versionCode\|versionName"` | `versionCode=11`, `versionName=1.0.10` |
| P4 | Provider registered | `& $adb shell dumpsys appwidget \| Select-String $provider -Context 0,5` | the receiver is listed with `updatePeriodMillis=1800000`, `resizeMode=3`, `widgetCategory=1` |
| P5 | Launch component | `& $adb shell am start -n $activity` | `Starting: Intent … MainActivity`, Clocks screen paints |
| P6 | No network permission | `& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe" dump permissions app\build\outputs\apk\release\app-release.apk` | no `android.permission.INTERNET`, no `android.permission.ACCESS_NETWORK_STATE` |
| P7 | Signed release | `& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk` | `CN=Clockwise`; SHA-256 `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9` |
| P8 | Generated preview marker | `& $adb shell run-as $pkg cat shared_prefs/widget_previews.xml` | `published_design_version` is `1`; see §3 if absent |
| P9 | TalkBack installed | `& $adb shell pm list packages \| Select-String talkback` | `com.google.android.marvin.talkback` (may be off) |
| P10 | StrictMode is active (debug) | `& $adb logcat -d -s StrictMode` after launching and scrolling | no `StrictMode policy violation` **from app code**; M5 enables log-only policies in debuggable builds |
| P11 | Shortcuts declared | `& $adb shell dumpsys shortcut \| Select-String "com.pj95.clockwise" -Context 0,8` | the app is listed; after launch it carries `add-city`, `planner`, `map` and up to three `city-*` entries |
| P12 | Room schema | `& $adb shell run-as $pkg base64 databases/clockwise.db`, decoded, then `sqlite3` on the host (the device has no `sqlite3`) | tables `cities`, `solar_days`, `saved_cities(cityId, position)`, `alarms(…, soundUri, snoozePolicy, snoozesUsed, snoozedAtEpochMillis, sortOrder)` and `timer(id, durationMillis, remainingMillis, endAtEpochMillis, running, label)` exist; DB `user_version` is `8` |

Pull the database **with its `-wal` and `-shm`** or you will read a database missing everything
written since the last checkpoint — the device leaves `journal_mode=wal` on:

```powershell
foreach ($f in "clockwise.db","clockwise.db-wal","clockwise.db-shm") {
  $b64 = & $adb shell "run-as $pkg base64 databases/$f"
  [IO.File]::WriteAllBytes("$shots\db\$f", [Convert]::FromBase64String(($b64 -join '')))
}
sqlite3 "$shots\db\clockwise.db" "pragma user_version; select * from saved_cities order by position;"
```

Do **not** use `adb exec-out … cat > file` in PowerShell: it corrupts the binary.

`uiautomator dump` and `adb shell input tap` are blocked on this ROM. P1–P12 avoid both (P11/P12
read platform state, not the touch screen); the interaction checks in §2–§5 are human actions by
design.

---

## 2. Launcher sizing matrix

### 2.1 Place the widget

Long-press an empty area of the home screen → **Widgets** → **Clockwise** → drag to a free
page. If a copy from an older build is already there, remove it first so this pass starts from
a known provider.

- [ ] **Clockwise is listed** in the picker.
- [ ] It can be placed on the home screen.
- [ ] Its default footprint is about **4×4 cells**.
- [ ] It can be resized **wider and taller**, and the row count changes.
- [ ] It can also be shrunk to about **2×2**, and the compact (one-city) layout appears.
- [ ] The card is **translucent**: a colourful wallpaper tints it, but every time stays legible.
- [ ] The corners are rounded on the device, not square (the radius is in the drawable, so this
      also proves the API-26 path).
- [ ] Removing the last copy removes it cleanly (no crash, no orphaned alarm).

### 2.2 Expected row counts

The row budget is computed from the **height the launcher reports** (and the font scale), not
from a hard-coded cell count. The table below is the arithmetic for the five declared sizes at
the default font scale. A launcher that hands the card a slightly different height may show
one more or one fewer row; the invariant is that **visible rows + `+N more` = all saved
cities**, never a silent drop.

| Declared size | Five default cities | Eight saved cities |
| --- | --- | --- |
| `180×110` (2×2) | 1 row + `+4 more cities` | 1 row + `+7 more cities` |
| `220×110` (compact) | 1 row + `+4 more cities` | 1 row + `+7 more cities` |
| `250×180` (4×3) | 3 rows + `+2 more cities` | 3 rows + `+5 more cities` |
| `250×250` (4×4 default) | all 5 rows, no count line | 5 rows + `+3 more cities` |
| `320×250` (5×4) | all 5 rows, no count line | 5 rows + `+3 more cities` |

At **1.5× font** the rows grow, so a `250×250` card with eight cities drops to about
**4 rows + `+4 more cities`**; the count line and the rows must never overlap. Record the
actual line you see rather than forcing the table's number.

- [ ] 2×2: one city, then the correct count.
- [ ] Compact: one city, then the correct count.
- [ ] 4×3: three cities, then the correct count.
- [ ] 4×4: five cities for five saved; five plus `+3` for eight saved.
- [ ] Wider 5×4: same five rows, with more horizontal breathing room.
- [ ] The order matches the app's city order, including after a drag reorder (§4e).
- [ ] The last city is never cut in half by the launcher.

### 2.3 What is drawn in each row

- [ ] Header: `WORLD TIME`, the **device's own** time (muted), and the refresh glyph.
- [ ] One row per visible city: sun/moon, name (including a custom label), weekday only when
      the city is on a different **date** from the device, and the time in the app-wide format.
- [ ] A weekday appears for a city across the date line at the current hour, and not for a
      city on the same date — the rule is the date, not the hour.
- [ ] Sun/moon matches each city's own hour (06:00–17:59 = sun, 18:00–05:59 = moon).
- [ ] `Tokyo (Village)`-style labels survive and are not truncated.
- [ ] Switch the app to **24h**: the widget follows without being re-added.
- [ ] Switch back to **12h**: the widget follows again.
- [ ] Empty state (remove every city): the card says `No cities saved. Tap to add one.` and
      tapping it opens the app.

---

## 3. Picker previews

Generated previews are **Android 15+ only** (`setWidgetPreviews`), published once per
`DESIGN_VERSION = 1`. This phone is Android 16, so it is the real test. Below API 35 the static
`drawable-nodpi/widget_preview.png` is the fallback and is not a failure.

- [ ] In the widget picker, the Clockwise preview is a **rendered card**, not a blank box,
      not the launcher's generic placeholder, and not a stale screenshot from the old design.
- [ ] It shows the same card design as the placed widget: `WORLD TIME`, header, rows and the
      light/dark palette of the current device theme.
- [ ] The preview is not stretched or cropped at the picker's preview aspect ratio.

**If the preview is stale**, force one republish. This clears only the private marker; saved
cities are untouched:

```powershell
& $adb shell run-as $pkg rm shared_prefs/widget_previews.xml
& $adb shell am force-stop $pkg
& $adb shell am start -n $activity
Start-Sleep -Seconds 5
& $adb shell run-as $pkg cat shared_prefs/widget_previews.xml
```

The file should now read `published_design_version` = `1`. The system rate-limits this call to
about twice an hour; if the marker is still absent, the call was rejected. Wait and launch
again — do **not** bump `DESIGN_VERSION` just to bypass the limit.

- [ ] The marker is present after the relaunch.
- [ ] Reopening the picker shows the new preview.
- [ ] `adb logcat -d -s WidgetPreviewPublisher` has no `could not be published` warning.

---

## 4. Interaction and deep links

The widget has four distinct targets. Test each one with a thumb, then repeat the row case
cold, warm and after rotation.

### 4a. Targets

- [ ] **Header** (`WORLD TIME` / device time) opens the app on **Clocks**.
- [ ] **A city row** opens **that city's detail screen**, not the top of the list.
- [ ] **`+N more`** opens the app (Clocks).
- [ ] **Refresh glyph** redraws the card **without opening the app**; the minute stays correct
      and no task appears in Recents.
- [ ] The **blank card background** is not a target; tapping it does nothing.
- [ ] The **empty state** opens the app.

### 4b. Deep-link state machine

Tap the **Tokyo** row in each state and answer:

| State | Procedure | Expected |
| --- | --- | --- |
| Cold | `& $adb shell am force-stop $pkg`, then tap the row | Tokyo detail; Back returns to Clocks; exactly one task |
| Warm | Open the app on Clocks, press Home, tap the row | Tokyo detail via `onNewIntent`; no second task; Back returns to Clocks |
| Rotation | On Tokyo detail, rotate the phone to landscape and back | the detail stays; it is not re-opened and the list is not shown |
| Unknown id | `& $adb shell am start -n $activity --es com.pj95.clockwise.extra.CITY_ID atlantis` | app opens on Clocks, no crash |
| Known id (no tap) | `& $adb shell am start -n $activity --es com.pj95.clockwise.extra.CITY_ID tokyo` | Tokyo detail, then Back to Clocks |

Confirm there is only one task:

```powershell
& $adb shell dumpsys activity activities | Select-String "com.pj95.clockwise" -Context 0,2
```

The extra is `com.pj95.clockwise.extra.CITY_ID` and is **consumed once**; a rotation must not
look like a second tap. If a rotation re-opens the detail after the user has navigated away,
that is a defect, not a quirk.

### 4c. Refresh behaviour

- [ ] **Awake minute tick:** leave the phone awake on the home screen with the app closed for
      2–3 minutes. The widget's minutes change at the same moment as the status-bar clock.
- [ ] **Screen-off cadence:** lock the phone for ~12 minutes, unlock, and look immediately. The
      card may be up to **five minutes** stale on the lock screen, but it must be redrawn on
      screen-on — its minute should already be current when the launcher paints, not one tick
      later.
- [ ] **Time change:** with the phone awake, change the device time by a few minutes
      (Settings → Date & time, or `adb shell date` on a debuggable image). The card redraws
      without waiting for the next minute.
- [ ] **Time-zone change:** switch the device zone; the header time and every row follow.
- [ ] **Date rollover:** set the clock to 23:59 and watch midnight pass; the weekday columns
      update.
- [ ] **Data-change redraw:** with the app open, add/remove a city, rename a label, or switch
      12h/24h. The widget updates within a second or two (`WidgetMirror`, not the alarm).
- [ ] **Last-copy removal:** remove the only widget, leave the phone a few minutes, and confirm
      the alarm is gone: `& $adb shell dumpsys alarm | Select-String "clockwise"` finds nothing.
- [ ] **Re-add:** add the widget again; the ticker re-arms without a reboot.

### 4d. Launcher shortcuts

Long-press the **app icon**. The static actions are always present; the city shortcuts are
published from the saved list and are budgeted against the launcher's total.

- [ ] The menu shows **Add city**, **Plan** and **Map**, in that order, with readable labels.
- [ ] **Add city** opens the search sheet; **Plan** opens the meeting planner; **Map** opens the
      world map. None of them opens on Clocks first.
- [ ] Up to **three** saved cities appear as `Time in <city>`, in the saved order, using a custom
      label when one is set.
- [ ] Reorder the cities (§4e); the menu's city order follows.
- [ ] Remove a city and long-press again; its shortcut disappears (no dead entry).
- [ ] Tap a city shortcut while the app is open: it opens that city's detail exactly once
      (`onNewIntent`), and Back returns to Clocks.
- [ ] Record how many city shortcuts HyperOS actually shows. If it shows fewer than three, that
      is the launcher's budget, not a failure — note the number.
- [ ] Optional: after using one city shortcut several times, check whether the launcher promotes
      it. `reportShortcutUsed` is advisory and OEMs may ignore it.

```powershell
# What the platform stored
& $adb shell dumpsys shortcut | Select-String "com.pj95.clockwise" -Context 0,12
```

### 4e. Drag to reorder, swipe to delete

**City cards**

- [ ] Long-press a city card: it lifts, a haptic fires, and the list does **not** jump.
- [ ] Drag it past another card's midpoint: the rows swap live under the finger.
- [ ] Release: the new order stays, and the widget (§2.2) and the launcher shortcuts (§4d)
      follow.
- [ ] Force-stop and reopen: the order survives.
- [ ] Drag and drop back where you started: no visible change, no flash.
- [ ] Start a drag, then move the finger to the screen edge: the list scrolls rather than
      dropping the card somewhere unintended. Record if the scroll-versus-drag trade-off feels
      wrong.
- [ ] With TalkBack on (§5a), use **Move up** / **Move down** instead; the same order results.
- [ ] Swipe a city card right-to-left: it disappears and a `City removed` snackbar offers
      **UNDO**. Tap **UNDO**: the city returns to the same position, and is still active if it was
      active before. Force-stop and reopen: the restored list survives.
- [ ] Swipe a city away and let the snackbar time out: the city stays removed, and the next saved
      city becomes active.

**Alarm cards** (§4k)

- [ ] Long-press an alarm card and drag it up or down: the cards swap live and the order is kept
      after a force-stop. Moving a card must **not** change when its alarm rings.
- [ ] With TalkBack on, use **Move up** / **Move down**; the same order results, and the first card
      has no Move up while the last has no Move down.
- [ ] Swipe an alarm card right-to-left: it disappears and an `Alarm deleted` snackbar offers
      **UNDO**. Tap **UNDO**: the same alarm returns with its switch state, sound, snooze policy
      and position, and is still armed if it was enabled.
- [ ] Delete an alarm, let the snackbar time out, then confirm with `adb shell dumpsys alarm` that
      no trigger for it remains.

### 4f. First-launch intro and replay

**Fresh install** — the only path that exists under the new `applicationId`:

- [ ] The first screen is the intro, not Clocks: `WELCOME`, `Every clock, at a glance.`, three
      feature rows and one **Get started** button.
- [ ] The page scrolls on a short screen and the action stays reachable at the largest font.
- [ ] **Get started** opens Clocks; reopening the app goes straight to Clocks.
- [ ] `dumpsys package` still shows 4.1.0 and the saved cities were seeded normally.

**Upgrade path from 2.1.0 — not testable on this build.** There is no store to upgrade from:
3.x is a different package, so a 2.1.0 "upgrade" is a fresh install. The presence-aware rule that
keeps the intro from appearing for a store written before the field existed is asserted by
`UserPreferencesTest` (`onboarding_completed` absent ⇒ completed) rather than by a device. The
4.0.0 → 4.1.0 upgrade **is** testable and is in §7.7.

**Replay:**

- [ ] Gear → **Show intro again** → the intro appears without restarting the app.
- [ ] Dismissing it returns to the screen you came from and does not duplicate a task.
- [ ] Reopening the app after a replay does not show the intro again.

### 4g. Material You and the four themes

- [ ] Gear → Theme shows **four** rows: Light Monochrome, Warm Sepia, Cool Slate and Material
      You. **True Dark and High Contrast must not appear anywhere** — not in the list, not by
      scrolling.
- [ ] Choose **Material You**: every screen repaints to wallpaper-derived colours, and the
      selected-card inversion and all text stay readable.
- [ ] Change the wallpaper (a strongly coloured one): the app follows on the next composition.
- [ ] Flip the device light/dark setting: Material You follows it, while the three static themes
      do **not**.
- [ ] Choose **Light Monochrome** again: it is byte-identical to 2.0.0 (compare with a remembered
      or captured screenshot).
- [ ] Restart the app: the chosen theme is still in force.
- [ ] If the phone previously stored True Dark or High Contrast, it opens on **Light
      Monochrome** after the upgrade rather than on a blank or black screen.
- [ ] On a pre-Android-12 device (if available), Material You shows the static Material 3
      light/dark fallback rather than a monochrome theme. Record the device and API level.

### 4h. Translucency and wallpaper

- [ ] On a busy/light wallpaper, every time remains readable at the 4×4 size and at 2×2.
- [ ] The hairline outline is visible, so the card edge does not disappear into a light
      wallpaper.
- [ ] On a dark wallpaper in device-dark mode, the card is still distinguishable from the
      background.
- [ ] Judge: is 94 % opacity the right balance, or does it read as opaque? Record the verdict;
      this is the evidence §5.11/§15.12 need.

### 4i. Launcher icon

The JVM renders `app/build/screenshots/app_icon.png` and inflates every layer, but only a
launcher can prove the adaptive icon, the mask and the themed variant work on a device.

- [ ] The launcher shows the **new** icon: deep royal blue, ivory dial, gold minute hand — not
      the old off-white monochrome mark. If the old one is still there, uninstall the 2.1.0
      `com.worldclock.offline` build first; two icons on one screen is the expected confusion.
- [ ] The mark is **not cropped** by the launcher's mask (circle, squircle or rounded square —
      whichever HyperOS uses). Nothing should touch the mask edge.
- [ ] Long-press → **App info** shows the same icon; so do Recents and the widget picker.
- [ ] Theme the icons (Settings → Home screen → *Themed icons*, or the launcher's equivalent):
      the monochrome layer should tint with the wallpaper, and the dial, hands and pin should
      still read at that size.
- [ ] Switch the launcher to its round-icon style if it offers one; the round layer must be
      circular, not a square in a circle.
- [ ] At **48 px** (a large grid or a small folder) the gold inlay and the opening in the dial
      are still distinguishable — this is the size the design has to survive.
- [ ] Judge: does it read as a clock, as a C, and as PJ95? Record the verdict.

### 4j. Compared with your local time

- [ ] Gear → the `COMPARED WITH YOUR LOCAL TIME` section shows one switch, labelled exactly
      **`Compared with your local time`**, and it is **on** after an upgrade from 3.0.0.
- [ ] Turn it off: the difference line disappears from every city card, from the map's
      selected-city panel and from the city detail screen. The times and day/night icons stay.
- [ ] The cards do **not** change height or jump; the space where the line was is simply empty.
- [ ] Open the meeting planner: its per-row offsets are still there. The planner is the
      comparison tool and is deliberately unaffected.
- [ ] Turn it back on: the lines return everywhere without a restart.
- [ ] Restart the app: the switch keeps the position you left it in.
- [ ] At the largest font (§5b), the switch row is still reachable by scrolling the sheet.

### 4k. Alarms

The JVM suite asserts the scheduling arithmetic, the stored fields and the snooze policy, the
wheel/dial switch and the sound selection. What it cannot assert is how the device behaves at ring
time — the foreground service, Doze, the lock screen — and how the picker surfaces feel.

**Upgrade and storage**

- [ ] Install 4.1.0 over a 4.0.0 install (same package and key): the saved cities, theme,
      12h/24h choice, placed widgets, timer and every alarm — including its chosen sound — survive,
      and the app opens normally.
- [ ] Pull the database and check `pragma user_version;` → **8**, and
      `pragma table_info(alarms);` → includes `snoozePolicy INTEGER NOT NULL`,
      `snoozesUsed INTEGER NOT NULL`, `snoozedAtEpochMillis INTEGER` and
      `sortOrder INTEGER NOT NULL`.
- [ ] `select id, hour, minute, sortOrder from alarms order by sortOrder;` → the positions are
      `0, 1, 2 …` in the same time-of-day order the 4.0.0 list showed.
- [ ] `select distinct snoozePolicy from alarms;` → **0** for every pre-upgrade alarm: nothing is
      silently made unsnoozable.
- [ ] On a fresh install the Alarms tab shows its empty state, not a blank screen.

**The list**

- [ ] Bottom bar reads **Alarms · Clocks · Map**; the active tab is announced/selected.
- [ ] There is **no `ALARMS · TIMER` control**. The header's right-hand button is an hourglass and
      opens the countdown; the close button and system Back return to the alarm list.
- [ ] The `YOUR ALARMS` header's `+` and the `ADD ALARM` row both open the editor; an existing
      card reopens it with its values.
- [ ] Each card is **two lines** — the time, then the label and repeat rule — and fits more alarms
      on one screen than the 4.0.0 card did.
- [ ] In 12-hour mode the card shows **`6:30` with `AM`/`PM` small beside it**; TalkBack reads it
      as one value, `6:30 AM`, not two.
- [ ] The card's switch turns an alarm on and off without opening the editor, and the state
      survives a restart.
- [ ] **One-shot** (no repeat days) rings at the next occurrence and is switched **off** after it
      rings; a repeating alarm stays on.
- [ ] When exact alarms are refused (Android 12/12L without the grant), the inline notice appears
      with a working action to the system screen. If the device grants it, record that instead —
      the row needs one of the two observed.

**The editor's time input**

- [ ] Under Light Monochrome, Warm Sepia and Cool Slate the time is a pair of **scrollable
      wheels**, not a number pad. No keypad appears at any point, and the soft keyboard never
      opens for the time.
- [ ] Fling the hour wheel: it settles on one value with a centre-line selection, without
      overshooting or leaving two values looking selected.
- [ ] Scroll from 23 to 00 and from 00 to 23; the wheel wraps in the direction the gesture
      implies, and the saved time matches what is under the line.
- [ ] In 12-hour mode the `AM` / `PM` control is present, is a 48 dp target, and changing it
      changes the stored hour by 12.
- [ ] Switch the app to **Material You** and reopen the editor: it now shows the platform
      **clock dial**, in the app's palette. Tap an hour, then a minute; the dial is learnable and
      the selected number is readable against the real wallpaper scheme.
- [ ] Save an alarm from each input and confirm the list card shows the same time.

**The sound**

- [ ] `SOUND` shows the current tone's name and opens a short, scrollable list. The first entry
      is **Default alarm sound**.
- [ ] Each row's **Preview** plays that tone; **Stop** stops it. Only one preview plays at a time.
- [ ] Leaving the list (Done, back, or dismissing the sheet) stops any preview — nothing keeps
      playing behind the app.
- [ ] Select a non-default tone, save, reopen the alarm: the row still names that tone.
- [ ] Ring the alarm: it uses the chosen tone. If the stored tone has been deleted from the
      device, it rings with the device default rather than silence.
- [ ] Record whether this ROM's alarm-tone list is useful, and whether its first entry matches
      the system alarm's tone.

**Ringing, snooze and dismissal**

- [ ] With the screen locked and the display off, the alarm **turns the screen on and rings over
      the lock screen**, showing the user's label as the hero and the time with `AM`/`PM` beneath
      it. There is no generic quote or eyebrow text.
- [ ] The background is the wallpaper's **dark** dynamic scheme, whatever theme the app is set to.
- [ ] The ring is audible on the **alarm stream** at the alarm volume, even when the phone is on
      silent; the tone loops, and the phone vibrates.
- [ ] Swipe the app away while it rings: the sound continues (the foreground service owns it).
- [ ] Settings → **Full-screen alarms** reports the platform's state; on Android 14+ turning it off
      in the system screen and returning repaints the row, and a ring then lands as a heads-up.
- [ ] **Dismiss** stops the ring and the vibration, removes the notification and leaves a one-shot
      alarm off / a repeating alarm on.
- [ ] An alarm set to **Every time** can be snoozed repeatedly; each snooze re-arms the same alarm
      **ten minutes** later without changing its stored time or repeat days.
- [ ] An alarm set to **Once** snoozes the first time and, on the re-fire, offers **Dismiss** only
      with the explanation that it has already been snoozed.
- [ ] An alarm set to **Never** never shows Snooze on the screen or in the notification; the screen
      says why.
- [ ] A label containing a medication word (`Medication`, `pills`, `insulin`, `dose`,
      `important`) makes the editor offer **TURN OFF SNOOZE**; accepting it saves `Never`.
- [ ] The audio stops by itself after ten minutes while the screen stays up and Dismiss still works.
- [ ] Reboot the phone with an armed alarm: after the launcher settles, the alarm is still armed
      (`dumpsys alarm`), and it rings at its time.
- [ ] Change the device time zone: the alarm still rings at its **wall-clock** time in the new
      zone.
- [ ] On Android 13+, deny the notification permission and ring an alarm: record what happens. The
      service cannot post its notification without it, so this is a known weak point rather than a
      claimed pass.
- [ ] In `dumpsys notification --noredact`, the alarm channel is **`alarm-ringing`**, has **no
      sound**, and no `alarms`/`alarms-*` channel remains from 4.0.0.
- [ ] The exact-alarm grant survives a reboot (check the inline notice is gone on the next
      launch if it was granted).

### 4l. Phase 1 — timer, screen saver, Settings and the information screens

The JVM suite asserts the timer's arithmetic, its stored row, the scheduler calls, the five
Settings groups, the information screens' text and the offline guardrail. What it cannot assert is
what this ROM does with a countdown in Doze, with a timer across a reboot, with an always-on
screen, and with the four system-grant screens. Run the blocks in this order.

**Upgrade and storage**

- [ ] Install 4.0.0 over a 3.1.0 install (same package and key): the saved cities, theme,
      12h/24h choice, placed widgets and every alarm — including its chosen sound — survive, and
      the app opens normally.
- [ ] Pull the database and check `pragma user_version;` → **6**, and
      `pragma table_info(timer);` → `id`, `durationMillis`, `remainingMillis`,
      `endAtEpochMillis`, `running`, `label`.
- [ ] `select count(*) from timer;` → **0** straight after the upgrade: no half-counted timer is
      invented at migration time.
- [ ] On a fresh install, Settings → **Screen saver** opens a clock, and the Alarms tab opens on
      the alarm list, not on the timer.

**The Alarms tab and the timer's setup**

- [ ] The title row shows **ALARMS · TIMER** as a real segmented control; the selected half is
      visibly selected and announced as selected, not merely tinted.
- [ ] Switching to **TIMER** replaces the list with three wheels reading **00:05:00**, a preset
      row, and one primary action labelled **START TIMER**. The bottom bar still has three tabs.
- [ ] Fling each wheel: it settles on one value under the centre line, without overshooting or
      leaving two values looking selected. Scrolling past 23 / 59 / 59 wraps as the gesture
      implies.
- [ ] A preset (e.g. `5 MIN`) sets the wheels to that duration in one tap.
- [ ] Set **00:00:00** and press Start: the timer does not start finished — record what the app
      does instead (it should raise the duration to the one-second minimum).
- [ ] At the largest font (§5b), all three wheels and **START TIMER** are still reachable and the
      wheels still show one number under the line rather than clipping.

**A running timer**

- [ ] Start **00:00:30**: the panel shows the countdown with `REMAINING`, and a quiet
      **Timer countdown** notification appears in the shade counting **down**, with no sound.
- [ ] The shade notification keeps counting while the app is in the background, and while the
      screen is off. It is ongoing and cannot be swiped away while the timer runs.
- [ ] **PAUSE** freezes the displayed time, removes the countdown notification and cancels the
      alarm (`dumpsys alarm` no longer lists it). **RESUME** continues from the frozen remainder,
      not from the original duration.
- [ ] Force-stop the app with a timer running, reopen it: the panel shows the timer still
      counting from its end instant, not from zero and not paused.
- [ ] **CANCEL TIMER** clears the row and the notification; nothing rings at the old moment.
- [ ] Put the phone in Doze (screen off, unplugged, `adb shell dumpsys deviceidle force-idle`) and
      let a short timer expire: it rings within a few seconds rather than at the next maintenance
      window. Record the delay.
- [ ] Reboot the phone with a running timer: after the launcher settles, the countdown
      notification is back (or the timer has already rung if its end passed while the phone was
      off) and `dumpsys alarm` lists it.
- [ ] Start a timer, then change the device time zone: the timer still ends the same number of
      **elapsed** seconds later, unlike an alarm, which keeps its wall-clock time.

**The ring**

- [ ] With the screen locked, a timer reaching zero rings **over the lock screen**, showing
      **TIMER**, **Time's up**, the duration and **Start again** / **Dismiss**.
- [ ] **Dismiss** stops the ring and clears the notification.
- [ ] **Start again** starts the same duration immediately and leaves the panel counting.
- [ ] With notifications denied on Android 13+, a timer still reaches zero without the process
      dying mid-ring; record what is shown. Re-grant afterwards.
- [ ] With the exact-alarm grant missing (Android 12/12L), the inline timer notice appears with a
      working action to the system screen, and a timer still runs — record whether it was late.

**Settings, in the order it reads**

- [ ] The sheet reads **APPEARANCE** · **COMPARED WITH YOUR LOCAL TIME** · **ALARMS** ·
      **EXTRAS** · **CHRONOS STUDIO**, and scrolls to reach the last row at the largest font.
- [ ] `ALARMS` holds four status rows: **Exact alarms**, **Notifications**, **Allow alarms through
      Do Not Disturb**, **Battery optimisation**. Each states its current condition in words
      (`ALLOWED` / `ALLOW` / `ON` / `OFF` / `EXEMPT` / `CHECK`) and is a status row, not a switch.
- [ ] Each of the four rows opens the matching system screen on this ROM. Record the exact screen
      name HyperOS shows for each — that is the part no JVM can know.
- [ ] Grant one of them, come back with Back: the row has repainted with the new state **without**
      reopening Settings.
- [ ] On Android 16 / HyperOS, confirm **Exact alarms** reads as granted (the app uses
      `USE_EXACT_ALARM`, so it should not need a trip to a settings screen) and that the
      **Battery optimisation** row still reports `CHECK`/`OFF` until the user exempts it — the app
      must not prompt for the exemption by itself.
- [ ] Deny notification policy access and confirm the DND row says so rather than claiming the
      bypass works.

**The screen saver**

- [ ] **EXTRAS → Screen saver** opens a **black, full-screen** clock with the time, the full date
      and `TAP ANYWHERE TO EXIT`. Every system bar is hidden; a swipe from the edge reveals them
      transiently and they hide again.
- [ ] The time matches the app's 12h/24h setting and the device's zone, and it advances minute by
      minute without a visible jump.
- [ ] Leave it untouched for **at least an hour** with the phone on charge: the display never
      sleeps. Then check the panel for any ghosting of the digits — the drift exists to prevent
      it.
- [ ] Watch for a minute: the type shifts slightly between positions about once a minute. Judge
      whether that reads as "still" or as a twitch, and record it.
- [ ] Tap anywhere: it returns to Settings. Back also returns. The display sleeps normally again
      after leaving — the app must not hold the screen awake in the background.
- [ ] Rotate the phone: the screen saver stays portrait, like the rest of the app.
- [ ] Confirm it is **not** an Android Daydream: Settings → Display → Screen saver does not list
      Chronos, and the in-app screen saver never starts by itself.

**Privacy, Help, Chronos Studio, feedback**

- [ ] **Privacy policy** opens inside the app (no browser, no WebView chrome), states that nothing
      leaves the phone, that there are no ads/trackers/billing, and that feedback is the user's to
      send. Back returns to Settings.
- [ ] **Help** names exact alarms, notifications and the lock screen, DND, battery optimisation and
      the screen saver. Every claim matches what this ROM actually did in the blocks above.
- [ ] **Chronos Studio** says plainly that the gallery is a placeholder and that **nothing can be
      bought from this screen**. No price, no purchase button.
- [ ] **Send feedback** opens the user's own mail app (or the chooser) with the subject
      **Chronos feedback** and a diagnostic block: version name and code, Android release and API,
      `MANUFACTURER MODEL`, locale, time zone, and the four alarm grant states. Nothing is sent
      until Send is pressed in that app; every line is editable.
- [ ] Remove/disable every mail app, tap **Send feedback** again: the same text is copied to the
      clipboard and a message says so. Paste it somewhere to confirm.
- [ ] The diagnostics contain no account, no device id and nothing that identifies the user.
- [ ] With the app open, check `adb shell dumpsys package com.pj95.clockwise | findstr INTERNET`:
      no `android.permission.INTERNET` and no `ACCESS_NETWORK_STATE` are requested.

---

## 5. Accessibility

### 5a. TalkBack

Settings → Accessibility → **TalkBack** → On. Keep your eyes off the screen as much as
possible; the point is what is *heard*, not what is drawn.

| Surface | Expected announcement |
| --- | --- |
| Clocks title | `World Time` — reachable with **heading** navigation |
| Clocks section | `YOUR CITIES` — also a heading |
| Gear | `Settings, button` |
| Plus | `Add city, button` |
| `PLAN A MEETING` | `Plan a meeting across time zones, button` — the purpose, not just the visible words |
| City card | **one swipe**, not seven: city, region, time, day/night, and `Compared with your local time: …` as words; the active card is announced **selected** |
| City reorder actions | with a card focused, **Move up** and **Move down** appear as custom actions; the first card has no Move up, the last no Move down, and a one-city list has neither |
| City delete action | **Remove city** appears as a custom action and removes the card; the snackbar announces `City removed` with an **UNDO** action |
| Clocks drag grip | must be **silent** (decorative), adding no extra focus stop |
| Bottom tabs | Alarms / Clocks / Map; the active one is announced as selected |
| Alarm tab | `Alarms`, then the count; a card reads the time, the label, the repeat summary and `on`/`off`; the switch is its own stop |
| Alarm reorder actions | **Move up**, **Move down** and **Delete alarm** appear as custom actions on a focused card |
| Undo snackbar | announced as `Alarm deleted` / `City removed` with a single **UNDO** action; tapping it restores the row |
| Alarm editor | `TIME`, the hour and minute wheels as adjustable controls (or the dial under Material You), `REPEAT`, the day chips as checkboxes, `SOUND`, the current tone as a button, then Save |
| Sound list | each row's name, its Preview and Stop buttons, and Done; the selected tone is announced as selected |
| Ringing screen | the user's label, then the time with its period as one value (`6:30 AM`), then `Snooze, button` (only when the policy allows it) and `Dismiss, button` — both reachable without unlocking |
| Alarm card | `6:30 AM, Morning run, Weekdays, switch, on` — the period is part of the time, not a separate node |
| Snooze chips | each reads as a radio button with its state: `Every time, selected`, `Once, not selected` |
| Alarms header | `Timer, button` (the hourglass); there is no segmented control to announce |
| Timer close | `Close timer, button`; the timer's wheels are one node each |
| Timer setup | the three wheels as adjustable controls labelled `Hours`, `Minutes`, `Seconds` — the numbers inside are silent, not fifty focus stops — then the presets and `START TIMER, button` |
| Timer running | one node whose value is read as **time left**, e.g. `2 minutes 5 seconds remaining`, never `02:05` as a time of day; then `PAUSE, button` |
| Timer paused | the same remainder, spoken as remaining, plus `RESUME, button` and `CANCEL TIMER, button` |
| Timer ring | `TIMER`, `Time's up`, the duration, then `Start again, button` and `Dismiss, button` |
| Screen saver | one button, `Screen saver clock. Tap to return to the app.`; the visible hint reads `TAP ANYWHERE TO EXIT` and the time is still readable as text |
| Search field | the placeholder, and typing is spoken |
| Close / clear | `Close`, `Clear search` |
| Search result | `Add Tokyo, button`; an already-saved city says `Tokyo is already added` |
| Detail Back | `Back` |
| 12h / 24h | two radio buttons; the active one is **selected** |
| `EDIT LABEL` | `Edit label for Tokyo, button` |
| Detail headings | `COMPARED WITH YOUR LOCAL TIME`, `YOUR LOCAL TIME` |
| Settings | `Settings` heading, `APPEARANCE`, `Theme`, then **four** radio rows — the active one selected; the colour swatch must be **silent**; then the `Compared with your local time` switch, which must say on/off; `Show intro again` is a button |
| Onboarding | `WELCOME`, the title, each feature row as readable text, then `Get started, button`; no swipe or gesture is required to leave it |
| Launcher long-press | the launcher announces each static action by its long label; a city shortcut's label is `Time in Tokyo` (launcher-dependent) |
| Map canvas | `Schematic world map. UTC offset ruler.` |
| Map marker | `Tokyo, 11:48 PM, Nighttime`; the active one adds `selected city` |
| Planner slider | `Meeting time`; swipe up/down changes it and the value is spoken **as a time**, never `50 percent` |
| Planner row | `Meeting time in Tokyo: 11:48 PM, within working hours` (or `outside working hours`) |
| Label dialog | the pane (`City label`) before the field, then `Label for Tokyo`, not just `edit box` |
| Widget refresh | `Refresh widget` |
| Widget row | the city, its time and day/night; a label is read, and the row is a target (launcher-dependent) |

- [ ] Heading navigation is a help, not a nuisance. If it is noisy, say so — the number of
      headings is a judgement, not a measurement.
- [ ] The card is a **single** merged item on the Clocks screen.
- [ ] Move up / Move down are reachable without hunting, and the list's reading order follows
      the move.
- [ ] The drag grip is not a separate focus stop.
- [ ] The intro's feature rows are read in a sensible order, and **Get started** is the obvious
      last stop.
- [ ] The difference is announced with its `Compared with your local time` prefix when the switch
      is on, and is absent from the card when it is off.
- [ ] Day/night is announced as `Daytime` / `Nighttime`.
- [ ] The label dialog's pane title is actually heard; `paneTitle` inside the app's own
      composition is not guaranteed to behave like a platform dialog window.
- [ ] On the widget, TalkBack reaches the header, rows and refresh; if the launcher exposes
      the RemoteViews children only as one blob or not at all, record that as a platform finding.

### 5b. Large font and display size

The JVM pass now includes `clocks_screen_large_font_360dp.png` (1.5× font). Reproduce it on the
phone, then push further than a screenshot can.

- Settings → Display → **Font size** → **Largest** (about 1.5×–2.0×), and separately
  **Display size** → **Largest**.
- [ ] Clocks: titles, city names, times, labels and the difference line do not clip or overlap.
- [ ] Detail: the long city/region line wraps rather than ellipsising the time away.
- [ ] Planner: rows remain one swipe each; the slider remains usable.
- [ ] Settings: the four theme rows and the comparison switch still scroll and are all reachable.
- [ ] Settings: the four `ALARMS` status rows, the `EXTRAS` rows and the Studio row are all
      reachable by scrolling, and each row's status word does not overlap its title.
- [ ] Alarm editor: the sheet scrolls, the wheels or dial remain usable, the sound row and Save
      stay reachable, and no text is clipped at 1.5×–2.0×.
- [ ] Timer: three wheels plus the colon still fit the width; the centre line still lands on one
      number; the presets wrap rather than clip; `START TIMER` stays reachable.
- [ ] Timer running: the countdown does not clip at the largest size, and the status word and
      actions still fit under it.
- [ ] Screen saver: the time and the full date fit without clipping, and the exit hint stays
      visible at the largest font.
- [ ] Alarm list: a card's time, label and repeat summary wrap or truncate without hiding the
      switch.
- [ ] Search: the query field and result rows remain readable.
- [ ] Widget: rows grow in height, the row count drops, and `+N more` still fits. No text is
      cut off at the card's bottom edge.
- [ ] Go back to the default font size; nothing stays stretched.

### 5c. The widget's small targets

A city row is **28 dp** tall, the header **32 dp**, and the refresh target **24 dp** — a
deliberate exception to the app's 48 dp rule, because six rows plus chrome do not fit a 4×4
card at 48 dp each. The rows are full-width with no gap between the name and the time; the
question is whether width compensates for height.

- [ ] Tap the **top edge**, **bottom edge** and **between the name and time** of the same row.
      Every tap opens the same city, not the row above/below.
- [ ] Tap the **refresh glyph** deliberately and beside it. The glyph must be reachable without
      opening the app by accident.
- [ ] Tap the **header** near its left and right ends; it is one target.
- [ ] With TalkBack on, a row is focusable and activates with a double-tap.
- [ ] Judge: is the 24 dp refresh too small in daily use? Would a 32 dp header help? Record the
      answer; this is the evidence §9 and §15.10 need.

### 5d. Theme divergence (judgement, not a defect)

The widget follows the **device** light/dark setting. The app can be pinned to any of four
palettes, including wallpaper-derived Material You.

- [ ] Flip the device theme → the widget follows.
- [ ] Change the app theme (gear → Theme) → the widget does **not** change.
- [ ] Put **Warm Sepia**, **Cool Slate** or **Material You** beside the neutral card and
      judge: acceptable glanceable surface, or a visible mismatch? This is the open decision in
      §15.12.

---

## 6. Reliability, doze, performance and memory

### 6a. Doze

While the screen is on, the tick is `setAndAllowWhileIdle` on the minute; Android may still
throttle it to roughly once per **9 minutes** in deep doze. While the screen is **off**, 2.1
deliberately backs off to an inexact **five-minute** `set`, which Doze may defer further. A lag
on the lock screen is expected, not a defect; the 30-minute `updatePeriodMillis` is the
backstop, and `ACTION_SCREEN_ON` redraws immediately.

**Natural doze (preferred):** unplug, lock the phone, leave it untouched for 20–30 minutes,
then compare the widget's minute with the status-bar clock. Unlock and look **immediately**; the
screen-on redraw is the behaviour under test.

**Forced idle (faster, needs a debuggable build and shell `DUMP` permission):**

```powershell
& $adb shell dumpsys battery unplug
& $adb shell dumpsys deviceidle force-idle
Start-Sleep -Seconds 600
& $adb shell dumpsys deviceidle get deep
& $adb shell dumpsys alarm | Select-String "clockwise" -Context 1,3
& $adb shell dumpsys deviceidle unforce
& $adb shell dumpsys battery reset
```

On the 3.0.0 reference phone this route does not work: `force-idle` answers
`Unable to go deep idle; stopped at INACTIVE` on every attempt, so only the natural-doze
procedure below produces evidence there. It also needs a widget placed — with no widget,
`WidgetTicker` has no alarm to observe at all.

- [ ] Record the lag on the **lock screen** after 20–30 minutes idle: ______.
- [ ] The lag is at most about five minutes plus whatever Doze deferred, or the 30-minute
      backstop has fired.
- [ ] On **unlock**, the card is current within the first paint, not still one interval behind.
- [ ] `dumpsys alarm` shows the pending tick using inexact `set` while the screen was off, and
      `setAndAllowWhileIdle` again after screen-on.
- [ ] The card is not blank, stale by hours, or crash-looping after idle.
- [ ] The alarm is still armed after `unforce` (`dumpsys alarm` lists the next tick).
- [ ] A running **timer** is listed as `setExactAndAllowWhileIdle` and reaches zero close to its
      end instant even in forced idle; record the delay in seconds.
- [ ] After `unforce`, the timer row is intact and the countdown notification still counts from
      the right remainder.
- [ ] Reboot: restart the phone, wait for the launcher, and confirm the widget is correct and
      ticking (the receiver re-arms on `onUpdate`).

### 6b. Performance and memory

Use the **release APK** for these numbers; debug is slower and carries StrictMode. Uninstall
the debug build first (this erases data and widgets), install release, and repeat the basic
widget placement so the measurement includes a real home screen.

```powershell
# Cold start, three times; record TotalTime and WaitTime
1..3 | ForEach-Object {
  & $adb shell am force-stop $pkg
  & $adb shell am start -W -n $activity
}

# Memory after the Clocks screen has been open for a minute
& $adb shell dumpsys meminfo $pkg | Select-String "TOTAL PSS|TOTAL RSS|Java Heap|Native Heap"

# Frame health: reset, scroll the city list for ~10 s, then read the summary
& $adb shell dumpsys gfxinfo $pkg reset
& $adb shell dumpsys gfxinfo $pkg | Select-String "Total frames|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile"

# App and DB size
Get-Item app\build\outputs\apk\release\app-release.apk | Select-Object Length
& $adb shell run-as $pkg ls -l databases
```

- [ ] Cold start `TotalTime` recorded for three runs: ______ / ______ / ______ ms.
- [ ] `TOTAL PSS` after one minute: ______ MB. Re-check after opening the map and planner;
      the increase is bounded, not monotonic.
- [ ] Janky frames: ______ % of ______ frames. Record the 90th/95th/99th percentiles.
- [ ] Rotate, background/foreground, and return: no `ANR`, no visible stall, the ticker resyncs.
- [ ] No `OutOfMemoryError` or `GC` storm in `adb logcat -d | Select-String "OutOfMemory|FATAL"`.

### 6c. StrictMode (debug only)

After reinstalling debug (or before switching to release), launch and scroll for 30 seconds:

```powershell
& $adb logcat -c
& $adb shell am force-stop $pkg
& $adb shell am start -n $activity
Start-Sleep -Seconds 10
& $adb logcat -d -s StrictMode
```

- [ ] No `StrictMode policy violation` lines. If there are, paste the stack here; M5 deliberately
      installed log-only policies so these are findings, never crashes.

### 6d. Solar cache bound (M5, debug build)

The cache keeps **30 days** behind the device's local date and is pruned on every app launch.
To see it, use the map's time travel for a few minutes across several cities, then pull the
database and inspect the date range. `run-as` requires the debuggable build; on release this
section is covered by the JVM policy test instead.

```powershell
# pull db + wal + shm (see §1 P12 for why all three)
foreach ($f in "clockwise.db","clockwise.db-wal","clockwise.db-shm") {
  $b64 = & $adb shell "run-as $pkg base64 databases/$f"
  [IO.File]::WriteAllBytes("$shots\db\$f", [Convert]::FromBase64String(($b64 -join '')))
}
sqlite3 "$shots\db\clockwise.db" "select count(*), min(dateEpochDay), max(dateEpochDay) from solar_days;"
```

`dateEpochDay` is a `LocalDate.toEpochDay()` value. Today's epoch day on the device:

```powershell
& $adb shell date +%s   # then convert, or let the JVM test be the authority
```

- [ ] The oldest row is no more than 30 local days before today.
- [ ] Launching again does not delete today's rows.
- [ ] `pruneBefore` leaves the cutoff day and newer rows intact (the JVM test asserts this too).

### 6e. Permissions and package facts

- [ ] `aapt2 dump permissions` shows **no** `INTERNET` and **no** `ACCESS_NETWORK_STATE`, and
      does show `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `VIBRATE`,
      `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK`, `FOREGROUND_SERVICE` and
      `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- [ ] `dumpsys package $pkg` shows `versionCode=11`, `versionName=1.0.10`, `minSdk=26`,
      `targetSdk=34`.
- [ ] The release certificate SHA-256 matches §1 P7.
- [ ] `pragma user_version;` on the pulled database is **8**, `alarms` carries `soundUri`,
      `snoozePolicy`, `snoozesUsed`, `snoozedAtEpochMillis` and `sortOrder`, and `timer` has its
      six columns.
- [ ] The APK sizes are recorded in §7.

---

## 7. Results and evidence

**§7.1–§7.4 are the 3.0.0 pass of 2026-10-27**, kept as history: the 3.1.0 build changes the
version, the database and the theme list, 4.0.0 adds the timer and 4.1.0 adds the ringing service
and per-alarm snooze, so none of those numbers are re-used. §7.5 lists the 3.1.0 rows, §7.6 the
4.0.0 rows and §7.7 the 4.1.0 rows, with their status.

### 7.1 Machine results

Device `IVQSS8HYWCCIM7EQ` (Redmi Turbo 4, HyperOS, Android 16, `Asia/Yangon`, 1220×2712,
density 520, UTC+6:30), 2026-10-27. Screenshots are in `%TEMP%\opencode\clockwise-device`.

| Check | Result (PASS / FAIL / BLOCKED) | Evidence |
| --- | --- | --- |
| P1 Device visible | **PASS** | one `device` line, no `unauthorized` |
| P2 Debug install | **PASS** | `Success` for debug, `Success` for the release APK; test APK via `adb install -t -r` |
| P3 Version 3.0.0 / 4 | **PASS** | `versionCode=4 minSdk=26 targetSdk=34`, `versionName=3.0.0` |
| P4 Provider registered | **PASS** | receiver listed, `min=250×250`, `minResize=180×110`, `updatePeriodMillis=1800000`, `resizeMode=3`, `widgetCategory=1`; `widgets.size=0` — deliberately nothing placed |
| P5 Launch component | **PASS** | Clocks painted; cold `TotalTime: 716 ms` on debug |
| P6 No network permission | **PASS** | declared: `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, the dynamic-receiver permission. No `INTERNET`, no `ACCESS_NETWORK_STATE` |
| P7 Release certificate | **PASS** | `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`; the APK pulled back off the phone re-verified to the same digest |
| P8 Generated-preview marker | **PASS** | `shared_prefs/widget_previews.xml` → `published_design_version = 1` |
| P9 TalkBack present | **PASS** | `com.google.android.marvin.talkback` installed (left off) |
| P10 StrictMode clean | **PASS, with one vendor note** | 6 × `IncorrectContextUseViolation` (`GestureDetector#init`, `ViewConfiguration`) raised from `com.mediatek.boostfwk` with `com.pj95.clockwise.ClockwiseApp` as the context. No disk-read, no network, no main-thread-I/O violation from app code |
| P11 Shortcuts declared | **PASS** | static `add-city`, `planner`, `map`; dynamic `city-sydney`, `city-los-angeles`, `city-tokyo`, each with the right intent and extras |
| P12 Room order table | **PASS** | `user_version = 3`, `saved_cities(cityId, position)`, `journal_mode=wal`, `quick_check=ok`, 22 cities, order `sydney, tokyo, los-angeles, new-york, london` |

### 7.2 Hardware results

| Section | Result | Evidence (screenshot / log / note) |
| --- | --- | --- |
| 2.1 Widget placed, listed, resized (incl. 2×2) | **BLOCKED** | no widget can be bound from a shell on this ROM (no `cmd appwidget` command); `widgets.size=0`. Needs a long-press on the home screen |
| 2.2 Row counts across five sizes | **BLOCKED** | follows 2.1 |
| 2.3 Content, labels, 12h/24h | **BLOCKED** | follows 2.1; the content rules stay asserted by the Roborazzi previews |
| 3 Generated picker preview | **BLOCKED** (marker PASS) | marker present (P8) and no `could not be published` warning; the picker itself needs a tap |
| 4a Targets | **BLOCKED** | no widget to tap |
| 4b Cold / warm / rotation / unknown id | **PARTIAL** | cold `--es com.pj95.clockwise.extra.CITY_ID tokyo` → Tokyo detail (`TotalTime: 767 ms`), Back returns to Clocks; the `onNewIntent` path was exercised by repeated launches. **Unknown-id and rotation not run** — rotation cannot be forced (§0.2) |
| 4c Tick, screen-off cadence, time/zone/date changes | **BLOCKED** | no widget, so `WidgetTicker` schedules nothing: `dumpsys alarm` has **no** `com.pj95.clockwise` entry, which is the correct state for zero widgets but is not the evidence this row wants |
| 4d Launcher shortcuts | **PARTIAL** | published and ordered correctly in `dumpsys shortcut`; the long-press menu itself cannot be opened |
| 4e Drag reorder and persistence | **PASS** | instrumented `draggingACityPersistsTheNewOrderAndTheDatabaseIsAtVersionThree` drags with real touch input; after `force-stop` both Room (`sydney, tokyo, los-angeles, new-york, london`) and the DataStore proto hold the new order |
| 4f Intro fresh install / replay | **PASS** (install path) | `01_intro.png`: fresh install opens on the intro; the instrumented suite dismisses it with the real **Get started** button. Replay row is tap-only → unverified |
| 4g Material You and wallpaper change | **PARTIAL** | six theme rows render and repaint (`08_planner.png`, `11_detail_tokyo.png`); a wallpaper change and the light/dark flip were not driven |
| 4h Translucent card legibility | **BLOCKED** | no widget to judge at 94 % |
| 4i Launcher icon, mask and themed variant | **PASS** (icon, mask) / **BLOCKED** (themed) | `02_app_info.png`, `03_home.png`: deep royal blue tile, ivory dial, gold hand, squircle-masked and uncropped, sitting beside the old 2.1.0 icon. Themed-icon toggle needs `WRITE_SECURE_SETTINGS` (denied) |
| 5a TalkBack control pass | **BLOCKED** | service cannot be enabled from the shell and cannot be driven without input injection |
| 5b Large font / display size | **BLOCKED** | both are Settings writes; also unverifiable without taps |
| 5c 28 dp rows, 24 dp refresh | **BLOCKED** | widget absent |
| 5d Device vs app theme | **BLOCKED** | widget absent |
| 6a Doze lag, screen-on redraw and reboot | **BLOCKED** | `dumpsys deviceidle force-idle` → `Unable to go deep idle; stopped at INACTIVE` on every attempt. Observed instead: the process survived 5+ minutes of screen-off and resumed drawing on `cmd power wakeup` |
| 6b Cold start, PSS, jank | **PASS** (release) | cold start **261 / 257 / 299 ms** (TotalTime, three runs); `TOTAL PSS 163 043 KB` (Graphics 93 994 KB) after the Clocks screen; 5.15 % janky of 330 frames, 50/90/95/99 = 13/34/38/97 ms. The frame sample is launch and tab navigation — a 10 s scroll needs a finger |
| 6c StrictMode | **PASS, with note** | see P10 |
| 6d Solar cache bound | **PASS** | 7 rows, `dateEpochDay` 20752–20754, today 20753: oldest is 1 day back, inside the 30-day bound |
| 6e Package facts | **PASS** | version/SDK as P3; no network permission; cert as P7; release APK 10 192 619 bytes |

The **instrumented suite** (debug build, `adb install -t -r` + `am instrument -w -r`) reports
`OK (3 tests)` in 184 s: first-launch defaults, map markers, and the drag/Room test above. Gradle's
`connectedDebugAndroidTest` cannot get past its install step on this ROM
(`INSTALL_FAILED_USER_RESTRICTED`); the manual route exercises the same APKs.

### 7.3 Findings

| # | Section | What I saw | Type (defect / judgement / expected) | Severity | Action |
| --- | --- | --- | --- | --- | --- |
| 1 | 4b | One frame was captured in **landscape** while the phone lay on its side; on the Tokyo detail the two clock lines looked vertically clipped mid-glyph | **OPEN — unconfirmed** | minor if real | The frame was stale (the screen had slept and `dumpsys gfxinfo` was static), and rotation cannot be forced, so it may be a capture artefact. Reproduce by opening the detail screen, rotating, and comparing with the portrait `05_clocks.png` |
| 2 | 4i | The new icon and the old 2.1.0 icon sit side by side on one home page; each opens a different app | **expected** | — | Documented consequence of the `applicationId` change (§0.5). Uninstall `com.worldclock.offline` to avoid the confusion |
| 3 | 5a | TalkBack is installed but off, and cannot be switched on from the shell | **expected on this ROM** | — | Blocked row, not a defect; see §0.2 for the toggle that unblocks input |
| 4 | 2.1 | No widget could be placed, so the 2×2/4×4 rows, translucency and refresh cadence are all still claims | **OPEN** | — | Needs one minute of thumb work; everything upstream (provider registration, preview marker, geometry tests) is green |
| 5 | 6a | `force-idle` refuses to reach deep idle on this HyperOS build | **expected on this ROM** | — | Use the natural-doze procedure (unplug, 20–30 min) with a widget placed |

**Suspects to confirm or clear first:**

| # | Suspect | What to look for | Status after this run |
| --- | --- | --- | --- |
| S1 | The widget uses the platform font, not Inter; figures are not tabular | Does the minute column shift width as digits change (`11:48` → `11:49`) where the app's rows sit still? | OPEN — no widget |
| S2 | The 4×4 footprint is a request; launchers resolve cells differently | Cramped card or large empty area at the default size; row count changing on resize | OPEN — no widget |
| S3 | The screen-off alarm is inexact and may be deferred by Doze | Lag on the lock screen; anything beyond ~5 minutes plus Doze deferral, or a card not refreshed on unlock | OPEN — no widget, no deep idle |
| S4 | The widget follows the device theme while the app can be pinned to any of six | Warm Sepia / High Contrast / Material You beside a neutral card; judge §15.12 | OPEN — no widget |
| S5 | `paneTitle` on an in-composition overlay may not be announced | Silence when the label dialog opens | OPEN — TalkBack blocked |
| S6 | Enlarged touch targets were raised to meet a guideline, not from a complaint | Does anything feel oversized or too spaced? | OPEN — needs a thumb |
| S7 | The six themes were judged by contrast tests, never by taste | Do Warm Sepia, Cool Slate, High Contrast and Material You read as designed themes? | PARTIAL — the six rows render; taste needs a person |
| S8 | The theme is read asynchronously, so a cold start can flash Light Monochrome | Force-stop and reopen with True Dark / High Contrast chosen: visible flash? (§15.7) | **CLEARED** — three cold starts with a stored theme showed no flash |
| S9 | Six theme rows make the settings sheet taller | Does it still fit comfortably, and not cover too much? | PARTIAL — `11_detail_tokyo.png` shows the sheet covering about two thirds of the screen and still scrolling |
| S10 | Widget rows are 28 dp and refresh 24 dp | Is a row easy to hit; is refresh reachable without opening the app? | OPEN — no widget |
| S11 | Debug StrictMode is log-only, so a violation is invisible unless logcat is read | Any `StrictMode policy violation` in §6c | **CLEARED for app code** — the only six violations are MediaTek's |
| S12 | The solar cache is pruned only when the app launches | If the app is never opened, the old cache stays; is any old row read? | **CLEARED for the bound** — 1 day back, 7 rows |
| S13 | Drag reordering competes with list scrolling | Does the long-press lift reliably, and does an edge drag scroll instead of dropping the card? | **PARTIAL** — the drag itself and its persistence are proven on hardware; feel and the edge-scroll trade-off still need a hand |
| S14 | The launcher decides how many dynamic shortcuts it accepts and in what order | Fewer than three city shortcuts, or usage reporting having no visible effect, is a platform finding | OPEN — the menu was never opened |
| S15 | The intro gate relies on proto presence, not just a boolean | Does a fresh install show it and an upgrade not? | **CLEARED for the install path** — the intro shows on a genuinely new package |
| S16 | Dynamic Material You colours come from the wallpaper and cannot be asserted on the JVM | Unreadable text or a selected card that does not stand out on some wallpapers | OPEN — wallpaper not changed |
| S17 | The legacy `mipmap-*dpi` PNGs are rendered from the vectors, not drawn by hand | If the icon changes but the rasters are not re-rendered, an old-device launcher would show the stale design. With minSdk 26 they are never loaded on a supported device | **CLEARED** — the launcher on API 36 resolves the adaptive XML, and it shows the Concept 3 artwork |
| S18 | The icon's gold accent is the only gold in the product | Does it read as PJ95 branding at 48 px, or as a stray highlight? Also check it against a light wallpaper | PARTIAL — at launcher size on a dark wallpaper it reads as a dial with a gold hand; a light wallpaper was not tried |

### 7.4 Sign-off

| | |
| --- | --- |
| Passed by | Automated ADB pass (agent), machine rows only |
| Date | 2026-10-27 |
| Build | debug `3.0.0 (4)` for the functional pass; release `3.0.0 (4)` left installed |
| Device | Redmi Turbo 4 (`IVQSS8HYWCCIM7EQ`), HyperOS, Android 16 |
| Verdict | ☐ PASS ☑ PASS WITH FINDINGS ☐ FAIL |

Not a full PASS: the machine surface is clean, but the widget, TalkBack, themed icon, landscape,
large-font and Doze rows are **BLOCKED**, not green. They are blocked by this ROM's shell
restrictions (§0.2), not by anything the build did. Re-run them with input injection enabled and
a widget placed before calling 3.0.0 hardware-verified.

### 7.5 3.1.0 checks — NOT RUN

Recorded on 2026-10-28, when the signed 3.1.0 release was installed **only** to verify its
certificate and version. The user asked that the new feature set not be exercised yet, so every
row below is **NOT RUN**, not PASS and not BLOCKED. Fill the status and the evidence in on the
next pass.

| Section | Status | What to record |
| --- | --- | --- |
| 6e Version and schema | **NOT RUN** | `versionCode=5 / 3.1.0`, `pragma user_version = 5`, `alarms.soundUri` present |
| 4j Comparison switch | **NOT RUN** | On after upgrade; off hides the line on cards, map panel and detail; planner unaffected; survives restart |
| 4g Four themes | **NOT RUN** | Exactly four rows; True Dark / High Contrast absent; a stored retired id lands on Light Monochrome |
| 4k Upgrade 3.0.0 → 3.1.0 | **NOT RUN** | Saved cities, theme and widget survive; alarm table created empty |
| 4k Alarm list, on/off, one-shot | **NOT RUN** | Ring time, switch persistence, one-shot switches itself off |
| 4k Wheel picker | **NOT RUN** | Fling settle, no keypad, wrap at 00/23, AM/PM changes the hour by 12 |
| 4k Material You dial | **NOT RUN** | Dial composes in the app palette; tapping hour then minute is learnable |
| 4k Sound preview and save | **NOT RUN** | One preview at a time, stops on leave, chosen tone survives, missing tone falls back |
| 4k Ring / snooze / dismiss | **NOT RUN** | Full-screen over the lock screen, snooze at ten minutes, dismiss leaves repeat state correct |
| 4k Reboot and zone change | **NOT RUN** | Alarm re-armed after boot; rings at wall-clock time in a new zone |
| 4k Exact-alarm notice | **NOT RUN** | Notice appears only when the grant is missing, and its action works |
| 4k Notification permission | **NOT RUN** | A denied `POST_NOTIFICATIONS` does not kill the ring |
| 5a TalkBack for alarms / switch | **NOT RUN** | Alarm card, wheels, sound rows and the comparison switch are announced with their state |
| 5b Largest font, alarm editor | **NOT RUN** | Wheels/dial, sound row and Save remain reachable |

No result is claimed for 3.1.0 in this file. The feature's JVM evidence is in
[SPECIFICATION.md](SPECIFICATION.md) §5.16, §11 (18a–18c) and §12.2.

### 7.6 4.0.0 Phase 1 checks — NOT RUN

Recorded on 2026-10-28, when the signed 4.0.0 release was installed **only** to verify its
certificate and version. The user asked that the new feature set not be exercised yet, so every
row below is **NOT RUN**, not PASS and not BLOCKED. Fill the status and the evidence in on the
next pass, using §4l.

| Section | Status | What to record |
| --- | --- | --- |
| 6e Version and schema | **NOT RUN** | `versionCode=6 / 4.0.0`, `pragma user_version = 6`, `timer` table present and empty |
| 4l Upgrade 3.1.0 → 4.0.0 | **NOT RUN** | Cities, theme, widget and alarms (with sound) survive; `timer` created empty |
| 4l `ALARMS · TIMER` control | **NOT RUN** | Reads as a choice, not a label; selected half announced as selected; bottom bar unchanged |
| 4l Timer wheels and presets | **NOT RUN** | Fling settle, wrap, preset fills the wheels, `00:00:00` is refused, large font fits |
| 4l Timer running | **NOT RUN** | Countdown notification counts down silently and survives background/screen-off |
| 4l Timer pause / resume / cancel | **NOT RUN** | Freeze, notification removed, alarm cancelled, resume continues from the remainder |
| 4l Timer in Doze | **NOT RUN** | Ring delay under `force-idle`, recorded in seconds |
| 4l Timer across reboot | **NOT RUN** | Re-armed after boot; an end instant passed while off rings at launch |
| 4l Timer ring | **NOT RUN** | Full-screen over the lock screen; Start again and Dismiss both behave |
| 4l Timer zone change | **NOT RUN** | Elapsed countdown unchanged by a zone change |
| 4l Four Settings status rows | **NOT RUN** | Each row's words match the platform; each opens the right HyperOS screen; state repaints on return |
| 4l Screen saver | **NOT RUN** | Immersive, awake for an hour, drift perceptible or not, tap/Back exits, no ghosting |
| 4l Screen saver is not a Daydream | **NOT RUN** | Absent from Display → Screen saver; never self-starts |
| 4l Privacy / Help / Studio | **NOT RUN** | Local content, no WebView, no price or purchase control |
| 4l Feedback mailto and clipboard | **NOT RUN** | Chooser appears with the diagnostics; clipboard fallback fires with no mail app |
| 4l No network permission | **NOT RUN** | `dumpsys package` lists neither `INTERNET` nor `ACCESS_NETWORK_STATE` |
| 5a TalkBack for the timer and screen saver | **NOT RUN** | Countdown spoken as time left; wheels one node each; screen saver leaves on a tap |
| 5b Largest font, timer | **NOT RUN** | Three wheels, presets and the primary action remain reachable |

No result is claimed for 4.0.0 in this file. The feature's JVM evidence is in
[SPECIFICATION.md](SPECIFICATION.md) §5.17–§5.19, §8.7 and §12.2.

### 7.7 4.1.0 alarm refinements and gestures — NOT RUN

Recorded on 2026-10-29 and extended on 2026-10-31, before any 4.1.0 build was installed on the
phone. The user asked that nothing be run on the device without explicit approval, so every row
below is **NOT RUN**, not PASS and not BLOCKED. Fill the status and the evidence in on the next
pass, using §4e and §4k.

| Section | Status | What to record |
| --- | --- | --- |
| 6e Version and schema | **NOT RUN** | `versionCode=7 / 4.1.0`, `pragma user_version = 8`, snooze and `sortOrder` columns present |
| 4k Upgrade 4.0.0 → 4.1.0 | **NOT RUN** | Cities, theme, widget, timer and alarms (with sound) survive; every alarm has `snoozePolicy = 0` and keeps its time-of-day position |
| 4k Simpler navigation | **NOT RUN** | No `ALARMS · TIMER` control; hourglass opens the timer; close and Back return to the list |
| 4k Compact cards | **NOT RUN** | Two-line cards, label · repeat together, more alarms visible than in 4.0.0 |
| 4k AM/PM beside the time | **NOT RUN** | Card, editor preview and ring show small `AM`/`PM`; TalkBack reads one value |
| 4k Timer deep link | **NOT RUN** | Tapping a timer notification lands on the countdown, not just the tab |
| 4e Alarm reorder | **NOT RUN** | Long-press drag and Move up/Move down persist the new order; force-stop keeps it; ringing time unchanged |
| 4e Alarm swipe + Undo | **NOT RUN** | Swipe deletes, `Alarm deleted` offers UNDO, and Undo restores the same id, settings and armed state |
| 4e City swipe + Undo | **NOT RUN** | Swipe deletes, `City removed` offers UNDO, and Undo restores position and active state |
| 4k Ring survives Doze | **NOT RUN** | Ring with the app swiped away, screen off and phone locked; tone loops on the alarm stream |
| 4k Full-screen ring | **NOT RUN** | Screen turns on over the lock screen; label is the hero; dark dynamic background; no quote |
| 4k Full-screen setting row | **NOT RUN** | Settings → Full-screen alarms matches the platform and repaints after the system screen |
| 4k Silent channel | **NOT RUN** | `alarm-ringing` has no sound; no 4.0.0 `alarms*` channel remains |
| 4k Snooze: Every time | **NOT RUN** | Repeated snoozes each re-arm ten minutes later; stored time and repeat days unchanged |
| 4k Snooze: Once | **NOT RUN** | First snooze accepted; re-fire offers Dismiss only with the explanation |
| 4k Snooze: Never | **NOT RUN** | No Snooze on screen or notification; the screen says why |
| 4k Critical-label suggestion | **NOT RUN** | A medication-like label offers TURN OFF SNOOZE; accepting saves Never; ordinary labels do not |
| 4k Ten-minute cap | **NOT RUN** | Audio stops after ten minutes; screen stays up; Dismiss still works |
| 4k Reboot and zone change | **NOT RUN** | Alarm re-armed after boot; rings at wall-clock time in a new zone |
| 4k Notification permission denied | **NOT RUN** | Record the actual behaviour; the service needs the notification and this is not claimed as a pass |
| 5a TalkBack for alarms | **NOT RUN** | Card time with AM/PM is one node; the snooze chips read as radio buttons with their state |
| 5b Largest font, alarms | **NOT RUN** | Two-line cards, chips, sound row and Save remain reachable; no clipped time |

No result is claimed for 4.1.0 in this file. The feature's JVM evidence is in
[SPECIFICATION.md](SPECIFICATION.md) §5.16, §8.6 and §12.2.

### 7.8 1.0.10 rebrand and module cleanup — AUTOMATED ROWS PASS, MANUAL ROWS PENDING

Prepared on 2026-11-12 and first run on **2026-11-13** on the reference phone
(Redmi Turbo 4 `24129RT7CC`, Android 16 / HyperOS V816) over ADB. The rebrand changes
identifiers, signing and module boundaries, so this pass confirms a clean fresh install.

Environment notes from the run:

- MIUI blocked the first two `adb install` attempts with
  `INSTALL_FAILED_USER_RESTRICTED` until **Developer options → Install via USB** was
  enabled on the phone; after that the same command installed cleanly.
- `run-as` only works on the **debug** track, so the database checks ran on a temporary
  debug install; the release build was uninstalled first (the signing key differs) and
  reinstalled afterwards, so the phone ends on the release build.
- This ROM refuses injected input (`input tap` → `INJECT_EVENTS` `SecurityException`), so
  the widget placement and TalkBack rows stay manual.

| Section | Status | Result |
| --- | --- | --- |
| 6e Version and schema | **PASS** | `versionCode=11 / 1.0.10`, label `Clockwise`; `pragma user_version = 10`; tables `cities`, `saved_cities`, `solar_days`, `alarms`, `timer`, `widget_configs`, `widget_licenses`, `room_master_table`; five default cities seeded in order (Tokyo, Sydney, Los Angeles, New York, London) |
| 4k Fresh install over 4.1.0 | **PASS** | `adb uninstall` then install succeeded; the same-build `-r` path was not needed this pass because the phone had no 4.1.0 install left (see §7.7 for the migration rows) |
| 4f Fresh install | **PASS** | Intro shows once on a fresh install (confirmed on the debug track; the release first-open was completed during setup), five default cities seeded, no crash in `logcat -b crash` |
| 5a Widget after rename | **NOT RUN** | Widget placement is a manual action on this ROM |
| 5b Deep links and shortcuts | **PASS** | City deep link (`CITY_ID`/`SHORTCUT_ID` extras) opened the Tokyo detail screen; the `planner` shortcut opened the meeting planner; dynamic city shortcuts published (`city-sydney`, `city-los-angeles`); six widget providers registered |
| 6a Signing | **PASS** | Host-side `apksigner verify --print-certs` on the installed APK: `CN=Clockwise`, SHA-256 `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9` |
| 6b No network | **PASS** | `aapt2 dump permissions`: no `INTERNET`, no `ACCESS_NETWORK_STATE`; only the alarm/notification set plus `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |
| 7a Database file | **PASS** | `databases/` holds only `clockwise.db` (+`-wal`/`-shm`); no `world_clock.db`; 0 alarms, 0 timers, solar cache empty on the fresh install |
| 7b Premium widget style | **PENDING** | The catalog row `premium_clockwise` exists in `widget_licenses` (palette `MATERIAL_YOU`, locked); the stored style id check needs a widget placed (manual) |
| 5c TalkBack | **NOT RUN** | Manual |

Automated rows are recorded; the widget, TalkBack and alarm/timer rows still need a human
run before any store action. The JVM evidence is in [SPECIFICATION.md](SPECIFICATION.md)
§12.2 and in [CHANGELOG.md](CHANGELOG.md).

---

## 8. Re-running everything

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.pj95.clockwise"
$activity = "com.pj95.clockwise/com.pj95.clockwise.MainActivity"
$provider = "com.pj95.clockwise/com.pj95.clockwise.widget.ClockwiseWidgetReceiver"
$shots = "$env:TEMP\opencode\clockwise-device"
New-Item -ItemType Directory -Force $shots | Out-Null

# Preconditions
& $adb devices -l
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell dumpsys package $pkg | Select-String "versionCode|versionName"
& $adb shell dumpsys appwidget | Select-String $provider -Context 0,6

# Launch and screenshot
& $adb shell am start -n $activity
& $adb shell screencap -p /sdcard/wc.png
& $adb pull /sdcard/wc.png "$shots\wc_live.png"

# Deep link (no tap needed)
& $adb shell am start -n $activity --es com.pj95.clockwise.extra.CITY_ID tokyo

# Launcher shortcuts and Room order
& $adb shell dumpsys shortcut | Select-String "com.pj95.clockwise" -Context 0,12
foreach ($f in "clockwise.db","clockwise.db-wal","clockwise.db-shm") {
  $b64 = & $adb shell "run-as $pkg base64 databases/$f"
  [IO.File]::WriteAllBytes("$shots\db\$f", [Convert]::FromBase64String(($b64 -join '')))
}
sqlite3 "$shots\db\clockwise.db" "pragma user_version; select * from saved_cities order by position; select id, hour, minute, enabled, repeatDays, soundUri, snoozePolicy, snoozesUsed, snoozedAtEpochMillis, sortOrder from alarms order by sortOrder; select * from timer;"

# Alarms and idle: an armed alarm uses setAlarmClock, a running timer setExactAndAllowWhileIdle,
# a widget tick set/setAndAllowWhileIdle
& $adb shell dumpsys alarm | Select-String "clockwise" -Context 1,6
& $adb shell dumpsys deviceidle get deep

# Generated preview marker
& $adb shell run-as $pkg cat shared_prefs/widget_previews.xml

# StrictMode (debug)
& $adb logcat -d -s StrictMode

# Release permissions and signature
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe" dump permissions "app\build\outputs\apk\release\app-release.apk"
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs "app\build\outputs\apk\release\app-release.apk"
```

### TalkBack from the shell (optional, and not done for you)

TalkBack changes how the whole phone is used, so turning it on is a human decision. **On the
3.0.0 reference phone these commands fail** — `settings put secure …` needs
`WRITE_SECURE_SETTINGS`, which the shell was not granted (§0.2). Use
**Settings → Accessibility → TalkBack** instead. Kept for a phone whose shell does hold the
permission:

```powershell
# ON
& $adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
& $adb shell settings put secure accessibility_enabled 1

# BACK OFF (restores the service that was enabled before: com.milink.service/...MLAcessibilityService)
& $adb shell settings put secure enabled_accessibility_services com.milink.service/com.miui.circulate.world.MLAccessibilityService
& $adb shell settings put secure accessibility_enabled 1
```

### Instrumented suite without Gradle's install step

```powershell
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb install -t -r "app\build\outputs\apk\debug\app-debug-androidTest.apk"
& $adb shell am instrument -w -r com.pj95.clockwise.test/androidx.test.runner.AndroidJUnitRunner
```

Expect `OK (3 tests)`, about three minutes. `gradlew connectedDebugAndroidTest` fails before the
first test with `INSTALL_FAILED_USER_RESTRICTED` unless MIUI's *Install via USB* is on.

---

## 9. What happens to this file

Findings go back into [SPECIFICATION.md](SPECIFICATION.md):

- Something broken → fixed, and a criterion or a row in §9 added so it cannot come back silently.
- Something disagreeable → a decision record in §5.11, §5.12 or §15.12, with what was rejected.
- Something still unverifiable → stays OPEN in §15.10/§15.11/§15.15/§15.16, with the reason.

The build facts and test counts live in §12.2; the release procedure lives in
[RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md).
