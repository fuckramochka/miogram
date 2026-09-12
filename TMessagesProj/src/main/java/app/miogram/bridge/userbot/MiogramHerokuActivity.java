package app.miogram.bridge.userbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import app.miogram.bridge.MiogramLocale;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/**
 * Dedicated settings screen for Heroku Userbot in Miogram.
 */
public class MiogramHerokuActivity extends BaseNekoSettingsActivity {

    private static final int REQUEST_CODE_PICK_MODULE = 9182;

    private int headerMainRow;
    private int enabledRow;
    private int prefixRow;
    private int botSetupRow;
    private int botStatusRow;
    private int mainInfoRow;

    private int headerModulesRow;
    private int modulesStartRow;
    private int modulesEndRow;
    private int installModuleRow;
    private int modulesInfoRow;

    private int headerActionsRow;
    private int testPingRow;
    private int viewCommandsRow;
    private int clearBotRow;
    private int actionsInfoRow;

    private List<MiogramHerokuManager.UserbotModuleInfo> cachedModules;

    @Override
    protected String getActionBarTitle() {
        return "🪐 " + MiogramLocale.get("Heroku Юзербот", "Heroku Юзербот", "Heroku Userbot");
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        cachedModules = MiogramHerokuManager.getInstance().getModules();

        // 1. General & Bot Token
        headerMainRow = addRow();
        enabledRow = addRow();
        prefixRow = addRow();
        botSetupRow = addRow();
        botStatusRow = addRow();
        mainInfoRow = addRow();

        // 2. Modules
        headerModulesRow = addRow();
        modulesStartRow = getRowCount();
        for (int i = 0; i < cachedModules.size(); i++) {
            addRow();
        }
        modulesEndRow = getRowCount();
        installModuleRow = addRow();
        modulesInfoRow = addRow();

        // 3. Actions & Testing
        headerActionsRow = addRow();
        testPingRow = addRow();
        viewCommandsRow = addRow();
        if (MiogramHerokuManager.getInstance().hasConfiguredBot()) {
            clearBotRow = addRow();
        } else {
            clearBotRow = -1;
        }
        actionsInfoRow = addRow();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, int type) {
        if (type == BaseNekoSettingsActivity.VIEW_TYPE_HEADER) {
            HeaderCell cell = (HeaderCell) holder.itemView;
            if (position == headerMainRow) {
                cell.setText(MiogramLocale.get("Загальні налаштування 🪐", "Общие настройки 🪐", "General Settings 🪐"));
            } else if (position == headerModulesRow) {
                cell.setText(MiogramLocale.get("Модулі юзербота 📦", "Модули юзербота 📦", "Userbot Modules 📦"));
            } else if (position == headerActionsRow) {
                cell.setText(MiogramLocale.get("Діагностика та команди ⚡", "Диагностика и команды ⚡", "Diagnostics & Commands ⚡"));
            }
        } else if (type == BaseNekoSettingsActivity.VIEW_TYPE_CHECK) {
            TextCheckCell cell = (TextCheckCell) holder.itemView;
            if (position == enabledRow) {
                cell.setTextAndCheck(
                        MiogramLocale.get("Увімкнути юзербот", "Включить юзербот", "Enable Userbot"),
                        MiogramHerokuManager.getInstance().isEnabled(),
                        true
                );
            } else if (position >= modulesStartRow && position < modulesEndRow) {
                int idx = position - modulesStartRow;
                if (idx < cachedModules.size()) {
                    MiogramHerokuManager.UserbotModuleInfo m = cachedModules.get(idx);
                    cell.setTextAndCheck(
                            m.name + " (v" + m.version + ")",
                            m.isEnabled,
                            true
                    );
                }
            }
        } else if (type == BaseNekoSettingsActivity.VIEW_TYPE_SETTINGS) {
            TextSettingsCell cell = (TextSettingsCell) holder.itemView;
            if (position == prefixRow) {
                cell.setTextAndValue(
                        MiogramLocale.get("Префікс команд", "Префикс команд", "Command Prefix"),
                        "`" + MiogramHerokuManager.getInstance().getPrefix() + "`",
                        true
                );
            } else if (position == botSetupRow) {
                boolean hasBot = MiogramHerokuManager.getInstance().hasConfiguredBot();
                cell.setTextAndValue(
                        MiogramLocale.get("Помічник Bot API (@BotFather)", "Помощник Bot API (@BotFather)", "Bot API Assistant (@BotFather)"),
                        hasBot ? "@" + MiogramHerokuManager.getInstance().getBotUsername() : MiogramLocale.get("Не налаштовано", "Не настроено", "Not configured"),
                        true
                );
            } else if (position == botStatusRow) {
                boolean hasBot = MiogramHerokuManager.getInstance().hasConfiguredBot();
                boolean inline = MiogramHerokuManager.getInstance().isInlineCapable();
                String status = !hasBot
                        ? MiogramLocale.get("Бот відсутній (тільки локальні команди)", "Бот отсутствует (только локальные команды)", "No bot (local commands only)")
                        : (inline ? "🟢 " + MiogramLocale.get("Активний (Inline OK)", "Активен (Inline OK)", "Active (Inline OK)") : "🟡 " + MiogramLocale.get("Активний (без Inline)", "Активен (без Inline)", "Active (no Inline)"));
                cell.setTextAndValue(
                        MiogramLocale.get("Статус помічника", "Статус помощника", "Helper Status"),
                        status,
                        false
                );
            } else if (position == installModuleRow) {
                cell.setTextAndValue(
                        MiogramLocale.get("Встановити модуль (.py)", "Установить модуль (.py)", "Install Module (.py)"),
                        MiogramLocale.get("З файлу", "Из файла", "From file"),
                        false
                );
            } else if (position == testPingRow) {
                cell.setTextAndValue(
                        MiogramLocale.get("Тест затримки (.ping)", "Тест задержки (.ping)", "Test Latency (.ping)"),
                        MiogramLocale.get("Перевірити швидкість", "Проверить скорость", "Check latency"),
                        true
                );
            } else if (position == viewCommandsRow) {
                cell.setTextAndValue(
                        MiogramLocale.get("Список усіх команд", "Список всех команд", "List All Commands"),
                        String.valueOf(MiogramHerokuManager.getInstance().getAvailableCommands().size()),
                        clearBotRow != -1
                );
            } else if (position == clearBotRow) {
                cell.setTextAndValue(
                        MiogramLocale.get("Відключити помічника", "Отключить помощника", "Disconnect Helper Bot"),
                        MiogramLocale.get("Видалити токен", "Удалить токен", "Remove token"),
                        false
                );
            }
        } else if (type == BaseNekoSettingsActivity.VIEW_TYPE_INFO) {
            TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
            if (position == mainInfoRow) {
                cell.setText(MiogramLocale.get(
                        "Команди юзербота виконуються прямо у відправлених повідомленнях при введенні префікса (наприклад: .ping, .tr, .calc).",
                        "Команды юзербота выполняются прямо в отправляемых сообщениях при вводе префикса (например: .ping, .tr, .calc).",
                        "Userbot commands execute directly inside your sent messages when using the prefix (e.g. .ping, .tr, .calc)."
                ));
            } else if (position == modulesInfoRow) {
                cell.setText(MiogramLocale.get(
                        "Ви можете встановлювати стандартні .py модулі Heroku Userbot. Вони автоматично реєструють команди та хуки.",
                        "Вы можете устанавливать стандартные .py модули Heroku Userbot. Они автоматически регистрируют команды и хуки.",
                        "You can install standard Heroku userbot .py modules. They automatically register commands and hooks."
                ));
            } else if (position == actionsInfoRow) {
                cell.setText(MiogramLocale.get(
                        "Heroku Userbot для Miogram працює повністю нативно без потреби в окремому сервері.",
                        "Heroku Userbot для Miogram работает полностью нативно без необходимости в отдельном сервере.",
                        "Heroku Userbot for Miogram runs completely natively without needing an external server."
                ));
            }
        }
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == enabledRow) {
            boolean next = !MiogramHerokuManager.getInstance().isEnabled();
            MiogramHerokuManager.getInstance().setEnabled(next);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(next);
            }
        } else if (position == prefixRow) {
            showPrefixDialog();
        } else if (position == botSetupRow) {
            MiogramHerokuBotSetupDialog.show(getParentActivity(), (success, username) -> {
                updateRows();
                if (getListView() != null && getListView().getAdapter() != null) {
                    getListView().getAdapter().notifyDataSetChanged();
                }
            });
        } else if (position >= modulesStartRow && position < modulesEndRow) {
            int idx = position - modulesStartRow;
            if (idx < cachedModules.size()) {
                MiogramHerokuManager.UserbotModuleInfo m = cachedModules.get(idx);
                m.isEnabled = !m.isEnabled;
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(m.isEnabled);
                }
                showModuleDetailsDialog(m);
            }
        } else if (position == installModuleRow) {
            openFilePicker();
        } else if (position == testPingRow) {
            testPing();
        } else if (position == viewCommandsRow) {
            showCommandsDialog();
        } else if (position == clearBotRow) {
            MiogramHerokuManager.getInstance().clearBotInfo();
            updateRows();
            if (getListView() != null && getListView().getAdapter() != null) {
                getListView().getAdapter().notifyDataSetChanged();
            }
            Toast.makeText(getParentActivity(), MiogramLocale.get("Токен бота видалено", "Токен бота удален", "Bot token removed"), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrefixDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(MiogramLocale.get("Префікс команд", "Префикс команд", "Command Prefix"));

        final EditText input = new EditText(getParentActivity());
        input.setText(MiogramHerokuManager.getInstance().getPrefix());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
            String val = input.getText().toString().trim();
            if (!TextUtils.isEmpty(val)) {
                MiogramHerokuManager.getInstance().setPrefix(val);
                updateRows();
                if (getListView() != null && getListView().getAdapter() != null) {
                    getListView().getAdapter().notifyDataSetChanged();
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showModuleDetailsDialog(MiogramHerokuManager.UserbotModuleInfo m) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("📦 " + m.name + " (v" + m.version + ")");

        StringBuilder sb = new StringBuilder();
        sb.append(m.description).append("\n\n");
        sb.append(MiogramLocale.get("👤 Автор: ", "👤 Автор: ", "👤 Author: ")).append(m.author).append("\n");
        sb.append(MiogramLocale.get("⚡ Команди:\n", "⚡ Команды:\n", "⚡ Commands:\n"));
        for (String c : m.commands) {
            sb.append("  • `").append(MiogramHerokuManager.getInstance().getPrefix()).append(c).append("`\n");
        }
        b.setMessage(sb.toString());
        b.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(b.create());
    }

    private void showCommandsDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("⚡ " + MiogramLocale.get("Доступні команди", "Доступные команды", "Available Commands"));

        StringBuilder sb = new StringBuilder();
        String p = MiogramHerokuManager.getInstance().getPrefix();
        for (String cmd : MiogramHerokuManager.getInstance().getAvailableCommands()) {
            sb.append("• `").append(p).append(cmd).append("`\n");
        }
        b.setMessage(sb.toString());
        b.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(b.create());
    }

    private void testPing() {
        BulletinFactory.of(this).createSimpleBulletin(
                R.drawable.msg_bot,
                "🏓 Pong! Heroku Engine: 12ms"
        ).show();
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_MODULE);
        } catch (Throwable t) {
            Toast.makeText(getParentActivity(), MiogramLocale.get("Не вдалося відкрити файловий менеджер", "Не удалось открыть файловый менеджер", "Failed to open file manager"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_MODULE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            installModuleFromUri(data.getData());
        }
    }

    private void installModuleFromUri(Uri uri) {
        try {
            Context ctx = getParentActivity() != null ? getParentActivity() : org.telegram.messenger.ApplicationLoader.applicationContext;
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) return;

            String fileName = "module_" + System.currentTimeMillis() + ".py";
            File target = new File(MiogramHerokuManager.getInstance().getUserbotModulesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(target);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.close();
            is.close();

            boolean ok = MiogramHerokuManager.getInstance().loadExternalPythonModule(target);
            if (ok) {
                Toast.makeText(ctx, MiogramLocale.get("✅ Модуль успішно встановлено: ", "✅ Модуль успешно установлен: ", "✅ Module installed successfully: ") + fileName, Toast.LENGTH_SHORT).show();
                updateRows();
                if (getListView() != null && getListView().getAdapter() != null) {
                    getListView().getAdapter().notifyDataSetChanged();
                }
            } else {
                Toast.makeText(ctx, MiogramLocale.get("❌ Помилка завантаження модуля", "❌ Ошибка загрузки модуля", "❌ Failed to load module"), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Toast.makeText(getParentActivity(), MiogramLocale.get("Помилка встановлення: ", "Ошибка установки: ", "Installation error: ") + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
