# Clockwise — Project Dossier
### ဒီဇိုင်း၊ နည်းပညာနှင့် ဗိသုကာ မှတ်တမ်း (Design & Technology Dossier)

| အချက် | တန်ဖိုး |
| --- | --- |
| App အမည် | **Clockwise** (ယခင် Clockwise) |
| Package | `com.pj95.clockwise` |
| လက်ရှိ version | **1.0.10** (`versionCode 11`) |
| ဖန်တီးသူ | **PJ95 Studio** |
| ဆက်သွယ်ရန် | `pj95studio@gmail.com` |
| Platform | Android 8.0+ (`minSdk 26`), `targetSdk 34`, `compileSdk 35` |
| ဘာသာစကား | Kotlin + Jetpack Compose |
| ဒီစာရွက်၏ ရည်ရွယ် | ယနေ့အထိ အကောင်အထည်ဖော်ခဲ့သော ဒီဇိုင်းဆိုင်ရာ ဆုံးဖြတ်ချက်များ၊ နည်းပညာများနှင့် ဗိသုကာကို တစ်နေရာတည်းတွင် စုစည်းရန် |

---

## 1. ထုတ်ကုန် ခြုံငုံသုံးသပ်ချက်

Clockwise သည် **အင်တာနက်မလိုအပ်သော၊ ကိုယ်ပိုင်လုံခြုံမှုကို ဦးစားပေးသော** ကမ္ဘာ့နာရီ၊ နှိုးစက်၊ Timer နှင့် Stopwatch အက်ပ်တစ်ခုဖြစ်သည်။

အဓိက ကတိကဝတ်များ—

- **Zero-internet**: `INTERNET` နှင့် `ACCESS_NETWORK_STATE` permission နှစ်ခုလုံးကို manifest မှ ဖယ်ထားသည်။ အက်ပ်သည် ဒေတာကို ဖုန်းထဲတွင်သာ သိမ်းသည်။
- **No account, no ads, no trackers**: အကောင့်မလို၊ ကြော်ငြာမပါ၊ analytics မပါ။
- **Offline-first**: မြို့အချိန်များ၊ နှိုးစက်များ၊ Timer နှင့် widget များ အားလုံး အော့ဖ်လိုင်း အလုပ်လုပ်သည်။
- **Single-activity Compose app**: မျက်နှာပြင်အားလုံးကို Compose ဖြင့် ရေးထားပြီး Activity တစ်ခုတည်းမှ လမ်းညွှန်သည်။
- **Accessible by design**: TalkBack၊ စာလုံးအရွယ်အစား ကြီးခြင်း၊ 48 dp touch target နှင့် content description များကို စနစ်တကျ ထည့်သွင်းထားသည်။

---

## 2. လက်ရှိ Feature များ

### 2.1 Clocks (ကမ္ဘာ့နာရီ)

- မြို့ ၅ မြို့ကို default အနေဖြင့် ထည့်ပေးသည်—Tokyo, Sydney, Los Angeles, New York, London။
- မြို့တစ်မြို့စီတွင် ဒေသခံအချိန်၊ UTC offset၊ ရက်သတ္တပတ်၊ နေ့/ည icon နှင့် ကိုယ့်အချိန်နှင့် ကွာခြားချက် ပြသည်။
- မြို့အမည် အစားထိုး label သတ်မှတ်နိုင်သည် (ဥပမာ “Tokyo (Head Office)”).
- မြို့များကို long-press ဆွဲ၍ အစီအစဉ်ပြောင်းနိုင်သည်၊ ဘယ်ဘက်သို့ swipe ၍ ဖျက်နိုင်သည် (UNDO ပါ)။
- မြို့ရှာဖွေရန် search sheet၊ မြို့အသေးစိတ် မျက်နှာပြင်၊ 12h/24h ရွေးချယ်မှု။
- မြို့များကို နှိုင်းယှဉ်ပြသော **Meeting Planner** (နာရီ slider ဖြင့် အစည်းအဝေးအချိန် ရှာခြင်း)။

### 2.2 Alarms (နှိုးစက်)

- Google Clock ပုံစံ **circular time picker**—နာရီ/မိနစ် dial နှင့် ဘေးတွင် AM/PM block။
- ဘယ်ဘက်အောက်ထောင့် **keyboard icon** ဖြင့် ဂဏန်း keypad သို့ ပြောင်းနိုင်သည်။
- နှိုးစက်တည်းဖြတ်ရန် sheet—
  - အချိန် (dial/keypad)
  - အပတ်စဉ် နေ့များ (Mon–Sun pills)
  - **Schedule alarm** ရက်စွဲ ရွေးချယ်မှု (တစ်ရက်တည်း)
  - အသံရွေးချယ်မှု (preview/stop ပါ)
  - **Vibrate while ringing** switch
  - Label
  - Save နှင့် အနီရောင် destructive Delete
