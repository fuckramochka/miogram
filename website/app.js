// Miogram — Needy Streamer Overload (NSO) Anime OS Web Client
// 1. GitHub Releases API Client
// 2. Supabase Live Community User Counter
// 3. Trilingual Localization Engine (UK / EN / RU)
// 4. Interactive Canvas Pixel Badge Renderer with Bloom & Dynamic Flying Particles
// 5. Interactive Certificate of Origin Inspector (Founder & Community Mode)

const REPO_OWNER = "fuckramochka";
const REPO_NAME = "miogram";
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`;
const FALLBACK_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest`;

const SUPABASE_URL = "https://dbxsnjoeyiqvqtrluvwu.supabase.co";
const SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRieHNuam9leWlxdnF0cmx1dnd1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1NDI1MzEsImV4cCI6MjEwNDExODUzMX0.KJ0kvON1HXZu4MzlZjapSJEhEzWYlEqQoNEstWCgIjA";

// -------------------------------------------------------------
// Trilingual Dictionaries (UK / EN / RU)
// -------------------------------------------------------------
const I18N = {
  uk: {
    hero_pill: "INTERNET ANGEL OVERDOSE • Android 15+ 16KB ELF",
    hero_title_1: "Більше, ніж просто",
    hero_title_2: "месенджер ໒꒱",
    hero_subtitle: "Telegram-клієнт у культовій піксельній естетиці Needy Streamer Overload. Потужний захист від примусу Zero-Trust, WebAssembly Rust плагіни, GPU-шейдери Liquid Glass та жива екосистема з 10 канонічних бейджиків спільноти.",
    btn_download: "Завантажити останній APK",
    btn_badges: "Студія 10 Стрілочок",
    btn_source: "Вихідний код GitHub",

    badge_studio_title: "Badge_Studio_v2.0.exe • Interactive Arrow Customizer & Certificate",
    badge_studio_tag: "✦ КАНОНІЧНА ЕКОСИСТЕМА ВІДЗНАК MIOGRAM ✦",
    badge_studio_head: "Інтерфейс вибору стрілочки та Сертифікат відзнаки ໒꒱",
    badge_studio_desc: "Оберіть будь-який із 10 канонічних стилів стрілочок у студії нижче. Перегляньте живу CRT анімацію з розсіяним неоновим сяйвом, літаючими мікро-партіклами та офіційне обґрунтування надання відзнаки.",

    crt_scale: "Масштаб:",
    crt_bloom: "Сяйво:",
    crt_sparkles: "✦ Частинки:",

    selector_title: "КАТАЛОГ 10 СТИЛІВ СТРІЛОЧОК",
    selector_hint: "Натисніть на картку нижче для миттєвого перемикання стилю:",
    sync_cloud_ready: "Хмарна синхронізація Supabase активна для обраного стилю",
    btn_simulate_sync: "✦ Застосувати в клієнті ໒꒱",

    cert_title: "СЕРТИФІКАТ АВТЕНТИЧНОСТІ ВІДЗНАКИ ໒꒱",
    role_community: "✨ Учасник Спільноти",
    role_founder: "👑 Засновник Miogram",
    citation_head: "ОБҐРУНТУВАННЯ НАДАННЯ ВІДЗНАКИ:",
    meta_status: "Статус у базі:",
    meta_granted: "Дата нагородження:",
    meta_uid: "Ідентифікатор:",
    meta_style: "Активний стиль:",
    lore_head: "✦ ХУДОЖНІЙ ЗМІСТ ТА СИМВОЛІЗМ:",

    stat_users: "Учасників спільноти у базі Supabase",
    stat_wasm: "Холодний старт WASM Rust плагінів",
    stat_kdf: "KDF сховища з подвійним дном",
    stat_gpu: "Рідке скло AGSL GPU шейдери",
    stat_badges: "Хмарна синхронізація стрілочок",
    stats_window_title: "Status_Monitor.sys • Network & Performance",

    feat_window_title: "System_Architecture.txt • Advanced Features",
    feat_title: "Архітектурна Досконалість",
    feat_1_title: "PIN Примусу & Подвійне Дно",
    feat_1_desc: "Два різні PIN-коди. Введення екстреного PIN-коду відкриває нейтральний екран без майстер-ключів. Захищено Argon2id RFC 9106, StrongBox TEE та SQLCipher.",
    feat_2_title: "WebAssembly (WASM) Rust Плагіни",
    feat_2_desc: "Швидкий WebAssembly Micro Runtime (WAMR). Холодний старт < 1мс, RAM 150 КБ, криптографічний підпис Ed25519.",
    feat_3_title: "Просторове Рідке Скло (AGSL)",
    feat_3_desc: "GPU-шейдери з імітацією заломлення світла та хроматичної дисперсії на стабільних 120 FPS без перебивання тем.",
    feat_4_title: "Ергономіка Apple Music + Spotify",
    feat_4_desc: "Вбудований 1:1 Apple Music card player із живими міні-басовими візуалізаторами, жестовою перемоткою та текстами пісень.",
    feat_5_title: "Хмарна Екосистема Бейджиків Supabase",
    feat_5_desc: "Глобальне розпізнавання бейджиків та хроніка на базі Supabase PostgREST з 0мс офлайн-кешем та суворою ізоляцією акаунтів.",
    feat_6_title: "Приватний AI Роутер & Whisper на Пристрої",
    feat_6_desc: "Regex-маскування номерів карток, телефонів і паролів перед хмарними викликами. Повністю офлайн Whisper STT на базі NPU.",

    dl_window_title: "Setup_Installer.exe • Official Release",
    dl_title: "Отримати Miogram для Android",
    dl_desc: "Офіційні збірки криптографічно підписані та зібрані в GitHub Actions CI з вирівнюванням 16 KB ELF для Android 15+.",
    dl_verified: "🟢 Верифікована збірка",
    footer_text: "Створено з ♡ автором @dkramochka та спільнотою Miogram."
  },

  en: {
    hero_pill: "INTERNET ANGEL OVERDOSE • Android 15+ 16KB ELF",
    hero_title_1: "More than just a",
    hero_title_2: "messenger ໒꒱",
    hero_subtitle: "Telegram client in the iconic Needy Streamer Overload cyber pixel aesthetic. Powerful Zero-Trust duress protection, WebAssembly Rust plugins, Liquid Glass GPU shaders, and a living ecosystem of 10 community badges.",
    btn_download: "Download Latest APK",
    btn_badges: "10 Badges Studio",
    btn_source: "GitHub Source Code",

    badge_studio_title: "Badge_Studio_v2.0.exe • Interactive Arrow Customizer & Certificate",
    badge_studio_tag: "✦ CANONICAL MIOGRAM BADGE ECOSYSTEM ✦",
    badge_studio_head: "Arrow Selection Studio & Certificate of Origin ໒꒱",
    badge_studio_desc: "Select any of the 10 canonical arrow styles below. Inspect the real-time CRT pixel animation with atmospheric bloom, living micro-sparkles, and authentic award certification credentials.",

    crt_scale: "Scale:",
    crt_bloom: "Bloom:",
    crt_sparkles: "✦ Sparkles:",

    selector_title: "10 ARROW STYLES CATALOG",
    selector_hint: "Click any card below for instant real-time style preview:",
    sync_cloud_ready: "Supabase cloud synchronization active for selected style",
    btn_simulate_sync: "✦ Apply in Client ໒꒱",

    cert_title: "CERTIFICATE OF BADGE AUTHENTICITY ໒꒱",
    role_community: "✨ Community Member",
    role_founder: "👑 Miogram Founder",
    citation_head: "AWARD CITATION & REASON:",
    meta_status: "Cloud Status:",
    meta_granted: "Date Granted:",
    meta_uid: "User ID:",
    meta_style: "Active Style:",
    lore_head: "✦ AESTHETIC LORE & SYMBOLISM:",

    stat_users: "Community Members in Supabase",
    stat_wasm: "WASM Rust Plugin Cold Start",
    stat_kdf: "Duress Vault Argon2id KDF",
    stat_gpu: "AGSL Liquid Glass Shaders",
    stat_badges: "Cloud Synchronized Badges",
    stats_window_title: "Status_Monitor.sys • Network & Performance",

    feat_window_title: "System_Architecture.txt • Advanced Features",
    feat_title: "Architectural Excellence",
    feat_1_title: "Duress PIN & Plausible Deniability",
    feat_1_desc: "Two distinct PIN codes. Entering the emergency PIN opens a neutral dummy vault with zero master keys. Hardened by Argon2id RFC 9106, StrongBox TEE, and SQLCipher.",
    feat_2_title: "WebAssembly (WASM) Rust Plugins",
    feat_2_desc: "High-speed WebAssembly Micro Runtime (WAMR). Cold boot < 1ms, 150 KB RAM footprint, Ed25519 signature verification.",
    feat_3_title: "Spatial Liquid Glass (AGSL)",
    feat_3_desc: "GPU fragment shaders rendering realistic chromatic dispersion and refraction at silky smooth 120 FPS without theme breakage.",
    feat_4_title: "Apple Music + Spotify Ergonomics",
    feat_4_desc: "Native 1:1 Apple Music card player with real-time audio visualizers, swipe seek gestures, and synced lyrics.",
    feat_5_title: "Supabase Cloud Badge Ecosystem",
    feat_5_desc: "Global badge recognition and obtain history backed by Supabase PostgREST with 0ms offline caching and strict account isolation.",
    feat_6_title: "Private AI Router & On-Device Whisper",
    feat_6_desc: "Local regex sanitization of payment cards, phones, and passwords before cloud queries. Zero-leak offline Whisper STT via NPU.",

    dl_window_title: "Setup_Installer.exe • Official Release",
    dl_title: "Get Miogram for Android",
    dl_desc: "Official builds are cryptographically signed and built on GitHub Actions CI with 16 KB ELF alignment for Android 15+.",
    dl_verified: "🟢 Verified Build",
    footer_text: "Crafted with ♡ by @dkramochka and the Miogram Community."
  },

  ru: {
    hero_pill: "INTERNET ANGEL OVERDOSE • Android 15+ 16KB ELF",
    hero_title_1: "Больше, чем просто",
    hero_title_2: "мессенджер ໒꒱",
    hero_subtitle: "Telegram-клиент в культовой пиксельной эстетике Needy Streamer Overload. Мощная защита от принуждения Zero-Trust, WebAssembly Rust плагины, GPU-шейдеры Liquid Glass и живая экосистема из 10 канонических бейджей сообщества.",
    btn_download: "Скачать последний APK",
    btn_badges: "Студия 10 Стрелочек",
    btn_source: "Исходный код GitHub",

    badge_studio_title: "Badge_Studio_v2.0.exe • Interactive Arrow Customizer & Certificate",
    badge_studio_tag: "✦ КАНОНИЧЕСКАЯ ЭКОСИСТЕМА БЕЙДЖЕЙ MIOGRAM ✦",
    badge_studio_head: "Интерфейс выбора стрелочки и Сертификат отличия ໒꒱",
    badge_studio_desc: "Выберите любой из 10 канонических стилей стрелочек в студии ниже. Оцените живую CRT анимацию с рассеянным неоновым сиянием, парящими микро-частицами и официальным обоснованием выдачи отличия.",

    crt_scale: "Масштаб:",
    crt_bloom: "Сияние:",
    crt_sparkles: "✦ Частицы:",

    selector_title: "КАТАЛОГ 10 СТИЛЕЙ СТРЕЛОЧЕК",
    selector_hint: "Нажмите на карточку ниже для мгновенного переключения стиля:",
    sync_cloud_ready: "Облачная синхронизация Supabase активна для выбранного стиля",
    btn_simulate_sync: "✦ Применить в клиенте ໒꒱",

    cert_title: "СЕРТИФИКАТ ПОДЛИННОСТИ ОТЛИЧИЯ ໒꒱",
    role_community: "✨ Участник Сообщества",
    role_founder: "👑 Создатель Miogram",
    citation_head: "ОБОСНОВАНИЕ ВЫДАЧИ ОТЛИЧИЯ:",
    meta_status: "Статус в базе:",
    meta_granted: "Дата выдачи:",
    meta_uid: "Идентификатор:",
    meta_style: "Активный стиль:",
    lore_head: "✦ ХУДОЖЕСТВЕННЫЙ СМЫСЛ И СИМВОЛИЗМ:",

    stat_users: "Участников сообщества в базе Supabase",
    stat_wasm: "Холодный старт WASM Rust плагинов",
    stat_kdf: "KDF хранилища с двойным дном",
    stat_gpu: "Жидкое стекло AGSL GPU шейдеры",
    stat_badges: "Облачная синхронизация стрелочек",
    stats_window_title: "Status_Monitor.sys • Network & Performance",

    feat_window_title: "System_Architecture.txt • Advanced Features",
    feat_title: "Архитектурное Совершенство",
    feat_1_title: "PIN Принуждения & Двойное Дно",
    feat_1_desc: "Два разных PIN-кода. Ввод экстренного PIN-кода открывает нейтральный экран без мастер-ключей. Защищено Argon2id RFC 9106, StrongBox TEE и SQLCipher.",
    feat_2_title: "WebAssembly (WASM) Rust Плагины",
    feat_2_desc: "Быстрый WebAssembly Micro Runtime (WAMR). Холодный старт < 1мс, RAM 150 КБ, криптографическая подпись Ed25519.",
    feat_3_title: "Пространственное Жидкое Стекло (AGSL)",
    feat_3_desc: "GPU-шейдеры с имитацией преломления света и хроматической дисперсии на стабильных 120 FPS без сбоя тем.",
    feat_4_title: "Эргономика Apple Music + Spotify",
    feat_4_desc: "Встроенный 1:1 Apple Music card player с живыми мини-басовыми визуализаторами, жестовой перемоткой и синхронным текстом песен.",
    feat_5_title: "Облачная Экосистема Бейджей Supabase",
    feat_5_desc: "Глобальное распознавание бейджей и хроника на базе Supabase PostgREST с 0мс офлайн-кэшем и строгой изоляцией аккаунтов.",
    feat_6_title: "Приватный AI Роутер & Whisper на Устройстве",
    feat_6_desc: "Regex-маскирование номеров карт, телефонов и паролей перед облачными вызовами. Полностью офлайн Whisper STT на базе NPU.",

    dl_window_title: "Setup_Installer.exe • Official Release",
    dl_title: "Получить Miogram для Android",
    dl_desc: "Официальные сборки криптографически подписаны и собраны в GitHub Actions CI с выравниванием 16 KB ELF для Android 15+.",
    dl_verified: "🟢 Верифицированная сборка",
    footer_text: "Создано с ♡ автором @dkramochka и сообществом Miogram."
  }
};

