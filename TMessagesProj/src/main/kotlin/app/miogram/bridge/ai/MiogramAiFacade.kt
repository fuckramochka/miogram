package app.miogram.bridge.ai

import android.content.Context
import app.miogram.bridge.passcode.MiogramLockFacade
import app.miogram.core.ai.AiEnvironment
import app.miogram.core.ai.AiPreferences
import app.miogram.core.ai.AiRouter
import app.miogram.core.ai.AiTask
import app.miogram.core.ai.ExecutionMode
import app.miogram.core.ai.RouteDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Facade binding the routing core to concrete providers.
 *
 * Key sourcing precedence for cloud tasks:
 *  1. Miogram vault namespace `ai.gemini.key` (sealed under the REAL profile;
 *     unavailable in decoy sessions and while locked) — the private mode;
 *  2. host-provided keyring (existing AI Studio keys from the translator
 *     settings), so users who already configured one or more keys keep working.
 *
 * All execution happens on Dispatchers.IO; nothing here may run on main.
 */
class MiogramAiFacade(
    private val context: Context,
    val sttEngine: LocalSttEngine,
    private val gemini: GeminiCloudClient = GeminiCloudClient(),
    /** Supplied by bridge wiring; existing keys from the host app settings. */
    private val hostKeysSupplier: () -> List<String> = { emptyList() },
    /** Uses the same selected Gemini model as the Java-facing Miogram AI UI. */
    private val cloudModelSupplier: () -> String = { GeminiCloudClient.DEFAULT_MODEL },
) {

    @Volatile
    var preferences: AiPreferences = AiPreferences()
        set(value) {
            field = value
            persistPreferences(value)
        }

    init {
        preferences = loadPreferences()
    }

    suspend fun environmentSnapshot(): AiEnvironment {
        val online = connectivityOnline()
        return AiEnvironment(
            localModelReady = sttEngine.isDownloaded(),
            cloudKeyConfigured = resolveCloudKeys().isNotEmpty(),
            networkOnline = online,
            networkMetered = online && isMetered(),
        )
    }

    suspend fun route(task: AiTask): RouteDecision =
        AiRouter.route(task, preferences, environmentSnapshot())

    /**
     * Executes [task] over [input] following current routing. Returns the
     * provider decision alongside the payload so UI can show "local/cloud".
     */
    suspend fun run(
        task: AiTask,
        input: ByteArray,
        systemPrompt: String,
        maxOutputTokens: Int = 1024,
    ): Outcome = withContext(Dispatchers.IO) {
        when (val decision = route(task)) {
            is RouteDecision.Unavailable -> Outcome.Unavailable(decision.reason)

            is RouteDecision.UseLocal -> when (task) {
                AiTask.TRANSCRIBE_AUDIO -> try {
                    Outcome.LocalText(sttEngine.transcribe(input))
                } catch (e: LocalSttEngine.SttException) {
                    Outcome.Failed("stt: ${e.code}")
                }
                else -> Outcome.Unavailable("task has no local backend")
            }

            is RouteDecision.UseCloud -> {
                val keys = resolveCloudKeys()
                if (keys.isEmpty()) return@withContext Outcome.Unavailable("no cloud key")
                val model = cloudModelSupplier().trim().ifEmpty { GeminiCloudClient.DEFAULT_MODEL }
                var lastFailure: Outcome = Outcome.Unavailable("no cloud key")
                for (key in keys) {
                    when (val result = gemini.complete(
                        GeminiCloudClient.Config(model, key),
                        systemPrompt,
                        input.toString(Charsets.UTF_8),
                        maxOutputTokens,
                    )) {
                        is GeminiCloudClient.Result.Success ->
                            return@withContext Outcome.CloudText(result.text, result.finishReason)
                        is GeminiCloudClient.Result.ApiError -> {
                            lastFailure = Outcome.Failed("api ${result.code}: ${result.message.take(200)}")
                            if (result.code == 401 || result.code == 403 || result.code == 429) continue
                            return@withContext lastFailure
                        }
                        is GeminiCloudClient.Result.Blocked -> return@withContext Outcome.Blocked(result.reason)
                        is GeminiCloudClient.Result.TransportError -> return@withContext Outcome.Failed("network: ${result.message.take(200)}")
                    }
                }
                lastFailure
            }
        }
    }

    sealed class Outcome {
        data class LocalText(val text: String) : Outcome()
        data class CloudText(val text: String, val finishReason: String?) : Outcome()
        data class Blocked(val reason: String) : Outcome()
        data class Unavailable(val reason: String) : Outcome()
        data class Failed(val detail: String) : Outcome()
    }

    // --- internals ---------------------------------------------------------

    /**
     * Vault-sealed key first (private mode, REAL session only), falling back
     * to the host supplier (existing translator settings key).
     */
    private suspend fun resolveCloudKeys(): List<String> {
        val vaultKey = try {
            MiogramLockFacade.aiKeyMaterial()?.use { material ->
                val bytes = material.bytes()
                try { String(bytes, Charsets.UTF_8).trim() } finally { java.util.Arrays.fill(bytes, 0) }
            }
        } catch (e: Exception) {
            null
        }
        if (!vaultKey.isNullOrBlank()) return listOf(vaultKey)
        return hostKeysSupplier().asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    private fun connectivityOnline(): Boolean = try {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
            ?: return true // assume online rather than blocking features
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
        true
    }

    private fun isMetered(): Boolean = try {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        cm?.isActiveNetworkMetered ?: false
    } catch (e: Exception) {
        false
    }

    private fun persistPreferences(prefs: AiPreferences) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
                putString("overrides_json", serializeOverrides(prefs))
                putBoolean("cloud_metered", prefs.cloudAllowedOnMeteredNetwork)
            }.apply()
        } catch (e: Exception) {
            // preferences are advisory; never crash the feature on storage
        }
    }

    private fun loadPreferences(): AiPreferences = try {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        AiPreferences(
            overrides = deserializeOverrides(sp.getString("overrides_json", null)),
            cloudAllowedOnMeteredNetwork = sp.getBoolean("cloud_metered", false),
        )
    } catch (e: Exception) {
        AiPreferences()
    }

    private fun serializeOverrides(prefs: AiPreferences): String =
        prefs.overrides.entries.joinToString(";") { "${it.key.name}=${prefs.modeFor(it.key)}" }

    private fun deserializeOverrides(raw: String?): Map<AiTask, ExecutionMode> =
        raw.orEmpty().split(';').filter { it.contains('=') }.mapNotNull {
            val (taskName, modeName) = it.split('=', limit = 2)
            val task = AiTask.entries.firstOrNull { t -> t.name == taskName } ?: return@mapNotNull null
            val mode = ExecutionMode.entries.firstOrNull { m -> m.name == modeName } ?: return@mapNotNull null
            task to mode
        }.toMap()

    companion object {
        private const val PREFS = "miogram_ai"
    }
}