- နှိုးစက်စာရင်းတွင် floating **+** FAB၊ နှိုးစက်တစ်ခုစီအတွက် on/off switch။
- Snooze policy (Every time / Once / Never) နှင့် ဆေးဝါးဆိုင်ရာ label များအတွက် snooze ပိတ်ရန် အကြံပြုချက်။
- နှိုးစက်မြည်စဉ် **full-screen ringing activity**—lock screen အပေါ်တွင် ပြ၊ screen ကို ဖွင့်၊ label ကို hero အနေဖြင့် ပြ။
- ဖုန်းပြန်ဖွင့်ချိန်၊ အချိန်/timezone ပြောင်းချိန်တွင် alarm များကို အလိုအလျောက် ပြန်စီစဉ်သည်။

### 2.3 Timer (အချိန်တိုင်)

- ကြိုတင်သတ်မှတ် duration များ—**5, 10, 15, 30 မိနစ်**။
- Keypad/wheel များကို default အနေဖြင့် ဖျောက်ထားပြီး keyboard icon ဖြင့် ဖွင့်သည်။
- Idle မျက်နှာပြင်တွင် duration ကို အလယ်တည့်တည့် ပြ၊ Start အောက်တွင် screen-off အလုပ်လုပ်ကြောင်း မြန်မာစာ အကြံပြုချက်။
- အလုပ်လုပ်နေစဉ် circular countdown ring၊ **+1:00** နှင့် reset/restart icon။
- Pause/Resume/Cancel၊ notification တွင် အချိန်တိုင် countdown။
- Timer ပြီးဆုံးလျှင် lock screen အပေါ် ring screen (Start again / Dismiss)။

### 2.4 Stopwatch

- Timer ဘေးတွင် icon-only control အဖြစ် ထည့်ထားသည်။
- Start/Pause/Reset၊ `MM:SS.cc` ပုံစံ၊ နာရီရှိလျှင် `H:MM:SS.cc`။
- In-memory measurement သာ—process သေလျှင် ပျက်သည် (တမင်သက်သက်)။

### 2.5 Settings နှင့် Legal & Support

- Theme ရွေးချယ်မှု—Light Monochrome၊ Warm Sepia၊ Cool Slate၊ Material You (နှင့် light/dark/system)။
- **ALARMS & PERMISSIONS** အပိုင်း—Notifications, Exact alarms, Full-screen alarms, Battery optimization, Do Not Disturb တို့၏ အခြေအနေကို `ALLOWED` / `NEEDS ATTENTION` အနေဖြင့် ပြ။
- နှိပ်လျှင် သက်ဆိုင်ရာ system settings မျက်နှာပြင်သို့ တိုက်ရိုက်သွားသည်။
- **Legal & Support** accordion—
  - Zero-Internet & Privacy Policy
  - App Guide & Chronos Tips
  - Terms of Service
  - Open Source Licenses
  - About & Version Info & Feedback (version 1.0.10, PJ95 Studio, `pj95studio@gmail.com`)
- Intro ကို ပြန်ကြည့်နိုင်သည်။

### 2.6 အခြား

- **Onboarding intro**—ပထမဆုံးဖွင့်ချိန် တစ်ကြိမ်သာ။
- **Screen saver / desk clock**—Settings မှ ဖွင့်နိုင်သော အနက်ရောင် full-screen နာရီ။
- **Home-screen widgets**—Glance ဖြင့် ရေးထားသော Clockwise widget နှင့် Chronos Studio style များ။
- **Launcher shortcuts**—Add city, Plan a meeting၊ ပြီးလျှင် မြို့ ၃ မြို့အထိ dynamic shortcut။

---

## 3. ဗိသုကာ (Architecture)

### 3.1 Multi-module structure

```
Clockwise/
├── app/                  # Composition root, MainActivity, navigation, widgets, services
├── core/
│   ├── domain/           # Pure Kotlin: time, solar, alarms, timers, map math, prefs enums
│   ├── data/             # Room, Proto DataStore, repositories, migrations
│   └── ui/               # Design system, shared components, resources
├── feature/
│   ├── alarm/            # Alarm list/editor/ringing screens + AlarmViewModel
│   ├── timer/            # Timer + Stopwatch screens + view models
│   └── map/              # World map canvas (legacy module, not in 1.0 navigation)
└── ...
```

**ဒီဇိုင်းမူဝါဒ**: dependency သည် အတွင်းသို့သာ စီးသည်။ `app` သည် feature များကို သိသည်၊ feature များသည် `core` ကို သိသည်၊ `core/domain` သည် Android ကို မမှီခိုပါ။ ထို့ကြောင့် domain logic ကို JVM ပေါ်တွင် အမြန်စမ်းသပ်နိုင်သည်။

