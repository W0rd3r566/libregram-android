# Native integration checkpoint

The upstream source revision is pinned to `53f392280a370199ffe5c6ef0ac88808ed17c575`.
`Tools/build-ws-proxy.sh` cross-compiles the JNI crate from source with Cargo.lock,
checks the exported start symbol and copies the upstream MIT license into APK assets.
It currently supports Linux CI and the client arm64-v8a / x86_64 ABI set.

The Java adapter retains upstream JNI package names intentionally; renaming these
without changing native symbols and the cached callback class breaks the JNI ABI.
It uses AndroidX Keep annotations, defers library loading and dispatches callbacks
to the main thread. Raw logs are discarded until redaction is implemented.

This checkpoint is not a completed proxy integration. The script has NOT been run
in an Android build environment yet and is NOT wired into the APK workflow yet.
A service, connection ownership/restoration, actual settings controls and tests are
still required. The visible settings screen remains a preview.

CI wiring must install a Rust toolchain compatible with upstream rust-toolchain.toml
and run `bash Tools/build-ws-proxy.sh` before Gradle assembles the application.
Local build scratch files are placed under `.ws-proxy-build/` and must not be committed.
