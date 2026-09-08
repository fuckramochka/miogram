package app.miogram.bridge.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

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
import org.telegram.ui.Components.EditTextBoldCursor;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.MiogramAiService;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram AI Settings with dynamic multilingual localization:
 * - Direct connection for Voice-to-Text Transcription (Gemini 3.5 Flash Lite)
 * - BYOK keyring with automatic rotation across multiple Gemini keys
 * - Privacy Protection (PII redaction)
 * - Model Selector (Gemini 3.5 Flash Lite, Gemini 3.5 Flash, Gemini 2.5 Flash)
 */
public class MiogramAiSettingsActivity extends BaseNekoSettingsActivity {

    private static final String PREFS = "miogram_ai_prefs";

    private int headerAiRow;
    private int keyRow;
    private int modelRow;
    private int getKeyRow;
    private int aiInfoRow;

    private int headerFeaturesRow;
    private int voiceTranscribeInfoRow;
    private int piiMaskRow;
    private int featuresInfoRow;

    @Override
    protected String getActionBarTitle() {
        return "Miogram AI";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        headerAiRow = addRow();
        keyRow = addRow();
        modelRow = addRow();
        getKeyRow = addRow();
        aiInfoRow = addRow();

        headerFeaturesRow = addRow();
        voiceTranscribeInfoRow = addRow();
        piiMaskRow = addRow();
        featuresInfoRow = addRow();
    }

