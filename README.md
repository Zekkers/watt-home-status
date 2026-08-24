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

## Add the home-screen widget

On each phone, after the app is installed:

1. Long-press an empty spot on the home screen.
2. Tap **Widgets**.
3. Find **Watt Home**.
4. Drag the widget onto the home screen (about 3×2 cells; it can be resized).
5. Tap the widget to open the same figures in the app.

The widget and app show:

| Field | Source |
| --- | --- |
| Battery (SOC %) | `soc_percent` |
| Solar W | `solar_w` |
| Overnight slot | `overnight.start`–`end` and `cap_percent` |
| 16:00 target | `target_1600_percent` |
| Peak window | `peak_window` |
| Next Power Up | `next_power_up` (shows **None scheduled** when `null`) |
| Last action | `last_action` |
| Updated | `updated`, displayed in **Europe/London** |

WorkManager refreshes the JSON about every **15 minutes** (and when you open the app, tap refresh, or add/update the widget). Android may stretch that toward 15–30 minutes to save battery.

## Rebuild from this repo

On a machine with JDK 17+ and the Android SDK:

```bash
cd android
export ANDROID_HOME=/path/to/android-sdk   # or write sdk.dir in local.properties
./gradlew assembleRelease
```

The signed APK is copied to `android/app/release/watt-home-status.apk`. Family keystore passwords live in `android/keystore.properties` — they are for household sideload only, not Play Store production.

Android project sources live under [`android/`](android/).
