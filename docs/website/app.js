// Miogram ໒꒱ — Modern Needy Streamer Overload 2026 Web Application
// 1. Supabase PostgREST Client & Realtime User Counter
// 2. GitHub Releases API Client with Auto APK Asset Resolver
// 3. 5-Language Localization Engine (UA / EN / PL / DE / RU)
// 4. Interactive High-DPI Pixel Badge Canvas Engine with Ambient Bloom & Sparkles
// 5. Telegram ID Verification Lookup in Supabase

const REPO_OWNER = "fuckramochka";
const REPO_NAME = "miogram";
const GITHUB_API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`;
const GITHUB_FALLBACK_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest`;

const SUPABASE_URL = "https://dbxsnjoeyiqvqtrluvwu.supabase.co";
const SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRieHNuam9leWlxdnF0cmx1dnd1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1NDI1MzEsImV4cCI6MjEwNDExODUzMX0.KJ0kvON1HXZu4MzlZjapSJEhEzWYlEqQoNEstWCgIjA";

// --------------------------------------------------------------------------
// Multilingual Dictionaries (UK / EN / PL / DE / RU)
// --------------------------------------------------------------------------
const I18N = {
  uk: {
    nav_features: "Функції",
    nav_badges: "Студія Відзнак",
    nav_feed: "Розумна Стрічка",
    nav_multichat: "Мультичат & PIP",
    nav_community: "Спільнота",
    btn_header_download: "Завантажити APK",
    btn_download_apk: "⚡ Завантажити APK",

    hero_badge_pill: "INTERNET ANGEL OVERDOSE • NSO 2026 EDITION",
    hero_title_1: "Найніжніший клієнт",
    hero_title_2: "нової ери ໒꒱",
    hero_subtitle: "Telegram-клієнт у культовій неоново-піксельній естетиці Needy Streamer Overload. Потужний Gemini 3.5 AI, мультичат split-screen, плаваючі віконця PIP, розумна стрічка без реклами та жива хмарна екосистема з 10 канонічних відзнак.",
    btn_hero_download: "Завантажити останній APK",
    btn_hero_badges: "Студія 10 Стрілочок",
    hero_counter_label: "верифікованих учасників у Supabase ໒꒱",

    badge_section_pill: "10 CANONICAL ARROW STYLES",
    badge_section_title: "Студія Канонічних Відзнак ໒꒱",
    badge_section_desc: "Оберіть будь-який стиль стрілочки для миттєвого інтерактивного перегляду. Кожна відзнака має офіційний сертифікат автентичності та синхронізується в додатку через Supabase.",
    ctrl_bloom: "Сяйво:",
    cert_title: "СЕРТИФІКАТ АВТЕНТИЧНОСТІ ໒꒱",
    cert_citation_label: "ОБҐРУНТУВАННЯ НАГОРОДЖЕННЯ:",
    cert_k_status: "Хмарний статус:",
    cert_k_style: "Активний стиль:",
    cert_lore_label: "✦ СИМВОЛІЗМ ТА ЗМІСТ:",
    selector_title: "Каталог 10 Стилів",
    selector_hint: "Натисніть для вибору:",
    lookup_title: "Перевірити свій Telegram ID у базі ໒꒱",
    lookup_desc: "Введіть ваш числовий Telegram ID для миттєвої перевірки статусу у Supabase:",
    btn_lookup: "Перевірити",

    feed_title: "Розумна Стрічка (Smart Feed) ໒꒱",
    feed_desc: "Більше ніякого рекламного сміття, скаму та нескінченного спаму. Miogram зчитує публікації ваших улюблених каналів за останній тиждень, пропускає через фільтр Gemini 3.5 Flash Lite та готує змістовні картки з фотографіями та ключовими тезами.",
    feed_b1_title: "100% Фільтрація реклами:",
    feed_b1_desc: "ШІ видаляє промо-пости, казино, крипто-сигнали та реферальні посилання.",
    feed_b2_title: "Стисла вижимка зі збереженням контексту:",
    feed_b2_desc: "Чіткі тези з головними подіями та авторським тоном.",
    feed_b3_title: "Збереження фото та медіа:",
    feed_b3_desc: "Повнорозмірні фотографії прив'язуються до кожної важливої новини.",
    feed_b4_title: "Швидкий перехід у Канбан:",
    feed_b4_desc: "Зберігайте важливі новини на дошку завдань в один дотик.",

    multichat_title: "Мультичат & Floating PIP",
    multichat_desc: "Працюйте з двома діалогами одночасно! Розділіть екран на дві половини з перетягуваним розділювачем або згорніть чат у компактне плаваюче міні-віконце поверх браузера, YouTube чи ігор.",
    kanban_title: "Вбудований Канбан-Органайзер",
    kanban_desc: "Перетворюйте повідомлення, ідеї та робочі завдання на картки канбан-дошки. 4 структуровані колонки: «📥 Вхідні», «⏳ В роботі», «🔔 Важливе» та «✅ Виконано» з миттєвим переходом до чату.",

    arch_pill: "NEXT-GEN ARCHITECTURE",
    arch_title: "Архітектурна Досконалість",
    arch_subtitle: "Miogram об'єднує культову естетику з безкомпромісною продуктивністю та криптографічним захистом.",
    feat_1_title: "PIN Примусу & Подвійне Дно",
    feat_1_desc: "Два різні PIN-коди. Введення екстреного PIN-коду відкриває нейтральний екран без майстер-ключів. Захищено Argon2id RFC 9106, StrongBox TEE та SQLCipher.",
    feat_2_title: "WASM Rust Плагіни",
    feat_2_desc: "Швидкий WebAssembly Micro Runtime (WAMR). Холодний старт < 1мс, RAM 150 КБ, сувора пісочниця та криптографічний підпис Ed25519.",
    feat_3_title: "Рідке Скло Liquid Glass (AGSL)",
    feat_3_desc: "GPU-шейдери з імітацією заломлення світла та хроматичної дисперсії на стабільних 120 FPS без перебивання стандартних тем Telegram.",
    feat_4_title: "Емоційна Аудіо-Транскрипція",
    feat_4_desc: "Gemini AI розпізнає емоції та просодику мови: крик виділяється КАПСОМ зі знаками оклику, плач — трикрапками, без цензури та заміни слів на теги.",
    feat_5_title: "Хмарна Синхронізація Supabase",
    feat_5_desc: "Глобальне розпізнавання бейджиків, онлайн-лічильник та автоматична верифікація через Supabase PostgREST з 0мс офлайн-кешем.",
    feat_6_title: "Плеєр у Стилі Apple Music",
    feat_6_desc: "Вбудований аудіоплеєр з живими міні-басовими візуалізаторами, жестовою перемоткою та синхронним текстом пісень.",

    dl_title: "Отримати Miogram для Android",
    dl_desc: "Офіційні збірки криптографічно підписані та зібрані в GitHub Actions CI з вирівнюванням 16 KB ELF для Android 15+.",
    btn_dl_apk: "Завантажити Miogram APK",
    footer_copy_line: "Створено з ♡ автором @dkramochka та спільнотою Miogram."
  },

  en: {
    nav_features: "Features",
    nav_badges: "Badge Studio",
    nav_feed: "Smart Feed",
    nav_multichat: "Multi-Chat & PIP",
    nav_community: "Community",
    btn_header_download: "Download APK",
    btn_download_apk: "⚡ Download APK",

    hero_badge_pill: "INTERNET ANGEL OVERDOSE • NSO 2026 EDITION",
    hero_title_1: "The Cutest Telegram Client",
    hero_title_2: "of the New Era ໒꒱",
    hero_subtitle: "Telegram client crafted in the iconic Needy Streamer Overload cyber pixel aesthetic. Gemini 3.5 AI, split-screen dual chat, floating PIP windows, ad-free smart feed, and 10 cloud badges.",
    btn_hero_download: "Download Latest APK",
    btn_hero_badges: "10 Badges Studio",
    hero_counter_label: "verified members in Supabase ໒꒱",

    badge_section_pill: "10 CANONICAL ARROW STYLES",
    badge_section_title: "Canonical Badges Studio ໒꒱",
    badge_section_desc: "Select any arrow style for instant real-time inspection. Each badge features an authentic origin certificate and live cloud sync via Supabase.",
    ctrl_bloom: "Bloom:",
    cert_title: "CERTIFICATE OF AUTHENTICITY ໒꒱",
    cert_citation_label: "AWARD CITATION & REASON:",
    cert_k_status: "Cloud Status:",
    cert_k_style: "Active Style:",
    cert_lore_label: "✦ AESTHETIC LORE & SYMBOLISM:",
    selector_title: "10 Styles Catalog",
    selector_hint: "Click to select style:",
    lookup_title: "Verify Your Telegram ID in Supabase ໒꒱",
    lookup_desc: "Enter your numeric Telegram ID for immediate status verification in Supabase:",
    btn_lookup: "Verify ID",

    feed_title: "Smart AI Feed ໒꒱",
    feed_desc: "Say goodbye to promo spam, scams, and clutter. Miogram aggregates posts from your favorite channels over the last 7 days, filters out ads with Gemini 3.5 AI, and formats crisp news cards with photos.",
    feed_b1_title: "100% Ad & Scam Filtering:",
    feed_b1_desc: "AI removes sponsored posts, casinos, crypto shills, and referral links.",
    feed_b2_title: "Context-preserving digest:",
    feed_b2_desc: "Crisp bullet points retaining essential facts and author's voice.",
    feed_b3_title: "High-resolution photos:",
    feed_b3_desc: "Full photos and media attached directly to each news card.",
    feed_b4_title: "One-tap Kanban Save:",
    feed_b4_desc: "Pin key channel stories straight to your personal Kanban board.",

    multichat_title: "Multi-Chat & Floating PIP",
    multichat_desc: "Chat with two contacts at once! Split the screen into two resizable panes or pop out a floating mini-window over browser, YouTube, or games.",
    kanban_title: "Built-in Kanban Organizer",
    kanban_desc: "Turn messages and reminders into actionable cards. 4 neat columns: Inbox, In Progress, Important, and Done with instant deep links to chat.",

    arch_pill: "NEXT-GEN ARCHITECTURE",
    arch_title: "Architectural Excellence",
    arch_subtitle: "Miogram unites iconic aesthetic design with zero-compromise security and speed.",
    feat_1_title: "Duress PIN & Secret Vault",
    feat_1_desc: "Two distinct PIN codes. Emergency PIN unlocks decoy profile without master keys. Hardened with Argon2id RFC 9106, StrongBox TEE, and SQLCipher.",
    feat_2_title: "WASM Rust Plugins",
    feat_2_desc: "Fast WebAssembly Micro Runtime (WAMR). Cold start < 1ms, 150KB RAM, strict sandboxing, and Ed25519 cryptographic signatures.",
    feat_3_title: "Liquid Glass Shaders (AGSL)",
    feat_3_desc: "GPU shaders simulating light refraction and chromatic dispersion at steady 120 FPS without breaking Telegram theme engine.",
    feat_4_title: "Emotional Audio Transcription",
    feat_4_desc: "Gemini AI detects prosody and emotions: screams formatted in ALL CAPS with exclamation marks, crying with ellipsis, no word censorship.",
    feat_5_title: "Supabase Cloud Sync",
    feat_5_desc: "Global badge recognition, live member tally, and instant verification via Supabase PostgREST with 0ms offline caching.",
    feat_6_title: "Apple Music-Grade Player",
    feat_6_desc: "Embedded audio player with live mini-bass visualizer, gesture seeking, and synchronized LRC lyrics.",

    dl_title: "Get Miogram for Android",
    dl_desc: "Official binaries are cryptographically signed and built on GitHub Actions CI with 16KB ELF page alignment for Android 15+.",
    btn_dl_apk: "Download Miogram APK",
    footer_copy_line: "Crafted with ♡ by @dkramochka and the Miogram community."
  },

  pl: {
    nav_features: "Funkcje",
    nav_badges: "Studio Odznak",
    nav_feed: "Inteligentny Feed",
    nav_multichat: "Multi-Czat & PIP",
    nav_community: "Społeczność",
    btn_header_download: "Pobierz APK",
    btn_download_apk: "⚡ Pobierz APK",

    hero_badge_pill: "INTERNET ANGEL OVERDOSE • EDYCJA 2026",
    hero_title_1: "Najsłodszy klient",
    hero_title_2: "nowej ery ໒꒱",
    hero_subtitle: "Klient Telegram w kultowej estetyce Needy Streamer Overload. Gemini 3.5 AI, podwójny ekran split-screen, pływające okno PIP, inteligentny feed bez reklam i 10 odznak w chmurze.",
    btn_hero_download: "Pobierz najnowszy APK",
    btn_hero_badges: "Studio 10 Strzałek",
    hero_counter_label: "zweryfikowanych członków w Supabase ໒꒱",

    badge_section_pill: "10 KANONICZNYCH STYLÓW STRZAŁEK",
    badge_section_title: "Studio Kanonicznych Odznak ໒꒱",
    badge_section_desc: "Wybierz dowolny styl strzałki do natychmiastowego podglądu. Każda odznaka posiada certyfikat autentyczności i synchronizację w Supabase.",
    ctrl_bloom: "Blask:",
    cert_title: "CERTYFIKAT AUTENTYCZNOŚCI ໒꒱",
    cert_citation_label: "UZASADNIENIE PRZYZNANIA:",
    cert_k_status: "Status w chmurze:",
    cert_k_style: "Aktywny styl:",
    cert_lore_label: "✦ SYMBOLIKA I ZNACZENIE:",
    selector_title: "Katalog 10 Stylów",
    selector_hint: "Kliknij, aby wybrać styl:",
    lookup_title: "Sprawdź swój Telegram ID w Supabase ໒꒱",
    lookup_desc: "Wpisz swój numeryczny Telegram ID, aby natychmiast sprawdzić status w bazie:",
    btn_lookup: "Sprawdź",

    feed_title: "Inteligentny Feed (Smart Feed) ໒꒱",
    feed_desc: "Koniec ze spamem i reklamami. Miogram pobiera posty z Twoich kanałów z ostatniego tygodnia, filtruje reklamy za pomocą Gemini 3.5 AI i tworzy przejrzyste karty z mediami.",
    feed_b1_title: "100% filtracja reklam:",
    feed_b1_desc: "AI usuwa posty sponsorowane, kasyna, krypto i linki referencyjne.",
    feed_b2_title: "Esencjonalne podsumowanie:",
    feed_b2_desc: "Zwięzłe punkty zachowujące kluczowy kontekst i ton autora.",
    feed_b3_title: "Zdjęcia wysokiej jakości:",
    feed_b3_desc: "Zdjęcia i grafiki powiązane z każdym ważnym newsem.",
    feed_b4_title: "Zapis do Tablicy Kanban:",
    feed_b4_desc: "Przypinaj ważne posty do osobistego organizera jednym kliknięciem.",

    multichat_title: "Multi-Czat & Pływające Okno PIP",
    multichat_desc: "Rozmawiaj na dwóch czatach jednocześnie! Podziel ekran lub zwiń czat do pływającego miniaturowego dymka nad innymi aplikacjami.",
    kanban_title: "Wbudowana Tablica Kanban",
    kanban_desc: "Organizuj zadania i wiadomości w 4 kolumny: Do zrobienia, W trakcie, Ważne oraz Zrobione.",

    arch_pill: "ARCHITEKTURA NOWEJ GENERACJI",
    arch_title: "Doskonałość Techniczna",
    arch_subtitle: "Miogram łączy unikalny design z bezkompromisową wydajnością i kryptografią.",
    feat_1_title: "PIN Przymusu & Podwójne Dno",
    feat_1_desc: "Awaryjny kod PIN otwiera pusty profil bez kluczy głównych (Argon2id RFC 9106, StrongBox TEE).",
    feat_2_title: "Wtyczki WASM Rust",
    feat_2_desc: "Błyskawiczny silnik WAMR. Start < 1ms, 150KB RAM, bezpieczna piaskownica i podpis Ed25519.",
    feat_3_title: "Płynne Szkło Liquid Glass (AGSL)",
    feat_3_desc: "Zaawansowane shadery GPU na stałych 120 FPS bez psucia motywów Telegrama.",
    feat_4_title: "Emocjonalna Transkrypcja Mowy",
    feat_4_desc: "Gemini AI wykrywa krzyk (PISANY CAPSLOCKIEM!) oraz płacz (z wielokropkami...), bez cenzury słów.",
    feat_5_title: "Chmura Supabase",
    feat_5_desc: "Globalne odznaki, licznik społeczności i weryfikacja przez Supabase PostgREST.",
    feat_6_title: "Odtwarzacz Apple Music",
    feat_6_desc: "Wbudowany odtwarzacz audio z wizualizacją basu i synchronizowanym tekstem piosenek.",

    dl_title: "Pobierz Miogram na Androida",
    dl_desc: "Oficjalne wydania podpisane kryptograficznie, zoptymalizowane pod 16 KB ELF dla Androida 15+.",
    btn_dl_apk: "Pobierz Miogram APK",
    footer_copy_line: "Stworzone z ♡ przez @dkramochka i społeczność Miogram."
  },

  de: {
    nav_features: "Funktionen",
    nav_badges: "Abzeichen-Studio",
    nav_feed: "Smart-Feed",
    nav_multichat: "Multi-Chat & PIP",
    nav_community: "Community",
    btn_header_download: "APK herunterladen",
    btn_download_apk: "⚡ APK herunterladen",

    hero_badge_pill: "INTERNET ANGEL OVERDOSE • NSO 2026 EDITION",
    hero_title_1: "Der süßeste Client",
    hero_title_2: "der neuen Ära ໒꒱",
    hero_subtitle: "Telegram-Client in der ikonischen Cyber-Pixel-Ästhetik von Needy Streamer Overload. Gemini 3.5 AI, Split-Screen Dual-Chat, schwebendes Bild-in-Bild-Fenster, werbefreier Smart-Feed und 10 Cloud-Abzeichen.",
    btn_hero_download: "Neueste APK herunterladen",
    btn_hero_badges: "10 Abzeichen Studio",
    hero_counter_label: "verifizierte Mitglieder in Supabase ໒꒱",

    badge_section_pill: "10 KANONISCHE ABZEICHEN-STILE",
    badge_section_title: "Kanonisches Abzeichen-Studio ໒꒱",
    badge_section_desc: "Wählen Sie einen Stil für eine interaktive Echtzeit-Vorschau. Jedes Abzeichen enthält ein Echtheitszertifikat und synchrone Cloud-Verifizierung.",
    ctrl_bloom: "Leuchten:",
    cert_title: "ECHTHEITSZERTIFIKAT ໒꒱",
    cert_citation_label: "VERLEIHUNGSBEGRÜNDUNG:",
    cert_k_status: "Cloud-Status:",
    cert_k_style: "Aktiver Stil:",
    cert_lore_label: "✦ SYMBOLIK & BEDEUTUNG:",
    selector_title: "10 Stile Katalog",
    selector_hint: "Klicken zum Auswählen:",
    lookup_title: "Telegram-ID in Supabase prüfen ໒꒱",
    lookup_desc: "Geben Sie Ihre numerische Telegram-ID ein, um Ihren Status sofort in Supabase zu prüfen:",
    btn_lookup: "Prüfen",

    feed_title: "Smart AI Feed ໒꒱",
    feed_desc: "Schluss mit Werbespam und Abzocke. Miogram sammelt die Beiträge Ihrer Lieblingskanäle der letzten 7 Tage, filtert Werbung per Gemini 3.5 AI heraus und bereitet saubere Karten mit Fotos vor.",
    feed_b1_title: "100% Werbefilterung:",
    feed_b1_desc: "KI entfernt Promotions, Casinos, Krypto-Hype und Referral-Links.",
    feed_b2_title: "Prägnante Zusammenfassung:",
    feed_b2_desc: "Klare Kernaussagen unter Wahrung des Kontexts und Originaltons.",
    feed_b3_title: "Hochauflösende Fotos:",
    feed_b3_desc: "Originalbilder bleiben erhalten und werden direkt an News geheftet.",
    feed_b4_title: "Direkt ins Kanban-Board:",
    feed_b4_desc: "Wichtige Neuigkeiten mit einem Klick auf die Aufgabenleiste pinnen.",

    multichat_title: "Multi-Chat & Schwebendes PIP",
    multichat_desc: "In zwei Chats gleichzeitig schreiben! Teilen Sie den Bildschirm in zwei Hälften oder minimieren Sie den Chat in ein frei bewegliches Overlay-Fenster.",
    kanban_title: "Integriertes Kanban-Board",
    kanban_desc: "Verwandeln Sie Nachrichten in Aufgaben. 4 Spalten: Eingang, In Arbeit, Wichtig und Erledigt mit Direktlinks zum Chat.",

    arch_pill: "ARCHITEKTUR DER NÄCHSTEN GENERATION",
    arch_title: "Architektonische Perfektion",
    arch_subtitle: "Miogram vereint ikonisches Design mit kompromissloser Sicherheit und Geschwindigkeit.",
    feat_1_title: "Notfall-PIN & Doppeltes Versteck",
    feat_1_desc: "Notfall-PIN öffnet getarnte Oberfläche ohne Hauptschlüssel (Argon2id RFC 9106, StrongBox TEE).",
    feat_2_title: "WASM Rust Plugins",
    feat_2_desc: "Ultraschnelle WAMR Sandbox. Kaltstart < 1ms, 150KB RAM, Ed25519 Signaturen.",
    feat_3_title: "Flüssigglas-Shader (AGSL)",
    feat_3_desc: "GPU-Lichtbrechung bei stabilen 120 FPS ohne Telegram-Themes zu stören.",
    feat_4_title: "Emotionale Audio-Transkription",
    feat_4_desc: "Gemini AI erkennt Emotionen: Schreien in GROSSBUCHSTABEN!, Weinen mit Auslassungspunkten...",
    feat_5_title: "Supabase Cloud-Sync",
    feat_5_desc: "Globale Abzeichen-Erkennung und Live-Zähler via Supabase PostgREST.",
    feat_6_title: "Apple Music Audio-Player",
    feat_6_desc: "Integrierter Player mit Bass-Visualisierung und synchronisierten Songtexten.",

    dl_title: "Miogram für Android laden",
    dl_desc: "Offizielle Releases sind kryptografisch signiert und für 16KB ELF auf Android 15+ optimiert.",
    btn_dl_apk: "Miogram APK laden",
    footer_copy_line: "Erstellt mit ♡ von @dkramochka und der Miogram Community."
  },

  ru: {
    nav_features: "Функции",
    nav_badges: "Студия Отличий",
    nav_feed: "Умная Лента",
    nav_multichat: "Мультичат & PIP",
    nav_community: "Сообщество",
    btn_header_download: "Скачать APK",
    btn_download_apk: "⚡ Скачать APK",

    hero_badge_pill: "INTERNET ANGEL OVERDOSE • NSO 2026 EDITION",
    hero_title_1: "Самый нежный клиент",
    hero_title_2: "новой эры ໒꒱",
    hero_subtitle: "Telegram-клиент в культовой неоново-пиксельной эстетике Needy Streamer Overload. Мощный Gemini 3.5 AI, мультичат split-screen, плавающие окна PIP, умная лента без рекламы и 10 облачных бейджей.",
    btn_hero_download: "Скачать последний APK",
    btn_hero_badges: "Студия 10 Стрелочек",
    hero_counter_label: "верифицированных участников в Supabase ໒꒱",

    badge_section_pill: "10 КАНОНИЧЕСКИХ СТИЛЕЙ СТРЕЛОЧЕК",
    badge_section_title: "Студия Канонических Отличий ໒꒱",
    badge_section_desc: "Выберите любой стиль стрелочки для мгновенного интерактивного предпросмотра. Каждый бейдж имеет сертификат подлинности и облачную синхронизацию в Supabase.",
    ctrl_bloom: "Сияние:",
    cert_title: "СЕРТИФИКАТ ПОДЛИННОСТИ ໒꒱",
    cert_citation_label: "ОБОСНОВАНИЕ НАГРАЖДЕНИЯ:",
    cert_k_status: "Облачный статус:",
    cert_k_style: "Активный стиль:",
    cert_lore_label: "✦ СИМВОЛИЗМ И ЗНАЧЕНИЕ:",
    selector_title: "Каталог 10 Стилей",
    selector_hint: "Нажмите для выбора стиля:",
    lookup_title: "Проверить свой Telegram ID в Supabase ໒꒱",
    lookup_desc: "Введите ваш числовой Telegram ID для мгновенной проверки статуса в базе:",
    btn_lookup: "Проверить ID",

    feed_title: "Умная Лента (Smart Feed) ໒꒱",
    feed_desc: "Больше никакого рекламного мусора, скама и спама. Miogram считывает посты ваших каналов за последнюю неделю, фильтрует рекламу через Gemini 3.5 AI и собирает лаконичные карточки с фото.",
    feed_b1_title: "100% Фильтрация рекламы:",
    feed_b1_desc: "ИИ удаляет промо-посты, казино, крипто-сигналы и реферальные ссылки.",
    feed_b2_title: "Сжатая выжимка с контекстом:",
    feed_b2_desc: "Четкие тезисы с главными событиями и авторским тоном.",
    feed_b3_title: "Сохранение фото и медиа:",
    feed_b3_desc: "Полноразмерные фото привязываются к каждой важной новости.",
    feed_b4_title: "Быстрый перенос в Канбан:",
    feed_b4_desc: "Сохраняйте важные посты на доску задач в одно касание.",

    multichat_title: "Мультичат & Floating PIP",
    multichat_desc: "Общайтесь в двух чатах одновременно! Разделите экран пополам или сверните чат в компактное плавающее мини-окно поверх браузера, YouTube или игр.",
    kanban_title: "Встроенный Канбан-Органайзер",
    kanban_desc: "Превращайте сообщения и идеи в карточки канбан-доски. 4 колонки: Входящие, В работе, Важное и Выполнено с быстрыми ссылками на чат.",

    arch_pill: "АРХИТЕКТУРА НОВОГО ПОКОЛЕНИЯ",
    arch_title: "Архитектурное Совершенство",
    arch_subtitle: "Miogram объединяет культовую эстетику с бескомпромиссной безопасностью и скоростью.",
    feat_1_title: "PIN Принуждения & Двойное Дно",
    feat_1_desc: "Аварийный PIN-код открывает нейтральный профиль без мастер-ключей (Argon2id RFC 9106, StrongBox TEE, SQLCipher).",
    feat_2_title: "WASM Rust Плагины",
    feat_2_desc: "Быстрый WAMR Micro Runtime. Холодный старт < 1мс, RAM 150 КБ, изолированная песочница и подпись Ed25519.",
    feat_3_title: "Жидкое Стекло Liquid Glass (AGSL)",
    feat_3_desc: "GPU-шейдеры преломления света на стабильных 120 FPS без сбоя стандартных тем Telegram.",
    feat_4_title: "Эмоциональная Транскрипция Аудио",
    feat_4_desc: "Gemini AI распознает просодику: крик пишется КАПСОМ с восклицательными знаками, плач — многоточиями...",
    feat_5_title: "Облачная Синхронизация Supabase",
    feat_5_desc: "Глобальное распознавание бейджей и онлайн-счетчик через Supabase PostgREST.",
    feat_6_title: "Плеер в Стиле Apple Music",
    feat_6_desc: "Встроенный плеер с живой визуализацией басов, жестовой перемоткой и текстами песен.",

    dl_title: "Получить Miogram для Android",
    dl_desc: "Официальные сборки криптографически подписаны и собраны в GitHub Actions CI с выравниванием 16 KB ELF для Android 15+.",
    btn_dl_apk: "Скачать Miogram APK",
    footer_copy_line: "Создано с ♡ автором @dkramochka и сообществом Miogram."
  }
};

