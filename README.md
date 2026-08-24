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
| **Watt Home · Battery** | 1×1 | SOC as `63%` on one line. Bolt inset inside the corner. |
| **Watt Home · Battery + session** | 2×1 | SOC on the left; Power Up as `12pm - 2pm` and a single bolt when opted in. |
| **Watt Home · Glance** | 2×2 | Top-aligned SOC + `12pm - 2pm`, one SOC day curve filling the extra height (optional thin W strip under it), and `£36.95 · 9 sess`. |
| **Watt Home · Overview** | ~3×2 / wide | Overnight slot, 16:00 target, solar W, Power Up `12pm - 2pm`, letterboxed SOC curve, updated time. |
| **Watt Home · Strip** | 4×1 | `63%` \| `12–14` \| weather \| results £ — handy on a dock. |

Glance always keeps a graph slot. 2×2 fills its height with the SOC curve on a 00:00–24:00 / 0–100% scale. Wide Overview letterboxes that same mapping so the curve is not stretched flat. Missing extras stay hidden.

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
