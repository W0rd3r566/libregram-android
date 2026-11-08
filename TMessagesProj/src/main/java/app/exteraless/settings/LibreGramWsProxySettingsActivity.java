package app.exteraless.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/** UI entry point only; native transport integration is not shipped yet. */
public class LibreGramWsProxySettingsActivity extends BaseNekoSettingsActivity {
    private int headerRow;
    private int statusRow;
    private int infoRow;
    private int sourceRow;
    private int footerRow;

    @Override
    protected void updateRows() {
        super.updateRows();
        headerRow = addRow("libregramWsHeader");
        statusRow = addRow("libregramWsStatus");
        infoRow = addRow("libregramWsInfo");
        sourceRow = addRow("libregramWsSource");
        footerRow = addRow("libregramWsFooter");
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.LibreGramWsProxy);
    }

    @Override
    protected String getKey() {
        return "libregramWsProxy";
    }

    @Override
    public int getSearchGuid() {
        return 24901;
    }

    @Override
    public int getSearchIcon() {
        return R.drawable.msg_language;
    }

    @Override
    public String getSearchPrefix() {
        return "LibreGramWs";
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == sourceRow) {
            org.telegram.messenger.browser.Browser.openUrl(getParentActivity(),
                    "https://github.com/valnesfjord/tg-ws-proxy-rs");
        }
    }

    private class ListAdapter extends BaseListAdapter {
        ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerRow) {
                return TYPE_HEADER;
            }
            if (position == infoRow || position == footerRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_TEXT;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getAdapterPosition() == sourceRow;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
            if (position == headerRow) {
                ((HeaderCell) holder.itemView).setText(getString(R.string.LibreGramWsProxy));
            } else if (position == statusRow) {
                ((TextCell) holder.itemView).setTextAndValueAndIcon(
                        getString(R.string.LibreGramWsStatus),
                        getString(R.string.LibreGramWsNotIntegrated), R.drawable.msg_language, false);
            } else if (position == infoRow) {
                ((TextInfoPrivacyCell) holder.itemView).setText(getString(R.string.LibreGramWsPreviewInfo));
            } else if (position == sourceRow) {
                ((TextCell) holder.itemView).setTextAndValueAndIcon(
                        getString(R.string.LibreGramWsEngine), "tg-ws-proxy-rs", R.drawable.msg_language, false);
            } else if (position == footerRow) {
                ((TextInfoPrivacyCell) holder.itemView).setText(getString(R.string.LibreGramWsCredits));
            }
        }
    }
}
