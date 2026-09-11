# Miogram Security & Zero-Trust Architecture

> **Miogram / Міограм** · Architecture & Security Whitepaper  
> Author: **[@dkramochka](https://t.me/dkramochka)**

---

## 1. Threat Model & Philosophy

Standard mobile messengers assume that anyone who enters a device passcode or provides a biometric signature has authorized full access to all private messaging history, cryptographic sessions, and cloud tokens. Under physical coercion, forced device unlock, or unauthorized device seizure, standard clients offer **zero protection**.

Miogram introduces a **Zero-Trust Duress & Anti-Coercion Security Model**:
1. **Plausible Deniability:** Under duress, entering a designated emergency PIN provides immediate access to a convincing, sterile decoy environment (`MiogramDecoyActivity`) without revealing the existence of encrypted vaults.
2. **Cryptographic Key Isolation:** Master encryption secrets are sealed within AndroidKeyStore hardware (StrongBox / TEE) and wrapped using Argon2id-derived keys.
3. **Timing Channel Elimination:** Key derivation times for normal PIN and Duress PIN are mathematically identical.
4. **Instant In-Memory Oblivion:** Sensitive plaintext buffers are zeroized upon lock transitions with generation-guarded race immunity.

---

## 2. Multi-Tier Key Wrapping Architecture

A raw PIN is **never** used directly as an encryption key. Miogram employs a three-tier envelope encryption pipeline:

```
[ User Input: PIN ]
         │
         ▼
Argon2id (RFC 9106, v=0x13)
  - Memory: 64 MiB (standard) / 16 MiB (mobile)
  - Iterations: t=3..4
  - Parallelism: p=1..2
  - Salt: Cryptographically random 32-byte salt per profile
         │
         ├───► [ Passcode Check Tag (32 bytes) ]  ───► Constant-Time Verification
         │
         └───► [ Wrapping Encryption Key (WEK, 32 bytes) ]
                         │
                         ▼  AES-256-GCM Unwrap
               [ Sealed MasterSecret Blob ]
                         │
                         ▼
               [ Ephemeral Master Secret (32 bytes) ]
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
 [ SQLCipher DB Passphrase ]    [ Profile Secret Storage ]
```

### Key Isolation Characteristics:
* **Master Secret:** Cryptographically strong 256-bit entropy generated at vault creation, never written to persistent storage in unencrypted form.
* **Metadata Blob:** Encrypted with AES-256-GCM via `MetadataCipher`. In production, this utilizes a non-exportable AndroidKeyStore hardware key (`AndroidKeystoreMetadataCipher`).
* **PIN Rotation:** Changing passcodes re-wraps the `MasterSecret` without requiring full re-encryption of the encrypted databases.

---

## 3. Duress (Decoy) Mode Engineering

### Side-Channel Timing Resistance
In naive duress implementations, attackers can measure the microsecond delay of PIN processing: if a decoy check terminates faster than an expensive Argon2id hash, the adversary detects deception.

Miogram guarantees timing uniformity:
```kotlin
// MiogramGate: Parallel timing equalization
val realDerivation = async { argon2idDerive(pin, realProfile.salt) }
val decoyDerivation = async { argon2idDerive(pin, decoyProfile.salt) }

val (realKey, decoyKey) = awaitAll(realDerivation, decoyDerivation)
// Constant-time tag check determines which session to open
```

### Behavior on Duress Trigger:
* **Zero Decryption:** Real profile master keys are never loaded into memory.
* **Decoy Workspace:** The UI displays a completely clean, innocent workspace (`MiogramDecoyActivity`) with `FLAG_SECURE` enabled and excluded from Android Recent Apps.
* **No Tamper Evidence:** No system logs, toast messages, or visual hints indicate that duress mode was entered.

---

## 4. Hardware StrongBox / TEE Integration

Where supported by the device hardware (Qualcomm Snapdragon, Google Tensor, Samsung Exynos with StrongBox Keymaster):
* **StrongBox Keymaster:** Dedicated tamper-resistant hardware security module (HSM) with independent CPU, secure memory, and true hardware RNG.
* **TEE (Trusted Execution Environment):** Isolated ARM TrustZone secure world protecting key operations against compromised Android kernels.
* **Key Attestation:** Verification that keys were generated inside genuine hardware vaults.

---

## 5. Biometric Coercion Safeguards

While biometric unlock (fingerprint / face) is convenient, it can be physically compelled (e.g. holding a sleeping user's finger to the sensor).

In Miogram:
* When the Double Bottom Vault is enabled, standard biometric unlock through `PasscodeView` is **strictly disabled** (`biometricAllowedWithMiogram() == false`).
* Only manual entry of the genuine master PIN will decrypt and mount the private workspace.

---

## 6. RAM Zeroization (`zeroizeNow`)

To defend against cold-boot attacks and memory inspection via root debuggers:
* All sensitive credentials reside inside `KeyMaterial : AutoCloseable` objects.
* When the app loses focus, transitions to background, or locks:
  ```kotlin
  MiogramGate.onHostPaused() -> zeroizeNow()
  ```
* Plaintext byte arrays and `CharArray` instances are explicitly overwritten with random/zero patterns (`Arrays.fill(..., 0)`).
* `String` objects are strictly prohibited for storing sensitive keys or PINs due to JVM string interning and immutable memory persistence.

---

## 7. Database Encryption (SQLCipher)

Miogram integrates **SQLCipher 4** for SQLite database encryption:
* **Cipher:** AES-256-CBC with PBKDF2-HMAC-SHA512 key derivation.
* **Per-Page HMAC:** Cryptographic integrity validation for every 4096-byte database page.
* **Migration Pipeline:** Automatic migration from plaintext SQLite to SQLCipher:
  1. `wal_checkpoint(TRUNCATE)`
  2. `ATTACH DATABASE ... AS encrypted KEY ...`
  3. `SELECT sqlcipher_export('encrypted')`
  4. `DETACH DATABASE encrypted`
  5. `PRAGMA cipher_integrity_check`