### 3.2 Layers

| Layer | တာဝန် | ဥပမာ |
| --- | --- | --- |
| UI (Compose) | မျက်နှာပြင်၊ interaction | `AlarmScreen`, `TimerPanel`, `ClocksScreen` |
| ViewModel | UI state နှင့် action များ | `ClockwiseViewModel`, `AlarmViewModel`, `TimerViewModel`, `StopwatchViewModel` |
| Repository | data အရင်းအမြစ် ပေါင်းစည်းမှု | `AlarmRepository`, `SavedCitiesRepository`, `TimerRepository` |
| Data source | Room / DataStore | `ClockwiseDatabase`, `UserPreferences` |
| Domain | စည်းမျဉ်းများ | `Alarm.nextTrigger()`, `CountdownTimer`, `SolarEphemeris` |

### 3.3 Dependency injection

- Hilt/Dagger မသုံးပါ။ `AppContainer` (`DefaultAppContainer`) ကို `ClockwiseApp` တွင် တစ်ကြိမ်တည် ဆောက်ပြီး ViewModel factories များက လိုအပ်သလို ယူသည်။
- ဤနည်းသည် build ကို ပေါ့ပါးစေပြီး စမ်းသပ်ရာတွင် fake container ထည့်ရန် လွယ်စေသည်။

### 3.4 State management

- **Kotlin Coroutines + Flow / StateFlow**။
- ViewModel များက `StateFlow<UiState>` ထုတ်ပေးပြီး Compose က `collectAsStateWithLifecycle()` ဖြင့် ဖတ်သည်။
- One-shot events (deep link, snackbar) ကို callback သို့မဟုတ် state field ဖြင့် ကိုင်တွယ်သည်။

### 3.5 Navigation

- **Navigation Compose**၊ single activity၊ route အမည်များကို `Routes` object တွင် စုထားသည်။
- `singleTop` + `onNewIntent` ဖြင့် widget/deep-link များကို တစ်ကြိမ်သာ ကိုင်တွယ်သည်။
- Deep-link extras များကို ဖတ်ပြီးတာနှင့် Intent မှ ဖယ်ရှားသည်—rotation တွင် ထပ်မဖွင့်စေရန်။

---

## 4. နည်းပညာ Stack

| အပိုင်း | နည်းပညာ / version |
| --- | --- |
| ဘာသာစကား | Kotlin 2.x, JVM target 17 |
| UI | Jetpack Compose (BOM `2025.02.00` → Compose 1.7.8), Material 3 |
| Activity | `androidx.activity:activity-compose 1.9.3` |
| Navigation | `androidx.navigation:navigation-compose 2.8.4` |
| Lifecycle | `lifecycle-runtime-ktx`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose 2.8.7` |
| Local DB | Room `2.6.1` (KSP) |
| Preferences | Jetpack Proto DataStore `1.1.1` + protobuf-javalite `3.25.5` |
| Widgets | Glance AppWidget `1.2.0`, Glance Material3 `1.2.0` |
| Billing | Google Play Billing client (Chronos Studio licensing) |
| Images/icons | Vector drawables, bundled Inter font |
| Build | Gradle Kotlin DSL, AGP 8.x, R8/ProGuard |
| Testing | JUnit 4, Robolectric 4.14.1, Roborazzi 1.26.0, Compose UI Test, Room Testing, AndroidX Test |
| Min/target | `minSdk 26`, `targetSdk 34`, `compileSdk 35` |

---

## 5. ဒီဇိုင်းစနစ် (Design System)

### 5.1 အရောင် (Colour)

အက်ပ်တွင် **fixed palette ၃ ခု + Material You** ရှိသည်။ မည်သည့် palette မဆို `ClockwisePalette` data class တစ်ခုတည်းဖြင့် ဖော်ပြပြီး `LocalClockwisePalette` မှတစ်ဆင့် မျက်နှာပြင်အားလုံးက ဖတ်သည်။ Material 3 ၏ ခရမ်းရောင်/အပြာရောင် default ကို လုံးဝ မသုံးပါ။

| Theme | သဘောထား | အဓိက အရောင် |
| --- | --- | --- |
| **Light Monochrome** | မူလ design၊ အဖြူ/အနက် | bg `#F3F3F1`, fg `#111111`, card `#FAFAF8` |
| **Warm Sepia** | စက္ကူ/မင်သုံး နွေးထွေးသော | bg `#F7F4EF`, fg `#2C2521` |
| **Cool Slate** | အေးမြသော မီးခိုးပြာ | bg `#EEF2F5`, fg `#1E293B` |
| **Material You** | wallpaper dynamic colour (Android 12+) | platform-generated; static fallback ရှိ |
| **Alarm Ringing** | နှိုးစက်မြည်စဉ် သီးသန့် | deep navy `#081426` + champagne gold `#C9A86A` |

