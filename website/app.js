// Miogram — Needy Streamer Overload (NSO) Anime OS Web Client
// 1. GitHub Releases API Client
// 2. Supabase Live Community User Counter
// 3. Trilingual Localization Engine (UK / EN / RU)
// 4. Interactive Canvas Pixel Badge Renderer with Bloom & Dynamic Flying Particles

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
    hero_pill: "INTERNET ANGEL OVERDOSE • Android 15+ Ready",
    hero_title_1: "Більше, ніж просто",
    hero_title_2: "месенджер ໒꒱",
    hero_subtitle: "Telegram-клієнт у культовій піксельній естетиці Needy Streamer Overload. Потужний захист від примусу Zero-Trust, WebAssembly Rust плагіни, GPU-шейдери Liquid Glass та жива екосистема з 10 бейджиків спільноти.",
    btn_download: "Завантажити останній APK",
    btn_source: "Вихідний код GitHub",
    stats_window_title: "Status_Monitor.sys • Network & Performance",
    stat_users: "Учасників спільноти",
    stat_wasm: "Запуск WASM плагінів",
    stat_kdf: "KDF сховища примусу",
    stat_gpu: "AGSL GPU шейдери",
    stat_badges: "Supabase хмарний синк",
    badge_window_title: "Badge_Gallery.exe • 10 Pixel Styles",
    badge_title: "10 Канонічних Піксельних Бейджиків",
    badge_desc: "Натхненні Needy Streamer Overload та ретро PC-98 піксель-артом. Атмосферне сяйво, спекулярні відблиски та живі мікро-партікли (✦).",
    feat_window_title: "System_Architecture.txt • Features",
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
    hero_pill: "INTERNET ANGEL OVERDOSE • Android 15+ Ready",
    hero_title_1: "More than just a",
    hero_title_2: "messenger ໒꒱",
    hero_subtitle: "Next-gen Telegram client styled in iconic Needy Streamer Overload cyber pixel aesthetic. Featuring Zero-Trust Duress Protection, WebAssembly Rust Plugins, Liquid Glass GPU Shaders, and a 10-badge community ecosystem.",
    btn_download: "Download Latest APK",
    btn_source: "GitHub Source Code",
    stats_window_title: "Status_Monitor.sys • Network & Performance",
    stat_users: "Community Members",
    stat_wasm: "WASM Plugin Boot",
    stat_kdf: "Duress Vault KDF",
    stat_gpu: "AGSL GPU Shaders",
    stat_badges: "Supabase Cloud Sync",
    badge_window_title: "Badge_Gallery.exe • 10 Pixel Styles",
    badge_title: "10 Canonical Pixel Badges",
    badge_desc: "Inspired by Needy Streamer Overload and retro PC-98 pixel art. Luminous neon contours, atmospheric bloom, specular shading, and dynamic starlight particles (✦).",
    feat_window_title: "System_Architecture.txt • Features",
    feat_title: "Architectural Excellence",
    feat_1_title: "Duress PIN & Double Bottom Vault",
    feat_1_desc: "Two distinct PINs. Entering your panic code loads a decoy screen without decrypting master keys. Protected with Argon2id RFC 9106, StrongBox TEE, and SQLCipher.",
    feat_2_title: "WebAssembly (WASM) Rust Plugins",
    feat_2_desc: "High-performance WebAssembly Micro Runtime (WAMR). Cold boot < 1ms, RAM footprint 150 KB, Ed25519 signature verification.",
    feat_3_title: "Spatial Liquid Glass (AGSL)",
    feat_3_desc: "Hardware-accelerated GPU shaders simulating light refraction and chromatic dispersion at 120 FPS without theme hijacking.",
    feat_4_title: "Apple Music + Spotify Ergonomics",
    feat_4_desc: "Built-in 1:1 Apple Music card player with live mini-bass visualizers, gesture scrubbing, and lyrics integration.",
    feat_5_title: "Supabase Cloud Badge Ecosystem",
    feat_5_desc: "Global badge resolution, lore histories, and acquisition records powered by Supabase PostgREST with 0ms offline caching.",
    feat_6_title: "Private AI Router & On-Device Whisper",
    feat_6_desc: "Automatic regex sanitization shields cards, phone numbers, and credentials before cloud calls. Offline Whisper STT on NPUs.",
    dl_window_title: "Setup_Installer.exe • Official Release",
    dl_title: "Get Miogram for Android",
    dl_desc: "Official builds are cryptographically signed and built directly from GitHub Actions CI with 16 KB ELF alignment for Android 15+.",
    dl_verified: "🟢 Verified Build",
    footer_text: "Built with ♡ by @dkramochka and the Miogram Community."
  },
  ru: {
    hero_pill: "INTERNET ANGEL OVERDOSE • Android 15+ Ready",
    hero_title_1: "Больше, чем просто",
    hero_title_2: "мессенджер ໒꒱",
    hero_subtitle: "Telegram-клиент в культовой пиксельной эстетике Needy Streamer Overload. Мощная защита от принуждения Zero-Trust, WebAssembly Rust плагины, GPU-шейдеры Liquid Glass и живая экосистема из 10 бейджиков сообщества.",
    btn_download: "Скачать последний APK",
    btn_source: "Исходный код GitHub",
    stats_window_title: "Status_Monitor.sys • Network & Performance",
    stat_users: "Участников сообщества",
    stat_wasm: "Запуск WASM плагинов",
    stat_kdf: "KDF хранилища сейфа",
    stat_gpu: "AGSL GPU шейдеры",
    stat_badges: "Supabase облачный синк",
    badge_window_title: "Badge_Gallery.exe • 10 Pixel Styles",
    badge_title: "10 Канонических Пиксельных Бейджиков",
    badge_desc: "Вдохновлены Needy Streamer Overload и ретро PC-98 пиксель-артом. Атмосферное свечение, спекулярные блики и живые микро-партиклы (✦).",
    feat_window_title: "System_Architecture.txt • Features",
    feat_title: "Архитектурное Совершенство",
    feat_1_title: "PIN Принуждения & Двойное Дно",
    feat_1_desc: "Два разных PIN-кода. Ввод тревожного PIN открывает нейтральный экран без мастер-ключей. Защищено Argon2id RFC 9106, StrongBox TEE и SQLCipher.",
    feat_2_title: "WebAssembly (WASM) Rust Плагины",
    feat_2_desc: "Быстрый WebAssembly Micro Runtime (WAMR). Холодный старт < 1мс, RAM 150 КБ, криптографическая подпись Ed25519.",
    feat_3_title: "Пространственное Жидкое Стекло (AGSL)",
    feat_3_desc: "GPU-шейдеры с имитацией преломления света и хроматической дисперсии на стабильных 120 FPS без перебивания тем.",
    feat_4_title: "Эргономика Apple Music + Spotify",
    feat_4_desc: "Встроенный плеер 1:1 в стиле Apple Music с живыми мини-басовыми визуализаторами, перемоткой жестами и текстами песен.",
    feat_5_title: "Облачная Экосистема Бейджиков Supabase",
    feat_5_desc: "Глобальное распознавание бейджиков и история на базе Supabase PostgREST с 0мс офлайн-кешем и строгой изоляцией аккаунтов.",
    feat_6_title: "Приватный AI Роутер & Whisper на Устройстве",
    feat_6_desc: "Regex-маскирование номеров карт, телефонов и паролей перед отправкой в облако. Полностью офлайн Whisper STT на NPU.",
    dl_window_title: "Setup_Installer.exe • Official Release",
    dl_title: "Получить Miogram для Android",
    dl_desc: "Официальные сборки криптографически подписаны и собраны в GitHub Actions CI с выравниванием 16 KB ELF для Android 15+.",
    dl_verified: "🟢 Верифицированная сборка",
    footer_text: "Создано с ♡ автором @dkramochka и сообществом Miogram."
  }
};