// --------------------------------------------------------------------------
// 10 Canonical Badges Data & Pixel Matrix
// --------------------------------------------------------------------------
const BADGES = {
  original: {
    code: "01 — ORIGINAL",
    title: {
      uk: "Класичне Кібер-Серце ໒꒱",
      en: "Canonical Cyber Heart ໒꒱",
      pl: "Klasyczne Cyber-Serce ໒꒱",
      de: "Klassisches Cyber-Herz ໒꒱",
      ru: "Классическое Кибер-Сердце ໒꒱"
    },
    reason: {
      uk: "Верифікований учасник спільноти Miogram • Неоновий канонічний стиль",
      en: "Verified Miogram Community Member • Neon Canonical Style",
      pl: "Zweryfikowany członek społeczności Miogram • Neonowy styl kanoniczny",
      de: "Verifiziertes Miogram Community-Mitglied • Neon-Kanonischer Stil",
      ru: "Верифицированный участник сообщества Miogram • Неоновый канонический стиль"
    },
    lore: {
      uk: "Канонічне крилате серце Miogram з візором-антеною, обсидіановим ядром, сяючим бірюзовим контуром та рожевими пір'ями. Перша відзнака екосистеми, з якої розпочалася вся історія проекту ໒꒱.",
      en: "Canonical Miogram winged heart with antenna visor, obsidian core, luminous cyan contour, and pink feathers. The foundational emblem of the entire ecosystem ໒꒱.",
      pl: "Kanoniczne skrzydlate serce Miogram z wizjerem-anteną, obsydianowym rdzeniem, świecącym turkusowym konturem i różowymi piórami. Pierwsza odznaka ekosystemu ໒꒱.",
      de: "Kanonisches geflügeltes Miogram-Herz mit Antennenvisier, Obsidiankern, leuchtender Cyan-Kontur und rosa Federn. Das Gründungsemblem des gesamten Ökosystems ໒꒱.",
      ru: "Каноническое крылатое сердце Miogram с визором-антенной, обсидиановым ядром, бирюзовым контуром и розовыми перьями. Первое отличие экосистемы ໒꒱."
    },
    bloomColor: "rgba(0, 240, 255, 0.4)",
    sparkleColor: "#00F0FF",
    pills: ["Cyan #00F0FF", "Pink #FF55A3", "Obsidian #0F141C"]
  },
  pink: {
    code: "02 — PINK",
    title: {
      uk: "K-Angel Неон 💖",
      en: "K-Angel Neon 💖",
      pl: "K-Angel Neon 💖",
      de: "K-Angel Neon 💖",
      ru: "K-Angel Неон 💖"
    },
    reason: {
      uk: "Естетичний контриб'ютор Needy Streamer Overload",
      en: "Aesthetic Needy Streamer Overload Contributor",
      pl: "Współtwórca estetyki Needy Streamer Overload",
      de: "Needy Streamer Overload Ästhetik-Beitragender",
      ru: "Эстетический контрибьютор Needy Streamer Overload"
    },
    lore: {
      uk: "Неоново-рожевий кібер-стиль із шевронами серця. Символ естетики Needy Streamer Overload та безмежної любові до Інтернет-Ангела †昇天†.",
      en: "Neon pink cyber aesthetic with chevron heart ribs. The quintessential symbol of Needy Streamer Overload devotion to the Internet Angel †昇天†.",
      pl: "Neonowo-różowy cyber-styl z szewronami serca. Symbol estetyki Needy Streamer Overload i oddania dla Internet Angel †昇天†.",
      de: "Neon-pinke Cyber-Ästhetik mit Herz-Chevron-Rippen. Das Symbol für Needy Streamer Overload Hingabe an den Internet-Engel †昇天†.",
      ru: "Неоново-розовый кибер-стиль с шевронами сердца. Символ эстетики Needy Streamer Overload и любви к Интернет-Ангелу †昇天†."
    },
    bloomColor: "rgba(255, 42, 147, 0.45)",
    sparkleColor: "#FF2A93",
    pills: ["Hot Pink #FF2A93", "Pure White #FFF0F7", "Plum #1B0F1C"]
  },
  cyan: {
    code: "03 — CYAN",
    title: {
      uk: "Електрична Блакить ⚡",
      en: "Electric Cyan ⚡",
      pl: "Elektryczny Błękit ⚡",
      de: "Elektrisches Cyan ⚡",
      ru: "Электрическая Лазурь ⚡"
    },
    reason: {
      uk: "За швидкість реакції та активну участь у тестуванні бета-функцій",
      en: "For lightning-fast responsiveness and active beta feature testing",
      pl: "Za błyskawiczną reakcję i aktywny udział w testach beta",
      de: "Für blitzschnelle Reaktionszeit und aktive Beta-Tests",
      ru: "За скорость реакции и активное участие в тестировании бета-функций"
    },
    lore: {
      uk: "Електричний блакитний стиль з білими акцентами та сяйвом. Символізує технологічність, холодний розум та надшвидку реакцію Miogram.",
      en: "Electric sky-blue cyber wings with luminous starlight. Symbolizes Miogram speed, clarity, cold intelligence, and next-gen technology.",
      pl: "Elektryczne błękitne skrzydła cybernetyczne. Symbolizuje szybkość, klarowność i technologię Miogram.",
      de: "Elektrische himmelblaue Cyber-Flügel mit Sternenlicht. Symbolisiert Geschwindigkeit, Klarheit und Next-Gen-Technologie.",
      ru: "Электрический лазурный стиль с белыми акцентами и сиянием. Символизирует технологичность и сверхбыструю реакцию Miogram."
    },
    bloomColor: "rgba(0, 229, 255, 0.45)",
    sparkleColor: "#00E5FF",
    pills: ["Electric Cyan #00E5FF", "Ice Cyan #E0F7FA", "Navy #0A1822"]
  },
  dark: {
    code: "04 — DARK",
    title: {
      uk: "Темний Обсидіан 🌌",
      en: "Midnight Obsidian 🌌",
      pl: "Północny Obsydian 🌌",
      de: "Mitternachts-Obsidian 🌌",
      ru: "Темный Обсидиан 🌌"
    },
    reason: {
      uk: "Особиста відзнака за прихильність до приватності та нічного режиму",
      en: "Personal distinction for commitment to privacy and dark mode stealth",
      pl: "Wyróżnienie za dbałość o prywatność i tryb nocny",
      de: "Auszeichnung für Engagement für Privatsphäre und Stealth-Modus",
      ru: "Личное отличие за приверженность приватности и ночному режиму"
    },
    lore: {
      uk: "Темний обсидіановий варіант з оксамитовим неоновим краєм для поціновувачів нічного режиму, таємничості та естетики глибокого космосу.",
      en: "Midnight obsidian wings with velvet violet aura. Crafted for night owls, stealth lovers, and deep-space cosmic vibes.",
      pl: "Ciemny obsydianowy wariant z aksamitną fioletową aurą dla miłośników nocy i kosmicznej estetyki.",
      de: "Mitternachts-Obsidian-Flügel mit violetter Aura für Liebhaber der Nacht und Weltraum-Vibes.",
      ru: "Темный обсидиановый вариант с бархатным фиолетовым краем для ценителей ночного режима и космоса."
    },
    bloomColor: "rgba(157, 78, 221, 0.45)",
    sparkleColor: "#C77DFF",
    pills: ["Velvet Violet #9D4EDD", "Neon Purple #C77DFF", "Deep Space #120B20"]
  },
  angel: {
    code: "05 — ANGEL",
    title: {
      uk: "Серафим із Німбом 👼",
      en: "Seraphim with Halo 👼",
      pl: "Serafin z Aureolą 👼",
      de: "Seraphim mit Heiligenschein 👼",
      ru: "Серафим с Нимбом 👼"
    },
    reason: {
      uk: "Духовний хранитель та мирний посол спільноти Miogram",
      en: "Spiritual guardian and peaceful ambassador of the Miogram community",
      pl: "Duchowy strażnik i ambasador pokoju społeczności Miogram",
      de: "Spiritueller Wächter und Friedensbotschafter der Miogram-Community",
      ru: "Духовный хранитель и мирный посол сообщества Miogram"
    },
    lore: {
      uk: "Ангельські крила з ширяючим білим німбом та лавандовим серцем. Відзнака гармонії, чистих помислів та піднесення †昇天†.",
      en: "Angelic wings with hovering white halo and lavender heart. The badge of purity, harmony, and transcendental ascension †昇天†.",
      pl: "Anielskie skrzydła z unoszącą się białą aureolą i lawendowym sercem. Znak harmonii i uniesienia †昇天†.",
      de: "Engelsflügel mit schwebendem Heiligenschein und Lavendelherz. Das Abzeichen für Reinheit und Transzendenz †昇天†.",
      ru: "Ангельские крылья с парящим белым нимбом и лавандовым сердцем. Знак гармонии и вознесения †昇天†."
    },
    bloomColor: "rgba(224, 170, 255, 0.4)",
    sparkleColor: "#FFFFFF",
    pills: ["White Halo #FFFFFF", "Lavender #C3BEF0", "Pure Light #FAFAFE"]
  },
  devil: {
    code: "06 — DEVIL",
    title: {
      uk: "Бунтарські Ріжки 😈",
      en: "Devil Rebel 😈",
      pl: "Buntownicze Rogi 😈",
      de: "Rebellische Hörner 😈",
      ru: "Бунтарские Рожки 😈"
    },
    reason: {
      uk: "За зухвалий гумор, креативні меми та бунтарський драйв",
      en: "For daring humor, creative community memes, and rebellious drive",
      pl: "Za odważny humor, memy i buntowniczego ducha",
      de: "Für gewagten Humor, kreative Memes und rebellischen Elan",
      ru: "За дерзкий юмор, креативные мемы и бунтарский драйв"
    },
    lore: {
      uk: "Грайливі ріжки та крила кажана з гарячим рожевим неоном. Відзнака бунтарського духу, свободи від правил та зухвалого шарму.",
      en: "Playful devil horns and scalloped bat wings with blazing neon. Distinctive emblem of rebellion, defiance, and chaos charm.",
      pl: "Zadbane rogi i skrzydła nietoperza z gorącym różowym neonem. Znak buntu i wolności od reguł.",
      de: "Verspielte Teufelshörner und Fledermausflügel mit feurigem Neon. Emblem der Rebellion und des freien Geistes.",
      ru: "Игривые рожки и крылья летучей мыши с горячим неоном. Знак бунтарского духа и свободы от правил."
    },
    bloomColor: "rgba(255, 0, 110, 0.45)",
    sparkleColor: "#FF006E",
    pills: ["Crimson Neon #FF006E", "Blood Wine #800030", "Obsidian #160810"]
  },
  pixel: {
    code: "07 — PIXEL",
    title: {
      uk: "8-Bit Ретро-Аркада 👾",
      en: "8-Bit Retro Arcade 👾",
      pl: "8-Bitowa Retro Arkada 👾",
      de: "8-Bit Retro-Arcade 👾",
      ru: "8-Bit Ретро-Аркада 👾"
    },
    reason: {
      uk: "Ретро-геймер та ентузіаст піксельної естетики 80-90х",
      en: "Retro gamer and pixel-art enthusiast of the 80-90s golden arcade era",
      pl: "Retro-gracz i entuzjasta estetyki pixel-art lat 80-90.",
      de: "Retro-Gamer und Pixel-Art-Enthusiast der goldenen Arcade-Ära",
      ru: "Ретро-геймер и энтузиаст пиксельной эстетики 80-90х"
    },
    lore: {
      uk: "Автентичний піксель-арт у стилі Game Boy Color та аркадних автоматів. Символ відданості класичним ігровим кореням та ностальгії.",
      en: "Authentic pixel-art in Game Boy Color and arcade cabinet style. Devotion to retro gaming roots and nostalgia.",
      pl: "Autentyczny pixel-art w stylu Game Boy Color i automatów zręcznościowych. Nostalgia i klasyka.",
      de: "Authentische Pixel-Art im Game Boy Color Stil. Hingabe an klassische Gaming-Wurzeln.",
      ru: "Аутентичный пиксель-арт в стиле Game Boy Color и игровых автоматов. Символ классического гейминга."
    },
    bloomColor: "rgba(56, 176, 0, 0.4)",
    sparkleColor: "#70E000",
    pills: ["Arcade Green #70E000", "CRT Mint #38B000", "Dark Phosphor #081B08"]
  },
  gold: {
    code: "08 — GOLD",
    title: {
      uk: "Золота Корона 👑",
      en: "Golden Crown 👑",
      pl: "Złota Korona 👑",
      de: "Goldene Krone 👑",
      ru: "Золотая Корона 👑"
    },
    reason: {
      uk: "Творець проекту, ключовий меценат або топ-контриб'ютор коду",
      en: "Project founder, major patron, or leading core code contributor",
      pl: "Założyciel projektu, mecenas lub wiodący kontrybutor kodu",
      de: "Projektgründer, Hauptförderer oder führender Code-Beitragender",
      ru: "Создатель проекта, ключевой меценат или топ-контрибьютор кода"
    },
    lore: {
      uk: "Королівське золото з сяючими дорогоцінними рубінами. Найвищий символ лідерства, визнання та внеску у створення Miogram.",
      en: "Royal aurum with blazing radiant rubies. The premier insignia of leadership, architectural craftsmanship, and prestige.",
      pl: "Królewskie złoto z lśniącymi rubinami. Najwyższy symbol przywództwa i wkładu w rozwój Miogram.",
      de: "Königliches Gold mit strahlenden Rubinen. Das höchste Symbol für Führung und Anerkennung.",
      ru: "Королевское золото с сияющими рубинами. Высший символ лидерства и вклада в создание Miogram."
    },
    bloomColor: "rgba(255, 183, 3, 0.45)",
    sparkleColor: "#FFB703",
    pills: ["Royal Gold #FFB703", "Imperial Ruby #D00000", "Deep Amber #201402"]
  },
  radioactive: {
    code: "09 — RADIOACTIVE",
    title: {
      uk: "Квантовий Ізотоп ☢️",
      en: "Quantum Isotope ☢️",
      pl: "Kwantowy Izotop ☢️",
      de: "Quanten-Isotop ☢️",
      ru: "Квантовый Изотоп ☢️"
    },
    reason: {
      uk: "За експериментальні дослідження та вихід за рамки можливого",
      en: "For daring experimental research and pushing beyond boundaries",
      pl: "Za odważne eksperymenty i przekraczanie granic możliwości",
      de: "Für kühne experimentelle Forschung und Pionierarbeit",
      ru: "За экспериментальные исследования и выход за рамки возможного"
    },
    lore: {
      uk: "Небезпечне радіоактивне зелене світіння атомного ядра. Відзнака нестримної енергії, інновацій та сміливих експериментів.",
      en: "Hazmat toxic green nuclear glow with cybernetic radiation hazard aura. Embodying limitless power and groundbreaking engineering.",
      pl: "Promieniotwórczy zielony blask jądra atomowego. Odznaka niepohamowanej energii i odważnych eksperymentów.",
      de: "Toxisch-grünes nukleares Leuchten. Verkörpert grenzenlose Energie und bahnbrechende Innovationen.",
      ru: "Опасное радиоактивное зеленое свечение атомного ядра. Знак неудержимой энергии и смелых экспериментов."
    },
    bloomColor: "rgba(0, 255, 102, 0.45)",
    sparkleColor: "#00FF66",
    pills: ["Isotope Green #00FF66", "Toxic Lime #76FF03", "Bunker Black #071509"]
  },
  rainbow: {
    code: "10 — RAINBOW",
    title: {
      uk: "Веселковий Спектр 🌈",
      en: "Rainbow Prism 🌈",
      pl: "Tęczowy Pryzmat 🌈",
      de: "Regenbogen-Prisma 🌈",
      ru: "Радужный Спектр 🌈"
    },
    reason: {
      uk: "За внесення яскравих барв, тепла та позитиву в життя спільноти",
      en: "For bringing vibrant colors, joy, and warmth into the community",
      pl: "Za wnoszenie barw, radości i ciepła do społeczności",
      de: "Für das Einbringen von Farbenfreude, Wärme und Positivität",
      ru: "За привнесение ярких красок, тепла и позитива в жизнь сообщества"
    },
    lore: {
      uk: "Анімований хроматичний спектр, що переливається всіма кольорами веселки. Символ різноманіття, креативу та безмежного оптимізму.",
      en: "Fluid prismatic spectrum flowing through all celestial hues. Celebrating diversity, creativity, and iridescent optimism.",
      pl: "Płynne tęczowe spektrum mieniące się wszystkimi barwami. Symbol różnorodności i optymizmu.",
      de: "Fließendes chromatisches Spektrum in allen Farben des Regenbogens. Symbol für Vielfalt und Kreativität.",
      ru: "Анимированный призматический спектр всех цветов радуги. Символ разнообразия и неиссякаемого оптимизма."
    },
    bloomColor: "rgba(255, 92, 151, 0.4)",
    sparkleColor: "#FF5C97",
    pills: ["Prism Pink #FF5C97", "Aurora Cyan #4CE0D2", "Sun Yellow #FFD166"]
  }
};