Palette တွင် `background`, `foreground`, `card`, `primary`, `primaryForeground`, `onPrimaryMuted`, `secondary`, `mutedForeground`, `accent`, `border`, `mapAccent`, `mapLandmass`, `mapNight` နှင့် `isDark` ပါဝင်သည်။

### 5.2 စာလုံး (Typography)

- **Inter** တစ်မျိုးတည်း၊ weights ၄ ခု (Regular/Medium/SemiBold/Bold) ကို APK ထဲ ထည့်ထားသည်—runtime တွင် font ဒေါင်းလုပ်မလုပ်ပါ။
- Material 3 type scale အားလုံးကို Inter သို့ map လုပ်ထားသည်။
- နာရီဂဏန်းများတွင် **tabular figures (`tnum`)** သုံးသည်—စက္ကန့်တိုင်း ဂဏန်းများ ဘေးတိုက် မလှုပ်စေရန်။
- အမည်ပေးထားသော style များ—`ScreenTitle`, `SectionLabel`, `CityTime`, `AlarmClock`, `TimerClock`, `ScreenSaverClock`, `RingingLabel` စသည်။

### 5.3 သဏ္ဌာန်နှင့် အကွာအဝေး (Shape & spacing)

- ထောင့်များသည် rounded rectangle (medium/large)။
- Card များတွင် 1 dp hairline border ဖြင့် နောက်ခံနှင့် ခွဲထားသည်။
- Touch target အနည်းဆုံး **48 dp** (widget row များမှအပ—အထူး ကြေညာထားသော ခြွင်းချက်)။
- စခရင်ဘောင်များတွင် navigation bar inset များကို ထည့်တွက်သည်။

### 5.4 Icon နှင့် အမှတ်တံဆိပ်

- Vector drawable များ၊ monochrome line style။
- App icon—deep royal blue နောက်ခံ၊ ivory dial၊ gold minute hand၊ adaptive icon + monochrome layer။
- နှိုးစက်မြည်စဉ် မျက်နှာပြင်သည် theme မခွဲ—အမြဲ navy/gold။

### 5.5 Motion

- `Crossfade` ဖြင့် Timer mode ပြောင်းခြင်း။
- `animateFloatAsState` ဖြင့် countdown ring ကို ချောမွေ့စွာ လျှော့ချခြင်း။
- Drag-reorder တွင် item များ live ရွေ့သည်။
- Animation များသည် အလုပ်များနေချိန်တွင် battery မကုန်စေရန် ကန့်သတ်ထားသည်။

### 5.6 Accessibility

- TalkBack—heading navigation၊ content description များ၊ selected state ကြေညာချက်။
- နှိုးစက်ကတ်တွင် အချိန် + AM/PM ကို တစ်ကြိမ်တည်း ဖတ်သည်။
- မြို့ကတ်/နှိုးစက်ကတ်များတွင် **Move up / Move down / Delete** custom actions။
- စာလုံးအရွယ်အစား 1.5×–2.0× တွင် မျက်နှာပြင်များ မညှပ်စေရန် စမ်းသပ်ထားသည်။
- အရောင် contrast ကို palette တစ်ခုစီအတွက် စမ်းသပ်ထားသည်။

---

## 6. နှိုးစက် အင်ဂျင် (Alarm Engine)

### 6.1 Data model

`Alarm` (domain) ↔ `AlarmEntity` (Room, table `alarms`)—

| Field | အဓိပ္ပာယ် |
| --- | --- |
| `id` | auto-increment |
| `hour`, `minute` | device timezone အတွင်း 0–23 / 0–59 |
| `label` | အသုံးပြုသူ စာသား |
| `enabled` | ဖွင့်/ပိတ် |
| `repeatDays` | bitmask; Monday=bit 0 … Sunday=bit 6; `0` = one-shot |
| `scheduledDateEpochDay` | တစ်ရက်တည်း သတ်မှတ်လျှင် ထို epoch day; `null` = repeat rule လိုက်နာ |
| `vibrate` | မြည်စဉ် တုန်ခါမှု |
| `soundUri` | ရွေးထားသော အသံ URI; `""` = device default |
| `snoozePolicy` | `SnoozePolicy.ordinal` |
| `snoozesUsed` | လက်ရှိ ring cycle တွင် snooze အကြိမ် |
| `snoozedAtEpochMillis` | snooze ပြန်မြည်ရမည့် အချိန် |
| `sortOrder` | အသုံးပြုသူ သတ်မှတ်ထားသော အစီအစဉ် |

### 6.2 Trigger ရွေးချယ်မှု

`Alarm.nextTrigger(now, zone)` သည်—

