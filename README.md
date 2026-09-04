<div align="center">

<img src="assets/logo.png" width="128" alt="Miogram">

# Miogram (Міограм)

### *More than just a messenger.*
**Next-Generation Telegram Client with Zero-Trust Security, WebAssembly & Cyber Pixel Badges**

[![Download APK](https://img.shields.io/badge/Download-Latest%20APK-00F0FF?style=for-the-badge&logo=android&logoColor=black)](https://github.com/fuckramochka/miogram/releases/latest)
[![Author](https://img.shields.io/badge/Author-@dkramochka-FF2A93?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/dkramochka)
[![License](https://img.shields.io/badge/License-GPL%20v3-9D4EDD?style=for-the-badge)](LICENSE)
[![CI](https://img.shields.io/badge/CI-Passing-00E5FF?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/fuckramochka/miogram/actions)

</div>

---

## ✦ Overview

**Miogram** is an advanced, high-performance Telegram client engineered from the ground up for power users who demand uncompromising privacy, hardware-backed security, cyber-aesthetic design, and boundless customization.

Unlike standard messaging forks, Miogram introduces a revolutionary architecture combining **Zero-Trust Duress Protection**, **Ultra-Fast WebAssembly (WASM) Rust Plugins**, **Liquid Glass AGSL GPU Shaders**, and a **Global Supabase-Powered Community Badge Ecosystem**.

---

## ʚ♡ɞ Key Features

### 1. ✦ 10 Canonical Pixel Badges & Supabase Cloud Ecosystem
Miogram introduces an exclusive, pixel-art community badge identity system inspired by the cyber-aesthetic of *Needy Streamer Overload* and retro PC-98 visuals:
* **10 Distinct Styles:**
  * `01 — ORIGINAL`: Canonical winged heart with tech visor, obsidian core, cyan glowing contour, and pink feather tips.
  * `02 — PINK`: Neon pink aesthetic with chevron heart ribs and pastel gradient wings.
  * `03 — CYAN`: Electric cyber sky-blue wings with luminous starlight.
  * `04 — DARK`: Midnight obsidian wings with glowing velvet violet fringe.
  * `05 — ANGEL`: Fluffy white wings, lavender periwinkle heart, and a floating glowing halo ring.
  * `06 — DEVIL`: Scalloped bat wings with cute devil horns and hot crimson/pink neon rim.
  * `07 — RAINBOW`: Prismatic 5-color rainbow spectrum feathers with golden halo trim.
  * `08 — OUTLINE`: Minimalist 1px cyber wireframe contour with bloom and transparent hollow center.
  * `09 — GLITCH`: Split chromatic RGB displacement with dynamic CRT scanline jitter.
  * `10 — PREMIUM`: Radiant 3-peak golden royal crown, amber wings, and golden chest armor ribs.
* **Atmospheric Lighting & Twinkling Sparkles:** Features radiant radial neon bloom, specular shading, and 6 animated floating starlight cross particles (✦). Contours and white pixel eyes (`• •`) remain crystal clear on AMOLED dark and light themes.
* **Supabase Cloud Resolution:** User badges, titles, and acquisition stories are stored in Supabase PostgREST database and cached locally for 0ms startup time.
* **Badge Lore & Obtain History:** Tapping someone else's badge reveals their authentic obtain story, granting reason, and date. Tapping your own badge opens the interactive cyber card carousel.
* **Strict Multi-Account Isolation:** Badges are tied strictly to verified `user_id` accounts. Secondary accounts without badges remain clean without leakage.

---

### 2. 🛡️ Duress PIN & Double Bottom Vault Protection
* **Dual Independent PIN Architecture:**
  * *Normal PIN:* Unlocks your genuine workspace.
  * *Duress (Emergency) PIN:* Instantly boots into a sterile, neutral decoy screen (`MiogramDecoyActivity`) without exposing or decrypting genuine master keys.
* **Argon2id KDF (RFC 9106) + Timing Equalization:** Mathematically uniform key derivation time between valid and decoy passes to neutralize timing side-channel attacks.
* **Hardware StrongBox / TEE Isolation:** Cryptographic master secrets are sealed within AndroidKeyStore hardware-backed keystores.
* **Biometric Forcing Defense:** In duress/vault modes, fingerprint unlocking is strictly suspended to prevent coerced unlocks while asleep.
* **Instant RAM Wipe (`zeroizeNow`):** Asynchronous zeroization of sensitive plaintext buffers upon lock transitions with generation-guard race protection.
* **Full Database Encryption:** SQLCipher database engine with automatic migration pipeline (`wal_checkpoint` $\rightarrow$ `sqlcipher_export` $\rightarrow$ `integrity_check`).

---

### 3. ⚡ WebAssembly (WASM) Rust Plugin Engine
* **< 1ms Instant Cold Boot:** Bytecode executes natively on the ultra-compact **WAMR** (WebAssembly Micro Runtime) engine instead of heavyweight interpreters.
* **Minimal Footprint:** Memory overhead slashed from ~60 MB to **~150 KB**, with APK overhead reduced to **~85 KB**.
* **Official Rust SDK (`sdk/rust/miogram-plugin-sdk`):** Native toolchain featuring the `register!` macro, typed envelope protocols, and Zero-Copy memory sharing.
* **Ed25519 Cryptographic Signatures:** Every plugin is verified against author signatures; binary `"HYPE"` codec prevents runtime tampering.
* **Granular Capability Gates:** Sandboxed permissions and automatic quarantine for faulty plugins.

---

### 4. 🎨 Spatial Liquid Glass & Audio Experience
* **Liquid Frosted Glass (AGSL):** Hardware-accelerated GPU shader calculating real-time light refraction, chromatic edge dispersion, and specular sheen at a fluid 120 FPS on Android 13+.
* **Theme Neutrality:** Full respect for user-created custom Telegram themes without color hijacking in `Theme.getColor`.
* **Apple Music & Spotify Ergonomic Player:** 1:1 Apple Music card player with live mini-bass visualizer and intuitive gesture scrubbing.
* **Discord & iOS Layout Presets:** Switch between classic Telegram layout, full Discord server/channel rails, or Cupertino frosted-glass navigation bars.

---

### 5. 🧠 Private AI Router & On-Device STT
* **Cloud Privacy Shield:** Automatic regex-based sanitization of phone numbers, credit cards, emails, and passwords before payload transmission to external AI models.
* **On-Device Whisper Speech-to-Text:** Local 16 kHz PCM audio frontend, BPE tokenizer, and ONNX Whisper decoder running directly on device NPUs without internet.
* **Bring-Your-Own-Key (BYOK):** Store private Gemini API keys inside encrypted vault storage with metered-data guards.

---

## 📱 Miogram Website & Direct APK Download

Download prebuilt releases, browse badge catalogs, and review updates directly from our landing page:
* **Website:** [Miogram Official Portal](https://fuckramochka.github.io/miogram/)
* **Direct APK Download:** [GitHub Releases](https://github.com/fuckramochka/miogram/releases/latest)

---

## 🏗 Project Architecture

Miogram enforces a strict unidirectional dependency graph:
```
app.miogram.ui        →    app.miogram.bridge    →    app.miogram.core
(Activities, Views)        (System Keystore, DB)      (Pure JVM Crypto, Vault, WASM)
```
* `app.miogram.core` — Zero Android SDK dependencies, 100% testable on pure JVM.
* `app.miogram.bridge` — Hardware bridge layer (AndroidKeyStore, Room, SQLCipher, Supabase).
* `sdk/rust/miogram-plugin-sdk` — Official Rust crate for WebAssembly plugin development.

---

## 🛠 Building from Source

### Prerequisites:
* **JDK:** 21 (Temurin or OpenJDK)
* **Android SDK:** Platform `android-37.0`, Build-Tools `36.0.0`, NDK `27.2.12479018`
* **Rust:** Stable toolchain with target `wasm32-unknown-unknown`
* **Python:** 3.11+

### Build Commands:
```bash
# 1. Clone repository with submodules
git clone --recursive https://github.com/fuckramochka/miogram.git miogram
cd miogram

# 2. Build Release APK via Gradle
./gradlew assembleAfatRelease
```

---

## 🇺🇦 Коротко про проєкт (Українська)

**Miogram (Міограм)** — український клієнт Telegram нового покоління, створений розробником [@dkramochka](https://t.me/dkramochka). Месенджер поєднує безкомпромісну безпеку «Подвійного дна» (два незалежні PIN-коди, Argon2id, StrongBox, повне шифрування SQLCipher), блискавичні WebAssembly-плагіни на Rust (<1 мс запуск, 150 КБ пам'яті), шейдери «Рідкого скла» на AGSL, аудіоплеєр у стилі Apple Music, та унікальну систему з 10 канонічних піксельних бейджів із хмарною синхронізацією через Supabase.

---

## ⚖️ License & Credits

Miogram is licensed under the [GNU General Public License v3.0 (GPLv3)](LICENSE).  
Built with ♡ by [@dkramochka](https://t.me/dkramochka) and the Miogram Community.