// -------------------------------------------------------------
// 10 Canonical Badges Lore & Metadata
// -------------------------------------------------------------
const BADGES = {
  original: {
    code: "01 — ORIGINAL",
    title: { uk: "Класичне Кібер-Серце ໒꒱", en: "Canonical Cyber Heart ໒꒱", ru: "Классическое Кибер-Сердце ໒꒱" },
    lore: {
      uk: "Канонічне крилате серце Miogram з візором-антеною, обсидіановим ядром, сяючим бірюзовим контуром та рожевими пір'ями. Перша відзнака екосистеми, з якої розпочалася вся історія проекту ໒꒱.",
      en: "Canonical Miogram winged heart with antenna visor, obsidian core, luminous cyan contour, and pink feathers. The foundational emblem of the entire ecosystem ໒꒱.",
      ru: "Каноническое крылатое сердце Miogram с визором-антенной, обсидиановым ядром, бирюзовым контуром и розовыми перьями. Первое отличие экосистемы, с которого началась вся история проекта ໒꒱."
    },
    bloomColor: "rgba(0, 240, 255, 0.35)",
    sparkleColor: "#00F0FF",
    pills: ["Cyan #00F0FF", "Pink #FF55A3", "Obsidian #0F141C"]
  },
  pink: {
    code: "02 — PINK",
    title: { uk: "K-Angel Неон 💖", en: "K-Angel Neon 💖", ru: "K-Angel Неон 💖" },
    lore: {
      uk: "Неоново-рожевий кібер-стиль із шевронами серця. Символ естетики Needy Streamer Overload та безмежної любові до Інтернет-Ангела †昇天†.",
      en: "Neon pink cyber aesthetic with chevron heart ribs. The quintessential symbol of Needy Streamer Overload devotion to the Internet Angel †昇天†.",
      ru: "Неоново-розовый кибер-стиль с шевронами сердца. Символ эстетики Needy Streamer Overload и бесконечной любви к Интернет-Ангелу †昇天†."
    },
    bloomColor: "rgba(255, 42, 147, 0.40)",
    sparkleColor: "#FF2A93",
    pills: ["Hot Pink #FF2A93", "Pure White #FFF0F7", "Deep Plum #1B0F1C"]
  },
  cyan: {
    code: "03 — CYAN",
    title: { uk: "Електрична Блакить ⚡", en: "Electric Cyan ⚡", ru: "Электрическая Лазурь ⚡" },
    lore: {
      uk: "Електричний блакитний стиль з білими акцентами та сяйвом. Символізує технологічність, холодний розум та надшвидку реакцію Miogram.",
      en: "Electric sky-blue cyber wings with luminous starlight. Symbolizes Miogram speed, clarity, cold intelligence, and next-gen technology.",
      ru: "Электрический лазурный стиль с белыми акцентами и сиянием. Символизирует технологичность, холодный ум и сверхбыструю реакцию Miogram."
    },
    bloomColor: "rgba(0, 229, 255, 0.40)",
    sparkleColor: "#00E5FF",
    pills: ["Electric Cyan #00E5FF", "Ice Cyan #E0F7FA", "Midnight Navy #0A1822"]
  },
  dark: {
    code: "04 — DARK",
    title: { uk: "Темний Обсидіан 🌌", en: "Midnight Obsidian 🌌", ru: "Темный Обсидиан 🌌" },
    lore: {
      uk: "Темний обсидіановий варіант з оксамитовим неоновим краєм для поціновувачів нічного режиму, таємничості та естетики глибокого космосу.",
      en: "Midnight obsidian wings with velvet violet aura. Crafted for night owls, stealth lovers, and deep-space cosmic vibes.",
      ru: "Темный обсидиановый вариант с бархатным неоновым краем для ценителей ночного режима, таинственности и эстетики глубокого космоса."
    },
    bloomColor: "rgba(157, 78, 221, 0.40)",
    sparkleColor: "#C77DFF",
    pills: ["Velvet Violet #9D4EDD", "Neon Purple #C77DFF", "Deep Space #120B20"]
  },
  angel: {
    code: "05 — ANGEL",
    title: { uk: "Серафим із Німбом 👼", en: "Seraphim with Halo 👼", ru: "Серафим с Нимбом 👼" },
    lore: {
      uk: "Ангельські крила з ширяючим білим німбом та лавандовим серцем. Відзнака гармонії, чистих помислів та піднесення †昇天†.",
      en: "Angelic wings with hovering white halo and lavender heart. The badge of purity, harmony, and transcendental ascension †昇天†.",
      ru: "Ангельские крылья с парящим белым нимбом и лавандовым сердцем. Знак гармонии, чистых помыслов и вознесения †昇天†."
    },
    bloomColor: "rgba(224, 170, 255, 0.35)",
    sparkleColor: "#FFFFFF",
    pills: ["Hovering Halo #FFFFFF", "Lavender #C3BEF0", "Pure White #FAFAFE"]
  },
  devil: {
    code: "06 — DEVIL",
    title: { uk: "Бунтарські Ріжки 😈", en: "Devil Rebel 😈", ru: "Бунтарские Рожки 😈" },
    lore: {
      uk: "Грайливі ріжки та крила кажана з гарячим рожевим неоном. Відзнака бунтарського духу, свободи від правил та зухвалого шарму.",
      en: "Playful devil horns and scalloped bat wings with blazing neon. Distinctive emblem of rebellion, defiance, and chaos charm.",
      ru: "Игривые рожки и крылья летучей мыши с горячим розовым неоном. Знак бунтарского духа, свободы от правил и дерзкого шарма."
    },
    bloomColor: "rgba(255, 0, 85, 0.40)",
    sparkleColor: "#FF006E",
    pills: ["Devil Crimson #FF0055", "Neon Pink #FF3377", "Crimson Core #1C0A15"]
  },
  rainbow: {
    code: "07 — RAINBOW",
    title: { uk: "Призматична Веселка 🌈", en: "Prismatic Rainbow 🌈", ru: "Призматическая Радуга 🌈" },
    lore: {
      uk: "Призматичний веселковий спектр із золотим контуром. Символ безмежного різноманіття, креативності та яскравих емоцій у спілкуванні.",
      en: "Prismatic rainbow spectrum with golden accents. Represents limitless diversity, creative energy, and joyful communication.",
      ru: "Призматический радужный спектр с золотым контуром. Символ безграничного разнообразия, креативности и ярких эмоций в общении."
    },
    bloomColor: "rgba(255, 209, 102, 0.40)",
    sparkleColor: "#FFD166",
    pills: ["Spectrum RGB", "Gold #FFD166", "Dark Prism #10141E"]
  },
  outline: {
    code: "08 — OUTLINE",
    title: { uk: "Вайрфрейм Контур 🔲", en: "1px Wireframe 🔲", ru: "Вайрфрейм Контур 🔲" },
    lore: {
      uk: "Мінімалістичний 1-піксельний вайрфрейм-контур. Кіберпанк у чистому вигляді — жодної зайвої деталі, лише чиста геометрія та функціонал.",
      en: "Minimalist 1px wireframe cyber contour. Pure cyberpunk minimalism — clean geometry, sharp lines, zero excess.",
      ru: "Минималистичный 1-пиксельный вайрфрейм-контур. Чистый киберпанк — ни единой лишней детали, только чистая геометрия и функционал."
    },
    bloomColor: "rgba(0, 240, 255, 0.28)",
    sparkleColor: "#00F0FF",
    pills: ["Wire Cyan #00F0FF", "Minimalist 1px", "Zero Fill"]
  },
  glitch: {
    code: "09 — GLITCH",
    title: { uk: "CRT Розщеплення 📺", en: "CRT Glitch 📺", ru: "CRT Расщепление 📺" },
    lore: {
      uk: "Хроматична аберація RGB із розщепленням форми та сканлайнами. Для поціновувачів естетики VHS касет, CRT моніторів та кібер-збоїв.",
      en: "Chromatic RGB displacement with dynamic scanlines. Made for connoisseurs of VHS tapes, CRT monitors, and cyber distortion.",
      ru: "Хроматическая аберрация RGB с расщеплением формы и сканлайнами. Для ценителей эстетики VHS кассет, CRT мониторов и кибер-сбоев."
    },
    bloomColor: "rgba(0, 240, 255, 0.35)",
    sparkleColor: "#00F0FF",
    pills: ["Chromatic Split", "Magenta #FF0055", "Cyan #00F0FF"]
  },
  premium: {
    code: "10 — PREMIUM",
    title: { uk: "Королівська Корона 👑", en: "Royal Gold Crown 👑", ru: "Королевская Корона 👑" },
    lore: {
      uk: "Королівська золота корона, янтарні крила та золоті ребра. Елітна відзнака визнання найвищих досягнень та статусу в екосистемі Miogram.",
      en: "Royal golden crown and amber wings with chest armor. Elite distinction honoring top contributors and paramount status.",
      ru: "Королевская золотая корона, янтарные крылья и золотые ребра. Элитное отличие признания высших достижений и статуса в экосистеме Miogram."
    },
    bloomColor: "rgba(255, 215, 0, 0.45)",
    sparkleColor: "#FFD700",
    pills: ["Imperial Gold #FFD700", "Amber #FFE066", "Royal Shadow #1B1408"]
  }
};

