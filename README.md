<div align="center">

<img src="assets/logo.png" width="128" alt="Miogram">

# Miogram (Міограм)

### Secure Agentic Workspace & Next-Gen Telegram Client

**Автор проєкту:** [@dkramochka](https://t.me/dkramochka)  
[![Telegram](https://img.shields.io/badge/Telegram-@dkramochka-2CA5E0?logo=telegram&logoColor=white)](https://t.me/dkramochka)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)
[![CI](https://img.shields.io/badge/CI-Passing-brightgreen.svg)](.github/workflows/miogram.yml)

</div>

---

## 🌟 Про проєкт (Українська)

**Miogram (Міограм)** — це клієнт Telegram нового покоління, створений для забезпечення безпрецедентного рівня конфіденційності, швидкодії та інтеграції зі штучним інтелектом.

На відміну від звичайних модифікацій месенджера, Miogram переосмислює архітектуру клієнта з акцентом на **Zero-Trust безпеку, апаратну ізоляцію та високопродуктивні технології WebAssembly**.

---

### 🛡 Головні можливості та переваги

#### 1. 🕵️‍♂️ Захист «Подвійне дно» (Duress PIN & Profile Vault)
* **Два незалежні PIN-коди:** 
  * *Основний PIN:* Відкриває ваш звичайний робочий простір.
  * *Тривожний PIN (Duress):* Миттєво відкриває нейтральний пустий екран-пустушку (`MiogramDecoyActivity`), не розшифровуючи справжні ключі.
* **Argon2id KDF (RFC 9106) + Timing Equalization:** Математично однакова тривалість розрахунку хешу для реального та тривожного паролів. Зловмисник не зможе визначити тип введеного PIN-коду за мілісекундами затримки.
* **Апаратна ізоляція (TEE):** Головний ключ `MasterSecret` запечатаний неекспортованим ключем всередині **AndroidKeyStore (StrongBox/TEE)**.
* **Захист від примусу:** При активному сейфі біометричний вхід (відбиток пальця) примусово блокується, щоб унеможливити розблокування сплячої людини.
* **Миттєвий RAM Wipe:** Асинхронне занулення оперативної пам'яті (`zeroizeNow`) при кожному переході на екран блокування із захистом від гонок станів (Generation Guard).
* **Шифрована база (SQLCipher):** Пайплайн переносу бази даних `HistoryDatabaseMigrator` (`wal_checkpoint` $\rightarrow$ `sqlcipher_export` $\rightarrow$ `integrity_check`).

#### 2. ⚡ Ультрашвидкі плагіни WebAssembly (WASM) + Rust SDK
* **Запуск за < 1 мс:** Замість громіздкого Python плагіни скомпільовані в бінарний WebAssembly під нативний рушій **WAMR**.
* **Економія ресурсів:** Споживання пам'яті скорочено з 60 МБ до **~150 КБ**, а розмір оверхеду в APK — з 50 МБ до **~85 КБ**.
* **Офіційний Rust SDK (`sdk/rust/miogram-plugin-sdk`):** Повноцінний набір інструментів для розробників плагінів з макросом `register!`, протоколом `envelope.rs` та Zero-Copy передачею даних через `miogram_call`.
* **Цифровий підпис (Ed25519):** Кожен плагін підписується ключем автора; бінарний кодек `"HYPE"` блокує будь-які спроби підміни байт-коду.
* **Система дозволів (Capabilities):** Гранулярний контроль доступу (`CapabilityGate`) з авто-карантином для збійних плагінів.

#### 3. 🧠 Приватний ШІ-маршрутизатор та оффлайн STT
* **Щит приватності (`CloudPrivacyPolicy`):** Автоматичне механічне маскування номерів телефонів, номерів банківських карток, паролів та email-адрес перед відправкою тексту до хмарних моделей (Google Gemini).
* **Локальне розпізнавання мови (Whisper STT):** Аудіо-фронтенд (16 кГц PCM), BPE-токенізатор `WhisperBpeTokenizer` та декодер `OnnxWhisperTranscriber` для транскрибації голосових повідомлень прямо на NPU процесора без інтернету.
* **Сейф ключів (BYOK):** Власний API-ключ Gemini зберігається всередині шифрованого Vault і блокується в тривожній (Decoy) сесії.
* **Захист трафіку:** Автоматичне блокування важких хмарних викликів при роботі через мобільну мережу (Metered Data Guard).

#### 4. 🎨 Просторова графіка «Рідке скло» (Spatial UI)
* **Апаратний AGSL-шейдер (`MiogramLiquidGlassView`):** Розрахунок заломлення світла (Refraction), хроматичної аберації на краях та світлових відблисків на GPU (Android 13+).
* **Адаптивний fallback:** Плавний перехід на легкі напівпрозорі шари на старіших смартфонах без просідання FPS (стабільні 120 FPS).

#### 5. 🚀 Стандарти надійності та Android 15+
* **16 KB ELF Page Alignment:** Повна відповідність вимогам Google Android 15+ для новітніх процесорів (Snapdragon 8 Gen 3/4, Tensor G4).
* **Детермінована збірка (`reproducible_apk_hash.py`):** Захист від бекдорів у процесі компіляції.
* **116+ автоматичних тестів:** Покриття всіх криптографічних примітивів, сховищ та маршрутизаторів.

---

## 🏗 Архітектура проекту

Слої коду розділені за суворим принципом односпрямованих залежностей:
```
app.miogram.ui        →  app.miogram.bridge  →  app.miogram.core
(екрани, Activity)       (адаптери, Keystore)     (чиста JVM-криптографія, Vault, WASM)
```
* `app.miogram.core` — не залежить від Android SDK і тестується на чистій JVM.
* `app.miogram.bridge` — зв'язок із системою (AndroidKeyStore, Room, SQLCipher).
* `sdk/rust/miogram-plugin-sdk` — крейт для розробки WebAssembly-плагінів на Rust.

Детальний архітектурний опис знаходиться у [docs/miogram/ARCHITECTURE.md](docs/miogram/ARCHITECTURE.md).

---

## 🛠 Інструкція зі збірки

### Вимоги:
* **JDK:** 21 (Temurin або OpenJDK)
* **Android SDK:** Build-Tools `36.0.0`, NDK `27.2.12479018`, Platform `android-37.0`
* **Rust:** Stable з таргетом `wasm32-unknown-unknown` (для плагінів)
* **Python:** 3.11+ (для Chaquopy-конфігурації)

### Локальна збірка:
1. Склонувати репозиторій із сабмодулями:
   ```bash
   git clone --recursive https://github.com/fuckramochka/miogram.git miogram
   cd miogram
   ```

2. Створити `local.properties` у корені:
   ```properties
   sdk.dir=/path/to/android-sdk
   TELEGRAM_APP_ID=ваш_app_id
   TELEGRAM_APP_HASH=ваш_app_hash
   ```

3. Запустити модульні тести:
   ```bash
   ./gradlew :TMessagesProj:testReleaseUnitTest
   ```

4. Зібрати Debug APK (`arm64-v8a`):
   ```bash
   export NATIVE_TARGET="arm64-v8a"
   ./gradlew :TMessagesProj:assembleDebug
   ```
   Готовий файл буде розташований у: `TMessagesProj/build/outputs/apk/debug/`.

---

## 🦀 Розробка плагінів на Rust

Приклад простого плагіна (`sdk/rust/miogram-plugin-sdk/examples/echo.rs`):

```rust
use miogram_plugin_sdk::{register, Plugin};

#[derive(Default)]
struct EchoPlugin;

impl Plugin for EchoPlugin {
    fn handle(&mut self, op: &str, payload: &[u8]) -> Result<Vec<u8>, i32> {
        match op {
            "ping" => Ok(b"pong".to_vec()),
            "on_message" => Ok(payload.to_vec()),
            _ => Ok(Vec::new()),
        }
    }
}

register!(EchoPlugin);
```

Збірка у WebAssembly:
```bash
cargo build --manifest-path sdk/rust/miogram-plugin-sdk/Cargo.toml --target wasm32-unknown-unknown --release
```

---

## 👥 Автори та подяки

* **Автор та провідний архітектор:** [@dkramochka](https://t.me/dkramochka)
* **Базова кодова база:** NagramX, exteraGram, AyuGram, Nekogram
* **Дизайн-матеріали:** [@the8055u](https://t.me/the8055u) & [@BlueprintDsgn](https://t.me/BlueprintDsgn)
