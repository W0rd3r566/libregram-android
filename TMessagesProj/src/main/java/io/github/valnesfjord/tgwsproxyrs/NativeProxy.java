package io.github.valnesfjord.tgwsproxyrs;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Keep;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * JNI ABI adapter for tg-ws-proxy-rs by valnesfjord (MIT).
 * The upstream package and static method names must match the pinned Rust symbols.
 * Loading is deferred: a missing native library must not crash application startup.
 */
@Keep
public final class NativeProxy {
    public interface Listener {
        void onListening(String link);
        void onError(String message);
        void onStopped();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArraySet<Listener> LISTENERS = new CopyOnWriteArraySet<>();
    private static boolean loaded;

    private NativeProxy() { }

    public static synchronized void load() {
        if (!loaded) {
            System.loadLibrary("tg_ws_proxy_jni");
            loaded = true;
        }
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    @Keep public static native String nativeStart(String args);
    @Keep public static native void nativeStop();
    @Keep public static native boolean nativeIsRunning();

    @Keep
    public static void onNativeLog(String line) {
        // Do not forward raw upstream logs: they may contain proxy credentials.
    }

    @Keep
    public static void onNativeListening(String link) {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) listener.onListening(link);
        });
    }

    @Keep
    public static void onNativeError(String message) {
        // Raw native errors are not exposed until a tested redaction layer exists.
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) listener.onError("WS transport failed");
        });
    }

    @Keep
    public static void onNativeStopped() {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) listener.onStopped();
        });
    }
}
