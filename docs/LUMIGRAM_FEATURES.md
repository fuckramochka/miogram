# LumiGram — каталог фіч (відсортовано)

> Зріз після ребренду ExteraLess → LumiGram. Движок плагінів НЕ чіпали (`app/exteraless/plugins/*` без змін).
> Пакет застосунку: `com.lumigram.app` (`gradle.properties:18`). Імʼя APK: `lumigram` (`TMessagesProj/build.gradle:281`).
> OTA: `app/exteraless/updater/GitHubUpdater.java:47` → `lumigram/lumigram`, стабільний канал 6 год / бета 2 год, ретраї ×3, fallback `/releases/latest`, атомарне завантаження `.part` + перевірка розміру.
> Переклади: `strings_oe*.xml` покриті для всіх `values-*` (англійська як база), українська `values-uk-rUA` — повна для ядра (oe, general, appearance, chats, feed, pill, player, icons, glyph, speech, ai).

## 0. Бренд LumiGram (зроблено)
- [x] `AppName=LumiGram` у `values/strings.xml:3` + всіх `values-*/strings.xml`
- [x] `NagramX=LumiGram` у `values/strings_nax.xml:3`
- [x] `OpenExtera/AppIconExteraless/Preferences/OECrash/CloudHero/ImportInfo` → LumiGram
- [x] `AndroidManifest.xml:462,525-526` mime `vnd.com.lumigram.app.*`
- [x] `rootProject.name=lumigram`, `APP_PACKAGE`, `gramName`
- [ ] Замінити лаунчер-іконки `ic_launcher_exteraless*` на `ic_launcher_lumigram*` (файли + `AndroidManifest.xml:115,121,140-141`)
- [ ] Новий `assets/logo.png`, сплеш, `R.drawable.exteraless_icon` → `lumigram_icon`
- [ ] Свій `google-services.json`, Maps API key, `TELEGRAM_APP_ID/HASH`, `CHANNEL_METADATA_ID`
- [ ] Перейменувати Java-пакети `app.exteraless` → `app.lumigram` (відкладено: ризик, чиста переустановка)

## 1. General — `settings/OpenExteraGeneralActivity.java`
- Переклад повідомлень: кнопка, весь чат, провайдер, цільова мова, не перекладати
- Числа без округлення, секунди в часі, відносний last seen, вібро, Zalgo-фільтр
- Download/Upload Boost (Off/Fast/Ultra)
- Папка збереження в Downloads
- Профіль: приховати телефон, ID+DC
- Архів: приховати папку, відкривати свайпом вниз, заборона свайпа unarchive
- Експорт/імпорт налаштувань (.json / .extera — формат сумісності залишено), скидання
- NagramX-екстри, AyuMoments-вхід, Last.fm у музику профілю
- Пуші: статус, батарея, діагностика в буфер; Crashlytics opt-in; ContentCapture; OSM/Google карти, фікс дрейфу
- Мережа: DNS-over-HTTPS, проксі-режими; False Bottom; питати перед сторіс; Ghost-відкладена відправка 12с
- Хмара: див. розділ 7

## 2. Appearance — `settings/OpenExteraAppearanceActivity.java`
- Аватари: радіуси, єдина форма для форумів
- Список чатів: сніжинки, статус у шапці, центр заголовка, сторіс (показати/сховати/вимкнути), FAB, пошук, міні-аватарки, текст заголовка
- Папки/таби: назви/іконки/обидва, лічильник (всі/крім беззвучних/вимк), приховати «Всі чати»
- Навігація: нижня панель, планшетний режим, Predictive Back + інтенсивність, Drawer + імерсивна анімація, пункти меню (показати/сховати/порядок/дільники)
- Blur: примусове, без аватарок; скло в меню повідомлень; енергозбереження <X%
- Секції: радіус, окремі заголовки, дільники (приховано/лінія/сегменти), Glass-контур
- IconPacks, PillStack, завантаження/шапка/навігація (M3/iOS стилі), Monet (Telemone/Classic), приховування (сторіс/кнопки/таби/AI), музика профілю (Telegram/Card)

## 3. Chats — `settings/OpenExteraChatsActivity.java`
- Стікери: час, форма (круг/повідомлення), розмір; відповіді (фон/емодзі/підкладка)
- Double-tap інфо; нижня кнопка Discuss; скляне меню
- Камера: Camera2/CameraX/Telegram, стабілізація, дзеркало, широкий кут, FPS, слайдер зуму, статичний зум; фото HD; приховати плитку камери
- Відео/войс: тривалість skip, автопауза, свайп у PiP, unmute гучністю
- Стікери/емодзі безліміт; адмін-шорткати; без привітального стікера; приховати Send-as/глобальний пошук
- Ввід: тап перемикає голос/відео, keep attach, без хвостика, edited→іконка, онлайн-індикатор
- Канали: приховати Share/Gift/Search, результати опитувань до голосу, реакції (канали/групи/приват), свайп-переходи, меню (копі фото/зберегти/повтор/деталі), seamless, AI-чат
- Широкі пости каналів + стрічки; деталі повідомлення (дата/DC/бітрейт/шлях); відповідь на видалене цитатою; збереження видалених (приват/групи/канали); форвард із захищених як копія
- Посилання: підтвердження (приховані/всі/ніколи), питати (войс/дзвінки/повтори/команди), чистка трекінгу (AdGuard, оновлення фільтра)
- HDR-фото; Premium-емодзі як стікери/посилання (LumiGram-клієнти бачать як емодзі); вікно перекладу TG; inline-математика + валюти через @calcmulabot