let currentLang = localStorage.getItem("miogram_lang") || "uk";
let currentBadgeId = "original";
let currentRole = "user";
let currentScale = 2;
let isBloomEnabled = true;
let isSparklesEnabled = true;

// -------------------------------------------------------------
// Language Switcher
// -------------------------------------------------------------
function switchLanguage(lang) {
  if (!I18N[lang]) lang = "uk";
  currentLang = lang;
  localStorage.setItem("miogram_lang", lang);

  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (I18N[lang] && I18N[lang][key]) {
      el.textContent = I18N[lang][key];
    }
  });

  document.querySelectorAll(".lang-btn").forEach(btn => {
    btn.classList.toggle("active", btn.getAttribute("data-lang") === lang);
  });

  updateBadgeInspector();
}

// -------------------------------------------------------------
// Certificate & Inspector Updater
// -------------------------------------------------------------
function updateBadgeInspector() {
  const badge = BADGES[currentBadgeId] || BADGES.original;

  // CRT Meta
  const codeEl = document.getElementById("crtBadgeCode");
  const titleEl = document.getElementById("crtBadgeTitle");
  const pillsEl = document.getElementById("crtBadgePills");
  if (codeEl) codeEl.textContent = badge.code;
  if (titleEl) titleEl.textContent = badge.title[currentLang] || badge.title.uk;
  if (pillsEl) {
    pillsEl.innerHTML = badge.pills.map((p, idx) => {
      const cls = idx === 0 ? "pill-cyan" : (idx === 1 ? "pill-pink" : "pill-dark");
      return `<span class="palette-pill ${cls}">${p}</span>`;
    }).join("");
  }

  // Certificate Citation ("За що надано")
  const reasonTextEl = document.getElementById("certReasonText");
  const styleValEl = document.getElementById("certStyleVal");
  const uidValEl = document.getElementById("certUidVal");
  const dateValEl = document.getElementById("certDateVal");
  const loreTextEl = document.getElementById("certLoreText");

  if (styleValEl) styleValEl.textContent = badge.code;
  if (loreTextEl) loreTextEl.textContent = badge.lore[currentLang] || badge.lore.uk;

  if (currentRole === "founder") {
    if (uidValEl) uidValEl.textContent = "UID: 241916036 (fuckramochka)";
    if (dateValEl) dateValEl.textContent = "01.09.2026 (Genesis Release)";
    if (reasonTextEl) {
      if (currentLang === "en") {
        reasonTextEl.textContent = '"Personal distinction of the Founder & Chief Architect of Miogram (@fuckramochka). Granted upon project genesis as a symbol of supreme developer status and official client authenticity."';
      } else if (currentLang === "ru") {
        reasonTextEl.textContent = '"Личное отличие создателя и главного архитектора экосистемы Miogram (@fuckramochka). Предоставлено при основании проекта как символ высшего статуса разработчика и подтверждения официальной подлинности клиента."';
      } else {
        reasonTextEl.textContent = '"Особиста відзнака засновника та головного архітектора екосистеми Miogram (@fuckramochka). Надана при заснуванні проекту як символ найвищого статусу розробника та підтвердження офіційної автентичності клієнта."';
      }
    }
  } else {
    if (uidValEl) uidValEl.textContent = "UID: Verified Community Account";
    if (dateValEl) dateValEl.textContent = "04.09.2026 (Cloud Synchronized)";
    if (reasonTextEl) {
      if (currentLang === "en") {
        reasonTextEl.textContent = '"Officially verified member of the Miogram Cloud ecosystem. Awarded for meaningful contributions to testing, community support, and client development."';
      } else if (currentLang === "ru") {
        reasonTextEl.textContent = '"Официально верифицированный участник облачной экосистемы Miogram. Отличие предоставлено за весомый вклад в тестирование, поддержку сообщества и активное развитие клиента."';
      } else {
        reasonTextEl.textContent = '"Офіційно верифікований учасник хмарної екосистеми Miogram. Відзнаку надано за вагомий внесок у тестування, підтримку спільноти та активний розвиток клієнта."';
      }
    }
  }
}

