<div align="center">

<img src="assets/logo.png" width="128" alt="Miogram Logo">

# Miogram (Міограм)

### *More than just a messenger. Telegram, but make it cute & powerful.*
**Next-Generation Telegram Client with Zero-Trust Security, Modern Audio Player, WebAssembly Plugins & Cyber Pixel Badges**

[![Download Latest APK](https://img.shields.io/badge/Download-Latest%20APK-00F0FF?style=for-the-badge&logo=android&logoColor=black)](https://github.com/fuckramochka/miogram/releases/latest)
[![Official Website](https://img.shields.io/badge/Website-MioGram%20Portal-FF69B4?style=for-the-badge&logo=googlechrome&logoColor=white)](https://fuckramochka.github.io/miogram/)
[![Author](https://img.shields.io/badge/Author-@dkramochka-FF2A93?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/dkramochka)
[![License](https://img.shields.io/badge/License-GPL%20v3-9D4EDD?style=for-the-badge)](LICENSE)
[![Android 15](https://img.shields.io/badge/Android%2015-16KB%20ELF%20Ready-3DDC84?style=for-the-badge&logo=android&logoColor=white)](docs/BUILD.md)

</div>

---

## ✦ Overview / Про проєкт

**Miogram** — незалежний високоефективний клієнт Telegram для Android, створений для тих, хто цінує абсолютну конфіденційність, сучасну естетику, першокласний звук і безмежну кастомізацію.

Miogram поєднує в собі:
- 🛡️ **Zero-Trust сховище «Подвійне дно»** з екстреним Duress PIN та апаратною ізоляцією StrongBox.
- 🎵 **Сучасний аудіоплеєр** з живою візуалізацією басів, текстами пісень та ергономікою Apple Music / Spotify.
- 🎨 **Мультимакетний інтерфейс**: миттєве перемикання між стилями Discord, iOS, Minimalist та класичним Telegram.
- ⚡ **WebAssembly (WASM) Rust плагін-рушій** з холодним запуском < 1 мс та мінімальним споживанням пам'яті (~150 КБ).
- ʚ♡ɞ **10 канонічних піксельних бейджів** з хмарною синхронізацією через Supabase та інтерактивними часточками.
- 🧠 **Приватний AI-роутер**: локальний Whisper STT без інтернету та розумна санітизація даних.

---

## ʚ♡ɞ Ключові можливості / Key Features

### 1. 🛡️ Захист від примусу (Duress PIN) та шифрування SQLCipher
* **Два незалежних PIN-коди:**
  * *Звичайний PIN:* розблоковує ваше основне захищене робоче середовище.
  * *Duress (Тривожний) PIN:* миттєво відкриває нейтральний декой-екран (`MiogramDecoyActivity`) без дешифрування справжніх ключів.
* **Argon2id KDF (RFC 9106) + Timing Equalization:** вирівнювання часу деривації для виключення таймінг-атак.
* **Апаратний захист StrongBox / TEE:** неекспортовані ключі в AndroidKeyStore.
* **Захист від примусової біометрії:** у захищеному режимі сканування відбитка вимикається, запобігаючи розблокуванню уві сні.
* **Миттєве очищення пам'яті (`zeroizeNow`):** асинхронне стирання відкритих ключів при переході у фоновий режим.
* **Повне шифрування бази даних:** рушій SQLCipher з перевіркою цілісності сторінок.

📖 *Детальніше у [Security Whitepaper](docs/SECURITY.md).*

---

### 2. 🎵 Сучасний аудіоплеєр з візуалізацією басів
* **Жива візуалізація басів (`MiogramBassVisualizer`):** плавний мультисмуговий спектральний аналізатор у компактному та повноекранному режимах, що адаптується до кольорів теми.
* **Повноекранна обкладинка з інфо:** назва треку, автор, кнопка улюбленого та живий візуалізатор відображаються прямо поверх повноформатної обкладинки.
* **6-кнопкова ергономічна панель:** виділена кнопка Shuffle (випадковий порядок), кнопка Repeat з підтримкою довгого натискання для виклику підменю, кнопки попереднього/наступного треку, Play/Pause та черга.
* **Фікс контрастності та подвійного ріпла:** кристально чиста біла іконка Play/Pause у режимі `SRC_IN` без темних артефактів.
* **Синхронізовані тексти пісень (LRC)** та жестове перемотування.

📖 *Детальніше у [Audio Player Architecture](docs/AUDIO_PLAYER.md).*

---

### 3. 🎨 Мультимакетний інтерфейс (Layout Switcher)
Перемикайте інтерфейс головного екрана в один дотик:
* **Discord Layout:** бічні сервери та канали, знайома структура для геймерів та спільнот.
* **iOS Cupertino:** витончена нижня панель та напівпрозорий розмитий заголовок.
* **Minimalist Rail:** ультракомпактна бічна колонка для фокусування на повідомленнях.
* **Classic & Modern Telegram:** перевірений часом швидкий інтерфейс.
* *Захист від нашарування:* автоматична система тегів `miogram_custom_layout` гарантує відсутність дублювання елементів при зміні режимів.

---

### 4. ⚡ WebAssembly (WASM) Rust плагін-рушій
* **Субмілісекундний запуск:** виконання на базі мікрорантайму WAMR без важких інтерпретаторів.
* **Мінімальний оверхед:** лише ~150 КБ оперативної пам'яті та ~85 КБ у фінальному APK.
* **Офіційний Rust SDK (`sdk/rust/miogram-plugin-sdk`):** набір інструментів з макросом `register!`, типізованими конвертами та нульовим копіюванням.
* **Криптографічний підпис Ed25519:** захист плагінів від модифікації.
* **Паралельна підтримка Python-плагінів (Chaquopy 3.11)** та Java/Kotlin розширень.

📖 *Детальніше у [Plugin Developer Guide](docs/PLUGINS_DEV_GUIDE.md).*

---

### 5. ʚ♡ɞ 10 канонічних піксельних бейджів та Supabase
* **10 унікальних стилів:** Original Visor, Neon Pink, Cyan Cyber, Dark Velvet, Angel Halo, Devil Horns, Rainbow Prismatic, Wireframe Outline, Chromatic Glitch, Royal Golden Crown.
* **Хмарна синхронізація Supabase:** статус учасника та історія нагородження зберігаються у базі PostgREST та кешуються локально для миттєвого відображення.
* **Інтерактивна картка:** натискання на бейдж показує історію та причину нагородження.

---

### 6. 🧠 Приватний AI-роутер та локальний Whisper STT
* **Автоматична санітизація (Privacy Shield):** номери карток, телефонів та паролі маскуються перед передачею в хмарні AI-сервіси.
* **Локальне розпізнавання мови (Whisper STT):** транскрипція аудіо безпосередньо на пристрої без виходу в інтернет.
* **Підтримка BYOK:** використання власних ключів Google Gemini з шифруванням у сховищі.

---

### 7. 🗑️ Великоднє яйце «Мусордроп» (`tg://musor_drop`)
* Інтерактивне відео-яйце з підтримкою відтворення як `.mp4`, так і `.mp3`.
* Вбудований ассет прямо в APK (`assets/musordrop.mp4`) забезпечує гарантовану роботу з коробки без необхідності завантажувати файли вручну.

📖 *Детальніше у [Easter Eggs Guide](docs/EASTER_EGGS.md).*

---

## 🌐 Вебсайт та пряме встановлення / Website & Downloads

* **Офіційний портал:** [https://fuckramochka.github.io/miogram/](https://fuckramochka.github.io/miogram/)
  * Лічильник активних користувачів у реальному часі через Supabase.
  * Пряме завантаження APK в один клік з GitHub Releases.
  * Інтерактивне демо тем (Strawberry milk, Lavender dream, Minty angel).
* **GitHub Releases:** [https://github.com/fuckramochka/miogram/releases/latest](https://github.com/fuckramochka/miogram/releases/latest)

---

## 🏗 Архітектура проєкту / Project Structure

Miogram слідує суворій односпрямованій архітектурі:
```
app.miogram.ui        →    app.miogram.bridge    →    app.miogram.core
(Activities, Views)        (System Keystore, DB)      (Pure JVM Crypto, Vault, WASM)
```

* `app.miogram.core` — 100% чиста JVM-логіка (криптографія, політики, кодеки), що тестується без емулятора.
* `app.miogram.bridge` — адаптери до Android-системи (AndroidKeyStore, Room, SQLCipher, Supabase).
* `sdk/rust/miogram-plugin-sdk` — Rust-бібліотека для розробки WASM-плагінів.
* `website/` — офіційний вебсайт проєкту на базі React 19, Vite та Tailwind.

---

## 🛠 Збирання з вихідного коду / Building from Source

### Системні вимоги:
* **JDK:** 21 (Eclipse Temurin або OpenJDK)
* **Android SDK:** Platform `37`, Build-Tools `36.0.0`
* **Android NDK:** `27.2.12479018`
* **Android 15 Сумісність:** вирівнювання 16 KB ELF встановлено у всіх нативних бібліотеках (`-Wl,-z,max-page-size=16384`).

### Команди збирання:
```bash
# 1. Клонувати репозиторій з субмодулями:
git clone --recursive https://github.com/fuckramochka/miogram.git
cd miogram

# 2. Зібрати Debug APK:
./gradlew assembleDebug

# 3. Зібрати оптимізований Release APK:
./gradlew assembleAfatRelease

# 4. Запустити модульні JVM-тести ядра:
./gradlew testReleaseUnitTest
```

📖 *Повний посібник зі збирання: [docs/BUILD.md](docs/BUILD.md).*

---

## 📚 Документація / Documentation Index

| Документ | Опис |
|---|---|
| 📐 [Architecture Blueprint](docs/ARCHITECTURE.md) | Цільова архітектура, правила ізоляції, карта інтеграції з ядром Telegram |
| 🛡️ [Security Whitepaper](docs/SECURITY.md) | Модель загроз, Duress PIN, Argon2id, StrongBox, SQLCipher, zeroizeNow |
| 🎵 [Audio Player Architecture](docs/AUDIO_PLAYER.md) | Живий візуалізатор басів, ергономіка керування, тексти пісень, жести |
| ⚡ [Plugin Developer Guide](docs/PLUGINS_DEV_GUIDE.md) | Створення плагінів на Rust (WASM), Python (Chaquopy) та Java/Kotlin |
| 🛠️ [Build & Compilation Guide](docs/BUILD.md) | Налаштування оточення, Gradle-скрипти, перевірка 16 KB ELF для Android 15 |
| 🎀 [Easter Eggs](docs/EASTER_EGGS.md) | Секретні команди та конвеєр відтворення Мусордропу |

---

## 📄 Ліцензія / License

Код Miogram поширюється під ліцензією **GNU General Public License v3.0 (GPL-3.0)**.  
Дивіться файл [LICENSE](LICENSE) для отримання повної інформації.

---

<div align="center">
Made with ♡ by <b>@dkramochka</b> and the Miogram Community.<br>
<i>Stay soft. Stay safe. Stay you. ✧</i>
</div>
