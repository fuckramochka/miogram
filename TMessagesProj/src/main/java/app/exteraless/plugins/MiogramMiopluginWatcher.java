package app.exteraless.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Watches a user-granted folder ("mioplugin") and auto-imports dropped
 * plugin files into the catalog after every app start (and on demand).
 *
 * <p>Why a SAF tree and not a hardcoded path: the internal plugins dir
 * ({@code filesDir/plugins}) is unreachable for the user without root, and
 * public storage needs a user grant on modern Android. The user picks the
 * folder once ({@code ACTION_OPEN_DOCUMENT_TREE}), we persist the permission
 * and rescan on startup. Routing reuses {@link PyModuleRouter}: Hikka-style
 * {@code .py} goes to the userbot, everything else goes through the normal
 * exteraGram install (no consent sheet in background — the folder grant
 * itself is the explicit trust action, same as "install from file").
 */
public final class MiogramMiopluginWatcher {

    static {
        try {
            app.miogram.bridge.ai.tools.MioTool.register(new app.miogram.bridge.ai.tools.MioTool.Def(
                    "scan_mioplugins",
                    "Scan Mioplugin Folder",
                    "Scans candidate mioplugin folders and SAF tree for plugins and imports them into catalog.",
                    false,
                    (account, params, cb) -> {
                        scanAndImport(org.telegram.messenger.ApplicationLoader.applicationContext, (imported, names, error) -> {
                            if (cb != null) {
                                if (error != null) {
                                    cb.run("Scan error: " + error);
                                } else {
                                    cb.run("Scanned mioplugin: imported " + imported + " plugins: " + names);
                                }
                            }
                        });
                    }
            ));
        } catch (Throwable ignore) {}
    }

    private MiogramMiopluginWatcher() {
    }

    private static final String PREFS_NAME = "mioplugin_watcher_prefs";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String KEY_IMPORTED = "imported_keys";

    private static final String[] SCAN_EXT = {".py", ".wasm", ".so", ".mioplugin", ".plugin", ".elyx", ".eaf"};

    public interface ScanCallback {
        void onDone(int imported, List<String> names, String error);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Intent createPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return intent;
    }

    public static boolean hasFolder(Context context) {
        String uri = prefs(context).getString(KEY_TREE_URI, null);
        return uri != null && !uri.isEmpty();
    }

    /** Short human label for the granted folder, e.g. "Miogram/mioplugin". */
    public static String getFolderLabel(Context context) {
        String uri = prefs(context).getString(KEY_TREE_URI, null);
        if (uri == null || uri.isEmpty()) return null;
        try {
            String treeId = DocumentsContract.getTreeDocumentId(Uri.parse(uri));
            int colon = treeId.indexOf(':');
            String path = colon >= 0 ? treeId.substring(colon + 1) : treeId;
            if (path.isEmpty()) return treeId;
            int slash = path.lastIndexOf('/');
            String leaf = slash >= 0 ? path.substring(slash + 1) : path;
            String parent = slash > 0 ? path.substring(0, slash) : "";
            int pslash = parent.lastIndexOf('/');
            if (pslash >= 0) parent = parent.substring(pslash + 1);
            return parent.isEmpty() ? leaf : (parent + "/" + leaf);
        } catch (Throwable t) {
            return uri;
        }
    }

    public static void saveFolderUri(Context context, Uri treeUri) {
        try {
            context.getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable t) {
            FileLog.e("mioplugin: persist permission failed", t);
        }
        prefs(context).edit().putString(KEY_TREE_URI, treeUri.toString()).apply();
    }

    public static void clearFolder(Context context) {
        prefs(context).edit().remove(KEY_TREE_URI).apply();
    }

    private static Set<String> loadImported(Context context) {
        try {
            return new HashSet<>(prefs(context).getStringSet(KEY_IMPORTED, new HashSet<>()));
        } catch (Throwable t) {
            return new HashSet<>();
        }
    }

    private static void saveImported(Context context, Set<String> keys) {
        try {
            prefs(context).edit().putStringSet(KEY_IMPORTED, new HashSet<>(keys)).apply();
        } catch (Throwable ignore) {}
    }