## 4. AppNavigation — `settings/OpenExteraAppNavigationActivity.java`
- Нижня панель Show/Hide/Floating, приховати Дзвінки/Профіль/Стрічку/Контакти, без заголовків
- Планшет Auto/On/Off; анімація назад Classic/Spring/Predictive/Slide
- Drawer-класика замість крапок; переноситься в Lumi-стиль

## 5. Glyph — `settings/OpenExteraGlyphActivity.java` + `glyph/GlyphController.java`
- Nothing Phone: спалах на нові, світіння дзвінка/запису, тільки при вимкненому екрані, превʼю; заглушені не світять

## 6. AyuMoments — `settings/OpenExteraAyuMomentsActivity.java`
- Ghost (не читати/не в мережі/без typing, читати при взаємодії, без звуку)
- Збереження видалених/редагованих + медіа за типами чатів, напівпрозорі видалені, іконки deleted/edited, дата прочитання
- Regex-фільтри (глобальні/юзери/чати, реверсні, імпорт/експорт), Shadow Ban, збереження в архіві
- База: експорт/імпорт (LumiGram+AyuGram), очищення

## 7. Cloud — `settings/OpenExteraCloudActivity.java`
- Upload/Restore/Clear, статус пристрій/хмара, автосинк після змін, опція AI-ключів

## 8. Feed — `feed/FeedController.java` + `feed/ui/FeedActivity.java`, `FeedChannelsActivity.java`
- Стрічка каналів, вкладка знизу, лічильник непрочитаних, архівні вмикаються, позначити все прочитаним, автододавання нових каналів, приховані канали

## 9. Pill Stack — `pillstack/PillStackController.java`, `PillRegistry.java`, `PillStackSettingsActivity.java`
- Віджети: погода (карта/гео, 15 хв), кеш, проксі+пінг, RAM/CPU/швидкість/DC-пінг, золото, крипта (UAH/USD/EUR…), курси (з/в, додати/видалити), мій статус; порядок drag-and-drop, нескінченний скрол, скидання

## 10. Player — MD3 (`LaunchActivity.java:3275`)
- Повний/міні, черга/шафл/швидкість/перемотка, тексти (пошук LRCLIB/lrcmux, синхронний/з файлу, інструментал), джерело (чат/профіль/пошук)

## 11. Icons — `icons/IconPacksActivity.java`, `IconPacksEditorActivity.java`, `appicons/AppIconsActivity.java`
- Паки .icons (metadata.json), увімкнення/пріоритет/видалення довгим тапом, точкова заміна через picker, ап-іконки лаунчера

## 12. AI — `ai/AiController.java`, `ai/ui/AiSettingsActivity.java`, `AiResponseSheet.java`
- OpenAI-сумісні сервіси, моделі+пошук+кастом, ключ лише на пристрої, reasoning (вимк/слаб/серед/сильн), температура, стрім, історія, ролі, пресети, запитати/думає/стоп/копі/вставити/повтор

## 13. Speech/Vosk — офлайн
- Моделі мов, завантаження/активація/видалення довгим тапом, все на пристрої

## 14. Оновлення LumiGram — `updater/GitHubUpdater.java`, `UpdateSheet.java`, `UpdaterSheet.java`
- Канали Вимк/Релізи/Бета; стабільні 6 год, бета 2 год; тап по лого = примусова перевірка; пропустити версію; чейнджлог; завантаження з прогресом + скасування; атомарний `.part` + перевірка розміру >1МБ; установка через FileProvider; ретраї ×3 з бекофом; fallback `/releases/latest`; rate-limit 403/429 без спаму
- TODO: підписати APK своїм `release.keystore`, змінити `REPO` на реальний, додати SHA-256 asset у реліз і перевірку

## 15. Other / NagramX-спадок — `settings/OpenExteraOtherActivity.java` + `strings_nax.xml`
- Ghost essentials, збереження видалених у ботах, переклад цілих чатів, AI-перекладач (OpenAI/Gemini/Groq/DeepSeek/Ollama/OpenRouter), закладки, заблоковані канали, UnifiedPush, камера в кружечках, нотатки контактів, дата last seen, шрифти/blur-камера debug, імпорт чатів, DNS, закріплені реакції, календар, жести браузера — аудит дублів з General/Chats у процесі

## 16. Плагіни — НЕ ЧІПАЛИ (заморожено за вимогою)
- `plugins/PythonPluginsEngine.java`, `PluginsController.java`, `PluginPermissions.java`, `GrantStore.java`, `InstallHelper.java`, `AuditJournal.java`, `SinkGate.java`, `Watchdog.java`, інтент-хуки `LaunchActivity.java:1599-1613`, Chaquopy 3.11, тільки arm64/x86_64
- Переклади `strings_oe_plugins.xml` для UK/інших — лише копії EN (без змін логіки), щоб нічого не зламати