1. `scheduledDateEpochDay` ရှိလျှင် ထို ရက်စွဲတည်းသာ ရွေးသည်။
2. မရှိလျှင် `repeatDays` bitmask အတိုင်း နောက်လာမည့် နေ့ကို ရွေးသည်။
3. `repeatDays == 0` ဖြစ်လျှင် နောက်တစ်ကြိမ် မြည်ပြီးနောက် ပိတ်သွားသည်။

### 6.3 Scheduling နှင့် ringing

- `AlarmManager.setExactAndAllowWhileIdle()` ဖြင့် exact alarm စီစဉ်သည်။
- `AlarmReceiver` က `AlarmRingingService` (foreground, `mediaPlayback`) ကို စတင်သည်။
- `AlarmRingingActivity` ကို full-screen intent ဖြင့် ဖွင့်သည်—`showWhenLocked`, `turnScreenOn`။
- Service သည် `MediaPlayer` ဖြင့် အသံကို loop လုပ်ပြီး `Vibrator` ဖြင့် တုန်ခါသည်။
- Dismiss/Snooze လုပ်လျှင် service ရပ်ပြီး notification ဖယ်သည်။
- `AlarmRescheduleReceiver` က BOOT_COMPLETED၊ MY_PACKAGE_REPLACED၊ TIME_SET၊ TIMEZONE_CHANGED တွင် alarm များကို ပြန်စီစဉ်သည်။

### 6.4 Snooze

- `Every time`—အကြိမ်အရေအတွက် မကန့်သတ်၊ ၁၀ မိနစ်စီ ပြန်မြည်။
- `Once`—တစ်ကြိမ်သာ snooze ရ၊ ဒုတိယအကြိမ် Dismiss သာ။
- `Never`—snooze လုံးဝ မပြ။
- ဆေးဝါးဆိုင်ရာ label များတွင် editor က `TURN OFF SNOOZE` အကြံပြုသည်။

---

## 7. Timer နှင့် Stopwatch

### 7.1 Timer

- `CountdownTimer` (domain) သည် duration၊ remaining နှင့် end instant ကို ကိုင်တွယ်သည်။
- `TimerScheduler` က exact alarm ဖြင့် အချိန်တိုင်း ပြီးဆုံးချိန်တွင် နှိုးသည်။
- `TimerReceiver` → `TimerRingingActivity` (lock screen အပေါ်)။
- Notification တွင် countdown ကို စက္ကန့်တိုင်း update လုပ်သည်။
- UI တွင် presets ၄ ခု၊ hidden wheels၊ circular ring၊ +1:00၊ reset/restart။

### 7.2 Stopwatch

- `StopwatchViewModel`—`startedAtMillis + accumulatedMillis` ဖြင့် elapsed ကို တွက်သည်။
- ၃၁ ms တိုင်း tick လုပ်သည် (စက္ကန့်ရာပုံ တိုင် အသက်ဝင်စေရန်)။
- Process သေလျှင် ပျက်သည်—တိုင်းတာမှုသည် user data မဟုတ်သောကြောင့်။

---

## 8. ဒေတာ သိမ်းဆည်းမှု (Persistence)

### 8.1 Room database — version 10

| Table | အသုံးပြုမှု |
| --- | --- |
| `cities` | မြို့ catalog (id, အမည်, ဒေသ, timezone, lat/lon) |
| `solar_days` | နေထွက်/နေဝင် cache |
| `saved_cities` | အသုံးပြုသူ သိမ်းထားသော မြို့များ + အစီအစဉ် |
| `alarms` | နှိုးစက်များ |
| `timer` | လက်ရှိ countdown row |
| `widget_licenses` | Chronos Studio license state |
| `widget_configs` | widget တစ်ခုစီ၏ မြို့ ၃ မြို့ ရွေးချယ်မှု |

**Migrations**—`MIGRATION_1_2` မှ `MIGRATION_9_10` အထိ အားလုံး ရှိသည်။ နောက်ဆုံး `9→10` သည် `scheduledDateEpochDay` နှင့် `vibrate` ကော်လံများကို ထည့်သည်။ Migration တစ်ခုစီအတွက် `ClockwiseDatabaseMigrationTest` ရှိသည်။

### 8.2 Proto DataStore

`saved_cities.proto` တွင်—

- `city_ids` (အစီအစဉ်လိုက်)
- `active_city_id`
- `use_24_hour`
- `seeded`
- `city_labels` (map)
- `theme` / `theme_mode` / `theme_mode_setting`
- `onboarding_completed`
- `compare_with_local_time`

Field အသစ်များကို `optional` ဖြင့် ထည့်ထားသဖြင့် အက်ပ်အဟောင်း ရေးထားသော store ကို ဖတ်နိုင်သည်။

---