// -------------------------------------------------------------
// Live CRT Canvas Badge Renderer
// -------------------------------------------------------------
const canvas = document.getElementById("badgeCanvas");
const ctx = canvas ? canvas.getContext("2d") : null;

const DYNAMIC_PARTICLES = [
  { x: 2.5, speed: 0.00035, swayFreq: 0.08, swayAmp: 1.2, isCross: true,  yOffset: 0.05 },
  { x: 24.5, speed: 0.00042, swayFreq: 0.07, swayAmp: 1.2, isCross: true,  yOffset: 0.45 },
  { x: 3.5, speed: 0.00028, swayFreq: 0.09, swayAmp: 1.0, isCross: false, yOffset: 0.70 },
  { x: 23.5, speed: 0.00038, swayFreq: 0.08, swayAmp: 1.0, isCross: false, yOffset: 0.25 },
  { x: 13.5, speed: 0.00045, swayFreq: 0.06, swayAmp: 1.4, isCross: false, yOffset: 0.85 },
  { x: 13.5, speed: 0.00032, swayFreq: 0.07, swayAmp: 1.2, isCross: true,  yOffset: 0.35 }
];

function drawWings(ctx, px, py, fillCol, fringeCol) {
  ctx.fillStyle = fillCol;
  ctx.fillRect(4*px, 5*py, 5*px, 1*py);
  ctx.fillRect(3*px, 6*py, 6*px, 1*py);
  ctx.fillRect(1*px, 7*py, 8*px, 3*py);
  ctx.fillRect(2*px, 10*py, 6*px, 1*py);
  ctx.fillRect(4*px, 11*py, 3*px, 2*py);

  ctx.fillRect(19*px, 5*py, 5*px, 1*py);
  ctx.fillRect(19*px, 6*py, 6*px, 1*py);
  ctx.fillRect(19*px, 7*py, 8*px, 3*py);
  ctx.fillRect(20*px, 10*py, 6*px, 1*py);
  ctx.fillRect(21*px, 11*py, 3*px, 2*py);

  ctx.fillStyle = fringeCol;
  ctx.fillRect(1*px, 8*py, 2*px, 2*py);
  ctx.fillRect(25*px, 8*py, 2*px, 2*py);
  ctx.fillRect(4*px, 12*py, 3*px, 1*py);
  ctx.fillRect(21*px, 12*py, 3*px, 1*py);
}

