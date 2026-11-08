#!/usr/bin/env bash
set -euo pipefail

# tg-ws-proxy-rs by valnesfjord, MIT; preserve upstream JNI names as an ABI contract.
REVISION=53f392280a370199ffe5c6ef0ac88808ed17c575
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SOURCE="$ROOT/.ws-proxy-build/source"
TARGET_DIR="$ROOT/.ws-proxy-build/target"
NDK="${ANDROID_NDK_HOME:-${ANDROID_HOME:?ANDROID_HOME is required}/ndk/27.2.12479018}"
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
ABI="${NATIVE_TARGET:-arm64-v8a}"
case "$ABI" in
  arm64-v8a) TARGET=aarch64-linux-android; CLANG=aarch64-linux-android27-clang ;;
  x86_64) TARGET=x86_64-linux-android; CLANG=x86_64-linux-android27-clang ;;
  *) echo "Unsupported WS build ABI: $ABI" >&2; exit 1 ;;
esac
command -v cargo >/dev/null
command -v rustup >/dev/null
test -x "$TOOLCHAIN/$CLANG"
mkdir -p "$SOURCE"
if [ ! -d "$SOURCE/.git" ]; then git -C "$SOURCE" init; fi
git -C "$SOURCE" fetch --depth=1 https://github.com/valnesfjord/tg-ws-proxy-rs.git "$REVISION"
git -C "$SOURCE" checkout --detach FETCH_HEAD
test "$(git -C "$SOURCE" rev-parse HEAD)" = "$REVISION"
rustup target add "$TARGET"
export CARGO_TARGET_DIR="$TARGET_DIR"
export PATH="$TOOLCHAIN:$PATH"
export CARGO_ENCODED_RUSTFLAGS=$'-C\x1flink-arg=-Wl,-z,max-page-size=16384'
case "$TARGET" in
  aarch64-linux-android)
    export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/$CLANG"
    export CC_aarch64_linux_android="$TOOLCHAIN/$CLANG"
    export AR_aarch64_linux_android="$TOOLCHAIN/llvm-ar"
    ;;
  x86_64-linux-android)
    export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN/$CLANG"
    export CC_x86_64_linux_android="$TOOLCHAIN/$CLANG"
    export AR_x86_64_linux_android="$TOOLCHAIN/llvm-ar"
    ;;
esac
cargo build --manifest-path "$SOURCE/Cargo.toml" --locked --release --target "$TARGET" -p tg-ws-proxy-jni --lib
OUTPUT="$ROOT/TMessagesProj/jni/$ABI"
mkdir -p "$OUTPUT"
cp "$TARGET_DIR/$TARGET/release/libtg_ws_proxy_jni.so" "$OUTPUT/"
"$TOOLCHAIN/llvm-readelf" --dyn-syms --wide "$OUTPUT/libtg_ws_proxy_jni.so" | grep -F Java_io_github_valnesfjord_tgwsproxyrs_NativeProxy_nativeStart
# The source license must accompany every packaged native build.
mkdir -p "$ROOT/TMessagesProj/src/main/assets/licenses"
cp "$SOURCE/LICENSE" "$ROOT/TMessagesProj/src/main/assets/licenses/tg-ws-proxy-rs-MIT.txt"
echo "Built pinned tg-ws-proxy-rs $REVISION for $ABI"