const BADGE_LORE = {
  uk: {
    original: { title: "Класичне Кібер-Серце", tag: "Класичний", lore: "Канонічне крилате серце Miogram з візором-антеною, обсидіановим ядром, сяючим бірюзовим контуром та рожевими пір'ями. Найперша відзнака екосистеми." },
    pink: { title: "Неоновий Оверлоад", tag: "Неон", lore: "Неоново-рожевий стиль з ребрами серця кольору фуксії та пастельними градієнтними крилами. Атмосфера Needy Streamer Overload." },
    cyan: { title: "Кібер Простір", tag: "Кібер", lore: "Електричні небесно-блакитні крила з осяйною бірюзовою аурою та зоряним світлом. Символізує блискавичну швидкість." },
    dark: { title: "Опівнічний Обсидіан", tag: "Оксамит", lore: "Нічні обсидіанові крила з оксамитовою фіолетовою каймою. Створено для поціновувачів глибокої темної теми." },
    angel: { title: "Небесний Серафим", tag: "Німб", lore: "Пухнасті білосніжні крила з ширяючим німбом та ніжним лавандовим серцем." },
    devil: { title: "Грайливий Демон", tag: "Роги", lore: "Гострі малинові ріжки, зубчасті крила кажана та яскравий рожевий контур." },
    rainbow: { title: "Призматичний Спектр", tag: "Призма", lore: "Плавний 5-колірний райдужний спектр пір'я із золотою каймою та різнокольоровими іскорками." },
    outline: { title: "Кібер Каркас", tag: "Каркас", lore: "Мінімалістичний 1px каркасний контур із неоновим світінням та прозорим центром." },
    glitch: { title: "Хроматичний Глітч", tag: "CRT Глітч", lore: "Хроматичне зміщення RGB (малиновий зліва, бірюзовий справа) з анімованими мікро-тремтіннями CRT-сканування." },
    premium: { title: "Королівське Золото", tag: "Золото", lore: "Сяюча 3-зубчаста золота корона, бурштинові крила та розкішна золота броня." }
  },
  en: {
    original: { title: "Classical Cyber Heart", tag: "Classic", lore: "Canonical Miogram winged heart with antenna visor, obsidian core, electric cyan glowing contour, and pink feather tips. The inaugural distinction of the ecosystem." },
    pink: { title: "Neon Overload", tag: "Neon", lore: "Neon pink style with hot pink chevron heart ribs and pastel gradient wings. True Needy Streamer Overload vibe." },
    cyan: { title: "Cyber Horizon", tag: "Cyber", lore: "Electric sky-blue wings with radiant cyan aura and luminous starlight. Symbolizes cutting-edge speed." },
    dark: { title: "Midnight Velvet", tag: "Velvet", lore: "Midnight obsidian wings with glowing velvet violet fringe. Crafted for dark mode connoisseurs." },
    angel: { title: "Seraphim Halo", tag: "Halo", lore: "Fluffy pure white wings with a floating glowing halo ring and soft lavender periwinkle heart." },
    devil: { title: "Playful Devil", tag: "Horns", lore: "Playful pointed devil horns, scalloped crimson bat wings, and hot pink glowing contour." },
    rainbow: { title: "Prismatic Rainbow", tag: "Prism", lore: "Smooth 5-color prismatic rainbow spectrum feathers with golden halo trim and multi-color sparkles." },
    outline: { title: "Wireframe Cyber", tag: "Wireframe", lore: "Minimalist 1px glowing cyber wireframe contour with bloom and transparent hollow center." },
    glitch: { title: "Chromatic Glitch", tag: "Cyber CRT", lore: "Chromatic RGB displacement (magenta left, cyan right) with animated CRT scanline jitters." },
    premium: { title: "Royal Golden Crown", tag: "Royal Gold", lore: "Radiant 3-peak royal golden crown, amber wings with horizontal slits, and golden chest armor." }
  },
  ru: {
    original: { title: "Классическое Кибер-Сердце", tag: "Классика", lore: "Каноническое крылатое сердце Miogram с визором-антенной, обсидиановым ядром, светящимся бирюзовым контуром и розовыми перьями. Первая награда экосистемы." },
    pink: { title: "Неоновый Оверлоад", tag: "Неон", lore: "Неоново-розовый стиль с ребрами сердца цвета фуксии и пастельными градиентными крыльями. Атмосфера Needy Streamer Overload." },
    cyan: { title: "Кибер Пространство", tag: "Кибер", lore: "Электрические небесно-голубые крылья с сияющей бирюзовой аурой и звездным светом. Символ молниеносной скорости." },
    dark: { title: "Полуночный Обсидиан", tag: "Бархат", lore: "Ночные обсидиановые крылья с бархатной фиолетовой каймой. Создан для ценителей глубокой темной темы." },
    angel: { title: "Небесный Серафим", tag: "Нимб", lore: "Пушистые белоснежные крылья с парящим нимбом и нежным лавандовым сердцем." },
    devil: { title: "Игривый Демон", tag: "Рожки", lore: "Острые малиновые рожки, зубчатые крылья летучей мыши и яркий розовый контур." },
    rainbow: { title: "Призматический Спектр", tag: "Призма", lore: "Плавный 5-цветный радужный спектр перьев с золотой каймой и разноцветными искорками." },
    outline: { title: "Кибер Каркас", tag: "Каркас", lore: "Минималистичный 1px каркасный контур с неоновым свечением и прозрачным центром." },
    glitch: { title: "Хроматический Глитч", tag: "CRT Глитч", lore: "Хроматическое смещение RGB (малиновый слева, бирюзовый справа) с анимированными микро-подергиваниями CRT-сканирования." },
    premium: { title: "Королевское Золото", tag: "Золото", lore: "Сияющая 3-зубчатая золотая корона, янтарные крылья и роскошная золотая броня." }
  }
};

