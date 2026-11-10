package com.libregram.safe;

/** Filters the main dialog list and group notifications per hidden chat. */
public final class SafeFilter {
    private SafeFilter() { }

    public static boolean isHiddenInList(long dialogId) {
        return SafeConfig.isLocked() && SafeConfig.isHidden(dialogId);
    }

    public static boolean isNotificationSuppressed(long dialogId) {
        return SafeConfig.isLocked() && SafeConfig.isHidden(dialogId);
    }

    public static boolean isAccountSuppressed() {
        return false;
    }
}