    private static boolean isPluginFile(String name) {
        if (name == null) return false;
        String low = name.toLowerCase(Locale.ROOT);
        for (String ext : SCAN_EXT) {
            if (low.endsWith(ext)) return true;
        }
        return false;
    }

    private static class ChildDoc {
        String name;
        Uri uri;
        long size;
        long modified;
    }

    private static List<ChildDoc> listChildren(Context context, Uri treeUri) {
        List<ChildDoc> out = new ArrayList<>();
        try {
            String treeId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId);
            String[] projection = {
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
            };
            try (Cursor c = context.getContentResolver().query(childrenUri, projection, null, null, null)) {
                if (c == null) return out;
                int nameIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int modIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                int sizeIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
                int idIdx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                while (c.moveToNext()) {
                    ChildDoc d = new ChildDoc();
                    d.name = nameIdx >= 0 ? c.getString(nameIdx) : null;
                    if (!isPluginFile(d.name)) continue;
                    d.modified = modIdx >= 0 ? c.getLong(modIdx) : 0;
                    d.size = sizeIdx >= 0 ? c.getLong(sizeIdx) : 0;
                    String docId = idIdx >= 0 ? c.getString(idIdx) : null;
                    if (docId == null) continue;
                    d.uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                    out.add(d);
                }
            }
        } catch (Throwable t) {
            FileLog.e("mioplugin: list failed", t);
        }
        return out;
    }

    private static File copyToCache(Context context, ChildDoc doc) throws Exception {
        File tmp = new File(context.getCacheDir(), "mioplugin_" + System.currentTimeMillis() + "_" + doc.name);
        try (InputStream in = context.getContentResolver().openInputStream(doc.uri);
             FileOutputStream out = new FileOutputStream(tmp)) {
            if (in == null) throw new IllegalStateException("cannot open " + doc.name);
            byte[] buf = new byte[65536];
            int len;
            long total = 0;
            while ((len = in.read(buf)) > 0) {
                total += len;
                if (total > 64L * 1024 * 1024) throw new IllegalStateException("file too large: " + doc.name);
                out.write(buf, 0, len);
            }
            out.flush();
        }
        return tmp;
    }

    /**
     * Scans the granted folder and imports new/changed files. Runs on its own
     * worker thread (install may block waiting for engines); callback on UI.
     */
    public static void scanAndImport(final Context appContext, final ScanCallback callback) {
        final Context ctx = appContext.getApplicationContext();
        Thread worker = new Thread(() -> {
            final List<String> importedNames = new ArrayList<>();
            String error = null;
            try {
                Set<String> known = loadImported(ctx);
                Set<String> seen = new HashSet<>();

                // 1. Scan candidate filesystem directories (direct file drops)
                List<File> candidateDirs = getCandidateDirs(ctx);
                for (File dir : candidateDirs) {
                    if (dir == null || !dir.exists() || !dir.isDirectory()) continue;
                    File[] files = dir.listFiles();
                    if (files == null) continue;
                    for (File f : files) {
                        if (!f.isFile() || !isPluginFile(f.getName())) continue;
                        String key = "fs:" + f.getName() + "|" + f.length() + "|" + f.lastModified();
                        seen.add(f.getName());
                        if (known.contains(key)) continue;
                        boolean handled = importLocalFile(f);
                        if (handled) {
                            known.add(key);
                            importedNames.add(f.getName());
                        }
                    }
                }

                // 2. Scan SAF tree URI if granted
                String uriStr = prefs(ctx).getString(KEY_TREE_URI, null);
                if (uriStr != null && !uriStr.isEmpty()) {
                    try {
                        Uri treeUri = Uri.parse(uriStr);
                        List<ChildDoc> children = listChildren(ctx, treeUri);
                        for (ChildDoc doc : children) {
                            String key = "saf:" + doc.name + "|" + doc.size + "|" + doc.modified;
                            seen.add(doc.name);
                            if (known.contains(key)) continue;
                            File tmp = null;
                            try {
                                tmp = copyToCache(ctx, doc);
                                boolean handled = importLocalFile(tmp);
                                if (handled) {
                                    known.add(key);
                                    importedNames.add(doc.name);
                                }
                            } catch (Throwable t) {
                                FileLog.e("mioplugin: saf import failed for " + doc.name, t);
                            } finally {
                                if (tmp != null) {
                                    try { tmp.delete(); } catch (Throwable ignore) {}
                                }
                            }
                        }
                    } catch (Throwable t) {
                        FileLog.e("mioplugin: saf scan failed", t);
                    }
                }

                saveImported(ctx, known);
                if (!importedNames.isEmpty()) {
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            PluginsController.getInstance().rescanAndLoadEnabled();
                        } catch (Throwable ignore) {}
                    });
                }
            } catch (Throwable t) {
                FileLog.e("mioplugin: scan failed", t);
                error = t.getMessage();
            }
            final String fErr = error;
            postDone(callback, importedNames.size(), importedNames, fErr);
        }, "mioplugin-scan");
        worker.setDaemon(true);
        worker.start();
    }

    public static List<File> getCandidateDirs(Context context) {
        List<File> dirs = new ArrayList<>();
        try {
            dirs.add(new File(android.os.Environment.getExternalStorageDirectory(), "mioplugin"));
        } catch (Throwable ignore) {}
        try {
            dirs.add(new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "mioplugin"));
        } catch (Throwable ignore) {}
        try {
            dirs.add(new File(android.os.Environment.getExternalStorageDirectory(), "Telegram/mioplugin"));
        } catch (Throwable ignore) {}
        try {
            if (context.getExternalFilesDir(null) != null) {
                dirs.add(new File(context.getExternalFilesDir(null), "mioplugin"));
            }
        } catch (Throwable ignore) {}
        try {
            dirs.add(new File(context.getFilesDir(), "mioplugin"));
        } catch (Throwable ignore) {}
        try {
            dirs.add(new File(AndroidUtilities.getSharingDirectory(), "mioplugin"));
        } catch (Throwable ignore) {}
        return dirs;
    }

    private static boolean importLocalFile(File file) {
        if (file == null || !file.exists()) return false;
        String low = file.getName().toLowerCase(Locale.ROOT);
        boolean handled = false;
        if (low.endsWith(".py")) {
            PyModuleRouter.Kind kind = PyModuleRouter.detectKind(file);
            if (kind == PyModuleRouter.Kind.HEROKU) {
                Activity act = topActivity();
                if (act != null) {
                    handled = PyModuleRouter.routeToHerokuIfNeeded(act, file);
                } else {
                    handled = installHerokuHeadless(file);
                }
            }
        }
        if (!handled) {
            handled = installExteraHeadless(file);
        }
        return handled;
    }

    private static Activity topActivity() {
        try {
            org.telegram.ui.ActionBar.BaseFragment f = org.telegram.ui.LaunchActivity.getSafeLastFragment();
            if (f != null) return f.getParentActivity();
        } catch (Throwable ignore) {}
        return null;
    }

    private static boolean installHerokuHeadless(File tmp) {
        try {
            return app.miogram.bridge.userbot.MiogramHerokuManager.getInstance().installModule(tmp);
        } catch (Throwable t) {
            FileLog.e("mioplugin: heroku install failed", t);
            return false;
        }
    }

    private static boolean installExteraHeadless(File tmp) {
        final boolean[] done = {false};
        final boolean[] ok = {false};
        try {
            PluginsController.getInstance().installPlugin(tmp, (success, err, plugin) -> {
                ok[0] = success;
                done[0] = true;
            });
            long deadline = System.currentTimeMillis() + 60000;
            while (!done[0] && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Throwable t) {
            FileLog.e("mioplugin: extera install failed", t);
        }
        return ok[0];
    }

    private static void postDone(ScanCallback callback, int count, List<String> names, String error) {
        if (callback == null) return;
        AndroidUtilities.runOnUIThread(() -> {
            try {
                callback.onDone(count, names, error);
            } catch (Throwable ignore) {}
        });
    }
}
