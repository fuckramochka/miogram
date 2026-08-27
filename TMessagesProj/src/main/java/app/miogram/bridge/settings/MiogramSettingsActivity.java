package app.miogram.bridge.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

import app.miogram.bridge.ui.MiogramAiSettingsActivity;
import app.miogram.bridge.ui.MiogramChatsSettingsActivity;
import app.miogram.bridge.ui.MiogramPerformanceActivity;
import app.miogram.bridge.ui.MiogramPrivacySettingsActivity;
import app.miogram.bridge.ui.MiogramVisualsActivity;
import app.miogram.bridge.updater.MiogramUpdater;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoTranslatorSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Unified Main Miogram Settings Hub.
 */
public class MiogramSettingsActivity extends BaseNekoSettingsActivity {

    private int headerCategoriesRow;
    private int visualsRow;
    private int chatsRow;
    private int privacyRow;
    private int translatorRow;
    private int performanceRow;
    private int categoriesInfoRow;

    private int headerAdvancedRow;
    private int aiRow;
    private int pluginsRow;
    private int updaterRow;
    private int advancedInfoRow;

    @Override
    protected String getActionBarTitle() {
        return "Налаштування Miogram";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerCategoriesRow = addRow();
        visualsRow = addRow();
        chatsRow = addRow();
        privacyRow = addRow();
        translatorRow = addRow();
        performanceRow = addRow();
        categoriesInfoRow = addRow();

        headerAdvancedRow = addRow();
        aiRow = addRow();
        pluginsRow = addRow();
        updaterRow = addRow();
        advancedInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == visualsRow) {
            presentFragment(new MiogramVisualsActivity());
        } else if (position == chatsRow) {
            presentFragment(new MiogramChatsSettingsActivity());
        } else if (position == privacyRow) {
            presentFragment(new MiogramPrivacySettingsActivity());
        } else if (position == translatorRow) {
            presentFragment(new NekoTranslatorSettingsActivity());
        } else if (position == performanceRow) {
            presentFragment(new MiogramPerformanceActivity());
        } else if (position == aiRow) {
            presentFragment(new MiogramAiSettingsActivity());
        } else if (position == pluginsRow) {
            presentFragment(new app.exteraless.plugins.ui.PluginsActivity());
        } else if (position == updaterRow) {
            MiogramUpdater.checkAndShowUpdate(this, true);
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerCategoriesRow || position == headerAdvancedRow) {
                return TYPE_HEADER;
            } else if (position == categoriesInfoRow || position == advancedInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_TEXT;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerCategoriesRow) {
                        cell.setText("Основні розділи");
                    } else if (position == headerAdvancedRow) {
                        cell.setText("Інтелект та Інструменти");
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == visualsRow) {
                        cell.setTextAndIcon("Зовнішній вигляд (Рідке скло, Аватарки, Теми)", R.drawable.msg_theme, true);
                    } else if (position == chatsRow) {
                        cell.setTextAndIcon("Чати та Медіа (CameraX, Дабл-тап, Стікери)", R.drawable.msg_camera, true);
                    } else if (position == privacyRow) {
                        cell.setTextAndIcon("Конфіденційність та Ghost Mode (AyuGram, Сховок)", R.drawable.msg_secret, true);
                    } else if (position == translatorRow) {
                        cell.setTextAndIcon("Перекладач (Google, DeepL, Yandex)", R.drawable.msg_translate, true);
                    } else if (position == performanceRow) {
                        cell.setTextAndIcon("Продуктивність (Download Boost 12x, Анімації)", R.drawable.msg_speed, false);
                    } else if (position == aiRow) {
                        cell.setTextAndIcon("Miogram AI (Gemini 2.5 + Whisper Voice)", R.drawable.msg_bot, true);
                    } else if (position == pluginsRow) {
                        cell.setTextAndIcon("Плагіни Miogram (Rust / WASM / Py)", R.drawable.msg_folders, true);
                    } else if (position == updaterRow) {
                        cell.setTextAndIcon("Перевірити оновлення Miogram", R.drawable.msg_update, false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == categoriesInfoRow) {
                        cell.setText("Повний набір функцій exteraless та Nagram, оптимізований та об'єднаний у Miogram.");
                    } else if (position == advancedInfoRow) {
                        cell.setText("Нативні розширення на WebAssembly, голосовий ШІ та система безшовних оновлень.");
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }
}
