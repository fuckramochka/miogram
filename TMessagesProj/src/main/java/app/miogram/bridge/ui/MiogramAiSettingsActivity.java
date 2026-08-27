package app.miogram.bridge.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;
import xyz.nextalone.nagram.NaConfig;

/**
 * Unified Miogram AI Settings:
 * - Direct connection for Voice-to-Text Transcription (Gemini 2.5 Flash)
 * - BYOK API Key Vault (single key activates transcription, summarization, and chat assistant)
 * - Privacy Protection (PII redaction)
 * - Model Selector (Gemini 2.5 Flash, Gemini 2.5 Pro)
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
        String k = prefs().getString("gemini_api_key", "");
        if (k.isEmpty()) {
            k = prefs().getString("gemini_key", "");
        }
        if (k.isEmpty()) {
            k = NaConfig.INSTANCE.getTranscribeProviderGeminiApiKey().String();
        }
        return k;
    }

    private String savedModel() {
        return prefs().getString("gen_model", "gemini-2.5-flash");
    }

    private boolean piiMaskEnabled() {
        return prefs().getBoolean("pii_mask", true);
    }

    private void saveKey(String key) {
        String trimmed = key.trim();
        prefs().edit()
                .putString("gemini_api_key", trimmed)
                .putString("gemini_key", trimmed)
                .apply();
        NaConfig.INSTANCE.getTranscribeProviderGeminiApiKey().setConfigString(trimmed);
        NaConfig.INSTANCE.getLlmProviderGeminiKey().setConfigString(trimmed);
        listAdapter.notifyItemChanged(keyRow);
    }

    private void saveModel(String model) {
        prefs().edit().putString("gen_model", model).apply();
        listAdapter.notifyItemChanged(modelRow);
    }

    private static String maskKey(String key) {
        if (key == null || key.isEmpty()) {
            return "Не встановлено";
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
        builder.setTitle("Google Gemini API Key");

        EditTextBoldCursor input = new EditTextBoldCursor(ctx);
        input.setText(savedKey());
        input.setHint("AIzaSy…");
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));

        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            saveKey(input.getText().toString());
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setNeutralButton("Очистити", (dialog, which) -> {
            saveKey("");
        });
        showDialog(builder.create());
    }

    private void showModelPicker() {
        Context ctx = getParentActivity();
        if (ctx == null) return;

        String[] models = {"gemini-2.5-flash (Рекомендовано, 0.3с)", "gemini-2.5-pro (Глибокий аналіз)", "gemini-2.0-flash"};
        String[] modelKeys = {"gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Модель Miogram AI");
        builder.setItems(models, (dialog, which) -> {
            saveModel(modelKeys[which]);
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
                        cell.setText("Конфігурація Gemini AI");
                    } else if (position == headerFeaturesRow) {
                        cell.setText("Застосування Miogram AI");
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == keyRow) {
                        cell.setTextAndValue("API Ключ Gemini", maskKey(savedKey()), true);
                    } else if (position == modelRow) {
                        cell.setTextAndValue("Модель ШІ", savedModel(), true);
                    } else if (position == voiceTranscribeInfoRow) {
                        cell.setTextAndValue("Розшифровка аудіо та кружечків", "Увімкнено (Gemini Multimodal)", false);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell cell = (TextCell) holder.itemView;
                    if (position == getKeyRow) {
                        cell.setTextAndIcon("Отримати безкоштовний ключ на Google AI Studio", R.drawable.msg_bot, false);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell cell = (TextCheckCell) holder.itemView;
                    if (position == piiMaskRow) {
                        cell.setTextAndCheck("Приховувати персональні дані (PII Shield)", piiMaskEnabled(), false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == aiInfoRow) {
                        cell.setText("Один ключ Gemini активує всі функції штучного інтелекту: миттєву розшифровку голосових, переклад та помічника.");
                    } else if (position == featuresInfoRow) {
                        cell.setText("Натисніть кнопку розшифровки на будь-якому голосовому повідомленні або кружечку в чаті для отримання тексту за 0.3 секунди.");
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
