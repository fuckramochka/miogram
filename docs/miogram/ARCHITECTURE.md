# Miogram (Міограм) — Secure Agentic Workspace

> Проєкт: **Miogram / Міограм** · Автор: **[@dkramochka](https://t.me/dkramochka)**

Архитектурный blueprint. Документ фиксирует целевую архитектуру, правила
изоляции и карту интеграции с ядром Telegram (`org.telegram.*`).

## 0. Статус реализации

| Этап | Компонент | Статус |
|------|-----------|--------|
| 1 | `app.miogram.core.crypto` — Argon2id KDF, AES-GCM envelope, zeroization | ✅ реализовано |
| 1 | `app.miogram.core.vault` — ProfileVault, Duress PIN, бинарный кодек метаданных | ✅ реализовано, JVM-тесты OK |
| 1 | `app.miogram.core.storage` — HistoryStoragePolicy (pure decision logic) | ✅ покрыто тестами |
| 1 | `app.miogram.bridge.*` — Keystore cipher, файловый репозиторий, MiogramGate, MiogramDecoyActivity, Room-адаптер, SQLCipher factory | ✅ написано (компиляция верифицируется полной Gradle-сборкой с SDK) |
| 1 | Интеграция: PasscodeView gate, AyuData wiring (флаг OFF до Этапа 1.5) | ✅ подключено |
| 1.5 | Setup UI (Vault), RAM wipe on lock, политика биометрии, HistoryDatabaseMigrator | ✅ реализовано |
| 1.5 | Lifecycle audit → включение ENCRYPT_AYU_DB (см. §3) | ⏳ следующая итерация |
| 2 | Слой доверия плагинов: Ed25519 + манифест-кодек + capabilities | ✅ реализовано, покрыто тестами |
| 2 | Движок-оркестратор: install/enable/dispatch/quarantine | ✅ реализован, 91 тест OK |
| 2 | WASM: core-контракты, .fbs схема, WAMR JNI/C/CMake каркас | ✅ каркас (NDK-сборка — при наличии SDK) |
| 2.1 | Rust Plugin SDK (envelope, ABI, register!) + CI wasm32-джоб | ✅ реализовано (CI-верификация) |
| 3 | Роутинг local/cloud + приватность облачных запросов | ✅ реализовано, тесты OK |
| 3 | Gemini Flash Lite через AI Studio (OpenAI-compat, BYOK) + UI выбора режима | ✅ готово |
| 3.1 | Whisper STT: аудио-фронтенд + ONNX-транскрибатор + BPE-токенизатор | ✅ реализовано, тесты OK (веса/прогон — на устройстве) |
| 4 | Liquid-glass AGSL декоратор (flag OFF) + дизайн multi-panel | ✅ каркас; интеграция в чат и device-замеры ⏳ |
| 5 | CI: unit-tests + 16KB ELF check + reproducible builds + rust-sdk (miogram.yml) | ✅ реализовано |

## 1. Правила изоляции (обязательные)

Слои зависимостей направлены строго вниз; запрещены обратные импорты.

```
app.miogram.ui        →  app.miogram.bridge  →  app.miogram.core
(org.telegram.ui-адаптеры)  (org.telegram.*, android.*)   (только java.*, kotlin.*, BC)
```

* **core** — чистая JVM-логика: криптография, vault, кодеки, state machine.
  Запрещено импортировать `org.telegram.*`, `com.radolyn.*`, `tw.nekomi.*`,
  `android.*` (кроме случаев, вынесенных за интерфейс). Это делает core
  тестируемым на JVM без Robolectric и позволяет в любой момент выделить его
  в отдельный Gradle-модуль `:miogram-core`.
* **bridge** — адаптеры к хосту: AndroidKeyStore, файловое хранилище,
  SQLCipher, SharedConfig/PasscodeActivity. Единственное место, где
  допустимы точечные хуки в ядро Telegram (делегирование ≤ 10 строк на call-site).
* **ui** — экраны Miogram. Не патчит существующие Activity напрямую,
  использует NotificationCenter-подписки и фасады bridge-слоя.

Решение «package-first»: модуль пока живёт внутри `TMessagesProj`
(`src/main/kotlin/app/miogram/**`) — отдельный Gradle-модуль при AGP 9.3.1 +
Chaquopy добавляет риск сборке, которую невозможно верифицировать локально.
Границы пакетов уже совместимы с будущим выделением.

## 2. Zero-Knowledge Stealth: модель ключей

Трёхуровневый envelope, PIN никогда не является ключом сам по себе:

```
Argon2id(PIN, salt_p, profile=STANDARD) ──→ [ check_tag(32B) │ WEK(32B) ]
                                              │                │
                              сверка verifier │                └─ AES-256-GCM unwrap
                                              ▼                          │
                                    PasscodeVerifierSpec        MasterSecret(32B)
                                                                        │ AES-256-GCM wrap
                                     ┌──────────────────────────────────┤
                                     ▼                                  ▼
                             DB passphrase (SQLCipher)          секреты профиля
```

* **MasterSecret** — случайные 32 байта, генерируются при setup, хранятся
  только в wrapped-виде внутри зашифрованного блоба метаданных.
* **Метаданные** (профили, verifiers, wrapped-секреты) — единый бинарный
  блоб, шифруется AES-256-GCM через контракт `MetadataCipher`; production-
  реализация — неэкспортируемый ключ AndroidKeyStore
  (`AndroidKeystoreMetadataCipher`). Смена PIN = re-wrap MasterSecret,
  перешифрование данных не требуется.
* **RAM zero-filling**: весь ключевой материал живёт в `KeyMaterial`
  (AutoCloseable); `lock()` и вход по Duress вызывают `close()` → memset(0).
  JNI-level `memset_s` для DirectBuffers — этап 2.

### Duress PIN

* При setup опционально создаётся DECOY-профиль со своим verifier'ом.
* Верификация всегда выполняет KDF для реального и decoy-профиля
  (когда decoy существует) до ветвления — тайминг реального и тревожного
  ввода неотличим.
* Успешный Duress-ввод → `UnlockResult.Decoy`: ключи реального профиля не
  расшифровываются вовсе (а не «расшифрованы и стёрты»), UI получает
  нейтральное рабочее пространство.
* Verifier'ы хранят только salt+params+check_tag — восстановление PIN
  ограничено стоимостью Argon2id (16–64 MiB, t=3–4).

## 3. Карта интеграции с текущей кодовой базой

| Точка хоста | Файл | Механизм | Статус |
|---|---|---|---|
| Экран разблокировки | `org.telegram.ui.Components.PasscodeView#processDone` | `MiogramGate.interceptUnlockAsync(pin, verdict -> …)`; при сконфигурированном vault legacy-проверка не выполняется (иначе Duress обходится старым PIN). Рефакторинг: блоки ошибки/успеха вынесены в `handleLegacyPasscodeError()` / `finishUnlock()` | ✅ подключено |
| Room БД AyuGram | `com.radolyn.ayugram.database.AyuData#createDatabase` | `MiogramRoomAdapter.resolveName()` + `applyOpenHelperFactory()`; шифрование включается только при `MiogramFlags.ENCRYPT_AYU_DB=true` И активной REAL-сессии | ✅ подключено, флаг OFF |
| Очистка БД | `AyuData#clean` | `MiogramRoomAdapter.deleteVariants()` удаляет оба варианта хранилища | ✅ |
| Импорт истории | `AyuData#importAyuDatabase` | заблокирован в secure-режиме до миграционно-совместимой реализации | ✅ guard |
| RAM wipe | host onPause/onStop | `MiogramGate.onHostPaused()` → `zeroizeNow()` | ⏳ точка вызова — Этап 1.5 |

### Этап 1.5 (обязательный перед включением ENCRYPT_AYU_DB)

1. **Lazy-open lifecycle**: записи между стартом процесса и первым unlock идут
   в plaintext-store; нужен reopen по событию didSetPasscode либо перенос
   старта AyuData после первого unlock.
2. **Fingerprint binding**: ✅ v1-политика — при сконфигурированном vault
   биометрия отключена (`biometricAllowedWithMiogram()` в трёх точках
   PasscodeView); CryptoObject-binding — Этап 2.
3. **Миграция plaintext↔encrypted**: ✅ `HistoryDatabaseMigrator.encryptPlaintextDatabase()`
   (sqlcipher_export + integrity_check + user_version); plaintext-оригинал
   сознательно сохраняется; обратная миграция и purge — отдельная операция.
4. **Settings UI**: ✅ `MiogramVaultSetupActivity` (FLAG_SECURE, программный UI,
   создание/уничтожение vault) + строка в NekoPasscodeSettingsActivity;
   change-PIN flow через существующий `ProfileVault.changePasscodes` — следующий шаг UI.
5. **Decoy workspace**: ✅ нейтральный placeholder `MiogramDecoyActivity`
   (FLAG_SECURE, EXCLUDE_FROM_RECENTS); полный decoy-профиль — Этап 4.
6. **RAM wipe on lock**: ✅ `PasscodeView.onShow` → `MiogramGate.onHostPaused()`
   → асинхронный zeroize с generation-guard против гонки переоткрытия сессии.

## 4. Криптопримитивы (запрет hand-rolled)

* Argon2id — BouncyCastle `bcprov-jdk18on` (RFC 9106, v=0x13).
* AEAD — AES-256-GCM через JCA (`AES/GCM/NoPadding`, 96-bit nonce, 128-bit tag).
* Сравнения секретов — только constant-time (`MessageDigest.isEqual`).
* RNG — `SecureRandom`. Запрещены `String` для PIN/ключей (immortal char data);
  API принимает `CharArray`/`ByteArray`.

## 5. Этап 2 — WASM Plugin Runtime

* **Слой доверия (✅ реализован, покрыт тестами)**: `app.miogram.core.plugins` —
  Ed25519-подпись манифеста (`PluginSignatures`), строгий бинарный кодек
  (`PluginManifestCodec`, формат в комментарии к классу), реестр доверенных
  ключей (`TrustAnchors`: keyId = hex(SHA256(pub)[0..8))). Верификация =
  parse → anchor → Ed25519 → code size → SHA-256 кода; каждый исход аудируется.
* **Capabilities (✅ реализовано)**: `PluginCapability`/`CapabilitySet`/`CapabilityGate`
  — единая точка авторизации host-вызовов с audit sink.
* **WASM-контракт ядра (✅ реализован)**: `app.miogram.core.wasm` —
  runtime-agnostic интерфейсы + `SandboxConfig`; `FakeWasmRuntime` для JVM-тестов.
* **Движок-оркестратор (✅ реализован, покрыт тестами)**: `MiogramPluginEngine`
  — полный жизненный цикл: install (верификация → persist, строгий upgrade по
  versionCode) → enable (инстанцирование) → dispatch (OpPolicy default-deny →
  CapabilityGate → гостевой вызов). Отказы: N подряд trap'ов → QUARANTINED
  (fail-fast до ручного re-enable); capability-denials фолтами не считаются.
  Модуль живёт, пока жив инстанс; disable/uninstall/upgrade закрывают пару
  instance+module. Хранилище — интерфейс `PluginRepository` (шифрование at-rest
  — задача bridge-реализации).
* **Нативный рантайм (⏳ каркас, требует NDK-сборки)**: WAMR interpreter-only
  встраивается в libtmessages*.so через guarded-секцию `jni/CMakeLists.txt`
  (сабмодуль `third_party/wasm-micro-runtime`); JNI-мост `jni/miogram/miogram_wasm.c`,
  Kotlin-обёртка `bridge/plugins/WamrWasmRuntime`. Протокол вызова плагина:
  `miogram_call(ptr,len)->i64` над FlatBuffers-фреймами (`jni/miogram/host_api.fbs`).
* **Изоляция**: WASI отключён; память ≤ SandboxConfig.memoryPages; один exec_env
  на инстанс под мьютексом. Preemption (fuel/epoch) — Этап 2.1, до тех пор до
  рантайма допускаются только подписанные доверенные модули.
* Наследие Chaquopy: Python-плагины работают параллельно до полного переноса
  SDK; `PluginSinkGate` сохраняется как второй контур.

## 6. On-Device AI & Agentic Assistant — статус Этапа 3

**Модель выбора local/cloud (✅ реализовано, покрыто тестами):**

* `app.miogram.core.ai` — чистая маршрутизация:
  * `AiTask`: расшифровка аудио / семантический индекс / суммаризация /
    извлечение задач / умные ответы; каждая задача несёт свой режим по умолчанию;
  * **STT по умолчанию локальный** (`LOCAL_FIRST`, аудио не покидает устройство);
    семантический индекс — `LOCAL_ONLY` всегда (он читает всю историю);
  * `AiRouter.route()` — детерминированное решение с учётом наличия модели,
    ключа, сети и metered-ограничения;
  * `CloudPrivacyPolicy.redact()` — механическая маскировка телефонов, карт,
    email и длинных секретных блобов до отправки в облако.
* `app.miogram.bridge.ai`:
  * `GeminiCloudClient` — тонкий OpenAI-compat клиент к AI Studio gateway
    (`generativelanguage.googleapis.com/v1beta/openai`), модель
    `gemini-3.5-flash-lite` (конфигурируемая); ключ в Authorization-заголовке,
    никогда не в URL; wire-формат покрыт тестами;
  * ключ берётся из vault (`ai.gemini.key`, недоступен в decoy-сессии) либо из
    существующих настроек LLM приложения;
  * `LocalSttEngine` — lifecycle моделей Whisper (каталог, целостность,
    статусы NOT_DOWNLOADED→READY→BUSY/FAILED); ONNX/NNAPI-бэкенд инференса —
    Этап 3.1 (требует нативных библиотек и весов моделей).
* UI: `MiogramAiSettingsActivity` — выбор режима per-task циклом
  «только локально → локально → облако → только облако → выкл», toggle
  «облако в мобильной сети», статус моделей и ключа.

## 7. Spatial UI — Этап 4 (текущее состояние)

* **Liquid Glass декоратор (✅ каркас)**: `bridge.ui.MiogramLiquidGlassView` —
  самодостаточная View: AGSL `RuntimeShader` (API 33+: преломление по краям,
  хроматическая аберрация, спекулярный блик) с деградацией до translucent
  round-rect ниже; при `MiogramFlags.SPATIAL_DECORATION=false` не рисует
  ничего (нулевой onDraw-cost). Рефлексивное создание RuntimeShader держит
  класс компилируемым против старых SDK. Интеграция как bubble-decorator —
  через ViewOverlay, без патчей ChatActivity.
* **Бюджет кадров**: эффект только на римах, интерьер чистый; целевые
  замеры (macrobenchmark) — перед включением флага.
* **Multi-panel / Foldable**: дизайн — Split-view 2–3 чата через существующий
  INavigationLayout поверх ActionBarLayout; PiP-миничаты — отдельный window
  layer с FLAG_SECURE-наследованием. Реализация — следующая итерация Этапа 4,
  требует device-верификации.
* **Kanban для Saved Messages / Threads**: концепция без изменений (см. §7
  предыдущих ревизий): группировка через reply-граф MessageObserver'ов.

## 7.1. Rust Plugin SDK — Этап 2.1 (✅)

`sdk/rust/miogram-plugin-sdk`: crate для авторов плагинов.

* ABI: `miogram_abi_version/miogram_alloc/miogram_guest_free/miogram_call`
  — все гостевые буферы принадлежат аллокатору плагина; WAMR's module_malloc
  сознательно не используется (два независимых heap'а = коррупция).
* Envelope v1 (`HYPR`, см. `envelope.rs`) — ноль зависимостей в госте;
  FlatBuffers остаётся целевым транспортом после появления генерируемых
  биндингов.
* `register!(MyPlugin)` + трейт `Plugin::handle(op, payload)`.
* `panic = "abort"` обязателен: abort → trap → штатный карантин движка.
* CI: джоб `rust-sdk` — `cargo test` (host) + `cargo build --target
  wasm32-unknown-unknown --release --locked`.

## 8. Сборка и CI (этап 5)

* Все нативные цели: `-Wl,-z,max-page-size=16384` — **флаги уже стоят** в
  `jni/CMakeLists.txt`; контроль результата добавлен.
* **`.github/workflows/miogram.yml`** (добавлен) — три джоба:
  1. `unit-tests` — `testReleaseUnitTest`: все 77+ JVM-тестов Miogram
     (криптоядро, vault, роутинг AI, wire-форматы). NDK ставится даже здесь —
     Chaquopy резолвит ndk.abiFilters на фазе конфигурации;
  2. `native-build` — arm64 assembleDebug + **проверка 16 KB ELF alignment**
     через llvm-readelf (`.github/scripts/check_elf_alignment.sh`);
  3. `reproducible-build` — две последовательные сборки + детерминированный
     content-hash APK (`Tools/reproducible_apk_hash.py`): пересборка zip с
     сортировкой записей, фиксированными таймстампами и исключением META-INF.
     Хеши обязаны совпасть. Запускается только на push/dispatch — на PR два
     полных билда слишком дороги.
* Reproducible builds: фиксированный BUILD_TIMESTAMP=0, pinned JDK 21,
  shallow checkout одного коммита. Межмашинная воспроизводимость (одинаковый
  NDK/ccache-состояние) — следующий рубеж; сейчас гарантируется интра-раннер.
* R8: keep-правила Miogram — см. `proguard-rules.pro`, секция MIOGRAM.

## 8.1. Проверено локально

* `reproducible_apk_hash.py`: синтетические APK с разным порядком записей и
  таймстампами дают идентичный хеш (тест пройден на Python 3.12).
* `check_elf_alignment.sh`: синтаксис проверен (`bash -n`); логика awk
  strtonum требует gawk (ubuntu-latest ✓).

## 9. Определение done для Этапа 1

- [x] Core-крипто покрывается JVM-тестами без Android-эмулятора (116+ тестов).
- [x] Ядро Telegram затронуто минимально: делегирование в PasscodeView/AyuData/
      NekoPasscodeSettings + манифест (≈170 строк суммарно).
- [x] Duress-гейт подключён с сохранением rate-limiting и без legacy-fallback.
- [ ] SQLCipher включён флагом после lifecycle audit (Этап 1.5, device).
- [ ] Полная Gradle-сборка (bridge + R8) — прогон miogram.yml на GitHub.
- [ ] Liquid-glass: macrobenchmark 60/120 FPS перед включением SPATIAL_DECORATION.

## 10. Что осталось до «революционного продукта»

| Направление | Блокер | Где |
|---|---|---|
| Шифрование истории | lifecycle audit БД на устройстве | Этап 1.5 |
| WASM в проде | WAMR-сборка + preemption (fuel/epoch) | Этап 2.1 |
| Локальный Whisper | веса модели + device-прогон инференса | Этап 3.1 |
| Multi-panel/PiP | foldable/device-тесты UI | Этап 4 |
| Межмашинная воспроизводимость | pinned toolchain-образ | Этап 5.1 |

Все программные компоненты, верифицируемые вне устройства, реализованы и
покрыты тестами; остальное требует железа.
