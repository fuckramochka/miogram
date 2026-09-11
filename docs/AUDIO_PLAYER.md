# Miogram Modern Audio Player Architecture

> **Miogram / Міограм** · Modern Audio Architecture Guide  
> Author: **[@dkramochka](https://t.me/dkramochka)**

---

## 1. Overview

Miogram features an overhauled, audiophile-grade media player experience built directly into the Telegram messaging platform. Combining Apple Music card ergonomics, Spotify-grade queue management, real-time dynamic audio visualization, and gesture-driven scrubbing, the player delivers a premier listening experience without external music apps.

---

## 2. Component Architecture

The audio subsystem is partitioned into modular UI and controller components:

```
AudioPlayerAlert (Host Dialog / Window)
  │
  ├───► MiogramModernPlayerLayout (Visual Card Deck)
  │       ├─── centerContainer (Cover Art / Fullscreen / Lyrics / Queue)
  │       │      ├─── Fullscreen Cover Holder
  │       │      │      ├─── Fullscreen Cover Art (Scaled / Centered)
  │       │      │      ├─── Fullscreen Title & Author (Dynamic Marquee)
  │       │      │      ├─── Fullscreen Favorite Heart Toggle
  │       │      │      └─── Fullscreen Bass Visualizer (Dynamic Floating)
  │       │      ├─── MiogramLyricsView (Synced Lyrics Engine)
  │       │      └─── Queue List Adapter
  │       │
  │       └─── bottomSection (Controls & Ergonomics)
  │              ├─── Track Info Header (Title, Performer, Heart Toggle)
  │              ├─── Progress Slider (Scrubbing, Time Insets)
  │              └─── Control Buttons Row:
  │                     [ Shuffle ]  [ Repeat ]  [ Prev ]  [ Play/Pause Hero ]  [ Next ]  [ Queue ]
  │                     └─── Integrated Compact Bass Visualizer
  │
  └───► MediaController (Telegram Audio Pipeline / ExoPlayer Backend)
```

---

## 3. Key Enhancements & Bug Fixes

### 3.1. Dynamic Bass Visualizer (`MiogramBassVisualizer`)
* **Real-Time Spectrum Simulation:** Generates multi-band audio reactive waveforms using smoothed decay physics.
* **Theme Integration:** Dynamically adapts bar colors to the active player palette or custom theme accent via `.setColor(int color)`.
* **Zero Resource Leaking:** Tickers and animation listeners are safely detached upon window detach (`onDetachedFromWindow`), preventing battery drain and background memory leaks.

### 3.2. Dedicated Controls Row (6-Slot Layout)
* **Dedicated Shuffle Button:** Direct 1-click toggling between Sequential and Random playback (`SharedConfig.toggleShuffleMusic()`).
* **Repeat Button with Submenu:** Tapping toggles repeat modes (Off $\rightarrow$ Repeat All $\rightarrow$ Repeat One); **Long-pressing** pops open the Telegram repeat submenu for instant fine-grained selection.
* **Hero Play/Pause Button Fix:** 
  * Removed duplicate nested ripple backgrounds that previously caused muddy/dark double-layer artifacts.
  * Replaced `PorterDuff.Mode.MULTIPLY` with `PorterDuff.Mode.SRC_IN` (`0xFFFFFFFF`), rendering play/pause icons with crisp, luminous white contrast over the accent circle.

### 3.3. Fullscreen Cover Mode & Dynamic Padding
* **Dynamic Content Padding:** Center container (`centerContainer`) dynamically measures the bottom section's height and applies bottom margin accordingly. This ensures lyrics, queue items, and album art are never clipped behind bottom controls on varied screen aspect ratios.
* **Fullscreen Metadata Display:** Fullscreen cover mode displays track title, artist name, and a heart favorite button overlaid directly onto the immersive artwork, accompanied by a dynamic bass visualizer.

---

## 4. Gestures & Navigation

* **Swipe Down:** Smooth spring-physics transition from fullscreen player to minimized compact bottom sheet.
* **Swipe Left / Right on Cover:** Skip to next or previous track in queue.
* **Long Press on Progress Slider:** Precision seeking mode with magnification.
* **Tap Lyrics Toggle:** Instant cross-fade to live synchronized LRC lyrics.
