package app.exteraless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import com.libregram.proxy.WsProxyService;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/** Native-backed controls; transport service is currently registered in debug builds only. */
public class LibreGramWsProxySettingsActivity extends BaseNekoSettingsActivity {
    private int headerRow, enabledRow, statusRow, infoRow, connectRow, tcpRow;
    private int advancedRow, portRow, poolRow, cfRow, workerRow, advancedInfoRow;
    private int diagnosticsRow, checkRow, journalRow, sourceRow, footerRow;
    private final Runnable observer = () -> {
        if (listAdapter != null) listAdapter.notifyDataSetChanged();
    };

    private SharedPreferences prefs() { return WsProxyService.preferences(getParentActivity()); }
    private boolean idle() {
        return WsProxyService.state() == WsProxyService.State.STOPPED
                || WsProxyService.state() == WsProxyService.State.ERROR;
    }

    @Override public boolean onFragmentCreate() {
        super.onFragmentCreate();
        WsProxyService.observe(observer);
        return true;
    }
    @Override public void onFragmentDestroy() {
        WsProxyService.unobserve(observer);
        super.onFragmentDestroy();
    }
    @Override protected void updateRows() {
        super.updateRows();
        headerRow = addRow("libregramWsHeader");
        enabledRow = addRow("libregramWsEnabled");
        statusRow = addRow("libregramWsStatus");
        infoRow = addRow();
        connectRow = addRow("libregramWsConnect");
        tcpRow = addRow("libregramWsTcp");
        advancedRow = addRow("libregramWsAdvanced");
        portRow = addRow("libregramWsPort");
        poolRow = addRow("libregramWsPool");
        cfRow = addRow("libregramWsCf");
        workerRow = addRow("libregramWsWorker");
        advancedInfoRow = addRow();
        diagnosticsRow = addRow("libregramWsDiagnostics");
        checkRow = addRow("libregramWsCheck");
        journalRow = addRow("libregramWsJournal");
        sourceRow = addRow("libregramWsSource");
        footerRow = addRow();
    }
    @Override protected String getActionBarTitle() { return getString(R.string.LibreGramWsProxy); }
    @Override protected String getKey() { return null; }
    @Override public int getSearchGuid() { return 24901; }
    @Override public int getSearchIcon() { return R.drawable.msg_language; }
    @Override public String getSearchPrefix() { return "LibreGramWs"; }
    @Override protected BaseListAdapter createAdapter(Context context) { return new ListAdapter(context); }

