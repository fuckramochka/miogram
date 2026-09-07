package app.miogram.bridge.folders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Settings activity for configuring chat subfolders, hierarchical grouping,
 * and smart category filters.
 */
public class MiogramSubfolderSettingsActivity extends BaseNekoSettingsActivity {

    private int headerGeneralRow;
    private int enabledRow;
    private int enabledInfoRow;

    private int headerHierarchyRow;
    private int hierarchicalRow;
    private int collapseTabsRow;
    private int hierarchyInfoRow;

    private int headerSmartRow;
    private int smartFiltersRow;
    private int showCountersRow;
    private int smartInfoRow;

    @Override
    protected String getActionBarTitle() {
        return MiogramLocale.get("Підпапки та Розумні фільтри", "Подпапки и Умные фильтры", "Subfolders & Smart Filters");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerGeneralRow = addRow();
        enabledRow = addRow();
        enabledInfoRow = addRow();

        headerHierarchyRow = addRow();
        hierarchicalRow = addRow();
        collapseTabsRow = addRow();
        hierarchyInfoRow = addRow();

        headerSmartRow = addRow();
        smartFiltersRow = addRow();
        showCountersRow = addRow();
        smartInfoRow = addRow();
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == enabledRow) {
            boolean v = !MiogramSubfolderEngine.isSubfoldersEnabled();
            MiogramSubfolderEngine.setSubfoldersEnabled(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == hierarchicalRow) {
            boolean v = !MiogramSubfolderEngine.isHierarchicalEnabled();
            MiogramSubfolderEngine.setHierarchicalEnabled(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == collapseTabsRow) {
            boolean v = !MiogramSubfolderEngine.isCollapseSubfoldersEnabled();
            MiogramSubfolderEngine.setCollapseSubfoldersEnabled(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == smartFiltersRow) {
            boolean v = !MiogramSubfolderEngine.isSmartFiltersEnabled();
            MiogramSubfolderEngine.setSmartFiltersEnabled(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        } else if (position == showCountersRow) {
            boolean v = !MiogramSubfolderEngine.isShowCountersEnabled();
            MiogramSubfolderEngine.setShowCountersEnabled(v);
            if (view instanceof TextCheckCell) ((TextCheckCell) view).setChecked(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
        switch (holder.getItemViewType()) {
            case TYPE_HEADER: {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if (position == headerGeneralRow) {
                    cell.setText(MiogramLocale.get("Головні налаштування", "Главные настройки", "General"));
                } else if (position == headerHierarchyRow) {
                    cell.setText(MiogramLocale.get("Ієрархія та Групування", "Иерархия и Группировка", "Hierarchy & Grouping"));
                } else if (position == headerSmartRow) {
                    cell.setText(MiogramLocale.get("Розумні категорії", "Умные категории", "Smart Categories"));
                }
                break;
            }
            case TYPE_CHECK: {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == enabledRow) {
                    cell.setTextAndCheck(
                        MiogramLocale.get("Увімкнути панель підпапок", "Включить панель подпапок", "Enable Subfolder Bar"),
                        MiogramSubfolderEngine.isSubfoldersEnabled(),
                        false
                    );
                } else if (position == hierarchicalRow) {
                    cell.setTextAndCheck(
                        MiogramLocale.get("Ієрархічні підпапки (Parent / Child)", "Иерархические подпапки (Parent / Child)", "Hierarchical Subfolders (Parent / Child)"),
                        MiogramSubfolderEngine.isHierarchicalEnabled(),
                        true
                    );
                } else if (position == collapseTabsRow) {
                    cell.setTextAndCheck(
                        MiogramLocale.get("Ховати підпапки з верхніх вкладок", "Скрывать подпапки из верхних вкладок", "Hide Subfolders from Main Tabs"),
                        MiogramSubfolderEngine.isCollapseSubfoldersEnabled(),
                        false
                    );
                } else if (position == smartFiltersRow) {
                    cell.setTextAndCheck(
                        MiogramLocale.get("Розумні фільтри чатів", "Умные фильтры чатов", "Smart Chat Filters"),
                        MiogramSubfolderEngine.isSmartFiltersEnabled(),
                        true
                    );
                } else if (position == showCountersRow) {
                    cell.setTextAndCheck(
                        MiogramLocale.get("Лічильники непрочитаних на бейджах", "Счетчики непрочитанных на бейджах", "Unread Counters on Badges"),
                        MiogramSubfolderEngine.isShowCountersEnabled(),
                        false
                    );
                }
                break;
            }
            case TYPE_INFO_PRIVACY: {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (position == enabledInfoRow) {
                    cell.setText(MiogramLocale.get(
                        "Відображає горизонтальну смужку підпапок над чатами з можливістю створення нових підпапок в 1 дотик.",
                        "Отображает горизонтальную полосу подпапок над чатами с возможностью создания новых подпапок в 1 клик.",
                        "Displays a horizontal subfolder bar above chats with 1-tap subfolder creation."
                    ));
                } else if (position == hierarchyInfoRow) {
                    cell.setText(MiogramLocale.get(
                        "Папки з роздільником (наприклад 'Робота / Проєкти') автоматично вкладаються в батьківську папку 'Робота'.",
                        "Папки с разделителем (например 'Работа / Проекты') автоматически вкладываются в родительскую папку 'Работа'.",
                        "Folders named with a separator (e.g. 'Work / Projects') are nested under 'Work'."
                    ));
                } else if (position == smartInfoRow) {
                    cell.setText(MiogramLocale.get(
                        "Додає швидкі пігулки 'Особисті', 'Групи', 'Канали', 'Боти' та 'Непрочитані' для миттєвої фільтрації будь-якої папки.",
                        "Добавляет быстрые пилюли 'Личные', 'Группы', 'Каналы', 'Боты' и 'Непрочитанные' для фильтрации любой папки.",
                        "Adds quick pills 'Personal', 'Groups', 'Channels', 'Bots', and 'Unread' to filter any folder."
                    ));
                }
                break;
            }
        }
    }
}
