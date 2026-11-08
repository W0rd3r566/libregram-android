# Native build

Upstream: valnesfjord/tg-ws-proxy-rs, MIT, revision `53f392280a370199ffe5c6ef0ac88808ed17c575`.

The feature workflow runs `bash Tools/build-ws-proxy.sh` with Rust 1.98.0 and Android NDK 27.2.12479018. The script cross-compiles the locked JNI crate, checks the exported JNI start symbol and copies the MIT license into APK assets. ABI support: arm64-v8a and x86_64; CI currently builds arm64-v8a only.

Java callback names and the io.github.valnesfjord.tgwsproxyrs package are an upstream ABI contract, not LibreGram application branding. AndroidX Keep annotations preserve the adapter under shrinking. Loading is deferred so application startup does not fail merely because a native payload is absent.

No successful build is yet verified. Upstream toolchain/NDK compatibility and packaged symbol behavior require CI and device tests. Scratch output is under .ws-proxy-build and must not be committed.
