package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) return;
        }
        setIcon(LauncherIcon.EXTERALESS);
    }
    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int state = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.EXTERALESS;
    }
    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        for (LauncherIcon candidate : LauncherIcon.values()) {
            ctx.getPackageManager().setComponentEnabledSetting(candidate.getComponentName(ctx),
                    candidate == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }
    // Component keys and enum constants are retained so existing launcher selections survive updates.
    // Preview vectors match the drawable configured for each manifest alias.
    public enum LauncherIcon {
        EXTERALESS("ExteralessIcon", R.drawable.exteraless_icon_background, R.drawable.exteraless_icon_foreground, R.string.AppIconExteraless),
        BLUEPRINT("BlueprintIcon", R.drawable.libregram_icon_graphite, R.drawable.app_icon_empty_foreground, R.string.AppIconBlueprint),
        RED("RedIcon", R.drawable.libregram_icon_ocean, R.drawable.app_icon_empty_foreground, R.string.AppIconRed),
        NYA("NyaIcon", R.drawable.libregram_icon_midnight, R.drawable.app_icon_empty_foreground, R.string.AppIconNya),
        AYU("AyuIcon", R.drawable.libregram_icon_mint, R.drawable.app_icon_empty_foreground, R.string.AppIconAyu),
        QUACK("QuackIcon", R.drawable.libregram_icon_violet, R.drawable.app_icon_empty_foreground, R.string.AppIconQuack),
        GO("GoIcon", R.drawable.libregram_icon_rose, R.drawable.app_icon_empty_foreground, R.string.AppIconGo),
        HAND("HandIcon", R.drawable.libregram_icon_ember, R.drawable.app_icon_empty_foreground, R.string.AppIconHand),
        MONO("MonoIcon", R.drawable.libregram_icon_silver, R.drawable.app_icon_empty_foreground, R.string.AppIconMono),
        NOTHING("NothingIcon", R.drawable.libregram_icon_cobalt, R.drawable.app_icon_empty_foreground, R.string.AppIconNothing),
        PLUS("PlusIcon", R.drawable.libregram_icon_crimson, R.drawable.app_icon_empty_foreground, R.string.AppIconPlus),
        TELEGRAM("TelegramIcon", R.drawable.libregram_icon_lime, R.drawable.app_icon_empty_foreground, R.string.AppIconTelegramOriginal),
        VINTAGE("VintageIcon", R.drawable.libregram_icon_sand, R.drawable.app_icon_empty_foreground, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.libregram_icon_iris, R.drawable.app_icon_empty_foreground, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.libregram_icon_ice, R.drawable.app_icon_empty_foreground, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.libregram_icon_mono, R.drawable.app_icon_empty_foreground, R.string.AppIconTurbo),
        NOX("NoxIcon", R.drawable.libregram_icon_night, R.drawable.app_icon_empty_foreground, R.string.AppIconNox);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        private ComponentName componentName;
        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            return componentName;
        }
        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }
        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}