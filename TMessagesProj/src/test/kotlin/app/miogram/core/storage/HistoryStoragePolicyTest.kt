package app.miogram.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryStoragePolicyTest {

    private val primary = "ayu-data"

    @Test
    fun `feature disabled keeps plaintext everywhere`() {
        val decision = HistoryStoragePolicy.resolve(primary, primary, featureEnabled = false, realSessionActive = true)
        assertEquals(HistoryStoragePolicy.Decision.Plaintext(primary), decision)
    }

    @Test
    fun `locked session keeps plaintext`() {
        val decision = HistoryStoragePolicy.resolve(primary, primary, featureEnabled = true, realSessionActive = false)
        assertEquals(HistoryStoragePolicy.Decision.Plaintext(primary), decision)
    }

    @Test
    fun `active session redirects only the primary database`() {
        val decision = HistoryStoragePolicy.resolve(primary, primary, featureEnabled = true, realSessionActive = true)
        assertEquals(
            HistoryStoragePolicy.Decision.Encrypted(primary + HistoryStoragePolicy.SECURE_SUFFIX),
            decision
        )

        val staging = HistoryStoragePolicy.resolve("ayu-data-import", primary, featureEnabled = true, realSessionActive = true)
        assertEquals(HistoryStoragePolicy.Decision.Plaintext("ayu-data-import"), staging)
    }

    @Test
    fun `secure suffix detection`() {
        assertTrue(HistoryStoragePolicy.isSecureName("$primary.secure"))
        assertFalse(HistoryStoragePolicy.isSecureName(primary))
    }

    @Test
    fun `variant names cover both stores`() {
        assertEquals(listOf(primary, "$primary.secure"), HistoryStoragePolicy.variantNames(primary))
    }
}