    private void message(String title, String text) {
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(title).setMessage(text)
                .setPositiveButton(getString(R.string.OK), null).create());
    }
    private void edit(String key, String title, boolean numeric, int fallback, int min, int max) {
        EditText field = new EditText(getParentActivity());
        field.setSingleLine(true);
        field.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        field.setText(numeric ? String.valueOf(prefs().getInt(key, fallback)) : prefs().getString(key, ""));
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(field)
                .setNegativeButton(getString(R.string.Cancel), null)
                .setPositiveButton(getString(R.string.OK), (dialog, which) -> {
                    try {
                        if (!idle()) return;
                        String value = field.getText().toString().trim();
                        if (numeric) {
                            int number = Integer.parseInt(value);
                            if (number < min || number > max) throw new IllegalArgumentException();
                            prefs().edit().putInt(key, number).apply();
                        } else {
                            if (!value.isEmpty()) value = WsProxyService.validateDomain(value);
                            prefs().edit().putString(key, value).apply();
                        }
                        observer.run();
                    } catch (IllegalArgumentException e) {
                        message(title, getString(R.string.LibreGramWsInvalid));
                    }
                }).create());
    }
    @Override protected void onItemClick(View view, int position, float x, float y) {
        if (position == sourceRow) {
            org.telegram.messenger.browser.Browser.openUrl(getParentActivity(),
                    "https://github.com/valnesfjord/tg-ws-proxy-rs");
        } else if (position == enabledRow) {
            if (!org.telegram.messenger.BuildConfig.DEBUG) {
                message(getActionBarTitle(), getString(R.string.LibreGramWsDebugOnly));
            } else if (WsProxyService.state() == WsProxyService.State.RUNNING
                    || WsProxyService.state() == WsProxyService.State.STARTING) {
                WsProxyService.stop(getParentActivity());
            } else if (idle()) {
                WsProxyService.start(getParentActivity());
            }
        } else if (position == checkRow) {
            WsProxyService.check();
        } else if (position == journalRow) {
            String report = "LibreGram WS Proxy\n" + WsProxyService.state().name() + "\n" + WsProxyService.events();
            showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTitle(getString(R.string.LibreGramWsJournal)).setMessage(report)
                    .setNegativeButton(getString(R.string.Close), null)
                    .setPositiveButton(getString(R.string.Copy), (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) getParentActivity()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("LibreGram WS", report));
                    }).create());
        } else if (!idle()) {
            message(getActionBarTitle(), getString(R.string.LibreGramWsStopToEdit));
        } else if (position == connectRow || position == tcpRow) {
            String key = position == connectRow ? "connect_client" : "tcp_fallback";
            boolean fallback = position == connectRow;
            if (position == tcpRow && !prefs().getBoolean(key, false)) {
                showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.LibreGramWsTcp))
                        .setMessage(getString(R.string.LibreGramWsTcpWarning))
                        .setNegativeButton(getString(R.string.Cancel), null)
                        .setPositiveButton(getString(R.string.OK), (dialog, which) -> {
                            prefs().edit().putBoolean(key, true).apply(); observer.run();
                        }).create());
            } else {
                prefs().edit().putBoolean(key, !prefs().getBoolean(key, fallback)).apply(); observer.run();
            }
        } else if (position == portRow) {
            edit("port", getString(R.string.LibreGramWsPort), true, 1443, 1024, 65535);
        } else if (position == poolRow) {
            edit("pool", getString(R.string.LibreGramWsPool), true, 2, 0, 8);
        } else if (position == cfRow || position == workerRow) {
            edit(position == cfRow ? "cf_domain" : "cf_worker",
                    getString(position == cfRow ? R.string.LibreGramWsCf : R.string.LibreGramWsWorker),
                    false, 0, 0, 0);
        }
    }
    private class ListAdapter extends BaseListAdapter {
        ListAdapter(Context context) { super(context); }
        @Override public int getItemViewType(int position) {
            if (position == headerRow || position == advancedRow || position == diagnosticsRow) return TYPE_HEADER;
            if (position == enabledRow || position == connectRow || position == tcpRow) return TYPE_CHECK;
            if (position == infoRow || position == advancedInfoRow || position == footerRow) return TYPE_INFO_PRIVACY;
            return TYPE_TEXT;
        }
        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int p = holder.getAdapterPosition();
            return p == enabledRow || p == connectRow || p == tcpRow || p == portRow || p == poolRow
                    || p == cfRow || p == workerRow || p == journalRow || p == sourceRow
                    || (p == checkRow && WsProxyService.state() == WsProxyService.State.RUNNING);
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
            if (holder.itemView instanceof HeaderCell) {
                ((HeaderCell) holder.itemView).setText(getString(position == headerRow ? R.string.LibreGramWsProxy
                        : position == advancedRow ? R.string.LibreGramWsAdvanced : R.string.LibreGramWsDiagnostics));
            } else if (holder.itemView instanceof TextCheckCell) {
                boolean enabled = WsProxyService.state() == WsProxyService.State.STARTING
                        || WsProxyService.state() == WsProxyService.State.RUNNING;
                ((TextCheckCell) holder.itemView).setTextAndCheck(
                        getString(position == enabledRow ? R.string.LibreGramWsEnable
                                : position == connectRow ? R.string.LibreGramWsConnect : R.string.LibreGramWsTcp),
                        position == enabledRow ? enabled : prefs().getBoolean(
                                position == connectRow ? "connect_client" : "tcp_fallback", position == connectRow), true);
            } else if (holder.itemView instanceof TextInfoPrivacyCell) {
                ((TextInfoPrivacyCell) holder.itemView).setText(getString(position == infoRow ? R.string.LibreGramWsInfo
                        : position == advancedInfoRow ? R.string.LibreGramWsAdvancedInfo : R.string.LibreGramWsCredits));
            } else {
                int title;
                String value = "";
                if (position == statusRow) {
                    title = R.string.LibreGramWsStatus;
                    switch (WsProxyService.state()) {
                        case STARTING: value = getString(R.string.LibreGramWsStarting); break;
                        case RUNNING: value = getString(R.string.LibreGramWsRunning); break;
                        case STOPPING: value = getString(R.string.LibreGramWsStopping); break;
                        case ERROR: value = getString(R.string.LibreGramWsError); break;
                        default: value = getString(R.string.LibreGramWsOff);
                    }
                } else if (position == portRow) { title = R.string.LibreGramWsPort; value = "" + prefs().getInt("port", 1443);
                } else if (position == poolRow) { title = R.string.LibreGramWsPool; value = "" + prefs().getInt("pool", 2);
                } else if (position == cfRow) { title = R.string.LibreGramWsCf; value = prefs().getString("cf_domain", "—");
                } else if (position == workerRow) { title = R.string.LibreGramWsWorker; value = prefs().getString("cf_worker", "—");
                } else if (position == checkRow) { title = R.string.LibreGramWsCheck; value = WsProxyService.checkResult();
                } else if (position == journalRow) { title = R.string.LibreGramWsJournal;
                } else { title = R.string.LibreGramWsEngine; value = "tg-ws-proxy-rs"; }
                ((TextCell) holder.itemView).setTextAndValueAndIcon(getString(title), value,
                        R.drawable.msg_language, position != sourceRow);
            }
        }
    }
}