let currentLang = localStorage.getItem("miogram_lang") || (navigator.language.startsWith("ru") ? "ru" : (navigator.language.startsWith("uk") ? "uk" : "en"));

function switchLanguage(lang) {
  currentLang = lang;
  localStorage.setItem("miogram_lang", lang);
  document.documentElement.lang = lang;

  document.querySelectorAll(".lang-pill").forEach(btn => {
    btn.classList.toggle("active", btn.getAttribute("data-lang") === lang);
  });

  const dict = I18N[lang] || I18N.en;
  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (dict[key]) {
      el.textContent = dict[key];
    }
  });

  const loreDict = BADGE_LORE[lang] || BADGE_LORE.en;
  document.querySelectorAll(".badge-tile").forEach(tile => {
    const id = tile.getAttribute("data-id");
    if (loreDict[id]) {
      const tagSpan = tile.querySelector(".tile-tag");
      if (tagSpan) tagSpan.textContent = loreDict[id].tag;
    }
  });

  updateActiveBadgeInfo();
}

// -------------------------------------------------------------
// Supabase Live Community User Counter
// -------------------------------------------------------------
async function fetchLiveUsers() {
  const countEl = document.getElementById("liveUserCount");
  if (!countEl) return;

  try {
    let count = 0;
    // 1. Try miogram_users
    let res = await fetch(`${SUPABASE_URL}/rest/v1/miogram_users?select=user_id`, {
      headers: {
        "apikey": SUPABASE_ANON,
        "Authorization": `Bearer ${SUPABASE_ANON}`,
        "Range": "0-0",
        "Prefer": "count=exact"
      }
    });

    if (res.ok) {
      const cr = res.headers.get("content-range");
      if (cr && cr.includes("/")) {
        count = parseInt(cr.split("/")[1], 10);
      }
    }

    // 2. Fallback to miogram_badges
    if (!count || count === 0) {
      res = await fetch(`${SUPABASE_URL}/rest/v1/miogram_badges?select=user_id`, {
        headers: {
          "apikey": SUPABASE_ANON,
          "Authorization": `Bearer ${SUPABASE_ANON}`,
          "Range": "0-0",
          "Prefer": "count=exact"
        }
      });
      if (res.ok) {
        const cr = res.headers.get("content-range");
        if (cr && cr.includes("/")) {
          count = parseInt(cr.split("/")[1], 10);
        }
      }
    }

    if (count > 0) {
      animateCounter(countEl, count);
    } else {
      countEl.textContent = "1";
    }
  } catch (err) {
    console.warn("Could not fetch user count from Supabase:", err);
    countEl.textContent = "1";
  }
}

