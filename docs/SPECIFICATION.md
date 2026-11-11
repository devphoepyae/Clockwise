# Clockwise — Living Specification

> **Document status:** Living document. It is updated after every working session.
> **App id:** `applicationId = com.pj95.clockwise`, `namespace = com.pj95.clockwise`
> **Platform:** Native Android — Kotlin + Jetpack Compose (no React Native, no Expo, no WebView, no web page)
> **Phase:** v1.0.10 Clockwise rebrand and multi-module Clean Architecture pass (full `WorldClock` → `Clockwise` identity cleanup, new `clockwise.db`, `premium_clockwise` widget style, new `clockwise.jks` signing key, site files removed). M0–M5 (through 2.0.0), the 2.1 feature pass, the 3.0 rebrand, the 3.1 alarm/theme/comparison pass, the 4.0 Phase 1 pass, the 4.1 alarm/gesture pass and the 1.0.10 rebrand pass are complete. The code is versioned `versionCode 11` / `versionName 1.0.10`; v1.0, v2.0.0 and 2.1.0 remain the historical shipped baselines under the retired `com.worldclock.offline` id.
> **Last updated:** 2026-11-12

---

## 0. How to read this document

This file is the single source of truth for what the app must do and what has actually
been built. It has three kinds of content, and they are labelled separately:

| Marker | Meaning |
| --- | --- |
| **REQUIREMENT** | A rule that must hold. Changing one requires an explicit decision. |
| **IMPLEMENTED** | What the code does today. Verified against the source tree. |
| **OPEN** | Unresolved. Needs a human decision. |

### 0.1 Section 11 provenance — RESOLVED

The original specification was supplied in conversation and the verbatim text of
**section 11 (acceptance criteria)** was not preserved in the working context, so the list in
[section 11](#11-acceptance-criteria) was reconstructed from the requirements that *were*
preserved.

**RESOLVED — 2026-10-17.** The user reviewed the reconstruction and confirmed it as correct
("လုံးဝ မှန်ကန်ပါတယ်" — *entirely correct*), with no changes requested. The
`criterionNN_*` test names are therefore final and the criteria are now REQUIREMENTs, not a
pending reconstruction.

### 0.2 Naming convention used for tests

Acceptance tests are named `criterionNN_description` so that a failing build points
directly at the requirement it breaks.

### 0.3 v2.0.0 release scope and deferred localization — SHIPPED LOCALLY

The v2.0.0 release added four capabilities on top of the v1.0 baseline:

1. Modern home-screen widgets built with Jetpack Glance 1.2.0, with responsive sizes,
   generated previews and interactive actions.
2. Solar-map time travel: scrub the map through the day instead of only showing "now".
3. A time-zone converter and meeting planner on top of the existing planner maths.
4. A sunrise/sunset engine with offline solar ephemeris calculations.

The release shipped as **`versionCode 2`, `versionName 2.0.0`**, under the application id
`com.worldclock.offline` and the existing signing key. M5 added the hardening that makes the
release defensible rather than only feature-complete: a bounded solar cache, application-scope
error handling, debug-only StrictMode and a large-font accessibility render. The release
procedure is in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) and the hardware worksheet in
[HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md). It is kept here as the 2.0.0 baseline; the
current release is 3.0.0 (§0.5).

**Localization is deliberately deferred.** This release ships an English-only UI. Burmese
and any other additional language will be considered in a later version; no Burmese
strings, resources or UI choices are part of v2. The existing bundled city names and
labels remain English, as required by constraint 9.

### 0.4 v2.1.0 release scope — DECISION

The 2.1.0 release is a feature release on top of 2.0.0. It adds:

1. **Material You** as a sixth, opt-in theme, with a static Material 3 light/dark fallback below
   Android 12. The five existing themes and their persisted ids are unchanged, and Light
   Monochrome remains the default.
2. **Long-press drag reordering** of the city list, with the display order owned by Room
   (`saved_cities`, database v3) and mirrored into Proto DataStore. Accessibility move actions
   provide the same reorder without a drag.
3. **Launcher shortcuts**: three static quick actions plus a budgeted set of dynamic city
   shortcuts, using the same city deep link as a widget row.
4. **A one-page first-launch intro**, gated in `MainActivity`, with a Settings replay action.
   Fresh installs see it; stores written before the field existed are treated as completed.
5. **A translucent, Material 3 widget card** at 94% opacity, a fifth (2×2) responsive size, and a
   resize floor of 180×110 dp.
6. **Battery-aware widget ticking**: one minute while the screen is on, five inexact minutes while
   it is off, an immediate redraw on screen-on, and redraws on time/time-zone/date changes.

The release ships as **`versionCode 3`, `versionName 2.1.0`**, under the same application id,
minimum/target SDK levels, signing key and no-network permission set. The release procedure is in
[RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md); the release note is
[CHANGELOG.md](CHANGELOG.md#v210).

Localization remains deliberately deferred, exactly as in 2.0.0.

### 0.5 v3.0.0 rebrand scope - DECISION

The 3.0.0 release does not add product behaviour. It changes who ships it and what it looks like:

1. **PJ95 package identity.** `applicationId` and `namespace` both become
   `com.pj95.clockwise`. Every package declaration, import, manifest component, intent action,
   deep-link extra, Room schema path and proto `java_package` moves with them.
2. **"Concept 3" app icon.** A deep royal blue adaptive icon with an ivory dial and gold accents,
   replacing the off-white monochrome mark.
3. **Studio branding** in the documents and the landing page.

**Decision record - the id change is a new app, not an update (2026-10-27).** Because
`applicationId` changes, Android treats 3.0.0 as an unrelated package: it does not upgrade over
`com.worldclock.offline` 2.1.0, it installs beside it, and a user's saved cities, theme and
widgets do not carry across. That is inherent to the request, not an oversight. It is also why
this is `versionCode 4` / `versionName 3.0.0` rather than a `2.2.0`: the version is the only
honest signal that the identity broke. Everything a user can do *inside* the app is unchanged,
and the "keep existing data" upgrade path from 2.1.0 no longer exists.

**Decision record - the visible name does not change (2026-10-27).** "Chronos" is the project
and studio codename; `app_name` stays `Clockwise`, so the launcher label, Recents card, Settings
entry and widget-picker label are unchanged. Renaming the product was not part of the request and
would invalidate every screenshot and worksheet instruction for no functional gain.

Localization remains deliberately deferred.

### 0.6 v3.1.0 scope - DECISION

3.1.0 is a focused usability release on top of 3.0.0. It adds the alarm feature and then simplifies
how the app is chosen and configured:

1. **Alarms.** A third bottom-bar destination (`Alarms`) lists the saved alarms; an alarm rings
   on the device's own clock with a full-screen notification, snooze and dismiss. Room gains an
   `alarms` table (database v4) and the alarm is armed through `AlarmManager`.
2. **Two themes retired.** True Dark and High Contrast are removed from Settings and from the
   palette set. The app ships **four** themes: Light Monochrome (default), Warm Sepia, Cool Slate
   and Material You. Stored ids `1` and `4` read as the default. The widget keeps a dark pair of
   its own (`NightClockwisePalette`) because it follows the device, not the app theme.
3. **A comparison switch.** Settings gains "Compared with your local time", on by default. Off
   removes the difference line from the city cards, the map's selected-city panel and the city
   detail screen. The meeting planner keeps its comparisons: it is the tool whose whole purpose is
   comparing times.
4. **Wheel time pickers.** The alarm editor no longer accepts typed digits. Hour and minute are
   scrollable wheels under every theme, and the platform's Material time picker (clock dial) under
   Material You.
5. **A per-alarm sound.** The editor gains a `SOUND` section — a clean list of the device's alarm
   tones, each previewable — stored per alarm (`alarms.soundUri`, database v5).

The release ships as **`versionCode 5`, `versionName 3.1.0`**, under the same application id,
minimum/target SDK levels, signing key and no-network permission set. Alarm timing and sound are
device-local; the app still declares no `INTERNET` permission and has no network path.

Localization remains deliberately deferred.

### 0.7 v4.0.0 Phase 1 scope - DECISION

4.0.0 is **Phase 1 of "Chronos"**: the app becomes an alarm clock and timer as well as a world
clock, and gains the minimum structure a release needs, without adding a network path.

1. **A countdown timer.** The Alarms tab gains an `ALARMS · TIMER` segmented control. One timer is
   active at a time; it is stored as an **end instant** (`timer` table, database v6), so it keeps
   counting while the app is closed, survives a reboot, and cannot drift by a missed tick. It runs
   through `AlarmManager.setExactAndAllowWhileIdle`, shows a quiet countdown notification using the
   platform chronometer, and rings at zero with a full-screen intent, **Start again** and
   **Dismiss**.
2. **Screen saver.** A non-exported `ScreenSaverActivity` renders a full-screen desk clock: black,
   no chrome, the time and date, a slow burn-in drift between five positions, and
   `FLAG_KEEP_SCREEN_ON` while visible. Tap anywhere or Back to leave. It is not an Android
   Daydream and never starts by itself.
3. **Settings becomes the app's control panel.** The sheet is grouped `APPEARANCE` ·
   `COMPARED WITH YOUR LOCAL TIME` · `ALARMS` · `EXTRAS` · `CHRONOS STUDIO`. The `ALARMS` group
   reads the four platform grants (exact alarms, notifications, DND bypass, battery optimisation)
   and each row opens the matching system screen. `EXTRAS` holds **Screen saver**, **Privacy
   policy**, **Help** and **Send feedback**.
4. **Zero data collection, stated in the app.** Privacy, Help and the Chronos Studio placeholder
   are local Compose screens — no WebView, no hosted page, no billing library. **Send feedback**
   opens the user's own mail app with a short diagnostic block (version, Android, device, locale,
   time zone, the four grant states) that the user can read and edit; if there is no mail app the
   same text is copied to the clipboard.
5. **A widget gallery placeholder.** `CHRONOS STUDIO` names the future gallery and says plainly
   that nothing can be browsed or bought yet. No Play Billing dependency ships in Phase 1.

The release ships as **`versionCode 6`, `versionName 4.0.0`**, under the same application id,
SDK levels and signing key. The APK still declares **no `INTERNET` and no `ACCESS_NETWORK_STATE`
permission**, and `OfflineGuardrailTest` asserts that against the merged manifest and the runtime
classpath so a dependency bump cannot quietly reintroduce one.

Localization remains deliberately deferred.

### 0.8 v4.1.0 alarm refinement and gesture scope - DECISION

4.1.0 is a focused pass over the alarms shipped in 3.1.0/4.0.0. It adds no network path, no
dependency and no new destination.

1. **A simpler alarm screen.** The `ALARMS · TIMER` segmented control is removed. The alarm list is
   the destination; a single hourglass in the header opens the countdown, and the close button or
   system Back returns. A timer notification deep-links straight to the countdown. Alarms are
   compact two-line cards (time, then label · repeat) and **AM/PM** is set small beside the time.
2. **A ring the platform cannot quietly silence.** The ring moves out of the notification channel
   and into `AlarmRingingService`: a `mediaPlayback` foreground service playing a looping
   `MediaPlayer` on `AudioAttributes.USAGE_ALARM`, with `setWakeMode`, an explicit wake lock and a
   repeating vibration. The channel is silent and the 4.0.0 `alarms*` channels are deleted.
3. **Full-screen ringing.** `USE_FULL_SCREEN_INTENT` opens `AlarmRingingActivity` over the lock
   screen. The screen uses the wallpaper's **dark** dynamic scheme regardless of the app theme, and
   shows only the user's label, the time with AM/PM, and the actions the policy allows.
4. **Per-alarm snooze.** `SnoozePolicy { ALLOWED, ONCE, NONE }` is stored per alarm with a
   per-cycle counter and marker. A medication-like label suggests `NONE`, but the choice is the
   user's. The rule is enforced by the ringing screen, the notification and the repository.
5. **Alarm drag-to-reorder.** A long-press vertical drag rearranges the alarm list exactly as the
   city list already works. The position is stored per alarm (`sortOrder`, Room v8) and backfilled
   by time of day on upgrade. Moving a card never re-arms anything.
6. **Swipe-to-delete with Undo.** A right-to-left swipe removes an alarm or city card. A snackbar
   offers **UNDO** for long enough to change your mind; Undo restores the exact alarm row (id,
   position, settings) or the city's previous order and active state. Both card types also expose
   Move up / Move down / delete custom actions for TalkBack.

The release ships as **`versionCode 7`, `versionName 4.1.0`**, under the same application id, SDK
levels and signing key. The APK still declares **no `INTERNET` and no `ACCESS_NETWORK_STATE`
permission**.

Localization remains deliberately deferred.

---

## 1. Product overview

Clockwise: the user keeps an ordered list of cities, sees each city's current local
time, and can explore where those cities sit on a schematic globe with a live day/night
terminator.

Everything is offline. There is no backend, no account, no cloud sync, no push
notification, no weather, no online map tiles and no map-provider API key.

**REQUIREMENT — distribution:** local/debug builds only at this stage.

**REQUIREMENT - reference designs:** `docs/reference-designs/sample_01.jpg` and
`docs/reference-designs/sample_02.jpg` are the source of truth for layout. They must not be deleted.

---

## 2. Non-negotiable constraints

These were agreed up front and are not open for reinterpretation during implementation.

1. **Native Android.** Kotlin + Jetpack Compose.
2. **Material 3 for behaviour and accessibility only** — never for a built-in colour. No
   component may fall back to a Material default, so the Material 3 default purple/blue accent
   cannot appear. The one exception is the **opt-in Material You** theme, which deliberately uses
   the platform's wallpaper-derived scheme (added 2026-10-27); it is never the default.