// --------------------------------------------------------------------------
// Pixel Matrix Render Engine (16x16 / 24x24)
// --------------------------------------------------------------------------
class PixelBadgeRenderer {
  constructor(canvas, badgeKey = "original", isHero = false) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.badgeKey = badgeKey;
    this.isHero = isHero;
    this.bloomOpacity = 0.35;
    this.sparklesEnabled = true;
    this.particles = [];
    this.time = 0;
    this.animId = null;

    this.initParticles();
    this.startAnimation();
  }

  initParticles() {
    this.particles = [];
    const count = this.isHero ? 12 : 20;
    for (let i = 0; i < count; i++) {
      this.particles.push({
        x: Math.random() * this.canvas.width,
        y: Math.random() * this.canvas.height,
        size: Math.random() * 3 + 1,
        speedY: -(Math.random() * 0.8 + 0.3),
        speedX: (Math.random() - 0.5) * 0.5,
        alpha: Math.random() * 0.8 + 0.2,
        decay: Math.random() * 0.015 + 0.005
      });
    }
  }

  setBadge(key) {
    this.badgeKey = key;
  }

  setBloom(value) {
    this.bloomOpacity = value / 100;
  }

  toggleSparkles() {
    this.sparklesEnabled = !this.sparklesEnabled;
    return this.sparklesEnabled;
  }

  startAnimation() {
    const loop = () => {
      this.time += 0.04;
      this.draw();
      this.animId = requestAnimationFrame(loop);
    };
    loop();
  }

  draw() {
    const w = this.canvas.width;
    const h = this.canvas.height;
    const ctx = this.ctx;

    ctx.clearRect(0, 0, w, h);

    const badge = BADGES[this.badgeKey] || BADGES.original;

    // 1. Draw dynamic ambient glow circle
    const breath = Math.sin(this.time * 2) * 10;
    const glowRadius = Math.min(w, h) * 0.38 + breath;
    const glowGrad = ctx.createRadialGradient(w / 2, h / 2, 10, w / 2, h / 2, glowRadius);
    glowGrad.addColorStop(0, badge.bloomColor);
    glowGrad.addColorStop(1, "rgba(0, 0, 0, 0)");

    ctx.fillStyle = glowGrad;
    ctx.fillRect(0, 0, w, h);

    // 2. Draw Pixel Badge Geometry
    ctx.save();
    ctx.translate(w / 2, h / 2 + Math.sin(this.time * 2.5) * 6);

    const scale = this.isHero ? 7 : 8.5;
    this.renderBadgePixels(ctx, this.badgeKey, scale, this.time);
    ctx.restore();

    // 3. Draw Floating Micro-Sparkles
    if (this.sparklesEnabled) {
      ctx.fillStyle = badge.sparkleColor;
      for (let p of this.particles) {
        ctx.globalAlpha = p.alpha;
        ctx.fillRect(Math.floor(p.x), Math.floor(p.y), p.size, p.size);

        p.y += p.speedY;
        p.x += p.speedX;
        p.alpha -= p.decay;

        if (p.alpha <= 0 || p.y < 0) {
          p.x = Math.random() * w;
          p.y = h + 10;
          p.alpha = Math.random() * 0.8 + 0.2;
        }
      }
      ctx.globalAlpha = 1.0;
    }
  }

  renderBadgePixels(ctx, key, s, t) {
    const drawRect = (x, y, w, h, col) => {
      ctx.fillStyle = col;
      ctx.fillRect(Math.floor(x * s), Math.floor(y * s), Math.floor(w * s), Math.floor(h * s));
    };

    // Color definitions based on badge style
    let cPrimary = "#00F0FF";
    let cSecondary = "#FF55A3";
    let cCore = "#0F141C";
    let cAccent = "#FFFFFF";

    if (key === "pink") {
      cPrimary = "#FF2A93"; cSecondary = "#FFF0F7"; cCore = "#1B0F1C"; cAccent = "#FF80BF";
    } else if (key === "cyan") {
      cPrimary = "#00E5FF"; cSecondary = "#FFFFFF"; cCore = "#0A1822"; cAccent = "#80F0FF";
    } else if (key === "dark") {
      cPrimary = "#9D4EDD"; cSecondary = "#C77DFF"; cCore = "#120B20"; cAccent = "#E0AAFF";
    } else if (key === "angel") {
      cPrimary = "#FFFFFF"; cSecondary = "#C3BEF0"; cCore = "#241E38"; cAccent = "#FFD166";
    } else if (key === "devil") {
      cPrimary = "#FF006E"; cSecondary = "#800030"; cCore = "#160810"; cAccent = "#FF5C97";
    } else if (key === "pixel") {
      cPrimary = "#70E000"; cSecondary = "#38B000"; cCore = "#081B08"; cAccent = "#CCFF33";
    } else if (key === "gold") {
      cPrimary = "#FFB703"; cSecondary = "#D00000"; cCore = "#201402"; cAccent = "#FFF3B0";
    } else if (key === "radioactive") {
      cPrimary = "#00FF66"; cSecondary = "#76FF03"; cCore = "#071509"; cAccent = "#B2FF59";
    } else if (key === "rainbow") {
      const hue = (t * 80) % 360;
      cPrimary = `hsl(${hue}, 100%, 65%)`;
      cSecondary = `hsl(${(hue + 60) % 360}, 100%, 75%)`;
      cCore = "#140D24";
      cAccent = "#FFFFFF";
    }

    // Centered at (0,0)
    // 1. Wings (Left & Right)
    const wingY = Math.sin(t * 3.5) * 1.5;

    // Left Wing
    drawRect(-12, -4 + wingY, 4, 3, cPrimary);
    drawRect(-10, -1 + wingY, 3, 3, cSecondary);
    drawRect(-14, -2 + wingY, 3, 2, cAccent);

    // Right Wing
    drawRect(8, -4 + wingY, 4, 3, cPrimary);
    drawRect(7, -1 + wingY, 3, 3, cSecondary);
    drawRect(11, -2 + wingY, 3, 2, cAccent);

    // 2. Heart Center Body
    drawRect(-4, -5, 3, 2, cPrimary);
    drawRect(1, -5, 3, 2, cPrimary);
    drawRect(-5, -3, 10, 4, cPrimary);
    drawRect(-4, 1, 8, 3, cPrimary);
    drawRect(-3, 4, 6, 2, cPrimary);
    drawRect(-1, 6, 2, 2, cPrimary);

    // Inner Core
    drawRect(-3, -3, 6, 4, cCore);
    drawRect(-2, 1, 4, 2, cCore);

    // Visor Antenna / Emblem
    drawRect(-2, -2, 4, 2, cSecondary);
    drawRect(-1, -1, 2, 1, cAccent);

    // Special Accessories: Halo for Angel, Horns for Devil, Crown for Gold
    if (key === "angel") {
      drawRect(-4, -9, 8, 1, "#FFD166");
      drawRect(-5, -8, 1, 1, "#FFD166");
      drawRect(4, -8, 1, 1, "#FFD166");
    } else if (key === "devil") {
      drawRect(-5, -8, 2, 3, "#FF006E");
      drawRect(3, -8, 2, 3, "#FF006E");
    } else if (key === "gold") {
      drawRect(-4, -8, 8, 2, "#FFB703");
      drawRect(-4, -10, 2, 2, "#FFB703");
      drawRect(-1, -11, 2, 3, "#FFD166");
      drawRect(2, -10, 2, 2, "#FFB703");
    }
  }
}

