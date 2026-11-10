package com.libregram.safe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import org.telegram.messenger.R;

/** Optional generic summary for Safe; never includes message text or sender. */
public final class SafeNotificationPost {
    private static final String CHANNEL = "libregram_safe";
    private static final int BASE_ID = 24903;
    private SafeNotificationPost() { }
    public static boolean allowed(int account, long dialogId) {
        return !SafeFilter.isNotificationSuppressed(account, dialogId);
    }
    public static boolean allowed() { return !SafeConfig.isLocked(); }
    public static void post(Context context, int account, int count) {
        if (count <= 0 || !SafeConfig.isLocked()) return;
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(CHANNEL, "LibreGram Safe", NotificationManager.IMPORTANCE_LOW));
            PendingIntent content = PendingIntent.getActivity(context, BASE_ID + account, new Intent(context, org.telegram.ui.LaunchActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.msg_language).setContentTitle("LibreGram")
                    .setContentText(String.valueOf(count)).setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET).setAutoCancel(true).setContentIntent(content);
            manager.notify(BASE_ID + account, builder.build());
        } catch (RuntimeException ignored) { }
    }
    public static void cancel(Context context, int account) {
        try { ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(BASE_ID + account); } catch (RuntimeException ignored) { }
    }
    public static void cancel(Context context) { cancel(context, 0); }
}
