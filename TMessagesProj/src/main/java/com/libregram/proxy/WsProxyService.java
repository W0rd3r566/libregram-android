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
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.valnesfjord.tgwsproxyrs.NativeProxy;

/** Owns the native runtime; never persists the temporary loopback route as a user proxy. */
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
    private boolean routed;
    private boolean stopping;
    private ProxySettings localSettings;
    private final Runnable startTimeout = () -> fail("Proxy startup timed out");
    private final Runnable stopTimeout = () -> {
        // Do not permit another Start if the native worker failed to acknowledge shutdown.
        error = "Native shutdown has not completed; restart LibreGram before retrying";
        state = State.ERROR;
        changed("shutdown_timeout");
        restoreRoute();
        stopSelf();
    };

    public static State state() { return state; }
    public static String error() { return error; }
    public static String checkResult() { return checkResult; }
    public static void observe(Runnable observer) { OBSERVERS.add(observer); }
    public static void unobserve(Runnable observer) { OBSERVERS.remove(observer); }
    public static String events() { return String.join("\n", EVENTS); }

    private static void changed(String event) {
        if (EVENTS.size() >= 80) EVENTS.removeFirst();
        EVENTS.addLast(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date()) + " " + event);
        for (Runnable observer : OBSERVERS) observer.run();
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("libregram_ws_proxy", Context.MODE_PRIVATE);
    }

    public static void start(Context context) {
        if (instance != null || state == State.STARTING || state == State.STOPPING) return;
        Intent intent = new Intent(context, WsProxyService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (RuntimeException e) {
            state = State.ERROR;
            error = "Android refused to start the service";
            changed("service_start_refused");
        }
    }

    public static void stop(Context context) {
        if (instance != null) instance.beginStop();
    }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        NativeProxy.addListener(this);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyChangedByRotation);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            beginStop();
            return START_NOT_STICKY;
        }
        if (state == State.STARTING || state == State.RUNNING || stopping) return START_NOT_STICKY;
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                        "LibreGram WS Proxy", NotificationManager.IMPORTANCE_LOW));
            }
            Intent open = new Intent(this, org.telegram.ui.LaunchActivity.class);
            PendingIntent content = PendingIntent.getActivity(this, 24901, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent stop = PendingIntent.getService(this, 24902,
                    new Intent(this, WsProxyService.class).setAction(STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
            Notification notification = builder.setSmallIcon(org.telegram.messenger.R.drawable.msg_language)
                    .setContentTitle("LibreGram · WS Proxy")
                    .setContentText("Local Telegram WebSocket transport")
                    .setContentIntent(content).setOngoing(true)
                    .addAction(new Notification.Action.Builder(null, "Stop", stop).build()).build();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            state = State.STARTING;
            error = "";
            checkResult = "";
            changed("starting");
            NativeProxy.load();
            SharedPreferences preferences = preferences(this);
            String secret = preferences.getString("secret", "");
            if (!secret.matches("[0-9a-f]{32}")) {
                byte[] bytes = new byte[16];
                new SecureRandom().nextBytes(bytes);
                StringBuilder hex = new StringBuilder();
                for (byte b : bytes) hex.append(String.format(java.util.Locale.US, "%02x", b & 255));
                secret = hex.toString();
                preferences.edit().putString("secret", secret).apply();
            }
            int port = preferences.getInt("port", 1443);
            if (port < 1024 || port > 65535) throw new IllegalArgumentException("port");
            int pool = preferences.getInt("pool", 2);
            if (pool < 0 || pool > 8) throw new IllegalArgumentException("pool");
            StringBuilder args = new StringBuilder("--host 127.0.0.1 --link-ip 127.0.0.1 --quiet --no-outbound-proxy --port ")
                    .append(port).append(" --secret ").append(secret).append(" --pool-size ").append(pool);
            String cf = preferences.getString("cf_domain", "");
            String worker = preferences.getString("cf_worker", "");
            if (!cf.isEmpty()) args.append(" --cf-domain ").append(validateDomain(cf));
            if (!worker.isEmpty()) args.append(" --cf-worker-domain ").append(validateDomain(worker));
            // Explicit allowlist of tiers: never inherit upstream's implicit direct TCP fallback.
            args.append(" --pinned-upstream ws");
            if (!worker.isEmpty()) args.append(",cfworker");
            if (!cf.isEmpty()) args.append(",cfproxy");
            if (preferences.getBoolean("tcp_fallback", false)) args.append(",tcp");
            MAIN.postDelayed(startTimeout, 20000);
            String failure = NativeProxy.nativeStart(args.toString());
            if (failure != null) fail("Native transport could not start");
        } catch (LinkageError e) {
            fail("Native WS library is unavailable for this build");
        } catch (RuntimeException e) {
            fail("Invalid configuration or service startup failure");
        }
        return START_NOT_STICKY;
    }

    public static String validateDomain(String domain) {
        String ascii = java.net.IDN.toASCII(domain.trim()).toLowerCase(java.util.Locale.US);
        if (ascii.length() > 253 || !ascii.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")) {
            throw new IllegalArgumentException("domain");
        }
        for (String label : ascii.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) {
                throw new IllegalArgumentException("domain");
            }
        }
        return ascii;
    }

    @Override public void onListening(String link) {
        MAIN.removeCallbacks(startTimeout);
        if (stopping) return;
        ProxySettings settings = ProxySettings.fromUri(Uri.parse(link));
        if (settings == null || !settings.isValid() || !"127.0.0.1".equals(settings.getAddress())
                || settings.getType() != ProxySettings.Type.MTPROTO) {
            fail("Native listener returned an invalid local endpoint");
            return;
        }
        localSettings = settings;
        if (preferences(this).getBoolean("connect_client", true)) {
            // Global setter updates every account. Persistent user settings remain untouched.
            ConnectionsManager.setProxySettings(true, settings);
            routed = true;
        }
        state = State.RUNNING;
        changed("listener_ready");
    }

    public static void check() {
        WsProxyService service = instance;
        if (service == null || state != State.RUNNING || service.localSettings == null) return;
        checkResult = "Checking";
        changed("check_started");
        ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(service.localSettings,
                time -> MAIN.post(() -> {
                    if (instance != service || state != State.RUNNING) return;
                    checkResult = time < 0 ? "Unavailable" : time + " ms";
                    changed(time < 0 ? "check_failed" : "check_passed");
                }));
    }

    private void beginStop() {
        if (stopping) return;
        stopping = true;
        MAIN.removeCallbacks(startTimeout);
        state = State.STOPPING;
        changed("stopping");
        restoreRoute();
        try {
            NativeProxy.load();
            NativeProxy.nativeStop();
            MAIN.postDelayed(stopTimeout, 5000);
        } catch (LinkageError | RuntimeException e) {
            state = State.STOPPED;
            stopSelf();
        }
    }

    private void restoreRoute() {
        if (!routed) return;
        routed = false;
        SharedPreferences user = MessagesController.getGlobalMainSettings();
        ConnectionsManager.setProxySettings(user.getBoolean("proxy_enabled", false),
                ProxySettings.fromSharedPreferences(user));
    }

    private void fail(String message) {
        MAIN.removeCallbacks(startTimeout);
        MAIN.removeCallbacks(stopTimeout);
        error = message;
        state = State.ERROR;
        changed("transport_error");
        restoreRoute();
        stopSelf();
    }

    @Override public void onError(String message) { fail(message); }
    @Override public void onStopped() {
        MAIN.removeCallbacks(startTimeout);
        MAIN.removeCallbacks(stopTimeout);
        restoreRoute();
        state = State.STOPPED;
        changed("stopped");
        stopSelf();
    }

    @Override public void didReceivedNotification(int id, int account, Object... args) {
        // A user selection or proxy rotation wins over the temporary WS route.
        if (routed) beginStop();
    }

    @Override public void onDestroy() {
        MAIN.removeCallbacks(startTimeout);
        MAIN.removeCallbacks(stopTimeout);
        NativeProxy.removeListener(this);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyChangedByRotation);
        restoreRoute();
        try { NativeProxy.nativeStop(); } catch (LinkageError | RuntimeException ignored) { }
        if (state != State.ERROR) state = State.STOPPED;
        instance = null;
        stopForeground(true);
        changed("service_destroyed");
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
