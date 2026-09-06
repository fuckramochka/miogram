# 🤖 MASTER PROMPT FOR CODEX: ARCHITECTURAL BLUEPRINT & AUDIT INSTRUCTIONS

> **Note for Codex**: DO NOT index, crawl, or grep the entire repository. This repository is a massive Android codebase (fork of Telegram Android + AyuGram + Miogram bridge, over 200MB of sources). All essential architectural context, file paths, rules, and integration points are already extracted and documented below. Follow the targeted review instructions at the bottom.

---

## 1. Project Overview & Identity
- **Repository**: `fuckramochka/miogram` (branch: `main`, local folder: `exteraless`)
- **App Name**: Miogram (Міограм)
- **Base**: Hardened Telegram Android v10.x/v11.x fork with AyuGram features + custom Miogram Secure Agentic Workspace.
- **Tech Stack**:
  - Java 21 & Kotlin 2.x
  - AGP 9.3.1 + Gradle 9.7.1
  - Chaquopy (Python 3.12 embedded runtime)
  - Ccache + NDK for C/C++ native libs
  - Programmatic UI (pure Android Views, `LayoutHelper`, `AndroidUtilities`, `Theme`), zero XML layouts for core screens.
  - CI: GitHub Actions (`.github/workflows/miogram.yml`), release builds `arm64-v8a`, auto-generates GitHub release tags and posts APK to Telegram channel.

---

## 2. Directory Structure & Key Components

The codebase follows a strict layer isolation model: `app.miogram.ui` → `app.miogram.bridge` → `app.miogram.core`.

```text
TMessagesProj/
├── src/main/java/
│   ├── org/telegram/                     <-- Upstream Telegram core engine
│   │   ├── messenger/
│   │   │   ├── MediaController.java      <-- ExoPlayer audio engine (tracks audioProgressMs)
│   │   │   ├── AndroidUtilities.java     <-- dp(), display metrics, bold fonts
│   │   │   └── LocaleController.java     <-- Localization
│   │   └── ui/
│   │       ├── Components/
│   │       │   ├── AudioPlayerAlert.java <-- Telegram audio player bottom sheet
│   │       │   └── LayoutHelper.java     <-- createFrame, createLinear helper
│   │       ├── Cells/
│   │       │   ├── ChatMessageCell.java  <-- Chat bubble rendering (hooks MiogramUiEngine)
│   │       │   └── DialogCell.java       <-- Chat list row (hooks cards, avatar glow, status)
│   │       ├── DialogsActivity.java      <-- Chats list / main activity
│   │       ├── ChatActivity.java         <-- Chat conversation view
│   │       └── ProfileActivity.java      <-- User / Channel profile header & banner
│   │
│   └── app/miogram/bridge/               <-- Miogram Java bridge & custom UI subsystem
│       ├── player/
│       │   └── MiogramModernPlayerLayout.java  <-- Overhauled full-height modern player view
│       ├── lyrics/
│       │   ├── MiogramLyricsView.java          <-- Full-screen LRC lyrics/karaoke view with waveform
│       │   ├── MiogramSourceSelectAlert.java   <-- "Джерело тексту" bottom sheet (8 sources)
│       │   ├── MiogramLyricsEngine.java        <-- Lyrics provider (LRCLib, NetEase, AI) + strict match
│       │   └── MiogramLrcModel.java            <-- LRC song & line data structures
│       ├── customui/
│       │   ├── MiogramCustomUiActivity.java    <-- Custom UI Studio settings screen
│       │   ├── MiogramCustomUiPrefs.java       <-- Preferences store for custom UI
│       │   ├── MiogramUiEngine.java            <-- Canvas drawing engine (bubbles, rings, names)
│       │   └── MiogramHaptic.java              <-- System haptic feedback engine
│       ├── ui/
│       │   ├── ios/MiogramIosLayout.java       <-- Cupertino iOS mode (Large titles, inset cards)
│       │   ├── discord/MiogramDiscordLayout.java <-- Discord rail mode (guild column, squircles)
│       │   ├── ame/MiogramAmeAesthetic.java    <-- Cyber-pastel / Ame vaporwave aesthetic
│       │   ├── MiogramGlassmorphism.java       <-- Glassmorphism / blur shader backdrops
│       │   └── player/MiogramBassVisualizer.java <-- Audio frequency/bass visualizer
│       ├── badge/                             <-- Profile badges (Supabase cloud + local)
│       ├── feed/                              <-- Smart Feed & AI Channel Digests
│       ├── kanban/                            <-- Kanban task boards in chats
│       ├── multichat/                         <-- Floating chat bubbles & split screen
│       └── MiogramLocale.java                 <-- Trilingual helper (UA / RU / EN)
│
└── src/main/kotlin/app/miogram/core/     <-- Zero-Knowledge cryptographic core
    ├── crypto/                                <-- Argon2id KDF, AES-256-GCM envelope
    ├── vault/                                 <-- ProfileVault, Duress PIN, metadata codec
    └── plugins/                               <-- WASM plugin engine & Ed25519 trust layer
```

---

## 3. Critical Telegram & Miogram Rules (NEVER BREAK THESE)

1. **No Missing Resources**:
   - Telegram does not have standard Android XML resources for everything.
   - Only use confirmed drawables: `R.drawable.msg_close`, `R.drawable.msg_list`, `R.drawable.filter_setup`, `R.drawable.baseline_favorite_20`, `R.drawable.action_share`, `R.drawable.arrow_more`, `R.drawable.ic_ab_other`.
   - Never reference nonexistent `R.string.*` resources. Use `MiogramLocale.get("Текст (UA)", "Текст (RU)", "Text (EN)")`.
