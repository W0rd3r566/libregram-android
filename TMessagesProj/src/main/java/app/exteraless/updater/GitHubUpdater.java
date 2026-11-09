package app.exteraless.updater;

import android.app.Activity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.LaunchActivity;

/** Legacy API facade. Automatic APK updates are disabled until LibreGram releases are verified. */
public final class GitHubUpdater {
    public static final String RELEASES_URL = "https://github.com/W0rd3r566/libregram-android/releases";
    private GitHubUpdater() { }
    public static final class Release {
        public String tag, name, body, apkUrl, apkName;
        public long apkSize, published;
        public String displayVersion() {
            String value = tag == null || tag.isEmpty() ? name : tag;
            return value != null && value.startsWith("v") ? value.substring(1) : value;
        }
    }
    public interface CheckCallback {
        void onChecked(Release release, Boolean newer, boolean failed);
    }
    public interface DownloadListener {
        void onProgress(long done, long total);
        void onFinished(boolean success, boolean canceled);
    }
    public static void check(boolean force) {
        if (!force) return;
        AndroidUtilities.runOnUIThread(() -> {
            BaseFragment fragment = LaunchActivity.getSafeLastFragment();
            if (fragment == null || fragment.getParentActivity() == null) return;
            fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity())
                    .setTitle("LibreGram")
                    .setMessage(LocaleController.getString(R.string.LibreGramUpdatesDisabled))
                    .setPositiveButton(LocaleController.getString(R.string.OK), null).create());
        });
    }
    public static void fetch(CheckCallback callback) {
        // Unavailable, not 'latest': no verified LibreGram release feed is enabled yet.
        AndroidUtilities.runOnUIThread(() -> callback.onChecked(null, false, true));
    }
    public static long lastCheck() { return 0; }
    public static boolean isDownloading() { return false; }
    public static void cancelDownload() { }
    public static void download(Activity activity, Release release, DownloadListener listener) {
        // Never install a cached or inherited upstream APK through this facade.
        AndroidUtilities.runOnUIThread(() -> listener.onFinished(false, true));
    }
}
