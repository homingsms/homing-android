# Homing for Android

Homing turns an Android phone into an SMS gateway. Your apps send and receive SMS and MMS through the phone's SIM cards, either over a local HTTP API on the phone itself or through Homing Cloud.

The app can:

- send SMS, data SMS and MMS, with delivery reports and multi-SIM selection
- receive SMS and MMS and forward them to your webhooks
- run a local API server on the phone (local mode)
- connect to Homing Cloud and wake up on push or server events (cloud mode)
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

Push wake-ups for cloud mode use Firebase Cloud Messaging. Put your project's `google-services.json` in `app/`. The Google Services plugin is applied only when that file exists. Without it the app builds and runs normally, push wake-ups are disabled and cloud mode falls back to server-sent events.

`google-services.json` is ignored by Git. CI reads it from the `GOOGLE_SERVICES_JSON` secret.

### Cloud server URL

The default cloud API is `https://api.homingsms.com/mobile/v1`. It is compiled into `BuildConfig.CLOUD_URL` and can still be changed in the app's server settings. To build with a different default, pass `-Phoming.cloudUrl=https://...` or set `HOMING_CLOUD_URL`.

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
| `debug` | Debug key | HTTPS only, cleartext allowed for `127.0.0.1` |
| `debugInsecure` | Debug key | Cleartext allowed everywhere, for testing against plain HTTP servers |
| `insecure` | Release key when configured, otherwise debug key | Cleartext allowed everywhere |
| `release` | Release key when configured, otherwise unsigned | HTTPS only, cleartext allowed for `127.0.0.1` |

## Project layout

- `app/src/main/java/com/homingsms/app/modules`: features (messages, receiver, gateway, local server, webhooks, MMS, encryption, health)
- `app/src/main/java/com/homingsms/app/serialization`: JSON configuration shared by the local API, cloud client, webhooks and settings
- `app/schemas`: Room schema history used for migrations
- `gradle/libs.versions.toml`: dependency versions

## License

Apache License 2.0. See `LICENSE`.
