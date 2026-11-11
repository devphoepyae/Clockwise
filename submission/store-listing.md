# Clockwise — Aptoide Connect submission sheet

Prepared 2026-11-12. Everything below is ready to paste into the Aptoide Connect
submission form. No store submission is planned for 1.0.10; this sheet is kept
current so it can be used if distribution is approved.

## App identity

| Field | Value |
|---|---|
| Store name | Clockwise |
| Package name | `com.pj95.clockwise` |
| Version | 1.0.10 (versionCode 11) |
| Category | Tools / Utility |
| Age rating | Everyone / 3+ |
| Price | Free |
| In-app purchases | None |
| Ads | None |
| Google Play | Not listed |
| Website | None — no public site is maintained |
| Privacy policy | In-app **Settings → Privacy** screen (no public URL) |
| Support email | dev.phoepyae@gmail.com |
| Source code | https://github.com/devphoepyae/Clockwise |
| License | MIT |

## Description (as supplied)

> A minimalist, lightweight, and offline-first Clockwise Android application
> built with native Kotlin and Jetpack Compose. Features real-time Solar
> Terminator map, live second-aligned ticker, and 12h/24h toggle.

### Optional longer description

Clockwise keeps the time in every city you care about, without an account, an
internet connection, ads, or tracking.

- **Offline-first** — no internet permission at all. Times, daylight-saving
  changes and the day/night map are calculated on the device.
- **Real-time solar terminator map** — a canvas-rendered world map shows which
  parts of the globe are in daylight right now.
- **Live second-aligned ticker** — clocks update precisely on the second.
- **12h / 24h toggle** — switch formats instantly, everywhere in the app.
- **City search and labels** — add cities with duplicate protection and give
  them labels such as "Head Office".
- **Meeting planner** — find an hour that falls inside working hours for the
  cities you follow.
- **Five themes** — Light Monochrome, True Dark, Warm Sepia, Cool Slate and
  High Contrast.
- **Small and native** — Kotlin and Jetpack Compose, no web views, no
  third-party advertising or analytics SDKs.

## Release notes (1.0.10)

> Clockwise rebrand release. Offline world clock with a live solar-terminator
> map, a second-aligned ticker, alarms, a countdown timer, home-screen widgets
> and five themes. This build is a fresh install with a new signing key.

## Upload files

| Asset | Path | Spec |
|---|---|---|
| Signed APK | `Clockwise-v1.0.10.apk` (kept beside this sheet, git-ignored) | 4,185,395 bytes; SHA-256 `E2886AC19404D10F8A140C1FE69FA14C12F5371BC4AB303482A835F81B0801AE`; signed; minSdk 26, targetSdk 34 — the same file is attached to the GitHub `v1.0.10` release |
| Icon | `submission/clockwise-icon-512.png` | 512×512 PNG |
| Feature graphic | `submission/clockwise-feature-1024x500.jpg` | 1024×500 JPG |
| Screenshot 1 | `submission/clockwise-screenshot-clocks.jpg` | 640×1280 JPG |
| Screenshot 2 | `submission/clockwise-screenshot-map.jpg` | 640×1280 JPG |
| Screenshot 3 | `submission/clockwise-screenshot-themes.jpg` | 640×1280 JPG |
| Screenshot 4 | `submission/clockwise-screenshot-widget.jpg` | 640×1280 JPG — the app's generated widget preview |

The store images are the padded 2:1 copies; the original full-resolution renders were removed from
the repository in 1.0.10.

## APK verification

```
package: com.pj95.clockwise
versionCode: 11
versionName: 1.0.10
minSdkVersion: 26
targetSdkVersion: 34
label: Clockwise
signer: CN=Clockwise, OU=Mobile, O=PJ95 Studio, L=Yangon, ST=Yangon, C=MM
SHA-256: cd4c512fab2b590f1340c617050bcbe7c06e256e03d9998e886cc298f2cb10c9
```

The only declared app-specific permission is the AndroidX-generated
`com.pj95.clockwise.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. There is no
`INTERNET`, location, storage, camera, microphone, contacts or advertising-ID
permission.

## Policy & compliance check

| Aptoide requirement | Clockwise status |
|---|---|
| Signed APK, not an `.aab` or zip | Pass — signed release APK, verified with `apksigner` |
| Target SDK not below 16 | Pass — targetSdk 34 |
| No invasive adware | Pass — no advertising SDK, no ads |
| No mandatory server / app works offline | Pass — no internet permission; all data local |
| No phishing, coin generators, reward abuse | Pass — not applicable |
| No external APK downloads | Pass — no download or install behaviour |
| No WebViews | Pass — 100% native Compose |
| No sexual, violent, hateful or illegal content | Pass — neutral utility content |
| Original app, no IP infringement | Pass — original Kotlin code, MIT-licensed, no third-party assets |
| Accurate listing and metadata | Pass — description matches the build |
| Stability and core functionality | Pass — JVM suite green, 0 failures; on-device verification recorded in HUMAN_VERIFICATION.md |
| Privacy policy and support contact | Pass — in-app Privacy screen and support email above |
| Age rating | Everyone; no data collection, ads or purchases |
| Advertising ID declaration | Not used — no `AD_ID` permission |
| Data safety / policy declarations | No data collected, no ads, no health/finance/government features, no login |

### Notes for the reviewer

- Clockwise is a pure utility. It has no account system, so no test credentials
  are required.
- The app declares no `INTERNET` permission. Reviewers can verify this with
  `aapt dump permissions`.
- All times are derived from Android's bundled IANA time-zone database.
- The Android system backup flag is enabled; this only covers the user's own
  city list and theme and is described in the privacy policy.

## External prerequisites (account owner must complete)

1. **Aptoide Connect subscription** — review only starts while the account has an
   active paid subscription.
2. **Google Android Developer Verification** — `com.pj95.clockwise` is not on
   Google Play, so it must be registered by a verified developer in the Android
   Developer Console (US$25 one-time for Full Distribution) or Play Console.
   Register SHA-256
   `CD4C512FAB2B590F1340C617050BCBE7C06E256E03D9998E886CC298F2CB10C9`.
3. **Application Ownership Request** — required because the app is not on Google
   Play. Needs the developer's legal name, address, contact details and a
   description of ownership; reviewed only with an active subscription.

## Submission settings

- Distribution countries: all available (or the developer's choice).
- Distribution channels: Aptoide network default.
- Release timing: publish automatically on approval (or schedule).
- Privacy declarations: no ads; no advertising ID; audience 13+ or Everyone;
  no restricted app access; not a health, finance or government app.
