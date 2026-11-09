# LibreGram versions

Product version starts at 0.0.1. LIBREGRAM_VERSION_CODE starts at 1262, above the previous installed 1261; never reset Android versionCode to 1. Increase both product version and versionCode for each distributed update. Keep APP_VERSION_NAME/APP_VERSION_CODE as inherited Telegram baseline metadata.

The product version overlay is TMessagesProj/libregram-version.gradle and must be applied after the inherited module build configuration. APK filename generation may still contain inherited version metadata until it is migrated separately; installed version and diagnostic commit ID are authoritative.

Inherited GitHub APK fetching and downloading are disabled, including forced downloads through the old facade. The project releases link now points to LibreGram. No verified automatic LibreGram release feed is implemented yet. The old updater settings sheet may report checking unavailable, not that a newer build does not exist.

Do not publish a stable release based solely on compilation. WS RUNNING/listener_ready means local listener readiness, not Telegram reachability. Verify checks, media, network changes and stop/restore before release.
