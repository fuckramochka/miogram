package app.miogram.bridge.plugins;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Build pipeline for Forge scaffolds.
 *
 * <p>Honest contract: Android ships no Rust toolchain, so on-device builds
 * only run where one exists (rooted/dev devices with cargo on PATH, or a
 * connected build host via adb — out of scope). Otherwise the pipeline
 * reports exactly what to run on desktop and where the .wasm must land.
 * Detection is a quick `command -v cargo` probe with a hard timeout; it
 * never blocks the UI thread.
 */
public final class MioForgeBuilder {

    public interface BuildCallback {
        /** Always invoked on a background thread; post to UI yourself. */
        void onResult(boolean success, String log);
    }

    private MioForgeBuilder() {}

    public static void probeAndBuild(File projectDir, BuildCallback callback) {
        probeAndBuild(projectDir, "rust", callback);
    }

    public static void probeAndBuild(File projectDir, String language, BuildCallback callback) {
        final boolean go = "go".equalsIgnoreCase(language);
        Utilities.globalQueue.postRunnable(() -> {
            StringBuilder log = new StringBuilder();
            if (go) {
                String tinygo = findBinary(log, "tinygo");
                if (tinygo == null) {
                    log.append("STATUS: TOOLCHAIN_MISSING\n");
                    log.append("No TinyGo toolchain on this device, so plugin.wasm cannot be produced here.\n");
                    log.append(MioForgeScaffold.buildInstructions(projectDir.getName(), "go"));
                    callback.onResult(false, log.toString());
                    return;
                }
                log.append("STATUS: TOOLCHAIN_FOUND (").append(tinygo).append(")\n");
                int code = run(log, projectDir, 300_000,
                        tinygo, "build", "-o", "plugin.wasm", "-target", "wasm", ".");
                File wasm = new File(projectDir, "plugin.wasm");
                if (code == 0 && wasm.isFile()) {
                    log.append("STATUS: BUILD_OK -> ").append(wasm.getAbsolutePath()).append("\n");
                    callback.onResult(true, log.toString());
                } else {
                    log.append("STATUS: BUILD_FAILED (exit=").append(code).append(")\n");
                    callback.onResult(false, log.toString());
                }
                return;
            }
            String cargo = findCargo(log);
            if (cargo == null) {
                log.append("STATUS: TOOLCHAIN_MISSING\n");
                log.append("No Rust toolchain on this device, so the .wasm cannot be produced here.\n");
                log.append(MioForgeScaffold.buildInstructions(projectDir.getName(), "rust"));
                callback.onResult(false, log.toString());
                return;
            }
            log.append("STATUS: TOOLCHAIN_FOUND (").append(cargo).append(")\n");
            int code = run(log, projectDir, 300_000,
                    cargo, "build", "--release", "--target", "wasm32-unknown-unknown");
            File wasm = new File(projectDir, "target/wasm32-unknown-unknown/release/" + projectDir.getName() + ".wasm");
            if (code == 0 && wasm.isFile()) {
                log.append("STATUS: BUILD_OK -> ").append(wasm.getAbsolutePath()).append("\n");
                callback.onResult(true, log.toString());
            } else {
                log.append("STATUS: BUILD_FAILED (exit=").append(code).append(")\n");
                callback.onResult(false, log.toString());
            }
        });
    }

    private static String findBinary(StringBuilder log, String name) {
        try {
            Process p = new ProcessBuilder("sh", "-c", "command -v " + name + " 2>/dev/null")
                    .redirectErrorStream(true).start();
            boolean done = p.waitFor(5, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            r.close();
            if (p.exitValue() == 0 && line != null && !line.trim().isEmpty()) return line.trim();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        log.append("probe: no ").append(name).append(" on PATH\n");
        return null;
    }

    private static String findCargo(StringBuilder log) {
        String[] probes = {
                "command -v cargo",
                "ls /data/local/tmp/cargo/bin/cargo",
                "ls /system/bin/cargo",
        };
        for (String probe : probes) {
            try {
                Process p = new ProcessBuilder("sh", "-c", probe + " 2>/dev/null")
                        .redirectErrorStream(true).start();
                boolean done = p.waitFor(5, TimeUnit.SECONDS);
                if (!done) {
                    p.destroyForcibly();
                    continue;
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = r.readLine();
                r.close();
                if (p.exitValue() == 0 && line != null && !line.trim().isEmpty()) {
                    String t = line.trim();
                    if (t.endsWith("/cargo") || t.equals("cargo")) return t;
                    if (probe.startsWith("ls")) return t;
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        log.append("probe: no cargo on PATH\n");
        return null;
    }

    private static int run(StringBuilder log, File dir, long timeoutMs, String... cmd) {
        Process process = null;
        try {
            process = new ProcessBuilder(cmd)
                    .directory(dir)
                    .redirectErrorStream(true)
                    .start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder tail = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                tail.append(buf, 0, n);
                if (tail.length() > 8000) tail.delete(0, tail.length() - 8000);
            }
            boolean done = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            log.append(tail);
            if (!done) {
                process.destroyForcibly();
                log.append("\n(build timed out)\n");
                return -1;
            }
            return process.exitValue();
        } catch (Throwable t) {
            FileLog.e(t);
            log.append("\n(launch failed: ").append(t.getMessage()).append(")\n");
            return -1;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
