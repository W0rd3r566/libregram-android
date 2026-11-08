# LibreGram WS proxy integration status

## Implemented

- A WS proxy entry in LibreGram settings, before Other.
- A preliminary settings screen that explicitly states that the native engine is not integrated.
- LibreGram channel and source links; upstream design attribution retained.
- Feature APK workflow in `.github/workflows/libregram-feature.yml`.

## Not implemented or verified

- Rust/JNI packaging, foreground service, transport controls, automatic client connection and restoration, diagnostic checks.
- Full branding/identifier migration.
- APK build and device tests. A committed workflow is not proof of a successful build.

## Build prerequisites

In the repository Actions secrets configure:

- `LOCAL_PROPERTIES`: base64-encoded private properties containing your own `TELEGRAM_APP_ID` and `TELEGRAM_APP_HASH`.
- `GOOGLE_SERVICES_JSON`: raw JSON downloaded from Firebase for an Android app registered as `com.libregram.app`.

The feature workflow checks these prerequisites before setting up the toolchains. It never prints credentials. The inherited Firebase configuration targets a different application ID; do not manually relabel it.

Pushes to `feature/libregram-ws-settings` and manual dispatches trigger the feature workflow. A successful run uploads an arm64 debug APK, not a verified release or a completed WS proxy implementation.

## Next implementation gates

1. Pin the tg-ws-proxy-rs revision and preserve its MIT license and attribution.
2. Build its JNI library from source for the client ABI set; keep JNI class names, callbacks and shrinker rules consistent.
3. Add an Android service with explicit start/stop/error states and foreground notification, using a localhost-only listener.
4. Connect the client only after the native listening callback; save and conditionally restore the previous proxy configuration.
5. Add native-backed settings and diagnostics, TLS verification enabled and no implicit third-party domains or direct TCP fallback.
6. Validate debug and minified builds, JNI symbols, start/stop races, multiple accounts, network switching, background behavior and media on-device.
