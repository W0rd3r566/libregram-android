package com.libregram.safe;

/** Filters only when the Safe session is explicitly locked. */
public final class SafeFilter {
    private SafeFilter() { }
    public static boolean isHiddenInList(int account, long dialogId) {
        return SafeConfig.isLocked() && SafeConfig.isHidden(account, dialogId);
    }
    public static boolean isHiddenInList(long dialogId) { return isHiddenInList(0, dialogId); }
    public static boolean isNotificationSuppressed(int account, long dialogId) {
        return SafeConfig.isLocked() && SafeConfig.isHidden(account, dialogId);
    }
    public static boolean isNotificationSuppressed(long dialogId) { return isNotificationSuppressed(0, dialogId); }
    public static boolean isAccountSuppressed() { return false; }
}
