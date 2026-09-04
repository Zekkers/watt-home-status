# Watt Home status

Public house battery / solar extras for Ian Day’s family phones, plus an optional live GivEnergy Cloud feed. Watt still writes [`status.json`](status.json) on `main` for Power Up, overnight, savings, and weather. The sideload app can also read the house All-In-One inverter live when you paste a GivEnergy API token.

Feed (no auth, extras):

`https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json`

Live battery (optional, per phone):

`https://api.givenergy.cloud/v1` with a Bearer token. The app only reads inverter `CH2414G328`.

## Download the app

Sideload this APK onto both phones:

**[android/app/release/watt-home-status.apk](android/app/release/watt-home-status.apk)**

Signed with the family keystore so later updates overwrite-install (same `applicationId` and signing key). The `.jks` is not in git; GitHub Actions uses repo secrets, and a local rebuild uses `android/keystore.properties`.

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
| **Watt Home · Battery** | 1×1 | Centered SOC (keeps `%`, including `100%` — never `10…`). Small live array-1 solar W under it. Lower half: Power Up times with the bolt to their right, not next to the %. |
| **Watt Home · Battery + session** | 2×1 | SOC and live solar W, Power Up times as two lines, bolt fully visible when opted in, plus a compact sparkline with a left-side `0W` mark. Never `63%pm`. |
| **Watt Home · Glance** | 2×2 | Top identical to 2×1’s header (SOC, `battery`, times, bolt). Power graph along the bottom with a left-side `0W` at the zero line. Savings `£36.95 · 9 sess` if it fits. |
| **Watt Home · Overview** | ~3×2 / 4×2 | Numbers on the left (SOC, solar, overnight, 16:00, peak, Power Up, savings). Graph fills the right pane at that pane’s real size — not a letterboxed strip under the text. `0W` sits on the left of the plot at the zero line. |
| **Watt Home · Strip** | 4×1 | `63%` \| `12–14` \| weather \| results £ — handy on a dock. |

2×1 and 2×2 keep a power sparkline (Options ticks on Overview / the app; compact tiles show solar + battery). A cream `0W` label sits on the left of the zero line whenever a power trace is drawn. Wide Overview puts that plot on the right of the numbers. Missing extras stay hidden.

WorkManager’s periodic poll is **15 minutes** when the pack is quiet (Android’s minimum for repeating work). After a successful live GivEnergy GET, if the battery is moving — `|battery_w|` over ~500 W, a Power Up window from `status.json` is in progress, or SOC just changed — the app schedules a one-shot follow-up in **90 seconds**. When things go quiet again it drops back to 15 minutes, so the phone is not polled every minute all day. All widget sizes share one DataStore cache and refresh together. Tap a widget to open the app immediately; a live refresh runs after the first frame, not before the tap. AppWidget `updatePeriodMillis` is 0; the 15-minute floor is WorkManager, not the launcher.

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

Pushes to `main`, version tags `v*` (for example `v1.2.7`), pull requests from this repo, and a manual **Run workflow** run unit tests, then `assembleRelease`, then upload **watt-home-status.apk** as a workflow artifact (these expire).

A same-repo pull request also publishes a GitHub **pre-release** tagged `pr-<number>` (for example `pr-7`) and comments the phone-tap URL. Re-pushes replace that APK.

A `v*` tag also attaches that APK to a GitHub Release.

Signing uses the same family key as previous sideload APKs. Add these four Actions secrets once (**Settings → Secrets and variables → Actions**). Do not put the keystore or passwords in git.

| Secret | What to put |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the family `.jks`. Linux: `base64 -w0 android/keystore/watt-family.jks`. macOS: `base64 -i android/keystore/watt-family.jks \| tr -d '\n'` |
| `KEYSTORE_PASSWORD` | Store password from your local `keystore.properties` |
| `KEY_ALIAS` | Key alias (local builds use `watt-family`) |
| `KEY_PASSWORD` | Key password from your local `keystore.properties` |

The first workflow run will fail until those four secrets exist. It will not upload an unsigned or debug-signed APK as the family build.
