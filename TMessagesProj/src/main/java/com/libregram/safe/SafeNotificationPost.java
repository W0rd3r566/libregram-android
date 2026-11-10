package com.libregram.safe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

/** "You have N new messages" only: never message text or sender for hidden chats. */
public final class SafeNotificationPost {
    private static final String CHANNEL = "libregram_safe";

    private SafeNotificationPost() { }

    public static boolean allowed() {
        return !SafeConfig.isLocked();
    }

    public static void post(Context context, int account, int count) {
        if (count <= 0) return;
        try {
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                        "LibreGram Safe", NotificationManager.IMPORTANCE_LOW));
            }
            Intent open = new Intent(context, org.telegram.ui.LaunchActivity.class);
            PendingIntent content = PendingIntent.getActivity(context, 24903, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.msg_language)
                    .setContentTitle("LibreGram")
                    .setContentText(count + "")
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setAutoCancel(true)
                    .setContentIntent(content);
            manager.notify(24903, builder.build());
        } catch (RuntimeException ignored) {
            // Never crash message processing because of a notification failure.
        }
    }

    public static void cancel(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(24903);
        } catch (RuntimeException ignored) {
        }
    }
}
