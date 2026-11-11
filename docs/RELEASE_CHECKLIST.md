# Release checklist - Clockwise v1.0.10

This is the human-run procedure for cutting the v1.0.10 build - the Clockwise rebrand and module
cleanup pass. It stops at
a **verified, signed local release**. Store submission, Uptodown, appeals and resubmission work are
deliberately **out of scope** for this milestone and must not be started from this checklist.

| | |
| --- | --- |
| Application id | `com.pj95.clockwise` |
| Namespace | `com.pj95.clockwise` |
| Version | `versionCode = 11`, `versionName = "1.0.10"` |
| minSdk / targetSdk / compileSdk | 26 / 34 / 35 |
| Signing | `keystore.properties` + `clockwise.jks` (both local and git-ignored) |
| Certificate SHA-256 | `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |
| Hardware worksheet | [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) |

> **`submission/` is tracked release material.** It contains store copy and images, not secrets.
> Keep keystores, `keystore.properties` and `local.properties` out of it.

Shell variables used below:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.pj95.clockwise"
$aapt = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe"
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat"
```

---

## 0. Preconditions

- [ ] The working tree contains only intended changes; `git status --short` reviewed.
- [ ] `submission/` contains no keystore, `keystore.properties` or `local.properties`.
- [ ] `keystore.properties` exists and points at the release keystore.
- [ ] The release certificate is the new `CN=Clockwise` key in `clockwise.jks`.
      1.0.10 changes the signing key and the database file name, so it is a **fresh install**: an
      older 4.1.0 build must be uninstalled first. Confirm the certificate for authorship only;
      upgradeability from 4.1.0 is intentionally not preserved.
- [ ] `local.properties` points at an installed Android SDK with build-tools 35.0.0.
- [ ] No store console, Uptodown page or appeal form is open.

Check the signing inputs without printing secrets:

```powershell
Test-Path keystore.properties
Get-Content keystore.properties | Select-String "storeFile|keyAlias"
```

---

## 1. Version and source freeze

- [ ] `app/build.gradle.kts` reads `versionCode = 11` and `versionName = "1.0.10"`.
- [ ] `applicationId` is still `com.pj95.clockwise`; namespace is still `com.pj95.clockwise`.
- [ ] `minSdk = 26`, `targetSdk = 34`, `compileSdk = 35`, `buildToolsVersion = "35.0.0"`.
- [ ] No `INTERNET` permission has been added to the manifest or a dependency, and no billing,
      ads, analytics or crash-reporting library has entered the dependency graph.
- [ ] No Burmese/localization resources, strings or UI changes are part of this release.
- [ ] `README.md` and `docs/*.md` describe 1.0.10, not 4.1.0; [CHANGELOG.md](CHANGELOG.md) is the
      single consolidated history and no `RELEASE_NOTES_v*.md` files remain.
- [ ] `submission/` is reviewed and contains no keystore, credentials or local paths.

---

## 2. Clean build

```powershell
.\gradlew.bat clean assembleDebug assembleRelease testDebugUnitTest lintDebug --console=plain
```

Expected:

