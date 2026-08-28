package app.miogram.bridge.ui;

/**
 * Compatibility entry into Miogram's plugin manager.
 *
 * The previous standalone implementation targeted a WASM engine
 * (app.miogram.core.plugins.*, WamrWasmRuntime) that was never shipped in
 * this tree, so the file could not compile and the screen was unreachable.
 * The real, working manager — Chaquopy/Python based, with permissions,
 * audit journal and install sheets — lives in
 * {@code app.exteraless.plugins.ui.PluginsActivity}; this alias keeps the
 * Miogram entry point stable.
 */
public class MiogramPluginsActivity extends app.exteraless.plugins.ui.PluginsActivity {
}