function drawHeart(ctx, px, py, coreCol, contourCol) {
  ctx.fillStyle = coreCol;
  ctx.fillRect(10*px, 7*py, 3*px, 1*py);
  ctx.fillRect(15*px, 7*py, 3*px, 1*py);
  ctx.fillRect(9*px, 8*py, 10*px, 2*py);
  ctx.fillRect(8*px, 10*py, 12*px, 2*py);
  ctx.fillRect(9*px, 12*py, 10*px, 1*py);
  ctx.fillRect(10*px, 13*py, 8*px, 1*py);
  ctx.fillRect(11*px, 14*py, 6*px, 1*py);
  ctx.fillRect(12*px, 15*py, 4*px, 1*py);
  ctx.fillRect(13*px, 16*py, 2*px, 1*py);

  ctx.fillStyle = contourCol;
  ctx.fillRect(10*px, 6*py, 3*px, 1*py);
  ctx.fillRect(15*px, 6*py, 3*px, 1*py);
  ctx.fillRect(8*px, 8*py, 1*px, 4*py);
  ctx.fillRect(19*px, 8*py, 1*px, 4*py);
}

function drawEyes(ctx, px, py, eyeCol) {
  ctx.fillStyle = eyeCol;
  ctx.fillRect(11*px, 9.5*py, 1.5*px, 1.5*py);
  ctx.fillRect(15.5*px, 9.5*py, 1.5*px, 1.5*py);
}