function animateCounter(el, target) {
  let current = 0;
  const step = Math.max(1, Math.floor(target / 20));
  const timer = setInterval(() => {
    current += step;
    if (current >= target) {
      current = target;
      clearInterval(timer);
    }
    el.textContent = current.toLocaleString();
  }, 40);
}

// -------------------------------------------------------------
// Fetch Latest Release from GitHub API
// -------------------------------------------------------------
async function fetchLatestRelease() {
  const releaseTagEl = document.getElementById("releaseTag");
  const releaseDateEl = document.getElementById("releaseDate");
  const releaseNotesEl = document.getElementById("releaseNotes");
  const primaryApkLink = document.getElementById("primaryApkLink");
  const heroDownloadBtn = document.getElementById("hero-download-btn");
  const primaryApkText = document.getElementById("primaryApkText");

  try {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    const tag = data.tag_name || "v1.0";
    const dateStr = data.published_at ? new Date(data.published_at).toLocaleDateString() : "";
    const notes = data.body || "Latest stable release with 10 community badges and zero-trust vault.";

    releaseTagEl.textContent = tag;
    releaseDateEl.textContent = dateStr ? `• Released ${dateStr}` : "";
    releaseNotesEl.textContent = notes;

    let apkUrl = FALLBACK_URL;
    let apkSize = "";
    if (data.assets && data.assets.length > 0) {
      const apkAsset = data.assets.find(a => a.name.endsWith(".apk"));
      if (apkAsset) {
        apkUrl = apkAsset.browser_download_url;
        apkSize = ` (${(apkAsset.size / (1024 * 1024)).toFixed(1)} MB)`;
      }
    }

    primaryApkLink.href = apkUrl;
    heroDownloadBtn.href = apkUrl;
    primaryApkText.textContent = `Download Miogram ${tag}${apkSize}`;
  } catch (err) {
    console.warn("Could not fetch release info, using fallback:", err);
    releaseTagEl.textContent = "Latest Release";
    releaseNotesEl.textContent = "Download the latest APK directly from the GitHub releases page.";
    primaryApkLink.href = FALLBACK_URL;
    heroDownloadBtn.href = FALLBACK_URL;
  }
}