## 9. Widgets နှင့် Chronos Studio

- **Glance AppWidget** ဖြင့် ရေးထားသည်။
- Main widget—`WORLD TIME` header၊ မြို့တစ်မြို့စီ၏ အချိန်၊ နေ့/ည icon၊ `+N more`။
- Chronos Studio styles—Free Slate, Dial 24h, Market Sessions, Global Clocks, Ambient Grid။
- `WidgetTicker` က မိနစ်တိုင်း update လုပ်သည်။
- `WidgetPreviewPublisher` က Android 15+ တွင် preview များ ထုတ်ပေးသည်။
- `WidgetPinManager` က launcher မှ တိုက်ရိုက် pin လုပ်နိုင်စေသည်။
- Widget id နှင့် Glance id ကြား ပြောင်းလဲမှုကို **public `GlanceAppWidgetManager`** (`getAppWidgetId` / `getGlanceIdBy`) ဖြင့်သာ လုပ်သည်။ Glance ၏ library-internal `AppWidgetId` ကို မသုံးတော့သဖြင့် `lintDebug` သည် error မထုတ်ပါ။
- Premium style များအတွက် Play Billing ဖြင့် license စစ်သည်၊ သို့သော် license state ကို Room မှ ဖတ်သည်။

---

## 10. Permission များနှင့် System Integration

| Permission | အသုံးပြုမှု |
| --- | --- |
| `USE_EXACT_ALARM` | Android 13+ exact alarms |
| `SCHEDULE_EXACT_ALARM` | Android 12/12L (maxSdk 32) |
| `POST_NOTIFICATIONS` | alarm/timer notification |
| `USE_FULL_SCREEN_INTENT` | lock screen ringing |
| `VIBRATE` | alarm vibration |
| `RECEIVE_BOOT_COMPLETED` | reboot တွင် alarm ပြန်စီစဉ် |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | ringing service |
| `WAKE_LOCK` | screen-off ringing |
| `com.android.vending.BILLING` | Chronos Studio |

`INTERNET` နှင့် `ACCESS_NETWORK_STATE` ကို `tools:node="remove"` ဖြင့် ဖယ်ထားသည်။

Settings ရှိ permission row များသည် in-app switch မဟုတ်ဘဲ **status row** များဖြစ်ပြီး သက်ဆိုင်ရာ system screen သို့ deep-link လုပ်သည်။

---

## 11. စမ်းသပ်မှု (Testing & Verification)

### 11.1 JVM suite

- **596 tests, 0 failures** (`:app:testDebugUnitTest`)။
- **Lint: 0 errors, 27 warnings** (`:app:lintDebug`)။ ကျန်ရှိသော warning များသည် dependency အသစ်များ၊ portrait lock နှင့် Compose style အကြံပြုချက်များသာ ဖြစ်သည်။
- Coverage—
  - Domain logic (alarm trigger, timer, timezone, solar, map projection)
  - Repository/DataStore/Room migrations
  - Compose UI (Robolectric) နှင့် acceptance criteria
  - **Roborazzi screenshots**—`app/build/screenshots/` တွင် သိမ်းသည်။

### 11.2 On-device smoke tests

`app/src/androidTest/.../ClockwiseDeviceSmokeTest.kt`—၆ ခု၊ **အားလုံး အောင်မြင်** (တစ်ခုချင်း `am instrument` ဖြင့်)—

1. Default cities နှင့် active city
2. Alarm FAB နှင့် Timer/Stopwatch detours
3. Alarm editor (dial/keypad/schedule/vibrate)
4. Timer presets/start/extend/reset
5. Settings permissions + Legal group
6. Drag reorder + Room schema 10

Run command (single test)—

```powershell
adb shell am instrument -w -e class `
  'com.pj95.clockwise.ClockwiseDeviceSmokeTest#<testName>' `
  com.pj95.clockwise.test/androidx.test.runner.AndroidJUnitRunner
```

> **MIUI ကန့်သတ်ချက်**—`connectedDebugAndroidTest` သည် ဤဖုန်းတွင် install/hang ပြဿနာ ရှိသည်။ တစ်ခုချင်း `am instrument` ဖြင့် ပြေးခြင်းက ပိုယုံကြည်ရသည်။ `adb shell input` နှင့် `uiautomator` သည် `INJECT_EVENTS` မရှိသဖြင့် ပိတ်ထားသည်။
>
> **Screen timeout**—ဤဖုန်း၏ screen timeout သည် ၁ မိနစ်သာ ဖြစ်ပြီး shell မှ ပြောင်း၍ မရပါ။ ထို့ကြောင့် test တစ်ခုစီသည် `FLAG_KEEP_SCREEN_ON` ကို `@Before` တွင် ထည့်ထားသည်။ မဟုတ်လျှင် မိနစ်အတွင်း screen ပိတ်ပြီး activity pause ဖြစ်ကာ Compose hierarchy မတွေ့တော့ပါ။
>
> **LazyColumn scrolling**—မြို့ကတ်များကို `performScrollTo()` ဖြင့် မဟုတ်ဘဲ list ကို `performScrollToNode(hasTestTag(...))` ဖြင့် scroll လုပ်သည်။ Lazy item သည် မြင်နိုင်သည့်အထိ compose မဖြစ်သေးသဖြင့် ကတ် node ကို တိုက်ရိုက်ရှာ၍ မရပါ။

