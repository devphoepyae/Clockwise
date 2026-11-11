# 🌍 Clockwise

**PJ95 Studio** · v1.0.10

A minimalist, lightweight, offline-first world clock for Android, built with native Kotlin and
Jetpack Compose. Designed for simplicity, performance and a clean monochrome aesthetic.

---

## ✨ Features

- **Offline-first:** every clock, map and solar calculation runs on device. The app requests no
  `INTERNET` or `ACCESS_NETWORK_STATE` permission at all.
- **Home-screen widgets:** Jetpack Glance 1.2.0 translucent Material 3 cards in five sizes from
  2×2 to 5×4, with a measured city-row budget, `+N more` overflow, generated Android 15+ picker
  previews, per-city deep links and a battery-aware ticker.
- **Solar map:** canvas-rendered schematic world map with a live day/night terminator, a 15-minute
  time scrubber and the selected city's sunrise/sunset line.
- **Meeting planner:** read the plan from the device or any saved city, convert the whole plan
  instantly, and jump to the best working-hours overlap.
- **Sunrise and sunset:** offline NOAA solar ephemeris with polar day/night handling and a bounded
  30-day Room cache.
- **Drag to reorder:** long-press a city or alarm card, with the order persisted atomically in Room;
  TalkBack users get Move up / Move down actions instead.
- **Swipe to delete with undo:** swipe a city or alarm card left, then tap **UNDO** in the snackbar.
- **Launcher shortcuts:** Add city, Plan and Map, plus up to three saved cities as dynamic shortcuts.
- **Offline alarms:** one-shot or repeating alarms, a wheel time picker, a previewable ringtone per
  alarm and a full-screen ring over the lock screen. The ring runs in a foreground service on the
  alarm stream, so it survives Doze, silent mode and a locked screen; snooze is per alarm.
- **Countdown timer and stopwatch:** a quiet chronometer notification, pause/resume and a full-screen
  ring with **Start again**. It keeps counting with the app closed and survives a reboot.
- **Screen saver:** a full-screen black desk clock with a slow burn-in drift, launched from Settings.
- **Private by design:** in-app privacy policy, Help and feedback screens. No analytics, tracker or
  billing library.
- **Material You:** optional wallpaper-derived colours on Android 12+, with a static Material 3
  fallback and three fixed monochrome themes.
- **Accessibility:** TalkBack labels, heading navigation, merged cards, reorder and delete actions,
  48 dp targets and a large-font layout pass.

---

## 🆕 What's new in 1.0.10

The **Clockwise** rebrand release. Every `WorldClock` / `worldclock` identifier, file, resource,
Gradle setting and documentation reference now reads `Clockwise` / `clockwise`, while the
application id stays `com.pj95.clockwise`.

The release moves the last `WorldClock` identities to the Clockwise brand:

| Identity | New value |
| --- | --- |
| Room database file | `clockwise.db` |
| Premium widget style id | `premium_clockwise` |
| Release keystore | `clockwise.jks`, certificate `CN=Clockwise` |

Because the signing key changed, 1.0.10 is a **fresh install**: uninstall an older build before
installing it. The database file name also changed, so saved cities and alarms start empty.

- Split the pure Kotlin core into `:core:model`, `:core:common` and `:core:domain`.
- Consolidated every release note into a single [docs/CHANGELOG.md](docs/CHANGELOG.md).
- Moved the long-form documentation under [`docs/`](docs/).
- Removed the obsolete `removed-for-v1.0/` archive and split-APK scratch files.

Full history: [docs/CHANGELOG.md](docs/CHANGELOG.md) — v1.0.10 back to v1.0.0.

---

## 🏗️ Architecture

PJ95 Studio multi-module Clean Architecture with unidirectional data flow:

```
:app
 ├── :feature:alarm ──┬── :core:domain
 │                    ├── :core:data
 │                    ├── :core:ui
 │                    └── :feature:timer
 ├── :feature:map ─────┬── :core:domain
 │                     └── :core:ui
 ├── :feature:timer ───┬── :core:domain
 │                     ├── :core:data
 │                     └── :core:ui
 ├── :core:data ───────┬── :core:domain
 │                     ├── :core:model
 │                     └── :core:common
 ├── :core:ui ─────────┬── :core:domain
 │                     ├── :core:model
 │                     └── :core:common
 ├── :core:domain ─────┬── :core:model
 │                     └── :core:common
 ├── :core:common ──────── :core:model
 └── :core:model
```

| Module | Kind | Contents |
| --- | --- | --- |
| `:core:model` | pure Kotlin | Dependency-free data types: `City`, `CityDirectory`, `Alarm`, `CountdownTimer`, `MinuteOfDay`, `CityTime`, `SolarEvents`, widget ids and theme enums. |
| `:core:common` | pure Kotlin | Pure helpers that depend only on the model: `CityTimeFormatter`, `ClockText`, `SecondTicker`, alarm display extensions. |
| `:core:domain` | pure Kotlin | Contracts and calculations: scheduling/licensing interfaces, projections, `SolarPosition`, `WorldLandmasses`, `SunriseSunsetCalculator`, working-hours policy. |
| `:core:data` | Android library | Room (`ClockwiseDatabase` v10, DAOs, entities, migrations, seeding), Proto DataStore, repositories. |
| `:core:ui` | Android library | Theme, shared components, ambient city art, `*UiState` contracts, state mappers and all Android resources. |
| `:feature:alarm` | Android library | Alarm list, editor and ringing screen. |
| `:feature:map` | Android library | World map screen and flat/globe canvas. |
| `:feature:timer` | Android library | Timer and stopwatch panels, view models and ringing screen. |
| `:app` | application | Composition root, navigation shell, city/planner/settings screens, widgets, services, shortcuts, screen saver and all tests. |

More detail: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 📱 Requirements

- Android 8.0 (API 26) or newer.
- No account, network connection or Google Play Services dependency.
- Generated widget-picker previews require Android 15 (API 35+); older devices use the static
  preview.

---

## 🛠️ Tech stack

- **Language:** Kotlin 2.0.21
- **Package:** `com.pj95.clockwise` (application id and namespace)
- **UI:** Jetpack Compose (BOM 2025.02.00)
- **Widgets:** Jetpack Glance 1.2.0 (`glance-appwidget` + `glance-material3`)
- **Persistence:** Proto DataStore for preferences; Room 2.6.1 (KSP) for the city catalog, solar
  cache, saved-city order, alarms and the timer
- **Time engine:** `java.time` IANA timezone calculations
- **Build:** compileSdk 35 / targetSdk 34 / minSdk 26
- **Localization:** English-only in this release

---

## 📦 Download and installation

Download the latest signed APK from the [Releases](../../releases) page and install it on an
Android device. There is no website or store listing for this release.

---

## 🧪 Verification and release

- [docs/SPECIFICATION.md](docs/SPECIFICATION.md) — behaviour, decisions and the test inventory.
- [docs/HUMAN_VERIFICATION.md](docs/HUMAN_VERIFICATION.md) — the real-device checklist.
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) — the v1.0.10 build, signing and
  publication steps.
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — every release from v1.0.0 to v1.0.10.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — module graph and package map.

```powershell
.\gradlew.bat clean assembleDebug assembleRelease testDebugUnitTest lintDebug
```

---

## 📄 License

MIT — see [LICENSE](LICENSE).