// --------------------------------------------------------------------------
// App Lifecycle & State Management
// --------------------------------------------------------------------------
class MiogramApp {
  constructor() {
    this.currentLang = localStorage.getItem("miogram_lang") || "uk";
    this.activeBadgeKey = "original";
    this.heroRenderer = null;
    this.studioRenderer = null;

    this.init();
  }

  init() {
    this.applyLocalization(this.currentLang);
    this.initHeaderEvents();
    this.initCanvases();
    this.renderBadgeList();
    this.updateCertificate(this.activeBadgeKey);
    this.fetchGitHubRelease();
    this.fetchSupabaseUserCount();
    this.initUserLookup();
  }

  // Language management
  applyLocalization(lang) {
    if (!I18N[lang]) lang = "uk";
    this.currentLang = lang;
    localStorage.setItem("miogram_lang", lang);

    document.documentElement.lang = lang;
    document.getElementById("currentLangLabel").innerText = lang.toUpperCase();

    // Update static DOM elements
    const elements = document.querySelectorAll("[data-i18n]");
    elements.forEach(el => {
      const key = el.getAttribute("data-i18n");
      if (I18N[lang] && I18N[lang][key]) {
        el.innerText = I18N[lang][key];
      }
    });

    // Update dynamic text
    this.updateCertificate(this.activeBadgeKey);
    this.updateHeroBadgeMeta(this.activeBadgeKey);
  }