### 11.3 Device incident မှတ်တမ်း — “opens then exits”

လက်တွေ့စစ်ဆေးချက်—`am start` က `Activity class does not exist` ပြန်သည်။ `pm list packages` တွင် `com.pj95.clockwise` မပါ။ Logcat တွင် MIUI Launcher ၏ `UninstallController.showDialog` → `PackageManager: Uninstall pkg: com.pj95.clockwise ... of com.miui.home` ကို 21:18:34 တွင် တွေ့ရသည်။

**အဓိပ္ပာယ်**—app သည် crash ဖြစ်၍ ထွက်သွားခြင်း မဟုတ်ပါ။ launcher မှ app ကို uninstall လုပ်လိုက်ခြင်း ဖြစ်သည်။ ပြန်လည် install လုပ်ပြီးနောက် process သည် ၃၀ စက္ကန့်ကျော် အသက်ရှင်နေပြီး activity က focus ရသည်။ နောက်တစ်ကြိမ် ပျောက်သွားပါက `adb install -r --user 0` ဖြင့် ပြန်ထည့်ပြီး logcat တွင် `Launcher.PendingUninstall` ကို ရှာပါ။

### 11.4 Manual checks (လူကိုယ်တိုင်)

- Screen-off/lock-screen နှိုးစက်မြည်ခြင်း
- ဖုန်း reboot ပြီးနောက် alarm ပြန်မြည်ခြင်း
- Doze အတွင်း timer
- TalkBack ဖြင့် နားထောင်ခြင်း
- Widget ကို home screen တွင် နေရာချခြင်း

---

## 12. Build နှင့် Release

### 12.1 Version စည်းမျဉ်း

`vMAJOR.MINOR.PATCH`—minor တစ်ခုလျှင် patch ၀–၁၀၀။ `versionCode` ကို ထို sequence အတိုင်း တွက်သည် (v1.0.0 → 1၊ v1.0.10 → 11)။

### 12.2 Commands

```powershell
$env:JAVA_HOME='C:\Users\Latitude\.jdks\jdk-17.0.20.1+1'

# Debug
.\gradlew.bat :app:assembleDebug

# JVM tests
.\gradlew.bat :app:testDebugUnitTest

# Release (signed when keystore.properties exists)
.\gradlew.bat :app:assembleRelease

# Lint
.\gradlew.bat :app:lintDebug

# Install (MIUI)
adb install -r --user 0 app\build\outputs\apk\debug\app-debug.apk
```

### 12.3 Signing

- `keystore.properties` တွင် store/key အချက်အလက်များ ရှိသည် (git-ignored)။
- Release build သည် R8 minify + resource shrink လုပ်သည်။
- Debug နှင့် release ကို အတူ install မလုပ်နိုင်ပါ (signing key မတူ)။

### 12.4 Output

