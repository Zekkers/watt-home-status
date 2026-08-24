# Watt Home status

Public house battery / solar extras for Ian Day’s family phones, plus an optional live GivEnergy Cloud feed. Watt still writes [`status.json`](status.json) on `main` for Power Up, overnight, savings, and weather. The sideload app can also read the house All-In-One inverter live when you paste a GivEnergy API token.

Feed (no auth, extras):

`https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json`

Live battery (optional, per phone):

`https://api.givenergy.cloud/v1` with a Bearer token. The app only reads inverter `CH2414G328`.

## Download the app

Sideload this APK onto both phones:

**[android/app/release/watt-home-status.apk](android/app/release/watt-home-status.apk)**

Signed with the family keystore in [`android/keystore/`](android/keystore/) (see that folder’s README). Same key for later updates so you do not have to uninstall first.

## Install on two Androids

Do this on each phone.

1. Copy `watt-home-status.apk` to the phone (AirDrop-style via Drive, USB, Messages, or email to yourself).
2. Open the file. If Android blocks it, tap **Settings** and allow install from that app (Files / Chrome / Drive / Gmail).
3. Install **Watt Home**.
4. On first launch, paste a **GivEnergy API token** (or skip and keep the public `status.json` feed). Create a token in **Account Settings → Manage API Tokens** on [givenergy.cloud](https://givenergy.cloud). Tap **Save**. **Test** checks the token; **Remove** clears it from this phone. The token stays in EncryptedSharedPreferences on that phone only — never in `status.json`.
5. Later: open the app → gear icon → same token screen.
6. Leave **battery optimisation** alone unless the widget stays stale; then set Watt Home to **Unrestricted** (Samsung: Settings → Apps → Watt Home → Battery → Unrestricted).

If no token is saved, the app keeps polling public `status.json` and shows **Add GivEnergy token for live battery**.

The app needs **Internet**. It does not ask for location, contacts, or notifications. Backup is off.

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
| **Watt Home · Battery** | 1×1 | Three rows only: large `63%`, small `12pm`, small `2pm`. Bolt 2× inset inside the rounded corner, not clipping SOC. Never one-line `12pm - 2pm`, never `6...`. |
| **Watt Home · Battery + session** | 2×1 | SOC on the left; `12pm` / `2pm` as two lines on the right. Never `63%pm`. |
| **Watt Home · Glance** | 2×2 | Left: the same 1×1 stack. Right: SOC graph. Top-aligned. Savings `£36.95 · 9 sess` if it fits without clipping the times. |
| **Watt Home · Overview** | ~3×2 / wide | Overnight slot, 16:00 target, solar W, Power Up as full `12pm - 2pm` or stacked `12pm`/`2pm`. Never `m - 2` or concatenate onto SOC. |
| **Watt Home · Strip** | 4×1 | `63%` \| `12–14` \| weather \| results £ — handy on a dock. |

Glance always keeps a graph slot. 2×2 fills its height with the SOC curve on a 00:00–24:00 / 0–100% scale. Wide Overview letterboxes that same mapping so the curve is not stretched flat. Missing extras stay hidden.

WorkManager refreshes about every **15 minutes** (and when you open the app, tap refresh, or add/update a widget). With a token it reads live SOC, array-1 solar W, and battery power from GivEnergy, plus today’s curve (downsampled to ~15 min). Public `status.json` still supplies Power Up, overnight, 16:00 target, last action, weather, and savings. Android may stretch the timer toward 15–30 minutes to save battery.

## Rebuild from this repo

On a machine with JDK 17+ and the Android SDK:

```bash
cd android
export ANDROID_HOME=/path/to/android-sdk   # or write sdk.dir in local.properties
./gradlew assembleRelease
```

The signed APK is copied to `android/app/release/watt-home-status.apk`. Family keystore passwords live in `android/keystore.properties` — they are for household sideload only, not Play Store production.

Android project sources live under [`android/`](android/).