// -------------------------------------------------------------
// Interactive 10 Badges Canvas Renderer
// -------------------------------------------------------------
const canvas = document.getElementById("badgeCanvas");
const ctx = canvas.getContext("2d");

let currentBadgeId = "original";
let currentBadgeNum = "01";
let currentBadgeName = "ORIGINAL";

const BADGE_CONFIGS = {
  original: { accent: "#00F0FF", glow: "rgba(0, 240, 255, 0.4)" },
  pink:     { accent: "#FF2A93", glow: "rgba(255, 42, 147, 0.4)" },
  cyan:     { accent: "#00E5FF", glow: "rgba(0, 229, 255, 0.4)" },
  dark:     { accent: "#C77DFF", glow: "rgba(199, 125, 255, 0.4)" },
  angel:    { accent: "#FFFFFF", glow: "rgba(255, 255, 255, 0.4)" },
  devil:    { accent: "#FF006E", glow: "rgba(255, 0, 110, 0.4)" },
  rainbow:  { accent: "#FFD166", glow: "rgba(255, 209, 102, 0.4)" },
  outline:  { accent: "#00F0FF", glow: "rgba(0, 240, 255, 0.4)" },
  glitch:   { accent: "#00F0FF", glow: "rgba(0, 240, 255, 0.4)" },
  premium:  { accent: "#FFD700", glow: "rgba(255, 215, 0, 0.4)" }
};

