package app.miogram.bridge.player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import app.miogram.bridge.MiogramLocale;
import app.miogram.bridge.customui.MiogramHaptic;

/**
 * Production picker: Custom Photo / Video -> internal copy -> Preview -> Apply.
 * Handles cancel / invalid / permission / deleted file gracefully.
 */
public class MiogramPlayerBackdropPicker extends BaseFragment {

    public static final String ARG_MODE = "mode"; // photo | video

    private static final int REQ_PICK = 1407;

    private String mode = "photo";
    private ImageView previewImage;
    private android.widget.VideoView previewVideo;
    private TextView statusView;
    private TextView applyBtn;
    private String stagedPath = "";
    private boolean stagedValid = false;

    @Override
    public boolean onFragmentCreate() {
        Bundle args = getArguments();
        if (args != null) {
            mode = args.getString(ARG_MODE, "photo");
        }
        if (!mode.equals("video")) mode = "photo";
        String cur = mode.equals("video") ? MiogramPlayerPrefs.getCustomVideoPath() : MiogramPlayerPrefs.getCustomPhotoPath();
        if (MiogramPlayerPrefs.isCustomMediaValid(cur)) {
            stagedPath = cur;
            stagedValid = true;
        }
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(MiogramLocale.get(
                mode.equals("video") ? "Фон-відео" : "Фон-фото",
                mode.equals("video") ? "Фон-видео" : "Фон-фото",
                mode.equals("video") ? "Background video" : "Background photo"));
        actionBar.setActionBarMenuOnItemClick(id -> {
            if (id == -1) finishFragment();
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scroll = new ScrollView(context);
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(col, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView hint = new TextView(context);
        hint.setText(MiogramLocale.get(
                "Обери файл → одразу бачиш превʼю → Застосувати. Копія зберігається всередині Miogram, переживає перезапуск.",
                "Выбери файл → сразу видишь превью → Применить. Копия хранится внутри Miogram и переживает перезапуск.",
                "Pick a file → instant preview → Apply. A copy is stored inside Miogram and survives restart."));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        col.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        FrameLayout previewBox = new FrameLayout(context);
        previewBox.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        col.addView(previewBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 220, 0, 0, 0, 12));

        previewImage = new ImageView(context);
        previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewBox.addView(previewImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        previewVideo = new android.widget.VideoView(context);
        previewVideo.setVisibility(View.GONE);
        previewBox.addView(previewVideo, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        statusView = new TextView(context);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setGravity(Gravity.CENTER);
        col.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView pickBtn = makeButton(context, MiogramLocale.get("Обрати файл…", "Выбрать файл…", "Pick file…"), false);
        pickBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            openPicker();
        });
        col.addView(pickBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0, 0, 0, 8));

        TextView clearBtn = makeButton(context, MiogramLocale.get("Прибрати кастомний фон", "Убрать кастомный фон", "Remove custom background"), true);
        clearBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            stagedPath = "";
            stagedValid = false;
            renderPreview();
        });
        col.addView(clearBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 0, 0, 0, 8));

        applyBtn = makeButton(context, MiogramLocale.get("Застосувати ✨", "Применить ✨", "Apply ✨"), false);
        applyBtn.setOnClickListener(v -> {
            MiogramHaptic.tap(v);
            if (stagedValid && MiogramPlayerPrefs.isCustomMediaValid(stagedPath)) {
                if (mode.equals("video")) {
                    MiogramPlayerPrefs.setCustomVideoPath(stagedPath);
                    MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO);
                } else {
                    MiogramPlayerPrefs.setCustomPhotoPath(stagedPath);
                    MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO);
                }
                finishFragment();
            } else if (stagedPath.isEmpty()) {
                if (mode.equals("video")) MiogramPlayerPrefs.setCustomVideoPath("");
                else MiogramPlayerPrefs.setCustomPhotoPath("");
                if (MiogramPlayerPrefs.getBackgroundMode() == MiogramPlayerPrefs.BG_MODE_CUSTOM_PHOTO
                        || MiogramPlayerPrefs.getBackgroundMode() == MiogramPlayerPrefs.BG_MODE_CUSTOM_VIDEO) {
                    MiogramPlayerPrefs.setBackgroundMode(MiogramPlayerPrefs.BG_MODE_COVER_BLUR);
                }
                finishFragment();
            } else {
                setStatus(MiogramLocale.get("Файл недоступний — обери інший.", "Файл недоступен — выбери другой.", "File unavailable — pick another."), true);
            }
        });
        col.addView(applyBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 8, 0, 0));

        fragmentView = root;
        renderPreview();
        return root;
    }

    private TextView makeButton(Context context, String text, boolean danger) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(danger ? 0xFFE55757 : 0xFFFFFFFF);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(14));
        gd.setColor(danger ? 0x1AE55757 : Theme.getColor(Theme.key_featuredStickers_addButton));
        tv.setBackground(gd);
        return tv;
    }

    private void setStatus(String s, boolean isError) {
        if (statusView != null) {
            statusView.setText(s);
            statusView.setTextColor(isError ? 0xFFE55757 : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        }
    }

    private void renderPreview() {
        try {
            if (previewVideo != null) {
                try {
                    previewVideo.stopPlayback();
                } catch (Throwable ignore) {}
                previewVideo.setVisibility(View.GONE);
            }
            if (stagedValid && MiogramPlayerPrefs.isCustomMediaValid(stagedPath)) {
                if (mode.equals("video")) {
                    previewImage.setImageDrawable(null);
                    previewVideo.setVideoPath(stagedPath);
                    previewVideo.setOnPreparedListener(mp -> {
                        try {
                            mp.setLooping(true);
                            mp.setVolume(0f, 0f);
                            previewVideo.start();
                        } catch (Throwable ignore) {}
                    });
                    previewVideo.setOnErrorListener((mp, what, extra) -> true);
                    previewVideo.setVisibility(View.VISIBLE);
                    setStatus(MiogramLocale.get("Превʼю відео (без звуку, loop).", "Превью видео (без звука, loop).", "Video preview (muted, loop)."), false);
                } else {
                    Bitmap bmp = BitmapFactory.decodeFile(stagedPath);
                    if (bmp != null) {
                        previewImage.setImageBitmap(bmp);
                        setStatus(MiogramLocale.get("Превʼю готове — натисни Застосувати.", "Превью готово — нажми Применить.", "Preview ready — tap Apply."), false);
                    } else {
                        stagedValid = false;
                        setStatus(MiogramLocale.get("Не вдалося прочитати зображення.", "Не удалось прочитать изображение.", "Could not decode image."), true);
                    }
                }
            } else {
                previewImage.setImageDrawable(null);
                if (stagedPath.isEmpty()) {
                    setStatus(MiogramLocale.get("Кастомний фон не обрано — буде стандартний blur.", "Кастомный фон не выбран — будет стандартный blur.", "No custom background — default blur will be used."), false);
                } else {
                    setStatus(MiogramLocale.get("Файл видалено або недоступний.", "Файл удалён или недоступен.", "File deleted or unavailable."), true);
                }
            }
            updateApply();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void updateApply() {
        if (applyBtn != null) {
            applyBtn.setAlpha(1f);
        }
    }

    private void openPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(mode.equals("video") ? "video/*" : "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQ_PICK);
        } catch (Throwable t) {
            setStatus(MiogramLocale.get("Немає застосунку для вибору файлів.", "Нет приложения для выбора файлов.", "No file picker available."), true);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_PICK) {
            if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
                setStatus(MiogramLocale.get("Вибір скасовано — лишено попередній фон.", "Выбор отменён — оставлен предыдущий фон.", "Pick cancelled — previous background kept."), false);
                return;
            }
            Uri uri = data.getData();
            try {
                Context ctx = getParentActivity();
                if (ctx == null) ctx = AndroidUtilities.applicationContext;
                try {
                    ctx.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Throwable ignore) {}
                File out = new File(ctx.getFilesDir(), mode.equals("video") ? "miogram_player_bg_video.mp4" : "miogram_player_bg_photo.jpg");
                try (InputStream in = ctx.getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(out)) {
                    if (in == null) throw new Exception("null stream");
                    byte[] buf = new byte[8192];
                    long total = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        total += n;
                        if (total > 80L * 1024 * 1024) throw new Exception("too large (>80MB)");
                        fos.write(buf, 0, n);
                    }
                    fos.flush();
                }
                if (!MiogramPlayerPrefs.isCustomMediaValid(out.getAbsolutePath())) {
                    throw new Exception("invalid file");
                }
                if (mode.equals("photo")) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(out.getAbsolutePath(), o);
                    if (o.outWidth <= 0 || o.outHeight <= 0) throw new Exception("bad image");
                }
                stagedPath = out.getAbsolutePath();
                stagedValid = true;
                renderPreview();
            } catch (Throwable t) {
                stagedValid = false;
                setStatus(MiogramLocale.get("Невірний файл — спробуй інший.", "Неверный файл — попробуй другой.", "Invalid file — try another."), true);
                FileLog.e(t);
            }
        } else {
            super.onActivityResultFragment(requestCode, resultCode, data);
        }
    }

    @Override
    public void onFragmentDestroy() {
        try {
            if (previewVideo != null) previewVideo.stopPlayback();
        } catch (Throwable ignore) {}
        super.onFragmentDestroy();
    }
}
