# Watt Home status

Public house battery / solar extras for Ian Day’s family phones, plus an optional live GivEnergy Cloud feed. Watt still writes [`status.json`](status.json) on `main` for Power Up, overnight, savings, and weather. The sideload app can also read the house All-In-One inverter live when you paste a GivEnergy API token.

Feed (no auth, extras):

`https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json`

Live battery (optional, per phone):

`https://api.givenergy.cloud/v1` with a Bearer token. The app only reads inverter `CH2414G328`.

## Download the app

Sideload this APK onto both phones from the rolling GitHub Release. Every successful `main` build refreshes these URLs — no version tag required:

- Latest release: https://github.com/Zekkers/watt-home-status/releases/latest
- Direct APK: https://github.com/Zekkers/watt-home-status/releases/latest/download/watt-home-status.apk

Signed with the family keystore so later updates overwrite-install (same `applicationId` and signing key). The `.jks` is not in git; GitHub Actions uses repo secrets, and a local rebuild uses `android/keystore.properties`.

## Install on two Androids

Do this on each phone.

1. On the phone, open the [direct APK link](https://github.com/Zekkers/watt-home-status/releases/latest/download/watt-home-status.apk) in Chrome (or download `watt-home-status.apk` from the [latest release](https://github.com/Zekkers/watt-home-status/releases/latest)).
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
| **Watt Home · Battery** | 1×1 | Centered `20%` (wrap-content, with `%`, never `20…`). Lower half: `12pm` / `2pm` with the bolt to their right. |
| **Watt Home · Battery + session** | 2×1 | SOC on the left with `battery`; `12pm` / `2pm` as two lines on the right, bolt at the far right. Never `63%pm`. |
| **Watt Home · Glance** | 2×2 | Top identical to 2×1. SOC graph along the bottom. Savings `£36.95 · 9 sess` if it fits. |
| **Watt Home · Overview** | ~3×2 / wide | Overnight slot, 16:00 target, solar W, Power Up as full `12pm - 2pm` or stacked `12pm`/`2pm`. Never `m - 2` or concatenate onto SOC. |
| **Watt Home · Strip** | 4×1 | `63%` \| `12–14` \| weather \| results £ — handy on a dock. |

2×2 keeps a graph slot along the bottom (SOC curve on a 00:00–24:00 / 0–100% scale). Wide Overview letterboxes that same mapping so the curve is not stretched flat. Missing extras stay hidden.

WorkManager’s periodic poll is **15 minutes** when the pack is quiet (Android’s minimum for repeating work). After a successful live GivEnergy GET, if the battery is moving — `|battery_w|` over ~500 W, a Power Up window from `status.json` is in progress, or SOC just changed — the app schedules a one-shot follow-up in **90 seconds**. When things go quiet again it drops back to 15 minutes, so the phone is not polled every minute all day. All widget sizes share one DataStore cache and refresh together. Tap a widget to force a live refresh (and open the app). AppWidget `updatePeriodMillis` is 0; the 15-minute floor is WorkManager, not the launcher.

With a token it reads live SOC, array-1 solar W, and battery power from GivEnergy, plus today’s curve (downsampled to ~15 min on the full poll; fast follow-ups only hit `/system-data/latest` and pin the graph tip). Public `status.json` still supplies Power Up, overnight, 16:00 target, last action, weather, and savings.

## Rebuild from this repo

On a machine with JDK 17+ and the Android SDK:

```bash
cd android
export ANDROID_HOME=/path/to/android-sdk   # or write sdk.dir in local.properties
./gradlew assembleRelease
```

The signed APK is copied to `android/app/release/watt-home-status.apk`. Local signing reads `android/keystore.properties` (gitignored; copy from `android/keystore.properties.example`) and the family `.jks`. That is household sideload only, not Play Store production.

Android project sources live under [`android/`](android/).

## GitHub Actions sideload APK

Pushes to `main`, version tags `v*` (for example `v1.2.5`), and a manual **Run workflow** run unit tests, then `assembleRelease`, then upload **watt-home-status.apk** as a workflow artifact (these expire). Every successful signed build also publishes that same filename to a rolling GitHub Release tagged `latest`, so these URLs stay current without cutting a version tag:

- https://github.com/Zekkers/watt-home-status/releases/latest
- https://github.com/Zekkers/watt-home-status/releases/latest/download/watt-home-status.apk

A `v*` tag still gets its own GitHub Release with the same APK attached. Existing version tags are left alone.

Signing uses the same family key as previous sideload APKs. Add these four Actions secrets once (**Settings → Secrets and variables → Actions**). Do not put the keystore or passwords in git.

| Secret | What to put |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the family `.jks`. Linux: `base64 -w0 android/keystore/watt-family.jks`. macOS: `base64 -i android/keystore/watt-family.jks \| tr -d '\n'` |
| `KEYSTORE_PASSWORD` | Store password from your local `keystore.properties` |
| `KEY_ALIAS` | Key alias (local builds use `watt-family`) |
| `KEY_PASSWORD` | Key password from your local `keystore.properties` |

The first workflow run will fail until those four secrets exist. It will not upload an unsigned or debug-signed APK as the family build.