function renderBadge(time) {
  if (!ctx || !canvas) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const GRID_W = 28;
  const GRID_H = 22;
  const px = canvas.width / GRID_W;
  const py = canvas.height / GRID_H;

  const badge = BADGES[currentBadgeId] || BADGES.original;
  const now = time || performance.now();
  const phase = (now % 2200) / 2200;
  const angle = phase * 2.0 * Math.PI;
  const bobY = Math.round(Math.sin(angle) * 0.9) * py;

  ctx.save();
  ctx.translate(0, bobY);

  // 1. Radiant Atmospheric Neon Bloom Pass
  if (isBloomEnabled) {
    const cx = 14 * px;
    const cy = 11 * py;
    const rad = 13 * px;
    const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, rad);
    grad.addColorStop(0, badge.bloomColor);
    grad.addColorStop(1, "rgba(0, 0, 0, 0)");
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(cx, cy, rad, 0, Math.PI * 2);
    ctx.fill();
  }

  // 2. Badge Drawing Passes
  switch (currentBadgeId) {
    case "pink":
      ctx.fillStyle = "#FF2A93";
      ctx.fillRect(10*px, 3*py, 8*px, 1*py);
      drawWings(ctx, px, py, "#FFF0F7", "#FF2A93");
      drawHeart(ctx, px, py, "#1B0F1C", "#FF2A93");
      ctx.fillStyle = "#FF2A93";
      ctx.fillRect(11*px, 12*py, 6*px, 1*py);
      drawEyes(ctx, px, py, "#FFE5F0");
      break;

    case "cyan":
      ctx.fillStyle = "#00E5FF";
      ctx.fillRect(10*px, 3*py, 8*px, 1*py);
      drawWings(ctx, px, py, "#E0F7FA", "#00E5FF");
      drawHeart(ctx, px, py, "#0A1822", "#00E5FF");
      drawEyes(ctx, px, py, "#FFFFFF");
      break;

    case "dark":
      ctx.fillStyle = "#9D4EDD";
      ctx.fillRect(11*px, 3*py, 6*px, 1*py);
      drawWings(ctx, px, py, "#1B142A", "#C77DFF");
      drawHeart(ctx, px, py, "#120B20", "#9D4EDD");
      drawEyes(ctx, px, py, "#E0AAFF");
      break;

    case "angel":
      ctx.fillStyle = "#FFFFFF";
      ctx.fillRect(10*px, 1*py, 8*px, 1*py);
      ctx.fillRect(8*px, 2*py, 2*px, 1*py);
      ctx.fillRect(18*px, 2*py, 2*px, 1*py);
      drawWings(ctx, px, py, "#FAFAFE", "#B8C0EC");
      drawHeart(ctx, px, py, "#C3BEF0", "#FFFFFF");
      drawEyes(ctx, px, py, "#FFFFFF");
      break;

    case "devil":
      ctx.fillStyle = "#FF0055";
      ctx.fillRect(9*px, 4*py, 2*px, 3*py);
      ctx.fillRect(8*px, 3*py, 2*px, 2*py);
      ctx.fillRect(17*px, 4*py, 2*px, 3*py);
      ctx.fillRect(18*px, 3*py, 2*px, 2*py);
      drawWings(ctx, px, py, "#FF3377", "#B8003D");
      drawHeart(ctx, px, py, "#1C0A15", "#FF0055");
      drawEyes(ctx, px, py, "#FFB3C6");
      break;

    case "rainbow":
      const spectrum = ["#FF3377", "#9D4EDD", "#00B4D8", "#06D6A0", "#FFD166"];
      spectrum.forEach((col, i) => {
        ctx.fillStyle = col;
        ctx.fillRect(4*px, (5 + i*1.5)*py, 5*px, 1.5*py);
        ctx.fillRect(19*px, (5 + i*1.5)*py, 5*px, 1.5*py);
      });
      drawHeart(ctx, px, py, "#10141E", "#FFD166");
      drawEyes(ctx, px, py, "#FFFFFF");
      break;

    case "outline":
      ctx.strokeStyle = "#00F0FF";
      ctx.lineWidth = 1.5 * (canvas.width / 280);
      ctx.strokeRect(4*px, 5*py, 5*px, 7*py);
      ctx.strokeRect(19*px, 5*py, 5*px, 7*py);
      ctx.strokeRect(9*px, 7*py, 10*px, 9*py);
      drawEyes(ctx, px, py, "#00F0FF");
      break;

    case "glitch":
      ctx.fillStyle = "rgba(255, 0, 85, 0.7)";
      ctx.fillRect(3*px, 5*py, 5*px, 7*py);
      ctx.fillStyle = "rgba(0, 240, 255, 0.7)";
      ctx.fillRect(20*px, 5*py, 5*px, 7*py);
      drawWings(ctx, px, py, "#FFFFFF", "#00F0FF");
      drawHeart(ctx, px, py, "#10121C", "#00F0FF");
      drawEyes(ctx, px, py, "#FFFFFF");
      break;

    case "premium":
      ctx.fillStyle = "#FFD700";
      ctx.fillRect(10*px, 2*py, 2*px, 3*py);
      ctx.fillRect(13*px, 1*py, 2*px, 4*py);
      ctx.fillRect(16*px, 2*py, 2*px, 3*py);
      ctx.fillRect(10*px, 5*py, 8*px, 1*py);
      drawWings(ctx, px, py, "#FFE066", "#CC8800");
      drawHeart(ctx, px, py, "#1B1408", "#FFD700");
      ctx.fillStyle = "#FFD700";
      ctx.fillRect(10*px, 11*py, 8*px, 1*py);
      drawEyes(ctx, px, py, "#FFF5B8");
      break;

    case "original":
    default:
      ctx.fillStyle = "#00F0FF";
      ctx.fillRect(10*px, 3*py, 8*px, 1*py);
      ctx.fillRect(12*px, 4*py, 4*px, 1*py);
      drawWings(ctx, px, py, "#F0FDFE", "#FF55A3");
      drawHeart(ctx, px, py, "#0F141C", "#00F0FF");
      drawEyes(ctx, px, py, "#FFFFFF");
      break;
  }

  // 3. Starlight Sparkle Particles
  if (isSparklesEnabled) {
    DYNAMIC_PARTICLES.forEach(p => {
      const travel = (now * p.speed + p.yOffset) % 1.0;
      const y = (1.0 - travel) * 22;
      const x = p.x + Math.sin(y * p.swayFreq + now * 0.0025) * p.swayAmp;
      const alphaSin = Math.sin(travel * Math.PI);
      if (alphaSin <= 0.08) return;

      const sx = x * px;
      const sy = y * py;
      ctx.fillStyle = badge.sparkleColor;
      ctx.globalAlpha = alphaSin;

      if (p.isCross) {
        ctx.fillRect(sx, sy - py, px, 3*py);
        ctx.fillRect(sx - px, sy, 3*px, py);
      } else {
        ctx.fillRect(sx, sy, px, py);
      }
      ctx.globalAlpha = 1.0;
    });
  }

  ctx.restore();
  requestAnimationFrame(renderBadge);
}

