# Quick controls and compatibility checkpoint

APK builds are manual-only in `.github/workflows/libregram-feature.yml` per request. No APK build was requested for this checkpoint.

Implemented source behavior:

- Main dialogs overflow menu: tap toggles the existing WS service, hold opens its settings. Transient states do not enqueue another toggle. The menu label is refreshed when the menu is reopened.
- New copied settings URLs use `libregramm`; incoming `libregramm`, `libregram` and legacy `exteraless` paths resolve in LibreGram. No claim is made that other clients recognize LibreGram links.
- Integration into large inherited Java files is performed by `Tools/integrate-libregram-controls.py`, invoked during Gradle settings evaluation. It validates all anchors, fails on upstream drift and is idempotent. This intentionally modifies tracked source files at build time; review generated diffs before committing a local build. Replacing this mechanism with direct committed Java edits is preferable in a full local checkout.
- Original launcher component names and persistence keys remain for compatibility.

Host validation performed: integration functions executed on actual source, second application produced identical output; generated link prefix and legacy handlers asserted; modified icon and string XML parsed. These checks do NOT establish Java compilation, Android resource compilation or runtime correctness.

Morning acceptance gates:

1. Run `python3 tests/test_libregram_controls.py` and branding checks, then manually build the feature branch.
2. Verify old and new settings links from a chat and from Android link intents.
3. Verify tap/hold on the WS item including during starting/stopping and after errors. Opening a menu does not need a live observer; reopen it for current label.
4. Verify intro, settings tile, chat settings icon row and every launcher icon preview against actual launcher output in both themes and Russian/English.
5. Verify start/stop, notification-denied behavior, failed native startup and repeated fast interactions. Confirm previous user proxy settings are unchanged.
6. Verify media, account changes, mobile/Wi-Fi changes and background lifecycle.

Known remaining limitations: debug-only service, no minified release validation, no guarantee of complete branding coverage across every localized or remotely supplied string, possible residual icon-preview cropping for full-vector variants (the inherited adaptive preview renderer applies bleed), no full DC-map editor/reset or Android runtime test suite yet. No zero-bug or complete-release claim is warranted.