    private android.content.SharedPreferences prefs() {
        Context ctx = getParentActivity() != null ? getParentActivity() : org.telegram.messenger.ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private String savedKey() {
        java.util.List<String> keys = MiogramAiService.getApiKeys();
        return keys.isEmpty() ? "" : keys.get(0);
    }

    private String savedKeysText() {
        return android.text.TextUtils.join("\n", MiogramAiService.getApiKeys());
    }

    private String keySummary() {
        int count = MiogramAiService.getApiKeys().size();
        if (count == 0) return maskKey("");
        if (count == 1) return maskKey(savedKey());
        return MiogramLocale.get(count + " ключі", count + " ключей", count + " keys") + " · " + maskKey(savedKey());
    }

    private String savedModel() {
        return MiogramAiService.getModel();
    }

    private boolean piiMaskEnabled() {
        return prefs().getBoolean("pii_mask", true);
    }

    private void saveKeys(String raw) {
        MiogramAiService.setApiKeys(MiogramAiService.parseApiKeys(raw));
        listAdapter.notifyItemChanged(keyRow);
    }

    private void saveModel(String model) {
        MiogramAiService.setModel(model);
        listAdapter.notifyItemChanged(modelRow);
    }

    private static String maskKey(String key) {
        if (key == null || key.isEmpty()) {
            return MiogramLocale.get("Не встановлено", "Не установлено", "Not set");
        }
        return key.substring(0, Math.min(6, key.length())) + "…••••";
    }

    @Override
    public void onItemClick(View view, int position, float x, float y) {
        if (position == keyRow) {
            showKeyDialog();
        } else if (position == modelRow) {
            showModelPicker();
        } else if (position == getKeyRow) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
                getParentActivity().startActivity(browserIntent);
            } catch (Exception ignored) {}
        } else if (position == piiMaskRow) {
            boolean next = !piiMaskEnabled();
            prefs().edit().putBoolean("pii_mask", next).apply();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(next);
            }
        }
    }

    private void showKeyDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Ключі Google Gemini", "Ключи Google Gemini", "Google Gemini API Keys"));

        EditTextBoldCursor input = new EditTextBoldCursor(ctx);
        input.setText(savedKeysText());
        input.setHint("AIzaSy…\nAIzaSy…");
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            saveKeys(input.getText().toString());
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setNeutralButton(MiogramLocale.get("Очистити", "Очистить", "Clear"), (dialog, which) -> {
            saveKeys("");
        });
        showDialog(builder.create());
    }

    private void showModelPicker() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] models = {
                "gemini-2.5-flash (" + MiogramLocale.get("Швидка, рекомендовано", "Быстрая, рекомендовано", "Fast, recommended") + ")",
                "gemini-2.5-pro (" + MiogramLocale.get("Глибокий аналіз", "Глубокий анализ", "Deep reasoning") + ")",
                "gemini-2.0-flash (" + MiogramLocale.get("Стабільна", "Стабильная", "Stable") + ")",
                MiogramLocale.get("Вказати власну модель…", "Указать свою модель…", "Custom model…")
        };
        String[] modelKeys = {"gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "custom"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Модель Miogram AI", "Модель Miogram AI", "Miogram AI Model"));
        builder.setItems(models, (dialog, which) -> {
            if ("custom".equals(modelKeys[which])) {
                showCustomModelDialog();
            } else {
                saveModel(modelKeys[which]);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showCustomModelDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(MiogramLocale.get("Власна назва моделі", "Своё название модели", "Custom Model Name"));

        EditTextBoldCursor input = new EditTextBoldCursor(ctx);
        input.setText(savedModel());
        input.setHint("gemini-2.5-flash");
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                saveModel(val);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerAiRow || position == headerFeaturesRow) {
                return TYPE_HEADER;
            } else if (position == keyRow || position == modelRow || position == voiceTranscribeInfoRow) {
                return TYPE_SETTINGS;
            } else if (position == getKeyRow) {
                return TYPE_TEXT;
            } else if (position == piiMaskRow) {
                return TYPE_CHECK;
            } else if (position == aiInfoRow || position == featuresInfoRow) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == headerAiRow) {
                        cell.setText(MiogramLocale.get("Конфігурація Gemini AI", "Конфигурация Gemini AI", "Gemini AI Configuration"));
                    } else if (position == headerFeaturesRow) {
                        cell.setText(MiogramLocale.get("Застосування Miogram AI", "Применение Miogram AI", "Miogram AI Applications"));
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == keyRow) {
                        cell.setTextAndValue(MiogramLocale.get("API ключі Gemini", "API ключи Gemini", "Gemini API Keys"), keySummary(), true);
                    } else if (position == modelRow) {
                        cell.setTextAndValue(MiogramLocale.get("Модель ШІ", "Модель ИИ", "AI Model"), savedModel(), true);
                    } else if (position == voiceTranscribeInfoRow) {
                        cell.setTextAndValue(MiogramLocale.get("Розшифровка аудіо та кружечків", "Расшифровка аудио и кружочков", "Voice & Video Notes"),
                                MiogramLocale.get("Увімкнено (Gemini Multimodal)", "Включено (Gemini Multimodal)", "Enabled (Gemini Multimodal)"), false);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == getKeyRow) {
                        cell.setTextAndIcon(MiogramLocale.get("Отримати безкоштовний ключ на Google AI Studio", "Получить бесплатный ключ на Google AI Studio", "Get free API key on Google AI Studio"), R.drawable.msg_bot, false);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == piiMaskRow) {
                        cell.setTextAndCheck(MiogramLocale.get("Приховувати персональні дані (PII Shield)", "Скрывать личные данные (PII Shield)", "Protect Personal Data (PII Shield)"), piiMaskEnabled(), false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == aiInfoRow) {
                        cell.setText(MiogramLocale.get("Додайте один або кілька ключів Gemini, по одному в рядку. Miogram обирає ключі по черзі та переходить до наступного, коли ключ неавторизований або вичерпав квоту.",
                                "Добавьте один или несколько ключей Gemini, по одному в строке. Miogram выбирает ключи по очереди и переходит к следующему, когда ключ не авторизован или исчерпал квоту.",
                                "Add one or more Gemini keys, one per line. Miogram rotates keys and tries the next one when a key is unauthorized or out of quota."));
                    } else if (position == featuresInfoRow) {
                        cell.setText(MiogramLocale.get("Натисніть кнопку розшифровки на будь-якому голосовому повідомленні або кружечку в чаті для отримання тексту за 0.3 секунди.",
                                "Нажмите кнопку расшифровки на любом голосовом сообщении или кружочке в чате для получения текста за 0.3 секунды.",
                                "Tap the transcribe icon on any voice message or video note to get verbatim text in 0.3 seconds."));
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
