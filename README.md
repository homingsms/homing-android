# Homing for Android

Homing turns an Android phone into an SMS gateway. Your apps send and receive SMS and MMS through the phone's SIM cards, either over a local HTTP API on the phone itself or through Homing Cloud.

The app can:

- send SMS, data SMS and MMS, with delivery reports and multi-SIM selection
- receive SMS and MMS and forward them to your webhooks
- run a local API server on the phone (local mode)
- pair with Homing Cloud and wake up on push, with a polling fallback (cloud mode)
- encrypt message content end to end, limit sending rates and respect working hours

## Requirements

- JDK 21 to run Gradle (the app compiles to Java 17 bytecode)
- Android SDK with platform 35 and recent build tools; set `ANDROID_HOME` or `sdk.dir` in `local.properties`
- A phone or emulator with Android 7.0 (API 24) or newer; telephony is required to send real messages

## Setup

```sh
git clone <repo-url> homing-android
cd homing-android
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### Firebase (optional)

Push wake-ups for cloud mode use Firebase Cloud Messaging. Put your project's `google-services.json` in `app/`. The Google Services plugin is applied only when that file exists. Without it the app builds and runs normally, push wake-ups are disabled and cloud mode pulls messages every `pullIntervalSeconds` instead. Each pull holds a short partial wake lock, and an allow-while-idle alarm wakes the phone for a heartbeat when Doze delays the regular loop. Android limits those alarms while the phone is idle, so expect a pull roughly every 10 to 15 minutes in deep Doze.

`google-services.json` is ignored by Git. CI reads it from the `GOOGLE_SERVICES_JSON` secret.

### Cloud server URL

The default Homing Cloud server is `https://api.homingsms.com`. It is compiled into `BuildConfig.CLOUD_URL` and used for pairing codes unless you change the cloud server address in Settings. A `homing://pair?server=...&code=...` link (the QR code in the console) opens the app, prefills the code and asks you to confirm before it pairs with that server. To build with a different default, pass `-Phoming.cloudUrl=https://...` or set `HOMING_CLOUD_URL`. Debug builds also accept `http://` servers such as `http://10.0.2.2:7010` from the emulator.

## Cloud mode

The phone talks to Homing Cloud over the device protocol (`/api/v1/device`):

1. Pair with a pairing code or a `homing://pair` link. The device token (`hd_...`) is stored encrypted with an Android Keystore key.
2. A foreground service sends a heartbeat with battery, network and per-SIM health every `heartbeatIntervalSeconds`, and pulls leased messages when the heartbeat reports pending messages, when an FCM `homing.wake` push arrives, and every `pullIntervalSeconds` as a fallback. A WorkManager job repeats the heartbeat every 15 minutes in case the service is stopped.
3. State changes (`queued`, `sending`, `sent`, `delivered`, `failed`) and received messages go to a Room outbox and are uploaded in batches. The outbox survives restarts and is retried with backoff.
4. Remote settings (pause, intervals, limits, opt-out keywords) are fetched when they change. A `401` from the cloud unpairs the phone.

## Local mode

The local server mirrors the Homing Cloud public API under `http://<phone>:<port>/api/v1`: `/messages`, `/inbox`, `/webhooks` and `/health`, with the same request and response shapes. Requests need `Authorization: Bearer hk_live_...`. Create and revoke keys on the Connect screen; each key has scopes (`messages:send`, `messages:read`, `inbox:read`, `webhooks:manage`, `devices:manage`, `admin`) and only a hash is stored. The OpenAPI document is served at `/api/v1/docs/openapi.json`.

```sh
curl -X POST http://192.168.1.20:8080/api/v1/messages \
  -H "Authorization: Bearer hk_live_..." -H "Content-Type: application/json" \
  -d '{"type":"sms","to":"+15555215554","text":"Hello"}'
```

Webhooks from the phone use the cloud envelope and the `Homing-Signature: t=<unix>,v1=<hex>` scheme, so the same receiver works with both modes.

### Release signing (optional)

Release builds are signed only when all of these are set and the keystore file exists:

| Variable | Purpose |
| --- | --- |
| `SIGNING_KEY_STORE_PATH` | Keystore path, defaults to `app/keystore.jks` |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

Without them `./gradlew assembleRelease` still works and writes an unsigned, minified APK. See `.env.example`.

