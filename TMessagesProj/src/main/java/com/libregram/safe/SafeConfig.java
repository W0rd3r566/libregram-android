package com.libregram.safe;

import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Local-only Safe configuration. Hidden identifiers are scoped by account and dialog. */
public final class SafeConfig {
    private static final String PREFS = "libregram_safe";
    private static final String KEY = "hidden_dialogs_v2";
    private static final String LEGACY_KEY = "hidden_dialogs";
    private static final String LOCK = "locked";
    private SafeConfig() { }
    private static SharedPreferences prefs() { return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0); }
    public static boolean isLocked() { return prefs().getBoolean(LOCK, true); }
    public static void setLocked(boolean locked) { prefs().edit().putBoolean(LOCK, locked).apply(); }
    private static String key(int account, long dialogId) { return account + ":" + dialogId; }
    public static boolean isHidden(int account, long dialogId) { return dialogs().contains(key(account, dialogId)); }
    public static boolean isHidden(long dialogId) { return isHidden(0, dialogId); }
    public static void setHidden(int account, long dialogId, boolean hidden) {
        if (account < 0 || dialogId == 0) return;
        Set<String> ids = new HashSet<>(dialogs());
        String value = key(account, dialogId);
        if (hidden) ids.add(value); else ids.remove(value);
        prefs().edit().putStringSet(KEY, ids).apply();
    }
    public static void setHidden(long dialogId, boolean hidden) { setHidden(0, dialogId, hidden); }
    public static Set<Long> hiddenDialogs(int account) {
        Set<Long> result = new HashSet<>();
        String prefix = account + ":";
        for (String value : dialogs()) {
            if (!value.startsWith(prefix)) continue;
            try { result.add(Long.parseLong(value.substring(prefix.length()))); } catch (NumberFormatException ignored) { }
        }
        return Collections.unmodifiableSet(result);
    }
    public static Set<Long> hiddenDialogs() { return hiddenDialogs(0); }
    private static Set<String> dialogs() {
        Set<String> stored = prefs().getStringSet(KEY, null);
        if (stored != null) return new HashSet<>(stored);
        Set<String> legacy = prefs().getStringSet(LEGACY_KEY, Collections.emptySet());
        Set<String> migrated = new HashSet<>();
        for (String value : legacy) {
            try { migrated.add(key(0, Long.parseLong(value))); } catch (NumberFormatException ignored) { }
        }
        if (!migrated.isEmpty()) prefs().edit().putStringSet(KEY, migrated).remove(LEGACY_KEY).apply();
        return migrated;
    }
    public static boolean anyHidden(int account) { return !hiddenDialogs(account).isEmpty(); }
    public static boolean anyHidden() { return anyHidden(0); }
    public static void clear() { prefs().edit().remove(KEY).remove(LEGACY_KEY).apply(); }
}
