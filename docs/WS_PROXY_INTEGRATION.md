# LibreGram WS proxy: experimental debug integration

## Implemented in source, NOT verified by a successful build

- Settings category entry, LibreGram GitHub/channel links and retained design credits.
- Native-backed debug start/stop controls, local port and pool size, optional user-provided Cloudflare/Worker domains, explicitly opt-in TCP fallback.
- Android foreground service (debug manifest only), main-thread lifecycle callbacks, bounded startup/shutdown waits, localhost-only listener, TLS verification enabled.
- Temporary client-wide routing after the listening callback. Saved user proxy settings are never overwritten; stopping restores the current persisted settings. User proxy-change notifications stop WS routing.
- Reachability check using the existing client MTProto proxy checker.
- Bounded structural event journal and copy action, without raw native logs or secrets.
- Pinned source build and MIT license packaging. Feature CI compiles Rust first, then validates private APK configuration and attempts an arm64 debug APK.
- Optional existing RELEASE_KEYSTORE secret restored for signing; matching passwords/alias must remain in LOCAL_PROPERTIES.

## Required private configuration

- LOCAL_PROPERTIES: base64 properties with TELEGRAM_APP_ID and TELEGRAM_APP_HASH.
- GOOGLE_SERVICES_JSON: raw Firebase JSON registered for com.libregram.app; inherited committed configuration has the wrong package.

## Limitations / remaining gates

No successful CI result or device verification has been observed. Committing a workflow only schedules a run; it does not prove it ran or passed. API permissions available to the editing assistant do not currently include Actions run inspection.

The service is deliberately registered only for debug. Release builds show a debug-only explanation. No boot auto-start, quick-settings tile, DC-map editor, reset control or full upstream flag editor is implemented. The existing upstream customization is retained, not extended in this checkpoint. Full code identifier rebranding is not complete: persistence keys, JNI ABI names and component names are intentionally retained pending compatibility review.

Review and test native lifecycle races, stop/start, notification-denied behavior, Android service restrictions, user proxy rotation/disable policies, adding accounts while running, Wi-Fi/mobile switching, media and process termination. Native upstream quiet mode suppresses normal logs, but native fatal errors may still reach upstream logcat; do not claim comprehensive redaction until that path is audited.

Static source review is not a substitute for compilation, minified build validation or a real device test. Do not distribute as a verified release.
