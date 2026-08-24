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
| **Watt Home · Battery** | 1×1 | Huge SOC as `63%` on one line. Bolt when `next_power_up` is set. |
| **Watt Home · Battery + session** | 2×1 | SOC on the left; Power Up as two lines (`12:00` then `14:00`) or **No Power Up**. |
| **Watt Home · Glance** | 2×2 | SOC + Power Up, today’s battery curve, and batch Power Up results £ when present. |
| **Watt Home · Overview** | ~3×2 | Overnight slot, 16:00 target, solar W, last action, curve, updated time. |
| **Watt Home · Strip** | 4×1 | `63%` \| `12–14` \| weather \| results £ — handy on a dock. |

Glance always keeps a graph slot. It draws today’s samples from `battery_w_series` (and `soc_series` when present). If those arrays are missing or empty, the slot stays empty — **waiting for today’s curve** — with no invented points. Results £ is the batch Power Up credit, not a single session.

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
