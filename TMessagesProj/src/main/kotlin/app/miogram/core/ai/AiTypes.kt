package app.miogram.core.ai

/**
 * Agentic tasks Miogram can execute. Each task carries its default
 * execution mode — the product-level answer to "local or cloud?".
 *
 * Policy anchors (user-facing defaults):
 *  * speech-to-text stays ON-DEVICE by default — audio never leaves the
 *    phone unless the user explicitly opts into a cloud transcription;
 *  * heavy text work (summaries, action extraction) prefers cloud Flash Lite
 *    when a key is configured, degrading to nothing rather than to a weak
 *    local model silently;
 *  * semantic indexing is always local: it processes every message and would
 *    leak the whole history otherwise.
 */
enum class AiTask(val defaultMode: ExecutionMode) {
    TRANSCRIBE_AUDIO(ExecutionMode.LOCAL_FIRST),
    SEMANTIC_INDEX(ExecutionMode.LOCAL_ONLY),
    SUMMARIZE_THREAD(ExecutionMode.CLOUD_FIRST),
    EXTRACT_ACTIONS(ExecutionMode.CLOUD_FIRST),
    SMART_REPLIES(ExecutionMode.CLOUD_FIRST);
}

enum class ExecutionMode {
    /** Never leaves the device; feature is off if the local model is absent. */
    LOCAL_ONLY,

    /** Local model wins whenever present; cloud only as explicit fallback. */
    LOCAL_FIRST,

    /** Cloud preferred; local used when offline / no key configured. */
    CLOUD_FIRST,

    /** Always cloud; hard-fails without a configured key. */
    CLOUD_ONLY,

    DISABLED,
}

/** User-adjustable overrides per task; null = use [AiTask.defaultMode]. */
data class AiPreferences(
    val overrides: Map<AiTask, ExecutionMode> = emptyMap(),
    /** Guard against surprise traffic on cellular connections. */
    val cloudAllowedOnMeteredNetwork: Boolean = false,
) {
    fun modeFor(task: AiTask): ExecutionMode = overrides[task] ?: task.defaultMode
}

/** What the environment can currently offer to the router. */
data class AiEnvironment(
    val localModelReady: Boolean,
    val cloudKeyConfigured: Boolean,
    val networkOnline: Boolean,
    val networkMetered: Boolean,
)

sealed class RouteDecision {
    /** Execute with the on-device runtime. */
    data object UseLocal : RouteDecision()

    /** Execute via the configured cloud gateway. */
    data class UseCloud(val reason: String) : RouteDecision()

    /** Nothing runnable right now; [reason] is safe to show in UI. */
    data class Unavailable(val reason: String) : RouteDecision()
}

object AiRouter {

    /**
     * Pure decision function — no I/O, fully deterministic given inputs.
     *
     * Resolution order per task:
     *  1. mode == DISABLED -> unavailable;
     *  2. LOCAL_ONLY       -> local if ready else unavailable;
     *  3. CLOUD_ONLY       -> cloud if key+network (+metering ok) else unavailable;
     *  4. LOCAL_FIRST      -> local when ready; cloud only if allowed AND usable;
     *                         otherwise whichever single side is usable, else out;
     *  5. CLOUD_FIRST      -> cloud when usable (metering respected); local as
     *                         fallback only for tasks that have a local path
     *                         (transcription), else unavailable.
     */
    fun route(task: AiTask, prefs: AiPreferences, env: AiEnvironment): RouteDecision {
        return when (prefs.modeFor(task)) {
            ExecutionMode.DISABLED ->
                RouteDecision.Unavailable("disabled by user")

            ExecutionMode.LOCAL_ONLY ->
                if (env.localModelReady) RouteDecision.UseLocal
                else RouteDecision.Unavailable("on-device model not downloaded")

            ExecutionMode.CLOUD_ONLY ->
                if (cloudUsable(prefs, env)) RouteDecision.UseCloud("mode=cloud-only")
                else unavailableCloud(env)

            ExecutionMode.LOCAL_FIRST -> when {
                env.localModelReady -> RouteDecision.UseLocal
                cloudUsable(prefs, env) -> RouteDecision.UseCloud("local model missing")
                else -> RouteDecision.Unavailable(
                    if (!env.cloudKeyConfigured) "no on-device model and no cloud key"
                    else "models unavailable"
                )
            }

            ExecutionMode.CLOUD_FIRST -> when {
                cloudUsable(prefs, env) -> RouteDecision.UseCloud("mode=cloud-first")
                task == AiTask.TRANSCRIBE_AUDIO && env.localModelReady -> RouteDecision.UseLocal
                else -> unavailableCloud(env)
            }
        }
    }

    private fun cloudUsable(prefs: AiPreferences, env: AiEnvironment): Boolean =
        env.cloudKeyConfigured && env.networkOnline &&
                (!env.networkMetered || prefs.cloudAllowedOnMeteredNetwork)

    private fun unavailableCloud(env: AiEnvironment): RouteDecision.Unavailable =
        RouteDecision.Unavailable(
            when {
                !env.cloudKeyConfigured -> "cloud key not configured"
                !env.networkOnline -> "offline"
                else -> "cloud disabled on metered network"
            }
        )
}
