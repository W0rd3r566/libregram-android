package com.libregram.proxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import io.github.valnesfjord.tgwsproxyrs.NativeProxy;

/** Temporary in-process route; persisted user proxies are never overwritten. */
public final class WsProxyService extends Service implements NativeProxy.Listener,
        NotificationCenter.NotificationCenterDelegate {
    public enum State { STOPPED, STARTING, RUNNING, STOPPING, ERROR }
    private static final String CHANNEL = "libregram_ws_proxy";
    private static final int NOTIFICATION_ID = 24901;
    private static final String STOP = "com.libregram.proxy.STOP";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArraySet<Runnable> OBSERVERS = new CopyOnWriteArraySet<>();
    private static final ArrayDeque<String> EVENTS = new ArrayDeque<>();
    private static volatile State state = State.STOPPED;
    private static String error = "";
    private static String checkResult = "";
    private static WsProxyService instance;
    private static boolean startRequested;
    private boolean routed, stopping, destroyed, ownsNative;
    private boolean savedEnabled;
    private ProxySettings savedSettings, localSettings;
    private long checkGeneration;
    private boolean checking;
    private Runnable checkTimeout;
    private final Runnable startTimeout = () -> fail("startup_timeout");
    private final Runnable stopTimeout = () -> {
        state = State.ERROR;
        error = "shutdown_timeout";
        changed(error);
        restoreRoute();
        stopSelf();
    };

    public static State state() { return state; }
    public static String error() { return error; }
    public static String checkResult() { return checkResult; }
    public static void observe(Runnable observer) { OBSERVERS.add(observer); }
    public static void unobserve(Runnable observer) { OBSERVERS.remove(observer); }
    public static String events() {
        synchronized (EVENTS) { return String.join("\n", EVENTS); }
    }
    private static void changed(String event) {
        synchronized (EVENTS) {
            if (EVENTS.size() >= 80) EVENTS.removeFirst();
            EVENTS.addLast(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(new java.util.Date()) + " " + event);
        }
        for (Runnable observer : OBSERVERS) observer.run();
    }
    private static String text(int resource) { return LocaleController.getString(resource); }
    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("libregram_ws_proxy", Context.MODE_PRIVATE);
    }
    public static void start(Context context) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> start(context.getApplicationContext()));
            return;
        }
        if (instance != null || startRequested || state == State.STARTING || state == State.STOPPING) return;
        startRequested = true;
        try {
            Intent intent = new Intent(context, WsProxyService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (RuntimeException e) {
            startRequested = false;
            state = State.ERROR;
            error = "service_start_refused";
            changed(error);
        }
    }
    public static void stop(Context context) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN.post(() -> stop(context.getApplicationContext()));
            return;
        }
        if (instance != null) instance.beginStop("user_toggle");
    }
    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        NativeProxy.addListener(this);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyChangedByRotation);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startRequested = false;
        if (intent != null && STOP.equals(intent.getAction())) {
            beginStop("notification_action");
            return START_NOT_STICKY;
        }
        if (state == State.STARTING || state == State.RUNNING || stopping) return START_NOT_STICKY;
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "LibreGram WS Proxy", NotificationManager.IMPORTANCE_LOW));
            PendingIntent content = PendingIntent.getActivity(this, 24901,
                    new Intent(this, org.telegram.ui.LaunchActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent stop = PendingIntent.getService(this, 24902,
                    new Intent(this, WsProxyService.class).setAction(STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
            Notification notification = builder.setSmallIcon(R.drawable.msg_language)
                    .setContentTitle("LibreGram · WS Proxy")
                    .setContentText(text(R.string.LibreGramWsNotification))
                    .setContentIntent(content).setOngoing(true)
                    .addAction(new Notification.Action.Builder(null, text(R.string.LibreGramWsStop), stop).build()).build();
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(NOTIFICATION_ID, notification);
            state = State.STARTING;
            error = "";
            cancelCheck();
            changed("starting");
            NativeProxy.load();
            // A previous runtime may still be winding down; never take ownership of it.
            if (NativeProxy.nativeIsRunning()) {
                fail("native_runtime_busy");
                return START_NOT_STICKY;
            }
            SharedPreferences user = MessagesController.getGlobalMainSettings();
            savedEnabled = user.getBoolean("proxy_enabled", false);
            savedSettings = ProxySettings.fromSharedPreferences(user);
            SharedPreferences settings = preferences(this);
            String secret = settings.getString("secret", "");
            if (!secret.matches("[0-9a-f]{32}")) {
                byte[] bytes = new byte[16];
                new SecureRandom().nextBytes(bytes);
                StringBuilder hex = new StringBuilder();
                for (byte b : bytes) hex.append(String.format(java.util.Locale.US, "%02x", b & 255));
                secret = hex.toString();
                settings.edit().putString("secret", secret).apply();
            }
            int port = settings.getInt("port", 1443), pool = settings.getInt("pool", 2);
            if (port < 1024 || port > 65535 || pool < 0 || pool > 8) throw new IllegalArgumentException();
            StringBuilder args = new StringBuilder("--host 127.0.0.1 --link-ip 127.0.0.1 --quiet --no-outbound-proxy --port ")
                    .append(port).append(" --secret ").append(secret).append(" --pool-size ").append(pool);
            String cf = settings.getString("cf_domain", ""), worker = settings.getString("cf_worker", "");
            if (!cf.isEmpty()) args.append(" --cf-domain ").append(validateDomain(cf));
            if (!worker.isEmpty()) args.append(" --cf-worker-domain ").append(validateDomain(worker));
            args.append(" --pinned-upstream ws");
            if (!worker.isEmpty()) args.append(",cfworker");
            if (!cf.isEmpty()) args.append(",cfproxy");
            if (settings.getBoolean("tcp_fallback", false)) args.append(",tcp");
            MAIN.postDelayed(startTimeout, 20000);
            String failure = NativeProxy.nativeStart(args.toString());
            if (failure == null) ownsNative = true;
            else fail("native_start_rejected");
        } catch (LinkageError e) { fail("native_library_unavailable");
        } catch (RuntimeException e) { fail("configuration_or_service_error"); }
        return START_NOT_STICKY;
    }
    public static String validateDomain(String domain) {
        String ascii = java.net.IDN.toASCII(domain.trim()).toLowerCase(java.util.Locale.US);
        if (ascii.length() > 253 || !ascii.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")) throw new IllegalArgumentException("domain");
        for (String label : ascii.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) throw new IllegalArgumentException("domain");
        }
        return ascii;
    }
    @Override public void onListening(String link) {
        if (destroyed || stopping || state != State.STARTING || !ownsNative) return;
        MAIN.removeCallbacks(startTimeout);
        ProxySettings settings = ProxySettings.fromUri(Uri.parse(link));
        if (settings == null || !settings.isValid() || !"127.0.0.1".equals(settings.getAddress())
                || settings.getType() != ProxySettings.Type.MTPROTO
                || settings.getPort() != preferences(this).getInt("port", 1443)) {
            fail("invalid_listener_endpoint"); return;
        }
        localSettings = settings;
        if (preferences(this).getBoolean("connect_client", true)) {
            routed = true;
            ConnectionsManager.setProxySettings(true, settings);
        }
        state = State.RUNNING;
        changed("listener_ready");
    }
    public static void check() {
        if (Looper.myLooper() != Looper.getMainLooper()) { MAIN.post(WsProxyService::check); return; }
        WsProxyService service = instance;
        if (service == null || state != State.RUNNING || service.localSettings == null || service.checking) return;
        service.checking = true;
        long generation = ++service.checkGeneration;
        checkResult = text(R.string.LibreGramWsChecking);
        changed("check_started");
        service.checkTimeout = () -> service.finishCheck(generation, -1, true);
        MAIN.postDelayed(service.checkTimeout, 15000);
        try {
            ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(service.localSettings,
                    time -> MAIN.post(() -> service.finishCheck(generation, time, false)));
        } catch (RuntimeException e) { service.finishCheck(generation, -1, false); }
    }
    private void finishCheck(long generation, long time, boolean timeout) {
        if (instance != this || destroyed || state != State.RUNNING || !checking || generation != checkGeneration) return;
        checking = false;
        if (checkTimeout != null) MAIN.removeCallbacks(checkTimeout);
        checkTimeout = null;
        checkResult = timeout ? text(R.string.LibreGramWsCheckTimeout)
                : time < 0 ? text(R.string.LibreGramWsUnavailable) : time + " ms";
        changed(timeout ? "check_timeout" : time < 0 ? "check_failed" : "check_passed");
    }
    private void cancelCheck() {
        ++checkGeneration;
        checking = false;
        if (checkTimeout != null) MAIN.removeCallbacks(checkTimeout);
        checkTimeout = null;
        checkResult = "";
    }
    private void beginStop(String reason) {
        if (destroyed || stopping) return;
        stopping = true;
        cancelCheck();
        MAIN.removeCallbacks(startTimeout);
        state = State.STOPPING;
        changed("stopping:" + reason);
        restoreRoute();
        if (!ownsNative) { state = State.STOPPED; stopSelf(); return; }
        try {
            NativeProxy.nativeStop();
            MAIN.postDelayed(stopTimeout, 5000);
        } catch (LinkageError | RuntimeException e) { fail("native_stop_error"); }
    }
    private void restoreRoute() {
        if (!routed) return;
        routed = false;
        SharedPreferences user = MessagesController.getGlobalMainSettings();
        ConnectionsManager.setProxySettings(user.getBoolean("proxy_enabled", false), ProxySettings.fromSharedPreferences(user));
    }
    private void fail(String message) {
        if (destroyed) return;
        cancelCheck();
        MAIN.removeCallbacks(startTimeout);
        MAIN.removeCallbacks(stopTimeout);
        error = message;
        state = State.ERROR;
        changed("transport_error:" + message);
        restoreRoute();
        stopSelf();
    }
    @Override public void onError(String message) { if (ownsNative) fail("native_transport_error"); }
    @Override public void onStopped() {
        if (destroyed || !ownsNative) return;
        cancelCheck();
        MAIN.removeCallbacks(startTimeout);
        MAIN.removeCallbacks(stopTimeout);
        ownsNative = false;
        restoreRoute();
        if (state != State.ERROR) state = State.STOPPED;
        changed("stopped");
        stopSelf();
    }
    @Override public void didReceivedNotification(int id, int account, Object... args) {
        if (!routed || stopping || destroyed) return;
        SharedPreferences user = MessagesController.getGlobalMainSettings();
        ProxySettings current = ProxySettings.fromSharedPreferences(user);
        boolean enabled = user.getBoolean("proxy_enabled", false);
        if (enabled != savedEnabled || !current.equals(savedSettings)) {
            beginStop(id == NotificationCenter.proxyChangedByRotation ? "proxy_rotation" : "user_proxy_changed");
        } else {
            changed("proxy_notification_unchanged");
        }
    }
    @Override public void onDestroy() {
        destroyed = true;
        startRequested = false;
        cancelCheck();
        MAIN.removeCallbacks(startTimeout);
        MAIN.removeCallbacks(stopTimeout);
        NativeProxy.removeListener(this);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyChangedByRotation);
        restoreRoute();
        if (ownsNative) {
            try { NativeProxy.nativeStop(); } catch (LinkageError | RuntimeException ignored) { }
        }
        if (state != State.ERROR) state = State.STOPPED;
        if (instance == this) instance = null;
        stopForeground(true);
        changed("service_destroyed");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