const DYNAMIC_PARTICLES = [
  { x: 3.5,  speed: 0.00030, sway: 0.28, amp: 1.2, isCross: false, offset: 0.12 },
  { x: 7.0,  speed: 0.00045, sway: 0.35, amp: 1.6, isCross: true,  offset: 0.48 },
  { x: 11.5, speed: 0.00038, sway: 0.22, amp: 1.0, isCross: false, offset: 0.74 },
  { x: 16.5, speed: 0.00042, sway: 0.25, amp: 1.1, isCross: false, offset: 0.29 },
  { x: 21.0, speed: 0.00048, sway: 0.33, amp: 1.5, isCross: true,  offset: 0.85 },
  { x: 24.5, speed: 0.00032, sway: 0.30, amp: 1.3, isCross: false, offset: 0.61 }
];

function updateActiveBadgeInfo() {
  const loreDict = BADGE_LORE[currentLang] || BADGE_LORE.en;
  const info = loreDict[currentBadgeId] || loreDict.original;

  const numberEl = document.getElementById("badgeNumber");
  const titleEl = document.getElementById("badgeTitle");
  const descEl = document.getElementById("badgeDesc");

  if (numberEl) numberEl.textContent = `${currentBadgeNum} — ${currentBadgeName}`;
  if (titleEl) titleEl.textContent = info.title;
  if (descEl) descEl.textContent = info.lore;
}

function renderBadge() {
  const now = performance.now();
  const w = canvas.width;
  const h = canvas.height;
  ctx.clearRect(0, 0, w, h);

  const px = w / 28;
  const py = h / 22;

  // Background Radial Bloom
  const cfg = BADGE_CONFIGS[currentBadgeId] || BADGE_CONFIGS.original;
  const pulse = Math.sin(now * 0.003) * 0.05 + 0.95;
  const grad = ctx.createRadialGradient(w / 2, h / 2, 10, w / 2, h / 2, 130 * pulse);
  grad.addColorStop(0, cfg.glow);
  grad.addColorStop(1, "rgba(0, 0, 0, 0)");
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, w, h);

  // Draw Specific Badge Geometry
  drawBadgeGeometry(ctx, currentBadgeId, px, py);

  // Draw Dynamic Upward Flying Particles
  drawDynamicSparkles(ctx, px, py, now, cfg.accent);

  requestAnimationFrame(renderBadge);
}