- [ ] `BUILD SUCCESSFUL`.
- [ ] Debug APK produced at `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Release APK produced at `app/build/outputs/apk/release/app-release.apk`.
- [ ] JVM suite: **0 failures**; expect **583 tests / 52 suites** (record the exact count from §3).
- [ ] `lintDebug`: **0 errors**; expect **33 warnings / 3 information**.
- [ ] No new warning that names a permission or an alarm component. The 33 warnings are the known
      baseline: mostly dependency-update notes, plus unused resources, plurals candidates, a
      vector path, an old target API, a redundant label, the expected `ObsoleteSdkInt` on
      `mipmap-anydpi-v26`, and the notes introduced by the alarm and Phase 1 code. The count is one
      below the alarm-only build because `ic_delete` is now used by the swipe background. The 3
      information items are `AutoboxingStateCreation` on `AlarmEditorSheet.kt`'s hour, minute and
      sound-index state, which are deliberate: those values are nullable or need `rememberSaveable`
      semantics. If a warning names `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` or a receiver, stop:
      the `MissingPermission` class of defect is back. `ObsoleteSdkInt` on `mipmap-anydpi-v26` is
      expected and a false positive: an `<adaptive-icon>` must live behind `-v26`, or
      `IconXmlAndPng` returns.

If the build fails, stop. Do not tag, publish or bump again to "make it pass".

---

## 3. Verification matrix

### 3.1 Tests and lint

- [ ] Read `app/build/test-results/testDebugUnitTest/*.xml`; sum `tests`, `failures`, `errors`,
      `skipped`. Record the totals below.
- [ ] Screenshots regenerated in `app/build/screenshots/`; **54** renders. The alarm set is
      `alarms_screen_360dp.png`, `alarms_empty_360dp.png`, `alarms_exact_notice_360dp.png`,
      `alarms_screen_cool_slate_360dp.png`, `alarm_editor_new_360dp.png`,
      `alarm_editor_existing_360dp.png`, `alarm_editor_dial_360dp.png`,
      `alarm_editor_critical_360dp.png`, `alarm_ringing_360dp.png` and
      `alarm_ringing_critical_360dp.png`; the 3.1.0 comparison pair
      `settings_sheet_comparison_off_360dp.png` / `clocks_screen_comparison_off_360dp.png`; and
      the five Phase 1 renders `timer_setup_360dp.png`, `timer_running_360dp.png`,
      `screen_saver_360dp.png`, `privacy_policy_360dp.png`, `chronos_studio_360dp.png`.
- [ ] No `*_dark_*.png` or `*_high_contrast_*.png` remains; the retired themes are gone from the
      artefacts as well as from the picker.
- [ ] `app/build/reports/lint-results-debug.html` opened; 0 errors.

| Item | Result |
| --- | --- |
| Tests / failures / suites | |
| Screenshots | |
| Lint errors / warnings | |

### 3.2 Version and package identity

```powershell
$aapt = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe"
& $aapt dump badging app\build\outputs\apk\release\app-release.apk |
  Select-String "package:|launchable-activity"
```

- [ ] `package: name='com.pj95.clockwise' versionCode='11' versionName='1.0.10'`.
- [ ] Launchable activity is `com.pj95.clockwise.MainActivity`.
- [ ] The manifest declares `android:icon="@mipmap/ic_launcher"` and
      `android:roundIcon="@mipmap/ic_launcher_round"`:

```powershell
& $aapt dump resources app\build\outputs\apk\release\app-release.apk |
  Select-String "mipmap/ic_launcher" -Context 0,6
```

- [ ] `mipmap/ic_launcher` lists **both** the five `(mdpi .. xxxhdpi)` PNGs and an
      `(anydpi)` XML — the adaptive icon. An APK with only the PNGs has lost the layered icon.
- [ ] `app/build/screenshots/app_icon.png` was reviewed and is the PJ95 "Concept 3" mark
      (royal blue, ivory dial, gold hand), not the 2.1 off-white one.

### 3.3 Signature

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

- [ ] `Verifies`.
- [ ] Certificate DN: `CN=Clockwise, OU=Mobile, O=PJ95 Studio, L=Yangon, ST=Yangon, C=MM`.
- [ ] SHA-256: `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9`.
- [ ] The APK is signed with the **release** key, not the debug key.

### 3.4 Permissions

```powershell
& $aapt dump permissions app\build\outputs\apk\release\app-release.apk
```

- [ ] No `android.permission.INTERNET`.
- [ ] No `android.permission.ACCESS_NETWORK_STATE`.
- [ ] The alarm set is present and reviewed: `USE_EXACT_ALARM` (and the legacy
      `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"`), `RECEIVE_BOOT_COMPLETED`,
      `POST_NOTIFICATIONS`, `VIBRATE`, `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK`,
      `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and the dynamic-receiver
      permission. None of them grants a network path.
- [ ] The permission set is the 4.0.0 baseline **plus exactly three**: `WAKE_LOCK`,
      `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, all required by
      `AlarmRingingService`. If anything else appears here, the build is wrong.

### 3.5 Size

```powershell
Get-Item app\build\outputs\apk\debug\app-debug.apk, app\build\outputs\apk\release\app-release.apk |
  Select-Object Name, Length
```

- [ ] Debug size recorded: **14.57 MiB** (re-measure after any change).
- [ ] Release size recorded: **10.00 MiB** (re-measure after any change).
- [ ] The growth against 4.0.0 is explained by the ringing service, the snooze column, the alarm
      order column and the swipe component, not by a dependency or an asset bundle. The signed
      release is 3.99 MiB; no billing, ads or analytics library is in the APK.

### 3.6 Install and smoke test

The debug and release builds use different signing keys, so choose one track. Both are **fresh
installs** in 1.0.10; uninstall any earlier build first:

```powershell
# Debug track
& $adb uninstall com.pj95.clockwise
& $adb install app\build\outputs\apk\debug\app-debug.apk

# Or release track (removes the debug build first; erases data)
& $adb uninstall com.pj95.clockwise
& $adb install app\build\outputs\apk\release\app-release.apk
```

- [ ] App launches from the launcher and via
      `adb shell am start -n com.pj95.clockwise/com.pj95.clockwise.MainActivity`.
- [ ] The launcher shows the **Concept 3** icon, and it is not cropped by the mask.
- [ ] Alarms, Clocks, Map, planner, detail, settings and the label dialog open.
- [ ] The Settings sheet offers **four** themes (Light Monochrome, Warm Sepia, Cool Slate,
      Material You), never True Dark or High Contrast, and the `Compared with your local time`
      switch is present and on.
- [ ] The alarm editor opens on the **wheels**; under Material You it opens on the **dial**; no
      keypad appears in either, and the live time preview shows **AM/PM** beside the digits.
- [ ] The `SOUND` picker lists the device's alarm tones and previews one.
- [ ] The `SNOOZE` row offers **Every time / Once / Never**, and a medication-like label shows the
      **TURN OFF SNOOZE** suggestion.
- [ ] The alarm list has **no `ALARMS · TIMER` control**; the hourglass opens the countdown and the
      close button returns. `00:05:00` starts and counts down; the shade shows a quiet countdown
      notification; the timer rings at zero.
- [ ] An alarm rings at its time **over the lock screen**, on the alarm stream, with the label as
      the hero; Snooze obeys the chosen policy and the one-shot switches itself off.
- [ ] Long-press an alarm card and drag it: the list rearranges and the order survives a
      force-stop. Moving a card does not change when its alarm rings.
- [ ] Swipe an alarm card left: it is removed and `Alarm deleted` offers **UNDO**. Tap **UNDO**:
      the same alarm returns with its switch, sound, snooze policy and position.
- [ ] Swipe a city card left: `City removed` offers **UNDO**, and Undo restores the city to its
      previous position and active state.
- [ ] Settings → **Full-screen alarms** reports the platform state and opens the system screen.
- [ ] Settings → **Screen saver** opens the black desk clock, hides the bars, and exits on a tap.
- [ ] Settings shows the five groups (`APPEARANCE`, `COMPARED WITH YOUR LOCAL TIME`, `ALARMS`,
      `EXTRAS`, `CHRONOS STUDIO`), the five alarm status rows, and opens Privacy, Help and the
      Studio placeholder without a browser.
- [ ] **Send feedback** opens a mail app (or copies to the clipboard when there is none).
- [ ] Long-press drag reorders a city, and the order survives a relaunch.
- [ ] The launcher's long-press menu shows Add city / Plan / Map and up to three saved cities.
- [ ] A fresh install shows the intro and seeds the five default cities.
- [ ] The widget can be placed, shows the expected rows, can shrink to 2×2, and the card is
      translucent (see [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §2).
- [ ] A city-row deep link opens the detail screen; Back returns to Clocks.
- [ ] No crash in `adb logcat -d | Select-String "FATAL EXCEPTION|AndroidRuntime"`.
- [ ] Reinstall over the same track: data, alarms, the timer and widgets survive (`-r` only).
- [ ] The Room database is at `user_version = 10`, in `clockwise.db`; `saved_cities`, `alarms` and
      `timer` exist, and `alarms` carries `soundUri`, the three snooze columns and `sortOrder`.

---

## 4. Release artefacts

- [ ] Create a clean output folder outside `submission/`:

```powershell
$release = "$env:TEMP\opencode\clockwise-v1.0.10"
New-Item -ItemType Directory -Force $release | Out-Null
Copy-Item app\build\outputs\apk\release\app-release.apk "$release\Clockwise-v1.0.10.apk"
Get-FileHash "$release\Clockwise-v1.0.10.apk" -Algorithm SHA256 |
  Format-List | Out-File "$release\Clockwise-v1.0.10.apk.sha256"
```

- [ ] APK named `Clockwise-v1.0.10.apk`.
- [ ] SHA-256 checksum file created.
- [ ] [CHANGELOG.md](CHANGELOG.md) reviewed and included in the release description.
- [ ] No keystore, `keystore.properties`, `local.properties` or `submission/` content is copied
      into the artefact folder.

---

## 5. Commit and tag

- [ ] Stage only the intended source and documentation files; **never** `submission/`.
- [ ] Commit message, for example: `release: rebrand Clockwise 1.0.10 and clean the module layout`.
- [ ] Confirm the commit does not contain `submission/`:

```powershell
git show --stat --oneline HEAD
git ls-files submission
```

The second command must print nothing.

- [ ] Tag the commit **after** the clean verification:

```powershell
git tag -a v1.0.10 -m "Clockwise 1.0.10"
```

- [ ] Do not push the tag until publication is approved.

---

## 6. Publication (only when approved)

This milestone prepares the release; it does not submit to a store.

- [ ] Upload `Clockwise-v1.0.10.apk` and its checksum to the GitHub release for `v1.0.10`.
- [ ] Verify the published APK's SHA-256 matches the local file.
- [ ] **Do not** publish a website, GitHub Pages or Netlify mirror for this release; the site files
      were removed.
- [ ] **Do not** create or touch an Uptodown listing, store listing, appeal or resubmission.

---

## 7. Rollback

If the release is wrong after publication:

- [ ] Re-point the download at the last known-good v1.0.10 asset rather than shipping a
      half-tested replacement.
- [ ] Do **not** reuse `versionCode 11` for a different binary. A new binary needs
      `versionCode 12` and a new tag.
- [ ] 1.0.10 uses a new signing key and `clockwise.db`; rolling back to 4.1.0 requires an
      uninstall and loses local data. Say so in the rollback note.
- [ ] Record the reason in `SPECIFICATION.md` §16 and in [CHANGELOG.md](CHANGELOG.md).

---

## 8. Sign-off

| | |
| --- | --- |
| Release version | 1.0.10 (11) |
| Build date | 2026-11-12 |
| Built by | Automated build; 3.0.0 hardware pass complete, 3.1.0 through 4.1.0 hardware passes pending |
| Tests / failures | 596 / 0 (54 suites) |
| Lint errors / warnings | 0 / 27 (0 information) |
| Debug APK size | 15.07 MiB (15,806,528 bytes) |
| Release APK size | 3.99 MiB (4,185,395 bytes) |
| Certificate SHA-256 | `cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9` |
| Permissions reviewed | ☑ |
| Hardware pass | ☐ not run ☑ pass with findings ☐ pass ☐ fail — the 3.0.0 rows are in [HUMAN_VERIFICATION.md](HUMAN_VERIFICATION.md) §7.1–§7.4; the 3.1.0 rows are **NOT RUN** (§7.5), the 4.0.0 rows are **NOT RUN** (§7.6), the 4.1.0 rows are **NOT RUN** (§7.7) and the 1.0.10 rows are **NOT RUN** (§7.8) |
| Published | ☑ no ☐ GitHub only ☐ other: |
| Store submission | **not part of this release** |
