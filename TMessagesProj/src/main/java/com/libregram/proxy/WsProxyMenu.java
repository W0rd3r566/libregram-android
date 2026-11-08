package com.libregram.proxy;

import static org.telegram.messenger.LocaleController.getString;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.ItemOptions;
import app.exteraless.settings.LibreGramWsProxySettingsActivity;

/** One runtime for both the settings screen and the main chat overflow menu. */
public final class WsProxyMenu {
    private WsProxyMenu() { }

    public static void addTo(ItemOptions options, BaseFragment fragment) {
        WsProxyService.State state = WsProxyService.state();
        int label;
        switch (state) {
            case RUNNING: label = R.string.LibreGramWsQuickDisable; break;
            case STARTING: label = R.string.LibreGramWsStarting; break;
            case STOPPING: label = R.string.LibreGramWsStopping; break;
            default: label = R.string.LibreGramWsQuickEnable;
        }
        // ItemOptions' four-argument overload is (icon, text, long press, click).
        options.add(R.drawable.msg_language, getString(label),
                () -> fragment.presentFragment(new LibreGramWsProxySettingsActivity()),
                () -> toggle(fragment));
        options.addGap();
    }

    private static void toggle(BaseFragment fragment) {
        if (fragment.getParentActivity() == null) return;
        if (!BuildConfig.DEBUG) {
            fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity())
                    .setTitle(getString(R.string.LibreGramWsProxy))
                    .setMessage(getString(R.string.LibreGramWsDebugOnly))
                    .setPositiveButton(getString(R.string.OK), null).create());
            return;
        }
        switch (WsProxyService.state()) {
            case RUNNING:
                WsProxyService.stop(fragment.getParentActivity());
                break;
            case STOPPED:
            case ERROR:
                WsProxyService.start(fragment.getParentActivity());
                break;
            default:
                // Startup/shutdown is serialized by the service; do not queue a second action.
                break;
        }
    }
}