function drawBadgeGeometry(c, id, px, py) {
  function drawWings(fillColor, fringeColor) {
    c.fillStyle = fillColor;
    c.fillRect(4 * px, 5 * py, 5 * px, 1 * py);
    c.fillRect(3 * px, 6 * py, 6 * px, 1 * py);
    c.fillRect(1 * px, 7 * py, 8 * px, 3 * py);
    c.fillRect(2 * px, 10 * py, 6 * px, 1 * py);
    c.fillRect(4 * px, 11 * py, 3 * px, 2 * py);

    c.fillRect(19 * px, 5 * py, 5 * px, 1 * py);
    c.fillRect(19 * px, 6 * py, 6 * px, 1 * py);
    c.fillRect(19 * px, 7 * py, 8 * px, 3 * py);
    c.fillRect(20 * px, 10 * py, 6 * px, 1 * py);
    c.fillRect(21 * px, 11 * py, 3 * px, 2 * py);

    c.fillStyle = fringeColor;
    c.fillRect(1 * px, 8 * py, 2 * px, 2 * py);
    c.fillRect(25 * px, 8 * py, 2 * px, 2 * py);
    c.fillRect(4 * px, 12 * py, 3 * px, 1 * py);
    c.fillRect(21 * px, 12 * py, 3 * px, 1 * py);
  }

  function drawHeart(coreColor, contourColor) {
    c.fillStyle = coreColor;
    c.fillRect(10 * px, 7 * py, 3 * px, 1 * py);
    c.fillRect(15 * px, 7 * py, 3 * px, 1 * py);
    c.fillRect(9 * px, 8 * py, 10 * px, 2 * py);
    c.fillRect(8 * px, 10 * py, 12 * px, 2 * py);
    c.fillRect(9 * px, 12 * py, 10 * px, 1 * py);
    c.fillRect(10 * px, 13 * py, 8 * px, 1 * py);
    c.fillRect(11 * px, 14 * py, 6 * px, 1 * py);
    c.fillRect(12 * px, 15 * py, 4 * px, 1 * py);
    c.fillRect(13 * px, 16 * py, 2 * px, 1 * py);

    c.fillStyle = contourColor;
    c.fillRect(10 * px, 6 * py, 3 * px, 1 * py);
    c.fillRect(15 * px, 6 * py, 3 * px, 1 * py);
    c.fillRect(8 * px, 8 * py, 1 * px, 4 * py);
    c.fillRect(19 * px, 8 * py, 1 * px, 4 * py);
  }

  function drawEyes(eyeColor) {
    c.fillStyle = eyeColor;
    c.fillRect(11 * px, 9.5 * py, 1.5 * px, 1.5 * py);
    c.fillRect(15.5 * px, 9.5 * py, 1.5 * px, 1.5 * py);
  }

  if (id === "original") {
    c.fillStyle = "#00F0FF";
    c.fillRect(10 * px, 3 * py, 8 * px, 1 * py);
    c.fillRect(12 * px, 4 * py, 4 * px, 1 * py);
    drawWings("#F0FDFE", "#FF55A3");
    drawHeart("#0F141C", "#00F0FF");
    drawEyes("#FFFFFF");
  } else if (id === "pink") {
    c.fillStyle = "#FF2A93";
    c.fillRect(10 * px, 3 * py, 8 * px, 1 * py);
    drawWings("#FFF0F7", "#FF2A93");
    drawHeart("#1B0F1C", "#FF2A93");
    c.fillStyle = "#FF2A93";
    c.fillRect(11 * px, 12 * py, 6 * px, 1 * py);
    drawEyes("#FFE5F0");
  } else if (id === "cyan") {
    c.fillStyle = "#00E5FF";
    c.fillRect(10 * px, 3 * py, 8 * px, 1 * py);
    drawWings("#E0F7FA", "#00E5FF");
    drawHeart("#0A1822", "#00E5FF");
    drawEyes("#FFFFFF");
  } else if (id === "dark") {
    c.fillStyle = "#9D4EDD";
    c.fillRect(11 * px, 3 * py, 6 * px, 1 * py);
    drawWings("#1B142A", "#C77DFF");
    drawHeart("#120B20", "#9D4EDD");
    drawEyes("#E0AAFF");
  } else if (id === "angel") {
    // Halo
    c.fillStyle = "#FFFFFF";
    c.fillRect(10 * px, 1 * py, 8 * px, 1 * py);
    c.fillRect(8 * px, 2 * py, 2 * px, 1 * py);
    c.fillRect(18 * px, 2 * py, 2 * px, 1 * py);
    drawWings("#FAFAFE", "#B8C0EC");
    drawHeart("#C3BEF0", "#FFFFFF");
    drawEyes("#FFFFFF");
  } else if (id === "devil") {
    // Horns
    c.fillStyle = "#FF0055";
    c.fillRect(9 * px, 4 * py, 2 * px, 3 * py);
    c.fillRect(8 * px, 3 * py, 2 * px, 2 * py);
    c.fillRect(17 * px, 4 * py, 2 * px, 3 * py);
    c.fillRect(18 * px, 3 * py, 2 * px, 2 * py);
    drawWings("#FF3377", "#B8003D");
    drawHeart("#1C0A15", "#FF0055");
    drawEyes("#FFB3C6");
  } else if (id === "rainbow") {
    const rainbow = ["#FF3377", "#9D4EDD", "#00B4D8", "#06D6A0", "#FFD166"];
    for (let i = 0; i < 5; i++) {
      c.fillStyle = rainbow[i];
      c.fillRect(4 * px, (5 + i * 1.5) * py, 5 * px, 1.5 * py);
      c.fillRect(19 * px, (5 + i * 1.5) * py, 5 * px, 1.5 * py);
    }
    drawHeart("#10141E", "#FFD166");
    drawEyes("#FFFFFF");
  } else if (id === "outline") {
    c.strokeStyle = "#00F0FF";
    c.lineWidth = 1.5;
    c.strokeRect(4 * px, 5 * py, 5 * px, 7 * py);
    c.strokeRect(19 * px, 5 * py, 5 * px, 7 * py);
    c.strokeRect(9 * px, 7 * py, 10 * px, 9 * py);
    drawEyes("#00F0FF");
  } else if (id === "glitch") {
    c.fillStyle = "rgba(255, 0, 85, 0.7)";
    c.fillRect(3 * px, 5 * py, 5 * px, 7 * py);
    c.fillStyle = "rgba(0, 240, 255, 0.7)";
    c.fillRect(20 * px, 5 * py, 5 * px, 7 * py);
    drawWings("#FFFFFF", "#00F0FF");
    drawHeart("#10121C", "#00F0FF");
    drawEyes("#FFFFFF");
  } else if (id === "premium") {
    // Crown
    c.fillStyle = "#FFD700";
    c.fillRect(10 * px, 2 * py, 2 * px, 3 * py);
    c.fillRect(13 * px, 1 * py, 2 * px, 4 * py);
    c.fillRect(16 * px, 2 * py, 2 * px, 3 * py);
    c.fillRect(10 * px, 5 * py, 8 * px, 1 * py);
    drawWings("#FFE066", "#CC8800");
    drawHeart("#1B1408", "#FFD700");
    c.fillStyle = "#FFD700";
    c.fillRect(10 * px, 11 * py, 8 * px, 1 * py);
    drawEyes("#FFF5B8");
  }
}

