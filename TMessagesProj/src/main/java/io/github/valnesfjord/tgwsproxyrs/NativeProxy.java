package io.github.valnesfjord.tgwsproxyrs;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Keep;
import java.util.concurrent.CopyOnWriteArraySet;
import com.libregram.proxy.WsErrorClassifier;

/** JNI names are an upstream ABI contract, not application branding. */
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
        if (!loaded) { System.loadLibrary("tg_ws_proxy_jni"); loaded = true; }
    }
    public static void addListener(Listener listener) { LISTENERS.add(listener); }
    public static void removeListener(Listener listener) { LISTENERS.remove(listener); }
    @Keep public static native String nativeStart(String args);
    @Keep public static native void nativeStop();
    @Keep public static native boolean nativeIsRunning();
    @Keep public static void onNativeLog(String line) {
        // Raw native logs may contain credentials; never expose them to the report.
    }
    @Keep public static void onNativeListening(String link) {
        Listener[] recipients = LISTENERS.toArray(new Listener[0]);
        MAIN.post(() -> {
            for (Listener listener : recipients) {
                if (LISTENERS.contains(listener)) listener.onListening(link);
            }
        });
    }
    @Keep public static void onNativeError(String message) {
        String code = WsErrorClassifier.classify(message);
        Listener[] recipients = LISTENERS.toArray(new Listener[0]);
        MAIN.post(() -> {
            for (Listener listener : recipients) {
                if (LISTENERS.contains(listener)) listener.onError(code);
            }
        });
    }
    @Keep public static void onNativeStopped() {
        Listener[] recipients = LISTENERS.toArray(new Listener[0]);
        MAIN.post(() -> {
            for (Listener listener : recipients) {
                if (LISTENERS.contains(listener)) listener.onStopped();
            }
        });
    }
}
