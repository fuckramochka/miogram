package app.miogram.bridge.github;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import app.miogram.bridge.MiogramLocale;

/**
 * Public GitHub Actions & Repository Bridge for Miogram.
 * Safe, keyless REST monitor for GitHub Actions workflow runs, commits, and releases.
 */
public class MiogramGitHubManager {

    private static volatile MiogramGitHubManager instance;

    public static MiogramGitHubManager getInstance() {
        if (instance == null) {
            synchronized (MiogramGitHubManager.class) {
                if (instance == null) {
                    instance = new MiogramGitHubManager();
                }
            }
        }
        return instance;
    }

    public static class WorkflowRun {
        public String repo = "fuckramochka/miogram";
        public String workflowName = "";
        public String status = "";      // "completed", "in_progress", "queued"
        public String conclusion = "";  // "success", "failure", "cancelled"
        public String commitMessage = "";
        public String commitSha = "";
        public String branch = "";
        public String htmlUrl = "";
        public long runId = 0;
        public long lastUpdated = 0;

        public boolean isSuccess() {
            return "completed".equals(status) && "success".equals(conclusion);
        }

        public boolean isRunning() {
            return "in_progress".equals(status) || "queued".equals(status);
        }

        public boolean isFailed() {
            return "completed".equals(status) && "failure".equals(conclusion);
        }

        public String getStatusDisplay() {
            if (isRunning()) return "Building...";
            if (isSuccess()) return "Success";
            if (isFailed()) return "Failed";
            if ("cancelled".equals(conclusion)) return "Cancelled";
            return "Idle";
        }
    }

    public interface WorkflowCallback {
        void onWorkflowLoaded(WorkflowRun run);
    }

    private static final String PREFS_NAME = "miogram_github_prefs";
    private static final String KEY_REPO = "github_repo";
    private static final String DEFAULT_REPO = "fuckramochka/miogram";

    private WorkflowRun cachedRun;
    private long lastFetchTime = 0;

    private MiogramGitHubManager() {
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getTrackedRepo() {
        return getPrefs().getString(KEY_REPO, DEFAULT_REPO);
    }

    public void setTrackedRepo(String repo) {
        if (TextUtils.isEmpty(repo)) repo = DEFAULT_REPO;
        getPrefs().edit().putString(KEY_REPO, repo.trim()).apply();
        cachedRun = null;
        lastFetchTime = 0;
    }

    /**
     * Fetches latest GitHub Actions workflow run for the configured repository.
     */
    public void fetchLatestWorkflow(boolean force, WorkflowCallback callback) {
        long now = SystemClock.elapsedRealtime();
        if (!force && cachedRun != null && (now - lastFetchTime < 30000)) {
            if (callback != null) callback.onWorkflowLoaded(cachedRun);
            return;
        }

        final String repo = getTrackedRepo();
        final String urlStr = "https://api.github.com/repos/" + repo + "/actions/runs?per_page=1";

        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(urlStr);
                conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("User-Agent", "Miogram-App");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder resp = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resp.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(resp.toString());
                    JSONArray runs = json.optJSONArray("workflow_runs");
                    if (runs != null && runs.length() > 0) {
                        JSONObject r = runs.getJSONObject(0);
                        WorkflowRun wr = new WorkflowRun();
                        wr.repo = repo;
                        wr.runId = r.optLong("id", 0);
                        wr.workflowName = r.optString("name", "Release Build");
                        wr.status = r.optString("status", "");
                        wr.conclusion = r.optString("conclusion", "");
                        wr.branch = r.optString("head_branch", "main");
                        wr.htmlUrl = r.optString("html_url", "");

                        JSONObject commit = r.optJSONObject("head_commit");
                        if (commit != null) {
                            String msg = commit.optString("message", "");
                            if (!TextUtils.isEmpty(msg)) {
                                wr.commitMessage = msg.split("\n")[0];
                            }
                            String sha = commit.optString("id", "");
                            if (sha.length() > 7) {
                                wr.commitSha = sha.substring(0, 7);
                            } else {
                                wr.commitSha = sha;
                            }
                        }
                        wr.lastUpdated = System.currentTimeMillis();

                        cachedRun = wr;
                        lastFetchTime = SystemClock.elapsedRealtime();

                        AndroidUtilities.runOnUIThread(() -> {
                            if (callback != null) callback.onWorkflowLoaded(wr);
                        });
                        return;
                    }
                }
            } catch (Throwable t) {
                FileLog.e("MiogramGitHubManager: fetch error", t);
            } finally {
                if (conn != null) conn.disconnect();
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) callback.onWorkflowLoaded(cachedRun);
            });
        });
    }

    public void openRepo(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + getTrackedRepo()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignore) {}
    }

    public void openWorkflowRuns(Context context) {
        if (context == null) return;
        try {
            String url = cachedRun != null && !TextUtils.isEmpty(cachedRun.htmlUrl)
                    ? cachedRun.htmlUrl
                    : "https://github.com/" + getTrackedRepo() + "/actions";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignore) {}
    }

    public void showSelectRepoDialog(Context context, WorkflowCallback callback) {
        if (context == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
        builder.setTitle(MiogramLocale.get("Вибір репозиторію GitHub", "Выбор репозитория GitHub", "Select GitHub Repository"));
        builder.setMessage(MiogramLocale.get(
                "Введіть репозиторій у форматі owner/repo (наприклад, fuckramochka/miogram):",
                "Введите репозиторий в формате owner/repo (например, fuckramochka/miogram):",
                "Enter repository as owner/repo (e.g. fuckramochka/miogram):"
        ));
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setSingleLine(true);
        input.setText(getTrackedRepo());
        builder.setView(input);
        builder.setPositiveButton(MiogramLocale.get("Зберегти", "Сохранить", "Save"), (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!TextUtils.isEmpty(val)) {
                setTrackedRepo(val);
                fetchLatestWorkflow(true, callback);
            }
        });
        builder.setNegativeButton(MiogramLocale.get("Скасувати", "Отмена", "Cancel"), null);
        builder.show();
    }
}
