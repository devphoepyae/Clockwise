# Uptodown rejection — appeal

- App: **Clockwise**
- App ID: **1000912268**
- Package: `com.worldclock.offline`
- Rejected: **26 October 2026, 13:52**
- Reason: *"Your app was made with an unmodified template. There are already many apps like yours on our platform, so please introduce additional functionality to make it different."*

## Short appeal — paste into Uptodown Console → Contact Us

> Hello Uptodown Editors,
>
> I would like to request a reconsideration of **Clockwise** (App ID **1000912268**, package `com.worldclock.offline`), rejected on 26 October 2026 with the reason "unmodified template."
>
> Clockwise is not a template-based app. It is an original Kotlin/Jetpack Compose application, MIT-licensed with public source code at https://github.com/devphoepyae/Clockwise. Its differentiating features include:
>
> - A real-time **solar terminator map** rendered on a Canvas, showing which parts of the globe are in daylight.
> - A **meeting planner** that finds working-hour overlaps across saved cities.
> - **City labels**, duplicate protection, and **five themes**.
> - **Offline-first** design with **no INTERNET permission**; time-zone and daylight-saving calculations run entirely on-device.
> - A second-aligned ticker, 12h/24h toggle, and **264 JVM tests with 0 failures**.
>
> The app is free, has no ads, no in-app purchases, and no tracking SDKs.
>
> I would be grateful if you could reconsider the rejection or tell me which additional functionality you would like to see. I can provide the source code, test results, or a debug build.
>
> Thank you,
> Pyae Phyo Aung
> dev.phoepyae@gmail.com

## Detailed version — use if an email reply is available

> Subject: Reconsideration request — Clockwise (App ID 1000912268)
>
> Hello Uptodown Editors,
>
> I am the developer of **Clockwise** (`com.worldclock.offline`), submitted on 21 October 2026 and rejected on 26 October 2026 with the reason "Your app was made with an unmodified template."
>
> I believe this assessment is based on the app's category rather than its implementation. Clockwise is not built from a template. It is a custom Kotlin/Jetpack Compose application with public, MIT-licensed source code at https://github.com/devphoepyae/Clockwise.
>
> What makes it different from a standard clock:
>
> 1. **Real-time solar terminator map** — a Canvas-rendered world map that shows the day/night boundary and updates continuously. This is not a static image or a stock widget.
> 2. **Meeting planner** — calculates an hour that falls inside working hours for every city the user follows.
> 3. **Offline-first architecture** — the app declares no `INTERNET` permission. All time-zone, DST, and map calculations run on-device. There is no account, no analytics, and no advertising SDK.
> 4. **City labels and duplicate protection** — users can label cities (for example, "Head Office") and cannot add the same city twice.
> 5. **Five themes** — Light Monochrome, True Dark, Warm Sepia, Cool Slate, and High Contrast.
> 6. **Quality** — 264 JVM tests, 0 failures; native Compose UI; no WebViews; signed release APK with a reproducible SHA-256.
>
> The app is free, has no ads, no in-app purchases, and no tracking. The privacy policy is available inside the app under Settings → Privacy; no public URL is maintained.
>
> I would be happy to provide the source code, test results, or a debug build. If there is a specific feature or quality bar you need, please tell me and I will implement it.
>
> Thank you for your time,
> Pyae Phyo Aung
> dev.phoepyae@gmail.com

## How to send

1. Log in to https://www.uptodown.dev/apps with the same method used to create the account (Google/GitHub).
2. Click **Contact Us** in the left sidebar.
3. Paste the short appeal and include the App ID **1000912268**.
4. If the console offers an email address or a reply thread, use the detailed version.

## If the appeal is declined

Do not create a new developer account or resubmit the identical APK. Choose one of these instead:

- **Add differentiating functionality** and resubmit to Uptodown:
  - Home-screen widgets (2×2 and 4×2) for multiple cities.
  - A time-travel slider on the solar map.
  - Sunrise/sunset and day-length for each city.
  - Import/export of the city list.
- **IzzyOnDroid** — free, curated F-Droid repository; good fit for an MIT-licensed offline app.
- **F-Droid** — free, open-source catalogue; requires a build recipe and review.
- **GitHub Releases** — immediate direct distribution; the APK is already signed.