## Gradle commands

| Command | What it does |
| --- | --- |
| `./gradlew assembleDebug` | Debug APK |
| `./gradlew assembleRelease` | Minified release APK (R8 and resource shrinking) |
| `./gradlew installDebug` | Build and install on a connected device |
| `./gradlew testDebugUnitTest` | JVM unit tests |
| `./gradlew connectedDebugAndroidTest` | Instrumented tests on a device or emulator |
| `./gradlew ktlintCheck` | Kotlin style check (`ktlintFormat` fixes what it can) |
| `./gradlew detekt` | Static analysis |
| `./gradlew lintDebug` | Android lint |
| `./scripts/emulator-smoke.sh` | Boots the `Pixel_8` AVD, installs the debug APK, sends an SMS through the local API to the emulator's own number and checks that it is sent and received |

## Checks

CI (`.github/workflows/android.yml`) runs on every pull request and push:

1. `./scripts/check-legacy-refs.sh`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew lintDebug`
5. `./gradlew testDebugUnitTest`
6. `./gradlew assembleDebug assembleRelease`

ktlint, detekt and Android lint use baselines for findings that existed before the toolchain update (`config/ktlint/baseline.xml`, `config/detekt/baseline.xml`, `app/lint-baseline.xml`). New code must pass without adding to them.

Tags matching `v*` run `.github/workflows/android-release.yml`, which builds a signed release APK from repository secrets and attaches it to a GitHub Release with a SHA-256 checksum.

## Build variants

| Build type | Signing | Network security |
| --- | --- | --- |
| `debug` | Debug key | HTTPS only, cleartext allowed for `127.0.0.1`, `localhost` and `10.0.2.2`; trusts user-installed CAs |
| `debugInsecure` | Debug key | Cleartext allowed everywhere, for testing against plain HTTP servers; trusts user-installed CAs |
| `insecure` | Release key when configured, otherwise debug key | Cleartext allowed everywhere |
| `release` | Release key when configured, otherwise unsigned | HTTPS only, cleartext allowed for `127.0.0.1` and `localhost`; system CAs only |

Debug builds include `DebugCommandReceiver`, which only the shell can call (it requires `android.permission.DUMP`). It is used by the smoke script:

```sh
adb shell am broadcast -n com.homingsms.app/.debug.DebugCommandReceiver -a com.homingsms.app.debug.ENABLE_LOCAL --ei port 8080
adb shell am broadcast -n com.homingsms.app/.debug.DebugCommandReceiver -a com.homingsms.app.debug.START
adb shell am broadcast -n com.homingsms.app/.debug.DebugCommandReceiver -a com.homingsms.app.debug.CREATE_API_KEY --es name test --es scopes admin
```

## Project layout

| Module | Contents |
| --- | --- |
| `:core:model` | Plain Kotlin types for the app state (gateway status and reasons, health, stats, settings, cloud pairing), API DTOs, validation, webhook signatures and key hashing. No Android dependencies |
| `:core:data` | Room database and migrations, typed DataStore settings with the migration from the old preferences, Keystore-backed secret storage, the device protocol client and sync engine, webhook storage and delivery, repositories |
| `:core:telephony` | SIM and subscription helpers, phone number handling, SMS result codes, outgoing SMS limit detection, device health, MMS PDU parsing and composing |
| `:core:server` | The Ktor local API: routes, API key auth, errors and the generated OpenAPI document. Pure JVM, tested with `testApplication` |
| `:core:designsystem` | Compose theme, components and the app shell with bottom bar or navigation rail |
| `:feature:*` | Stateless Compose screens for onboarding, home, messages, inbox, connect, webhooks, settings and about |
| `:app` | Android services, receivers, workers, dependency wiring, the single `MainActivity` with Navigation Compose, and one ViewModel per screen |

`AppStateRepository` in `:app` exposes everything the UI needs as `StateFlow`s of `:core:model` types: gateway status with typed reasons, today's counts, the 24 hour series, per-SIM stats with daily caps, device health, local server info with API keys, cloud pairing and connection state, webhook endpoints and settings, plus paged message and inbox queries.

Room schemas live in `core/data/schemas`.
- `gradle/libs.versions.toml`: dependency versions

## License

Apache License 2.0. See `LICENSE`.
