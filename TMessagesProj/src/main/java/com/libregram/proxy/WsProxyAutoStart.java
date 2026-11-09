package com.libregram.proxy;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import org.telegram.messenger.BuildConfig;

/** One attempt per process, only after a visible activity resumes. */
public final class WsProxyAutoStart implements Application.ActivityLifecycleCallbacks {
    private static boolean registered;
    private boolean attempted;
    public static synchronized void register(Application app) {
        if (!registered && BuildConfig.DEBUG) {
            registered = true;
            app.registerActivityLifecycleCallbacks(new WsProxyAutoStart());
        }
    }
    @Override public void onActivityResumed(Activity activity) {
        if (attempted) return;
        attempted = true;
        if (WsProxyService.preferences(activity).getBoolean("auto_start", false)) {
            WsProxyService.State state = WsProxyService.state();
            if (state == WsProxyService.State.STOPPED) WsProxyService.start(activity);
        }
    }
    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { }
}