function drawDynamicSparkles(c, px, py, now, color) {
  for (const p of DYNAMIC_PARTICLES) {
    const travel = (now * p.speed + p.offset) % 1.0;
    const y = (1.0 - travel) * 22;
    const x = p.x + Math.sin(y * p.sway + now * 0.0025) * p.amp;

    const alphaSin = Math.sin(travel * Math.PI);
    if (alphaSin <= 0.08) continue;

    c.save();
    c.globalAlpha = alphaSin;
    c.fillStyle = color;

    const sx = x * px;
    const sy = y * py;

    if (p.isCross) {
      c.fillRect(sx, sy - 1.0 * py, 1.0 * px, 3.0 * py);
      c.fillRect(sx - 1.0 * px, sy, 3.0 * px, 1.0 * py);
      c.fillStyle = "#FFFFFF";
      c.fillRect(sx, sy, 1.0 * px, 1.0 * py);
    } else {
      c.fillRect(sx - 0.5 * px, sy - 0.5 * py, 1.0 * px, 1.0 * py);
      c.fillStyle = "#FFFFFF";
      c.fillRect(sx - 0.25 * px, sy - 0.25 * py, 0.5 * px, 0.5 * py);
    }
    c.restore();
  }
}

// Selector Tile Clicks
document.querySelectorAll(".badge-tile").forEach(tile => {
  tile.addEventListener("click", () => {
    document.querySelectorAll(".badge-tile").forEach(t => t.classList.remove("active"));
    tile.classList.add("active");

    currentBadgeId = tile.getAttribute("data-id");
    currentBadgeNum = tile.getAttribute("data-num");
    currentBadgeName = tile.querySelector(".tile-name") ? tile.querySelector(".tile-name").textContent : currentBadgeId.toUpperCase();

    updateActiveBadgeInfo();
  });
});

// Language Pills Click
document.querySelectorAll(".lang-pill").forEach(btn => {
  btn.addEventListener("click", () => {
    const lang = btn.getAttribute("data-lang");
    switchLanguage(lang);
  });
});

// Retro Clock
function updateClock() {
  const clockEl = document.getElementById("taskbarClock");
  if (!clockEl) return;
  const d = new Date();
  const h = String(d.getHours()).padStart(2, "0");
  const m = String(d.getMinutes()).padStart(2, "0");
  const s = String(d.getSeconds()).padStart(2, "0");
  clockEl.textContent = `${h}:${m}:${s}`;
}

// Initialization
document.addEventListener("DOMContentLoaded", () => {
  switchLanguage(currentLang);
  fetchLatestRelease();
  fetchLiveUsers();
  updateClock();
  setInterval(updateClock, 1000);
  requestAnimationFrame(renderBadge);
});