3. **Monochrome palette by default.** No gradients. The three hand-built themes are monochrome;
   the opt-in Material You theme adds wallpaper-derived colour. Phase 1 was light-only; phase 2
   added the warm and cool variants, and 3.1.0 retired the two high-contrast ones (see
   [section 7.1](#71-colour)).
4. **No backend, no API, no accounts, no cloud sync, no push, no weather.**
5. **No online map tiles, no map provider, no API key.** The map is drawn in Compose
   `Canvas` from bundled vector data.
6. **Timezone maths uses `java.time`** (`ZoneId`, `ZonedDateTime`, `ZoneRules`) with
   **IANA ids only**. UTC offsets are **never** hard-coded; DST is automatic.
7. **Sun icon for local hour 06:00–17:59, moon icon for 18:00–05:59.**
8. **Persistence via Jetpack Proto DataStore:** ordered saved-city id list, active city id,
   `use24Hour`, the theme, the local-time comparison switch and the first-launch gate. Restored on
   launch. Unknown ids are dropped. Failures surface a non-blocking, understandable message — never
   a silent swallow.
9. **English UI labels. Emoji are never used as UI icons.** Icon-only controls carry
   accessibility labels; selected and disabled states carry semantics.
10. **Must lay out on a 360 dp wide portrait screen without text clipping.**
11. **Inter typography** (Regular / Medium / SemiBold / Bold); base corner radius **16 dp**;
    city card height **≈ 93 dp**; horizontal page padding **≈ 24 dp**.

### 2.1 Agreed decisions (user-approved)

| Decision | Value |
| --- | --- |
| `applicationId` | `com.pj95.clockwise` (namespace is the same string since 3.0.0) |
| `minSdk` | 26 |
| `compileSdk` / `targetSdk` | 35 / 34 — compileSdk 35 is required by Glance 1.2.0; targetSdk stays 34 for this release |
| Localization | English-only in v2; additional languages (including Burmese) are deferred to a later version |
| 12h / 24h | Applies app-wide **and** persists |
| Map day/night shading | Included |
| Verification | JVM screenshot tests **plus** device end-to-end |
| Dependency injection | Manual `AppContainer` — **confirmed by the user**; Hilt rejected (keeps the app light, no APK growth, faster compile, existing tests already pass) |
| Instrumented / Espresso tests | **Deprioritised by the user** — flaky across OEM animation and timing settings. Unit + Roborazzi tests carry the coverage; the device suite is a minimal smoke test |
| Acceptance criteria | Section 11 wording **confirmed by the user** — see [section 0.1](#01-section-11-provenance--resolved) |
| Themes | **Four**: Light Monochrome, Warm Sepia, Cool Slate and the opt-in **Material You** palette. Each is one tap, persisted, default Light Monochrome. True Dark and High Contrast were retired in 3.1.0 (**user choice**, 2026-10-28) |
| City display order | **Room** `saved_cities` (database v3) owns the order; Proto DataStore keeps membership/settings and is mirrored on every change (**user choice**, 2026-10-27) |
| Comparison with local time | **On by default**, switchable in Settings; off hides the difference on the cards, the map panel and the detail screen, and leaves the planner's comparisons alone (**user choice**, 2026-10-28) |
| Alarm time input | **Wheel pickers** by default, the platform **clock dial** under Material You; no keypad in either case (**user choice**, 2026-10-28) |
| Alarm sound | Per alarm, chosen from a previewable list of the device's alarm tones; empty means the device default (database v5, `alarms.soundUri`) (**user choice**, 2026-10-28) |
| Launcher shortcuts | Three static quick actions (`Add city`, `Plan`, `Map`) plus up to three dynamic city shortcuts, budgeted against the launcher's total limit (**user choice**, 2026-10-27) |
| First-launch intro | One page, one action, gated in `MainActivity`; replayable from Settings; absent field means completed for upgraders (**user choice**, 2026-10-27) |
| Theme picker location | Settings sheet opened from a gear action on the Clocks screen (**user choice**, 2026-10-17) |
| Theme tokens | Light Monochrome `#F3F3F1` / `#111111`; Warm Sepia `#F7F4EF` / `#2C2521`; Cool Slate `#EEF2F5` / `#1E293B`; Material You from the platform scheme (**user choice**, 2026-10-18, 2026-10-27 and 2026-10-28) |
| Widget palette | Follows the **device's** light/dark setting, not the chosen app theme — including Material You. Still open; see [section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme) |
| Widget card | A 94%-opaque Material 3 rounded shape with a hairline outline, so wallpaper can tint but never obscure text (**user choice**, 2026-10-27) |
| Widget ticking | One-minute `setAndAllowWhileIdle` while the screen is on; five-minute inexact `set` while it is off; immediate redraw on screen-on (**user choice**, 2026-10-27) |

---

## 3. Technology stack

**IMPLEMENTED** — exact versions in use.

### 3.1 Build

| Component | Version | Where |
| --- | --- | --- |
| Gradle wrapper | 8.11.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.7.3 | `build.gradle.kts` |
| Kotlin | 2.0.21 | `build.gradle.kts` |
| Compose compiler plugin | `org.jetbrains.kotlin.plugin.compose` 2.0.21 | `build.gradle.kts` |
| KSP | `com.google.devtools.ksp` 2.0.21-1.0.28 | `build.gradle.kts` |
| protobuf-gradle-plugin | 0.9.4 | `build.gradle.kts` |
| `buildToolsVersion` | 35.0.0 (installed automatically by the first compileSdk 35 build) | `app/build.gradle.kts` |
| JDK / `jvmTarget` | 17 | `app/build.gradle.kts` |
| Version catalog | none — versions are inline | — |

### 3.2 Runtime dependencies

| Artifact | Version |
| --- | --- |
| `androidx.compose:compose-bom` | 2025.02.00 (Compose 1.7.8) |
| `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview` | from BOM |
| `androidx.compose.material3:material3` | from BOM |
| `androidx.core:core-ktx` | 1.13.1 |
| `androidx.activity:activity-compose` | 1.9.3 |
| `androidx.lifecycle:lifecycle-runtime-ktx` / `-runtime-compose` / `-viewmodel-compose` | 2.8.7 |
| `androidx.navigation:navigation-compose` | 2.8.4 |
| `androidx.datastore:datastore` | 1.1.1 |
| `androidx.room:room-runtime` / `room-ktx` | 2.6.1 |
| `androidx.room:room-compiler` (KSP) | 2.6.1 |
| `androidx.glance:glance-appwidget` | 1.2.0 |
| `androidx.glance:glance-material3` | 1.2.0 |
| `com.google.protobuf:protobuf-javalite` | 3.25.5 |
| `com.google.protobuf:protoc` | 3.25.5 |

Proto DataStore remains the source of truth for membership and settings (the saved-city id
**set**, active city, 12h/24h, theme, labels and onboarding state). Room owns the queryable city
catalog, the solar cache and — from v2.1 — the saved-city **display order**; it is not a second
store for settings. `ClockRepository` reconciles the two on read and writes both on every reorder,
membership change or label change, so the widget's DataStore fallback and the screens cannot
disagree.

### 3.3 Test dependencies

| Artifact | Version | Scope |
| --- | --- | --- |
| `junit:junit` | 4.13.2 | `testImplementation` |
| `androidx.compose.ui:ui-test-junit4` | from BOM | `test` + `androidTest` |
| `androidx.test.ext:junit` | 1.2.1 | `test` + `androidTest` |
| `org.robolectric:robolectric` | 4.14.1 | `testImplementation` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.8.1 | `testImplementation` |
| `io.github.takahirom.roborazzi:roborazzi` | 1.26.0 | `testImplementation` |
| `io.github.takahirom.roborazzi:roborazzi-compose` | 1.26.0 | `testImplementation` |
| `androidx.room:room-testing` | 2.6.1 | `test` + `androidTest` |
| `androidx.glance:glance-appwidget-testing` | 1.2.0 | `testImplementation` |
| `androidx.test:runner` | 1.6.2 | `androidTestImplementation` |
| `androidx.compose.ui:ui-test-manifest` | from BOM | `debugImplementation` |
| `androidx.compose.ui:ui-tooling` | from BOM | `debugImplementation` |

**Decision record:** `androidx.test:rules` and `espresso-core` were removed from
`androidTestImplementation`. They are no longer used: the device suite drives the app through
Compose's own test APIs, and Espresso's `pressBack()` is unusable on this device anyway
(see [section 15.1](#151-resolved--instrumented-test-defects)).

### 3.4 Assets

| Asset | Source |
| --- | --- |
| Inter Regular / Medium / SemiBold / Bold | `rsms/inter` v4.1 release, bundled as `res/font/inter_*.ttf` |
| Icons | Material Design Icons *outlined* 24 px SVGs hand-converted to vector drawables (`material-icons-extended` is **not** used) |

---

## 4. Architecture and directory layout

**IMPLEMENTED** — single Gradle module `:app`.

```text
app/src/main/
  proto/saved_cities.proto                  Proto DataStore schema
  java/com/pj95/clockwise/
    ClockwiseApp.kt                        Application; owns AppContainer, widget mirror + preview publishing
    MainActivity.kt                         Single activity; onboarding gate; singleTop city/shortcut deep links; enableEdgeToEdge()
    di/AppContainer.kt                      Manual DI + Context.appContainer
    alarm/                                  Alarm feature plumbing (3.1.0)
      AlarmReceiver.kt                      Fires the ringing notification, then re-arms or switches the alarm off
      AlarmRescheduleReceiver.kt            Boot / time / time-zone change -> re-arm every stored alarm
      AlarmRingingActivity.kt               Full-screen ring over the lock screen; Snooze and Dismiss actions
      AlarmNotifications.kt                 Channel per sound, CATEGORY_ALARM notification, full-screen intent
      AlarmSoundCatalog.kt                  Device alarm tones, preview, resolveAlarmSoundUri
      AndroidAlarmScheduler.kt              AlarmScheduling via setAlarmClock, snooze, exact-alarm check
    shortcuts/CityShortcuts.kt              Static action ids, dynamic city shortcuts, budget, city intent
    shortcuts/ShortcutMirror.kt             Saved ids/labels -> launcher shortcut republication
    data/
      local/ClockwiseDatabase.kt           Room database (v8: catalog + solar cache + saved-city order + alarms + timer)
      local/entity/CityEntity.kt            Room row + City <-> CityEntity mapping
      local/entity/SolarDayEntity.kt        Cached solar day + SolarEvents <-> row mapping
      local/entity/SavedCityEntity.kt       cityId + position row for the user's display order
      local/entity/AlarmEntity.kt           hour, minute, label, enabled, repeatDays, soundUri, sortOrder
      local/dao/CityDao.kt                  observe / getAll / findById / upsertAll / count / search
      local/dao/SolarDao.kt                 find / upsert / deleteOlderThan / clear / count
      local/dao/SavedCityDao.kt             ordered flow / replaceAll / appendIfMissing / remove
      local/dao/AlarmDao.kt                 observe / getById / insert / update / delete / setEnabled / maxSortOrder / reorder
      local/seed/CitySeeder.kt              Idempotent copy of the bundled directory into Room
      model/City.kt                         City(id, name, region, country, timeZone, lat, lon)
      model/CityDirectory.kt                The 22-city offline directory + search
      prefs/UserPreferences.kt              Domain model of persisted state
      prefs/AppTheme.kt                     The four themes (ids 0, 2, 3, 5) + proto mapping + retirement
      prefs/UserPreferencesSerializer.kt    DataStore Serializer
      repository/CityRepository.kt          Catalog flow, search and find backed by Room
      repository/CityOrderStore.kt          Room-backed display order used by ClockRepository
      repository/ClockRepository.kt         add / remove / reorder / select / setUse24Hour / setTheme / label / onboarding / comparison
      repository/AlarmRepository.kt         AlarmRepository + RoomAlarmRepository (store, then arm or cancel)
      repository/PrefsError.kt              Error type surfaced to the UI
      repository/SolarRepository.kt         Sunrise/sunset with the solar cache; prune and clear
    domain/
      time/SecondTicker.kt                  Emits the current Instant once per second
      time/CityTime.kt                      Per-city computed time value object
      time/CityTimeFormatter.kt             IANA-aware formatting + offset labels
      time/TimeDifference.kt                Offset gap between two zones at one instant
      time/MeetingTime.kt                   MinuteOfDay value class + working-day rule
      alarm/Alarm.kt                        Alarm value, RepeatDays mask, nextTrigger
      alarm/AlarmScheduling.kt              The scheduler seam (schedule / cancel / snooze / canScheduleExact)
      map/GeoPoint.kt                       lat/lon pair
      map/EquirectangularProjection.kt      x/y fractions for lat/lon
      map/SolarPosition.kt                  Subsolar point, terminator, isDaylight
      map/WorldLandmasses.kt                26 hand-authored landmass outlines
      solar/SolarEphemeris.kt              NOAA declination + equation of time
      solar/SolarEvents.kt                 Sunrise, sunset, solar noon, polar-day/night result
      solar/SunriseSunsetCalculator.kt     Civil date + point + zone -> SolarEvents
    widget/
      ClockwiseWidget.kt                  GlanceAppWidget; responsive sizes + generated preview
      ClockwiseWidgetReceiver.kt          Provider receiver; owns ticker/mirror lifecycle
      RefreshWidgetAction.kt               In-place redraw from the card's refresh glyph
      WidgetMirror.kt                      Saved-cities/labels/time-format -> immediate redraw
      WidgetTicker.kt                      Screen-aware alarm + private tick receiver
      WidgetPreviewPublisher.kt            API 35+ generated picker previews, once per design
    ui/
      ClockwiseRoot.kt                     Scaffold shell, NavHost, search + settings sheets, editor, label dialog, snackbar, deep links
      ClockwiseViewModel.kt                State holder
      ClockwiseUiState.kt                  UI state model
      AlarmViewModel.kt                     AlarmUiState + AlarmViewModel
      PlannerUiState.kt                     Meeting planner state model
      WidgetUiState.kt                      Widget row/state model
      mapping/ClockwiseStateMapper.kt      buildClockwiseUiState — the only path from data to UI
      mapping/PlannerStateMapper.kt         buildPlannerUiState — source conversion, best overlap
      mapping/WidgetStateMapper.kt          buildWidgetUiState + WIDGET_MAX_ROWS
      navigation/Routes.kt                  Route constants (alarms, clocks, map, planner, city detail)
      components/CityCard.kt                CityCard + CityCardModel (shared by list and map)
      components/SwipeToDeleteBox.kt        Shared right-to-left swipe-to-delete wrapper
      components/CircleIconButton.kt        Icon-only button; drawn at 44 dp, 48 dp touch target
      components/DaySlider.kt               Hand-drawn 24-hour planner slider + DaySliderTags
      components/TimeDifferenceLabel.kt     "Same time" / "+2 hrs 30 mins ahead" wording
      components/ClockwiseBottomBar.kt     Alarms / Clocks / Map
      components/ClockwiseSegmentedControl.kt  12h / 24h segmented control (48 dp options)
      components/WheelTimePicker.kt         Snapping hour/minute wheels + AlarmTimeTestTags
      components/WorldMapCanvas.kt          Map renderer + markers + WorldMapTestTags
      screens/AlarmScreen.kt                Saved alarms; exact-alarm notice; add action
      screens/AlarmEditorSheet.kt           Time (wheel or dial), repeat, sound, label, save, delete
      screens/AlarmRingingScreen.kt         Snooze / Dismiss surface behind the full-screen ring
      screens/ClocksScreen.kt               City list; drag reorder host; gear action opens settings; planner entry
      screens/CityDetailScreen.kt           Detail, 12h/24h toggle, label action, remove, view on map
      screens/CityLabelDialog.kt            Modal label editor overlay
      screens/CitySearchSheet.kt            Search / add
      screens/OnboardingScreen.kt           One-page first-launch intro + OnboardingTags
      screens/MeetingPlannerScreen.kt       Slider, legend and per-city rows
      screens/SettingsSheet.kt              Settings sheet + SettingsContent (four-theme picker, comparison switch, intro replay)
      screens/WorldMapScreen.kt             Map screen
      widget/DayNightColorProvider.kt       Device light/dark resolution of the app's tokens
      widget/WidgetGeometry.kt              Responsive breakpoints, row heights, widgetRowBudget
      widget/ClockwiseWidgetContent.kt     The card: header, rows, overflow, empty state
      samples/PreviewSamples.kt             Fixed instants for previews and tests
      theme/Color.kt                        ClockwisePalette, four palettes + night pair + Material You fallbacks, ClockwiseColors
      theme/Type.kt                         Inter FontFamily, Material3 type roles, ClockwiseTextStyles
      theme/Shape.kt                        ClockwiseShapes (base 16 dp)
      theme/Theme.kt                        paletteFor(theme), palette -> ColorScheme, MaterialTheme
      theme/SystemBarAppearance.kt          Keeps system bar icons legible against the palette

app/src/main/res/
  values/colors.xml                         Light window background
  values/themes.xml                         Light launch theme
  values-night/colors.xml                   Dark window background
  values-night/themes.xml                   Dark launch theme
  drawable/ic_refresh.xml                   Widget refresh glyph
  drawable/ic_alarm.xml                     Alarms tab glyph
  drawable/widget_background.xml            Translucent rounded widget card (94 % fill + hairline)
  drawable/ic_launcher_background.xml       "Concept 3" icon background: royal blue + facet
  drawable/ic_launcher_foreground.xml       "Concept 3" icon mark: dial, hands, gold accents
  drawable/ic_launcher_monochrome.xml       Themed-icon layer, single colour
  drawable-nodpi/widget_preview.png         Static picker preview (pre-API 35 fallback)
  mipmap-anydpi-v26/ic_launcher.xml         Adaptive icon (background + foreground + monochrome)
  mipmap-anydpi-v26/ic_launcher_round.xml   Round-mask variant of the same artwork
  mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher*.png  Legacy raster fallbacks, rendered from the vectors
  xml/clockwise_widget_info.xml           Widget provider info: footprint, resize floor, backstop, preview
  xml/shortcuts.xml                         Three static launcher quick actions

app/schemas/
  com.pj95.clockwise.data.local.ClockwiseDatabase/1.json   Exported Room schema v1
  com.pj95.clockwise.data.local.ClockwiseDatabase/2.json   Exported Room schema v2 (solar cache)
  com.pj95.clockwise.data.local.ClockwiseDatabase/3.json   Exported Room schema v3 (saved-city order)
  com.pj95.clockwise.data.local.ClockwiseDatabase/4.json   Exported Room schema v4 (alarms)
  com.pj95.clockwise.data.local.ClockwiseDatabase/5.json   Exported Room schema v5 (per-alarm sound)
  com.pj95.clockwise.data.local.ClockwiseDatabase/6.json   Exported Room schema v6 (countdown timer)
  com.pj95.clockwise.data.local.ClockwiseDatabase/7.json   Exported Room schema v7 (per-alarm snooze)
  com.pj95.clockwise.data.local.ClockwiseDatabase/8.json   Exported Room schema v8 (alarm sort order)
```

The schema directory is named after the database class's **fully-qualified name**, so renaming
the package renamed the directory. The v1-v3 JSON files moved with it unchanged: a Room schema
does not embed the package name, and the identity hash is computed from the schema itself, so
the 3.0.0 build validates against exactly the same v1-v3 snapshots that 2.1.0 exported. v4 and v5
are the alarm table and its sound column ([section 5.16](#516-alarms)). v7 adds the snooze
columns and v8 the stored alarm order.

**REQUIREMENT:** `ui/mapping/ClockwiseStateMapper.kt` is the single place where city data
(including map coordinates) reaches the UI. Screens never read the repository directly.

---

## 5. Functional requirements

### 5.1 City directory

**IMPLEMENTED** — 22 cities, bundled offline in `CityDirectory.kt`, each with an IANA
timezone id and map coordinates.

| # | City | id | IANA timezone |
| --- | --- | --- | --- |
| 1 | Tokyo | `tokyo` | `Asia/Tokyo` |
| 2 | Sydney | `sydney` | `Australia/Sydney` |
| 3 | Los Angeles | `los-angeles` | `America/Los_Angeles` |
| 4 | New York | `new-york` | `America/New_York` |
| 5 | London | `london` | `Europe/London` |
| 6 | Yangon | `yangon` | `Asia/Yangon` |
| 7 | Singapore | `singapore` | `Asia/Singapore` |
| 8 | Bangkok | `bangkok` | `Asia/Bangkok` |
| 9 | Mumbai | `mumbai` | `Asia/Kolkata` |
| 10 | Dubai | `dubai` | `Asia/Dubai` |
| 11 | Paris | `paris` | `Europe/Paris` |
| 12 | Berlin | `berlin` | `Europe/Berlin` |
| 13 | Cairo | `cairo` | `Africa/Cairo` |
| 14 | Johannesburg | `johannesburg` | `Africa/Johannesburg` |
| 15 | Reykjavik | `reykjavik` | `Atlantic/Reykjavik` |
| 16 | Toronto | `toronto` | `America/Toronto` |
| 17 | Vancouver | `vancouver` | `America/Vancouver` |
| 18 | Seoul | `seoul` | `Asia/Seoul` |
| 19 | Hong Kong | `hong-kong` | `Asia/Hong_Kong` |
| 20 | Auckland | `auckland` | `Pacific/Auckland` |
| 21 | Honolulu | `honolulu` | `Pacific/Honolulu` |
| 22 | Buenos Aires | `buenos-aires` | `America/Argentina/Buenos_Aires` |

**REQUIREMENT:** search matches city name, region, country label, or IANA timezone id,
case-insensitively. A blank query returns the whole directory.

### 5.2 Defaults on first launch

| Setting | Value |
| --- | --- |
| Saved cities, in order | Tokyo, Sydney, Los Angeles, New York, London |
| Active city | Los Angeles |
| Initial time format | **24-hour** |
| Initial theme | **Light Monochrome** (the user has chosen an exact look since the five-theme pass; Material You is opt-in) |
| First-launch intro | **Shown** on a fresh install, until **Get started** is tapped |
| City order storage | A fresh install seeds Room from the same default list (database v3) |

**Decision record:** the 24-hour default is read from the reference mockups. 12-hour was
never the default.

### 5.3 Clocks screen

**IMPLEMENTED**

- Eyebrow `THE WORLD, IN SYNC`, title `World Time`.
- Section label `YOUR CITIES` with a live count (`5 CITIES · 00:05`).
- One `CityCard` per saved city, in saved order, each showing name, region, offset label
  (`UTC+9`), local time, weekday + date, a sun or moon icon, and — phase 2 stage 2 — the
  **time difference against the device's own clock** (see
  [section 5.9](#59-time-difference-indicator)).
- A city with a custom label shows it in brackets after the name (`Tokyo (Head Office)`); see
  [section 5.8](#58-city-label-dialog).
- The active city's card is inverted (black on light, light on dark — see
  [section 7.1](#71-colour)).
- **Long-press drag reordering.** Holding a card lifts it (haptic `LongPress`), dragging past
  another card's midpoint swaps them live, and releasing persists the final order **once** — and
  only if it differs from the saved order. A cancelled drag restores the saved order. The list is
  keyed by city id, so the animation follows the card rather than the row index.
- **Accessibility reordering.** Every card carries **Move up** / **Move down** custom actions in
  place of a drag. The first card omits Move up, the last omits Move down, and a list of one city
  offers neither move. The active card is announced as `selected`.
- **Swipe to delete, with Undo.** A right-to-left swipe on a card removes the city immediately and
  shows a `City removed` snackbar with **UNDO** (see [section 5.16](#516-alarms) for the shared
  `SwipeToDeleteBox` and the reason the swipe is one-directional). Undo restores the city to its
  previous position **and** makes it active again if it was active before, rather than appending it
  to the end. Every card also carries a **Remove city** custom action for TalkBack.
- `ADD CITY` action opens the search sheet.
- **`PLAN A MEETING`** action opens the meeting planner
  ([section 5.10](#510-meeting-planner)).
- **Gear action** in the header opens the settings sheet.
- Caption `AUTO-ADJUSTS FOR DAYLIGHT SAVING`.
- Empty state: `No cities yet` + `Add a city to track its local time.` + `Find a city`.
- Loading state: `Loading your cities…`.

Each card is laid out as **three independent lines** — `offset | weekday`, `name | time`,
`region | difference` — rather than one two-column grid. That detail matters: with a shared
grid, a long label or a long difference line would shrink the column that holds the city
name. Per line, only the item that does not fit is ellipsized, and the city name is the last
thing to give way.

The card shows a small drag grip in its trailing edge. The grip is decorative to a screen
reader (`contentDescription = null`); the custom actions are the accessible path, and making the
grip a second focusable target would add a swipe without adding a capability.

**Decision record — Room owns the order, DataStore owns membership (2026-10-27).** A drag is a
relational operation, so the display order lives in a `saved_cities` table keyed by city id and
ordered by `position`. Proto DataStore still holds the ordered id list because it is the
membership source, the widget's fallback and the format an older build can read; every reorder
writes both in one call. Room wins when the two disagree, unknown ids are pruned, saved ids Room
does not know are appended, and an empty Room table is seeded from DataStore. Writing during the
drag would be a disk write per pixel, so the persisted order is written once on release; a drag
that ends where it started costs no write at all.

### 5.4 City detail screen

**IMPLEMENTED**

- `YOUR LOCAL TIME` label and the device's own timezone.
- **Stacked hour-over-minute clock** with weekday and date to the right — matches the
  reference mockup.
- **`COMPARED WITH YOUR LOCAL TIME`** section — phase 2 stage 2 — showing this city's
  difference against the device clock
  ([section 5.9](#59-time-difference-indicator)).
- The city's custom label, when set, on its own line under the region (`(Head Office)`), with
  an `ADD LABEL` / `EDIT LABEL` action beneath it
  ([section 5.8](#58-city-label-dialog)).
- 12h / 24h segmented control, labelled `Time format`.
- `Remove` (destructive) and `View on map`.
- Back arrow.

### 5.5 Search sheet

**IMPLEMENTED**

- Title `Add a city`, placeholder `City, country or timezone`.
- Live filtering over the 22-city directory.
- Already-saved cities are shown as added/disabled rather than hidden.
- No-results state: `No cities found` + `Try a different city, country or timezone name.`
- Adding a city makes it the active city and returns to the Clocks screen.

### 5.6 Behaviour rules

| Rule | Requirement |
| --- | --- |
| Selecting a city | It becomes active **and** its detail screen opens |
| Removing the active city | Another saved city is promoted to active |
| Removing the last city | Empty state, no crash, no dangling active id |
| Restart | Saved order, active city, format, theme and city labels are restored |
| Unknown persisted id | Dropped silently, order preserved, active id repaired |
| Unknown label id | Dropped silently (see [section 5.8](#58-city-label-dialog)) |
| Day/night icon | Sun for 06:00–17:59, moon for 18:00–05:59, **local** hour |
| Time difference reference | Always the **device's own** zone, never the active city |
| Meeting planner slot | A time of day in the chosen **source city's** zone (the device by default); one slot drives every row, and every row is read at that single instant |
| Meeting planner recommendation | The quarter-hour slot of the source day that puts the most cities inside 09:00–17:59; ties go to the cities' collective local noon, then the earlier slot. At least two cities must be at work, otherwise `NO OVERLAP` |

### 5.7 Settings sheet

**IMPLEMENTED** — `screens/SettingsSheet.kt`.

- Opened by the gear action on the Clocks screen; dismissed by the scrim, the drag handle or
  system back. The content scrolls, so a small screen with a large font can still reach every
  row.
- Eyebrow `HOW IT LOOKS`, title `Settings`, divider, section label `APPEARANCE`.
- Label `Theme`, then the four themes as full-width rows: a small preview swatch, the theme's
  name, and an inverted pill on the one in force. Each row is at least 48 dp tall and is a radio
  button in one `selectableGroup()`, so a screen reader announces e.g. `Warm Sepia, radio button,
  selected`. The swatch is decorative — the name is the label — and carries no semantics.
- The fourth row is **Material You**. On Android 12+ its swatch is the wallpaper-derived scheme;
  below Android 12 it is the static Material 3 light/dark fallback, so the row never shows a
  colour the device cannot actually produce.
- Hint `Applies to every screen straight away, and is remembered.`
- Section `COMPARED WITH YOUR LOCAL TIME`, containing one switch row labelled exactly
  **`Compared with your local time`**. On by default. The whole row is the 48 dp target and is
  `toggleable`, so the control reports on/off rather than being a decorative knob. A hint under it
  says what turning it off does: the difference line disappears from the city cards, the map's
  selected-city panel and the city detail screen. The meeting planner is unaffected — it is the
  comparison tool. Test tag `SettingsTestTags.CompareToggle`.
- Section **`ALARMS`** (4.0.0): a hint, then four status rows — **Exact alarms**,
  **Notifications**, **Allow alarms through Do Not Disturb** and **Battery optimisation**. Each
  row names what it is, why it matters, and its current state in words (`ALLOWED`, `ALLOW`, `ON`,
  `OFF`, `EXEMPT`, `CHECK`), and each opens the matching system screen. The states are read by
  `readAlarmReadiness(context)` when the sheet opens and again on every `ON_RESUME`, so a grant
  made in the system screen is reflected on return. See [section 5.19](#519-privacy-help-chronos-studio-and-feedback).
- Section **`EXTRAS`** (4.0.0): **Screen saver**, **Privacy policy**, **Help**, **Send feedback**,
  and — when a replay callback is supplied — **Show intro again**.
- Section **`CHRONOS STUDIO`** (4.0.0): one row, `Chronos Studio`, with the status `SOON`, opening
  the local placeholder screen. No billing dependency ships.
- Below the appearance section, a **Show intro again** row (`settings_replay_intro`) replays the
  first-launch page. It is a full-width ≥ 48 dp button and is absent when no replay callback is
  supplied (previews and direct-root tests).
- Changing the theme or the comparison switch writes it to DataStore and repaints **without
  recreating the activity**, so the sheet stays open while the change happens under it —
  including its own swatches.

**Decision record — why rows, not a segmented control (2026-10-18):** the three-mode control
could not carry the names. `Light Monochrome` is 16 characters, and the segmented control's
options are text plus 28 dp of padding inside a 360 dp sheet, so even four of them would either
truncate or shrink below the 48 dp target the phase-2 pass had just established. A vertical list
also has room for the preview swatch, which is what turns "four names" into "four looks". The
segmented control itself is unchanged and still used for the 12h/24h choice on the detail screen.

**Decision record — True Dark and High Contrast retired (2026-10-28):** the user asked for both to
be removed completely, not hidden. `AppTheme` therefore has four entries and the two palettes are
deleted from `Color.kt`; leaving the enum names or the palette objects behind would let a stray
screen paint a theme the picker cannot offer. Their persisted ids (`1` and `4`) are **left unused**
rather than reused, and both read as `AppTheme.Default` so an existing install lands on a real
theme instead of crashing or showing an invisible screen. The legacy three-mode `THEME_MODE_DARK`
also reads as the default, because it meant True Dark. The widget keeps a dark palette of its own
(`NightClockwisePalette`, previously `DarkClockwisePalette`), since it follows the device rather
than the app theme; that pair is a rendering input, not a theme a user can choose.

**Decision record — the comparison switch sits in Settings (2026-10-28):** the difference is an
app-wide presentation choice, so it belongs beside the theme, not on a per-city card or the detail
screen. Making it a switch rather than a per-card control means one decision, one place, and a
state the screen reader can report. The old behaviour (always compare) remains the default via an
`optional bool` proto field, so an upgrade does not silently remove a line the user relies on.

**Decision record:** the theme control was deliberately **not** placed on the city detail
screen (a per-city surface is the wrong home for an app-wide preference) and **not** given a
fourth bottom-bar tab (that would depart from the reference design's three destinations). The
user chose the gear-action/sheet option on 2026-10-17.

### 5.8 City label dialog

**IMPLEMENTED** — `screens/CityLabelDialog.kt`.

A per-city personal label / note (the requirement's `Tokyo (Head Office)`), edited from the
city detail screen.

- Opened by `ADD LABEL` (no label yet) or `EDIT LABEL` (label set) on the detail screen.
- Title `City label`, support text `Shown after the city name, for example Tokyo (Head
  Office).`, a single-line field (placeholder `Head Office`), `Cancel` and `Save`.
- `Remove label` appears only when a label is already set; it clears it.
- Save trims; an empty or all-whitespace entry **clears** the label rather than storing blanks.
- Maximum length 40 characters (`CITY_LABEL_MAX_LENGTH`); the field refuses anything longer.
- Dismissed by `Cancel`, the system back gesture, or a tap outside the card.
- Dismissing without saving leaves the stored label untouched.

**Where the label is shown:** in brackets after the city name — the Clocks card (ellipsized if
the user's label is long, never the city name), the map strip pill (uppercased with the rest of
the pill), the map selected-city panel, and the detail screen, where it also gets its own line.

**Decision record — not a platform dialog.** The dialog is a modal overlay inside the app's own
composition, not an `androidx.compose.ui.window.Dialog`. Two reasons: a platform dialog brings
its own container shape, elevation and button styling, which is exactly the Material default look
this project must not have; and a dialog window cannot be rendered or driven by the JVM
screenshot tests — a text field inside a dialog window never reaches an idle state under
Robolectric, so the dialog would have been the only untested surface in the app. The overlay is
drawn from the same palette as every other surface and is covered by both screenshot and
behaviour tests.

### 5.9 Time difference indicator

**IMPLEMENTED** — `domain/time/TimeDifference.kt`, `ui/components/TimeDifferenceLabel.kt`.

Compares a city with the **device's own** zone at the same instant and shows the gap.

| Case | Text |
| --- | --- |
| No gap | `Same time` |
| Whole hours ahead | `+2 hrs ahead` |
| Whole hours behind | `-5 hrs behind` |
| Minutes involved | `+2 hrs 30 mins ahead`, `-30 mins behind` |

- The maths is `ZoneId.rules.getOffset(instant)` for both zones, subtracted. Nothing is
  hard-coded, so half-hour zones (`UTC+5:30`, `UTC+6:30`), quarter-hour zones (`UTC+5:45`) and
  both directions of daylight saving are all exact (acceptance criteria 13a).
- Shown on every city card (bottom right), in the map's selected-city panel, and on the detail
  screen under `COMPARED WITH YOUR LOCAL TIME`.
- The whole comparison is switchable: Settings → `Compared with your local time`
  ([section 5.7](#57-settings-sheet)). Off removes the line from those three surfaces; it does not
  remove the planner's per-row offsets, because comparing times is what the planner is for. A card
  with no difference line keeps the same height and simply leaves the space blank.
- Card text is spoken in full: `Compared with your local time: +2 hrs 30 mins ahead`, because
  `+2 hrs ahead` alone does not say *ahead of what*.

**Decision record — the reference is the device, not the active city.** Comparing with the
active city was tried first and rejected: tapping a city makes it active, so the detail screen
would have read `Same time` for the city you are looking at. The device's own clock is the only
reference that is stable across all three surfaces. The user chose it on 2026-10-18.

---

### 5.10 Meeting planner

**IMPLEMENTED** — `ui/screens/MeetingPlannerScreen.kt`, `ui/components/DaySlider.kt`,
`domain/time/MeetingTime.kt`, `ui/mapping/PlannerStateMapper.kt`.

A 24-hour time-zone converter and meeting planner for arranging a call across the saved cities:
choose a time of day in one city, and every saved city's reading for that instant appears at once.

- Reached from `PLAN A MEETING`, a bordered action directly under the `YOUR CITIES` header on
  the Clocks screen. It is styled like `ADD CITY` so the two read as a pair, and it sits at the
  top because at 360 dp a full city list pushes anything at the foot of the screen out of view.
- `FROM` is a horizontally scrolling row of source chips: `LOCAL` first, then one per saved city
  in saved order. The device is selected by default, so the planner opens asking "what time
  will it be for everyone if we meet at 09:00 where I am". Choosing a city turns the same
  control into a converter: the wall-clock reading stays put, the instant moves to that city's
  zone, and every row is re-read there. The choice survives rotation and is deliberately **not**
  persisted; a source that is no longer saved falls back to `LOCAL`.
- The headline names the source — `YOUR LOCAL TIME` or `TIME IN TOKYO` — then the time at 48 sp,
  the date and the source's offset at that slot (`Sun, 21 Jan · UTC+9`). The date is the
  **source city's current civil date**, so a source across the date line keeps the day its own
  user would call today rather than inheriting the device's.
- The **day slider** is a hand-drawn 24-hour control: a track with a tick per hour, labelled
  `00 06 12 18 24`, a draggable marker, and the working day (09:00–18:00 in the source zone)
  shaded as a pill inside the track. Tap anywhere to jump there, or drag to sweep.
- The slot moves in **15-minute steps** (`MinuteOfDay.STEP_MINUTES`), from 00:00 to 23:45. The
  last slot is deliberately 23:45 rather than midnight: 24:00 is the next day, and a slider that
  can reach it would silently change the date under the user.
- One row per saved city, in saved order, each showing the name (with the user's label), the
  offset from the **source** measured at the chosen slot, the city's own weekday, and its local
  time. The weekday is there because a city across the date line is on a different day and a
  bare `18:30` would hide that.
- A city whose local time falls inside 09:00–18:00 is marked with a filled marker and a
  full-contrast time; a city outside it gets a hollow marker and a muted time. The rule is
  stated under the slider (`WITHIN 09:00–18:00` / `OUTSIDE`) rather than applied silently.
- The header of the city list carries the **recommendation**: the quarter-hour slot in the
  source day that puts the most cities inside 09:00–17:59, shown as `BEST 06:30` (`BEST 6:30 AM`
  in 12-hour form). It is a button: tapping it moves the slider and the whole plan to that slot.
  The recommendation is calculated over all 96 quarter-hour slots of the source day — never
  sampled or rounded — and ties go to the smallest sum of squared distances from each working
  city's local noon, then to the earlier slot. A slot with fewer than two cities at work is not
  an overlap, so the planner shows a non-interactive `NO OVERLAP` rather than inventing a
  recommendation; Tokyo and New York, for example, never share a working hour.
- Opens on the current time, so the planner agrees with the Clocks screen behind it, and stays
  on the clock until the slider is touched; from the first touch the slot is the user's. The
  slot and the source city both survive rotation (`rememberSaveable`) and are deliberately
  **not** persisted — they are a scratch calculation, not settings.
- With no cities saved it says `No cities to plan for` and offers `Find a city`; the slider is
  still there, still works, and the source row and recommendation are hidden because there is
  nothing to convert or compare.

**Decision record — its own screen, not a fourth tab.** The reference design's bottom bar has
three destinations, and a fourth was rejected when the theme control was placed
([section 5.7](#57-settings-sheet)). The planner also needs the height: a day ruler plus a row
per city does not fit in a sheet.

**Decision record — the slot is read in a source city, the device by default.** The planner has
two jobs: answer "what time will it be for everyone if we meet at 09:00 where I am", and convert
"09:00 in Tokyo is what time in California". Both are the same operation with a different
reference. The device is the default because it preserves the earlier behaviour and matches the
reference every city card uses ([section 5.9](#59-time-difference-indicator)); choosing a saved
city is a deliberate conversion, shown by the selected chip and the headline. Keeping the
wall-clock reading while moving the instant — rather than keeping the instant and showing 11:30
in Tokyo — is what makes the control a converter: the user is asking what 09:00 in Tokyo means
elsewhere. The source date follows the source city for the same reason: it is that city's
09:00 today, and a city across the date line must not be silently moved to the device's date.
The source, like the slot, is route-local and not persisted.

**Decision record — the working day is 09:00–18:00, start inclusive, end exclusive.** A
judgement, not a fact, so it lives as one named rule (`isWithinWorkingDay`) and is shown to the
user as a legend. Nothing is hidden by it. The rule is applied in each city's own zone at the
slot's instant, never in the source zone.

**Decision record — best overlap is an exhaustive scan with a defined tie-break.** The
recommendation has to be explainable: it maximises the number of cities inside the working day
over all 96 slots, then prefers the slot where those cities are collectively closest to local
noon (the sum of squared distances), then the earlier slot. Squared distance punishes dragging
one city far from noon more than leaving two cities a little early, and the scan handles
half-hour zones and the date line without a closed-form overlap. A single city at work is not an
overlap, so a recommendation requires at least two cities; `NO OVERLAP` is a real answer, not a
failure. The scan lives in `bestWorkingHoursSlot` as a pure function, not in the screen, so it
is testable without a composition and the answer is the same wherever it is asked.

**Decision record — the slider is hand-drawn, not Material's.** Material's `Slider` brings its
own track geometry, thumb size and tick marks — the default Material look this project does not
use — and the shaded working-day band has to line up with the marker exactly. The control keeps
the accessibility contract Material's slider implements: `progressBarRangeInfo` with a
`setProgress` action and a `stateDescription` that reads the slot as a time, so TalkBack can
adjust it without touching the screen and never announces a bare percentage.

### 5.11 Home-screen widget

**IMPLEMENTED - v2 milestone M4.** The v1.0 release shipped without a widget. The retired
Glance 1.1.1 implementation was archived under `removed-for-v1.0/` and deleted in v1.0.10; the
shipped widget is the Glance 1.2.0 rebuild described here.

The widget is a second view of the same data, never a second opinion: its rows are built by
`buildWidgetUiState`, which delegates to `buildClockwiseUiState` and then trims the list to the
widget's shape. Formats, labels, day/night icons and cross-date-line weekdays are therefore
identical to the screens.

| Aspect | Decision |
| --- | --- |
| Toolkit | Jetpack Compose Glance 1.2.0 (`GlanceAppWidget` + `GlanceAppWidgetReceiver`); the card is drawn by `ClockwiseWidgetContent` |
| Contents | Header `WORLD TIME` with the device's own time (muted) and a refresh glyph, a hairline, then one row per city: sun/moon icon, name (labels included), weekday **only when the city is on a different date** from the device, and the time in the app-wide format; then `+N more` when the list is longer than the row budget; an empty card says so instead of looking broken |
| Responsive sizes | `SizeMode.Responsive` with five breakpoints — `180×110` (2×2), `220×110`, `250×180`, `250×250` and `320×250` dp. The same card lists one city when compact and up to six when stretched; the host picks the closest size |
| Row budget | `widgetRowBudget` reads the size the launcher actually gave this copy (`LocalSize` height) and the user's font scale, then reserves the chrome, the `+N more` line when it will be drawn, and as many city rows as fit. A missing size falls back to `WIDGET_MAX_ROWS = 6`; a cramped card still shows at least one city. Overflow is counted, never dropped |
| Default footprint | 4×4 cells — `minWidth`/`minHeight` 250 dp with `targetCellWidth`/`targetCellHeight` 4, resizable both ways. `minResizeWidth`/`minResizeHeight` are **180/110 dp**, so the 2×2 bucket is reachable instead of being dead code. The 250×250 responsive size is the provider's default, so a freshly added widget shows all five default cities |
| Card | A drawable shape, not a flat colour: the app's light card token at **94% opacity** (`#F0FAFAF8`), 16 dp corners and a 1 dp outline at 20% border. Wallpaper can tint the card but cannot make a time unreadable; the corners render on every supported API rather than only where the launcher rounds them |
| Theme | The **device's** light/dark setting, not the app's stored choice: a widget sits on a home screen, not inside the app. `DayNightColorProvider` resolves each token against the device configuration, because Glance offers literal day/night colours only through a resource. The card inherits this through `drawable/` and `drawable-night/`. The choice remains open (section 15.12) |
| Tap | The header opens `MainActivity`. A city row opens **that city's detail screen** through an explicit Intent extra (`MainActivity.EXTRA_CITY_ID`); `singleTop` plus a one-shot extra keeps a tap from stacking a second task and stops a rotation replaying it. `+N more` and the empty state open the app; the refresh glyph redraws in place without opening it |
| Refresh | Screen-aware. While interactive, an `AlarmManager.setAndAllowWhileIdle` alarm on every minute boundary, self-rearming while a widget exists. While the screen is off, a **five-minute** aligned boundary through inexact `AlarmManager.set`, which Doze may batch or defer. `ACTION_SCREEN_ON` redraws immediately and restores the minute cadence. Time, time-zone and date changes also redraw immediately. `updatePeriodMillis="1800000"` remains the backstop |
| Data changes | `WidgetMirror` watches only `savedCityIds`, `cityLabels` and `use24Hour` and redraws on change; a failed redraw is logged and does not kill the mirror. It starts with the app or the first widget and stops with the last one |
| Previews | On Android 15+ the card publishes generated picker previews with `setWidgetPreviews`, once per widget-design version (`DESIGN_VERSION = 1`), retrying after a rate-limit or failure. Below API 35, and until the first successful publish, the picker uses the static `previewImage` |

**Decision record — a minute alarm, not WorkManager (2026-10-26; cadence revised 2026-10-27).**
WorkManager's minimum periodic interval is 15 minutes, which is not a clock.
`setAndAllowWhileIdle` is inexact by design: it fires on the minute while the device is awake,
and Android throttles it to roughly once every 9 minutes in deep doze. That is the honest
trade-off for a widget that shows minutes without holding a wake lock; the 30-minute
`updatePeriodMillis` is the backstop if an alarm is lost (for example across a reboot).
`setExactAndAllowWhileIdle` was rejected because it needs the `SCHEDULE_EXACT_ALARM` permission
for no visible gain. **v2.1 backs off to five minutes while the screen is off**, using plain
`set`: nobody reads a home-screen card behind a locked screen, so an extra wake-up every minute
buys nothing, and an inexact alarm can be batched with other work. The screen-on receiver
redraws at once and puts the minute cadence back, so the stale interval is never visible.

**Decision record — a translucent card, not a transparent one (2026-10-27).** A fully
transparent card would let a busy wallpaper put a bright patch behind `01:45` and make the one
thing the widget exists to show unreadable. A fully opaque card would be a pasted-on rectangle
that ignores the Material 3 surface language. The card is therefore the app's card colour at
**94% opacity**, with a hairline outline and the 16 dp radius in the drawable itself. The alpha
is high enough that contrast is never decided by the wallpaper and low enough that the card
still reads as glass.

**Decision record — five sizes, including 2×2 (2026-10-27).** The 2×2 bucket only works if the
provider declares a resize floor below its default footprint; `minResizeWidth`/`minResizeHeight`
are therefore 180/110 dp while `minWidth`/`minHeight` stay at the 4×4 default. Without the two
resize attributes the launcher would refuse to shrink the card and the smallest responsive
breakpoint would never be chosen.

**Decision record — the ticker runs only while a widget exists.** `scheduleWidgetTick` checks
`hasHomeScreenWidgets` first, and the alarm is cancelled when the last widget is removed, so a
user who never adds one is never woken by it. `ClockwiseApp.startWidgetMirror()` is idempotent
and starts the mirror from the same check; the last removal stops both.

**Decision record — a measured row budget, not a fixed six.** The retired widget always drew six
rows because `LocalSize` was not trusted below API 31, which made a 3×2 card try to draw six
cities and a stretched card stop at the same six. M4 declared the four responsive sizes and reads
the size the host actually chose; 2.1 added the fifth (`180×110`). The six-row ceiling survives
only as the design's maximum (and as the fallback when a host reports no size). The `+N more`
line is part of the budget, so a city gives up its row to it rather than being drawn over.

**Decision record — generated previews are API 35+ only.** `setWidgetPreviews` is the first API
that can render a live Glance card in the picker. Below it the static `previewImage` is the only
option, so the drawable is drawn to match the card (500×500, the light card on a neutral
background) rather than being a placeholder. The call is rate-limited by the system, so success
must be the only result that records the design version; a rejection or failure leaves the marker
alone and the next launch tries again. Publishing happens on app start, because there is no
callback that asks for a preview.

**Decision record — the widget follows the device theme.** Following the app's stored theme was
rejected: a home screen is shared with other apps, and a widget that ignores the device's
setting looks broken next to them. The consequence is documented in
[section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen).

**Re-opened by the palette themes (2026-10-18), and still open in 3.1.0.** The rejection above
assumed the app had two palettes whose light/dark split matched the device's, which made "device"
and "app" agree most of the time. With Warm Sepia and Cool Slate that coincidence is gone, and the
retirement of True Dark did not restore it, so this decision is recorded as **open** with three
options and no answer yet — see
[section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme). The behaviour is
unchanged until that is decided, and the Settings hint now says so on screen.

**Widget-specific accessibility trade-off.** A city row is 28 dp tall (the header 32 dp and the
refresh target 24 dp), below the 48 dp guideline the app's own surfaces meet. A 4×4 home-screen
card cannot give six rows 48 dp each, so the rows are full-width targets with no gaps between
name and time, which is what keeps them hittable; the refresh glyph is the one small secondary
target. This is documented in [section 9](#9-accessibility-requirements) and remains a
hardware-verification item ([section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen)).

### 5.12 Phase 2 integration, accessibility and polish

**IMPLEMENTED** — the pass that connects the phase 2 features to each other and finishes their
accessibility, with no change to what any of them does.

| Item | Decision |
| --- | --- |
| Reachability | Every phase 2 feature is reachable from the Clocks screen — the theme switch behind the gear, the planner from `PLAN A MEETING`, labels from a city's detail screen, and the widget from the launcher. Nothing is hidden behind a long-press or a gesture |
| Touch targets | Every control added in phase 2 is at least 48 dp tall. `CircleIconButton` keeps its drawn circle (44 dp at the small call sites, 48 dp by default) and puts the click, the label and the semantics on a box around it that is never smaller than 48 dp (`MinTouchTarget`); the segmented control's options, the four theme rows, the comparison switch row, the wheel and sound rows and the bordered/text action rows (`PLAN A MEETING`, `ADD CITY`, `Find a city`, `EDIT LABEL`) are clamped with `heightIn(min = MinTouchTarget)` |
| Headings | Screen titles and section headers carry the `heading()` semantics — `World Time` / `YOUR CITIES`, `Meeting planner` / `YOUR LOCAL TIME` or `TIME IN <CITY>` / `YOUR CITIES AT THIS TIME`, `Settings` / `APPEARANCE`, `City label` and the detail screen's two blocks — so TalkBack can jump between sections instead of swiping through them |
| The label dialog | Announced as a pane (`paneTitle`), because it is drawn inside the app's own composition rather than in a platform dialog window; its field carries `a11y_city_label_field` (`Label for %1$s`) so the edit box names the city it edits |
| The difference on a card | Already announced as `Compared with your local time: %1$s` in phase 2 stage 2; asserted for the first time in criterion [17c](#11-acceptance-criteria) |

**What this pass deliberately did not change:** no colour, type, spacing or wording changes; the
only visual differences are the taller segmented control and the 48 dp slots around the small
circular buttons, both visible in the re-recorded screenshots.

### 5.13 Sunrise and sunset engine

**IMPLEMENTED (v2 M1)** — `domain/solar/SolarEphemeris.kt`, `domain/solar/SolarEvents.kt`,
`domain/solar/SunriseSunsetCalculator.kt`, `data/repository/SolarRepository.kt`,
`data/local/entity/SolarDayEntity.kt`, `data/local/dao/SolarDao.kt`.

| Aspect | Decision |
| --- | --- |
| Model | `SolarEphemeris` computes the Sun's declination and the equation of time from an instant using the NOAA first-order equations. `SunriseSunsetCalculator` turns a **civil date**, a `GeoPoint` and an IANA `ZoneId` into `SolarEvents` |
| Rise/set definition | The Sun's centre is 0.833° below the horizon — the usual apparent sunrise/sunset, including refraction and the solar disc's radius. Astronomical twilight is a different product and is not claimed |
| Result | `SolarEvents` carries the local date, zone, `NORMAL` / `POLAR_DAY` / `POLAR_NIGHT` kind, sunrise and sunset instants (both `null` on a polar day/night), solar noon, and day length |
| Date anchoring | Solar noon is calculated from the UTC minutes-of-day formula but anchored to **local noon** in the requested zone. This keeps the result on the requested civil date across the date line, where the UTC date of local noon can differ from the UTC date of solar noon |
| DST and zones | Events are absolute `Instant`s; `ZonedDateTime` is used only to render the local wall clock, so a DST transition cannot shift a sunrise by an hour |
| Polar behaviour | A cosine outside `[-1, 1]` is an explicit polar day or night rather than a clamped fake event. At the poles the constant solar elevation is used instead of dividing by `cos(latitude)` |
| Cache | `SolarRepository` caches one row per city and local date in `solar_days`. A cached row whose latitude/longitude no longer matches the city is recomputed; a cache read or write failure is best-effort and never blocks the calculated result |
| Database | Room v2 adds `solar_days`, keyed by `(cityId, dateEpochDay)` with an index on the date. Migration 1 -> 2 creates the table and leaves `cities` untouched; destructive fallback remains disabled |
| Surfaced | M2 shows the selected city's solar line on the map. The detail card and widgets do not show it yet |

**Verification:** fixture tests pin sunrise, sunset and solar noon to published values for New
York, London, Tokyo, Sydney and Singapore, plus Tromsø's polar day and polar night. Further
tests cover leap day, the US spring-forward day, sunrise-before-noon-before-sunset invariants,
the February and November equation-of-time extrema, cache hits, coordinate invalidation,
DAO replacement/pruning, and the 1 -> 2 migration.

### 5.14 Launcher shortcuts

**IMPLEMENTED (v2.1)** — `shortcuts/CityShortcuts.kt`, `shortcuts/ShortcutMirror.kt`,
`res/xml/shortcuts.xml`, `MainActivity`.

Long-pressing the app icon shows the actions worth reaching without opening the app.

| Aspect | Decision |
| --- | --- |
| Static actions | Three, always present: `Add city` (`add-city`), `Plan` (`planner`), `Map` (`map`). Declared in `res/xml/shortcuts.xml` and referenced from the activity's `android.app.shortcuts` metadata |
| Dynamic cities | The first **three** saved cities, in saved order, with the user's label when set. Published as `city-<cityId>`, ranked 0..n, and replaced wholesale on every change |
| Budget | The launcher counts static and dynamic shortcuts together. `dynamicShortcutLimit(maxTotal)` subtracts the three static actions and clamps to `0..3`: API 26–29 launchers (maximum five) get **two** city shortcuts, modern launchers get three, and a launcher reporting no room gets none rather than an exception |
| Deep link | `cityIntent` starts `MainActivity` with `EXTRA_CITY_ID` and `EXTRA_SHORTCUT_ID`, `ACTION_VIEW`, `NEW_TASK | SINGLE_TOP`. It is the same city route as a widget row; the extra shortcut id is what distinguishes the two |
| Usage | `MainActivity` consumes `EXTRA_SHORTCUT_ID` once and calls `ShortcutManagerCompat.reportShortcutUsed` inside `runCatching`, so a launcher that rejects the report cannot block the tap. Widget rows do not set the id and are therefore not counted |
| Updates | `ShortcutMirror` watches only `savedCityIds` and `cityLabels`, so a theme, format or active-city change republishes nothing; a failed publication is reported and the mirror keeps running. It starts with the application |
| Unknown ids | A city no longer in the directory never becomes a shortcut, and a label is trimmed/ignored when blank — so a catalog change cannot leave a dead end |

**Decision record — a shortlist, not a second city list (2026-10-27).** A long-press menu is
for the two or three things a person does most; publishing every saved city would turn it into a
worse version of the Clocks screen. The first three in saved order also mean a reorder changes
the shortlist, which is what a user who arranged the list expects.

**Decision record — no shortcut for the widget or theme (2026-10-27).** A widget is added by the
launcher's own picker, and the theme sheet is not a destination. Neither could be a shortcut
without inventing an entry path the app does not otherwise have.

### 5.15 First-launch intro

**IMPLEMENTED (v2.1)** — `ui/screens/OnboardingScreen.kt`, `MainActivity`,
`ClockRepository.completeOnboarding()` / `restartOnboarding()`.

| Aspect | Decision |
| --- | --- |
| Gate | `MainActivity` shows the intro only when `!state.isLoading && !state.onboardingCompleted`; `ClockwiseRoot` is not involved, so direct-root tests and previews are unchanged |
| Content | Eyebrow `WELCOME`, title `Every clock, at a glance.`, support line, three feature rows (`Clocks that stay right`, `Plan across time zones`, `Private by design`) and one primary action, `Get started` |
| Layout | One scrollable page with navigation-bar padding, so the action stays reachable on a short screen and at a large font scale. No pager, no skip link, no account prompt |
| Dismissal | `Get started` calls `completeOnboarding()` and the normal shell composes; the action is ≥ 48 dp and the page is tagged `OnboardingTags.SCREEN` for tests |
| Replay | `Show intro again` in Settings calls `restartOnboarding()`, which flips the stored field back to `false`; the intro therefore appears on the next composition without a restart |
| Upgrade rule | The proto field is `optional bool onboarding_completed = 8`. A fresh install seeds an explicit `false`; a store written before the field existed has no presence bit and reads as **completed**. That is what keeps upgraders out of an intro they have already seen, while a genuinely new install still gets it |
| Read failure | `UserPreferences.Default` has `onboardingCompleted = true`, so a DataStore read failure can never trap the user behind the intro |

**Decision record — a gate in the activity, not the shell (2026-10-27).** Putting the check in
`ClockwiseRoot` would have made every existing direct-root test render an intro it never asked
for. The activity already owns the theme and the deep-link extras, so it is the one place that
can decide what the first composition is without changing the shell's contract.

### 5.16 Alarms

**IMPLEMENTED (3.1.0, gestures 4.1.0)** — `screens/AlarmScreen.kt`, `screens/AlarmEditorSheet.kt`,
`screens/AlarmRingingScreen.kt`, `components/WheelTimePicker.kt`, `components/SwipeToDeleteBox.kt`,
`ui/AlarmViewModel.kt`, `domain/alarm/*`, `alarm/*`, `data/local/{entity,dao}/*Alarm*`,
`repository/AlarmRepository.kt`.

Alarms are the third bottom-bar destination. The bar is now `Alarms · Clocks · Map`; search used
to hold the left slot as an action and moved back to the `+` actions on the Clocks and Map screens,
where the cities it searches for actually live. The list follows the layout the Clocks screen
established: eyebrow `ALARMS`, title `Alarms`, a section label with a count, then one card per
alarm. The card's switch is the only control that writes without opening the editor; tapping the
card opens it.

| Aspect | Decision |
| --- | --- |
| Stored fields | `hour`, `minute`, `label` (≤ 40 chars), `enabled`, `repeatDays` (bit mask, Monday = bit 0), `soundUri` (`""` = device default), `snoozePolicy` (`SnoozePolicy` ordinal), `snoozesUsed`, `snoozedAtEpochMillis`, `sortOrder` |
| Repeat mask | `RepeatDays`: `NONE` is a **one-shot** alarm that rings at the next occurrence and switches itself off; `WEEKDAYS`, `WEEKEND` and `EVERY_DAY` are the common presets, and the seven chips set any combination |
| Next trigger | `Alarm.nextTrigger(now)` is pure Kotlin and builds candidates with `ZonedDateTime.of`, so an alarm set for 07:30 reads 07:30 on the wall clock across a DST transition rather than "24 hours later". A one-shot alarm looks at today and tomorrow; a repeating one scans eight days, which always contains its weekday |
| Arming | `AndroidAlarmScheduler` uses `AlarmManager.setAlarmClock`, the only API that both honours the trigger in Doze and shows the system's next-alarm icon. Where exact alarms are refused it falls back to `setAndAllowWhileIdle`, because a few minutes late beats an alarm that never rings |
| Exact alarms | `USE_EXACT_ALARM` is declared (an alarm clock is the intended use). `canScheduleExact()` reflects the platform; when it is false the list shows an inline notice with an action to the system screen, rather than failing silently |
| Reschedule | `AlarmRescheduleReceiver` re-arms every stored alarm after boot, a time change or a time-zone change; all four broadcasts are protected, so the receiver is not exported |
| Ringing | `AlarmReceiver` calls `onFired`, rolls the occurrence forward (or disables a one-shot), then starts `AlarmRingingService` with `ContextCompat.startForegroundService`. The service owns a looping `MediaPlayer` on `USAGE_ALARM`, a wake lock and a repeating vibration, and shows the silent `alarm-ringing` notification that carries the full-screen intent into `AlarmRingingActivity`. The audio stops after ten minutes; the screen stays up |
| Ringing notification | Silent, `IMPORTANCE_HIGH`, `CATEGORY_ALARM`, `FOREGROUND_SERVICE_IMMEDIATE`, public on the lock screen, ongoing, with `Snooze` (omitted when the policy refuses it) and `Dismiss` |
| Full-screen intent | `USE_FULL_SCREEN_INTENT` plus `setShowWhenLocked`/`setTurnScreenOn`; `AlarmReadiness.fullScreenIntentAllowed` reads Android 14's `canUseFullScreenIntent()` and Settings offers the matching system screen |
| Snooze | `SnoozePolicy.ALLOWED` snoozes without limit, `ONCE` allows one per ring cycle, `NONE` offers Dismiss only. `SnoozeRules.canSnooze` is checked by the screen, the notification and `RoomAlarmRepository.snooze`, which returns false rather than arming a refused snooze |
| Snooze cycle | `snoozesUsed` resets on a natural fire and on Dismiss; a snooze re-fire clears only `snoozedAtEpochMillis`, so "once" means once per morning. `AlarmReceiver` re-reads the row after `onFired` so the notification reflects the count this ring uses |
| Notification permission | On Android 13+ posting is guarded by `POST_NOTIFICATIONS`; when it is refused the foreground service cannot post its notification, so the ring is surfaced as an in-app diagnostic rather than pretending it can ring |
| Persistence | Room v4 adds `alarms`; `MIGRATION_3_4` creates it and leaves the clocks, solar cache and saved-city order untouched. v5 adds `soundUri` with a default of `""` (`MIGRATION_4_5`). v6 adds the `timer` table (`MIGRATION_5_6`). v7 adds `snoozePolicy`, `snoozesUsed` and `snoozedAtEpochMillis`, defaulting to unlimited snoozing (`MIGRATION_6_7`). v8 adds `sortOrder` and backfills it by time of day (`MIGRATION_7_8`) |
| Ordering | `AlarmDao` orders by `sortOrder, hour, minute, id`, so the user's arrangement wins and time is only the stable tie-break. `save` appends a new alarm at `maxSortOrder + 1`; `reorder` rewrites every position in one transaction and never touches `AlarmManager` |
| Reorder gesture | `AlarmScreen` mirrors the city list: long-press lifts the card with a `LongPress` haptic, a vertical drag swaps cards live, and the order is persisted once on release and only if it changed. A cancelled drag restores the saved order |
| Swipe to delete | `SwipeToDeleteBox` wraps each alarm and city card in a `SwipeToDismissBox` with **only** `EndToStart` enabled, because the same card owns a long-press vertical drag and two delete directions would make the first movement ambiguous. Deletion is immediate and Undo follows in the shared shell snackbar (`SnackbarDuration.Long`) |
| Undo | `ClockwiseRoot` dismisses any current snackbar and offers **UNDO**. An alarm is restored with its original id, position, switch, sound and snooze state and re-armed if it was enabled; a city returns to its previous order and active state via `ClockRepository.restoreCity` |
| Accessibility | Every card carries **Move up** / **Move down** / delete custom actions, so reorder and removal work without a drag or swipe; the first card omits Move up and the last omits Move down |
| Repository | `RoomAlarmRepository.save` writes the row and then arms or cancels it, so "stored" and "scheduled" cannot disagree; an update that matches no row arms nothing, and editing an alarm resets its snooze cycle |
| Test tags | `AlarmTestTags` on the list, `AlarmEditorTestTags` on the sound and snooze sections, `AlarmTimeTestTags` on both time pickers |

**The time input is a wheel, not a keypad (2026-10-28).** The editor's `TIME` section is a custom
`WheelTimePicker`: a `LazyColumn` of hour and minute values with `rememberSnapFlingBehavior`, the
selection marked by a fixed centre line, the value nearest the line held in state. In 12-hour mode
an `AM` / `PM` segmented control sits beside the wheels. Every value is a valid one, scrolling is
the natural gesture for a list of numbers, and there is no keyboard to dismiss. Under **Material
You** the section renders the platform's `TimePicker` (clock dial) instead — that theme advertises
itself as the system-native look, and the dial is what a user of it expects; it is wrapped in the
app's palette (`TimePickerDefaults.colors`) so it does not import Material's default purple. The
direct numeric entry is gone entirely, under every theme.

**The sound is chosen by name and auditioned in place (2026-10-28).** `SOUND` shows one row with
the current sound's name and a chevron. It opens a short, scrollable list — not a spinner:
each row has its own `Preview` / `Stop` action, so a tone can be heard before it is committed,
which a dropdown cannot offer while it is open. Selecting a row closes the list; leaving the sheet
stops any preview. The first option is always `Default alarm sound` with an empty URI, so the
picker is never blank even on an OEM ROM whose alarm cursor is empty. A stored URI that can no
longer be played falls back to the device's default at ring time rather than ringing silence.

**Decision record — the service is the ring, the channel is silent (2026-10-29).** Android fixes a
channel's sound when the channel is created and the app cannot read back whether the user has muted
it. A phone that has to wake someone up cannot depend on that, so v4.1.0 moved the audio into
`AlarmRingingService` on `USAGE_ALARM`. The ringing channel is a new id (`alarm-ringing`) with no
sound of its own, and every 4.0.0 `alarms*` channel — including the per-sound ones — is deleted on
upgrade, because leaving them would show the user settings that no longer control anything.

**Decision record — snooze is a property of the alarm, not a global setting (2026-10-29).** A
wake-up alarm and a medication reminder deserve different answers, and only the user knows which is
which. `SnoozePolicy` is stored per alarm; a label containing a whole critical word (`medication`,
`pills`, `insulin`, `dose`, `important`, …) only *suggests* `NONE`. The same rule is read by the
ringing screen, the notification builder and the repository, so a stale notification action cannot
postpone an alarm the user made critical.

**Decision record — AM/PM is a span, not a second `Text` (2026-10-29).** Two `Text`s in a `Row`
would look right and read wrong: TalkBack would announce `6:30` and `PM` separately, and the period
would sit on a different baseline. `AlarmTimeDisplay` builds one `AnnotatedString` with the period as
a `SpanStyle`, so the card, the editor preview and the ringing screen share one renderer, one
announcement and one baseline.

**Decision record — the gesture is delete-then-Undo, and only one direction (2026-10-31).** A
swipe is already a decision, so a confirmation dialog would make the fast path slower than opening
the editor. The card is removed on the swipe and the snackbar carries the escape hatch: `UNDO`
restores the exact row rather than a lookalike, because the alarm id is what the editor, the
scheduler and any pending notification already refer to. Swipe is **right-to-left only** — the
same card owns a long-press vertical drag for reordering, and accepting a start-to-end swipe as well
would make the first movement of either gesture ambiguous. The drag and the swipe are also both
available as custom accessibility actions, so neither capability depends on a precise touch.

**Decision record — new alarms append, and the migration preserves the visible list (2026-10-31).**
Once the user can arrange the list, only the user decides where the next alarm belongs, so `save`
writes `maxSortOrder + 1` instead of slotting by time. Existing installs see no change: `MIGRATION_7_8`
assigns `0, 1, 2 …` in the time-of-day order the 4.0.0 list already used. Reordering writes only
`sortOrder`; no alarm is re-armed by a move, because moving a card never changes when it rings.

### 5.17 Countdown timer

**IMPLEMENTED (4.0.0)** — `domain/timer/*`, `alarm/{AndroidTimerScheduler,TimerReceiver,
TimerNotifications,AndroidTimerAlerting}`, `repository/TimerRepository.kt`, `ui/TimerViewModel.kt`,
`ui/screens/TimerPanel.kt`, `ui/components/TimerDurationPicker.kt`, `ui/screens/TimerRingingScreen.kt`,
`alarm/TimerRingingActivity.kt`, Room v6 `timer`.

The timer is a section of the Alarms tab, opened by the hourglass in the header (v4.1.0; it was an
`ALARMS · TIMER` segmented control in 4.0.0). The close button or system Back returns to the alarm
list, and a timer notification deep-links straight to the countdown. The bottom bar still has three
destinations: a timer answers the same question an alarm does — "what will interrupt me next?" — so
it shares the tab rather than taking a fourth.

| Aspect | Decision |
| --- | --- |
| Stored shape | One row, fixed id `1`: `durationMillis`, `remainingMillis`, `endAtEpochMillis`, `running`, `label` |
| Running timer | Derived from `endAtEpochMillis` (`remainingAt(now)`), never decremented. A missed tick delays a redraw; it cannot make the countdown wrong |
| Paused timer | `running = 0`, `endAtEpochMillis = NULL`, `remainingMillis` frozen. Pause and resume are therefore lossless across a process death |
| Duration | `00:00:00`–`23:59:59` on three wheels; clamped to `MIN_DURATION_MILLIS` (1 s) … `MAX_DURATION_MILLIS` (99:59:59) at the repository, so a wheel that lands on zero cannot start a finished timer |
| Arming | `AndroidTimerScheduler` uses `setExactAndAllowWhileIdle` (not `setAlarmClock`: a timer must not claim the status-bar alarm icon). Where exact alarms are refused it falls back to `setAndAllowWhileIdle` |
| Running notification | `IMPORTANCE_LOW`, silent, ongoing, `setUsesChronometer(true)` + `setChronometerCountDown(true)`. The system ticks it, so no per-second foreground service and no wake lock |
| Ringing | `TimerNotifications.ALERT_CHANNEL_ID` (`IMPORTANCE_HIGH`, alarm usage, `setBypassDnd(true)`), `CATEGORY_ALARM`, full-screen intent into `TimerRingingActivity` |
| Restore | `rescheduleRunning()` runs at process start and after boot/time change. A stored end instant already in the past rings **now** rather than being dropped |
| Finish | `finish()` writes `remainingMillis = 0`, disarms, removes the countdown and posts the ring. The row is kept so "Start again" is one tap |
| Reset | `reset()` clears the row and both notifications |
| Test tags | `TimerTestTags` (wheels, Start/Pause/Resume/Cancel/Reset, display, presets) |

**Decision record — one timer, not a list (2026-10-28).** Android's own clock allows several
timers; this app keeps one. A list needs per-timer notifications, a merge rule for the shade, and
a way to name them, and none of that is in the Phase 1 brief. One row with a fixed primary key
also makes "replace whatever was running" the natural write, so two timers can never race.

**Decision record — the trigger is re-checked, not trusted (2026-10-28).** `TimerReceiver` reads
the stored row before ringing. A wake-up that arrives early re-arms itself; one that arrives after
the user paused or reset finds no running row and only clears the notification. This is what keeps
a cancelled timer from ringing at the old moment.

### 5.18 Screen saver

**IMPLEMENTED (4.0.0)** — `screensaver/ScreenSaverActivity.kt`, `ui/screens/ScreenSaverScreen.kt`,
opened from Settings → `EXTRAS` → **Screen saver**.

- A **separate, non-exported activity**, not a dialog and not an Android Daydream: it is launched
  only by the app, hides every system bar with `WindowInsetsControllerCompat`, and holds
  `FLAG_KEEP_SCREEN_ON` while visible.
- Pure black background, the time in 88 sp tabular Inter, the full localized date, and a dim
  `TAP ANYWHERE TO EXIT` hint. The whole surface is the exit target; Back also finishes.
- The block of type drifts between five offsets (±10 dp horizontally, ±8 dp vertically) once a
  minute, so no glyph holds one pixel long enough to ghost. The travel is small enough to read as
  still.
- Time format follows the app's 12h/24h setting; the theme does **not** apply — a screen saver that
  followed a paper-white theme would not save anything.

**Decision record — `FLAG_KEEP_SCREEN_ON`, not a wake lock (2026-10-28).** The flag is scoped to
the visible window and is released by the platform the moment the activity stops, so a forgotten
screen saver cannot hold the CPU awake or drain the battery in the background.

### 5.19 Privacy, Help, Chronos Studio and feedback

**IMPLEMENTED (4.0.0)** — `ui/screens/InfoScreen.kt`, `ui/Feedback.kt`, `Routes.PRIVACY/HELP/STUDIO`.

All three information screens share one shape — back action, eyebrow, title, then titled
paragraphs — and all three are **local Compose content**. There is no WebView, no hosted policy and
no asset fetched at runtime: the app has no network permission to fetch one with, and text that
ships in the APK cannot change after the user has read it.

| Screen | States |
| --- | --- |
| **Privacy policy** | Nothing leaves your phone (no internet permission, no account, no sync); what is stored (cities, labels, alarms, the timer, preferences, the intro gate — all in app-private storage); no ads, trackers, analytics, crash reporter or billing library; feedback is the user's own mail app and nothing is sent until they press send; the user's control (remove anything, clear app data) |
| **Help** | Alarms and timers; exact alarms (granted automatically on 13+, a setting on 12/12L); notifications and the lock screen; Do Not Disturb (policy access, and that the app never changes the user's DND schedule); battery optimisation (and that the app does not prompt for the exemption automatically); the screen saver |
| **Chronos Studio** | A widget gallery as a **placeholder**; one-time purchase, no ads and no subscription when it arrives; the app stays offline except for that purchase; a footnote that nothing is charged and no purchase can be made from this screen |

**Send feedback** builds a `mailto:` intent with a fixed subject and a diagnostic block — app
version name and code, Android release and API, `MANUFACTURER MODEL`, locale, time zone, and the
four alarm grant states — then hands it to whichever mail app the user already has. If no app
handles it, the same text is copied to the clipboard and a snackbar says so. The diagnostics
contain nothing that identifies the user, and the user can edit every line before sending.

**Decision record — the settings groups (2026-10-28).** The sheet now reads in the order a problem
is met: how it looks, how it compares time, **whether alarms are allowed to ring**, the extras, and
the Studio placeholder. The four alarm rows are status rows — `ALLOWED` / `ALLOW` / `ON` / `OFF` /
`EXEMPT` / `CHECK` — rather than switches, because each one is a trip to a system screen the app
cannot change on its own; the status is read from the platform on `ON_RESUME`, so returning from a
system screen repaints the row that was just granted.

## 6. World map specification

### 6.1 Rendering

**IMPLEMENTED** — `WorldMapCanvas.kt`, drawn entirely with Compose `Canvas`.

Draw order: ocean → graticule → landmasses → night shading → sun marker → active meridian →
UTC ruler, with city markers as real composables on top.

| Layer | Detail |
| --- | --- |
| Ocean | `ClockwiseColors.Card` fill |
| Graticule | 1 px `ClockwiseColors.Border` lines every **30°** |
| Landmasses | `ClockwiseColors.MapLandmass` fills from 26 hand-authored outlines. It is `accent` in the three fixed themes; Material You takes it from the wallpaper's `surfaceContainerHighest`. In every case it is distinct from the ocean, so the continents never vanish |
| Night shading | `ClockwiseColors.MapNight` — the ink colour at **18 %** in the light themes, translucent black at **18 %** in a light wallpaper scheme and **45 %** in a dark one (a fixed alpha would be invisible on one map and mud on the other) |
| Sun marker | Rayed disc at the subsolar point from `SolarPosition`, drawn with `MapAccent` on a card-coloured halo so it reads over both ocean and land. It moves with the terminator during time travel, making the scrub legible |
| Active meridian | `ClockwiseColors.MapAccent` (`#BF6B6B`), 1.5 px vertical line at the active city's longitude |
| UTC ruler | Hour ticks from −12 to +14, label every 2 h, measured with `rememberTextMeasurer`/`drawText`; labels that would collide with the border are skipped |

**REQUIREMENT:** no map tiles, no map provider, no API key, no image assets.

### 6.2 Projection

**IMPLEMENTED** — equirectangular.

```text
x = (longitude + 180) / 360
y = (90 - latitude) / 180
```

Chosen so that meridians and parallels are straight lines and the terminator is cheap to
evaluate per longitude.

### 6.3 Day/night terminator

**IMPLEMENTED** — analytic, per longitude, from the standard first-order NOAA solar
approximation (`SolarPosition.subsolarPoint`), then:

```text
φ = atan( -cos(Δλ) · cos(δ) / sin(δ) )
```

Two deliberate implementation notes, both recorded because they were bugs first:

1. **`atan`, not `atan2`.** `atan2` selects the antipodal branch and returns latitudes
   outside ±90°.
2. **Declination is clamped to a minimum of 0.5°.** At the equinoxes the Sun sits over the
   equator and the equation degenerates; the clamp keeps the curve continuous and
   unambiguous all year round.

Greenwich mean sidereal time is computed in **hours per day** and must be **multiplied by
15** to become degrees before use. (This was also a bug first.)

Traced at 180 steps (one per 2° of longitude), then closed through whichever pole is in
shadow.

### 6.4 Markers

**IMPLEMENTED**

- Markers are **composables, not canvas shapes**, so each gets a **44 dp** touch target, a
  ripple, and a screen-reader label.
- Saved city: hollow circle with a solid centre dot.
- Active city: `#BF6B6B` diamond rotated 45°, outlined in the card colour.
- Label: `"<City>, <time>, <offset>"`, plus `", selected city"` when active.
- Tapping a marker makes that city active.
- Test tags live in `internal object WorldMapTestTags` in `WorldMapCanvas.kt`.

### 6.5 Map screen layout

**IMPLEMENTED** — `WorldMapScreen.kt`, vertically centred with `Arrangement.Center`:

1. City strip (pills, auto-scrolls to the active city) with `+` at the top right
2. Selected-city clock panel (tappable → detail screen), including the city's solar line
3. Map canvas at `aspectRatio(1.7f)`
4. Time-travel control: `LIVE` / `TIME TRAVEL`, the device's time and date, and the day slider
5. Legend: `SELECTED` / `SAVED CITY` / `IN NIGHT`
6. Caption: `TAP A MARKER TO SELECT · SHADED AREA IS NIGHT`

Empty state replaces the strip with a titled header plus the `+` button, and shows
`No cities on the map` + `Add a city to see where it sits on the globe.` + `Find a city`.

### 6.6 Time travel and solar display

**IMPLEMENTED** — `ui/MapUiState.kt`, `ui/mapping/MapStateMapper.kt`,
`ClockwiseRoot.kt`, `screens/WorldMapScreen.kt`, `components/DaySlider.kt`.

| Rule | Detail |
| --- | --- |
| Scope | The slider scrubs the **device's current civil day** on the planner's 15-minute grid. It does not cross into another day; `NOW` returns to live time |
| State | `travelMinute: MinuteOfDay?` lives on the map route and is `rememberSaveable`; `null` means live. Rotation, tab switches and the back stack do not silently return the map to live time |
| Single source | `buildMapUiState` is pure and builds the map's own city rows at the chosen instant. The screen never calculates times, offsets, dates or solar events |
| City clocks | Markers, strip and selected-city panel all read the travelled instant, so they agree with the terminator and the Sun |
| Solar date | The active city's civil date at the travelled instant, not the device's date. At 03:00 in `Asia/Yangon` the Los Angeles solar line is for the 19th, while the device's date is the 20th |
| Solar loading | `ClockwiseViewModel.solarEvents` reads `SolarRepository`, so the map shares the Room v2 cache. A failed lookup omits the line rather than failing the screen |
| Stale results | A summary is accepted only when both `cityId` and `date` match the current map state; the route also hides it while a new lookup is in flight, so a slow load cannot paint yesterday's sunrise over today's map |
| Solar line | `SUNRISE <time> · SUNSET <time>` on a normal day, or `MIDNIGHT SUN · NO SUNSET` / `POLAR NIGHT · NO SUNRISE`. Times follow the app-wide 12h/24h setting |
| Live vs travel | The mode is written out (`LIVE` / `TIME TRAVEL`) rather than carried by colour alone; the live thumb sits at the exact current minute, and only a drag snaps it to the grid |
| Accessibility | The slider exposes its range and a set-progress action and speaks the chosen time; the solar line is one merged node with a full sentence; `NOW` has a click label |

---

## 7. Design system

### 7.1 Colour

**IMPLEMENTED** — `ClockwisePalette` (`theme/Color.kt`). The app ships **four** themes: the three
hand-built monochrome palettes below, plus the opt-in **Material You** palette added in 2.1 and
still one of the four. For the three fixed themes, these tokens are the *only* colours the app
uses and nothing falls back to a Material default, so the Material 3 purple/blue accent cannot
appear. Material You is the deliberate exception: it maps the platform's wallpaper-derived scheme
onto the same `ClockwisePalette` tokens (§7.1.1), so every call site still reads one palette type
and no screen knows which theme produced it.

| Token | Light Monochrome | Warm Sepia | Cool Slate | Material You (static fallback) | Use |
| --- | --- | --- | --- | --- | --- |
| `background` | `#F3F3F1` | `#F7F4EF` | `#EEF2F5` | `#FEF7FF` / dark `#141218` | App main surface, sheet surface |
| `foreground` | `#111111` | `#2C2521` | `#1E293B` | `#1D1B20` / `#E6E0E9` | Main text, primary actions |
| `card` | `#FAFAF8` | `#FCFAF6` | `#F8FAFC` | `#F7F2FA` / `#211F26` | City cards, elevated surfaces, map water |
| `primary` | `#111111` | `#2C2521` | `#1E293B` | `#6750A4` / `#D0BCFF` | Selected card, active controls |
| `primaryForeground` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` / `#381E72` | Text and icons on `primary` |
| `onPrimaryMuted` | `#9EFFFFFF` | `#9EFFFFFF` | `#9EFFFFFF` | `onPrimary` @ 72 % | Supporting text on the selected card |
| `secondary` | `#E8E8E6` | `#EFEAE2` | `#E4EAEF` | `#ECE6F0` / `#2B2930` | Secondary / raised surfaces |
| `mutedForeground` | `#777774` | `#7A7065` | `#64748B` | `#49454F` / `#CAC4D0` | Supporting text, offsets, labels |
| `accent` | `#E3E3E0` | `#EDE7DD` | `#DDE5EB` | `#E8DEF8` / `#4A4458` | Soft selected surfaces, theme-picker container |
| `border` | `#D9D9D6` | `#E2DACE` | `#CFD9E1` | `#79747E` / `#938F99` | Outlines, separators, graticule |
| `mapAccent` | `#BF6B6B` | `#BF6B6B` | `#BF6B6B` | `#7D5260` / `#EFB8C8` | Map meridian, active pin, destructive action |
| `mapLandmass` | `#E3E3E0` | `#EDE7DD` | `#DDE5EB` | `#E7E0EC` / `#36343B` | Continents on the schematic map |
| `mapNight` | `#111111` @ 18 % | `#2C2521` @ 18 % | `#1E293B` @ 18 % | black @ 18 % light / 45 % dark | Wash over the half of the globe in shadow |
| `isDark` | `false` | `false` | `false` | from the scheme | Drives `darkColorScheme()` and the system bar icons |

**Theme ids.** `AppTheme` (`data/prefs/AppTheme.kt`) numbers the four: Light Monochrome `0`,
Warm Sepia `2`, Cool Slate `3`, Material You `5`. The ids are **left where they were**, so no
existing install repaints on upgrade; `1` (True Dark) and `4` (High Contrast) are retired and
never handed to a new theme. The id is persisted, so the numbering is part of the stored format —
tests pin both the ids and the mapping, and assert that the two retired ids read as the default.

**The three monochrome themes are complete palettes, not a light/dark switch.** There is no
"follow the device" option among them: the user has chosen an exact look, and repainting it
because the phone switched at sunset would throw that choice away. This replaces the phase-2
`System / Light / Dark` mode (user decision, 2026-10-18) and is asserted — the same theme paints
the same palette on a device in dark mode as on one in light mode. Material You is the one
theme that does read the device, because that is what the user asked for by choosing it; it
still resolves once into a `ClockwisePalette` rather than being consulted per call site. The
home-screen widget remains a separate surface that follows the *device* (§5.11), which is why
`NightClockwisePalette` still exists even though True Dark was retired.

**The agreed tokens are user-specified:** Light Monochrome's `#F3F3F1` / `#111111`, Warm Sepia's
`#F7F4EF` / `#2C2521` and Cool Slate's `#EEF2F5` / `#1E293B` are the values the theme design names
(2026-10-18). Every other value is derived so that all three keep the *same relationships*:

- `primary` is the **inverse of the base surface** in all three, so selection reads as a card
  flipped to the opposite ink.
- `card` is one step lighter than `background`; the two themes that are not monochrome step
  `secondary`, `accent` and `border` *down* from the background by the same kind of increments.
- `mapAccent` is identical in all three — the one warm accent works against every background and
  is the app's only non-monochrome element.
- `mapLandmass` is `accent` and is distinct from the ocean, so the continents never vanish; a
  test asserts the pair differs rather than pinning a hex value.
- `mapNight` **carries its own alpha** and is not derived from `foreground`. Deriving it would
  wash a dark map *lighter*, which reads as daylight. In the light themes it is the ink colour
  at 18 %, so the light monochrome value stays byte-identical to phase 1.

**`NightClockwisePalette` is not a theme (3.1.0).** It is the dark half of the widget's day/night
pair, used only by `DayNightColorProvider`: `#111111` background, `#1A1A1A` card, `#E1E1DE`
foreground, `#777774` muted, black night wash at 45 %. It was `DarkClockwisePalette` while True
Dark existed; the values are unchanged, the name now says what it is actually for.

**How the active palette reaches the UI.** `paletteFor(theme)` is the single place that maps a
theme to a palette (pure, so the four-way mapping is unit-testable without a device).
`FixedClockwiseTheme(theme)` provides the result through `LocalClockwisePalette`. `ClockwiseColors`
exposes it with Material-style composable getters (`ClockwiseColors.Card`), mirroring
`MaterialTheme.colorScheme`, so a screen written once paints correctly in every theme and no
call site needed to change. Because they read a composition local they are only readable from
a composable scope — non-composable drawing code (the map's `DrawScope` steps) receives a
`ClockwisePalette` parameter instead.

**Contrast (asserted by `ClockwisePaletteTest`, over the three static themes, the widget's night
palette, and both Material You fallbacks):** main text ≥ 4.5:1 on both background and card, text
on the selected card ≥ 4.5:1, supporting text ≥ 3:1 on the background, the card and the soft pills
(it is used only for short supporting labels, so the large-text / non-text threshold is the right
bar), and the translucent supporting text on the selected card is composited before it is
measured. A wallpaper-derived scheme cannot be asserted from a JVM test, so the dynamic path is
checked by construction instead: every token is taken from a Material role that Material 3 already
guarantees to pair, and the Robolectric screenshot renders the static fallback.

#### 7.1.1 Material You

**IMPLEMENTED (2.1)** — `paletteFor` / `FixedClockwiseTheme` (`ui/theme/Theme.kt`),
`MaterialYouFallbackPalette` / `MaterialYouDarkFallbackPalette` (`ui/theme/Color.kt`).

| Aspect | Decision |
| --- | --- |
| Availability | `dynamicLightColorScheme` / `dynamicDarkColorScheme` on **Android 12+ (API 31)**; the choice is offered on every device, because hiding it by API level would make the settings list change shape between phones |
| Light/dark | The platform scheme matching the **device** configuration (`isSystemInDarkTheme()`), because a wallpaper scheme has no stored light/dark variant of its own |
| Role mapping | `background` ← `background`, `foreground` ← `onBackground`, `card` ← `surfaceContainer`, `primary` ← `primary`, `primaryForeground` ← `onPrimary`, `onPrimaryMuted` ← `onPrimary` at 72 % alpha, `secondary` ← `surfaceContainerHigh`, `mutedForeground` ← `onSurfaceVariant`, `accent` ← `secondaryContainer`, `border` ← `outlineVariant`, `mapAccent` ← `tertiary`, `mapLandmass` ← `surfaceContainerHighest`; `mapNight` stays translucent black at 45 % in a dark scheme and 18 % in a light one, because a wallpaper colour cannot promise to darken the map |
| Fallback | Below API 31, or when the platform returns no dynamic scheme, the static `MaterialYouFallbackPalette` / `MaterialYouDarkFallbackPalette` is used — a real Material 3 light/dark pair, not a monochrome theme. The screen therefore never claims wallpaper colours it cannot produce |
| Dark detection | `isDark` on the resolved palette, so the system bar icons and `darkColorScheme()` follow the scheme actually painted |
| Persistence | Saved as `SAVED_APP_THEME_MATERIAL_YOU = 5`; the three older ids are unchanged and Light Monochrome remains the default |

**Decision record — an opt-in theme, not a replacement (2026-10-27).** Material You is additive
because the monochrome palettes are the app's identity and some users chose them precisely to
avoid wallpaper-derived colour. Making it opt-in also keeps the persisted ids stable: no
migration is needed and an upgrade cannot repaint an existing install. When True Dark and High
Contrast were retired in 3.1.0, Material You was deliberately **kept** — it was the one theme that
takes its colours from the device rather than from a hand-built palette, which made it the only
row in the picker that could not be reproduced by another.

**Decision record — a Material fallback, not a monochrome one (2026-10-27).** Falling back to
Light Monochrome below Android 12 would make the same setting produce a different *kind* of
look on old and new phones. A static Material 3 light/dark pair keeps the promise the row makes
("Material You") as closely as the platform allows, and gives the Robolectric suite a
deterministic palette to render.

**System bars.** `SystemBarAppearance` sets the status- and navigation-bar icon appearance
from the *active palette* rather than the device setting. `enableEdgeToEdge()` decides from the
device configuration, which was only right while the mode was `System`. The three fixed themes are
all light, so on a dark device the device-derived choice would put light icons over an off-white
bar; reading `isDark` from the palette is what keeps them legible. For Material You the two agree
by definition, because the scheme follows the device configuration.

**REQUIREMENT:** apart from the opt-in Material You theme, no purple, no blue accent and no
gradients.

### 7.2 Typography

**IMPLEMENTED** — Inter Regular / Medium / SemiBold / Bold mapped onto the Material 3 type
roles, plus a `ClockwiseTextStyles` object for app-specific styles such as
`MapRulerLabel`.

| App style | Size / weight | Used by |
| --- | --- | --- |
| `Eyebrow` | 11 sp Medium, 1.6 tracking | Small-caps label above every screen title |
| `ScreenTitle` | 32 sp Bold, −0.6 tracking | `World Time`, `Meeting planner` |
| `SectionLabel` | 11 sp SemiBold, 1.4 tracking | `YOUR CITIES`, `PLAN A MEETING` |
| `DstCaption` | 10 sp Medium, 1.2 tracking | `AUTO-ADJUSTS FOR DAYLIGHT SAVING`, planner legend |
| `CityDifference` | 10 sp Medium | Time difference on a city card |
| `DetailDifference` | 18 sp SemiBold | Time difference on the detail screen |
| `DetailCityLabel` | 15 sp Medium | The user's label, in brackets |
| `MapRulerLabel` | 9 sp Medium, tabular | Longitude ruler on the map |
| `PlannerClock` | 48 sp Bold, tabular | The chosen slot at the top of the meeting planner |
| `PlannerRowName` | 17 sp SemiBold | City name in a planner row |
| `PlannerSourceChip` | 11 sp SemiBold, 0.6 tracking | `LOCAL` and city chips in the planner's `FROM` row |
| `PlannerRowTime` | 20 sp Bold, tabular | A city's time in a planner row |
| `PlannerRowDifference` | 11 sp Medium | Offset under a planner row's name |
| `PlannerAxisLabel` | 9 sp Medium, tabular | Hour labels under the day slider |

Tabular figures matter wherever a clock is drawn or a column of times is aligned: proportional
digits would make `11:30` and `18:30` different widths and the times would jitter as the
minutes change.

### 7.3 Shape and spacing

| Token | Value |
| --- | --- |
| Base corner radius | 16 dp (`ClockwiseShapes.medium`) |
| City card height | ≈ 93 dp |
| Horizontal page padding | ≈ 24 dp |
| Map aspect ratio | 1.7 |
| Minimum touch target | 48 dp (`MinTouchTarget`) |

### 7.4 App icon

**IMPLEMENTED (3.0.0)** — `drawable/ic_launcher_background.xml`,
`drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml`,
`mipmap-anydpi-v26/ic_launcher.xml`, `mipmap-anydpi-v26/ic_launcher_round.xml`, the five
`mipmap-*dpi` PNG pairs, asserted by `AppIconTest`.

PJ95 "Concept 3". The mark has to be legible at 48 px on a home screen and still say four things,
so it is one construction rather than four ornaments:

| Reading | How it is drawn |
| --- | --- |
| A clock | A 25 dp radius dial with two hands and a centre pin |
| The letter **C** (Chronos) | The dial is a 330° arc: it is open across the top right, so the silhouette of the ring is a C |
| A globe | The ring doubles as the meridian; the opening suggests the visible hemisphere instead of a closed border |
| Myanmar script | The mark is built on a circle with straight, cut strokes crossing it — the construction Latin-Myanmar hybrid lettering shares — and the minute hand leaves the dial through the opening, where a Burmese letter would carry its tail |

**Geometry.** 108 × 108 dp adaptive canvas. Dial centre `(54, 54)`, radius `25`, stroke `6.5`;
the arc runs from 60° round to 30°, leaving a 30° opening. Hour hand to `(54, 40.5)`, minute hand
to `(75.21, 32.79)` (45°, i.e. through the opening), pin radius `3.6`. The outermost point is
`r = 30` from centre, inside the 66 dp safe zone (21–87) that every launcher mask honours, so no
mask shape clips the mark.

**Palette.** Deep royal blue `#0E2A66` field with one facet at `#143675` (the same blue lifted one
step, a 45° cut in the bottom-right corner); ivory `#F7F4EC` dial and hour hand; gold `#E2B65A`
minute hand, centre pin and a 50° inlay on the lower-left of the dial. Gold appears three times and
nowhere else, which is what keeps it an accent rather than a second colour. Ivory is the app's own
off-white surface (`#FAFAF8`) pushed slightly warm so it does not read as pure white against the
blue.

**Layers, not a flattened bitmap.** The background is a drawable rather than a colour so the facet
survives the mask; the foreground carries the mark; the monochrome layer is a separate file because
one colour cannot distinguish the gold inlay from the ivory ring, and merging them would leave a
seam — it reduces the mark to the ring, the hands, the pin and the opening, which still reads as
both a clock and a C. All three are vectors.

**Legacy rasters.** `minSdk` is 26, so `mipmap-anydpi-v26` is chosen on every supported device and
the `mipmap-*dpi` PNGs are never loaded at runtime. They exist for tools and launchers that read
the base `mipmap/ic_launcher` resource instead of the v26 variant. They are **not** hand-drawn: each
is the two vector layers composed at 48/72/96/144/192 px, so a change to the vectors that is not
re-rendered shows up as a stale raster rather than as a second design. `AppIconTest` renders the
composed icon to `app/build/screenshots/app_icon.png` for review and inflates every layer, which is
the only thing a JVM can check about artwork.

**Decision record — a dark field, unlike the app (2026-10-27).** The five app themes are light,
warm or monochrome; the icon is deep royal blue. That is deliberate: the icon is the PJ95 studio
identity for the product, not a preview of the first screen, and a near-white tile would disappear
against the light home screens the app is designed for. The gold is the only colour the app itself
never uses, which is what makes the icon recognisable as branding.

**Decision record — keep `app_name` as "Clockwise" (2026-10-27).** "Chronos" is the project and
studio codename. The launcher label, Recents card and widget-picker label stay "Clockwise", so
the rebrand changes the publisher and the artwork, not what the user thinks the app is called.

---

## 8. Data model and persistence

### 8.1 Proto schema

**IMPLEMENTED** — `app/src/main/proto/saved_cities.proto`, compiled with `protobuf-lite`.

```proto
// Superseded: the light/dark choice of the three-mode build. Read-only, for
// migration; nothing writes it any more and the next theme change clears it.
enum SavedThemeMode {
  THEME_MODE_SYSTEM = 0;   // proto3 zero value; also what an older store reads as
  THEME_MODE_LIGHT  = 1;
  THEME_MODE_DARK   = 2;
}

// The themes, numbered as AppTheme ids 0-5.
enum SavedAppTheme {
  SAVED_APP_THEME_LIGHT_MONOCHROME = 0;
  SAVED_APP_THEME_TRUE_DARK        = 1;
  SAVED_APP_THEME_WARM_SEPIA       = 2;
  SAVED_APP_THEME_COOL_SLATE       = 3;
  SAVED_APP_THEME_HIGH_CONTRAST    = 4;
  SAVED_APP_THEME_MATERIAL_YOU     = 5;  // added in 2.1, after the original five
}

message SavedCities {
  repeated string city_ids = 1;      // ordered; order is significant
  string active_city_id = 2;         // empty when nothing is selected
  bool use_24_hour = 3;              // true = 24-hour; first launch seeds true
  bool seeded = 4;                   // false until defaults are seeded
  SavedThemeMode theme_mode = 5;     // legacy; read for migration only
  map<string, string> city_labels = 6;  // label per city id; absent = no labels
  optional SavedAppTheme theme = 7;  // the chosen theme; absent = nothing stored
  optional bool onboarding_completed = 8;  // absent = completed (pre-2.1 store)
  optional bool compare_with_local_time = 9;  // absent = on (pre-3.1 store)
}
```

**Decision record — field 4:** named `seeded`, not `initialized`, to avoid any clash with
protobuf's generated `isInitialized()`. It exists so that an empty city list can mean *"the
user removed every city"* rather than *"new install"*.

**Decision record — field 5 (legacy):** the enum is `SavedThemeMode`, **not** `ThemeMode`, so the
generated type cannot collide with the domain enum of the same concept. `THEME_MODE_SYSTEM` is
`0`, so a store written before theme support existed (field absent) reads as `SYSTEM`. The palette
themes replaced this field (field 7); it is kept so that those stores still read, and `setTheme`
clears it in the same write, so a store carries one answer rather than two that can disagree.
`toMigratedAppTheme()` maps `LIGHT → Light Monochrome` — what those users were actually looking
at — and `DARK → the default`, because dark meant True Dark, which 3.1.0 retired. `SYSTEM → Light
Monochrome`, because "follow the device" cannot be resolved after the fact and guessing a theme
the user never chose would be worse than the default.

**Decision record — field 7:** `theme` is `optional` (**`optional`**, and this is the point)
rather than a plain enum field. Light Monochrome is the proto3 zero value, so without the
presence bit "nothing stored" and "Light Monochrome chosen" would be the same bytes — and every
user who had picked dark under the old three-mode switch would silently be handed the light
theme on upgrade. `UserPreferences.toUserPreferences()` therefore reads
`hasTheme() ? theme.toAppTheme() : themeMode.toMigratedAppTheme()`.
`seededWithDefaults()` uses `toBuilder()` and therefore never overwrites a stored theme choice.

**Migration is one-way and needs no rewrite.** A migrated store still carries the legacy field
until the user next changes the theme; the read path is the only thing that has to know about
it, and every subsequent write converges the store on field 7.

**Decision record — field 6:** `city_labels` is a map keyed by city id, not a repeated message,
because a city has at most one label and the id is the only stable key (a label cannot identify
its city). An absent map reads as "no labels", so no migration is needed. Unknown keys are
pruned on read, the same rule as `city_ids`; a label for a city that is merely **removed from
the list** but still exists in the directory is **kept**, so re-adding that city restores its
label instead of silently losing it.

**Decision record — field 8 (onboarding, 2026-10-27):** `onboarding_completed` is `optional`
for exactly the same reason as `theme`. The intro must appear on a **fresh install** but must
**not** appear for someone upgrading from 2.0.0 or earlier. `seededWithDefaults()` writes an
explicit `false` on a store it creates, while a pre-existing store has no presence bit;
`toUserPreferences()` reads `hasOnboardingCompleted() ? onboardingCompleted : true`. A plain
`bool` would make "never stored" and "dismissed" the same bytes and either re-show the intro to
every existing user or hide it from every new one. A DataStore read failure also resolves to
`UserPreferences.Default`, whose `onboardingCompleted` is `true`, so a broken store can never
trap the user behind the intro.

**Decision record — field 9 (comparison, 2026-10-28):** `compare_with_local_time` is `optional`
for the same reason again, but the default is deliberately **true**: the app always showed the
difference before the switch existed, so a store with no presence bit has to read as "on" or an
upgrade would silently remove a line the user relies on. `toUserPreferences()` reads
`hasCompareWithLocalTime() ? compareWithLocalTime : true`, and both `UserPreferences.Default` and
`UserPreferences.Empty` compare, so a read failure keeps the familiar screen rather than a
mysteriously empty one. An explicit `false` is preserved: the presence bit is what distinguishes
"never chosen" from "turned off".

### 8.2 Repository behaviour

**IMPLEMENTED** — `ClockRepository`

- `addCity`, `removeCity`, `reorderCities`, `selectCity`, `restoreCity`, `setUse24Hour`, `setTheme`,
  `setCityLabel`, `setCompareWithLocalTime`, `completeOnboarding`, `restartOnboarding`.
- `restoreCity(cityId, orderedIds, activeCityId)` is the Undo path: it re-adds the city if the
  removal already landed, sanitises the previous order against what is saved now, and restores the
  previously active city when it is still in the list. It writes the Proto list and the Room
  positions exactly like `reorderCities`, so a restored list and a dragged list are the same shape.
- `setTheme(theme)` writes field 7 and clears the legacy field 5.
- `setCompareWithLocalTime(enabled)` writes only field 9; the cities, theme and labels are
  untouched, so a display preference never reshuffles the list.
- `reorderCities(orderedIds)` sanitises user input — only currently saved ids are accepted,
  duplicates collapse, and any saved id the caller left out is appended in its old relative
  order rather than dropped. It writes twice in one call: the Proto `city_ids` list (so the
  proto and the widget fallback agree) and the Room `saved_cities` positions. A no-op order
  returns before either write, so a drag that ends where it started costs nothing. A read
  failure emits `PrefsError.ReadFailed` and leaves both stores untouched.
- `completeOnboarding()` writes `onboarding_completed = true`; `restartOnboarding()` writes
  `false`, which is what makes the Settings replay work without a restart.
- `setCityLabel(cityId, label)` trims the label; `null`, an empty string or whitespace clears
  it. It only ever touches the label map.
- Prunes ids that are no longer in the directory.
- Repairs a dangling active id by promoting a surviving city.
- Emits `PrefsError` on read/write failure; `ClockwiseRoot` surfaces it in a snackbar
  (`Couldn't read your saved cities. Showing defaults.` / `Couldn't save your changes.`).
- Room is reconciled on every read: `synchronizeOrder()` seeds an empty `saved_cities` table
  from the DataStore order (the 2.0.0 upgrade path), then keeps the Room order, prunes ids
  DataStore no longer holds and appends ids Room has not seen yet. A Room failure falls back to
  the DataStore order and is repaired on the next attempt rather than surfaced as an error.

### 8.3 Room city catalog

**IMPLEMENTED (M0)** — `data/local/`, `data/repository/CityRepository.kt`.

Room stores the bundled 22-city catalog in `cities` with a stable `id` primary key,
English labels, IANA timezone, coordinates, `isBuiltIn` and `sortOrder`. The database is
opened lazily on first use and the application seeds it on an IO coroutine at startup;
seeding is an idempotent `REPLACE` upsert, so a launch after an update refreshes built-in
rows without duplicating them or deleting future custom cities.

`CityRepository` exposes `cities: Flow<List<City>>`, `search(query)` and `find(id)`. A Room
read failure falls back to `CityDirectory.all`, so the app is never left without a catalog.
User preferences are **not** duplicated in Room, with one deliberate exception: the saved-city
**display order** lives in `saved_cities` (v2.1, [section 8.5](#85-saved-city-display-order)).
Proto DataStore remains authoritative for membership, the active city, 12h/24h, theme, labels
and onboarding state.

### 8.4 Room solar cache

**IMPLEMENTED (M1)** — `solar_days`, `SolarDao`, `SolarRepository`.

Version 2 adds one row per city and local date: the sunrise/sunset instants (nullable on a
polar day or night), solar noon, the day kind, the day length, the coordinates used, and
when the row was calculated. The primary key is `(cityId, dateEpochDay)` and the date is
indexed for pruning.

The coordinates are part of the row so a corrected city cannot serve a stale answer: on a
read, a row whose coordinates differ from the city is ignored and overwritten. Cache reads
and writes are best-effort; a Room failure still returns the freshly calculated events.

**M5 — bounded retention.** `SolarCachePolicy` defines a **30-day** retention window computed
from the **device's local date** (`LocalDate.now(clock.withZone(ZoneId.systemDefault()))`), not
the UTC date: a UTC cutoff would prune a day early in Yangon (UTC+6:30) and a day late in Los
Angeles (UTC-7). `ClockwiseApp` calls `SolarRepository.pruneBefore(cutoff)` on every launch,
so the table cannot grow by one row per city per day forever. The current UI only reads today's
solar row, so the 30-day grace never hides a value it would ask for; pruning is best-effort and
a failure is logged for the next launch.

Schemas v1, v2 and v3 are exported under `app/schemas/`. Migration 1 -> 2 only creates
`solar_days`; migration 2 -> 3 only creates `saved_cities`. `cities` is untouched by both. No
destructive migration is configured.

### 8.5 Saved-city display order

**IMPLEMENTED (2.1)** — `SavedCityEntity`, `SavedCityDao`, `CityOrderStore`, database v3.

Room owns the order the Clocks screen and the widget read; Proto DataStore keeps membership and
is mirrored on every write. The table is deliberately tiny:

| Column | Type | Meaning |
| --- | --- | --- |
| `cityId` | `TEXT NOT NULL`, primary key | Stable city id; one row per saved city |
| `position` | `INTEGER NOT NULL`, indexed | 0-based display position, ascending |

- `CityOrderStore.order` is a `Flow<List<String>>` ordered by `position`; `replaceAll` writes
  the list in one transaction, `remove` deletes one row and `getOrder()` is the synchronous
  reconciliation read.
- The authority is decided per question: **membership** (which cities are saved) is DataStore's,
  because it is the format an older build and the widget can read; **order** is Room's, because
  a drag is a relational operation and the row positions are what the UI observes.
- **Migration 2 -> 3** creates `saved_cities` and its index and leaves the table **empty**. The
  repository seeds it from the existing DataStore `city_ids` on first read, so an upgrade keeps
  the user's list in the order they had it. No destructive migration is configured.
- Schema v3 is exported under `app/schemas/`, alongside v1 and v2, and a migration test opens
  v2, runs `MIGRATION_2_3` and asserts the resulting table and index.
- Removing a city deletes its row and then re-runs reconciliation; adding a city appends it and
  repairs any missing row, so the two stores converge without a full rewrite. Unknown ids are
  pruned on both sides; a saved id Room has not seen is appended in DataStore order.

### 8.6 Room alarms

**IMPLEMENTED (3.1.0, extended 4.1.0)** — `AlarmEntity`, `AlarmDao`, `RoomAlarmRepository`, database
v4, v5, v7 and v8.

```text
alarms
  id                    INTEGER PRIMARY KEY AUTOINCREMENT
  hour                  INTEGER NOT NULL       -- 0-23, device zone
  minute                INTEGER NOT NULL       -- 0-59
  label                 TEXT NOT NULL          -- "" when unset, <= 40 chars
  enabled               INTEGER NOT NULL       -- 0 / 1
  repeatDays            INTEGER NOT NULL       -- bit mask, Monday = bit 0, 0 = one-shot
  soundUri              TEXT NOT NULL          -- "" = the device's own alarm sound (v5)
  snoozePolicy          INTEGER NOT NULL       -- SnoozePolicy ordinal, 0 = ALLOWED (v7)
  snoozesUsed           INTEGER NOT NULL       -- snoozes in the current cycle (v7)
  snoozedAtEpochMillis  INTEGER                -- NULL when not snoozed (v7)
  sortOrder             INTEGER NOT NULL       -- user's chosen list position (v8)
```

- **Migration 3 -> 4** creates the table and leaves `cities`, `solar_days` and `saved_cities`
  untouched; the table starts empty.
- **Migration 4 -> 5** adds `soundUri TEXT NOT NULL DEFAULT ''`, so every alarm stored by 3.0.x
  keeps ringing with the device default rather than with silence or a null.
- **Migration 6 -> 7** adds `snoozePolicy` (default `0`, unlimited), `snoozesUsed` (default `0`) and
  a nullable `snoozedAtEpochMillis`. Every existing alarm therefore behaves exactly as it did in
  4.0.0, and no ring is in progress when the upgrade runs.
- **Migration 7 -> 8** adds `sortOrder INTEGER NOT NULL DEFAULT 0` and backfills every row with its
  position in the old `hour, minute, id` order, so the upgrade is invisible: the list opens in
  exactly the order the user left it. New alarms are appended at `maxSortOrder + 1`.
- Schema v4, v5, v6, v7 and v8 are exported next to v1-v3, and `ClockwiseDatabaseMigrationTest`
  opens a real v3, v4, v5, v6 and v7 file, runs the matching migration and asserts both the row
  values and `PRAGMA user_version`.
- `AlarmDao.reorder` rewrites the positions in one `@Transaction`; a move never calls
  `AlarmScheduling`, so a card that changes place keeps the trigger it already had. `getAll`,
  `observeAll` and `getEnabled` all order by `sortOrder, hour, minute, id`.
- `AlarmDao.markSnoozed` increments the counter and writes the marker in one statement, so two taps
  cannot both read the same count and each decide they are the first. `resetSnoozeCycle` clears both
  for a natural fire or a Dismiss; `clearSnoozeMarker` keeps the count for a snooze re-fire.
- `RoomAlarmRepository` saves the row first and only then arms or cancels through
  `AlarmScheduling`; an update that matches no row arms nothing, so a deleted alarm cannot leave
  a phantom trigger. `snooze` refuses a policy-blocked write, and `save` resets the cycle because an
  edited alarm is a new description of what should ring next. `restore` re-inserts the exact row
  (explicit id, position and settings) and re-arms it only when it was enabled, and `reorder`
  delegates to the DAO without touching the scheduler.
- No destructive migration is configured at any version.

### 8.7 Room timer

**IMPLEMENTED (4.0.0)** — `TimerEntity`, `TimerDao`, `RoomTimerRepository`, database v6.

```text
timer
  id                INTEGER PRIMARY KEY     -- always 1
  durationMillis    INTEGER NOT NULL        -- what the user chose
  remainingMillis   INTEGER NOT NULL        -- frozen remainder while paused
  endAtEpochMillis  INTEGER                 -- NULL while paused
  running           INTEGER NOT NULL        -- 0 / 1
  label             TEXT NOT NULL           -- "" when unset
```

- **Migration 5 -> 6** creates the table and nothing else; it starts empty, so an upgrade gains a
  timer tab that is ready to start rather than a half-counted timer invented at upgrade time.
- Schema v6 is exported next to v1–v5, and `ClockwiseDatabaseMigrationTest` opens a real v5 file
  with an alarm in it, runs `MIGRATION_5_6`, asserts the timer table is empty, inserts a paused
  row and reads it back, and checks that the alarm and its chosen `soundUri` survived and that
  `PRAGMA user_version` is 6.
- `TimerDao` is a fixed-key `@Upsert` plus a `clear()`: there is one timer, so "insert or replace"
  is the only write that makes sense and two timers cannot race.
- `RoomTimerRepository` writes the row, then arms/disarms `AlarmManager`, then updates the
  notification, all in one coroutine, so "stored", "armed" and "on the shade" cannot disagree.
- No destructive migration is configured at any version.

---

## 9. Accessibility requirements

**IMPLEMENTED**

| Requirement | Status |
| --- | --- |
| Icon-only controls have `contentDescription` | Yes — add, back, check, close, delete, expand, globe, moon, settings, search, sun |
| Emoji never used as a UI icon | Yes — vector drawables only |
| Selected state exposed | Yes — `Role.RadioButton` + `Selected` semantics on the 12h/24h control, the **six** theme rows, the planner's source chips, and map pills and markers. A city card also exposes `selected` when it is the active city |
| A group of choices announces itself as a group | The theme picker's rows sit in `selectableGroup()`, so a screen reader says "Warm Sepia, radio button, selected" and the six are read as one set of options; the planner's `FROM` chips do the same, with the device and every saved city as the options |
| Reordering is possible without a drag | Every city card carries **Move up** (`a11y_move_city_up`) and **Move down** (`a11y_move_city_down`) `CustomAccessibilityAction`s. The first card omits Move up, the last omits Move down, and a one-city list offers neither. The drag grip is decorative (`contentDescription = null`) so it adds no focus stop |
| A reorder is confirmed by the list, not only by position | The card's announcement includes the city, its time and its offset, and moving it changes the reading order; `selected` still names the active city after the move |
| Disabled/already-added state exposed | Yes — search results announce `%1$s is already added` |
| Press feedback | Ripple on every interactive surface, including the gear action |
| Back behaviour | Detail screen back arrow **and** system back return to the previous destination; system back dismisses the search and settings sheets |
| Screen-reader descriptions | `a11y_world_map`, `a11y_utc_ruler`, `a11y_map_marker`, `a11y_map_marker_selected`, `a11y_select_city_on_map`, `a11y_settings`, `a11y_difference_from_your_time`, `a11y_edit_city_label`, `a11y_meeting_time_slider`, `a11y_planner_row`, `a11y_planner_source`, `a11y_planner_source_device`, `a11y_planner_best`, `a11y_planner_apply_best`, `a11y_planner_no_overlap`, `a11y_move_city_up`, `a11y_move_city_down`, `a11y_widget_refresh`, `a11y_timer_remaining`, `a11y_screen_saver` |
| The first-launch intro is readable and dismissible | The intro is plain text plus one primary button, all inside the normal scroll and semantics tree; `OnboardingTags.SCREEN` tags the page for tests, and **Get started** is a standard ≥ 48 dp button carrying its own visible label. No custom gesture or pager is required to leave it |
| Launcher shortcuts have names, not just icons | Each static shortcut carries a short and a long label (`shortcut_add_city_*`, `shortcut_planner_*`, `shortcut_map_*`), and each dynamic city shortcut's long label is `Time in %1$s` (`shortcut_city_long`), so the launcher's own accessibility surface announces the city |
| Colour is never the only signal | Selected cards also change text colour and elevation; every theme in the picker is named in text and its row reports itself as a selected radio button, so the preview swatch is decoration rather than the signal; the planner's working day is a shaped marker plus a stated legend, not a tint alone |
| Meaning carried by a value alone is spoken in context | A card's difference is announced as `Compared with your local time: %1$s`, not as the bare `+2 hrs ahead` |
| A control whose value is not a plain percentage says so | The day slider's `stateDescription` reads the slot (`09:00`), because TalkBack's default for a progress bar — "38 percent" — would be useless for a meeting time |
| A row of values is one swipe, not four | Each planner row merges its children and announces `Meeting time in %1$s: %2$s, %3$s`, including the city's weekday and whether it is within working hours |
| A recommendation is a named state, not a colour | The `BEST <time>` action announces `Best time %1$s: %2$d cities within working hours` and offers `Use the best time`; the non-interactive `NO OVERLAP` state says why there is no recommendation |
| A control is never smaller than a finger, on the app's own surfaces | Every interactive control in the app is at least 48 dp tall. The 44 dp circular buttons draw at 44 dp but carry the click on a 48 dp box, and the segmented control's options are padded out to 48 dp, so the drawn size and the hit size are separate numbers. The home-screen widget cannot meet this and is the documented exception below |
| Large text does not clip the layout | The Clocks screen is rendered at a **1.5× font scale** in `LayoutScreenshotTest` (`clocks_screen_large_font_360dp.png`), so typography growth is reviewable without a device. The widget derives row and header heights from the user's font scale and reduces the row budget instead of drawing text past the card's edge. The largest Android font/display settings remain a hardware item in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) |
| The widget's small controls trade height for width | A city row is a full-width 28 dp tap target (the header 32 dp, the refresh target 24 dp), below the 48 dp guideline. Six rows plus chrome do not fit in a 4×4 card at 48 dp each, so the rows have no gaps and span the card, making the effective target far wider than it is tall; the refresh glyph is deliberately the one small secondary action. This is the widget-specific trade-off recorded in [section 5.11](#511-home-screen-widget) and checked by hand in [section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen) |
| Screen titles and section headers are headings | `heading()` on `World Time` / `YOUR CITIES`, `Meeting planner` / `YOUR LOCAL TIME` or `TIME IN <CITY>` / `YOUR CITIES AT THIS TIME`, `Settings` / `APPEARANCE`, `City label`, and the detail screen's `COMPARED WITH YOUR LOCAL TIME` / `YOUR LOCAL TIME`, so TalkBack's heading navigation works |
| A dialog drawn in the app's own composition announces itself | `CityLabelDialog` sets `paneTitle`, because it is an in-composition overlay rather than a platform dialog window; its field is named `Label for %1$s` instead of falling back to the placeholder |
| The widget's icons and actions are labelled | The sun/moon in each widget row carries `a11y_daytime` / `a11y_nighttime`, and the refresh glyph carries `a11y_widget_refresh`. The header opens the app, a row opens that city's detail, and `+N more` opens the app; `RemoteViews` cannot merge semantics, so a row is announced in reading order — which is why the weekday is shown only when it differs from the device's date |
| A countdown is spoken as time left, not as digits | The timer display is one node whose `contentDescription` is `%1$s remaining`, and its status line says `REMAINING` or `PAUSED` in words, so TalkBack never reads `02:05` as a time of day |
| The timer's wheels are one control each | Each wheel is a single node labelled `Hours` / `Minutes` / `Seconds`; the numbers inside are decorative, because fifty focus stops per column would be unusable |
| The screen saver can always be left | The whole surface is one button whose label is `TAP ANYWHERE TO EXIT` and whose description is `Screen saver clock. Tap to return to the app.`; system Back also finishes, so a mis-tap is never a trap |
| A settings status is words, not a colour | The four alarm rows spell out `ALLOWED` / `ALLOW` / `ON` / `OFF` / `EXEMPT` / `CHECK` beside the row title, so a grant that is missing is readable without seeing its tint |

---

## 10. (reserved)

Reserved so that section numbers in this document line up with the original specification's
section numbering. No content.

---

## 11. Acceptance criteria

**REQUIREMENT** — confirmed by the user on 2026-10-17 (see
[section 0.1](#01-section-11-provenance--resolved)).

**IMPLEMENTED** — every criterion below has an executable test in
`app/src/test/java/com/pj95/clockwise/ui/AcceptanceCriteriaTest.kt`.

| # | Criterion | Test |
| --- | --- | --- |
| 1 | First launch shows Tokyo, Sydney, Los Angeles, New York, London in that order with Los Angeles active | `criterion01_firstLaunchShowsTheDefaultCitiesInOrderWithLosAngelesActive` |
| 2 | Every city resolves to its correct IANA timezone and offset | `criterion02_everyCityMatchesItsIanaTimezone` |
| 3 | Daylight saving is applied automatically and correctly | `criterion03_daylightSavingIsAppliedAutomatically` |
| 4 | Sun icon for local 06:00–17:59, moon icon for 18:00–05:59 | `criterion04_dayAndNightFollowTheSixToEighteenRule` |
| 5 | Search covers the 22-city directory and refuses duplicates | `criterion05_searchCoversTheDirectoryAndRejectsDuplicates` |
| 6 | Selecting a city makes it active and opens its detail screen | `criterion06_selectingACityMakesItActiveAndOpensItsDetail` |
| 7 | Removing cities promotes a new active city; emptying shows the empty state | `criterion07_removingCitiesHandlesTheActiveCityAndTheEmptyState` |
| 8 | The 12h/24h switch changes the whole app and persists | `criterion08_theTimeFormatSwitchesAndPersists` |
| 9 | Saved state survives restart; unknown ids are dropped and the active id repaired | `criterion09_savedStateSurvivesRestartAndUnknownIdsAreDropped`<br>`criterion09b_unknownIdsAreDroppedAndTheActiveCityIsRepaired` |
| 10 | The map plots cities, shades night, and drives selection | `criterion10_theMapPlotsCitiesAndDrivesSelection` |
| 11 | Layout holds at 360 dp and every control is labelled; empty and error states are reachable | `criterion11_layoutHoldsAt360dpAndControlsAreLabelled`<br>`criterion11b_iconOnlyControlsCarryContentDescriptions`<br>`criterion11c_theEmptyAndErrorStatesAreReachable` |

**Phase 2 — theme (four themes since 3.1.0)**

| # | Criterion | Test |
| --- | --- | --- |
| 12a | The chosen theme is persisted and survives a restart, without disturbing the saved cities; a store written by the three-mode build is migrated (light → Light Monochrome, dark and system → the default) rather than reset | `criterion12a_theChosenThemeIsPersistedAndSurvivesARestart`<br>`criterion12a_anOlderStoreIsMigratedToATheme` |
| 12b | Each of the four themes provides its own palette (Material You through its static fallback off-device), a change of theme repaints without recreating anything, and every fixed theme paints a light surface regardless of the device's own light/dark setting | `criterion12b_theChosenThemePaintsTheScreenWithTheMatchingPalette`<br>`criterion12b_theDeviceSettingNoLongerPicksThePalette` |
| 12c | The appearance control is reachable, offers all four themes by name, reports which is in force as a radio selection, renders neither retired theme, hands the new choice back to the caller, and leaves every control labelled | `criterion12c_theSettingsSheetOffersEveryThemeAndReportsTheSelection` |

**Phase 2 — time difference and city labels**

| # | Criterion | Test |
| --- | --- | --- |
| 13a | The difference is the gap between the two zones' offsets at the same instant, so half-hour and quarter-hour zones are exact and daylight saving is picked up automatically | `criterion13a_theDifferenceIsTheOffsetGapAtThatInstant` |
| 13b | The indicator reads `Same time`, `+N hrs ahead` or `-N hrs behind`, keeping the minutes a half-hour zone needs | `criterion13b_theDifferenceReadsAheadBehindOrSameTime` |
| 13c | Every city card carries the difference, measured from the device's own time | `criterion13c_everyCityCardShowsTheDifferenceFromTheDeviceTime` |
| 13d | The detail screen shows the difference and names the reference it is measured against | `criterion13d_theDetailScreenNamesTheReferenceItComparesWith` |
| 14a | A city label is stored against the city id, survives a restart, can be changed and cleared, and disturbs nothing else | `criterion14a_aCityLabelIsPersistedAndCleared` |
| 14b | The label reaches the screens as the name followed by the label, leaving unlabelled cities alone | `criterion14b_theLabelIsShownAfterTheCityName` |
| 14c | The whole edit loop works through the real app shell: dialog opens, saves, shows, and removes the label | `criterion14c_theEditDialogSavesAndRemovesALabel` |

**Phase 2 — meeting planner**

| # | Criterion | Test |
| --- | --- | --- |
| 15a | The Clocks screen offers the meeting planner, and opening it puts a 24-hour slider on screen set to the current time | `criterion15a_theClocksScreenOffersAFullDayMeetingPlanner` |
| 15b | The slot is the single input to every row: sweeping the slider moves all saved cities at once, each by the same amount, and every row is that city's own clock at the slot's instant | `criterion15b_everyCityMovesWithTheSlot` |
| 15c | The slider is drivable with a finger and by a screen reader, and the plan follows the movement live rather than on release | `criterion15c_theSliderIsDrivableAndThePlanFollowsItLive` |
| 15d | The planner names the working day, judges it in each city's own zone, and marks the day each city is on | `criterion15d_thePlannerNamesTheWorkingDayInEachCitysOwnZone` |
| 15e | The planner converts from a selected saved city: the wall-clock reading stays, the instant moves, the source date follows that city, and every difference is measured from it | `criterion15e_thePlannerConvertsFromAChosenSourceCity` |
| 15f | The planner recommends the quarter-hour slot that puts the most cities inside the working day, and the recommendation moves the whole plan to it | `criterion15f_theBestOverlapIsRecommendedAndAppliedToThePlan` |
| 15g | When no slot puts at least two cities inside working hours, the planner states `NO OVERLAP` rather than inventing a recommendation | `criterion15g_noWorkingHoursOverlapIsStatedRatherThanInvented` |

**Grid contract (15b/15c, from `MeetingTimeTest`):** the first slot is 00:00, the last is 23:45,
every minute of the day snaps to a 15-minute slot within one step, and stepping the slider
always advances every city by exactly one step — including across midnight.

**Phase 2 — home-screen widget**

| # | Criterion | Test |
| --- | --- | --- |
| 16a | The app ships a home-screen widget that is declared the way Android requires (exported provider, provider info, `APPWIDGET_UPDATE`, a resizable 4×4 default footprint, the 30-minute backstop, a private ticker receiver) and reads the same store the screens read | `criterion16a_theWidgetIsDeclaredAndReadsTheSavedCitiesFromDataStore` |
| 16b | Every saved city appears with its own current time, in the app-wide format and with the user's labels; cities across the date line say which day they are on; past the row budget the rest are counted, never dropped; and a row always equals the card for the same city | `criterion16b_theWidgetListsTheSavedCitiesWithTheirTimesAndCountsTheRest` |
| 16c | The card is drawn — the device's own clock as the anchor, the cities below it, `+N more`, the sun/moon per city — the header and count line open the app, and the palette comes from the app's tokens resolved against the **device's** light/dark setting | `criterion16c_theCardDrawsTheCitiesAndOpensTheApp` |
| 16d | The widget is kept up to date: an alarm on every minute boundary while a widget exists, nothing when there is none, and an immediate redraw whenever the saved cities, labels or time format change | `criterion16d_theWidgetRefreshesWithTheClockAndWithTheSavedCities` |
| 16e | A widget row is a deep link: tapping a city opens the app on that city's detail screen, makes it active, and consumes the pending id once so a recomposition cannot replay it; back still returns to the city list | `criterion16e_aWidgetTapOpensThatCitysDetailScreen` |

**Phase 2 — integration, accessibility and polish**

| # | Criterion | Test |
| --- | --- | --- |
| 17a | The theme picker is wired into the running app: choosing a theme in the shell repaints the whole shell through `MainActivity`'s own wiring and is written back through the repository | `criterion17a_theAppearanceChosenInTheShellRepaintsTheApp` |
| 17b | Every control phase 2 added is at least 48 dp tall, and the icon-only controls — whose size the app sets — are 48 dp in both directions | `criterion17b_everyPhaseTwoControlMeetsTheTouchTargetGuideline` |
| 17c | The phase 2 surfaces are structured for assistive technology: titles and section headers are headings, the label dialog announces itself as a pane and names its field after the city, and a city card announces its difference | `criterion17c_thePhaseTwoSurfacesAreStructuredForAssistiveTechnology` |

**v2.1 — Material You, reordering, shortcuts, intro and widget changes**

The 2.1 features are covered by their own suites rather than by new numbered acceptance
criteria: they extend surfaces that already have criteria, and a per-suite list points at the
failing behaviour more precisely than a re-numbered criterion would.

| Area | Criterion | Suite |
| --- | --- | --- |
| Material You | The fourth theme is persisted, maps to `SAVED_APP_THEME_MATERIAL_YOU = 5`, resolves to a Material 3 scheme on API 31+, and to the static light/dark fallback below it; the three older ids are unchanged | `ClockwiseThemeTest`, `ClockwisePaletteTest`, `UserPreferencesTest` |
| Reorder persistence | Room stores a dense 0-based order, a reorder replaces the whole order rather than appending, removing a city closes the gap, duplicate ids cannot create duplicate rows, and the observable order follows every write | `SavedCityDaoTest` |
| Reorder reconciliation | `reorderCities` sanitises user input, writes both stores in one call, is a no-op for an unchanged order, and seeds Room from DataStore on an upgrade; v2 -> v3 creates `saved_cities` | `ClockRepositoryTest`, `ClockwiseDatabaseMigrationTest` |
| Drag and accessibility reorder | Dragging down persists the new order; Move up / Move down produce the same order as a drag; the first card has no Move up, the last no Move down, a one-city list has neither; the active card is `selected`; every card exposes its name and time | `ClocksScreenReorderTest` |
| Launcher shortcuts | Three static actions are declared in order and target `MainActivity`; saved cities become dynamic shortcuts in saved order, with labels, dropping unknown/duplicate ids; the budget leaves room for the static actions; the intent is one-shot and carries the usage id | `CityShortcutsTest`, `LauncherShortcutsDeclarationTest` |
| Shortcut updates | The mirror publishes at start, ignores theme/format changes, republishes on add and label change, publishes a repeated snapshot once, and survives a failed publication | `ShortcutMirrorTest` |
| First-launch intro | The intro explains the app in one page, has exactly one primary action that dismisses it, and its footnote points at Settings for replay; the gate is skipped for upgraders and shown for a fresh install | `OnboardingScreenTest` |
| Widget card and sizes | Five responsive breakpoints including `180×110`, the row budget still trims rather than drops, and the declared provider is resizable below its default footprint | `WidgetGeometryTest`, `ClockwiseWidgetTest`, `AcceptanceCriteriaTest` |
| Battery-aware ticking | One-minute `setAndAllowWhileIdle` while interactive, five-minute inexact `set` while the screen is off, immediate redraw on screen-on, and redraw on time/time-zone/date changes | `WidgetTickerTest` |
| Screenshots | The intro, Material You settings sheet and the Material You fallback render are recorded alongside the existing per-theme renders | `LayoutScreenshotTest` |

Three further tests live in `ClockwiseNavigationTest`, because they are about the flow rather
than about a surface: `theClocksScreenOpensTheMeetingPlannerAndBackReturnsToTheList`,
`thePlannerWithNothingSavedOffersTheCitySearch`, and `theSettingsActionOpensTheAppearanceSheet`.

**3.1.0 — the comparison switch, the time input and the alarm sound**

| # | Criterion | Test |
| --- | --- | --- |
| 18a | Settings offers `Compared with your local time` as a switch, on by default; turning it off removes every card's difference line and turning it on restores it | `criterion18a_theComparisonSwitchHidesTheDifferenceOnTheCards` |
| 18b | The alarm editor shows hour and minute wheels under every non-Material-You theme and the platform clock dial under Material You; no keypad entry exists in either | `criterion18b_theWheelIsTheDefaultPickerAndMaterialYouAsksForTheDial` |
| 18c | A sound can be previewed, chosen and saved with the alarm, the row then names it, and a preview never outlives the list | `criterion18c_aSoundIsPreviewedChosenAndSavedWithTheAlarm` |

The alarm feature itself is covered by `AlarmScheduleTest` (next trigger, repeat masks, DST and
one-shot behaviour), `RoomAlarmRepositoryTest` (store-then-arm, sound round-trip),
`AlarmDaoTest`, `ClockwiseDatabaseMigrationTest` (3 → 4 and 4 → 5), `AlarmScreenTest` and
`AlarmSoundCatalogTest` (default entry, fallback, channel per sound). Two more tests pin the
theme retirement: `UserPreferencesTest` and `ClockRepositoryTest` assert that the stored True Dark
and High Contrast names, and the legacy `THEME_MODE_DARK`, read as the default.

**4.0.0 — Phase 1: the timer, the screen saver and the local information screens**

The Phase 1 surfaces are covered by their own suites rather than by renumbered criteria, for the
same reason the v2.1 features were: each new surface has one suite that names the failing
behaviour more precisely than a numbered criterion would.

| Area | Criterion | Suite |
| --- | --- | --- |
| Timer arithmetic | A running timer derives its remainder from its end instant and never reports negative time; a paused one keeps its frozen remainder; the clock shows hours only when hours are left and rounds a part-second up to `00:01`; the summary reads in the units the user chose | `CountdownTimerTest` |
| Timer storage and arming | Start stores the end instant and arms it; pause freezes the remainder, disarms and clears the countdown; resume counts from the frozen remainder; finish writes zero and rings; reset clears the row and both notifications; a future timer is re-armed unchanged, a missed one rings immediately, a paused one is not re-armed; a duration below one second is raised to the minimum | `RoomTimerRepositoryTest` |
| Timer screen | The wheels start the chosen duration (default `00:05:00`); a running timer shows the countdown and pauses; a paused one offers Resume; a finished one explains itself and offers a new one; the exact-alarm notice appears only when the platform refuses exact triggers | `TimerPanelTest` |
| Timer segment | `ALARMS · TIMER` replaces the alarm list with the countdown setup, and Start reports the chosen duration | `AlarmScreenTest` |
| Timer migration | 5 → 6 creates an empty `timer` table, a paused row round-trips, and an existing alarm keeps its label and sound | `ClockwiseDatabaseMigrationTest` |
| Settings and information screens | The five Settings groups are present and every `EXTRAS` row and the Studio row call their callback; the four alarm rows spell out each missing grant (`ALLOW` twice, `OFF`, `CHECK`); Privacy states zero collection and goes back; Help names exact alarms, DND and the screen saver; Studio says plainly that nothing can be bought; the screen saver shows the time and exits on a tap | `PhaseOneScreenTest` |
| Offline guardrail | The merged manifest requests neither `INTERNET` nor `ACCESS_NETWORK_STATE`; no billing, ads, analytics or crash-reporting class is on the classpath; the four alarm permissions are still declared | `OfflineGuardrailTest` |
| Screenshots | The timer in setup and running, the screen saver, the privacy policy and the Studio placeholder are recorded at 360 dp | `LayoutScreenshotTest` |

The alarm readiness reader (`AlarmReadiness`, `readAlarmReadiness`) is exercised through
`PhaseOneScreenTest`'s status rows; the platform queries behind it are hardware facts and are
listed in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

**4.1.0 — alarm refinements and gestures**

The alarm pass keeps the same pattern: each behaviour is named by the suite that would fail if it
broke, rather than by a new numbered criterion.

| Area | Criterion | Suite |
| --- | --- | --- |
| Compact card and AM/PM | One card per alarm, the switch is the only write without the editor, and the period is part of the time's `AnnotatedString` | `AlarmScreenTest`, `ClockTextTest` |
| Snooze policy | `ALLOWED`/`ONCE`/`NONE` is enforced by the screen, the notification and the repository; the suggestion never overrides the user | `SnoozePolicyTest`, `AlarmScreenTest`, `RoomAlarmRepositoryTest` |
| Alarm order | The chosen `sortOrder` wins over time of day, `reorder` rewrites every position in one transaction, and a new alarm appends | `AlarmDaoTest`, `RoomAlarmRepositoryTest` |
| Reorder gesture | A long-press vertical drag persists the new order once; Move up / Move down do the same without a drag | `AlarmScreenTest`, `ClocksScreenReorderTest` |
| Swipe and Undo | A right-to-left swipe deletes an alarm or city; Undo restores the exact alarm row or the city's previous order and active state | `AlarmScreenTest`, `ClocksScreenReorderTest`, `ClockwiseNavigationTest`, `ClockRepositoryTest` |
| Migration | 7 → 8 adds `sortOrder`, backfills it by time of day, and keeps the snooze state | `ClockwiseDatabaseMigrationTest` |

---

## 12. Verification strategy

### 12.1 Four layers

| Layer | Tool | Command | What it proves |
| --- | --- | --- | --- |
| Unit / domain | JUnit 4 | `testDebugUnitTest` | Timezone maths, DST, solar position, projection, repository rules, directory |
| Screenshot / layout | Roborazzi + Robolectric (JVM, no emulator) | `testDebugUnitTest` | Real Compose rendering at a fixed 360 dp device size |
| End-to-end | Compose UI testing on a physical device | `connectedDebugAndroidTest` | Install, launch, real DataStore, real system clock and timezone, real rendering |
| Widget | Glance's own unit harness | `testDebugUnitTest` | The widget's tree, text, responsive row budget, colour resolution and click actions. It has **no** screenshot: Glance draws through `RemoteViews`, which Roborazzi cannot render |

**Decision record:** Roborazzi renders the real Compose tree on the JVM, so a screenshot
regression is caught without an emulator. `roborazzi.test.record=true` is set for all test
tasks, so `testDebugUnitTest` refreshes the snapshots in `app/build/screenshots/`.

### 12.2 Current results

**JVM suite: 596 tests, 0 failures, 54 suites** (v1.0 baseline plus M0 catalog, M1 solar, M2 map time-travel, M3 planner converter/recommendation, M4 widget rebuild, M5 hardening, the v2.1 Material You, reorder, shortcut, intro and ticker suites, the v3.0 rebrand, the 3.1.0 alarms, comparison switch, wheel time picker and alarm sound, the 4.0.0 timer, screen saver, information screens and offline guardrail, the 4.1.0 alarm refinements, clock-text split, snooze policy, alarm reorder and swipe-to-delete with Undo, and the 1.0.10 rebrand and module split).

| Test file | Tests |
| --- | --- |
| `CityDirectoryTest` | 11 |
| `UserPreferencesTest` | 23 |
| `ClockRepositoryTest` | 37 |
| `CityDaoTest` | 5 |
| `SavedCityDaoTest` | 8 |
| `SolarDaoTest` | 5 |
| `AlarmDaoTest` | 14 |
| `ClockwiseDatabaseMigrationTest` | 6 |
| `SolarRepositoryTest` | 6 |
| `SolarCachePolicyTest` | 5 |
| `RoomAlarmRepositoryTest` | 18 |
| `RoomTimerRepositoryTest` | 10 |
| `EquirectangularProjectionTest` | 5 |
| `SolarPositionTest` | 12 |
| `SolarEphemerisTest` | 6 |
| `SunriseSunsetCalculatorTest` | 10 |
| `WorldLandmassesTest` | 5 |
| `CityTimeFormatterTest` | 9 |
| `ClockTextTest` | 4 |
| `TimeDifferenceTest` | 6 |
| `MeetingTimeTest` | 14 |
| `SecondTickerTest` | 2 |
| `CountdownTimerTest` | 8 |
| `AlarmScheduleTest` | 11 |
| `SnoozePolicyTest` | 8 |
| `AlarmSoundCatalogTest` | 5 |
| `ClockwisePaletteTest` | 20 |
| `ClockwiseThemeTest` | 6 |
| `CityShortcutsTest` | 9 |
| `LauncherShortcutsDeclarationTest` | 3 |
| `ShortcutMirrorTest` | 6 |
| `OfflineGuardrailTest` | 3 |
| `AcceptanceCriteriaTest` | 45 |
| `AppIconTest` | 3 |
| `LayoutScreenshotTest` | 53 |
| `PlannerStateMapperTest` | 18 |
| `MeetingPlannerScreenTest` | 15 |
| `AlarmScreenTest` | 17 |
| `ClockwiseNavigationTest` | 14 |
| `CitySearchContentTest` | 5 |
| `ClocksScreenReorderTest` | 9 |
| `OnboardingScreenTest` | 3 |
| `WorldMapScreenTest` | 17 |
| `MapStateMapperTest` | 16 |
| `WidgetStateMapperTest` | 11 |
| `WidgetGeometryTest` | 11 |
| `ClockwiseWidgetTest` | 16 |
| `WidgetMirrorTest` | 7 |
| `WidgetTickerTest` | 6 |
| `WidgetPreviewPublisherTest` | 6 |
| `PhaseOneScreenTest` | 6 |
| `TimerPanelTest` | 5 |
| **Total** | **583** |

**Screenshot artefacts: 54**, all 360 dp, in `app/build/screenshots/`:

`app_icon.png`, `app_shell_360dp.png`,
`clocks_screen_360dp.png`, `clocks_empty_360dp.png`, `clocks_screen_comparison_off_360dp.png`,
`clocks_screen_labelled_360dp.png`, `clocks_screen_labelled_cool_slate_360dp.png`,
`clocks_screen_12h_360dp.png`, `clocks_screen_large_font_360dp.png`,
`clocks_screen_warm_sepia_360dp.png`, `clocks_screen_cool_slate_360dp.png`,
`clocks_screen_material_you_360dp.png`,
`city_detail_360dp.png`, `city_detail_12h_360dp.png`, `city_detail_cool_slate_360dp.png`,
`city_detail_labelled_360dp.png`, `city_label_dialog_360dp.png`,
`city_label_dialog_cool_slate_360dp.png`,
`planner_screen_360dp.png`, `planner_screen_cool_slate_360dp.png`, `planner_screen_12h_360dp.png`,
`planner_screen_source_360dp.png`, `planner_empty_360dp.png`,
`map_screen_360dp.png`, `map_screen_london_360dp.png`, `map_screen_summer_360dp.png`,
`map_screen_empty_360dp.png`, `map_screen_warm_sepia_360dp.png`, `map_screen_cool_slate_360dp.png`,
`map_screen_labelled_360dp.png`, `map_screen_time_travel_360dp.png`,
`search_sheet_360dp.png`, `search_sheet_empty_360dp.png`,
`alarms_screen_360dp.png`, `alarms_empty_360dp.png`, `alarms_exact_notice_360dp.png`,
`alarms_screen_cool_slate_360dp.png`, `alarm_editor_new_360dp.png`,
`alarm_editor_existing_360dp.png`, `alarm_editor_dial_360dp.png`, `alarm_editor_critical_360dp.png`,
`alarm_ringing_360dp.png`, `alarm_ringing_critical_360dp.png`,
`settings_sheet_360dp.png`, `settings_sheet_comparison_off_360dp.png`,
`settings_sheet_warm_sepia_360dp.png`, `settings_sheet_cool_slate_360dp.png`,
`settings_sheet_material_you_360dp.png`,
`onboarding_360dp.png`,
`timer_setup_360dp.png`, `timer_running_360dp.png`, `screen_saver_360dp.png`,
`privacy_policy_360dp.png`, `chronos_studio_360dp.png`.

**Every theme has a list render and an appearance-sheet render:**
Light Monochrome is `clocks_screen_360dp.png` / `settings_sheet_360dp.png`, and Warm Sepia,
Cool Slate and Material You are the `*_warm_sepia_*`, `*_cool_slate_*` and `*_material_you_*`
pairs. `clocks_screen_material_you_360dp.png` is the **static fallback** rather than a wallpaper
scheme: Robolectric cannot produce a dynamic Material You scheme, which is exactly why the
fallback exists and is worth a render of its own. The True Dark and High Contrast renders were
deleted with their palettes in 3.1.0, and the dark-palette variants of the detail, map, dialog,
planner and alarm screens were replaced by Cool Slate ones, so the second-palette review still
has a full set.

The light-mode artefacts are byte-for-byte what phase 1 produced: the light tokens and the
light `mapNight` wash are unchanged, so nothing was re-styled by adding the dark palette.
Phase 2 stage 2 changed the city card's internal layout (three lines instead of a two-column
grid), which is visible in every clocks render — deliberately, and described in
[section 5.3](#53-clocks-screen). Phase 2 stage 3 added the `PLAN A MEETING` action to the
clocks renders and the three planner renders. The integration pass added the four renders that
pair the phase 2 features with the dark theme and the 12-hour format, and re-recorded every
other one: the segmented control grew to a 48 dp target and the small circular buttons now sit
in 48 dp slots ([section 5.12](#512-phase-2-integration-accessibility-and-polish)). The
palette-theme pass added the per-theme renders above and re-recorded the two map renders,
whose `IN NIGHT` legend swatch gained an outline — on the darker themes the swatch had
been a black dot on a black background. The M2 pass re-recorded every map render because the
time-travel control and solar line are new, and added `map_screen_time_travel_360dp.png`
(03:00 on the sample day). The M3 pass re-recorded all four existing planner renders because
the source row and recommendation action are new, and added
`planner_screen_source_360dp.png` (12:00 read in Tokyo rather than on the device). The M5 pass
added `clocks_screen_large_font_360dp.png`, the Clocks screen rendered at a 1.5× font scale;
the default-scale renders are unchanged. The v2.1 pass added
`clocks_screen_material_you_360dp.png`, `settings_sheet_material_you_360dp.png` and
`onboarding_360dp.png`. The Clocks renders also gained the decorative drag grip on each card;
the other surfaces are unchanged. The v3.0 rebrand re-recorded **only** `app_icon.png`: it now
composes the royal-blue background layer, clips the mark with a circular mask and writes the result,
so the PNG shows what a home screen shows rather than the off-white monochrome mark. No screen
render changed, because no colour, type or shape token changed — the rebrand is the package and the
launcher icon, not the UI.

**The 3.1.0 pass** added the alarms list (filled, empty and exact-alarm-notice), the editor with
the wheels, the same editor under Material You (dial), the ringing screen, the settings sheet with
the switch off, and a Clocks render with the comparison off. It also re-recorded the four per-theme
Clocks/tile renders and every render that had been on the retired dark palettes. Two renders are
the evidence for the design rules: `alarm_editor_new_360dp.png` shows the wheels and no keypad, and
`alarm_editor_dial_360dp.png` shows the platform dial under Material You, both at the same height
and reading position. The screenshot for a comparison-off list also proves the difference line is
gone without the card changing height.

**The 4.0.0 Phase 1 pass** added five renders: `timer_setup_360dp.png` (three wheels reading
`00:05:00`, the preset row and one primary action), `timer_running_360dp.png` (the countdown at
`02:05` with its progress hairline), `screen_saver_360dp.png` (black, one time and one date),
`privacy_policy_360dp.png` and `chronos_studio_360dp.png`. No existing render changed: Phase 1
added surfaces rather than restyling one, and the Settings sheet's new groups are reviewed in the
sheet renders already in the set.

**The widget has no Roborazzi screenshot.** Glance draws through `RemoteViews`, which Roborazzi
cannot render, so the M4 widget is asserted by its own harness instead: `ClockwiseWidgetTest`
composes the real card at the declared sizes, and `WidgetGeometryTest` / `WidgetStateMapperTest`
pin the row budget and the shared readings. `drawable-nodpi/widget_preview.png` is a 500×500
static image shipped for pre-Android-15 widget pickers; it is an app asset, not a test artefact,
and is not part of `app/build/screenshots/`.

**The 4.1.0 pass** re-recorded every alarm render for the compact card and AM/PM, and added
`alarm_ringing_critical_360dp.png` (no Snooze, the reason spelled out) and
`alarm_editor_critical_360dp.png` (the medication suggestion in the editor). The ringing renders
now use `AlarmRingingTheme`, so they show the dark wallpaper scheme the activity actually paints
rather than the light theme around it.

**Device suite: 2 tests, 0 failures** (`connectedDebugAndroidTest`).

**Device run:** see [section 14.4](#144-device-verification) and
[section 14.5](#145-stage-4--dark-theme). The 4.0.0 build is installed on the Redmi Turbo 4 but its
on-device checks have **not** been run — the user asked for the install without testing; they are
listed in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

### 12.3 Reference values used by the tests

Screenshot and unit tests pin these instants:

| Name | Instant |
| --- | --- |
| `referenceInstant` (winter) | `2024-01-20T16:40:00Z` |
| `summerInstant` | `2024-07-20T04:40:00Z` |
| Device zone used in previews/tests | `Asia/Yangon` |

Expected offsets (winter): Tokyo `01:40 UTC+9 Sun 21 Jan`, Sydney `03:40 UTC+11`,
Los Angeles `08:40 UTC-8 Sat 20 Jan`, New York `11:40 UTC-5`, London `16:40 UTC+0`.
Expected offsets (summer): Los Angeles `UTC-7`, New York `UTC-4`, London `UTC+1`,
Sydney `UTC+10`. Mumbai is `UTC+5:30`.

**Map time-travel reference.** Travelling to `03:00` on the sample day (20 Jan 2024,
`Asia/Yangon`) means `2024-01-19T20:30:00Z`: Los Angeles reads `12:30 Fri 19 Jan` and Tokyo
`05:30 Sat 20 Jan`. At `23:59` the device is still on 20 Jan while Tokyo has reached 21 Jan,
which is why the map requests solar events for the **city's** date, not the device's.

**Planner converter reference.** The default five-city planner opens at `09:00` in Yangon.
Reading that same wall-clock `09:00` in Tokyo instead means `2024-01-21T00:00:00Z`: Tokyo reads
`09:00 Sun`, Los Angeles `16:00 Sat`, and the source offset is `UTC+9`. The default five-city
recommendation is `06:30` (Tokyo, Sydney and Los Angeles at work); a Tokyo + New York planner
has no shared working hour and returns `NO OVERLAP`.

**Widget reference values.** The widget tests use the same winter instant and the same device
zone. At the 250×250 default size the five default cities fit, so Tokyo and Sydney are the only
rows on another day and the day/night icons follow each city's own local hour. At 220×110 the
budget is one city plus `+4 more cities`; with eight saved cities at 250×250 it is five rows plus
`+3 more cities`, because the count line costs a city's row. The day/night split is three day
(Los Angeles, New York, London) and two night (Tokyo, Sydney). Three harness details are
load-bearing: `runGlanceAppWidgetUnitTest` must be given `setContext(context)` and
`setAppWidgetSize(...)` before `provideComposable`, Glance's `hasText` /
`hasContentDescription` are substring matchers (the tests use `hasTextEqualTo` /
`hasContentDescriptionEqualTo` where the wording itself is checked, and `onAllNodes(...)`
where a value such as `Sun` legitimately appears twice), and the appwidget testing
`assertHasStartActivityClickAction(intent)` matches with `Intent.filterEquals`, which ignores
extras — so the city extra is asserted separately against `cityDetailIntent`.

**JVM text metrics are not real.** Robolectric's stub font measures text at close to zero width,
so a test can prove a control is at least 48 dp *tall* but not that it is 48 dp *wide* when its
width comes from its label. Criterion 17b asserts widths only for the icon-only controls, whose
size the app sets; the PNGs are the evidence for the rest.

**A bottom sheet is a window.** Touch injection does not cross into `ModalBottomSheet` under the
JVM renderer: `performClick()` on one of its options does nothing, while
`performSemanticsAction(SemanticsActions.OnClick)` reaches the callback. `criterion17a` uses the
action and says why.

---

## 13. Build and run

**IMPLEMENTED**

```powershell
# Build the debug APK
.\gradlew.bat assembleDebug

# Build the release APK (signed when keystore.properties exists)
.\gradlew.bat assembleRelease

# Run the JVM suite (also refreshes screenshots in app/build/screenshots/)
.\gradlew.bat testDebugUnitTest

# Lint
.\gradlew.bat lintDebug

# Full clean verification
.\gradlew.bat clean assembleDebug assembleRelease testDebugUnitTest lintDebug

# Verify the shipped permission set (expect no INTERNET / ACCESS_NETWORK_STATE)
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe" dump permissions app\build\outputs\apk\release\app-release.apk

# Install on the connected device
.\gradlew.bat installDebug

# Run the on-device end-to-end suite
.\gradlew.bat connectedDebugAndroidTest
```

Known benign build noise, safe to ignore:

- `Unable to strip libandroidx.graphics.path.so, libdatastore_shared_counter.so`
- On PowerShell, Gradle's `e:` diagnostics surface as `NativeCommandError`; filter with
  `Select-String -Pattern "^e: |FAILED|BUILD"`.

**Known baseline (v3.0.0, 2026-10-27):** `assembleDebug`, `assembleRelease`, `testDebugUnitTest`
and `lintDebug` pass. The JVM suite ends at **438 tests, 0 failures, 40 suites, 41 screenshots**;
`lintDebug` reports **0 errors, 30 warnings, 0 information** — 19 dependency-update notes, 4 unused
resources, 3 plurals candidates, 1 `VectorPath`, 1 `OldTargetApi`, 1 `RedundantLabel` and 1
`ObsoleteSdkInt` on `mipmap-anydpi-v26`, which is a false positive: an `<adaptive-icon>` must live
behind `-v26`, and moving it to `mipmap-anydpi` re-introduces `IconXmlAndPng`. The merged release
manifest declared **no runtime permission at all** (only the explicit removal of
`ACCESS_NETWORK_STATE`), and therefore **no `INTERNET` and no `ACCESS_NETWORK_STATE`**. The APK is
`com.pj95.clockwise` / `versionCode 4` / `versionName 3.0.0`; the debug APK is **14.09 MiB** and
the signed release APK is **9.72 MiB** (the release grew ~30 KB over 2.1.0, which is the five legacy
icon PNG pairs; the debug growth is dex/multi-dex layout, not shipped functionality). Re-check both
after each milestone. The release certificate is unchanged:
SHA-256 `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`. Re-run this
command after any change; `testReleaseUnitTest` is not part of it and cannot run the Compose
tests, because `ui-test-manifest` is a `debugImplementation` only.

**Known baseline (v4.1.0, 2026-10-31):** `assembleDebug`, `assembleRelease`, `testDebugUnitTest`
and `lintDebug` pass. The JVM suite ends at **583 tests, 0 failures, 52 suites, 54 screenshots**;
`lintDebug` reports **0 errors, 33 warnings, 3 information** (the three are
`AutoboxingStateCreation` on `AlarmEditorSheet.kt`'s hour, minute and sound-index state, kept
deliberately; the warning count is one below the alarm-only build because `ic_delete` is now used
by the swipe background). The merged release manifest carries `USE_EXACT_ALARM` (plus legacy
`SCHEDULE_EXACT_ALARM` capped at API 32), `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`,
`VIBRATE`, `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PLAYBACK` and the dynamic-receiver permission — and **no `INTERNET` and
no `ACCESS_NETWORK_STATE`**. The three foreground/wake permissions are new in 4.1.0 and are what
`AlarmRingingService` requires. The APK is `com.pj95.clockwise` / `versionCode 7` /
`versionName 4.1.0`; the debug APK is **14.57 MiB** and the signed release APK is **10.00 MiB**.
The release certificate is unchanged:
SHA-256 `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`.

**Known baseline (v4.0.0, 2026-10-28):** `assembleDebug`, `assembleRelease`, `testDebugUnitTest`
and `lintDebug` passed. The JVM suite ended at **542 tests, 0 failures, 50 suites, 52
screenshots**; `lintDebug` reported **0 errors, 34 warnings, 0 information**. The merged release
manifest carried `USE_EXACT_ALARM` (plus legacy `SCHEDULE_EXACT_ALARM` capped at API 32),
`RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `VIBRATE`, `USE_FULL_SCREEN_INTENT` and the
dynamic-receiver permission — and **no `INTERNET` and no `ACCESS_NETWORK_STATE`**. Phase 1 added
the timer and the screen saver without a new platform permission. The APK was
`com.pj95.clockwise` / `versionCode 6` / `versionName 4.0.0`; the debug APK was **13.76 MiB** and
the signed release APK was **9.96 MiB**. The release certificate is unchanged:
SHA-256 `2752c30168f68b53911db395d6ada42e2e5a3843e16cf200dd403552afbb50ad`.

---

## 14. Implementation log

### 14.1 Stage 0 — scaffold

Gradle wrapper, `local.properties`, `.gitignore`, manifest with
`windowSoftInputMode="adjustResize"`, `ClockwiseApp`, manual `AppContainer`,
`saved_cities.proto` code generation.

### 14.2 Stage 1 — visual foundation

Full monochrome `lightColorScheme`; Inter `FontFamily` plus Material 3 type roles and
`ClockwiseTextStyles`; `ClockwiseShapes`; `CityCard`; `CircleIconButton`;
`ClockwiseBottomBar`; adaptive monochrome app icon; `ClockwiseRoot` shell;
`MainActivity` with `enableEdgeToEdge()`; `City` + `CityDirectory`.

### 14.3 Stage 2 — working clocks

`SecondTicker`; `CityTimeFormatter` / `CityTime`; `ClockwiseViewModel` +
`ClockwiseUiState` + `buildClockwiseUiState`; `CitySearchSheet` / `CitySearchContent`;
`ClockwiseSegmentedControl`; `CityDetailScreen`; `ClocksScreen`; `ClockRepository`
(with `PrefsError` → snackbar); `UserPreferences` / `seededWithDefaults()`; `Routes` /
`NavHost`; `PreviewSamples`.

### 14.4 Stage 3 — world map, polish and verification

Added `domain/map/*` and `ui/components/WorldMapCanvas.kt` +
`ui/screens/WorldMapScreen.kt`; deleted `MapPlaceholder`; `Routes.MAP` now renders the real
map; `CityCardModel` gained `latitude` / `longitude`, populated by `buildClockwiseUiState`.

Two real bugs were found and fixed while verifying the map:

1. Sidereal time was used in hours instead of degrees — fixed by multiplying by `15.0`.
2. `atan2` was used in `terminatorLatitude` — replaced with `atan`.

**Device verification (Xiaomi `24129RT7CC`, codename `rodin`, Android 16 / API 36,
1220×2712 px at 520 dpi = 375 dp wide).**

| Check | Result |
| --- | --- |
| `installDebug` | Installed successfully |
| Launch | Starts, gains focus, no crash in `logcat` (`-b crash` empty) |
| Clocks screen correctness | **Correct.** Yangon `00:05 Oct 17` (UTC+6:30) → Tokyo `02:35 UTC+9`, Sydney `04:35 UTC+11` (AEDT, correct DST), Los Angeles `10:35 Fri UTC-7` (PDT, correct DST), New York `13:35 Fri UTC-4` (EDT), London `18:35 Fri UTC+1` (BST). Sun/moon icons match the 06:00–17:59 rule. |
| Layout width | 375 dp — wider than the 360 dp target, so no clipping risk |
| Map tab rendering | **Verified.** Semantics tree shows the canvas, the active meridian, all five markers at correct projected positions, and the selected-city panel. |
| Marker tap → active city | **Verified.** Tapping the London marker flips `Selected` from `los-angeles` to `london`. |

**Limitation:** `adb shell input tap` and `input keyevent` are blocked on this device
(`java.lang.SecurityException: Injecting input events requires the caller … to have the
INJECT_EVENTS permission`) because Xiaomi HyperOS gates simulated input behind a
user-enabled developer setting. Scripted taps via `adb` are therefore not possible; the
Compose UI tests drive the app through the instrumentation instead, which is permitted.

### 14.5 Stage 4 — dark theme (phase 2)

Stage 4 of the project, stage 1 of phase 2.

**Added:** the `ClockwisePalette` data class with a light and a dark instance and the
`LocalClockwisePalette` composition local; `ClockwiseColors` reworked into Material-style
composable getters so no call site had to change; `ThemeMode` plus its proto mapping; the
`SavedThemeMode` enum and `theme_mode = 5` in the schema; `ClockRepository.setThemeMode`;
`themeMode` threaded through `ClockwiseUiState` and `buildClockwiseUiState`;
`ClockwiseTheme(themeMode)` with `resolveDarkTheme`; `ic_settings`; `SettingsSheet` /
`SettingsContent`; the gear action on the Clocks screen; `values-night` launch theme and
window background; `SystemBarAppearance`.

**Changed:** `WorldMapCanvas`'s `DrawScope` steps now take a `ClockwisePalette` parameter
(draw code is not composable), and the map's night wash moved from a private `NIGHT_ALPHA`
constant onto the `mapNight` token. `MainActivity` applies the theme so the palette is
correct on the first composition of every screen.

**Device verification — dark theme (Xiaomi `24129RT7CC`, Android 16 / API 36, 375 dp wide).**

The device was put into night mode with `adb shell cmd uimode night yes`. Because the stored
mode was `System` (no `theme_mode` field on the wire — proto3 omits the zero value, which is
the `System` value), the app resolved to the dark palette with no further interaction:

| Check | Result |
| --- | --- |
| Dark palette applied | **Correct.** Near-black background, `#1A1A1A` rows, light text |
| Inverted selected state in dark | **Correct.** Saved-city rows show a *light* circle with a *dark* check — the mirror of the light theme |
| Status bar icons | **Correct.** Light icons on the dark app surface |
| Search sheet in dark | **Correct.** Dark sheet, dark field with light outline, muted offset text |
| Settings sheet on device | **Correct.** Opened from the gear; `System` / `Light` / `Dark` all rendered; selection state visible; choosing a mode repainted the app immediately |
| Data integrity | The user's own saved cities, order and active city were untouched throughout |

**Note on method.** The system night mode was borrowed for this check and restored to
`auto` afterwards. The app's own stored preferences were backed up first
(`app/build/device/prefs_backup/`) and never modified — the theme was driven through the
settings sheet and the system setting, exactly as a user would.

**Not verified on device:** the explicit `Dark`-on-a-light-device case that
`SystemBarAppearance` exists for. Isolating it needs the stored mode set to `Dark` while the
device is in light mode; the reasoning is recorded in
[section 7.1](#71-colour) and the fix is exercised by every screenshot test.

### 14.6 Stage 5 — time difference and city labels (phase 2)

Stage 2 of phase 2, the project's fifth stage.

**Added:** `domain/time/TimeDifference.kt` (an inline value class over a signed minute count,
with `between(cityZone, referenceZone, instant)` built on `ZoneRules.getOffset`),
`ui/components/TimeDifferenceLabel.kt`, `ui/screens/CityLabelDialog.kt`, `ic_edit`, and the
fetched-string resources for both features.

**Changed:**

- `CityCard` — a label and a difference per card, and the card body rebuilt as three
  independent lines (`offset | weekday`, `name | time`, `region | difference`). A shared
  two-column grid let the difference line's width shrink the column holding the city name,
  which truncated `Tokyo (Head Office)`. Per line, only the item that does not fit gives way.
- `CityDetailScreen` — the `COMPARED WITH YOUR LOCAL TIME` section, the label line and the
  `ADD LABEL` / `EDIT LABEL` action.
- `WorldMapScreen` — pills and the selected-city panel use the labelled name; the panel shows
  the difference.
- `ClockwiseViewModel` / `buildClockwiseUiState` — `cityLabels` threaded through as the
  single source of both labels and differences.
- `ClockRepository` — `setCityLabel`; `UserPreferences` — `cityLabels` plus pruning;
  `saved_cities.proto` — `city_labels = 6`.
- `ClockwiseRoot` — the shell became an explicit `Box` stack, because the label dialog is
  drawn in the app's own composition rather than in a platform dialog window
  ([section 5.8](#58-city-label-dialog)).
- `Type.kt` — `CityDifference`, `DetailCityLabel`, `DetailDifference`, `MapCityDifference`.
- `string.xml` — the difference wording, the label dialog, and two accessibility strings.

**A finding worth keeping.** A text field inside a Compose **dialog window** never reaches an
idle state under Robolectric: `setContent` on a bare `AlertDialog` containing an
`OutlinedTextField` fails with `Compose did not get idle after 50383 attempts in 60 SECONDS`,
while the identical `OutlinedTextField` in the main window is driven normally by
`performTextInput`. This was isolated with a throwaway probe test (a bare `Dialog`, a bare
`AlertDialog`, an `AlertDialog` with a field, and a field in the main window) and it is the
reason the dialog is a modal overlay. The app's bottom sheets avoid the same trap differently —
their **content** composables (`CitySearchContent`, `SettingsContent`) are what the tests
render, never the `ModalBottomSheet` itself.

**Verification.** `assembleDebug` and `testDebugUnitTest` are green at **165 tests, 0
failures**, with 21 screenshots; the three new renders are `clocks_screen_labelled_360dp.png`,
`map_screen_labelled_360dp.png`, `city_detail_labelled_360dp.png` and
`city_label_dialog_360dp.png`. The renders confirmed the layout fixes: `Tokyo (Head Office)`
and `New York (HQ)` print in full at 360 dp, the card still measures 93 dp, and `Save` no
longer wraps onto two lines.

### 14.7 Stage 6 — meeting planner (phase 2)

Stage 3 of phase 2, the project's sixth stage.

**Added:** `domain/time/MeetingTime.kt` (a `MinuteOfDay` value class over minutes since
midnight, validation in the constructor, `snappedToGrid`, `onDate`, `ofInstant`, and the
`isWithinWorkingDay` rule), `ui/PlannerUiState.kt`, `ui/mapping/PlannerStateMapper.kt`,
`ui/components/DaySlider.kt` (the hand-drawn 24-hour control plus `DaySliderTags`), and
`ui/screens/MeetingPlannerScreen.kt`.

**Changed:**

- `Routes` — `PLANNER`; `ClockwiseRoot` — the route, with the slot held in `rememberSaveable`
  and derived from `state.now` until the slider is touched.
- `ClocksScreen` — `onPlanMeetingClick` and the `PLAN A MEETING` action under the section
  header.
- `ClockwiseViewModel` — `deviceZone`, so the planner's route and mapper use the same zone the
  differences are measured against, with no loading corner case.
- `PreviewSamples` — `plannerState(labels)` and `emptyPlannerState()`, so planner renders are
  built by the production mapper.
- `Type.kt` — the five planner styles; `strings.xml` — the planner's wording, the two legend
  items, and three accessibility strings.

**Numbers that had to be right.** The grid is 15 minutes, so a day has 96 slots from 00:00 to
23:45 — the last step stops short of midnight because 24:00 is the next day, and `Compose`
counts the steps *between* the ends, hence `GRID_STEPS = 94`. The slider's touch mapping and its
drawing use the same 24-hour scale while the value range stops at 23:45, which is why the marker
at the far right sits a hair short of the tick for `24`.

**Two daylight-saving corners are handled by `java.time` and pinned by tests.** A slot inside
the spring-forward gap (New York, 02:30 on 10 March 2024) is moved forward by the length of the
gap; a slot the clocks pass through twice (01:30 on 3 November 2024) takes the earlier offset.

**A near-miss caught by the render.** The working-day band was first drawn as a fill of the
whole track height. With the marker resting on the band's start it read as a slider's own
active range — a filled selection from the thumb to 18:00. It is now a rounded pill inset inside
the track, with the hour ticks drawn over it in `border` and the band in `accent`, so the ruler
stays continuous and the band cannot be mistaken for the control's own state.

**Verification.** `assembleDebug` and `testDebugUnitTest` are green at **199 tests, 0 failures**,
20 suites, with 24 screenshots; the three new renders are `planner_screen_360dp.png`,
`planner_screen_dark_360dp.png` and `planner_empty_360dp.png`, and `clocks_screen_360dp.png`
shows the new action row. `criterion15b` sweeps all 96 slots against times computed independently
in the test, and asserts that every step moves every city by exactly one step.

**Not verified on device:** the planner has not been driven on hardware. The slider's touch
mapping, the TalkBack announcement of its `stateDescription`, and the feel of the 15-minute
snapping are all covered on the JVM but not on a real touchscreen (see
[section 15.9](#159-open--the-meeting-planner-has-not-been-touched-on-hardware)).

### 14.8 Stage 7 — home-screen widget (phase 2)

Stage 4 of phase 2, the project's seventh stage.

**Superseded by M4 (2026-10-26):** the fixed six-row budget and Glance 1.1.1 implementation
described below were replaced by the responsive, Glance 1.2.0 rebuild in
[section 14.13](#1413-v2-m4--glance-120-widget-rebuild). This entry remains as the phase 2
history.

**Added:** `ui/WidgetUiState.kt`, `ui/mapping/WidgetStateMapper.kt` (`WIDGET_MAX_ROWS = 6`),
`ui/widget/ClockwiseWidgetContent.kt`, `ui/widget/DayNightColorProvider.kt`,
`widget/ClockwiseWidget.kt`, `widget/ClockwiseWidgetReceiver.kt`, `widget/WidgetTicker.kt`,
`widget/WidgetMirror.kt`, and `res/xml/clockwise_widget_info.xml`.

**Changed:** `AndroidManifest.xml` — the widget receiver (`exported`, the `APPWIDGET_UPDATE`
filter, the `android.appwidget.provider` meta-data) and the private ticker receiver;
`ClockwiseApp.kt` — an application scope with a `CoroutineExceptionHandler` and an idempotent
`startWidgetMirror()`, called only when a widget exists; `strings.xml` — the widget's wording and
the `widget_more_cities` plural; `app/build.gradle.kts` — Glance and its test harness.

**Numbers that had to be right.** Six rows is the budget, and it is a constant rather than a
measurement: `LocalSize` is unreliable below API 31, so the card draws six and counts the rest
with `+N more` from the plural resource. The default footprint is 4×4 cells — 250 dp minimum in
both directions, which is exactly where six rows and the header stop being cramped. The tick
alarm is set on the *next* minute boundary (`nextMinuteBoundary`), not one minute from now, so
the widget's seconds stay aligned with the device clock.

**Three Glance API constraints were found by reading the resolved jars, not the docs.**
`GlanceComposable` is a `@ComposableTargetMarker` rather than a `@Composable` annotation, so a
content composable needs both; `ColorFilter` is `androidx.glance.ColorFilter`, not the Compose
one; and Glance 1.1 has no literal day/night `ColorProvider` — only `ColorProvider(@ColorRes)`
and `ColorProvider(Color)` — and no `letterSpacing` or `overflow` in its `TextStyle`. The first
of those shapes the file signatures, the last three are why `DayNightColorProvider` exists and
why a long city name is clipped by its own weight rather than ellipsised.

**Decision record — the widget follows the device theme, not the app's stored choice.** A home
screen is shared with other apps; a card that ignores the device's light/dark setting looks
broken next to them. The consequence is that an explicit app choice and the widget can disagree,
which is recorded in [section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen).

**Decision record — the row budget, the alarm and the theme are the widget's three judgement
calls,** and each is stated in [section 5.11](#511-home-screen-widget) with the alternative that
was rejected: a measured row budget (unreliable below API 31), WorkManager (15-minute minimum),
`setExactAndAllowWhileIdle` (needs `SCHEDULE_EXACT_ALARM` for no visible gain).

**Verification.** `assembleDebug` and `testDebugUnitTest` are green at **237 tests, 0 failures,
24 suites**. The four new suites are `WidgetStateMapperTest` (12), `ClockwiseWidgetTest` (11,
including the day/night colour resolution and the click action asserted against `MainActivity`),
`WidgetMirrorTest` (7) and `WidgetTickerTest` (4), plus criteria 16a–16d. The widget adds **no
screenshot**: Glance draws through `RemoteViews`, which Roborazzi cannot render, so the card is
asserted through `runGlanceAppWidgetUnitTest` instead.

**Not verified on device:** the widget has never been on a real home screen — see
[section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen).

### 14.9 Stage 8 — integration, accessibility and polish (phase 2)

Stage 5 of phase 2, the project's eighth stage. No screen's meaning changed; this pass connected
what the previous three stages had already built and finished their accessibility.

**Added:** `MinTouchTarget` (`ui/components/CircleIconButton.kt`), three acceptance criteria
(17a–17c), three navigation tests, four screenshot renders and one string
(`a11y_city_label_field`).

**Changed:**

- `CircleIconButton` — the click, the label and the semantics moved from the drawn circle to a box
  around it that is never below 48 dp, so a 44 dp control still has a comfortable target.
- `ClockwiseSegmentedControl` — each option is a 48 dp target wrapped around the unchanged pill.
- `ClocksScreen`, `MeetingPlannerScreen`, `CityDetailScreen` — the bordered and text action rows
  (`PLAN A MEETING`, `ADD CITY`, `Find a city`, `EDIT LABEL`) are clamped to 48 dp with
  `heightIn(min = MinTouchTarget)`.
- The same screens plus `SettingsSheet` and `CityLabelDialog` — `heading()` on every screen title
  and section header; `paneTitle` and a `contentDescription` naming the city on the label dialog.

**What the pass found, and what it means for later tests.**

- **A `ModalBottomSheet` is a separate window.** `performClick()` on one of its options does
  nothing under the JVM renderer — the injected touch never crosses into the sheet's window —
  while `performSemanticsAction(SemanticsActions.OnClick)` reaches the callback. The shell-level
  appearance test uses the action and says why. `criterion12c`, which renders `SettingsContent`
  directly, is unaffected.
- **Robolectric's stub font measures text at close to zero width.** A node's *height* is real; a
  text-driven *width* is not. Criterion 17b therefore asserts 48 dp in both directions only for
  the icon-only controls, whose size the app sets, and asserts height for everything else. The
  first draft of that test failed on `System` with "Actual width is 31.5 dp" — the `System` pill
  is text plus 28 dp of padding, and the stub font measures `System` at 3.5 dp.

**Verification.** A clean build — `clean assembleDebug testDebugUnitTest --no-build-cache` —
finishes at **247 tests, 0 failures, 24 suites**, with 28 screenshots, in about 2 m 30 s. The
four new renders pair the phase 2 features with the dark theme and the 12-hour format
(`clocks_screen_labelled_dark_360dp.png`, `clocks_screen_12h_360dp.png`,
`planner_screen_12h_360dp.png`, `city_label_dialog_dark_360dp.png`); the other 24 were
re-recorded because the segmented control and the small circular buttons changed size.

**Not verified on device:** none of this pass was seen on hardware. The enlarged targets were
chosen to meet the 48 dp guideline, not from a user report, and the headings and the dialog's
`paneTitle` have not been heard through TalkBack — see
[section 15.11](#1511-open--the-accessibility-pass-has-not-been-heard-on-hardware).

### 14.10 Stage 9 — the five-theme pass (phase 2)

The user's next request: replace the light/dark/system switch with **five switchable themes** —
Light Monochrome, True Dark, Warm Sepia, Cool Slate and High Contrast — each switchable in one
tap from Settings, each persisted so it survives a restart, and each covered by a screenshot.

**Decision record (user, 2026-10-18).** The choice of *how* to reconcile the old three-mode
setting with five themes was put to the user, who chose "replace with the five palettes":
Settings offers exactly the five themes, one tap each. The consequence is that device-following
is gone — a theme is an exact look, not a light/dark switch. The migration is recorded in
[section 8.1](#81-proto-schema).

**Added:** `data/prefs/AppTheme.kt` (the enum, its ids, the proto mapping and the migration);
three palettes and the `mapLandmass` token in `theme/Color.kt`; `paletteFor(theme)` in
`theme/Theme.kt`; the five-row picker in `SettingsSheet.kt`; five `theme_*` strings; seven
screenshots.

**Changed:**

- `ThemeMode.kt` **deleted**; `AppTheme` replaces it everywhere (`UserPreferences.theme`,
  `ClockRepository.setTheme`, `ClockwiseViewModel.setTheme`, `ClockwiseUiState.theme`,
  `ClockwiseRoot`, `MainActivity`).
- `saved_cities.proto` — new `SavedAppTheme` enum and `optional theme = 7`; the old
  `theme_mode = 5` is kept read-only for migration.
- `resolveDarkTheme(mode, systemInDarkTheme)` **deleted**. It had no business existing once a
  theme names an exact palette: `isSystemInDarkTheme()` is no longer read anywhere in the app.
- `SettingsSheet` — the three-option segmented control became five named rows. A segmented
  control cannot carry `Light Monochrome` at 360 dp, and a theme needs a preview, so each row is
  a full-width ≥ 48 dp radio row holding a swatch (decorative, `contentDescription` absent), the
  theme's name, and an inverted pill when it is the one in force. The rows sit in
  `selectableGroup()` so the five are announced as one set of options.
- `WorldMapCanvas` — landmasses now use `mapLandmass` rather than `accent`, and
  `WorldMapScreen`'s `IN NIGHT` legend swatch gained an outline.

**The two design problems the pass had to solve.**

1. **A black shadow over a black ocean is invisible.** High Contrast's background *and* card are
   pure black, so a black night wash leaves the ocean unchanged and the terminator disappears.
   Two ways out were considered: give High Contrast a light wash (rejected — a light wash reads
   as daylight, and "night is always painted darker" is an invariant the other four themes rely
   on), or separate the two roles `accent` was carrying. `accent` is also the fill of the app's
   soft pills, where white text has to stay readable, so it must stay dark; the landmasses needed
   to go light. `mapLandmass` was split out of `accent` for exactly this, is `accent` in the four
   soft themes (so their maps are unchanged), and is white in High Contrast. The wash is drawn
   over the continents, so that is where the terminator now lives. `map_screen_high_contrast_360dp.png`
   is the evidence.
2. **`System` could not be migrated honestly.** A user who had chosen `System` was seeing
   whichever of two palettes their device was set to, and that fact is not in the store. Rather
   than guess, `SYSTEM → Light Monochrome` (the default), and the reasoning is written into the
   migration function and the proto decision record.

**What the pass found, and what it means for later tests.**

- **A "night is darker" test does not prove the terminator is visible.** The old check — the wash
  has a low luminance and a higher alpha on the dark theme — passes for a wash that is painted
  over nothing. The palette test now composites the wash *over the landmasses* and asserts the
  result is darker, which is the property the map actually depends on.
- **`optional` is what makes the migration decidable.** Light Monochrome is the proto3 zero
  value, so a plain enum field would have made "nothing stored" and "Light Monochrome chosen"
  the same bytes, and every dark-mode user would have been handed the light theme on upgrade.
  A test switches the legacy field to dark *and* the new field to Light Monochrome to pin the
  presence bit's precedence.
- **The theme picker's rows are `selectable`, and a `selectable` node merges its children.** The
  row's bounds are therefore what `assertHeightIsAtLeast(48.dp)` measures, and its name is what
  `onNodeWithText` finds — the same shape the segmented control's options already had, so
  criterion 17b's touch-target sweep extended to the five rows without a new mechanism.

**Verification.** A clean build — `clean assembleDebug testDebugUnitTest --no-build-cache` —
finishes at **264 tests, 0 failures, 24 suites**, with **35 screenshots**, in about 3 m 10 s;
APK 13.0 MB. `ClockwisePaletteTest` went from 10 tests to 16 because every contrast, inversion,
accent and map check now runs over all five themes rather than the original two, and
`ClockwiseThemeTest` pins the five-way mapping, the ids and the default.

**Not verified on device:** the three new palettes have never been seen on the Redmi Turbo 4 —
only in Roborazzi renders — and the widget still resolves its own colours from the device, so
choosing Warm Sepia or High Contrast leaves the home-screen card neutral grey. That is the
pre-existing behaviour ([section 5.11](#511-home-screen-widget)), not a regression, but it is now
visible enough to be worth a decision — see
[section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme).

**Not verified by a human:** the five themes have not been judged by eye for taste, only asserted
for contrast and distinctness. Warm Sepia's, Cool Slate's and High Contrast's derived supporting
greys are the assistant's choices, like the dark theme's before them — see
[section 15.8](#158-open--the-palette-values-are-partly-derived).

### 14.11 v2 M2 — solar map time travel

**IMPLEMENTED** — the map can now show any minute of the device's current day rather than only
"now", and the selected city gains an offline sunrise/sunset line.

- **State:** `MapUiState` / `MapSolarSummary` and the pure `buildMapUiState` mapper. The map
  builds its own city rows at the travelled instant, so markers, strip and panel agree with the
  terminator. `MapUiState.activeCity` keeps the app's fallback-to-first-city rule.
- **Route:** `ClockwiseRoot` owns `rememberSaveable` travel minute (`null` = live), loads solar
  events through `ClockwiseViewModel.solarEvents` / `SolarRepository`, and discards results
  whose city or date no longer match.
- **Screen:** `WorldMapScreen` takes `MapUiState` plus `onTimeChange` / `onGoLive`; the new
  control shows `LIVE` or `TIME TRAVEL`, the device time/date/offset, and a `DaySlider` with the
  working-day band removed. `NOW` resets to live. The selected-city panel prints the solar day,
  and polar day/night have explicit wording rather than a blank or a fake time.
- **Canvas:** a rayed Sun marker at the subsolar point, drawn between the night shading and the
  active meridian.
- **Typography/format:** `CityTimeFormatter.timeText(LocalTime, use24Hour)` is now shared by
  regular snapshots and solar times, so the 12h/24h setting reaches the solar line too. The
  slider's progress range is clamped to its last grid slot while still drawing 23:59 exactly.
- **Tests:** seven new `WorldMapScreenTest` cases (live, travel, NOW, slider semantics, solar
  line, polar day, polar night), sixteen `MapStateMapperTest` cases (readouts, date-line dates,
  stale-solar rejection, fallback, 12h/24h, polar summaries) and the new time-travel screenshot.
  JVM suite is now **282 tests, 0 failures, 27 suites, 36 screenshots**.

**Not verified on device:** the new map control has only been exercised in Roborazzi renders
and Compose tests; touch scrubbing and TalkBack on the slider still need a hardware pass.

### 14.12 v2 M3 — planner source conversion and best overlap

**IMPLEMENTED** — the meeting planner is now a true converter, and it recommends an hour.

- **Domain:** `bestWorkingHoursSlot(date, sourceZoneId, cityZoneIds)` returns a
  `WorkingHoursOverlap`, scanning all 96 quarter-hour slots of the source day. It maximises the
  number of cities inside 09:00–17:59, breaks ties by the sum of squared distances from local
  noon among the working cities, then by the earlier slot, and returns `null` unless at least
  two cities are at work.
- **State:** `PlannerSource` plus `sourceCityId`, `sourceName`, `sources`, `bestSlot`,
  `bestTimeText` and the computed `bestWorkingCount` on `PlannerUiState`.
- **Mapper:** `buildPlannerUiState(..., sourceCityId)` resolves the source against the saved
  cities (falling back to the device), reads the slot on the source's current civil date,
  measures every row's difference from the source, and computes the recommendation in the same
  pure pass.
- **Screen:** a `FROM` chip row (`LOCAL` first), a headline that switches between
  `YOUR LOCAL TIME` and `TIME IN <CITY>`, and the `BEST <time>` / `NO OVERLAP` action in the
  city-list header. The best action is a button that moves the slider; the no-overlap state is
  a status with no click action. Test tags live in `PlannerTestTags`.
- **Route:** `ClockwiseRoot` keeps the source city in `rememberSaveable`, so rotation preserves
  it and leaving the screen discards it, like the slot.
- **Tests:** `MeetingTimeTest` grew from 10 to 14 (best slot, tie-break, minimum two, empty),
  `PlannerStateMapperTest` from 10 to 18 (source chips, source date, source-relative
  differences, fallback, best recomputation, 12h/24h), `MeetingPlannerScreenTest` from 7 to 15
  (chips, headings, best button, no overlap, empty planner), and `AcceptanceCriteriaTest` from
  33 to 36 (15e–15g). The new screenshot is `planner_screen_source_360dp.png`; the four
  existing planner renders were re-recorded. JVM suite is now **306 tests, 0 failures,
  27 suites, 37 screenshots**.

**Not verified on device:** the source chips and the best action have only been exercised in
Roborazzi renders and Compose tests; touch, TalkBack grouping and the source row's horizontal
scroll still need a hardware pass.

### 14.13 v2 M4 — Glance 1.2.0 widget rebuild

**IMPLEMENTED** — the v1.0 widget is rebuilt on Glance 1.2.0: responsive sizes, generated picker
previews, explicit row actions and a city deep link.

- **State:** `WidgetUiState` / `WidgetRow` and the pure `buildWidgetUiState`, still one
  delegation to `buildClockwiseUiState` so a widget row and a city card cannot disagree.
  `WIDGET_MAX_ROWS = 6` remains the design ceiling.
- **Responsive geometry:** `WIDGET_SIZES` declares `220×110`, `250×180`, `250×250` and
  `320×250` dp through `SizeMode.Responsive`; `widgetRowBudget` reads `LocalSize`, the font
  scale and whether the `+N more` line will be drawn, then clamps to 1..6. A missing size falls
  back to six. `WidgetGeometry` owns the padding, rule, row/header/count heights and font sizes
  so the drawing and the budget cannot drift apart.
- **Drawing:** the card is the launcher's app-widget background with the app's card token, a
  hairline and one row per city (sun/moon, name, weekday only across the date line, time).
  Overflow becomes `+N more`; an empty card says so. The refresh glyph runs
  `RefreshWidgetAction` in place; the header, a row, the count line and the empty state carry
  their own actions. Colours resolve through `DayNightColorProvider` against the **device's**
  light/dark setting.
- **Deep link:** each row starts `MainActivity` with `EXTRA_CITY_ID` and
  `FLAG_ACTIVITY_SINGLE_TOP`; the activity is `singleTop`, consumes the extra once in
  `onCreate` / `onNewIntent`, and `ClockwiseRoot` waits for the graph's first back-stack entry
  before selecting the city and opening its detail route. Back therefore returns to the city
  list rather than leaving the app.
- **Previews:** `providePreview` composes the same card, and `previewSizeMode` uses the same
  responsive set so the taller picker previews exist. `WidgetPreviewPublisher` calls
  `setWidgetPreviews` on Android 15+ once per `DESIGN_VERSION`, records only a real success,
  retries after a rate-limit or failure, and leaves the static
  `drawable-nodpi/widget_preview.png` as the fallback on older releases.
- **Lifecycle:** the provider receiver starts the mirror and minute ticker on enable/update,
  cancels both on the last removal, and `ClockwiseApp` restores them when a process starts
  with a widget on screen. `WidgetTickerReceiver` redraws through Glance's `updateAll`, then
  re-arms the next minute. The manifest adds only the exported provider and the private ticker
  receiver; no permission was added.
- **Tests:** six new suites — `WidgetStateMapperTest` (11), `WidgetGeometryTest` (10),
  `ClockwiseWidgetTest` (16), `WidgetMirrorTest` (7), `WidgetTickerTest` (4) and
  `WidgetPreviewPublisherTest` (6) — plus acceptance criteria 16a–16e in
  `AcceptanceCriteriaTest`, which grew from 36 to 41. The widget keeps **no screenshot**:
  Glance draws through `RemoteViews`. JVM suite at M4 was **365 tests, 0 failures, 33 suites,
  37 screenshots**; [section 12.2](#122-current-results) reports the current totals.
- **Verification:** `assembleDebug`, `assembleRelease`, `testDebugUnitTest` and `lintDebug`
  pass (0 errors, 30 warnings). The release APK was 9.64 MiB and still declared no `INTERNET`
  or `ACCESS_NETWORK_STATE`.
- **Not verified on device:** the card has not been placed on a real home screen, so the
  launcher's size resolution, the generated picker preview, the deep link, the 28 dp rows and
  the minute alarm under doze remain hardware items ([section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen)).

### 14.14 v2 M5 — hardening and 2.0.0 release preparation

**IMPLEMENTED** — the release-hardening pass, plus the version bump and the release documents.

- **Bounded solar cache.** New `data/repository/SolarCachePolicy.kt`: `RETENTION_DAYS = 30`,
  with the cutoff computed from the **device's local date** (`clock.withZone(systemDefault)`),
  not UTC. `ClockwiseApp` calls `SolarRepository.pruneBefore()` in its application scope on
  every launch, so the table cannot grow one row per city per day forever. The current UI only
  reads today's row, so the 30-day grace never hides a requested value.
- **Application-scope error handling.** The maintenance scope now carries
  `CoroutineExceptionHandler`; an uncaught failure in seeding, pruning or preview publishing is
  logged instead of reaching the default handler.
- **Debug-only StrictMode.** `ClockwiseApp` installs `detectAll()` thread and VM policies with
  `penaltyLog()` only when `ApplicationInfo.FLAG_DEBUGGABLE` is set. Release behaviour is
  unchanged, and a debug pass can find main-thread I/O or a leaked resource without turning a
  warning into a crash.
- **Large-font accessibility evidence.** `LayoutScreenshotTest` renders the Clocks screen with a
  1.5× font scale (`clocks_screen_large_font_360dp.png`). The widget's row/header heights already
  derive from the font scale, so the same change is visible in its row budget.
- **Tests.** New `SolarCachePolicyTest` (5 tests: retention, 30-day cutoff, UTC-midnight
  boundary in Yangon, the Los Angeles side of UTC, stability through a local day), one new
  `SolarRepositoryTest` pruning test (the cutoff day survives, older rows do not), and one new
  screenshot test. Totals are in [section 12.2](#122-current-results).
- **Release preparation.** `versionCode = 2`, `versionName = "2.0.0"`; added
  [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) and
  [CHANGELOG.md](CHANGELOG.md#v200); updated `README.md`,
  `SPECIFICATION.md` and `HUMAN_VERIFICATION.md`. The application id, namespace, SDK levels,
  signing key and permission model are unchanged.
- **Deliberately not done.** No Burmese/localization work, no store/Uptodown action, no change
  to `submission/`, and no claim that the hardware checks passed. The landing page keeps its
  v1.0.0 link until a v2.0.0 asset is actually published (§6 of the checklist).

### 14.15 v2.1 — Material You, reordering, shortcuts, intro and widget polish

**IMPLEMENTED** — the 2.1 feature pass, shipped as `versionCode 3` / `versionName 2.1.0`.

- **Material You.** Added `AppTheme.MATERIAL_YOU(5)` and
  `SAVED_APP_THEME_MATERIAL_YOU = 5`, leaving the five original ids untouched.
  `rememberClockwisePalette` resolves the theme: the five static palettes are unchanged, while
  Material You uses `dynamicLight/DarkColorScheme` on API 31+ and
  `MaterialYouFallbackPalette` / `MaterialYouDarkFallbackPalette` below it.
  `paletteFromMaterialScheme` maps Material roles onto `ClockwisePalette`, so no screen knows
  which theme produced its colours. Added the sixth Settings row and its swatch, plus renders
  for the settings sheet and the pre-Android-12 fallback.
- **Room-backed reordering.** Database v3 adds `saved_cities` (`cityId` primary key,
  indexed `position`) with `MIGRATION_2_3`. `CityOrderStore` exposes the ordered flow and
  transactional writes; `ClockRepository.reorderCities` sanitises input, writes DataStore and
  Room in one call, and no-ops an unchanged order. `synchronizeOrder` seeds and repairs drift,
  so an upgrade keeps the user's list. The insert is moved by a drag host in `ClocksScreen`
  (long-press, haptic, live swap, write on release), and the accessibility path is
  `CustomAccessibilityAction` Move up / Move down on each card, with `selected` on the active
  card. Added `SavedCityDaoTest`, `ClocksScreenReorderTest` and repository/migration cases.
- **Launcher shortcuts.** `res/xml/shortcuts.xml` declares the three static actions
  (`add-city`, `planner`, `map`) with short and long labels. `CityShortcuts` publishes the first
  saved cities as `city-<id>` dynamic shortcuts, using labels when present and a
  `dynamicShortcutLimit` that subtracts the three static actions and clamps to 0..3.
  `ShortcutMirror` watches only saved ids and labels. `ShortcutManagerCompat.reportShortcutUsed`
  is called from `MainActivity` for `EXTRA_SHORTCUT_ID`, which only city shortcuts set, so a
  widget tap is not counted. Added `CityShortcutsTest`, `ShortcutMirrorTest` and
  `LauncherShortcutsDeclarationTest`, and cleared lint's `ReportShortcutUsage`.
- **First-launch intro.** `Optional bool onboarding_completed = 8` with presence-aware reading:
  fresh installs seed `false`, pre-2.1 stores read as completed. `MainActivity` gates
  `OnboardingScreen` on `!isLoading && !onboardingCompleted`; `completeOnboarding` /
  `restartOnboarding` drive dismissal and the Settings replay row. `UserPreferences.Default`
  is completed, so a read failure cannot trap the user. Added `OnboardingScreenTest` and a
  screenshot.
- **Widget card and sizes.** The card is now `drawable/widget_background.xml`, a 94%-opaque
  rounded shape with a hairline outline. `WIDGET_SIZES` gained `180×110`, and the provider's
  `minResizeWidth`/`minResizeHeight` are 180/110 dp while the default footprint stays 4×4.
- **Battery-aware ticking.** `WidgetTicker` uses one-minute `setAndAllowWhileIdle` while the
  screen is on, five-minute inexact `set` while it is off, and redraws immediately on
  `ACTION_SCREEN_ON`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED`.
  The minute cadence is restored on screen-on; the 30-minute `updatePeriodMillis` remains the
  backstop. Extended `WidgetTickerTest`.
- **Docs and version.** Bumped `versionCode` to 3 and `versionName` to `2.1.0`; added
  [CHANGELOG.md](CHANGELOG.md#v210), kept the 2.0.0 note as history, and
  updated `README.md`, this specification, `HUMAN_VERIFICATION.md` and `RELEASE_CHECKLIST.md`.
  The application id, namespace, SDK levels, signing key and no-network permission set are
  unchanged, and `submission/` remains untracked.
- **Deliberately not done.** No localization, no store/Uptodown/appeal/resubmission action, and
  no claim that the hardware checks passed.

### 14.16 v3.0 — PJ95 package identity and "Concept 3" icon

**IMPLEMENTED** — the 3.0 rebrand, shipped as `versionCode 4` / `versionName 3.0.0`.

- **Package identity.** `applicationId` and `namespace` both set to `com.pj95.clockwise`
  (`app/build.gradle.kts`), replacing `com.worldclock.offline` and `com.worldclock.app`. The three
  source trees moved with `git mv` (`app/src/{main,test,androidTest}/java/com/clockwise/app` →
  `com/pj95/clockwise`); 122 Kotlin, XML and proto files were rewritten in one pass, preserving
  their BOMs. The manifest's relative component names follow the namespace automatically; the
  two places that spelled the id out — the widget tick action
  (`com.pj95.clockwise.action.WIDGET_TICK`) and the file-provider authority — were updated with
  it. The proto `package`/`java_package` is `com.pj95.clockwise.data.prefs`, and the ProGuard
  keep rule follows.
- **Room schema path.** `app/schemas/com.worldclock.app.data.local.WorldClockDatabase/` moved to
  `com.pj95.clockwise.data.local.ClockwiseDatabase/` unchanged: the directory is the database
  class's fully-qualified name, but the JSON does not embed the package and the identity hash is
  computed from the schema, so the v1-v3 snapshots still validate.
- **"Concept 3" icon.** New `drawable/ic_launcher_background.xml` (`#0E2A66` field, `#143675`
  diagonal facet), `ic_launcher_foreground.xml` (330° dial open at the top right, ivory ring and
  hour hand, gold minute hand at 45° through the opening, gold centre pin and a 50° lower-left
  inlay) and `ic_launcher_monochrome.xml` (single-colour themed variant). Declared in
  `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`. Legacy
  `mipmap-{m,h,xh,xxh,xxxh}dpi` PNG pairs were rendered from those vectors — rounded-square mask
  for `ic_launcher`, circular for `ic_launcher_round` — and are never loaded at runtime on API 26+.
  `AppIconTest` grew to three tests: compose the icon to `app/build/screenshots/app_icon.png`, assert
  `ApplicationInfo.icon`, and inflate every layer. The unused `R.color.ic_launcher_background` was
  deleted; the background is a drawable now.
- **Doc and version work.** Bumped to `3.0.0`/`4`, added
  [CHANGELOG.md](CHANGELOG.md#v300), and updated `README.md`,
  `HUMAN_VERIFICATION.md`, `RELEASE_CHECKLIST.md` and this specification.
  `app_name` stays **Clockwise**; "Chronos" is the project codename.
- **Deliberately not done.** No UI restyle, no new behaviour, no localization, no store action,
  no change to `submission/`, and no claim that the hardware checks passed. The landing page that
  existed at the time was later removed in 1.0.10.

### 14.17 v3.1 — alarms, four themes, the comparison switch, wheel time and alarm sound

**IMPLEMENTED** — the 3.1.0 usability release, `versionCode 5` / `versionName 3.1.0`.

- **Alarms.** New `domain/alarm` (`Alarm`, `RepeatDays`, `nextTrigger`, `AlarmScheduling`),
  `alarm/*` (`AndroidAlarmScheduler` with `setAlarmClock`, `AlarmReceiver`,
  `AlarmRescheduleReceiver`, `AlarmNotifications`, `AlarmRingingActivity`), Room v4 `alarms` +
  `AlarmDao` + `AlarmEntity`, `RoomAlarmRepository`, `AlarmViewModel`, and the Alarms tab with
  its editor and ringing screen. The bottom bar became `Alarms · Clocks · Map`; search moved back
  to the `+` actions on Clocks and Map. Manifest gains `USE_EXACT_ALARM`,
  `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `VIBRATE` and `USE_FULL_SCREEN_INTENT`, and
  still declares no `INTERNET` or `ACCESS_NETWORK_STATE`.
- **Two themes retired.** `AppTheme` dropped True Dark and High Contrast (`TRUE_DARK = 1` and
  `HIGH_CONTRAST = 4` are left unused), `DarkClockwisePalette` became
  `NightClockwisePalette` for the widget, and `HighContrastClockwisePalette` was deleted.
  `paletteFor` is a four-way mapping; stored ids 1 and 4 and the legacy `THEME_MODE_DARK` all read
  as the default. `SettingsSheet` scrolls and shows four rows.
- **Comparison switch.** Proto `optional bool compare_with_local_time = 9` (absent = on),
  `UserPreferences.compareWithLocalTime`, `ClockRepository.setCompareWithLocalTime`,
  `ClockwiseViewModel` + UI state + mapper plumbing, the Settings switch, and `showDifference`
  carried into the card, the map panel and the detail screen. The planner keeps its offsets.
- **Wheel time and sound.** `WheelTimePicker` (snapping `LazyColumn`s, centre line, 12 h AM/PM
  segmented control); the editor renders the platform `TimePicker` under Material You instead.
  `Alarm.soundUri` + v5 migration, `AlarmSoundCatalog` / `AndroidAlarmSoundCatalog` (device alarm
  tones, `Ringtone` preview, default-first), the `SOUND` row and picker dialog, and one
  notification channel per distinct URI (`channelId(uri)`; the default keeps `"alarms"`).
- **Lint.** `AlarmNotifications.show` now checks `POST_NOTIFICATIONS` with
  `ContextCompat.checkSelfPermission` and catches `SecurityException`, which cleared the one
  `MissingPermission` error the feature introduced. `lintDebug` is 0 errors / 32 warnings.
- **Tests.** New `AlarmScheduleTest`, `AlarmDaoTest`, `RoomAlarmRepositoryTest`,
  `AlarmScreenTest`, `AlarmSoundCatalogTest`; new migration 4 → 5 case; criteria 18a-18c; every
  theme/criteria/screenshot test updated for four themes and the night pair. JVM suite: **503
  tests, 0 failures, 45 suites, 47 screenshots**; `assembleDebug`, `assembleRelease`,
  `assembleDebugAndroidTest` and `lintDebug` pass.
- **Deliberately not done.** No localization, no store/Uptodown action, no change to
  `submission/`, and no claim that the hardware checks passed. The wheel's feel, the real
  ringtone list and preview, the full-screen ring over a lock screen and Doze behaviour are
  human items in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

### 14.18 v4.0 Phase 1 - the timer, the screen saver and the app's own Settings

**IMPLEMENTED** — Phase 1 of "Chronos", `versionCode 6` / `versionName 4.0.0`.

- **Timer.** New `domain/timer` (`CountdownTimer`, `TimerFormat`, `TimerScheduling`,
  `TimerAlerting`), `alarm/{AndroidTimerScheduler,TimerReceiver,TimerNotifications,
  AndroidTimerAlerting,TimerRingingActivity}`, Room v6 `timer` + `TimerDao` + `TimerEntity` +
  `MIGRATION_5_6`, `RoomTimerRepository`, `TimerViewModel`, `TimerPanel`,
  `TimerDurationPicker`/`TimerPresets`, `TimerRingingScreen`, and the `ALARMS · TIMER` segmented
  control on the Alarms tab. The running notification is the platform chronometer rather than a
  per-second service.
- **Screen saver.** `screensaver/ScreenSaverActivity` (non-exported, immersive,
  `FLAG_KEEP_SCREEN_ON`) and `ui/screens/ScreenSaverScreen` with the five-position burn-in drift,
  reachable from Settings.
- **Settings.** The sheet is now grouped `APPEARANCE` · `COMPARED WITH YOUR LOCAL TIME` ·
  `ALARMS` · `EXTRAS` · `CHRONOS STUDIO`; `AlarmReadiness`/`readAlarmReadiness` supply the four
  grant states, re-read on `ON_RESUME`, and each row opens the matching system screen.
- **Local information screens.** `ui/screens/InfoScreen.kt` (Privacy, Help, Chronos Studio) behind
  three routes, and `ui/Feedback.kt` (mailto + clipboard fallback + diagnostics). No WebView and no
  billing dependency.
- **Wiring.** `MainActivity`/`ClockwiseRoot` accept a timer deep link
  (`EXTRA_TIMER`), the process re-arms the stored timer at start and after boot, and the alarm
  channel now sets `setBypassDnd(true)`.
- **Guardrails.** `OfflineGuardrailTest` asserts no `INTERNET`/`ACCESS_NETWORK_STATE` in the
  merged manifest, no billing/ads/analytics/crash class on the classpath, and the four alarm
  permissions still declared.
- **Lint.** `WindowCompat.getInsetsController` replaces the platform controller in the screen
  saver (the one `WrongConstant` error Phase 1 introduced), and `ScreenSaverScreen` uses the
  lambda `Modifier.offset` overload. `lintDebug` is 0 errors / 34 warnings.
- **Tests.** New `CountdownTimerTest`, `RoomTimerRepositoryTest`, `TimerPanelTest`,
  `PhaseOneScreenTest`, `OfflineGuardrailTest`, a timer-segment test in `AlarmScreenTest`, the
  5 → 6 migration case, and five new renders. JVM suite: **542 tests, 0 failures, 50 suites,
  52 screenshots**; `assembleDebug`, `assembleRelease` and `lintDebug` pass.
- **Deliberately not done.** No localization, no store/Uptodown action, no change to
  `submission/`, and **no on-device test run**: the build is installed and the hardware checks are
  listed but unrun, at the user's request.

### 14.19 v4.1 alarm refinements and gestures - compact cards, a service-owned ring, per-alarm snooze, reorder and swipe

**IMPLEMENTED** — `versionCode 7` / `versionName 4.1.0`.

- **Simpler navigation.** Removed the `ALARMS · TIMER` segmented control and the top-right `+`.
  The header now carries an hourglass (`ic_hourglass.xml`) that opens the timer, and the timer
  section has a close button plus a `BackHandler`. `ClockwiseRoot` carries a saveable
  `timerRequested` flag so a timer notification lands on the countdown even when the Alarms tab is
  already open. Alarm creation remains on the `YOUR ALARMS` header `+` and the `ADD ALARM` row.
- **Compact cards.** `AlarmCard` is two lines: the time, then the label and repeat rule. The switch
  is still the only control that writes without opening the editor.
- **AM/PM as a span.** `domain/time/ClockText` + `CityTimeFormatter.clockText()` split a time into
  hour/minute/period; `ui/components/AlarmTimeDisplay` renders them as one `AnnotatedString` with a
  `SpanStyle` period, so TalkBack reads `6:30 AM` as one value. Used by the card, the editor preview
  and the ringing screen.
- **Service-owned ring.** New `alarm/AlarmRingingService` (foreground `mediaPlayback`, looping
  `MediaPlayer` on `USAGE_ALARM`, `setWakeMode` + explicit wake lock, repeating vibration,
  ten-minute cap, `ACTION_STOP`). `AlarmReceiver` calls `onFired`, rolls forward or disables a
  one-shot, then starts the service through `ContextCompat.startForegroundService`.
- **Silent channel.** `AlarmNotifications` now posts on `alarm-ringing` with no sound, deletes every
  legacy `alarms*` channel on upgrade, sets `FOREGROUND_SERVICE_IMMEDIATE`, and omits Snooze when
  the policy refuses it.
- **Full-screen ring.** `USE_FULL_SCREEN_INTENT` + `AlarmRingingActivity`
  (`showWhenLocked`/`turnScreenOn`). `AlarmReadiness.fullScreenIntentAllowed` reads Android 14's
  `canUseFullScreenIntent()`, and Settings offers **Full-screen alarms** with the matching system
  screen. `AlarmRingingScreen` uses `AlarmRingingTheme` (dark dynamic palette), the user's label as
  the hero, the time with AM/PM beneath it, and no generic quote text.
- **Per-alarm snooze.** `domain/alarm/SnoozePolicy` (`ALLOWED`/`ONCE`/`NONE`) with
  `SnoozeRules.canSnooze`/`suggestsCritical`. Room v7 adds `snoozePolicy`, `snoozesUsed` and
  `snoozedAtEpochMillis` (`MIGRATION_6_7`); `AlarmDao` gains `markSnoozed`, `resetSnoozeCycle` and
  `clearSnoozeMarker`; `RoomAlarmRepository.snooze` returns `Boolean` and refuses a blocked write.
  The editor offers the three policies and a whole-word medication suggestion.
- **Alarm reorder.** `AlarmEntity`/`Alarm` gain `sortOrder`; Room v8 adds the column and
  `MIGRATION_7_8` backfills it by time of day. `AlarmDao` orders by `sortOrder, hour, minute, id`,
  exposes `maxSortOrder`/`setSortOrder` and a transactional `reorder`, and `save` appends new alarms
  at `maxSortOrder + 1`. `AlarmScreen` mirrors the city list's long-press drag with a `LongPress`
  haptic and `graphicsLayer` translation, persisting once on release.
- **Swipe to delete with Undo.** New `components/SwipeToDeleteBox` wraps alarm and city cards in a
  `SwipeToDismissBox` with only `EndToStart` enabled. `ClockwiseRoot` deletes immediately and shows
  `Alarm deleted` / `City removed` with **UNDO** (`SnackbarDuration.Long`). `AlarmRepository.restore`
  re-inserts the exact row and re-arms it when enabled; `ClockRepository.restoreCity` restores the
  previous order and active city. Both card types gain Move up / Move down / delete custom actions.
- **Guardrails.** `OfflineGuardrailTest` now also requires `WAKE_LOCK`, `FOREGROUND_SERVICE` and
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- **Tests.** New `ClockTextTest` (4) and `SnoozePolicyTest` (8); extended `AlarmDaoTest` (14),
  `RoomAlarmRepositoryTest` (18), `ClockwiseDatabaseMigrationTest` (6), `ClockRepositoryTest` (37),
  `AlarmSoundCatalogTest`, `AlarmScreenTest` (17), `ClocksScreenReorderTest` (9),
  `ClockwiseNavigationTest` (14), `LayoutScreenshotTest` and `FakeAlarmRepository`.
  JVM suite: **583 tests, 0 failures, 52 suites, 54 screenshots**; `assembleDebug`,
  `assembleRelease` and `lintDebug` pass (0 errors / 33 warnings / 3 information).
- **Deliberately not done.** No localization, no store/Uptodown action, no change to `submission/`,
  and **no on-device test run**: the build is installed only after approval, and the hardware rows
  are listed but unrun, at the user's request.

### 14.20 v1.0.10 Clockwise rebrand, module split and fresh-install identity

**IMPLEMENTED** — `versionCode 11` / `versionName 1.0.10`, package `com.pj95.clockwise`.

- **Brand.** Every remaining `WorldClock` / `worldclock` identifier, file, resource, Gradle setting
  and documentation reference now reads `Clockwise` / `clockwise`. The application id and namespace
  stay `com.pj95.clockwise`.
- **Persisted identities.** The Room database file is `clockwise.db`; the premium widget style id is
  `premium_clockwise`; the release keystore is `clockwise.jks` with a new
  `CN=Clockwise, OU=Mobile, O=PJ95 Studio, L=Yangon, ST=Yangon, C=MM` certificate (SHA-256
  `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9`). Because the signing key
  changed, 1.0.10 is a **fresh install**: an older 4.1.0 build must be uninstalled first, and saved
  cities/alarms start empty. The database seeds at `user_version = 10`.
- **Modules.** The pure Kotlin core is split into `:core:model` (dependency-free types),
  `:core:common` (formatting and display helpers) and `:core:domain` (contracts and calculations),
  below `:core:data`, `:core:ui` and the `:feature:*` modules. Dependencies point inward only; see
  [ARCHITECTURE.md](ARCHITECTURE.md).
- **Repository.** All release history is consolidated in [CHANGELOG.md](CHANGELOG.md), long-form
  docs live under `docs/`, and the GitHub Pages/Netlify site files and `screenshots/` were removed.
  No live URL or website is maintained.
- **Tests and build.** JVM suite: **596 tests, 0 failures, 54 suites**; `lintDebug` passes with
  0 errors / 27 warnings. `assembleRelease` produces a signed APK with no `INTERNET` or
  `ACCESS_NETWORK_STATE` permission. The automated device checks pass on the reference phone
  (fresh install, `clockwise.db` at v10, `CN=Clockwise`, deep links, no crash); the widget
  and TalkBack rows stay manual ([HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §7.8).
- **Deliberately not done.** No behaviour or UI redesign, no localization, no store/Uptodown action,
  and no claim that the hardware checks passed.

## 15. Known issues and open questions

### 15.1 RESOLVED — instrumented test defects

`ClockwiseEndToEndTest` had **four test-authoring defects** (not app defects). The file has
been **deleted** and replaced by `ClockwiseDeviceSmokeTest.kt`, a deliberately minimal
two-test on-device suite. Instrumented and Espresso tests were **deprioritised by the user**:
they are flaky across OEM animation and timing settings, and the unit + Roborazzi layers
already carry the coverage.

The findings are kept because they are still true of the app, and any future device test will
hit them:

| # | Root cause (still true) | Guidance for future device tests |
| --- | --- | --- |
| 1 | The 12h/24h control lives on the **city detail screen**, not the Clocks screen. | Navigate into a city's detail first. |
| 2 | A city card shows the **region** (`Myanmar`) and the **offset** (`UTC+6:30`), never the IANA id. | Assert on `Yangon` and the offset. |
| 3 | Espresso's `pressBack()` refuses to run when the device animation scale is non-zero. | Do not use `pressBack()`. Use the on-screen Back button or `activity.onBackPressedDispatcher`. |
| 4 | Every city mutation is a disk write plus a flow round-trip, so the UI updates a beat later; and during a navigation transition both destinations are briefly composed. | Wait with `waitUntil` against **semantics state**, not display text. |

**Evidence for #4.** A temporary diagnostic (`waitUntil` on the London marker's `Selected`
semantics, 8 s budget) reported:

```text
selected after navigating to map: los-angeles
london selected after tap: true
los-angeles still selected: false
timed out waiting for the tap: false
```

So the marker tap **does** work; it is simply not synchronous. The diagnostic file was
removed after use.

**Consequence beyond the tests.** This is a genuine architectural characteristic worth
remembering: on a real device every mutation is a *disk* write followed by a flow round-trip,
so the UI updates a beat later. The JVM suite never exposed this because
`FakeSavedCitiesDataStore` is synchronous. This now applies to the theme as well — the
settings sheet repaints one beat after the tap, which is why `criterion12c` asserts on the
callback rather than on the resulting palette. Any future code that assumes a mutation is
visible immediately will be wrong; any future UI that needs instant feedback should apply it
optimistically.

### 15.2 RESOLVED — dependency injection

The app uses a hand-written `AppContainer` plus a `Context.appContainer` extension. Hilt was
offered as an alternative and the **user confirmed the manual approach** (2026-10-17):
it keeps the app lightweight, avoids APK growth, compiles faster, and all existing tests
already pass. Switching later remains containable because
`ui/mapping/ClockwiseStateMapper.kt` is the single data → UI path.

### 15.3 CLOSED — emulator tooling

Emulator end-to-end verification was originally blocked: the Android SDK has no
`cmdline-tools` (no `sdkmanager` / `avdmanager`) and no AVDs exist. This is now **moot** —
a physical device is connected and has been used for end-to-end verification. Reinstalling
`cmdline-tools` is no longer required unless a second, scriptable device is wanted for
`adb`-driven UI automation.

### 15.4 Note — not a defect

An early device screenshot showed the app opening on the Tokyo detail screen instead of the
Clocks list. This was a stale task being resumed by `am start`, not a navigation bug. A
`force-stop` followed by a fresh start shows the Clocks list, as required.

### 15.5 OPEN — no version control

`D:\MY APP\Clockwise` is **not a git repository**. All work exists only as working-tree
files. There is no history, no diff, and no way to recover an overwritten file.

This is now the highest-risk item in the project: phases 1 and 2 both restyled shared files
(`Color.kt`, `Theme.kt`, `ClocksScreen.kt`, `WorldMapCanvas.kt`), and there is no way to diff
or revert any of it.

### 15.6 RESOLVED — section 11 wording

Confirmed by the user; see [section 0.1](#01-section-11-provenance--resolved).

### 15.7 OPEN — first-frame flash when the theme disagrees with the device

The theme is stored in DataStore, which is read asynchronously. Until the first emission arrives
the app paints `AppTheme.Default` — Light Monochrome — and the *launch theme*
(`res/values/` and `res/values-night/`) still follows the device. So whenever the chosen theme
and the device's own setting disagree there is one wrong-coloured frame at cold start: Warm Sepia
or Cool Slate on a dark device starts dark, Material You on a light device starts light.

The flash is now limited to the light/dark disagreement, because 3.1.0 removed the two dark app
themes; it can no longer be triggered by a dark app theme on a light device. Between the four
shipped themes only Material You actually follows the device, so in practice the remaining case is
a light themed app on a phone set to dark.

- Impact: low. The read completes in tens of milliseconds and only affects cold starts, and the
  status/navigation-bar icons are already correct because `SystemBarAppearance` follows the
  palette rather than the device.
- Full fix would mean blocking the first frame on the DataStore read, or mirroring the choice
  into a synchronous store such as `SharedPreferences` purely for the launch theme — which would
  also mean shipping four launch themes instead of two. Not worth the complexity yet.

### 15.8 OPEN — the palette values are partly derived

Light Monochrome's base pair was specified by the user; the rest, and everything in Warm Sepia and
Cool Slate except the base pairs, were **derived** to preserve the relationships the design relies
on (see [section 7.1](#71-colour)). They have been reviewed as relational values — contrast,
inversion, distinctness, a visible terminator — and not as design values, and nobody has looked
at them by eye except in the Roborazzi renders.

The most opinionated derivations:

- `primary = foreground` in every fixed theme, so the selected card is the *inverse* of the base
  surface. The alternative is a subtly raised surface with ink text, which loses the inversion but
  is quieter.
- Warm Sepia's and Cool Slate's `mutedForeground` are the assistant's choices (`#7A7065`,
  `#64748B`), picked to clear 4:1 on their own background. They are supporting-label greys, not
  body text, but the taste is unjudged.
- The widget's night pair (`NightClockwisePalette`) is derived the same way and is now the only
  dark surface the app can paint, so it is the part with the least independent review.

### 15.9 OPEN — the meeting planner has not been touched on hardware

The day slider is the first control in the app that is **dragged** rather than tapped, and the
first that reports a value to TalkBack. Its behaviour is covered on the JVM — touch mapping,
snapping, the `setProgress` action, the read-out — but what a JVM cannot tell us is still open:

- How the 15-minute snapping feels under a real finger. Continuous dragging with a snapped
  value can feel sticky if the step is too coarse, and 15 minutes is a judgement.
- Whether TalkBack announces `stateDescription` as written. It is set on the same node as
  `progressBarRangeInfo`, which is the contract Material's own slider uses, but the spoken
  result has not been heard.
- Whether a 10 dp thumb is comfortable to grab. It meets the touch-target rule because the whole
  48 dp strip is the target, not just the circle — which is also why a miss still lands.
- Whether the horizontally scrolling `FROM` source chips are discoverable — the row has no
  scroll affordance beyond the clipped next chip — and whether `BEST 06:30` reads as an action
  rather than a status beside `YOUR CITIES AT THIS TIME`.

The same input-injection limitation that blocks tapping the label dialog on this device applies
here: this ROM refuses `adb shell input tap` and `adb shell input keyevent`, so a scripted
on-device pass would have to wait for a human finger.

### 15.10 OPEN — the widget has not been placed on a real home screen

The widget is asserted thoroughly on the JVM — the declared provider info, the responsive row
budget and the `+N more` count at each breakpoint, the per-city time and day, the sun/moon per
city, the colour each token resolves to under a light and a dark device configuration, the
header/row/count click actions and the city extra, the screen-aware alarm cadence, and the
mirror's redraw on a saved-cities change. What a JVM cannot tell us is still open:

- **Whether the card looks right in a real launcher.** Glance draws through `RemoteViews`, which
  Roborazzi cannot render, so there is no widget screenshot at all. Five rows at 250 dp, a 32 dp
  header and 28 dp rows are arithmetic, not a picture.
- **Whether the launcher resolves the responsive sizes as intended.** The card declares
  `180×110`, `220×110`, `250×180`, `250×250` and `320×250` dp and asks for a 4×4 default with
  `targetCellWidth`/`targetCellHeight`, while `minResizeWidth`/`minResizeHeight` are 180/110 dp;
  every launcher resolves cells differently, and the card has never been measured on a grid. The
  compact layout's `+N more` and the six-row ceiling have only been exercised with a synthetic
  Glance size.
- **Whether the translucent card is legible on a real wallpaper.** The 94%-opacity fill, the
  hairline outline and the 16 dp radius are asserted as resource values, but no JVM can put the
  card on a busy photo. Contrast against a light and a dark wallpaper is a by-eye check.
- **Whether the generated picker preview appears.** `setWidgetPreviews` is Android 15+ only and
  rate-limited; the publisher's success/retry logic is unit-tested, but the live preview has not
  been seen in a picker, and the static `widget_preview.png` fallback has not been compared with
  the real card side by side.
- **Whether a row's deep link behaves under a real task stack.** The Intent, `singleTop` and
  one-shot extra are unit-tested, but tapping a row while the app is open, from a cold start and
  after rotation has not been exercised by hand.
- **Whether the ticker actually saves battery.** The screen-aware cadence is unit-tested, but the
  real saving depends on how the launcher and Doze batch the inexact alarm. The worksheet measures
  screen-off wake-ups and the card's staleness rather than assuming the five-minute interval is
  honoured.
- **Which font the launcher uses.** `RemoteViews` cannot carry a bundled font, so the widget is
  drawn in the platform's family, not Inter, and its figures are not tabular — the minute column
  will shift width as the digits change where the app's rows do not.
- **Whether the small widget targets are usable.** The rows are full-width but 28 dp tall, the
  header 32 dp and the refresh target 24 dp — a deliberate exception to the app's 48 dp rule
  ([section 9](#9-accessibility-requirements)), because six rows plus chrome do not fit a 4×4
  card at 48 dp each. Whether the width compensates has not been tested with a finger or TalkBack.
- **Whether the app's theme choice and the widget's device theme disagree in practice.** They do:
  the widget follows the device, the app follows the stored choice — including Material You — so
  Warm Sepia, Cool Slate or a wallpaper scheme leaves the card neutral. See
  [section 15.12](#1512-open--the-widget-does-not-follow-the-chosen-theme).
- **What the release actually costs in performance and memory.** M5 added the measuring
  procedure (cold start, PSS, `gfxinfo` jank percentiles) to
  [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §6, but no release-build numbers have been
  recorded on the reference phone. The JVM suite proves behaviour, not frame time.
- **Whether the bounded solar cache stays bounded on a real database.** The 30-day cutoff and
  the cutoff-day boundary are unit-tested; the launch-time prune has not been observed against
  the on-device `clockwise.db`.
- **Whether a debug StrictMode pass is clean.** The policies are installed only in debuggable
  builds and log rather than throw; no on-device logcat has been read yet.

The device also blocks the obvious manual check — this ROM refuses `adb shell input tap` and
`adb shell input keyevent` — so placing the widget on the home screen is a human action.

**Machine-side preconditions were last checked on 2026-10-18, before M4, and have not been
repeated since.** The then-current provider was registered with the system
(`dumpsys appwidget`: `ClockwiseWidgetReceiver`, `updatePeriodMillis=1800000`, `resizeMode=3`,
`widgetCategory=1`), and the widget does **not** need to be placed for that to be true. That
check also found a real problem: the build installed on the device was from 2026-10-17 23:10 —
**before the widget existed** — so there were no widget receivers and nothing in the picker. The
check must be repeated with the 3.0.0 APK installed (the provider info now also declares
`previewImage`, the five responsive sizes and the 180/110 dp resize floor); the compiled
resource itself is asserted by `criterion16a`, but registration is a platform fact. Since 3.0.0
the application id, namespace and both component names share one prefix:
`com.pj95.clockwise/com.pj95.clockwise.widget.ClockwiseWidgetReceiver` and
`com.pj95.clockwise/com.pj95.clockwise.MainActivity`. Until 2.1.0 the id was
`com.worldclock.offline` while the classes lived in `com.worldclock.app`, which is why the
worksheet used to spell both out; a component name written as `com.clockwise.*` is now wrong,
and so is the `com.worldclock.offline` id the 2.1.0 release still installs under.

The remaining work, step by step, is in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

### 15.11 OPEN — the accessibility pass has not been heard on hardware

The integration pass set `heading()` on every title and section header, `paneTitle` on the label
dialog, and `contentDescription` naming the city on the dialog's field. All of it is asserted as
semantics on the JVM, and none of it has been heard through TalkBack:

- Whether `paneTitle` on an **in-composition** overlay is announced as panes usually are. It is
  the standard Compose idiom for "a pane appeared" and it is the only signal the dialog has, but
  TalkBack's treatment of a pane title inside the same window is not something a JVM test can
  report.
- Whether a field whose `contentDescription` is `Label for Tokyo` is read instead of, or as well
  as, its placeholder. The description is on the same node as the editable text, and the spoken
  order has not been heard.
- Whether heading navigation is a help or a nuisance at this size — the ten headings across five
  surfaces is a judgement, not a measurement.
- Whether the **largest** Android font and display-size settings still fit. A 1.5× Clocks render
  is now captured on the JVM, but the widget at 2.0× text and the largest display size have not
  been seen on the phone.

The enlarged touch targets are also unverified by hand: they were raised to meet the 48 dp
guideline, not from a report that anything was hard to hit.

**The theme picker was added after this pass** (and became four rows in 3.1.0), and it is the one
new surface whose spoken behaviour has not been thought through on hardware either: four radio
rows in a `selectableGroup()`, each named by its theme, with a decorative swatch that is
deliberately silent. The *rendering* is covered — `criterion12c` asserts each row's label and
selection, and `assertEveryControlIsLabelled()` asserts no row is unlabelled — but whether
"Light Monochrome, radio button, selected" is what is actually heard, and whether a radio list is
the right shape for this control, is unjudged. The comparison switch added in 3.1.0 is the same
kind of question: its row is `toggleable`, so it should say "on"/"off", but that has not been
heard.

**The screen reader's input is also blocked.** Turning TalkBack on is the human's step, but the
*output* half cannot be inspected either: `adb shell uiautomator dump` fails on this ROM with
`ERROR: null root node returned by UiTestAutomationBridge`, the same class of limitation as the
refused `input tap`. So the semantics cannot be read off the live tree the way the widget's
provider could be read off `dumpsys appwidget` — Step 2 is by ear or not at all. The exact
announcements to listen for, control by control, are listed in
[HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md).

### 15.12 OPEN — the widget does not follow the chosen theme

The home-screen widget resolves its own colours from the **device's** light/dark setting
([section 5.11](#511-home-screen-widget)), and the mirror deliberately does not redraw on a theme
change — a home screen is not the app's screen. That was invisible while the app had exactly two
palettes whose light/dark split matched the device's: "the widget follows the device" and "the
widget follows the app" produced the same card most of the time. The M4 rebuild kept this
behaviour: the generated picker preview and the home-screen card both resolve the same tokens
against the device, not against the stored app theme. **v2.1 and 3.1.0 leave it unchanged**: the
card is translucent, but its colours still come from the device, and Material You does not change
that.

The palette themes end the old coincidence, and retiring the two dark ones does not restore it.
Choosing Warm Sepia or Cool Slate leaves a neutral grey card on the home screen next to a warm or
cool app; choosing Material You leaves the same neutral card beside a wallpaper-coloured app. The
card can disagree with the app in *both* directions at once (a dark device with Light Monochrome
chosen).

Three options, none chosen yet — the question was put to the user alongside the palette
replacement and this half was left unanswered, so the existing behaviour was kept:

1. **Keep it as it is.** The widget is a glanceable card, not a themed surface; a launcher may
   even look more consistent if the card matches the launcher's own light/dark mode. Cost:
   nothing to build, one documented inconsistency. The 2.1 Settings hint no longer mentions the
   widget, so the inconsistency is documented here and in the release notes rather than on
   screen.
2. **Give the widget the app's palettes too.** The colours already resolve through a token
   interface, so this means resolving from the *stored* theme rather than the device
   configuration, and redrawing on a theme change (which would also need the mirror to stop
   ignoring field 7). Cost: a redraw on every theme change, and a card that can clash with a
   launcher in the opposite mode — the night card on a white home screen. Material You would make
   this worse, because a wallpaper scheme would then have to be mirrored into `RemoteViews`.
3. **Give the widget only the theme's *darkness*.** Resolve the light/dark decision from the
   chosen theme's `isDark` but keep the neutral two-colour card. Now that no shipped app theme is
   dark, this would reduce to "always use the day card" and no longer solve the mismatch it was
   proposed for; it stays listed so the option is not resurrected without noticing that.
   Material You's `isDark` follows the device anyway.

This needs a decision before the widget's colours are considered finished; it is recorded here
rather than acted on because the user's answer covered the *app's* themes and not the widget's.

### 15.13 OPEN — the 2.1 surfaces have not been exercised by hand

Everything below is asserted on the JVM and none of it has been touched or heard on the
reference phone. The worksheet in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) lists the exact
steps; this is the list of what is *not* proven.

- **Material You on real wallpaper.** `dynamicLight/DarkColorScheme` cannot be resolved in
  Robolectric, so the dynamic path is only checked by construction. Whether a given wallpaper
  produces readable text and a distinguishable selected card is a by-eye check. The static
  fallback and its contrast are asserted.
- **Drag reordering.** The gesture host, the haptic and the write-on-release are unit-tested by
  injecting a drag, but the scroll-versus-drag trade-off, the lift animation and the feel of
  dragging past the list edge have not been tried with a finger. The accessibility Move up /
  Move down path is the fallback if the drag proves awkward.
- **Launcher shortcuts.** `ShortcutManagerCompat` writes to the launcher's store; the tests
  assert the intents, the budget and the labels, but whether HyperOS shows the three static
  actions, how many dynamic cities it actually accepts, whether it reorders them by usage, and
  whether `reportShortcutUsed` changes that order are platform facts. The long-press menu has
  not been opened.
- **The first-launch intro.** The gate and the dismissal are asserted with a fake store. The
  real fresh-install path, the upgrade path (no intro) and the Settings replay have not been
  walked on a device.
- **Translucency.** See [section 15.10](#1510-open--the-widget-has-not-been-placed-on-a-real-home-screen):
  the 94 % fill has not been seen over a busy wallpaper.

### 15.14 OPEN — the icon is confirmed on a launcher; the themed variant is not

`AppIconTest` composes the two vector layers under a **circular** mask and writes
`app/build/screenshots/app_icon.png`, and it inflates the background, foreground, monochrome and
both mipmap declarations. That proves the artwork parses and that the manifest points at it.

**Seen on the device (2026-10-27, `HUMAN_VERIFICATION.md` §4i):** the HyperOS launcher resolves
the `mipmap-anydpi-v26` layer and draws the Concept 3 artwork under its squircle mask with the
66 dp safe zone intact — no part of the dial, the gold hand or the 30° opening touches the mask
edge. The same image appears in App info and in the app drawer, beside the old 2.1.0 tile (two
different apps, as expected). The legacy `mipmap-*dpi` rasters are therefore not the images a
supported device loads, which closes the stale-raster worry for API 26+.

Still unproven:

- **Themed icons.** The monochrome layer is a single colour by construction; whether the system
  tint keeps the hands and the 30° opening readable at 48 px is a by-eye check, and the toggle
  needs `WRITE_SECURE_SETTINGS` (denied to the shell on this ROM).
- **48 px legibility.** The gold inlay is 6.5 dp of a 108 dp canvas — about 3 px at 48 px. It
  reads as an accent on a dark wallpaper; a light wallpaper was not tried.
- **A round-icon launcher style.** HyperOS offers no equivalent switch.

The steps are in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §4i; the results are recorded
there in §7.2.

### 15.15 OPEN — the 3.1.0 alarm and wheel surfaces have not been tried by hand

The wheel, the sound picker and the alarm ring are covered on the JVM, but each depends on
something a JVM cannot produce:

- **The wheels' feel.** `WheelTimePicker` snaps with `rememberSnapFlingBehavior`; how the fling
  settles, whether a 56 dp row is easy to stop on the right hour, and whether the centre line
  reads as a selection rather than a divider are finger judgements.
- **The platform dial under Material You.** The render proves it composes in the app's colours,
  not that the dial's own gestures (tap a number, then the minute ring) are learnable on this
  screen, or that `TimePickerDefaults.colors` keeps the selected number readable against a real
  wallpaper scheme.
- **The device's tone list.** Robolectric has no ringtone store, so every test uses a fake
  catalog. Whether this ROM lists a useful set of alarm tones, whether the first-entry default is
  the same tone the system alarm uses, and whether preview stops when the dialog closes are phone
  facts.
- **The ring itself.** `setAlarmClock` + full-screen intent + `CATEGORY_ALARM` is the standard
  recipe, but the lock screen, Doze, the exact-alarm grant on Android 12/12L, the notification
  permission on 13+, and whether Snooze re-arms at ten minutes have not been observed. The list's
  inline notice when exact alarms are refused is also unexercised on a device that refuses them.
- **The comparison switch.** The line disappearing is asserted; whether the cards look
  unexpectedly empty without it is a taste judgement, and whether the switch is reachable at the
  largest font on a short screen has not been seen.

The steps are in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md); no result is claimed here.

### 15.16 OPEN — the 4.0.0 timer, screen saver and Settings surfaces have not been tried by hand

The Phase 1 build is installed on the Redmi Turbo 4 (Android 16) and deliberately **not launched**:
the user asked for the install without testing. Everything below is a JVM-only claim until it is
run.

- **The timer's ring.** `setExactAndAllowWhileIdle` + a countdown chronometer + a full-screen
  intent is the standard recipe, but whether the ring is on time in Doze, whether the countdown
  notification keeps ticking with the app closed, and whether **Start again** restarts the same
  duration have not been observed.
- **The timer across a reboot.** The row survives and `rescheduleRunning()` runs at process start,
  but a device restart with a running timer has not been exercised; nor has the case where the end
  instant passed while the phone was off.
- **The screen saver.** `FLAG_KEEP_SCREEN_ON`, the immersive hide and the burn-in drift are
  assertions on paper. Whether the display really stays awake for an hour, whether a swipe reveals
  the bars and how, and whether the drift is perceptible (or not) are finger judgements. This ROM
  may also refuse `setShowWhenLocked`-adjacent behaviour, though the screen saver does not use it.
- **The four Settings status rows.** `readAlarmReadiness` reads `canScheduleExactAlarms()`,
  `areNotificationsEnabled()`, `canBypassDnd()` and `isIgnoringBatteryOptimizations()`. Whether
  HyperOS reports each one the way the row expects, whether the four system-screen intents resolve
  on this ROM, and whether the state refreshes on return have not been seen.
- **The mailto path.** Whether a mail app is installed, whether the chooser appears, and whether the
  clipboard fallback fires when it is not, are phone facts. The diagnostic text itself is asserted
  only by reading the code.
- **The wheels and the segmented control on the timer.** Same finger judgements as the alarm
  editor: whether `00:05:00` is easy to set, whether three wheels fit a 360 dp screen at a large
  font, and whether `ALARMS · TIMER` reads as a choice rather than a label.

The steps are in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §4l; no result is claimed here.

---

## 16. Amendment history

| Date | Change |
| --- | --- |
| 2026-10-17 | Document created. Captures the full Stage 0–3 implementation, the device verification, the four instrumented-test defects, and the open questions. |
| 2026-10-17 | Folded in three user decisions: section 11 wording confirmed; manual `AppContainer` confirmed (Hilt rejected); instrumented/Espresso tests deprioritised. Replaced the failing `ClockwiseEndToEndTest` with the two-test `ClockwiseDeviceSmokeTest`, trimmed the unused `androidTest` dependencies, and marked §15.1–§15.6 accordingly. |
| 2026-10-17 | Phase 2, stage 1 — dark theme. Added the dark palette and the token table, `ThemeMode` and its schema field, the settings sheet and gear action, `resolveDarkTheme` wiring, `values-night` launch theme, `SystemBarAppearance`, acceptance criteria 12a–12c, 5 new dark/settings screenshots, and §15.7/§15.8. Suite grew from 107 to 138 tests. |
| 2026-10-18 | Phase 2, stage 2 — time difference and city labels. Added §5.8, §5.9, §8.1 `city_labels = 6`, §8.2 `setCityLabel`, criteria 13a–13d and 14a–14c, §14.6, and the a11y row for context-spoken values. `CityCard` rebuilt as three independent lines after the shared grid truncated `Tokyo (Head Office)`; the label dialog is a modal overlay because a text field in a dialog window cannot be tested on the JVM. Suite grew from 138 to 165 tests, screenshots from 17 to 21. |
| 2026-10-18 | Phase 2, stage 3 — meeting planner. Added §5.10, §5.3's `PLAN A MEETING` action, §5.6's planner row, the §7.2 typography table, four a11y rows, criteria 15a–15d with the grid contract, §14.7, §15.9, and three planner screenshots. The working day (09:00–18:00, the device's own zone, 15-minute slots ending at 23:45) is stated on screen rather than applied silently. The slider is hand-drawn to keep Material's default track out of the app and to keep the shaded band aligned with the marker. Suite grew from 165 to 199 tests, screenshots from 21 to 24. |
| 2026-10-18 | Phase 2, stage 4 — home-screen widget. Added §5.11, §3.2/§3.3 (Glance and its test harness), the widget rows of §4, criteria 16a–16d, §14.8, §15.10, and the `ui/WidgetUiState.kt` · `ui/mapping/WidgetStateMapper.kt` · `ui/widget/*` · `widget/*` files. Documented the three judgement calls with their rejected alternatives: a fixed six-row budget instead of a measured one, `AlarmManager.setAndAllowWhileIdle` on the minute boundary instead of WorkManager, and the device's light/dark setting instead of the app's stored choice. The widget adds no screenshot — Glance draws through `RemoteViews` — and is asserted through `runGlanceAppWidgetUnitTest` instead. Suite grew from 199 to 237 tests in 24 suites; the APK grew from 11.37 MB to about 13.0 MB. |
| 2026-10-18 | Phase 2, stage 5 — integration, accessibility and polish. Added §5.12, `MinTouchTarget`, three acceptance criteria (17a–17c), three navigation tests in `ClockwiseNavigationTest`, §14.9, §15.11 and four 12-hour/dark renders. Every control added in phase 2 is now at least 48 dp tall (the 44 dp circular buttons carry their interaction on a 48 dp box, the segmented control's options are padded out to 48 dp); screen titles and section headers carry `heading()`; the label dialog sets `paneTitle` and names its field after the city. Recorded two test-harness findings: injected touches do not cross into a `ModalBottomSheet`, and Robolectric's stub font measures text at close to zero width, so width assertions are only meaningful for controls the app sizes itself. A clean `clean assembleDebug testDebugUnitTest --no-build-cache` ends at **247 tests, 0 failures, 24 suites, 28 screenshots**. |
| 2026-10-18 | Human-verification prep. Added `HUMAN_VERIFICATION.md` — the worksheet for the two things the JVM cannot do (placing the widget, hearing TalkBack) with a control-by-control list of expected announcements, the exact `adb` probes, and a findings table. Machine-side: the provider is now verified registered on the device (`dumpsys appwidget`), and the build that was installed turned out to be **pre-widget** (2026-10-17 23:10), so the current APK was installed over it. `uiautomator dump` is blocked by this ROM as well, so the live semantics tree cannot be read — recorded in §15.11. |
| 2026-10-18 | Phase 2, stage 9 — the five themes. Replaced the `System / Light / Dark` mode with five palettes (Light Monochrome, True Dark, Warm Sepia, Cool Slate, High Contrast) per the user's decision, so device-following is gone: `ThemeMode`/`resolveDarkTheme` deleted for `AppTheme`/`paletteFor(theme)`, the proto gained `SavedAppTheme` + `optional theme = 7` with read-time migration of the legacy field, and the Settings segmented control became five ≥ 48 dp radio rows with decorative swatches. Added the `mapLandmass` token (`accent` in the four soft themes, white in High Contrast) because a black night wash cannot shade High Contrast's black ocean — the terminator lives on the light continents instead. Rewrote §7.1 as a five-column token table, and added §15.12 (the widget still follows the device, three options, no decision yet). A clean `clean assembleDebug testDebugUnitTest --no-build-cache` ends at **264 tests, 0 failures, 24 suites, 35 screenshots**; `ClockwisePaletteTest` now runs every contrast, inversion and map check over all five themes. |
| 2026-10-26 | v2 M0 — Modern Foundation. Raised `compileSdk` to 35 and build tools to 35.0.0 for Glance 1.2.0, moved Compose to BOM 2025.02.00 / Compose 1.7.8, added Glance 1.2.0 (`glance-appwidget`, `glance-material3`) and Room 2.6.1 via KSP 2.0.21-1.0.28. Added the Room city catalog (`CityEntity`, `CityDao`, `ClockwiseDatabase` v1 with exported schema, idempotent `CitySeeder`, `CityRepository`), wired the shared `Clock` and catalog into `AppContainer`, and seeded the catalog at startup. Removed transitive `ACCESS_NETWORK_STATE` from the merged manifest; the release APK keeps no network permissions. Fixed the API-34-only `LocalDate.ofInstant` calls in `PlannerStateMapper` and `AcceptanceCriteriaTest`. Documented the deferred localization decision (§0.3) and updated the stack, baseline and widget status. Release build, JVM suite (231 tests) and `lintDebug` pass. |
| 2026-10-26 | v2 M1 — Solar Engine. Added the NOAA `SolarEphemeris` (declination + equation of time), `SunriseSunsetCalculator` with the 0.833° apparent-horizon definition, and `SolarEvents` with explicit `NORMAL` / `POLAR_DAY` / `POLAR_NIGHT` kinds. Solar noon is anchored to local noon so the requested civil date survives the date line; events stay absolute across DST. Added the Room v2 `solar_days` cache, `SolarDao`, `SolarRepository` (best-effort cache, coordinate invalidation, pruning) and migration 1 -> 2, wired into `AppContainer`. Fixture tests pin New York, London, Tokyo, Sydney, Singapore and Tromsø against published values, with leap-day, DST, cache, DAO and migration coverage. JVM suite is now **258 tests, 0 failures, 26 suites**; `lintDebug` passes. |
| 2026-10-26 | v2 M2 — Solar Map Time Travel. The map now scrubs the device's current day on a 15-minute grid and returns to live time with `NOW`. Added `MapUiState` / `MapSolarSummary`, the pure `buildMapUiState` and `buildMapSolarSummary` mappers, route-local `rememberSaveable` travel state, solar loading through `SolarRepository` with city/date stale-result rejection, a `DaySlider` without the working-day band, a subsolar Sun marker on the canvas, and the selected city's sunrise/sunset line with explicit polar wording. Shared `CityTimeFormatter.timeText` so solar times follow 12h/24h. Added §6.6, 7 screen tests, 16 mapper tests and `map_screen_time_travel_360dp.png`, and re-recorded every map render. JVM suite is now **282 tests, 0 failures, 27 suites, 36 screenshots**. |
| 2026-10-26 | v2 M3 — Planner Source Conversion and Best Overlap. Added a `FROM` source-chip row (device first, then saved cities) so the planner converts from any saved city: the wall-clock reading stays, the instant moves, the source date follows the source city, and row differences are measured from it. Added `bestWorkingHoursSlot` / `WorkingHoursOverlap`, an exhaustive 96-slot scan that maximises cities inside 09:00–17:59, breaks ties by distance from local noon then the earlier slot, and requires at least two cities at work. The city-list header now carries `BEST <time>` as a button that moves the whole plan, or a non-interactive `NO OVERLAP`. Source state is route-local `rememberSaveable`. Added criteria 15e–15g, eight mapper tests, eight screen tests, four domain tests, `planner_screen_source_360dp.png`, and re-recorded the four existing planner renders. Updated §5.10, §5.6, §7.2 and the a11y tables. JVM suite is now **306 tests, 0 failures, 27 suites, 37 screenshots**. |
| 2026-10-26 | v2 M4 — Glance 1.2.0 widget rebuild. Rebuilt the widget on Glance 1.2.0 with `SizeMode.Responsive` (`220×110`, `250×180`, `250×250`, `320×250`), a measured `widgetRowBudget` over `LocalSize`/font scale (ceiling six, at least one), and `+N more` costing a city row. Added `WidgetUiState`/`WidgetRow`, `buildWidgetUiState`, `WidgetGeometry`, `DayNightColorProvider`, `ClockwiseWidgetContent`, the provider/receiver/ticker/mirror files, `RefreshWidgetAction`, and `WidgetPreviewPublisher` (`setWidgetPreviews` on API 35+, once per `DESIGN_VERSION`, static `widget_preview.png` fallback). The header opens the app; a row deep-links to that city's detail through a one-shot `EXTRA_CITY_ID` with `singleTop`, and `ClockwiseRoot` waits for the graph before navigating. Updated §5.11, §4, §9 (widget touch-target exception), criteria 16a–16e, §12.1–12.3, §13, §14.13, §15.10 and §15.12. The widget has no screenshot (RemoteViews); six new suites plus five criteria take the JVM suite to **365 tests, 0 failures, 33 suites, 37 screenshots**. `lintDebug` is 0 errors / 30 warnings; the release APK is 9.64 MiB with no `INTERNET`/`ACCESS_NETWORK_STATE`. |
| 2026-10-27 | v2 M5 — hardening. Added `SolarCachePolicy` (30-day retention, device-local cutoff) and the launch-time `SolarRepository.pruneBefore()` call; a `CoroutineExceptionHandler` on the application scope; debug-only, log-only `StrictMode` policies gated on `FLAG_DEBUGGABLE`; and a 1.5× font-scale Clocks render. Added `SolarCachePolicyTest` (5), a cutoff-day pruning test in `SolarRepositoryTest` (now 6) and `clocks_screen_large_font_360dp.png` in `LayoutScreenshotTest` (now 37). Updated §8.4, §9, §12.2, §13, §14.14 and §15.10–§15.11. JVM suite is **372 tests, 0 failures, 34 suites, 38 screenshots**; `lintDebug` 0 errors / 30 warnings; debug APK 13.33 MiB, release APK 9.64 MiB. |
| 2026-10-27 | v2.0.0 release preparation. Bumped `versionCode` to 2 and `versionName` to `2.0.0`, keeping `com.worldclock.offline`, the release key, minSdk 26 / targetSdk 34 / compileSdk 35 and the no-network permission set. Added `RELEASE_CHECKLIST.md` and `RELEASE_NOTES_v2.0.0.md`; rewrote `HUMAN_VERIFICATION.md` around the 2.0.0 build, corrected the launch/provider component names, added the four-size row matrix, generated-preview republish procedure, cold/warm/rotation deep-link checks, 28 dp widget-target checks, doze and performance/memory procedures, and result tables. Updated `README.md` and this specification. No public site was maintained after 1.0.10. No store, Uptodown, appeal or resubmission action; `submission/` remains untracked. |
| 2026-10-27 | v2.1.0 — Material You, reordering, shortcuts, intro and widget polish. Added the sixth theme (`MATERIAL_YOU = 5`) with dynamic colours on API 31+ and a static Material 3 light/dark fallback below it; the five existing ids and Light Monochrome default are unchanged. Added Room database v3 `saved_cities` + `MIGRATION_2_3`, `CityOrderStore`, `ClockRepository.reorderCities`/`synchronizeOrder`, and long-press drag plus Move up / Move down actions. Added three static + budgeted dynamic launcher shortcuts with usage reporting. Added the presence-aware `onboarding_completed = 8` intro and its Settings replay. Made the widget card a 94 %-opaque rounded shape, added the `180×110` size and 180/110 dp resize floor, and made ticking screen-aware (1 min on, 5 inexact min off, immediate redraw on screen-on and on time changes). Bumped to `versionCode 3` / `versionName 2.1.0`, added `RELEASE_NOTES_v2.1.0.md`, and updated §2–§13, §14.15, §15.10–§15.13 and this history. No localization, store/Uptodown/appeal action, or claim that the hardware pass ran; `submission/` stays untracked. |
| 2026-10-27 | v3.0.0 — PJ95 identity and "Concept 3" icon. Set `applicationId` and `namespace` to `com.pj95.clockwise`, moved the three source trees with `git mv`, rewrote all package/import/manifest/action/extra/proto references, and renamed the exported Room schema directory (JSON unchanged). Replaced the icon with three vector layers (royal-blue field + facet, ivory/gold dial marked C, monochrome variant) declared in `mipmap-anydpi-v26`, plus five rendered legacy PNG pairs; deleted the unused background colour and grew `AppIconTest` to three tests. Bumped to `versionCode 4` / `versionName 3.0.0`, added `RELEASE_NOTES_v3.0.0.md`, and updated §0.5, §2, §4, §7.4, §11–§14, §15.14, `README.md`, `HUMAN_VERIFICATION.md`, `RELEASE_CHECKLIST.md` and this specification. The launcher label stays "Clockwise"; no UI or behaviour changed; no localization, store action or hardware claim. |
| 2026-10-27 | v3.0.0 device pass. Ran P1–P12 over ADB on the Redmi Turbo 4: all PASS, including the release certificate pulled back off the phone, the generated-preview marker (`DESIGN_VERSION = 1`), and the Room file pulled with its WAL and queried on the host (`user_version = 3`, `saved_cities` order matching a real drag). Extended `ClockwiseDeviceSmokeTest` with a hardware drag that asserts the persisted order and schema 3; the suite is `OK (3 tests)` via `am instrument`, because this ROM blocks Gradle's install step. Measured the release build (cold start 257–299 ms, 5.15 % janky of 330 frames) and rewrote `HUMAN_VERIFICATION.md` §0.2/§7 with the per-check evidence and the exact ROM restrictions that block widget, TalkBack, themed-icon, landscape, large-font and Doze rows. §15.14 now records the icon as seen through the launcher. JVM suite unchanged at **438 tests / 40 suites / 41 screenshots**, lint 0 errors / 30 warnings. The launcher icon, drag reorder and intro are hardware-verified; the widget and accessibility passes remain OPEN. |
| 2026-10-28 | v3.1.0 — alarms, four themes, the comparison switch, wheel time and alarm sound. Added the alarm feature (Room v4 `alarms` + dao/entity/repository, `Alarm`/`RepeatDays`/`nextTrigger`, `setAlarmClock` scheduler, boot/time-zone reschedule, `CATEGORY_ALARM` notification with full-screen ring and snooze, Alarms tab, editor, ringing screen) and per-alarm sound (Room v5 `soundUri`, `AlarmSoundCatalog`, previewable picker, channel per tone). Retired True Dark and High Contrast (ids 1 and 4 unused, dark palette renamed `NightClockwisePalette` for the widget only) and added the Settings `Compared with your local time` switch (proto field 9, absent = on) hiding the difference on cards, the map panel and the detail screen. Replaced the alarm keypad with `WheelTimePicker` by default and the platform `TimePicker` under Material You. Guarded `POST_NOTIFICATIONS` in `AlarmNotifications.show` to clear lint's `MissingPermission`. Added §0.6, §5.16, §8.6 and §15.15, rewrote §5.7, §5.9, §6.1, §7.1, §7.1.1 and §8, updated §2, §4, §11 (18a–18c), §12.2 and §14.17, and replaced every retired-theme render. Bumped to `versionCode 5` / `versionName 3.1.0`. JVM suite **503 tests, 0 failures, 45 suites, 47 screenshots**; `lintDebug` 0 errors / 32 warnings; debug and release builds pass. No localization, store action or hardware claim; `submission/` stays untracked. |
| 2026-10-28 | v4.0.0 Phase 1 — the timer, the screen saver, Settings groups and the local information screens. Added the countdown timer (Room v6 `timer` + `MIGRATION_5_6`, `CountdownTimer`/`TimerFormat`, `setExactAndAllowWhileIdle` scheduler with an early/late re-check, chronometer countdown notification, full-screen ring with Start again, `ALARMS · TIMER` control, three-wheel duration picker, process-start and boot restore) and the in-app screen saver (`ScreenSaverActivity`, immersive, `FLAG_KEEP_SCREEN_ON`, five-position burn-in drift). Regrouped Settings into `APPEARANCE` · `COMPARED WITH YOUR LOCAL TIME` · `ALARMS` · `EXTRAS` · `CHRONOS STUDIO`, added `AlarmReadiness`/`readAlarmReadiness` status rows re-read on `ON_RESUME`, and added the local Privacy, Help and Chronos Studio screens plus `Feedback` (mailto, clipboard fallback, diagnostics). Added `OfflineGuardrailTest`. Added §0.7, §5.17–§5.19, §8.7 and §15.16, updated §5.7, §9, §11, §12.2 and §14.18, and added five renders. Bumped to `versionCode 6` / `versionName 4.0.0`. JVM suite **542 tests, 0 failures, 50 suites, 52 screenshots**; `lintDebug` 0 errors / 34 warnings; debug and release builds pass. No localization, store action or hardware claim; the build is installed but not launched; `submission/` stays untracked. |
| 2026-10-29 | v4.1.0 — alarm refinements. Removed the `ALARMS · TIMER` toggle and the top-right `+`; the hourglass opens the timer, with a close button, `BackHandler` and a saveable deep-link flag. Made alarm cards two lines and set AM/PM small beside the time through `ClockText` + `AlarmTimeDisplay` (one `AnnotatedString`, so TalkBack reads one value). Moved the ring into `AlarmRingingService` (foreground `mediaPlayback`, looping `MediaPlayer` on `USAGE_ALARM`, wake lock, repeating vibration, ten-minute cap), made `alarm-ringing` silent, deleted the legacy `alarms*` channels, and kept the full-screen intent with an Android 14 permission row in Settings. Added per-alarm `SnoozePolicy` (`ALLOWED`/`ONCE`/`NONE`) with a whole-word critical-label suggestion, Room v7 columns + `MIGRATION_6_7`, and policy checks in the screen, the notification and the repository. Added §0.8, extended §5.16/§8.6, updated §0, §11, §12.2, §13, §14.19 and this history, and added two renders. Bumped to `versionCode 7` / `versionName 4.1.0`. JVM suite **565 tests, 0 failures, 52 suites, 54 screenshots**; `lintDebug` 0 errors / 34 warnings / 3 information; debug and release builds pass. No localization, store action or hardware claim; `submission/` stays untracked. |
| 2026-10-31 | v4.1.0 — alarm reorder and swipe-to-delete with Undo. Added `sortOrder` to `AlarmEntity`/`Alarm`, Room v8 + `MIGRATION_7_8` (backfilled by time of day), `AlarmDao.maxSortOrder`/`setSortOrder`/transactional `reorder`, and `save` appends new alarms at the end. Added `AlarmScreen` long-press vertical drag mirroring the city list, with a `LongPress` haptic and `graphicsLayer` translation. Added `components/SwipeToDeleteBox` (right-to-left only, because the same card owns the vertical drag) and wired swipe delete on alarm and city cards. `ClockwiseRoot` now shows `Alarm deleted` / `City removed` with **UNDO** (`SnackbarDuration.Long`); `AlarmRepository.restore` re-inserts the exact row and re-arms it when enabled, and `ClockRepository.restoreCity` restores the previous order and active city. Added Move up / Move down / delete custom actions to both card types. Extended §0.8, §5.3, §5.16, §8.2, §8.6, §11, §12.2, §14.19 and this history; added `8.json`. JVM suite **583 tests, 0 failures, 52 suites, 54 screenshots**; `lintDebug` 0 errors / 33 warnings / 3 information; debug and release builds pass. No localization, store action or hardware claim; `submission/` stays untracked. |
| 2026-11-12 | v1.0.10 — Clockwise rebrand and multi-module Clean Architecture. Renamed every remaining `WorldClock`/`worldclock` identity to `Clockwise`/`clockwise`; moved the Room file to `clockwise.db`, the premium widget style id to `premium_clockwise`, and the release key to `clockwise.jks` with a new `CN=Clockwise` certificate. Split the pure core into `:core:model`, `:core:common` and `:core:domain`, consolidated release history into `docs/CHANGELOG.md`, added `docs/ARCHITECTURE.md`, and removed the GitHub Pages/Netlify site files and `screenshots/`. 1.0.10 is a **fresh install** because the signing key changed. JVM suite and `lintDebug` pass; the signed release APK carries no `INTERNET`/`ACCESS_NETWORK_STATE`. No behaviour change, localization, store action or hardware claim. |
| 2026-11-13 | 1.0.10 device pass (automated rows). Fresh-installed the signed release on the reference phone (Redmi Turbo 4, Android 16 / HyperOS V816): `versionCode 11` / `1.0.10`, label `Clockwise`; `clockwise.db` at `pragma user_version = 10` with the five seeded cities, no `world_clock.db`; `CN=Clockwise` certificate; no `INTERNET`/`ACCESS_NETWORK_STATE`; city deep links and the planner shortcut open; six widget providers registered; no crash. MIUI required **Install via USB** before adb installs were allowed, and `run-as` DB checks ran on a temporary debug install (release reinstalled afterwards). Widget placement and TalkBack remain manual (§7.8). |
