# Building LibreGram Android

This guide describes the LibreGram Android build. The source still contains upstream components and identifiers, but the Android application ID is set to the LibreGram namespace.

## Requirements

The repository's current GitHub Actions workflows use:

- JDK 21
- Python 3.11 for the Chaquopy build step
- Android SDK platform 37
- Android Build Tools 36.0.0
- Android NDK 27.2.12479018
- Git with submodule support

A local Android Studio installation with the matching SDK/NDK components can also be used.

## Clone

Clone the LibreGram fork and all required submodules:

```bash
git clone --recursive --shallow-submodules https://github.com/W0rd3r566/libregram-android.git
cd libregram-android
```

If the repository was cloned without submodules:

```bash
git submodule update --init --recursive --depth=1
```

## Telegram API credentials

Obtain your own `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH` from [my.telegram.org](https://my.telegram.org/auth).

Create `local.properties` in the repository root:

```properties
TELEGRAM_APP_ID=your_app_id
TELEGRAM_APP_HASH=your_app_hash
```

Keep this file private. Do not commit credentials, signing passwords, API keys, or keystores.

## Application ID and Firebase

The Android application ID is:

```properties
APP_PACKAGE=com.libregram.app
```

The Firebase configuration at `TMessagesProj/google-services.json` must contain an Android client whose package name is exactly `com.libregram.app`. The inherited upstream file still targets `com.exteraless.app` and must be replaced before building.

Register the LibreGram Android app in the Firebase project used for this fork, download its `google-services.json`, and replace the file at `TMessagesProj/google-services.json`. Do not simply edit the old JSON package-name field: Firebase must generate the matching client configuration.

The Google Maps key is also package/certificate restricted. The inherited key may not work for LibreGram until a matching key is configured.

## Optional integrations inherited from upstream

Depending on which integrations are being tested, the baseline may also require:

- A valid Google Maps API key in the `com.google.android.maps.v2.API_KEY` manifest metadata.
- A numeric `BaseRemoteHelper.CHANNEL_METADATA_ID` for the metadata channel, without the `-100` prefix.

Use your own project credentials. Do not publish private credentials in issues, commits, or chat.

## Debug build

From the repository root:

```bash
./gradlew :TMessagesProj:assembleDebug
```

The APK is normally produced under:

```text
TMessagesProj/build/outputs/apk/debug/
```

For native build targeting, the inherited project supports `arm64-v8a`, `universal`, and `SKIP` through the `NATIVE_TARGET` environment variable. The project currently builds 64-bit ABIs because of its bundled Chaquopy/Python setup.

## Release signing

A release keystore is not included in the repository. To configure local release signing, place your own keystore at:

```text
TMessagesProj/release.keystore
```

Add the corresponding values to the private `local.properties` file:

```properties
KEYSTORE_PASS=your_keystore_password
ALIAS_NAME=your_key_alias
ALIAS_PASS=your_key_password
```

Never commit the keystore or passwords. Back up the release key securely: changing the signing key can prevent users from installing updates over an existing installation.

## GitHub Actions

The GitHub Actions build reads the Telegram API credentials from the repository secret below. The value is the Base64-encoded contents of a private `local.properties` file. It also supports the optional release-signing secret:

- `LOCAL_PROPERTIES`: Base64-encoded `local.properties` containing `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH` (required).
- `RELEASE_KEYSTORE`: the release keystore, base64-encoded (optional for a debug build).

On Linux, create the values with:

```bash
base64 -w0 local.properties
base64 -w0 TMessagesProj/release.keystore
```

Do not paste the resulting values into commits or public issues. Configure them only in the repository's GitHub Actions secrets.

Review the workflow triggers and publishing behavior before enabling release automation. A successful CI build must be confirmed before calling a release ready.

## Build verification

A successful Gradle command is only one part of validation. Before distributing an APK, also verify:

- The app installs and launches on a supported Android device.
- Login, notifications, media, calls, and background services work.
- The launcher icon and app label are correct.
- Firebase and provider authorities match the final application ID.
- The build can be updated using the intended release signing key.
