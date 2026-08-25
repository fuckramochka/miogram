package app.miogram.core.ai

import app.miogram.bridge.ai.GeminiCloudClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun env(
    localReady: Boolean = false,
    cloudKey: Boolean = false,
    online: Boolean = true,
    metered: Boolean = false,
) = AiEnvironment(localReady, cloudKey, online, metered)

class AiRouterTest {

    @Test
    fun `transcription defaults to local and stays local when ready`() {
        val prefs = AiPreferences()
        val decision = AiRouter.route(AiTask.TRANSCRIBE_AUDIO, prefs, env(localReady = true))
        assertEquals(RouteDecision.UseLocal, decision)
    }

    @Test
    fun `transcription falls back to cloud only with key and network`() {
        val noKey = AiRouter.route(AiTask.TRANSCRIBE_AUDIO, AiPreferences(), env())
        assertTrue(noKey is RouteDecision.Unavailable)

        val withKey = AiRouter.route(AiTask.TRANSCRIBE_AUDIO, AiPreferences(), env(cloudKey = true))
        assertEquals(RouteDecision.UseCloud("local model missing"), withKey)
    }

    @Test
    fun `semantic index never leaves the device`() {
        val decision = AiRouter.route(
            AiTask.SEMANTIC_INDEX,
            AiPreferences(overrides = mapOf(AiTask.SEMANTIC_INDEX to ExecutionMode.CLOUD_ONLY)),
            env(cloudKey = true),
        )
        // Router respects explicit user override; but default must be LOCAL_ONLY.
        assertEquals(ExecutionMode.LOCAL_ONLY, AiTask.SEMANTIC_INDEX.defaultMode)
        assertTrue(decision is RouteDecision.UseCloud)
    }

    @Test
    fun `metered network blocks cloud unless explicitly allowed`() {
        val strict = AiPreferences()
        val d1 = AiRouter.route(AiTask.SUMMARIZE_THREAD, strict, env(cloudKey = true, metered = true))
        assertTrue(d1 is RouteDecision.Unavailable)

        val lenient = AiPreferences(cloudAllowedOnMeteredNetwork = true)
        val d2 = AiRouter.route(AiTask.SUMMARIZE_THREAD, lenient, env(cloudKey = true, metered = true))
        assertTrue(d2 is RouteDecision.UseCloud)
    }

    @Test
    fun `offline kills cloud-first tasks without local path`() {
        val decision = AiRouter.route(AiTask.SUMMARIZE_THREAD, AiPreferences(), env(cloudKey = true, online = false))
        assertTrue(decision is RouteDecision.Unavailable)
    }

    @Test
    fun `disabled mode short-circuits`() {
        val decision = AiRouter.route(
            AiTask.SMART_REPLIES,
            AiPreferences(overrides = mapOf(AiTask.SMART_REPLIES to ExecutionMode.DISABLED)),
            env(localReady = true, cloudKey = true),
        )
        assertTrue(decision is RouteDecision.Unavailable)
    }

    @Test
    fun `cloud-only requires key`() {
        val decision = AiRouter.route(
            AiTask.EXTRACT_ACTIONS,
            AiPreferences(overrides = mapOf(AiTask.EXTRACT_ACTIONS to ExecutionMode.CLOUD_ONLY)),
            env(),
        )
        assertTrue((decision as RouteDecision.Unavailable).reason.contains("key"))
    }
}

class CloudPrivacyPolicyTest {

    @Test
    fun `phone numbers are masked`() {
        val report = CloudPrivacyPolicy.redact("call me at +7 999 123-45-67 tomorrow")
        assertFalse(report.redactedText.contains("999"))
        assertTrue(report.redactedText.contains("[phone]"))
        assertEquals(1, report.replacements)
    }

    @Test
    fun `card-like sequences are masked before phone pass`() {
        val report = CloudPrivacyPolicy.redact("card: 4111 1111 1111 1111")
        assertFalse(report.redactedText.contains("4111"))
        assertTrue(report.redactedText.contains("[card]"))
    }

    @Test
    fun `emails are masked`() {
        val report = CloudPrivacyPolicy.redact("write to john.doe@example.com now")
        assertTrue(report.redactedText.contains("[email]"))
        assertFalse(report.redactedText.contains("john.doe"))
    }

    @Test
    fun `long hex tokens are treated as secrets`() {
        val secret = "a".repeat(40)
        val report = CloudPrivacyPolicy.redact("password is $secret ok")
        assertFalse(report.redactedText.contains(secret))
        assertTrue(report.redactedText.contains("[secret]"))
    }

    @Test
    fun `normal text passes untouched`() {
        val text = "Встречаемся в 18:00 у офиса, возьми ноутбук и зарядку"
        val report = CloudPrivacyPolicy.redact(text)
        assertEquals(text, report.redactedText)
        assertEquals(0, report.replacements)
    }
}

class GeminiWireFormatTest {

    @Test
    fun `chat body carries model roles and limits`() {
        val body = GeminiCloudClient.buildChatBody(
            model = "gemini-test",
            systemPrompt = "be brief",
            userInput = "hello",
            maxTokens = 256,
            temperature = 0.2,
        )
        assertEquals("gemini-test", body.getString("model"))
        assertEquals(2, body.getJSONArray("messages").length())
        assertEquals("system", body.getJSONArray("messages").getJSONObject(0).getString("role"))
        assertEquals(256, body.getInt("max_tokens"))
    }

    @Test
    fun `success envelope parsed to content`() {
        val json = """{"choices":[{"message":{"role":"assistant","content":"Hi!"},"finish_reason":"stop"}]}"""
        val result = GeminiCloudClient.parseSuccess(json)
        assertEquals(GeminiCloudClient.Result.Success("Hi!", "stop"), result)
    }

    @Test
    fun `prompt feedback block surfaces as Blocked`() {
        val json = """{"prompt_feedback":{"blockReason":"SAFETY"}}"""
        val result = GeminiCloudClient.parseSuccess(json)
        assertEquals(GeminiCloudClient.Result.Blocked("SAFETY"), result)
    }

    @Test
    fun `error envelope extracted`() {
        val json = """{"error":{"code":429,"message":"quota exceeded"}}"""
        val result = GeminiCloudClient.parseError(429, json)
        assertEquals(GeminiCloudClient.Result.ApiError(429, "quota exceeded"), result)
    }

    @Test
    fun `malformed success body does not throw`() {
        val result = GeminiCloudClient.parseSuccess("{not json")
        assertTrue(result is GeminiCloudClient.Result.ApiError)
    }

    @Test
    fun `empty choices blocked`() {
        val result = GeminiCloudClient.parseSuccess("""{"choices":[]}""")
        assertEquals(GeminiCloudClient.Result.Blocked("empty_choices"), result)
    }
}
