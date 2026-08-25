package app.miogram.core.vault

import app.miogram.core.crypto.MiogramKdf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultCodecTest {

    private fun sampleMetadata(): VaultMetadata {
        val salt = ByteArray(16) { (it + 1).toByte() }
        val tag = ByteArray(32) { (it * 3).toByte() }
        val real = ProfileRecord(
            id = "real-1",
            kind = ProfileKind.REAL,
            label = "Main",
            verifier = PasscodeVerifierSpec(salt, MiogramKdf.Params.STANDARD, tag),
        ).withWrappedSecret("master", ByteArray(60) { 0x42 })

        val decoySalt = ByteArray(16) { (it + 7).toByte() }
        val decoy = ProfileRecord(
            id = "decoy-1",
            kind = ProfileKind.DECOY,
            label = "Личное",
            verifier = PasscodeVerifierSpec(decoySalt, MiogramKdf.Params.INTERACTIVE, ByteArray(32) { 0x11 }),
        )

        return VaultMetadata(listOf(real, decoy))
    }

    @Test
    fun `encode decode roundtrip preserves all fields`() {
        val decoded = VaultCodec.decode(VaultCodec.encode(sampleMetadata()))
        assertEquals(2, decoded.profiles.size)

        val real = decoded.findReal()!!
        assertEquals("real-1", real.id)
        assertEquals("Main", real.label)
        assertEquals(MiogramKdf.Params.STANDARD, real.verifier.params)
        assertEquals(16, real.verifier.salt.size)
        assertEquals(32, real.verifier.checkTag.size)
        assertEquals(1, real.wrappedSecrets.size)

        val decoy = decoded.decoys().single()
        assertEquals("decoy-1", decoy.id)
        assertEquals("Личное", decoy.label)
        assertEquals(MiogramKdf.Params.INTERACTIVE, decoy.verifier.params)
        assertEquals(0, decoy.wrappedSecrets.size)
    }

    @Test
    fun `unsupported version rejected`() {
        val payload = VaultCodec.encode(sampleMetadata())
        payload[4] = 0x09
        assertThrows(VaultFormatException::class.java) { VaultCodec.decode(payload) }
    }

    @Test
    fun `truncated payload rejected`() {
        val payload = VaultCodec.encode(sampleMetadata())
        assertThrows(VaultFormatException::class.java) { VaultCodec.decode(payload.copyOf(payload.size - 5)) }
    }

    @Test
    fun `trailing garbage rejected`() {
        val payload = VaultCodec.encode(sampleMetadata())
        val padded = payload.copyOf(payload.size + 3)
        assertThrows(VaultFormatException::class.java) { VaultCodec.decode(padded) }
    }

    @Test
    fun `empty profile list rejected on encode`() {
        assertThrows(VaultFormatException::class.java) { VaultCodec.encode(VaultMetadata(emptyList())) }
    }
}
