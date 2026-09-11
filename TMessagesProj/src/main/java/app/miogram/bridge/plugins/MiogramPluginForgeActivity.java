package app.miogram.bridge.plugins;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.ai.MiogramAiService;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Plugin Forge: describe a plugin in words, get a Rust WASM scaffold.
 *
 * <p>Flow: description -> dedicated model ({@code gemini-3.8-flash}) ->
 * validated lib.rs + Cargo.toml + manifest.json saved under app files ->
 * on-device build attempt (real where a toolchain exists, exact desktop
 * instructions otherwise). Python-plugin output is intentionally NOT
 * produced here: generated code targets the WASM ABI, not Chaquopy.
 */
public class MiogramPluginForgeActivity extends BaseFragment {

    private static final int REQUEST_CODE_PICK_WASM = 1421;

    private EditTextBoldCursor input;
    private TextView modelNote;
    private TextView generateBtn;
    private ProgressBar progress;
    private ProgressBar buildProgress;
    private TextView statusView;
    private TextView codeView;
    private LinearLayout resultRow;
    private TextView saveBtn;
    private TextView buildBtn;
    private TextView copyBtn;
    private LinearLayout fallbackRow;
    private TextView importWasmBtn;
    private TextView exportZipBtn;

    private MiogramAiService.ForgeResult lastResult;
    private File lastProjectDir;
    private String forgeLanguage = "rust";
    private TextView langRustBtn;
    private TextView langGoBtn;
    private TextView stepsView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(MiogramLocale.get("Кузня плагінів", "Кузница плагинов", "Plugin Forge"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout root = (FrameLayout) fragmentView;
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scroll = new ScrollView(context);
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView hint = new TextView(context);
        hint.setText(MiogramLocale.get(
                "Опишіть плагін своїми словами — ШІ напише Rust-код під WASM ABI і збере проєкт.",
                "Опишите плагин своими словами — ИИ напишет Rust-код под WASM ABI и соберет проект.",
                "Describe the plugin in words — AI writes the Rust code for the WASM ABI and scaffolds the project."));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        content.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        input = new EditTextBoldCursor(context);
        input.setHint(MiogramLocale.get("Наприклад: плагін, що робить текст великими літерами за командою upper…",
                "Например: плагин, делающий текст заглавным по команде upper…",
                "E.g. a plugin that uppercases text on the upper command…"));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite)));
        input.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        content.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        modelNote = new TextView(context);
        modelNote.setText(MiogramLocale.get("Модель: ", "Модель: ", "Model: ") + MiogramAiService.PLUGIN_MODEL);
        modelNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        modelNote.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        content.addView(modelNote, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        LinearLayout langRow = new LinearLayout(context);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRustBtn = makeLangButton(context, "Rust");
        langRustBtn.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            forgeLanguage = "rust";
            refreshLangButtons();
        });
        langRow.addView(langRustBtn, LayoutHelper.createLinear(0, 40, 1.0f, 0, 0, 6, 0));
        langGoBtn = makeLangButton(context, "Go + TinyGo");
        langGoBtn.setOnClickListener(v -> {
            MiogramHaptic.select(v);
            forgeLanguage = "go";
            refreshLangButtons();
        });
        langRow.addView(langGoBtn, LayoutHelper.createLinear(0, 40, 1.0f, 6, 0, 0, 0));
        content.addView(langRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        refreshLangButtons();

        generateBtn = makeButton(context, MiogramLocale.get("Згенерувати плагін", "Сгенерировать плагин", "Generate plugin"), true);
        generateBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onGenerate();
        });
        content.addView(generateBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 12));

        progress = new ProgressBar(context);
        progress.setVisibility(View.GONE);
        content.addView(progress, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));        statusView = new TextView(context);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        content.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        codeView = new TextView(context);
        codeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        codeView.setTypeface(android.graphics.Typeface.MONOSPACE);
        codeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        codeView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite)));
        codeView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        codeView.setVisibility(View.GONE);
        content.addView(codeView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        stepsView = new TextView(context);
        stepsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        stepsView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        stepsView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite)));
        stepsView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        stepsView.setVisibility(View.GONE);
        content.addView(stepsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        resultRow = new LinearLayout(context);
        resultRow.setOrientation(LinearLayout.HORIZONTAL);
        resultRow.setVisibility(View.GONE);

        saveBtn = makeButton(context, MiogramLocale.get("Зберегти", "Сохранить", "Save"), false);
        saveBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onSave();
        });
        resultRow.addView(saveBtn, LayoutHelper.createLinear(0, 46, 1.0f, 0, 0, 6, 0));

        copyBtn = makeButton(context, MiogramLocale.get("Копія", "Копия", "Copy"), false);
        copyBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onCopy();
        });
        resultRow.addView(copyBtn, LayoutHelper.createLinear(0, 46, 1.0f, 6, 0, 6, 0));

        buildBtn = makeButton(context, MiogramLocale.get("Зібрати", "Собрать", "Build"), true);
        buildBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onBuild();
        });
        resultRow.addView(buildBtn, LayoutHelper.createLinear(0, 46, 1.0f, 6, 0, 0, 0));

        content.addView(resultRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        fallbackRow = new LinearLayout(context);
        fallbackRow.setOrientation(LinearLayout.HORIZONTAL);
        fallbackRow.setVisibility(View.GONE);

        importWasmBtn = makeButton(context, MiogramLocale.get("📥 Імпорт plugin.wasm", "📥 Импорт plugin.wasm", "📥 Import plugin.wasm"), false);
        importWasmBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onPickWasm();
        });
        fallbackRow.addView(importWasmBtn, LayoutHelper.createLinear(0, 44, 1.0f, 0, 0, 6, 0));

        exportZipBtn = makeButton(context, MiogramLocale.get("📤 Експорт ZIP", "📤 Экспорт ZIP", "📤 Export ZIP"), false);
        exportZipBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            onExportZip();
        });
        fallbackRow.addView(exportZipBtn, LayoutHelper.createLinear(0, 44, 1.0f, 6, 0, 0, 0));

        content.addView(fallbackRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        buildProgress = new ProgressBar(context);
        buildProgress.setVisibility(View.GONE);
        content.addView(buildProgress, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 8, 0, 0, 0));
        return fragmentView;
    }

    private TextView makeButton(Context context, String text, boolean primary) {
        TextView b = new TextView(context);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(AndroidUtilities.bold());
        b.setGravity(Gravity.CENTER);
        if (primary) {
            b.setTextColor(0xFFFFFFFF);
            b.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12),
                    Theme.getColor(Theme.key_featuredStickers_addButton),
                    Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        } else {
            b.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            b.setBackground(Theme.getSelectorDrawable(false));
        }
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        generateBtn.setAlpha(busy ? 0.5f : 1f);
        generateBtn.setClickable(!busy);
    }

    private TextView makeLangButton(Context context, String text) {
        TextView b = new TextView(context);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(AndroidUtilities.bold());
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private void refreshLangButtons() {
        paintLangButton(langRustBtn, "rust".equals(forgeLanguage));
        paintLangButton(langGoBtn, "go".equals(forgeLanguage));
    }

    private void paintLangButton(TextView b, boolean selected) {
        if (b == null) return;
        b.setTextColor(selected ? 0xFFFFFFFF : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        b.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10),
                selected ? Theme.getColor(Theme.key_featuredStickers_addButton)
                        : Theme.getColor(Theme.key_windowBackgroundWhite)));
        b.setContentDescription(b.getText());
    }

    private void renderSteps() {
        if (lastResult == null || lastResult.steps == null || lastResult.steps.isEmpty()) {
            stepsView.setVisibility(View.GONE);
            return;
        }
        StringBuilder sb = new StringBuilder(MiogramLocale.get("Як це працює:\n", "Как это работает:\n", "How it works:\n"));
        int n = 1;
        for (String s : lastResult.steps) {
            sb.append(n++).append(". ").append(s).append('\n');
        }
        stepsView.setText(sb.toString().trim());
        stepsView.setVisibility(View.VISIBLE);
    }

    private void onGenerate() {
        String desc = input.getText().toString().trim();
        if (desc.isEmpty()) {
            Toast.makeText(getParentActivity(), MiogramLocale.get("Опишіть плагін спочатку", "Сначала опишите плагин", "Describe the plugin first"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!MiogramAiService.hasApiKey()) {
            statusView.setText(MiogramLocale.get("Додайте Gemini API-ключ у Miogram AI — без нього кузня глуха.",
                    "Добавьте Gemini API-ключ в Miogram AI — без него кузница глуха.",
                    "Add a Gemini API key in Miogram AI first — the forge is deaf without it."));
            presentFragment(new app.miogram.bridge.ui.MiogramAiSettingsActivity());
            return;
        }
        setBusy(true);
        statusView.setText(MiogramLocale.get("Модель думає…", "Модель думает…", "Model is thinking…"));
        final String lang = forgeLanguage;
        MiogramAiService.generatePluginCode(desc, lang, (res, err) -> {
            setBusy(false);
            if (res != null && res.hasCode()) {
                lastResult = res;
                lastProjectDir = null;
                codeView.setText(res.code);
                codeView.setVisibility(View.VISIBLE);
                resultRow.setVisibility(View.VISIBLE);
                fallbackRow.setVisibility(View.VISIBLE);
                renderSteps();
                statusView.setText(MiogramLocale.get("Готово: ", "Готово: ", "Done: ") + res.name + " (" + res.id + ", " + res.language + ")");
                MiogramHaptic.success(codeView);
            } else {
                // Offline fallback: valid echo scaffold from the local template.
                String id = MioForgeScaffold.sanitizeId(desc.length() > 24 ? desc.substring(0, 24) : desc);
                String code = "go".equals(lang) ? MioForgeScaffold.goMain(id) : MioForgeScaffold.libRs(id);
                lastResult = new MiogramAiService.ForgeResult(id, id,
                        MiogramLocale.get("Офлайн-заготовка (echo). Опишіть ще раз з інтернетом для повного коду.",
                                "Офлайн-заготовка (echo). Опишите еще раз с интернетом для полного кода.",
                                "Offline echo scaffold. Describe again online for full code."),
                        "Utility", lang, code, null);
                lastProjectDir = null;
                codeView.setText(lastResult.code);
                codeView.setVisibility(View.VISIBLE);
                resultRow.setVisibility(View.VISIBLE);
                fallbackRow.setVisibility(View.VISIBLE);
                renderSteps();
                statusView.setText(err != null ? err : MiogramLocale.get("Модель мовчить — підставлено заготовку.",
                        "Модель молчит — подставлена заготовка.", "Model is silent — scaffold used instead."));
            }
        });
    }

    private void onSave() {
        saveProject(null);
    }

    /** Saves the scaffold; runs onDone on the UI thread afterwards (null = toast only). */
    private void saveProject(Runnable onDone) {
        if (lastResult == null) return;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context ctx = ApplicationLoader.applicationContext;
                File dir = new File(new File(ctx.getFilesDir(), "forge"), lastResult.id);
                if (!dir.exists()) dir.mkdirs();
                if ("go".equals(lastResult.language)) {
                    writeFile(new File(dir, "main.go"), lastResult.code);
                    writeFile(new File(dir, "go.mod"), MioForgeScaffold.goMod(lastResult.id));
                } else {
                    File srcDir = new File(dir, "src");
                    if (!srcDir.exists()) srcDir.mkdirs();
                    writeFile(new File(srcDir, "lib.rs"), lastResult.code);
                    writeFile(new File(dir, "Cargo.toml"), MioForgeScaffold.cargoToml(lastResult.id));
                }
                writeFile(new File(dir, "manifest.json"),
                        MioForgeScaffold.manifestJson(lastResult.id, lastResult.name, lastResult.description, lastResult.category, lastResult.language));
                lastProjectDir = dir;
                AndroidUtilities.runOnUIThread(() -> {
                    statusView.setText(MiogramLocale.get("Збережено: ", "Сохранено: ", "Saved: ") + dir.getAbsolutePath());
                    Toast.makeText(getParentActivity(), MiogramLocale.get("Проєкт збережено", "Проект сохранен", "Project saved"), Toast.LENGTH_SHORT).show();
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() ->
                        statusView.setText(e.getMessage() != null ? e.getMessage() : "save failed"));
            }
        });
    }

    private static void writeFile(File f, String content) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f, false)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private void onCopy() {
        if (lastResult == null || lastResult.code == null) return;
        try {
            ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Forge " + lastResult.id, lastResult.code));
                Toast.makeText(getParentActivity(), MiogramLocale.get("Код скопійовано", "Код скопирован", "Code copied"), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {}
    }

    private void onBuild() {
        if (lastResult == null) {
            statusView.setText(MiogramLocale.get("Спочатку згенеруйте код.", "Сначала сгенерируйте код.", "Generate code first."));
            return;
        }
        if (lastProjectDir == null) {
            // Autosave, then build — one tap does the whole pipeline.
            statusView.setText(MiogramLocale.get("Зберігаю проєкт…", "Сохраняю проект…", "Saving project…"));
            saveProject(this::startBuild);
            return;
        }
        startBuild();
    }

    private void startBuild() {
        final File dir = lastProjectDir;
        if (dir == null) return;
        final String lang = lastResult != null ? lastResult.language : "rust";
        buildBtn.setAlpha(0.5f);
        buildBtn.setClickable(false);
        buildProgress.setVisibility(View.VISIBLE);
        statusView.setText(MiogramLocale.get("Шукаю тулчейн і збираю…", "Ищу тулчейн и собираю…", "Probing toolchain and building…"));
        MioForgeBuilder.probeAndBuild(dir, lang,
                (MioForgeBuilder.BuildLogListener) live ->
                        AndroidUtilities.runOnUIThread(() -> statusView.setText(live)),
                (success, log) -> AndroidUtilities.runOnUIThread(() -> {
                    buildProgress.setVisibility(View.GONE);
                    buildBtn.setAlpha(1f);
                    buildBtn.setClickable(true);
                    statusView.setText((success ? "BUILD OK\n" : "") + log);
                    if (fallbackRow != null) {
                        fallbackRow.setVisibility(View.VISIBLE);
                    }
                    if (success) {
                        MiogramHaptic.success(statusView);
                        Toast.makeText(getParentActivity(),
                                MiogramLocale.get("Плагін зібрано: plugin.wasm", "Плагин собран: plugin.wasm", "Plugin built: plugin.wasm"),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getParentActivity(),
                                MiogramLocale.get("Збірка не вдалася — скористайтесь імпортом або експортом", "Сборка не удалась — воспользуйтесь импортом или экспортом", "Build failed — use import or export below"),
                                Toast.LENGTH_LONG).show();
                    }
                }));
    }

    private void onPickWasm() {
        if (lastProjectDir == null) {
            if (lastResult != null) {
                saveProject(this::onPickWasm);
                return;
            }
            Toast.makeText(getParentActivity(), MiogramLocale.get("Спочатку згенеруйте проєкт", "Сначала сгенерируйте проект", "Generate project first"), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, MiogramLocale.get("Виберіть plugin.wasm", "Выберите plugin.wasm", "Select plugin.wasm")), REQUEST_CODE_PICK_WASM);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_WASM && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            importWasmFromUri(uri);
        }
    }

    private void importWasmFromUri(Uri uri) {
        if (lastProjectDir == null) {
            if (lastResult != null) {
                saveProject(() -> importWasmFromUri(uri));
                return;
            }
            Toast.makeText(getParentActivity(), MiogramLocale.get("Спочатку згенеруйте проєкт", "Сначала сгенерируйте проект", "Generate project first"), Toast.LENGTH_SHORT).show();
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context ctx = ApplicationLoader.applicationContext;
                File dest = new File(lastProjectDir, "plugin.wasm");
                try (java.io.InputStream in = ctx.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(dest)) {
                    if (in == null) throw new IllegalStateException("Cannot read selected file");
                    byte[] buf = new byte[65536];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                }
                AndroidUtilities.runOnUIThread(() -> {
                    statusView.setText(MiogramLocale.get("ІМПОРТОВАНО: ", "ИМПОРТИРОВАНО: ", "IMPORTED: ") + dest.getAbsolutePath() + " (" + dest.length() + " bytes)");
                    MiogramHaptic.success(statusView);
                    Toast.makeText(getParentActivity(), MiogramLocale.get("plugin.wasm успішно імпортовано в проєкт!", "plugin.wasm успешно импортирован в проект!", "plugin.wasm successfully imported!"), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    statusView.setText(MiogramLocale.get("Помилка імпорту: ", "Ошибка импорта: ", "Import error: ") + e.getMessage());
                    Toast.makeText(getParentActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onExportZip() {
        if (lastProjectDir == null) {
            if (lastResult != null) {
                saveProject(this::onExportZip);
                return;
            }
            Toast.makeText(getParentActivity(), MiogramLocale.get("Спочатку згенеруйте проєкт", "Сначала сгенерируйте проект", "Generate project first"), Toast.LENGTH_SHORT).show();
            return;
        }
        final File dir = lastProjectDir;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                Context ctx = ApplicationLoader.applicationContext;
                File zipFile = new File(ctx.getCacheDir(), dir.getName() + "_forge_project.zip");
                if (zipFile.exists()) zipFile.delete();

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    zipDirectory(dir, dir.getName(), zos);
                    zos.flush();
                }

                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("application/zip");
                        Uri uri = FileProvider.getUriForFile(
                                ApplicationLoader.applicationContext,
                                ApplicationLoader.getApplicationId() + ".provider",
                                zipFile
                        );
                        share.putExtra(Intent.EXTRA_STREAM, uri);
                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getParentActivity().startActivity(Intent.createChooser(share, MiogramLocale.get("Поділитися проєктом плагіна", "Поделиться проектом плагина", "Share plugin project")));
                    } catch (Exception e) {
                        FileLog.e(e);
                        Toast.makeText(getParentActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "Zip error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws Exception {
        File[] files = folder.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[8192];
        for (File f : files) {
            if (f.isDirectory()) {
                zipDirectory(f, parentFolder + "/" + f.getName(), zos);
                continue;
            }
            try (FileInputStream in = new FileInputStream(f)) {
                zos.putNextEntry(new ZipEntry(parentFolder + "/" + f.getName()));
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }
}
