#!/usr/bin/env python3
"""Prepare reviewed alpha source edits; validate anchors and idempotence."""
from pathlib import Path
import re
import runpy

ROOT = Path(__file__).resolve().parents[1]
replace = runpy.run_path(str(ROOT / 'Tools/integrate-libregram-controls.py'))['replace']

def service(s):
    s = replace(s, '    private boolean savedEnabled;', '''    private long sessionDeadline;
    private static long visibleDeadline;
    private final Runnable sessionTimer = new Runnable() {
        @Override public void run() {
            if (destroyed || stopping || sessionDeadline == 0) return;
            long remaining = sessionDeadline - android.os.SystemClock.elapsedRealtime();
            if (remaining <= 0) beginStop("session_expired");
            else MAIN.postDelayed(this, Math.min(remaining, 1000));
        }
    };
    public static long remainingSessionMillis() {
        return visibleDeadline == 0 ? 0 : Math.max(0,
                visibleDeadline - android.os.SystemClock.elapsedRealtime());
    }
    private void clearSessionTimer() {
        MAIN.removeCallbacks(sessionTimer);
        sessionDeadline = 0;
        visibleDeadline = 0;
    }
    private boolean savedEnabled;''')
    s = replace(s, '        state = State.RUNNING;\n        changed("listener_ready");', '''        state = State.RUNNING;
        int minutes = preferences(this).getInt("session_minutes", 0);
        if (minutes == 15 || minutes == 60) {
            sessionDeadline = android.os.SystemClock.elapsedRealtime() + minutes * 60_000L;
            visibleDeadline = sessionDeadline;
            MAIN.post(sessionTimer);
            changed("session_limit_minutes:" + minutes);
        }
        changed("listener_ready");''')
    for before, after in [
        ('        stopping = true;\n        cancelCheck();', '        stopping = true;\n        clearSessionTimer();\n        cancelCheck();'),
        ('    private void fail(String message) {\n        if (destroyed) return;', '    private void fail(String message) {\n        if (destroyed) return;\n        clearSessionTimer();'),
        ('    @Override public void onStopped() {\n        if (destroyed || !ownsNative) return;', '    @Override public void onStopped() {\n        if (destroyed || !ownsNative) return;\n        clearSessionTimer();'),
        ('        destroyed = true;', '        destroyed = true;\n        clearSessionTimer();'),
    ]:
        s = replace(s, before, after)
    return s

def screen(s):
    s = replace(s, 'journalRow, sourceRow, footerRow;', 'journalRow, sourceRow, footerRow, resetRow, durationRow;')
    s = replace(s, '        sourceRow = addRow("libregramWsSource");', '        durationRow = addRow("libregramWsDuration");\n        resetRow = addRow("libregramWsReset");\n        sourceRow = addRow("libregramWsSource");')
    s = replace(s, '        } else if (position == enabledRow) {', '''        } else if (position == durationRow) {
            if (!idle()) {
                message(getActionBarTitle(), getString(R.string.LibreGramWsStopToEdit));
                return;
            }
            showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTitle(getString(R.string.LibreGramWsDuration))
                    .setItems(new CharSequence[]{getString(R.string.LibreGramWsNoTimer),
                            getString(R.string.LibreGramWs15Minutes), getString(R.string.LibreGramWs60Minutes)},
                            (dialog, which) -> {
                                if (!idle()) return;
                                prefs().edit().putInt("session_minutes", new int[]{0, 15, 60}[which]).apply();
                                observer.run();
                            }).create());
        } else if (position == resetRow) {
            if (!idle()) {
                message(getActionBarTitle(), getString(R.string.LibreGramWsStopToEdit));
                return;
            }
            showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTitle(getString(R.string.LibreGramWsReset))
                    .setMessage(getString(R.string.LibreGramWsResetConfirm))
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .setPositiveButton(getString(R.string.OK), (dialog, which) -> {
                        if (!idle()) return;
                        prefs().edit().remove("auto_start").remove("connect_client")
                                .remove("tcp_fallback").remove("port").remove("pool")
                                .remove("cf_domain").remove("cf_worker").remove("session_minutes").apply();
                        observer.run();
                    }).create());
        } else if (position == enabledRow) {''')
    s = replace(s, 'String report = "LibreGram WS Proxy\\n" + WsProxyService.state().name() + "\\n" + WsProxyService.events();', '''String report = "LibreGram WS Proxy\\n"
                    + "Version: " + org.telegram.messenger.BuildConfig.VERSION_NAME + "\\n"
                    + "Commit: " + org.telegram.messenger.BuildConfig.BUILD_COMMIT_ID + "\\n"
                    + "Android API: " + android.os.Build.VERSION.SDK_INT + "\\n"
                    + "State: " + WsProxyService.state().name() + "\\n"
                    + "Error: " + WsProxyService.error() + "\\n" + WsProxyService.events();''')
    s = replace(s, 'return p == autoStartRow ||', 'return p == durationRow || p == resetRow || p == autoStartRow ||')
    s = replace(s, '} else if (position == journalRow) { title = R.string.LibreGramWsJournal;', '''} else if (position == durationRow) {
                    title = R.string.LibreGramWsDuration;
                    int minutes = prefs().getInt("session_minutes", 0);
                    value = getString(minutes == 15 ? R.string.LibreGramWs15Minutes
                            : minutes == 60 ? R.string.LibreGramWs60Minutes : R.string.LibreGramWsNoTimer);
                } else if (position == resetRow) { title = R.string.LibreGramWsReset;
                } else if (position == journalRow) { title = R.string.LibreGramWsJournal;''')
    return s

def settings(s):
    s = replace(s, '0xFF993C38, 0xFF993C38, R.drawable.exteraless_icon_tile', '0xFF0B0D12, 0xFF0B0D12, R.drawable.exteraless_icon_tile')
    return replace(s, '            updateColors();\n        }\n\n        public static void applyIconColors(', '''            updateColors();
            // Full-color brand vector: no inherited badge behind it and no glyph tint.
            iconLayout.setBackground(icon == R.drawable.exteraless_icon_tile ? null : iconBackground);
            if (icon == R.drawable.exteraless_icon_tile) iconView.clearColorFilter();
        }

        public static void applyIconColors(''')

def manifest(s):
    def alias(match):
        value = match.group(0)
        if 'android.intent.category.LAUNCHER' not in value: return value
        return re.sub(r'android:label="[^"]*"', 'android:label="LibreGram"', value)
    return re.sub(r'<activity-alias\b.*?</activity-alias>', alias, s, flags=re.S)

def main():
    runpy.run_path(str(ROOT / 'Tools/integrate-libregram-startup.py'))
    transformations = {
        'TMessagesProj/src/main/java/com/libregram/proxy/WsProxyService.java': service,
        'TMessagesProj/src/main/java/app/exteraless/settings/LibreGramWsProxySettingsActivity.java': screen,
        'TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java': settings,
        'TMessagesProj/src/main/AndroidManifest.xml': manifest,
    }
    updates = []
    for name, transform in transformations.items():
        path = ROOT / name
        result = transform(path.read_text(encoding='utf-8'))
        assert transform(result) == result, name
        updates.append((path, result))
    for path, result in updates: path.write_text(result, encoding='utf-8')
    print('Alpha timer, reset, report and launcher branding applied.')

if __name__ == '__main__': main()