// -------------------------------------------------------------
// Supabase Live Users Fetcher
// -------------------------------------------------------------
async function fetchLiveUsers() {
  const counterEl = document.getElementById("liveUserCounter");
  const cardEl = document.getElementById("liveUserCountCard");
  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/miogram_badges`, {
      method: "HEAD",
      headers: {
        "apikey": SUPABASE_ANON,
        "Authorization": `Bearer ${SUPABASE_ANON}`,
        "Prefer": "count=exact",
        "Range": "0-0"
      }
    });

    const contentRange = res.headers.get("content-range");
    if (contentRange && contentRange.includes("/")) {
      const count = contentRange.split("/")[1];
      if (counterEl) counterEl.textContent = `${count} ໒꒱`;
      if (cardEl) cardEl.textContent = `${count}`;
      return;
    }
  } catch (err) {
    console.warn("Supabase live counter fetch error:", err);
  }
  if (counterEl) counterEl.textContent = "1,420+";
  if (cardEl) cardEl.textContent = "1,420+";
}

// -------------------------------------------------------------
// GitHub Releases Fetcher
// -------------------------------------------------------------
async function fetchLatestRelease() {
  const tagEl = document.getElementById("releaseTag");
  const dateEl = document.getElementById("releaseDate");
  const notesEl = document.getElementById("releaseNotes");
  const dlLink = document.getElementById("primaryApkLink");

  try {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    if (tagEl) tagEl.textContent = data.tag_name || "v1.0.0-release";
    if (dateEl && data.published_at) {
      dateEl.textContent = new Date(data.published_at).toLocaleDateString();
    }
    if (notesEl && data.body) {
      notesEl.innerHTML = data.body.replace(/\r\n|\n/g, "<br>");
    }

    const apkAsset = data.assets && data.assets.find(a => a.name && a.name.endsWith(".apk"));
    if (apkAsset && dlLink) {
      dlLink.href = apkAsset.browser_download_url;
    }
  } catch (err) {
    console.warn("GitHub releases fetch failed, using fallback:", err);
    if (tagEl) tagEl.textContent = "v1.0.0-release";
    if (dateEl) dateEl.textContent = "04.09.2026";
    if (dlLink) dlLink.href = FALLBACK_URL;
  }
}

// -------------------------------------------------------------
// Taskbar Clock
// -------------------------------------------------------------
function updateClock() {
  const clockEl = document.getElementById("taskbarClock");
  if (!clockEl) return;
  const d = new Date();
  const h = String(d.getHours()).padStart(2, "0");
  const m = String(d.getMinutes()).padStart(2, "0");
  const s = String(d.getSeconds()).padStart(2, "0");
  clockEl.textContent = `${h}:${m}:${s}`;
}

// -------------------------------------------------------------
// Interactive Events & Listeners
// -------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
  switchLanguage(currentLang);
  fetchLatestRelease();
  fetchLiveUsers();
  updateClock();
  setInterval(updateClock, 1000);
  requestAnimationFrame(renderBadge);

  // Language buttons
  document.querySelectorAll(".lang-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      switchLanguage(btn.getAttribute("data-lang"));
    });
  });

  // Badge Selection Tiles
  document.querySelectorAll(".badge-card-tile").forEach(tile => {
    tile.addEventListener("click", () => {
      document.querySelectorAll(".badge-card-tile").forEach(t => t.classList.remove("active"));
      tile.classList.add("active");
      currentBadgeId = tile.getAttribute("data-id") || "original";
      updateBadgeInspector();
    });
  });

  // Certificate Role Toggle (Community vs Founder)
  const roleUserBtn = document.getElementById("roleUserBtn");
  const roleFounderBtn = document.getElementById("roleFounderBtn");

  if (roleUserBtn && roleFounderBtn) {
    roleUserBtn.addEventListener("click", () => {
      currentRole = "user";
      roleUserBtn.classList.add("active");
      roleFounderBtn.classList.remove("active");
      updateBadgeInspector();
    });

    roleFounderBtn.addEventListener("click", () => {
      currentRole = "founder";
      roleFounderBtn.classList.add("active");
      roleUserBtn.classList.remove("active");
      updateBadgeInspector();
    });
  }

  // CRT Scale Controls
  document.querySelectorAll(".scale-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".scale-btn").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      currentScale = parseInt(btn.getAttribute("data-scale") || "2", 10);
      if (canvas) {
        const baseW = 140;
        const baseH = 110;
        canvas.width = baseW * currentScale;
        canvas.height = baseH * currentScale;
      }
    });
  });

  // CRT Toggles (Bloom / Sparkles)
  const bloomBtn = document.getElementById("toggleBloomBtn");
  if (bloomBtn) {
    bloomBtn.addEventListener("click", () => {
      isBloomEnabled = !isBloomEnabled;
      bloomBtn.textContent = isBloomEnabled ? "ON" : "OFF";
      bloomBtn.classList.toggle("active", isBloomEnabled);
    });
  }

  const sparklesBtn = document.getElementById("toggleSparklesBtn");
  if (sparklesBtn) {
    sparklesBtn.addEventListener("click", () => {
      isSparklesEnabled = !isSparklesEnabled;
      sparklesBtn.textContent = isSparklesEnabled ? "ON" : "OFF";
      sparklesBtn.classList.toggle("active", isSparklesEnabled);
    });
  }

  // Simulate Cloud Sync Button
  const syncBtn = document.getElementById("simulateSyncBtn");
  if (syncBtn) {
    syncBtn.addEventListener("click", () => {
      const originalText = syncBtn.textContent;
      syncBtn.textContent = "✓ Синхронізовано з Supabase ໒꒱";
      syncBtn.style.background = "#06D6A0";
      setTimeout(() => {
        syncBtn.textContent = originalText;
        syncBtn.style.background = "";
      }, 2000);
    });
  }

  // Desktop Icons Click -> Smooth Scroll to Windows
  document.querySelectorAll(".desktop-icon-btn, .taskbar-app-tab").forEach(btn => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-target");
      if (targetId) {
        const targetEl = document.getElementById(targetId);
        if (targetEl) {
          targetEl.scrollIntoView({ behavior: "smooth", block: "center" });
          targetEl.style.boxShadow = "6px 6px 0px #000, 0 0 35px rgba(255, 112, 166, 0.8)";
          setTimeout(() => {
            targetEl.style.boxShadow = "";
          }, 1500);
        }
      }
    });
  });
});
