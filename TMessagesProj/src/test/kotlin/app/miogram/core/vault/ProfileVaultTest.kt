package app.miogram.core.vault

import app.miogram.core.crypto.AesGcm
import app.miogram.core.crypto.AesGcmMetadataCipher
import app.miogram.core.crypto.MiogramKdf
import app.miogram.core.crypto.KeyMaterial
import app.miogram.core.vault.ProfileVault.Companion.NAMESPACE_DB_PASSPHRASE
import app.miogram.core.vault.ProfileVault.Companion.NAMESPACE_MASTER
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class InMemoryBlobRepository : BlobRepository {
    private var blob: ByteArray? = null

    override fun read(): ByteArray? = blob?.copyOf()
    override fun write(blob: ByteArray) {
        this.blob = blob.copyOf()
    }

    override fun delete() {
        blob = null
    }
}

class ProfileVaultTest {

    private lateinit var repo: InMemoryBlobRepository
    private lateinit var cipher: AesGcmMetadataCipher

    @Before
    fun setUp() {
        repo = InMemoryBlobRepository()
        cipher = AesGcmMetadataCipher()
    }

    private fun newVault(): ProfileVault =
        ProfileVault(repo, cipher)

    private fun pin(value: String): CharArray = value.toCharArray()

    private fun initializedVault(decoy: Boolean = true): ProfileVault {
        val vault = newVault()
        vault.initialize("Main", pin("1111"), MiogramKdf.Params.TEST_FAST)
        if (decoy) vault.addDecoyProfile("Decoy", pin("9999"), MiogramKdf.Params.TEST_FAST)
        return vault
    }

    @Test
    fun `real unlock opens REAL session`() {
        val vault = initializedVault()
        val result = vault.unlock(pin("1111"))
        assertTrue(result is UnlockResult.Success)
        assertEquals(ProfileKind.REAL, (result as UnlockResult.Success).profile.kind)
        result.release()
        vault.lock()
    }

    @Test
    fun `unknown passcode denied`() {
        val vault = initializedVault()
        assertEquals(UnlockResult.Denied, vault.unlock(pin("0000")))
    }

    @Test
    fun `duress unlock reveals decoy and drops real session`() {
        val vault = initializedVault()
        vault.unlock(pin("1111"))
        assertTrue(vault.isActiveSessionReal())

        val duressResult = vault.unlock(pin("9999"))
        assertTrue(duressResult is UnlockResult.Success)
        assertEquals(ProfileKind.DECOY, (duressResult as UnlockResult.Success).profile.kind)
        assertFalse(vault.isActiveSessionReal())
        duressResult.release()
    }

    @Test
    fun `lock clears session`() {
        val vault = initializedVault()
        vault.unlock(pin("1111"))
        vault.lock()
        assertNull(vault.activeProfile())
    }

    @Test
    fun `state survives vault recreation`() {
        initializedVault().unlock(pin("1111"))
        val recreated = newVault()
        assertTrue(recreated.isInitialized())
        assertTrue(recreated.hasDecoyProfiles())
        assertTrue(recreated.unlock(pin("1111")) is UnlockResult.Success)
    }

    @Test
    fun `wrapped secret is stable across lock cycles`() {
        val vault = initializedVault()
        vault.unlock(pin("1111"))

        val first = vault.ensureWrappedSecret("db.test")
        first.withRaw { original ->
            vault.lock()
            vault.unlock(pin("1111"))
            val second = vault.ensureWrappedSecret("db.test")
            second.withRaw { again -> assertArrayEquals(original, again) }
            second.close()
        }
        first.close()
    }

    @Test
    fun `ensure secret requires unlocked session`() {
        val vault = initializedVault()
        assertThrows(VaultStateException::class.java) { vault.ensureWrappedSecret("db.test") }
    }