- Debug—`app/build/outputs/apk/debug/app-debug.apk`
- Release—`app/build/outputs/apk/release/app-release.apk`
- Test—`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

---

## 13. Production Readiness

### ပြီးစီးပြီး

- [x] Zero-internet permission set
- [x] Room v10 + migration tests
- [x] 596 JVM tests အောင်မြင်
- [x] Compose UI + screenshot tests
- [x] Release signing config + R8
- [x] Alarm exact scheduling + boot reschedule
- [x] Full-screen ringing + foreground service
- [x] Notification/exact/battery/DND status rows
- [x] Legal & Support စာသားအားလုံး in-app
- [x] Onboarding gate
- [x] Accessibility labels နှင့် custom actions
- [x] Widget ticker + previews
- [x] `lintDebug`—0 errors (Glance ၏ library-internal API များကို public `GlanceAppWidgetManager` ဖြင့် အစားထိုးပြီး)
- [x] Device smoke tests ၆/၆ အောင်မြင် (first-launch, alarm FAB, alarm editor, timer, settings, drag reorder)
- [x] Debug APK ကို ဖုန်းပေါ်တွင် install လုပ်ပြီး ၃၀ စက္ကန့် stability စစ်ဆေးပြီး

### လုပ်ရန် ကျန်ရှိ

- [ ] **Screen-off/lock-screen alarm ကို ဖုန်းပေါ်တွင် ကိုယ်တိုင်စမ်းသပ်ရန်** (ဤစက်တွင် ADB ဖြင့် မစမ်းနိုင်ပါ)
- [ ] Play Store အတွက် `targetSdk 35+` သို့ မြှင့်ရန် (edge-to-edge ကို စမ်းသပ်ပြီးမှ)
- [ ] Physical device တွင် widget နေရာချခြင်း၊ TalkBack နှင့် Doze စမ်းသပ်ရန်
- [ ] Release APK ကို ဖုန်းပေါ်တွင် ထည့်ပြီး နောက်ဆုံး smoke pass ပြေးရန်
- [ ] Crash reporting မထည့်ရန် ဆုံးဖြတ်ထားသည် (zero-internet မူဝါဒ) — လိုအပ်လျှင် local log သာ

---

## 14. အဓိက ဖိုင် လမ်းညွှန်

| ဖိုင် | တာဝန် |
| --- | --- |
| `app/src/main/java/com/pj95/clockwise/MainActivity.kt` | single activity၊ deep link ကိုင်တွယ်မှု |
| `.../ui/ClockwiseRoot.kt` | shell၊ navigation၊ ViewModel ချိတ်ဆက်မှု |
| `.../ui/screens/ClocksScreen.kt` | မြို့စာရင်း၊ drag/swipe |
| `.../ui/screens/SettingsSheet.kt` | settings၊ permission၊ legal |
| `feature/alarm/.../AlarmScreen.kt` | နှိုးစက်စာရင်း + FAB |
| `feature/alarm/.../AlarmEditorSheet.kt` | နှိုးစက်တည်းဖြတ်မှု |
| `feature/timer/.../TimerPanel.kt` | timer setup/active/finished |
| `feature/timer/.../StopwatchPanel.kt` | stopwatch |
| `core/ui/.../ClockwiseTimePicker.kt` | dial + keypad |
| `core/ui/.../NumericKeypad.kt` | shared keypad |
| `core/ui/.../CityCard.kt` | မြို့ကတ် |
| `core/ui/.../theme/Color.kt` | palette များ |
| `core/ui/.../theme/Type.kt` | Inter typography |
| `core/data/.../ClockwiseDatabase.kt` | Room v10 + migrations |
| `core/data/src/main/proto/saved_cities.proto` | DataStore schema |
| `core/domain/.../alarm/Alarm.kt` | trigger စည်းမျဉ်း |
| `app/.../alarm/AlarmRingingService.kt` | ringing service |
| `app/.../widget/` | Glance widgets |

---

## 15. ဖြစ်ပေါ်တိုးတက်မှု မှတ်တမ်း

| မျိုးဆက် | အဓိက ပြောင်းလဲမှု |
| --- | --- |
| Clockwise 2.0 | မူလ clock list၊ design system၊ 12h/24h |
| Clockwise 2.1 | intro၊ label၊ search |
| Clockwise 3.0 | rebrand၊ package အသစ်၊ accessibility |
| Clockwise 3.1 | theme ပြန်လည်သတ်မှတ်၊ widget |
| Clockwise 4.0 | alarms၊ timer၊ screen saver၊ settings |
| Clockwise 4.1 | alarm refinement၊ drag gesture၊ snooze |
| **Clockwise 1.0** | package → `com.pj95.clockwise`၊ ပိုမိုရှင်းလင်းသော clock list၊ Alarms/Timer/Stopwatch၊ Google Clock ပုံစံ picker၊ Legal & Support၊ permission status rows |

> မှတ်ချက်—`PROJECT_DOSSIER.md` သည် လက်ရှိ source tree (Clockwise 1.0.10) ကို အခြေခံသည်။ `SPECIFICATION.md`/`HUMAN_VERIFICATION.md` များကို 1.0.10 အတွက် ပြန်လည်မွမ်းမံထားပြီး ဖြစ်သည်။

---

## 16. ဝေါဟာရ

| စကားလုံး | အဓိပ္ပာယ် |
| --- | --- |
| **Glance** | Jetpack Compose ဖြင့် widget ရေးသော library |
| **Proto DataStore** | Protocol Buffers ဖြင့် သိမ်းသော အသုံးပြုသူ preference store |
| **Roborazzi** | JVM ပေါ်တွင် Compose screenshot ရိုက်သော စမ်းသပ်မှု tool |
| **Exact alarm** | Doze အတွင်းပါ အချိန်တိတိ မြည်စေသော AlarmManager mode |
| **Full-screen intent** | lock screen အပေါ် မျက်နှာပြင် ဖွင့်ခွင့်ပြုသော notification intent |
| **FAB** | Floating Action Button |
| **Tabular figures** | ဂဏန်းအကျယ် တူညီသော font feature (`tnum`) |
