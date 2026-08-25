package app.miogram.bridge.ai

import app.miogram.core.ai.CloudPrivacyPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal OpenAI-compatible chat client pointed at the Google AI Studio
 * gateway (`https://generativelanguage.googleapis.com/v1beta/openai`),
 * i.e. Gemini Flash Lite with a user-supplied (BYOK) API key.
 *
 * Deliberately separate from tw.nekomimi.llm: Miogram tasks need their own
 * system prompts, privacy redaction and key sourcing, and must not break when
 * the translator-oriented stack refactors. Wire format is plain OpenAI chat,
 * so pointing [baseUrl] at any OpenAI-compatible provider keeps working.
 *
 * The API key is sent via Authorization header — never in the URL, never in
 * logs; [redactRequest] scrubs sensitive content before it leaves the device.
 */
class GeminiCloudClient(
    private val http: OkHttpClient = defaultHttp(),
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    data class Config(val model: String, val apiKey: String)

    sealed class Result {
        data class Success(val text: String, val finishReason: String?) : Result()
        data class ApiError(val code: Int, val message: String) : Result()
        data class Blocked(val reason: String) : Result()
        data class TransportError(val message: String) : Result()
    }

    /**
     * @param systemPrompt stable instruction; not redacted (authored by us).
     * @param userInput untrusted conversation content; ALWAYS redacted.
     */
    suspend fun complete(
        config: Config,
        systemPrompt: String,
        userInput: String,
        maxOutputTokens: Int = 1024,
        temperature: Double = 0.4,
        redactRequest: Boolean = true,
    ): Result {
        val effectiveInput = if (redactRequest) {
            CloudPrivacyPolicy.redact(userInput).redactedText
        } else {
            userInput
        }
        val body = buildChatBody(config.model, systemPrompt, effectiveInput, maxOutputTokens, temperature)
            .toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    when {
                        response.isSuccessful -> parseSuccess(text)
                        else -> parseError(response.code, text)
                    }
                }
            } catch (e: Exception) {
                Result.TransportError(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai"
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()

        /** OpenAI chat-completions request shape. Internal for unit tests. */
        internal fun buildChatBody(
            model: String,
            systemPrompt: String,
            userInput: String,
            maxTokens: Int,
            temperature: Double,
        ): JSONObject = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userInput)))
            put("temperature", temperature)
            put("max_tokens", maxTokens)
        }

        /**
         * Parses the OpenAI-compatible success envelope:
         * `{choices:[{message:{content}, finish_reason}], prompt_feedback?}`.
         * Internal for unit tests.
         */
        internal fun parseSuccess(body: String): Result = try {
            val root = JSONObject(body)

            root.optJSONObject("prompt_feedback")
                ?.optString("blockReason", "")
                ?.takeIf { it.isNotEmpty() }
                ?.let { return Result.Blocked(it) }

            val choices = root.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                Result.Blocked("empty_choices")
            } else {
                val first = choices.getJSONObject(0)
                val finishReason = if (first.isNull("finish_reason")) null else first.optString("finish_reason")
                val content = first.optJSONObject("message")?.optString("content")
                    ?: first.optString("text")

                if (content.isBlank()) {
                    Result.Blocked(finishReason ?: "empty_content")
                } else {
                    Result.Success(content, finishReason)
                }
            }
        } catch (e: Exception) {
            Result.ApiError(-1, "malformed success body: ${e.message}")
        }

        internal fun parseError(code: Int, body: String): Result {
            val message = try {
                JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(300)
            } catch (e: Exception) {
                body.take(300)
            }
            return Result.ApiError(code, message)
        }
    }
}