    @Test
    fun `changePasscodes re-keys verifiers but preserves secrets`() {
        val vault = initializedVault()
        vault.unlock(pin("1111"))
        val dbKeyBefore = vault.ensureWrappedSecret(NAMESPACE_DB_PASSPHRASE)
        val beforeBytes = dbKeyBefore.bytes()
        dbKeyBefore.close()
        vault.lock()

        vault.changePasscodes(pin("1111"), pin("2222"), pin("8888"), pin("9999"))

        assertEquals(UnlockResult.Denied, vault.unlock(pin("1111")))
        assertEquals(UnlockResult.Denied, vault.unlock(pin("9999")))

        val realUnlock = vault.unlock(pin("2222"))
        assertTrue(realUnlock is UnlockResult.Success)
        val dbKeyAfter = vault.ensureWrappedSecret(NAMESPACE_DB_PASSPHRASE)
        dbKeyAfter.withRaw { after -> assertArrayEquals(beforeBytes, after) }
        dbKeyAfter.close()

        vault.lock()
        val duressUnlock = vault.unlock(pin("8888"))
        assertTrue(duressUnlock is UnlockResult.Success)
        assertEquals(ProfileKind.DECOY, (duressUnlock as UnlockResult.Success).profile.kind)
        duressUnlock.release()
    }

    @Test
    fun `re-keying decoys without current duress pin rejected`() {
        val vault = initializedVault()
        assertThrows(VaultStateException::class.java) {
            vault.changePasscodes(pin("1111"), pin("2222"), pin("8888"))
        }
    }

    @Test
    fun `changePasscodes rejects wrong current passcode`() {
        val vault = initializedVault()
        assertThrows(VaultStateException::class.java) {
            vault.changePasscodes(pin("424242"), pin("2222"), null)
        }
    }

    @Test
    fun `removing duress pin deletes decoy profiles`() {
        val vault = initializedVault()
        vault.changePasscodes(pin("1111"), pin("2222"), null)
        assertFalse(vault.hasDecoyProfiles())
        assertEquals(UnlockResult.Denied, vault.unlock(pin("9999")))
    }

    @Test
    fun `adding duress pin creates decoy`() {
        val vault = newVault()
        vault.initialize("Main", pin("1111"))
        assertFalse(vault.hasDecoyProfiles())
        vault.addDecoyProfile("Panic", pin("7777"))
        assertTrue(vault.unlock(pin("7777")) is UnlockResult.Success)
    }

    @Test
    fun `double initialization rejected`() {
        val vault = newVault()
        vault.initialize("Main", pin("1111"))
        assertThrows(VaultStateException::class.java) { vault.initialize("Again", pin("2222")) }
    }

    @Test
    fun `metadata tampering surfaces as format error`() {
        initializedVault()
        val blob = repo.read()!!
        blob[blob.size - 3] = (blob[blob.size - 3].toInt() xor 0x55).toByte()
        repo.write(blob)

        val tampered = newVault()
        assertThrows(VaultFormatException::class.java) { tampered.unlock(pin("1111")) }
    }

    @Test
    fun `wipeAll destroys persisted state`() {
        val vault = initializedVault()
        vault.wipeAll()
        assertFalse(vault.isInitialized())
        assertNull(repo.read())
        assertNull(vault.activeProfile())
    }

    @Test
    fun `master namespace sealed per profile`() {
        val vault = initializedVault()
        vault.unlock(pin("1111"))
        val realRecord = vault.activeProfile()!!
        assertTrue(realRecord.wrappedSecrets.containsKey(NAMESPACE_MASTER))
        val masterSealed = realRecord.wrappedSecrets.getValue(NAMESPACE_MASTER)
        assertTrue(masterSealed.size > AesGcm.KEY_LENGTH)
    }

    @Test
    fun `key material random respects length bounds`() {
        assertThrows(IllegalArgumentException::class.java) { KeyMaterial.random(java.security.SecureRandom(), 8) }
        val km = KeyMaterial.random(java.security.SecureRandom(), 32)
        assertEquals(32, km.size)
        assertNotEquals(0, km.bytes()[0].toInt())
        km.close()
    }
}
