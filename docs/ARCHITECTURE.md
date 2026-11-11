# Clockwise — Architecture

PJ95 Studio multi-module Clean Architecture for **Clockwise v1.0.10**.

## 1. Module graph

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
| `:core:model` | pure Kotlin (JVM) | Dependency-free data types: `City`, `CityDirectory`, `LandmarkType`, `Alarm`/`RepeatDays`, `CountdownTimer`/`TimerFormat`, `MinuteOfDay`, `WorkingHoursOverlap`, `CityTime`, `TimeDifference`, `MarketSessions`, `SolarEvents`, `SolarEphemeris`, `GeoPoint`, `AppTheme`/`ThemeMode`, `LicenseState`, `WidgetCatalog`. |
| `:core:common` | pure Kotlin (JVM) | Pure helpers that depend only on the model: `CityTimeFormatter`, `ClockText`, `SecondTicker`, and the alarm `timeText`/`clockText` display extensions. |
| `:core:domain` | pure Kotlin (JVM) | Contracts and calculations: `AlarmScheduling`, `TimerScheduling`, `TimerAlerting`, `EquirectangularProjection`, `GlobeProjection`, `SolarPosition`, `WorldLandmasses`, `SunriseSunsetCalculator`, working-hours policy (`isWithinWorkingDay`, `bestWorkingHoursSlot`). No Android, Compose or protobuf types. |
| `:core:data` | Android library | Room (`ClockwiseDatabase` v10, DAOs, entities, migrations, seeding), Proto DataStore (`UserPreferences`, `SavedCities`), repositories, `AppContainer` + `AppContainerProvider` + `Context.appContainer`. |
| `:core:ui` | Android library | Theme/palette/typography, shared components, ambient city art, UI state models (`ClockwiseUiState`, `MapUiState`, `PlannerUiState`, `WidgetUiState`), state mappers, **all** Android resources (namespace `com.pj95.clockwise.core.ui`). |
| `:feature:alarm` | Android library | `AlarmViewModel`, `AlarmScreen`, `AlarmEditorSheet`, `AlarmRingingScreen`. |
| `:feature:map` | Android library | `WorldMapScreen` (2D/3D toggle, comparison tray) and `WorldMapCanvas` (flat plate + orthographic globe). |
| `:feature:timer` | Android library | `TimerViewModel`, `StopwatchViewModel`, `TimerPanel`, `StopwatchPanel`, `TimerRingingScreen`. |
| `:app` | application | Composition root: `MainActivity`, `ClockwiseApp`, `DefaultAppContainer`, navigation shell (`ClockwiseRoot`), city/clocks/planner/settings screens, Glance widgets and receivers, alarm/timer activities and services, shortcuts, screen saver, all tests. |

### Dependency rule

Dependencies point inward only:

```
:app → features → :core:ui / :core:data → :core:domain → :core:common → :core:model
```

`api` edges are used where a module's public signatures expose a lower layer's types, so
consumers do not have to repeat the dependency.

### Contract-first boundaries

- Features never see Room or DataStore types directly: they consume repository interfaces and
  models re-exported from `:core:model` through `:core:data`'s `api` dependencies.
- Feature view models obtain dependencies through the `Context.appContainer` extension declared
  in `:core:data`; `:app` supplies the implementation via `ClockwiseApp : AppContainerProvider`.
- Presentation state (`*UiState`) and its mappers are shared contracts in `:core:ui`, so the shell
  and every feature speak the same types without depending on each other.
- `:feature:alarm` depends on `:feature:timer` only because the alarm screen embeds the countdown
  panel; no other feature-to-feature edge exists.
- Display formatting lives in `:core:common`, not in `:core:model`, so the model module stays
  dependency-free.

## 2. Package map

| Package | Module | Notes |
| --- | --- | --- |
| `com.pj95.clockwise.model` | `:core:model` | `City`, `CityDirectory`, `LandmarkType` |
| `com.pj95.clockwise.model.alarm` | `:core:model` | `Alarm`, `RepeatDays` |
| `com.pj95.clockwise.model.prefs` | `:core:model` | `AppTheme`, `ThemeMode` |
| `com.pj95.clockwise.model.solar` | `:core:model` | `SolarEvents`, `SolarEphemeris` |
| `com.pj95.clockwise.model.time` | `:core:model` | `CityTime`, `MinuteOfDay`, `TimeDifference`, `MarketSessions`, `WorkingHoursOverlap` |
| `com.pj95.clockwise.model.timer` | `:core:model` | `CountdownTimer`, `TimerFormat` |
| `com.pj95.clockwise.model.widget` | `:core:model` | `LicenseState`, `WidgetCatalog` |
| `com.pj95.clockwise.common.alarm` | `:core:common` | `Alarm.timeText`, `Alarm.clockText` |
| `com.pj95.clockwise.common.time` | `:core:common` | `CityTimeFormatter`, `ClockText`, `SecondTicker` |
| `com.pj95.clockwise.domain.*` | `:core:domain` | contracts, projections, solar calculator, working-hours policy |
| `com.pj95.clockwise.data.*` | `:core:data` | `local`, `prefs`, `repository`, container |
| `com.pj95.clockwise.ui.*` | `:core:ui` | `theme`, `components`, `mapping`, `*UiState` |
| `com.pj95.clockwise.ui.*` | `:feature:*` | feature screens and view models |
| `com.pj95.clockwise.*` | `:app` | root shell, widgets, services, shortcuts, DI |

## 3. Offline guardrails

- No `android.permission.INTERNET` in the merged manifest.
- `OfflineGuardrailTest` enforces that only the alarm/timer/notification permission set is
  declared and that network, ads and analytics permissions stay forbidden.
- Verified on the built release APK: no `INTERNET`, no `ACCESS_NETWORK_STATE`.

## 4. Release shrinking

- `release { isMinifyEnabled = true; isShrinkResources = true }`.
- `proguard-rules.pro` keeps only the generated protobuf-lite preference types that R8 cannot see
  through the serializer; Compose, Room and Glance contribute their own consumer rules.

## 5. Verification

| Check | Result |
| --- | --- |
| `:core:model`, `:core:common`, `:core:domain` builds | pass |
| `:app:compileDebugKotlin` | pass |
| `:app:testDebugUnitTest` | pass, 0 failures |
| `:app:lintDebug` | 0 errors, pre-existing warnings only |
| `:app:assembleRelease` (R8) | pass |
| Release APK permissions | no `INTERNET`, no `ACCESS_NETWORK_STATE` |