  initHeaderEvents() {
    const langBtn = document.getElementById("langDropdownBtn");
    const langDropdown = document.getElementById("langDropdownMenu");

    langBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      langDropdown.classList.toggle("show");
    });

    document.addEventListener("click", () => {
      langDropdown.classList.remove("show");
    });

    const langOpts = document.querySelectorAll(".lang-opt");
    langOpts.forEach(opt => {
      opt.addEventListener("click", () => {
        const selectedLang = opt.getAttribute("data-lang");
        this.applyLocalization(selectedLang);
        langDropdown.classList.remove("show");
      });
    });

    // Mobile drawer toggle
    const menuToggle = document.getElementById("mobileMenuToggle");
    const drawer = document.getElementById("mobileDrawer");
    const drawerClose = document.getElementById("drawerCloseBtn");

    menuToggle.addEventListener("click", () => drawer.classList.add("open"));
    drawerClose.addEventListener("click", () => drawer.classList.remove("open"));

    document.querySelectorAll(".drawer-link, .drawer-btn-download").forEach(link => {
      link.addEventListener("click", () => drawer.classList.remove("open"));
    });
  }

  initCanvases() {
    const heroCanvas = document.getElementById("heroBadgeCanvas");
    if (heroCanvas) {
      this.heroRenderer = new PixelBadgeRenderer(heroCanvas, "original", true);
    }

    const studioCanvas = document.getElementById("studioBadgeCanvas");
    if (studioCanvas) {
      this.studioRenderer = new PixelBadgeRenderer(studioCanvas, "original", false);
    }

    // Studio controls
    const bloomSlider = document.getElementById("bloomSlider");
    if (bloomSlider) {
      bloomSlider.addEventListener("input", (e) => {
        if (this.studioRenderer) {
          this.studioRenderer.setBloom(e.target.value);
        }
      });
    }

    const sparkleBtn = document.getElementById("sparkleToggleBtn");
    if (sparkleBtn) {
      sparkleBtn.addEventListener("click", () => {
        if (this.studioRenderer) {
          const active = this.studioRenderer.toggleSparkles();
          sparkleBtn.classList.toggle("active", active);
        }
      });
    }
  }

  renderBadgeList() {
    const listContainer = document.getElementById("badgeCardsList");
    if (!listContainer) return;

    listContainer.innerHTML = "";

    Object.keys(BADGES).forEach(key => {
      const badge = BADGES[key];
      const item = document.createElement("div");
      item.className = `badge-select-item ${key === this.activeBadgeKey ? "active" : ""}`;
      item.setAttribute("data-key", key);

      // Mini thumbnail canvas
      const canvas = document.createElement("canvas");
      canvas.width = 44;
      canvas.height = 44;
      canvas.className = "item-thumb-canvas";
      const ctx = canvas.getContext("2d");
      const thumbRenderer = new PixelBadgeRenderer(canvas, key, true);

      const info = document.createElement("div");
      info.className = "item-info";

      const code = document.createElement("span");
      code.className = "item-code";
      code.innerText = badge.code;

      const name = document.createElement("span");
      name.className = "item-name";
      name.innerText = badge.title[this.currentLang] || badge.title.uk;

      info.appendChild(code);
      info.appendChild(name);

      item.appendChild(canvas);
      item.appendChild(info);

      item.addEventListener("click", () => {
        this.selectBadge(key);
      });

      listContainer.appendChild(item);
    });
  }

  selectBadge(key) {
    this.activeBadgeKey = key;

    // Highlight selected item
    document.querySelectorAll(".badge-select-item").forEach(el => {
      el.classList.toggle("active", el.getAttribute("data-key") === key);
    });

    if (this.studioRenderer) {
      this.studioRenderer.setBadge(key);
    }
    if (this.heroRenderer) {
      this.heroRenderer.setBadge(key);
    }

    this.updateCertificate(key);
    this.updateHeroBadgeMeta(key);
  }

  updateHeroBadgeMeta(key) {
    const badge = BADGES[key] || BADGES.original;
    const nameEl = document.getElementById("heroBadgeName");
    const descEl = document.getElementById("heroBadgeDesc");

    if (nameEl) nameEl.innerText = badge.title[this.currentLang] || badge.title.uk;
    if (descEl) descEl.innerText = badge.reason[this.currentLang] || badge.reason.uk;
  }

  updateCertificate(key) {
    const badge = BADGES[key] || BADGES.original;
    const codeEl = document.getElementById("studioBadgeCode");
    const reasonEl = document.getElementById("certReason");
    const styleEl = document.getElementById("certStyleName");
    const loreEl = document.getElementById("certLore");
    const paletteEl = document.getElementById("studioPalette");

    if (codeEl) codeEl.innerText = badge.code;
    if (reasonEl) reasonEl.innerText = badge.reason[this.currentLang] || badge.reason.uk;
    if (styleEl) styleEl.innerText = badge.title[this.currentLang] || badge.title.uk;
    if (loreEl) loreEl.innerText = badge.lore[this.currentLang] || badge.lore.uk;

    if (paletteEl) {
      paletteEl.innerHTML = "";
      badge.pills.forEach(pillText => {
        const pill = document.createElement("span");
        pill.className = "palette-pill";
        pill.innerText = pillText;
        paletteEl.appendChild(pill);
      });
    }
  }

  // GitHub Release API
  async fetchGitHubRelease() {
    try {
      const resp = await fetch(GITHUB_API_URL);
      if (!resp.ok) throw new Error("Network response not ok");
      const data = await resp.json();

      const tag = data.tag_name || "v1.0.0";
      const heroTagEl = document.getElementById("heroReleaseTag");
      if (heroTagEl) {
        heroTagEl.innerText = `${tag} • Android 15+ 16KB ELF`;
      }

      // Find apk download URL
      let apkUrl = data.html_url || GITHUB_FALLBACK_URL;
      if (data.assets && data.assets.length > 0) {
        const apkAsset = data.assets.find(a => a.name.endsWith(".apk"));
        if (apkAsset) {
          apkUrl = apkAsset.browser_download_url;
        }
      }

      const heroDl = document.getElementById("heroDownloadBtn");
      if (heroDl) heroDl.href = apkUrl;

      const finalDl = document.getElementById("finalDownloadBtn");
      if (finalDl) finalDl.href = apkUrl;
    } catch (e) {
      console.warn("Using fallback GitHub release link", e);
    }
  }

  // Supabase User Counter & Realtime Tally
  async fetchSupabaseUserCount() {
    try {
      const resp = await fetch(`${SUPABASE_URL}/rest/v1/miogram_badges?select=user_id`, {
        headers: {
          "apikey": SUPABASE_ANON,
          "Authorization": `Bearer ${SUPABASE_ANON}`
        }
      });
      if (resp.ok) {
        const data = await resp.json();
        if (Array.isArray(data) && data.length > 0) {
          const counterEl = document.getElementById("liveUserCount");
          if (counterEl) {
            counterEl.innerText = `${data.length.toLocaleString()}+`;
          }
        }
      }
    } catch (e) {
      console.warn("Supabase counter fetch:", e);
    }
  }

  // Telegram ID Verification Lookup
  initUserLookup() {
    const input = document.getElementById("lookupIdInput");
    const btn = document.getElementById("lookupBtn");
    const resultBox = document.getElementById("lookupResult");

    if (!btn || !input || !resultBox) return;

    btn.addEventListener("click", async () => {
      const val = input.value.trim();
      if (!val) {
        resultBox.style.display = "block";
        resultBox.style.background = "rgba(255, 92, 151, 0.15)";
        resultBox.style.color = "#ff5c97";
        resultBox.innerText = "Будь ласка, введіть числовий Telegram ID.";
        return;
      }

      resultBox.style.display = "block";
      resultBox.style.background = "rgba(255, 255, 255, 0.05)";
      resultBox.style.color = "#c4bcdc";
      resultBox.innerText = "Запит до Supabase PostgREST...";

      try {
        const url = `${SUPABASE_URL}/rest/v1/miogram_badges?user_id=eq.${encodeURIComponent(val)}&select=*`;
        const resp = await fetch(url, {
          headers: {
            "apikey": SUPABASE_ANON,
            "Authorization": `Bearer ${SUPABASE_ANON}`
          }
        });

        if (resp.ok) {
          const data = await resp.json();
          if (Array.isArray(data) && data.length > 0) {
            const user = data[0];
            resultBox.style.background = "rgba(76, 224, 210, 0.15)";
            resultBox.style.color = "#4ce0d2";
            resultBox.innerHTML = `<strong>✓ Верифіковано в Supabase!</strong><br>Відзнака: ${user.title || "Miogram Community ໒꒱"}<br>Причина: ${user.obtained_reason || "Верифікований учасник"}<br>Дата: ${user.obtained_at ? new Date(user.obtained_at).toLocaleDateString() : "Нещодавно"}`;
          } else {
            resultBox.style.background = "rgba(255, 92, 151, 0.15)";
            resultBox.style.color = "#ff5c97";
            resultBox.innerHTML = `<strong>ID ${val} не знайдено в базі.</strong><br>Відзнака призначається автоматично при вході в клієнт Miogram ໒꒱.`;
          }
        } else {
          resultBox.style.color = "#ff5c97";
          resultBox.innerText = "Помилка підключення до бази Supabase.";
        }
      } catch (err) {
        resultBox.style.color = "#ff5c97";
        resultBox.innerText = "Помилка мережі при перевірці.";
      }
    });
  }
}

// Boot application when DOM is ready
document.addEventListener("DOMContentLoaded", () => {
  window.miogramApp = new MiogramApp();
});
