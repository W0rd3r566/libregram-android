package com.libregram.safe;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Local-only Safe configuration; identifiers never leave this device. */
public final class SafeConfig {
    private static final String PREFS = "libregram_safe";
    private static final String KEY = "hidden_dialogs";
    private static final String LOCK = "locked";

    private SafeConfig() { }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
    }

    /** Both states are known locally; a missing flag is never interpreted as unlocked. */
    public static boolean isLocked() {
        return prefs().getBoolean(LOCK, true);
    }

    public static void setLocked(boolean locked) {
        prefs().edit().putBoolean(LOCK, locked).apply();
    }

    public static boolean isHidden(long dialogId) {
        return dialogs().contains(Long.toString(dialogId));
    }

    public static void setHidden(long dialogId, boolean hidden) {
        SharedPreferences.Editor editor = prefs().edit();
        Set<String> ids = new HashSet<>(dialogs());
        if (hidden) ids.add(Long.toString(dialogId));
        else ids.remove(Long.toString(dialogId));
        editor.putStringSet(KEY, ids);
        editor.apply();
    }

    public static Set<Long> hiddenDialogs() {
        Set<Long> result = new HashSet<>();
        for (String value : dialogs()) {
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                // Corrupted entries are skipped, never propagated as crash sources.
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<String> dialogs() {
        return prefs().getStringSet(KEY, new HashSet<>());
    }

    public static void clear() {
        prefs().edit().clear().apply();
    }

    public static boolean anyHidden() {
        return !dialogs().isEmpty();
    }
}