2. **Programmatic View Creation**:
   - Do NOT use XML layouts. Everything in Telegram UI is constructed in code using `LayoutHelper.createFrame(...)`, `LayoutHelper.createLinear(...)`, `AndroidUtilities.dp(...)`, and `Theme.getColor(...)`.
3. **Threading & Concurrency**:
   - All network and intensive tasks (e.g. lyrics API calls, AI inference, file reading) MUST run on background threads (`Utilities.globalQueue` or `Dispatchers.IO`), never on the main Looper thread.
4. **Final Variables & Lambdas**:
   - Never reassign `final` fields inside constructors. Initialize adapters and layout managers before any lambda or listener that references them.
5. **Canvas & Performance in `onDraw()`**:
   - `ChatMessageCell` and `DialogCell` are rendered 120 times per second during fast fling.
   - **NO allocations inside `onDraw` / `draw` methods** (`new Paint()`, `new RectF()`, `new Path()`). All objects must be pre-allocated static or member fields.

---

## 4. Current State of Key Focus Areas

### 🎵 1. Audio Player & Lyrics
- **Current implementation**:
  - `AudioPlayerAlert.java` hosts `MiogramModernPlayerLayout`.
  - `MiogramModernPlayerLayout.java` manages switching between Cover, Lyrics (`MiogramLyricsView`), and Queue.
  - `MiogramLyricsView.java` has top header with marquee track name/artist, mode toggle `[A]`, source selector `[≡-]`, close button `[✕]`, horizontal `WaveformDotsView`, auto-scrolling lyrics with bold active line (22sp) and smooth centering, click-to-seek, and compact error/empty state.
  - `MiogramSourceSelectAlert.java` provides an 8-source bottom sheet (Auto, Server, LRCLib, NetEase, Yandex, Genius, YouTube, AI) with Toast pill feedback.
  - `MediaController.java` records real-time playback position in `audioProgressMs = (int) lastProgress;`.
  - `MiogramLyricsEngine.java` features strict track validation (`isMatchingTrack`: ±6s duration and ≥ 55% title word similarity).

### 🎨 2. Custom UI Studio & Engine
- **Current implementation**:
  - `MiogramCustomUiActivity.java`: A massive studio with live preview cards for chat bubbles, avatar rings, glow effects, text gradients, and profile banners.
  - `MiogramUiEngine.java`: Injects canvas hooks into `ChatMessageCell` (bubble gradients, glows, text shaders), `DialogCell` (card backgrounds, avatar glow rings, online dots), and `ProfileActivity` (custom banners, avatars, profile backgrounds).
  - `MiogramCustomUiPrefs.java`: Key-value storage backing custom UI settings with legacy fallback.

### 📱 3. Layouts & Navigation Modes
- **Current implementation**:
  - `MiogramIosLayout.java`: iOS Cupertino mode with large collapsible headers, SF-styled inset grouped cards, squircle continuous curve avatars, and iOS search bar.
  - `MiogramDiscordLayout.java`: Discord rail mode with a left column for servers/folders, animated morphing squircles (24dp to 16dp), pill indicators, and mute/deafen bars.
  - `MiogramAmeAesthetic.java`: Ame-chan cyber-pastel / vaporwave aesthetic (neon pink, cyan, lavender, halo rings).
  - `MiogramGlassmorphism.java`: Liquid glass blurs and gradient strokes.

---

## 5. YOUR TASK (CODEX)

Do NOT read the whole repository. Open and inspect ONLY the following 6 files:
1. `TMessagesProj/src/main/java/app/miogram/bridge/player/MiogramModernPlayerLayout.java`
2. `TMessagesProj/src/main/java/app/miogram/bridge/lyrics/MiogramLyricsView.java`
3. `TMessagesProj/src/main/java/app/miogram/bridge/customui/MiogramCustomUiActivity.java`
4. `TMessagesProj/src/main/java/app/miogram/bridge/customui/MiogramUiEngine.java`
5. `TMessagesProj/src/main/java/app/miogram/bridge/ui/ios/MiogramIosLayout.java`
6. `TMessagesProj/src/main/java/app/miogram/bridge/ui/discord/MiogramDiscordLayout.java`

Based on this architectural blueprint and your targeted inspection of these 6 files, generate a **Comprehensive, Prioritized Master Action Plan** of everything that needs improvement, modernizing, or refactoring.

Structure your response into the following clear sections:

### 📋 Section A: Audio Player & Lyrics Overhaul
- UI/UX polish (gestures, sheet drag physics, full-height cover transitions, fluid animations).
- Karaoke sync refinements (syllable-level highlight, smooth line interpolation, spring scrolling physics).
- Audio features (equalizer, bass booster integration, playback speed slider, audio cache).
- Edge cases & error handling (offline mode, failed searches, malformed LRC).

### 📐 Section B: Layouts & Navigation Systems
- iOS / Discord / Classic layout modes: seamless runtime switching without app restart or visual artifacts.
- Header transitions (Large Title collapsing, search bar blur, edge-to-edge Android 15/16 window insets).
- Bottom navigation / Floating action bar / Gesture interactions.
- Split-screen & multi-chat ergonomics.

### 🎨 Section C: Custom UI Studio & Canvas Rendering
- `MiogramUiEngine` optimizations: 120Hz fling performance, eliminating heap allocations in canvas draw cycles.
- Bubble styles: realistic gradients, 3D glass borders, dynamic light angles, emoji reaction alignments.
- Avatar customizations: hexagon, star, squircle morphing shapes, animated pulsing story rings.
- Presets system: import/export custom themes via JSON / QR code / link.

### ⚡ Section D: Quick Wins vs. Deep Architectural Changes
- Table of immediate high-impact fixes (1-2 days) vs. deep architectural enhancements (1-2 weeks).
- Risk analysis for each proposed item.
