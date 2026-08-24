# Watt Home status

Public house battery / solar snapshot for Ian Day’s family phones. Watt writes [`status.json`](status.json) on `main`. This repo also holds a sideloadable Android app that reads that file — no login, no tracking.

Feed (no auth):

`https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json`

## Download the app

Sideload this APK onto both phones:

**[android/app/release/watt-home-status.apk](android/app/release/watt-home-status.apk)**

Signed with the family keystore in [`android/keystore/`](android/keystore/) (see that folder’s README). Same key for later updates so you do not have to uninstall first.

## Install on two Androids

Do this on each phone.

1. Copy `watt-home-status.apk` to the phone (AirDrop-style via Drive, USB, Messages, or email to yourself).
2. Open the file. If Android blocks it, tap **Settings** and allow install from that app (Files / Chrome / Drive / Gmail).
3. Install **Watt Home**. There is no account screen — it should open straight onto battery and solar.
4. Leave **battery optimisation** alone unless the widget stays stale; then set Watt Home to **Unrestricted** (Samsung: Settings → Apps → Watt Home → Battery → Unrestricted).

The app needs **Internet** so it can fetch `status.json`. It does not ask for location, contacts, or notifications.

## Add a home-screen widget

On each phone, after the app is installed:

1. Long-press an empty spot on the home screen.
2. Tap **Widgets**.
3. Find **Watt Home**.
4. Pick a size and drag it onto the home screen.
5. Tap any widget to open the same figures in the app.

Sizes in the picker (under **Watt Home**, no search needed):

| Picker name | Size | Shows |
| --- | --- | --- |
| **Watt Home · Battery** | 1×1 | SOC fills the tile (`63%`) on one line. Bolt when `opted_in`. |
| **Watt Home · Battery + session** | 2×1 | SOC on the left; Power Up as one line (`12:00–14:00`) and a single bolt when opted in. No weather overlay. |
| **Watt Home · Glance** | 2×2 | SOC + one-line Power Up, today’s SOC curve, optional signed W strip, and `£36.95 · 9 sess` when gbp is present. |
| **Watt Home · Overview** | ~3×2 | Overnight slot, 16:00 target, solar W, last action, same-aspect curve, updated time. |
| **Watt Home · Strip** | 4×1 | `63%` \| `12–14` \| weather \| results £ — handy on a dock. |

Glance always keeps a graph slot. It prefers `soc_series` (`{t, soc}`) as today’s battery curve on a 00:00–24:00 axis, letterboxed to the same 2×2 plot aspect so a wide Overview does not squash or stretch the shape. `battery_w_series` is an optional signed sparkline with a zero line. Missing extras stay hidden — no invented points. Widget savings is one line (`£36.95 · 9 sess`), never a mid-word ellipsis.

WorkManager refreshes the JSON about every **15 minutes** (and when you open the app, tap refresh, or add/update a widget). Android may stretch that toward 15–30 minutes to save battery.

## Rebuild from this repo

On a machine with JDK 17+ and the Android SDK:

```bash
cd android
export ANDROID_HOME=/path/to/android-sdk   # or write sdk.dir in local.properties
./gradlew assembleRelease
```

The signed APK is copied to `android/app/release/watt-home-status.apk`. Family keystore passwords live in `android/keystore.properties` — they are for household sideload only, not Play Store production.

Android project sources live under [`android/`](android/).
